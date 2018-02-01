/**
 * Created by Jonathan Moldover on 7/8/17
 * Copyright 2018 Bright Spots
 * Purpose: perform ranked choice tabulation calculations
 * round-by-round tabulation of votes to each candidate
 * handles overvote / undervote decisions batch elimination and tiebreaks
 * results are logged to console and audit file
 * Version: 1.0
 */

package com.rcv;

import java.util.*;

public class Tabulator {

  // When the CVR contains an overvote we "normalize" it to use this string
  static String explicitOvervoteLabel = "overvote";

  // OvervoteRule determines how overvotes are handled
  enum OvervoteRule {
    EXHAUST_IMMEDIATELY,
    ALWAYS_SKIP_TO_NEXT_RANK,
    EXHAUST_IF_ANY_CONTINUING,
    IGNORE_IF_ANY_CONTINUING,
    EXHAUST_IF_MULTIPLE_CONTINUING,
    IGNORE_IF_MULTIPLE_CONTINUING,
    RULE_UNKNOWN
  }

  // OvervoteDecision is the result of applying an OvervoteRule to an overvote in a CVR
  enum OvervoteDecision {
    NONE,
    EXHAUST,
    IGNORE,
    SKIP_TO_NEXT_RANK,
  }

  // TieBreakMode determines how ties will be handled
  enum TieBreakMode {
    RANDOM,
    INTERACTIVE,
    PREVIOUS_ROUND_COUNTS_THEN_RANDOM,
    PREVIOUS_ROUND_COUNTS_THEN_INTERACTIVE,
    MODE_UNKNOWN
  }

  // cast vote records parsed from CVR input files
  private List<CastVoteRecord> castVoteRecords;
  // all candidateIDs for this election parsed from the election config (does not include UWIs)
  private List<String> candidateIDs;
  // election config contains specific rules and CVR input file paths to be used when tabulating
  // this election
  private ElectionConfig config;

  // roundTallies is a map from round number to a map from candidate ID to vote total for the round
  // e.g. roundTallies[1] contains a map of all candidate ID -> votes for each candidate in round 1
  // this structure is computed over the course of tabulation
  private Map<Integer, Map<String, Integer>> roundTallies = new HashMap<>();

  // candidateToRoundEliminated is a map from candidate ID to round in which they were eliminated
  private Map<String, Integer> candidateToRoundEliminated = new HashMap<>();

  // the winning candidateID
  private String winner;

  // when tabulation is complete this will be how many rounds did it take to determine a winner
  private int currentRound = 0;

  // simple container class used during batch elimination process to store the results
  // for later logging output
  static class BatchElimination {
    // the candidate eliminated
    String candidateID;
    // how many total votes were totaled when this candidate was eliminated
    int runningTotal;
    // next highest count total (validates that we were correctly batch eliminated)
    int nextHighestTally;

    // constructor simply assigns input params to member variables
    // param: candidateID the candidate eliminated
    // param: runningTotal how many total votes were totaled when this candidate was eliminated
    // param: nextHighestTally next highest count total (validates that we were correctly batch eliminated)
    BatchElimination(String candidateID, int runningTotal, int nextHighestTally) {
      this.candidateID = candidateID;
      this.runningTotal = runningTotal;
      this.nextHighestTally = nextHighestTally;
    }
  }

  // map of round to TieBreak objects to record how tiebreaks were decided
  private Map<Integer, TieBreak> roundToTieBreak = new HashMap<>();

  // Tabulator constructor assigns input params to member variables and caches the candidateIDs
  // which will be used when reading input cast vote records
  // param: castVoteRecords list of all cast vote records to be tabulated for this contest
  // param: config describes various tabulation rules to be used for tabulation
  Tabulator(
      List<CastVoteRecord> castVoteRecords,
      ElectionConfig config
  ) {
    this.castVoteRecords = castVoteRecords;
    this.candidateIDs = config.getCandidateCodeList();
    this.config = config;
  }

  // purpose: run the main tabulation routine to determine election results
  //  this is the high-level control of the tabulation algorithm
  // throws: Exception when no candidates are eliminated in a round
  public void tabulate() throws Exception {

    log("Beginning tabulation for contest.");
    log("There are %d candidates for this contest:", numCandidates());
    for (String candidateID : candidateIDs) {
      log("%s", candidateID);
    }
    log("There are %d cast vote records for this contest.", castVoteRecords.size());

    // add UWI string to candidateIDs so it will be tallied similarly to other candidates
    if (config.undeclaredWriteInLabel() != null) {
      this.candidateIDs.add(config.undeclaredWriteInLabel());
    }

    // Loop until we achieve a majority winner: at each iteration we will eliminate one or more
    // candidates and gradually transfer votes to the remaining candidates.
    while (winner == null) {
      currentRound++;
      log("Round: %d", currentRound);

      // currentRoundCandidateToTally is a map from candidateID to vote tally for the current round.
      // At each iteration of this loop the eliminatedRound object will gain entries as candidates
      // are eliminated.
      // Conversely, the currentRoundCandidateToTally object returned here will contain fewer
      // entries, each of which will have as many or more votes than they did in prior rounds.
      // Eventually a winner will be chosen.
      Map<String, Integer> currentRoundCandidateToTally =
        getRoundTally(castVoteRecords, candidateToRoundEliminated, currentRound);
      roundTallies.put(currentRound, currentRoundCandidateToTally);

      // currentRoundTallyToCandidates is a sorted map from tally to candidate(s) with that tally.
      SortedMap<Integer, LinkedList<String>> currentRoundTallyToCandidates = buildTallyToCandidates(
        currentRoundCandidateToTally,
        currentRoundCandidateToTally.keySet(),
        true
      );
      // see if a winner is determined in this iteration
      winner = identifyWinner(currentRoundCandidateToTally, currentRoundTallyToCandidates);

      // if no winner determine who will be eliminated in this round
      if (winner == null) {
        // container for eliminated candidate(s)
        List<String> eliminated = new LinkedList<>();

        // Four mutually exclusive ways to eliminate candidates.
        boolean foundCandidateToEliminate =
          // 1. Some races contain undeclared write-ins that should be dropped immediately.
          dropUWI(eliminated, currentRoundCandidateToTally) ||
          // 2. If there's a minimum vote threshold, drop all candidates below that threshold.
          dropCandidatesBelowThreshold(eliminated, currentRoundTallyToCandidates) ||
          // 3. Otherwise, try batch elimination.
          doBatchElimination(eliminated, currentRoundTallyToCandidates) ||
          // 4. If we didn't do batch elimination, eliminate the remaining candidate with the lowest
          //    tally, breaking a tie if needed.
          doRegularElimination(eliminated, currentRoundTallyToCandidates);

        if (!foundCandidateToEliminate) {
          throw new Exception("Failed to eliminate any candidates.");
        }
        // store the losers
        for (String loser : eliminated) {
          candidateToRoundEliminated.put(loser, currentRound);
        }
      }
    }
  }

  private String identifyWinner(
    Map<String, Integer> currentRoundCandidateToTally,
    SortedMap<Integer, LinkedList<String>> currentRoundTallyToCandidates
  ) {
    String selectedWinner = null;

    // currentRoundTotalVotes is total votes across all candidates in this round
    int currentRoundTotalVotes = 0;
    for (int numVotes : currentRoundCandidateToTally.values()) {
      currentRoundTotalVotes += numVotes;
    }
    log("Total votes in round %d: %d.", currentRound, currentRoundTotalVotes);

    int maxVotes = currentRoundTallyToCandidates.lastKey();
    // Does the leader have a majority of non-exhausted ballots? In other words, is maxVotes
    // greater than half of the total votes counted in this round?
    if (maxVotes > (float)currentRoundTotalVotes / 2.0) {
      // we have a winner
      for (Integer votes : currentRoundTallyToCandidates.keySet()) {
        // record winner
        if (votes == maxVotes) {
          selectedWinner = currentRoundTallyToCandidates.get(votes).getFirst();
          break;
        }
      }
      log("%s won in round %d with %d votes.", winner, currentRound, maxVotes);
    }
    return selectedWinner;
  }

  private boolean dropUWI(
    List<String> eliminated, Map<String, Integer> currentRoundCandidateToTally
  ) {
    if (
      currentRound == 1 &&
      config.undeclaredWriteInLabel() != null &&
      candidateIDs.contains(config.undeclaredWriteInLabel()) &&
      currentRoundCandidateToTally.get(config.undeclaredWriteInLabel()) > 0
    ) {
      eliminated.add(config.undeclaredWriteInLabel());
      log(
        "Eliminated %s in round %d because it represents undeclared write-ins. It had %d votes.",
        config.undeclaredWriteInLabel(),
        currentRound,
        currentRoundCandidateToTally.get(config.undeclaredWriteInLabel())
      );
    }
    return !eliminated.isEmpty();
  }

  private boolean dropCandidatesBelowThreshold(
    List<String> eliminated,
    SortedMap<Integer, LinkedList<String>> currentRoundTallyToCandidates
  ) {
    if (
      eliminated.isEmpty() &&
      config.minimumVoteThreshold() != null &&
      currentRoundTallyToCandidates.firstKey() < config.minimumVoteThreshold()
    ) {
      for (int count : currentRoundTallyToCandidates.keySet()) {
        if (count < config.minimumVoteThreshold()) {
          for (String candidate : currentRoundTallyToCandidates.get(count)) {
            eliminated.add(candidate);
            log(
              "Eliminated %s in round %d because they only had %d vote(s), below the minimum " +
                "threshold of %d.",
              candidate,
              currentRound,
              count,
              config.minimumVoteThreshold()
            );
          }
        }
      }
    }
    return !eliminated.isEmpty();
  }

  private boolean doBatchElimination(
    List<String> eliminated,
    SortedMap<Integer, LinkedList<String>> currentRoundTallyToCandidates
  ) {
    if (eliminated.isEmpty() && config.batchElimination()) {
      List<BatchElimination> batchEliminations = runBatchElimination(currentRoundTallyToCandidates);
      if (batchEliminations.size() > 1) {
        for (BatchElimination elimination : batchEliminations) {
          eliminated.add(elimination.candidateID);
          log(
            "Batch-eliminated %s in round %d. The running total was %d vote(s) and the " +
              "next-highest count was %d vote(s).",
            elimination.candidateID,
            currentRound,
            elimination.runningTotal,
            elimination.nextHighestTally
          );
        }
      }
    }
    return !eliminated.isEmpty();
  }

  private boolean doRegularElimination(
    List<String> eliminated,
    SortedMap<Integer, LinkedList<String>> currentRoundTallyToCandidates
  ) {
    if (eliminated.isEmpty()) {
      String loser;
      int minVotes = currentRoundTallyToCandidates.firstKey();
      LinkedList<String> lastPlace = currentRoundTallyToCandidates.get(minVotes);
      if (lastPlace.size() > 1) {
        TieBreak tieBreak =
          new TieBreak(lastPlace, config.tiebreakMode(), currentRound, minVotes, roundTallies);
        loser = tieBreak.getSelection();
        roundToTieBreak.put(currentRound, tieBreak);
        log(
          "%s lost a tie-breaker in round %d against %s. Each candidate had %d vote(s). %s",
          loser,
          currentRound,
          tieBreak.nonselectedString(),
          minVotes,
          tieBreak.getExplanation()
        );
      } else {
        loser = lastPlace.getFirst();
        log(
          "%s was eliminated in round %d with %d vote(s).",
          loser,
          currentRound,
          minVotes
        );
      }
      eliminated.add(loser);
    }
    return !eliminated.isEmpty();
  }

  public void generateSummarySpreadsheet(String outputFile) {
    ResultsWriter writer = new ResultsWriter().
      setNumRounds(currentRound).
      setRoundTallies(roundTallies).
      setCandidatesToRoundEliminated(candidateToRoundEliminated).
      setOutputFilePath(outputFile).
      setContestName(config.contestName()).
      setJurisdiction(config.jurisdiction()).
      setOffice(config.office()).
      setElectionDate(config.electionDate()).
      setNumCandidates(numCandidates()).
      setUndeclaredWriteInString(config.undeclaredWriteInLabel()).
      setWinner(winner).
      setElectionConfig(config);

    writer.generateSummarySpreadsheet();
  }

  // helper function for logging to console and audit
  // param: s input s to be logged
  // param: var1 ... objects to be formatted into the log output
  static void log(String s, Object... var1) {
    Logger.log(s, var1);
  }

  // Function: runBatchElimination
  // Purpose: applies batch elimination logic to the input vote counts to remove multiple candidates
  // in a single round if their vote counts are so low that they could not possibly end up winning.
  // Consider, after each round of voting a candidate not eliminated could potentially receive ALL
  // the votes from candidates who ARE eliminated, keeping them in the race and "leapfrogging"
  // ahead of candidates who were leading them.
  //
  // In this algorithm we sum candidate vote totals (low to high) and find where this leapfrogging
  // is impossible: that is, when the sum of all batch-eliminated candidates' votes fails to equal
  // or exceed the next-lowest candidate vote total.
  // param: currentRoundTallyToCandidates map from vote tally to candidates with that tally
  // returns: list of BatchElimination objects, one for each batch-eliminated candidate
  private List<BatchElimination> runBatchElimination(
    SortedMap<Integer, LinkedList<String>> currentRoundTallyToCandidates
  ) {
    // The sum total of all vote counts examined. This must equal or exceed the next-lowest
    // candidate tally to prevent batch elimination.
    int runningTotal = 0;
    // Tracks candidates whose totals have been included in the runningTotal and thus are being
    // considered for batch elimination.
    List<String> candidatesSeen = new LinkedList<>();
    // Tracks candidates who have been batch-eliminated (to prevent duplicate eliminations).
    Set<String> candidatesEliminated = new HashSet<>();
    // BatchElimination objects contain contextual data that will be used by the tabulation to log
    // the batch elimination results.
    List<BatchElimination> eliminations = new LinkedList<>();
    // At each iteration, currentVoteTally is the next-lowest vote count received by one or more
    // candidate(s) in the current round.
    for (int currentVoteTally : currentRoundTallyToCandidates.keySet()) {
      // Test whether leapfrogging is possible.
      if (runningTotal < currentVoteTally) {
        // Not possible, so eliminate everyone who has been seen and not eliminated yet.
        for (String candidate : candidatesSeen) {
          if (!candidatesEliminated.contains(candidate)) {
            candidatesEliminated.add(candidate);
            eliminations.add(new BatchElimination(candidate, runningTotal, currentVoteTally));
          }
        }
      }
      // Add the candidates for the currentVoteTally to the seen list and accumulate their votes.
      List<String> currentCandidates = currentRoundTallyToCandidates.get(currentVoteTally);
      runningTotal += currentVoteTally * currentCandidates.size();
      candidatesSeen.addAll(currentCandidates);
    }
    return eliminations;
  }

  private OvervoteDecision getOvervoteDecision(
    Set<String> candidateIDs,
    Map<String, Integer> eliminatedRound
  ) {
    OvervoteDecision decision;

    if (candidateIDs.size() == 0 ||
        (candidateIDs.size() == 1 && candidateIDs.toArray()[0] != explicitOvervoteLabel)) {
      decision = OvervoteDecision.NONE;
    } else if (config.overvoteRule() == OvervoteRule.EXHAUST_IMMEDIATELY) {
      decision = OvervoteDecision.EXHAUST;
    } else if (config.overvoteRule() == OvervoteRule.ALWAYS_SKIP_TO_NEXT_RANK) {
      decision = OvervoteDecision.SKIP_TO_NEXT_RANK;
    } else {
      List<String> continuingAtThisRank = new LinkedList<>();
      for (String candidateID : candidateIDs) {
        if (eliminatedRound.get(candidateID) == null) {
          continuingAtThisRank.add(candidateID);
        }
      }

      if (continuingAtThisRank.size() > 0) {
        if (config.overvoteRule() == OvervoteRule.EXHAUST_IF_ANY_CONTINUING) {
          decision = OvervoteDecision.EXHAUST;
        } else if (config.overvoteRule() == OvervoteRule.IGNORE_IF_ANY_CONTINUING) {
          decision = OvervoteDecision.IGNORE;
        } else if (continuingAtThisRank.size() > 1) {
          if (config.overvoteRule() == OvervoteRule.EXHAUST_IF_MULTIPLE_CONTINUING) {
            decision = OvervoteDecision.EXHAUST;
          } else { // if there's > 1 continuing, OvervoteDecision.NONE is not a valid option
            decision = OvervoteDecision.IGNORE;
          }
        } else {
          decision = OvervoteDecision.NONE;
        }
      } else {
        decision = OvervoteDecision.NONE;
      }
    }

    return decision;
  }

  // purpose: determine if the input rankings specify a candidate who has not been eliminated
  // i.e. a continuing candidate
  // param: rankToCandidateIDs ordered map of rankings (most preferred to least) to candidateIDs
  // a set is used to accomodate overvotes
  // param: candidateToRoundEliminated map of candidateID to round in which they were eliminated
  // return: true if there is a continuing candidate otherwise false
  private boolean hasContinuingCandidates(
    SortedMap<Integer, Set<String>> rankToCandidateIDs,
    Map<String, Integer> candidateToRoundEliminated
  ) {
    // the result of this function
    boolean foundContinuingCandidate = false;
    // iterate through all candidateID sets
    for (Set<String> candidateIDSet : rankToCandidateIDs.values()) {
      // iterate through all candidateIDs in the set
      for (String candidateID : candidateIDSet) {
        // see if the candidate has been eliminated
        if (candidateToRoundEliminated.get(candidateID) == null) {
          foundContinuingCandidate = true;
          break;
        }
      }
      if (foundContinuingCandidate) {
        break;
      }
    }
    return foundContinuingCandidate;
  }

  // roundTally returns a map from candidate ID to vote tally for this round.
  // param: castVoteRecords contains all castVoteRecords for this election
  // param: candidateToRoundEliminated map of candidateID to round in which they are eliminated
  // param: the current round
  // return: map of candidateID to vote tallies for this round
  private Map<String, Integer> getRoundTally(
    List<CastVoteRecord> castVoteRecords,
    Map<String, Integer> candidateToRoundEliminated,
    int currentRound
  ) throws Exception {
    // map of candidateID to vote tally to store the results
    Map<String, Integer> roundTally = new HashMap<>();

    // initialize round tallies to 0 for all continuing candidates
    for (String candidateID : candidateIDs) {
      if (candidateToRoundEliminated.get(candidateID) == null) {
        roundTally.put(candidateID, 0);
      }
    }

    // loop over the ballots and count votes for continuing candidates
    for (CastVoteRecord cvr : castVoteRecords) {
      if (cvr.isExhausted()) {
        continue;
      }
      SortedMap<Integer, Set<String>> rankToCandidateIDs = cvr.sortedRankings();

      if (!hasContinuingCandidates(rankToCandidateIDs, candidateToRoundEliminated)) {
        cvr.exhaust(currentRound, "no continuing candidates");
        continue;
      }

      Integer lastRank = null;

      for (int rank : rankToCandidateIDs.keySet()) { // loop over the rankings within one ballot
        Set<String> candidateIDs = rankToCandidateIDs.get(rank);

        // handle possible overvote
        OvervoteDecision overvoteDecision = getOvervoteDecision(candidateIDs, candidateToRoundEliminated);
        if (overvoteDecision == OvervoteDecision.EXHAUST) {
          cvr.exhaust(currentRound, "overvote");
          break;
        } else if (overvoteDecision == OvervoteDecision.IGNORE) {
          String description = String.format("%d|ignored:%s|", currentRound, "overvote");
          cvr.addRoundDescription(description, currentRound);
          break;
        } else if (overvoteDecision == OvervoteDecision.SKIP_TO_NEXT_RANK) {
          continue;
        }

        // and possible undervote
        if (config.maxSkippedRanksAllowed() != null &&
            lastRank != null && rank - lastRank > config.maxSkippedRanksAllowed() + 1) {
          cvr.exhaust(currentRound, "undervote");
          break;
        }

        String selectedCandidateID = null;
        for (String candidateID : candidateIDs) {
          if (candidateToRoundEliminated.get(candidateID) == null) {
            if (selectedCandidateID != null) {
              throw new Exception(
                "We failed to handle an overvote with multiple continuing candidates properly."
              );
            } else {
              // found a continuing candidate, so increase their tally by 1
              selectedCandidateID = candidateID;
              String description = String.format("%d|%s|", currentRound, selectedCandidateID);
              cvr.addRoundDescription(description, currentRound);
              roundTally.put(selectedCandidateID, roundTally.get(selectedCandidateID) + 1);
            }
          }
        }

        if (selectedCandidateID != null) {
          break; // we've found our candidate already
        }
        lastRank = rank;
      } // end looping over the rankings within one ballot
    } // end looping over all ballots

    return roundTally;
  }


  private int numCandidates() {
    int num = candidateIDs.size();
    if (config.undeclaredWriteInLabel()!= null &&
      candidateIDs.contains(config.undeclaredWriteInLabel())) {
      num--;
    }
    return num;
  }

  public void doAudit(List<CastVoteRecord> castVoteRecords) {
    for(CastVoteRecord cvr : castVoteRecords) {
      log(cvr.getAuditString());
    }
  }

  // purpose: utility function to "invert" the input map of candidateID => tally
  // into a sorted map of tally => List<candidateID>
  // a list is used because multiple candidates may have the same (tying) tally
  // this is used to determine when a final winner is picked, and running tiebreak logic
  // param: roundTally
  //  input map of candidateID to tally for a particular round
  // param candidatesToInclude:
  //  input list of candidateIDs may appear in the output.
  //  This filters out candidates when running a tiebreak tabulation which relies
  //  on the tied candidate's previous round totals to break the tie
  // param: shouldLog
  //  if set output the candidate tally to console and audit
  // return: map of tally => List<candidateID> from the input data (excluding candidates
  //  not appearing in candidatesToInclude
  public static SortedMap<Integer, LinkedList<String>> buildTallyToCandidates(
    Map<String, Integer> roundTally,
    Set<String> candidatesToInclude,
    boolean shouldLog
  ) {
    // output map structure containing the map of vote tally to candidate(s)
    SortedMap<Integer, LinkedList<String>> tallyToCandidates = new TreeMap<>();
    // for each candidate record their vote total into the countToCandidates object
    // candidate is the current candidate as we iterate all candidates under consideration
    for (String candidate : candidatesToInclude) {
      // vote count for this candidate
      int votes = roundTally.get(candidate);
      if (shouldLog) {
        Logger.log("Candidate %s got %d votes.", candidate, votes);
      }
      // all candidates in the existing output structure (if any) who received the same vote tally
      LinkedList<String> candidates = tallyToCandidates.get(votes);
      if (candidates == null) {
        // new container list for candidates who recieved this vote tally
        candidates = new LinkedList<>();
        tallyToCandidates.put(votes, candidates);
      }
      candidates.add(candidate);
    }
    return tallyToCandidates;
  }
}
