/**
 * Created by Jonathan Moldover on 7/8/17
 * Copyright 2018 Bright Spots
 * Purpose: perform ranked choice tabulation calculations
 * round-by-round tabulation of votes to each candidate
 * handles overvote / undervote decisions and tiebreaks
 * results are logged to console and audit file
 * Version: 1.0
 */

package com.rcv;

import java.util.*;

public class Tabulator {

  // When the CVR contains an overvote we "normalize" it to use this string
  // TODO: find a better place for this
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

  // input cast vote records parsed from cvr files
  private List<CastVoteRecord> castVoteRecords;
  // all  candidateIDs for this election prased from the election config (does not include UWIs)
  private List<String> candidateIDs;
  // election config contains specific rules and input cvr file paths to be used when tabulating this election
  private ElectionConfig config;
  // roundTallies is a map of round number to maps of candidate IDs to vote totals for that round
  // e.g. roundTallies[1] contains a map of all candidate ID -> votes received by that candidate in round 1
  // this structure is created during the course of tabulation
  private Map<Integer, Map<String, Integer>> roundTallies = new HashMap<Integer, Map<String, Integer>>();

  // eliminatedRound is a map of candidate IDs to the round in which they were eliminated
  private Map<String, Integer> eliminatedRound = new HashMap<String, Integer>();

  // the winning candidateID
  private String winner;

  // when tabulation is complete this will be how many rounds did it take to determine a winner
  private int currentRound = 0;

  // simple container class used during batch elimination process to store the results
  static class BatchElimination {
    // the candidate eliminated
    String candidateID;
    // how many votes
    int runningTotal;
    int nextHighestCount;

    BatchElimination(String candidateID, int runningTotal, int nextHighestCount) {
      this.candidateID = candidateID;
      this.runningTotal = runningTotal;
      this.nextHighestCount = nextHighestCount;
    }
  }

  private Map<Integer, TieBreak> tieBreaks = new HashMap<>();

  Tabulator(
      List<CastVoteRecord> castVoteRecords,
      ElectionConfig config
  ) {
    this.castVoteRecords = castVoteRecords;
    this.candidateIDs = config.getCandidateCodeList();
    this.config = config;
  }

  // purpose: run the main tabulation routine
  public void tabulate() throws Exception {

    log("Beginning tabulation for contest.");
    log("There are %d candidateIDs for this contest:", numCandidates());
    for (String option : candidateIDs) {
      log("%s", option);
    }
    log("There are %d cast vote records for this contest.", castVoteRecords.size());

    // add UWI string to contest options so it will be tallied similarly to other candidateIDs
    if (config.undeclaredWriteInLabel() != null) {
      this.candidateIDs.add(config.undeclaredWriteInLabel());
    }

    // exhaustedBallots is a map of ballot indexes to the round in which they were exhausted
    Map<Integer, Integer> exhaustedBallots = new HashMap<>();

    // loop until we achieve a majority winner: at each iteration we will eliminate one or more candidates and
    // gradually transfer votes to the remaining candidates
    while (winner == null) {
      currentRound++;
      log("Round: %d", currentRound);

      // roundTally is map of candidateID to vote tally for the current round.
      // It is generated based on previously eliminated candidateIDs (contained in eliminatedRound object).
      // At each iteration of this loop the eliminatedRound object will gain entries as candidateIDs are eliminated.
      // Conversely the roundTally object returned here will contain fewer entries, each of which will have more votes.
      // Eventually a winner will be chosen.
      Map<String, Integer> currentRoundCandidateToTally = getRoundTally(castVoteRecords, eliminatedRound, exhaustedBallots, currentRound);
      roundTallies.put(currentRound, currentRoundCandidateToTally);

      // Map of vote tally to candidate(s). A list is used to handle ties.
      SortedMap<Integer, LinkedList<String>> currentRoundTallyToCandidates = buildTallyToCandidates(
        currentRoundCandidateToTally,
        currentRoundCandidateToTally.keySet(),
        true
      );

      winner = identifyWinner(currentRoundCandidateToTally, currentRoundTallyToCandidates);

      if (winner == null) {
        // container for eliminated candidate(s)
        List<String> eliminated = new LinkedList<>();

        // Four mutually exclusive ways to eliminate candidateIDs.
        boolean foundCandidateToEliminate =
          // 1. Some races contain undeclared write-ins that should be dropped immediately.
          dropUWI(eliminated, currentRoundCandidateToTally) ||
          // 2. If there's a minimum vote threshold, drop all candidateIDs failing to meet that threshold.
          dropCandidatesBelowThreshold(eliminated, currentRoundTallyToCandidates) ||
          // 3. Otherwise, try batch elimination.
          doBatchElimination(eliminated, currentRoundTallyToCandidates) ||
          // 4. And if we didn't do batch elimination, eliminate last place now, breaking a tie if necessary.
          doRegularElimination(eliminated, currentRoundTallyToCandidates);

        if (!foundCandidateToEliminate) {
          throw new Exception("Failed to eliminate any candidates.");
        }

        for (String loser : eliminated) {
          eliminatedRound.put(loser, currentRound);
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
    // Does the leader have a majority of non-exhausted ballots?
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

  private boolean dropUWI(List<String> eliminated, Map<String, Integer> currentRoundCandidateToTally) {
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
              "Eliminated %s in round %d because they only had %d vote(s), below the minimum threshold of %d.",
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
            elimination.nextHighestCount
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
        TieBreak tieBreak = new TieBreak(lastPlace, config.tiebreakMode(), currentRound, minVotes, roundTallies);
        loser = tieBreak.getSelection();
        tieBreaks.put(currentRound, tieBreak);
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
      setCandidatesToRoundEliminated(eliminatedRound).
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

  // function: runBatchElimination
  // purpose: applies batch elimination logic to the input vote counts to remove multiple candidates in
  // a single round if their vote counts are so low they could not possibly advance.
  // Consider, after each round of voting a candidate not eliminated could potentially receive ALL the votes
  // from candidates who ARE eliminated, keeping them in the race and "leapfrogging"
  // ahead of candidates who were leading them.
  //
  // In this algorithm we sum candidate vote totals (low to high) and find where this leapfrogging is impossible,
  // that is, when the sum of all batch-eliminated candidate's votes cannot equal or exceed the next-highest candidate
  // vote total.
  // param: currentRoundTallyToCandidates map of
  // returns: list of BatchElimination objects, one for each batch eliminated candidate
  private List<BatchElimination> runBatchElimination(
    SortedMap<Integer, LinkedList<String>> currentRoundTallyToCandidates
  ) {
    // the sum total of all vote counts examined.  this must equal or exceed the next-highest vote total
    // to stop the batch elimination.
    int runningTotal = 0;
    // tracks candidates whose totals have been included in the runningTotal and thus are being considered
    // for batch elimination
    List<String> candidatesSeen = new LinkedList<>();
    // tracks candidates who have been batch eliminated (to prevent them from being eliminated twice)
    Set<String> candidatesEliminated = new HashSet<>();
    // BatchElimination objects contain resulting data which will be used by the tabulation to process
    // the batch elimination results
    List<BatchElimination> eliminations = new LinkedList<>();
    // at each iteration currentVoteTally is the next highest vote count received for any candidate(s)
    for (int currentVoteTally : currentRoundTallyToCandidates.keySet()) {
      // see if leapfrogging is possible
      if (runningTotal < currentVoteTally) {
        // not possible so eliminate everyone who has been seen and not eliminated yet
        for (String candidate : candidatesSeen) {
          if (!candidatesEliminated.contains(candidate)) {
            candidatesEliminated.add(candidate);
            eliminations.add(new BatchElimination(candidate, runningTotal, currentVoteTally));
          }
        }
      }
      // add the candidates for the currentVoteTally to the seen list and accumulate their votes
      // currentCandidates contains all candidates who received the next highest vote total
      List<String> currentCandidates = currentRoundTallyToCandidates.get(currentVoteTally);
      runningTotal += currentVoteTally * currentCandidates.size();
      candidatesSeen.addAll(currentCandidates);
    }
    return eliminations;
  }

  private boolean exhaustBallot(
    int ballotIndex,
    int round,
    Map<Integer, Integer> exhaustedBallots,
    String reason,
    CastVoteRecord cvr
  ) {
    String description = String.format("%d|exhausted:%s|", round, reason);
    cvr.addRoundDescription(description, round);
    exhaustedBallots.put(ballotIndex, round);
    return true;
  }

  private OvervoteDecision getOvervoteDecision(
    Set<String> contestOptionIds,
    Map<String, Integer> eliminatedRound
  ) {
    OvervoteDecision decision;

    if (contestOptionIds.size() == 0 ||
        (contestOptionIds.size() == 1 && contestOptionIds.toArray()[0] != explicitOvervoteLabel)) {
      decision = OvervoteDecision.NONE;
    } else if (config.overvoteRule() == OvervoteRule.EXHAUST_IMMEDIATELY) {
      decision = OvervoteDecision.EXHAUST;
    } else if (config.overvoteRule() == OvervoteRule.ALWAYS_SKIP_TO_NEXT_RANK) {
      decision = OvervoteDecision.SKIP_TO_NEXT_RANK;
    } else {
      List<String> continuingAtThisRank = new LinkedList<String>();
      for (String optionId : contestOptionIds) {
        if (eliminatedRound.get(optionId) == null) {
          continuingAtThisRank.add(optionId);
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

  private boolean hasContinuingCandidates(
    SortedMap<Integer, Set<String>> rankings,
    Map<String, Integer> eliminatedRound
  ) {
    boolean found = false;
    for (Set<String> optionIds : rankings.values()) {
      for (String optionId : optionIds) {
        if (eliminatedRound.get(optionId) == null) {
          found = true;
          break;
        }
      }
      if (found) {
        break;
      }
    }

    return found;
  }

  // roundTally returns a map of candidate ID to vote tallies
  // generated based on previously eliminated candidateIDs contained in eliminatedRound object
  // at each iteration of this loop, the eliminatedRound object will get more entries as candidateIDs are eliminated
  // conversely the roundTally object returned here will contain fewer entries each of which will have more votes
  // eventually a winner will be chosen
  private Map<String, Integer> getRoundTally(
    List<CastVoteRecord> castVoteRecords,
    Map<String, Integer> eliminatedRound,
    Map<Integer, Integer> exhaustedBallots,
    int round
  ) throws Exception {
    Map<String, Integer> roundTally = new HashMap<>();

    // initialize round tallies for non-eliminated candidateIDs
    for (String contestOptionId : candidateIDs) {
      if (eliminatedRound.get(contestOptionId) == null) {
        roundTally.put(contestOptionId, 0);
      }
    }

    // loop over the ballots and count votes for continuing candidateIDs
    for (int i = 0; i < castVoteRecords.size(); i++) {
      if (exhaustedBallots.get(i) != null) {
        continue;
      }
      CastVoteRecord cvr = castVoteRecords.get(i);
      SortedMap<Integer, Set<String>> rankings = cvr.sortedRankings();

      if (!hasContinuingCandidates(rankings, eliminatedRound)) {
        exhaustBallot(i, round, exhaustedBallots, "no continuing candidateIDs", cvr);
        continue;
      }

      Integer lastRank = null;

      for (int rank : rankings.keySet()) { // loop over the rankings within one ballot
        Set<String> contestOptionIds = rankings.get(rank);

        // handle possible overvote
        OvervoteDecision overvoteDecision = getOvervoteDecision(contestOptionIds, eliminatedRound);
        if (overvoteDecision == OvervoteDecision.EXHAUST) {
          exhaustBallot(i, round, exhaustedBallots, "overvote", cvr);
          break;
        } else if (overvoteDecision == OvervoteDecision.IGNORE) {
          String description = String.format("%d|ignored:%s|", round, "overvote");
          cvr.addRoundDescription(description, round);
          break;
        } else if (overvoteDecision == OvervoteDecision.SKIP_TO_NEXT_RANK) {
          continue;
        }

        // and possible undervote
        if (config.maxSkippedRanksAllowed() != null &&
            lastRank != null && rank - lastRank > config.maxSkippedRanksAllowed() + 1) {
          exhaustBallot(i, round, exhaustedBallots, "undervote", cvr);
          break;
        }

        String selectedOptionId = null;
        for (String optionId : contestOptionIds) {
          if (eliminatedRound.get(optionId) == null) {
            if (selectedOptionId != null) {
              throw new Exception(
                "Our code failed to handle an overvote with multiple continuing candidateIDs properly."
              );
            } else {
              // found a continuing candidate, so increase their tally by 1
              selectedOptionId = optionId;
              String description = String.format("%d|%s|", round, selectedOptionId);
              cvr.addRoundDescription(description, round);
              roundTally.put(selectedOptionId, roundTally.get(selectedOptionId) + 1);
            }
          }
        }

        if (selectedOptionId != null) {
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
