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

  // OvervoteDecision is the result of applying an OvervoteRule to a CVR in a particular round
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
  // election config contains specific rules and file paths to be used during tabulation
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

    // function: BatchElimination constructor
    // purpose: create a new BatchElimination object simple container
    // param: candidateID the candidate eliminated
    // param: runningTotal how many total votes were totaled when this candidate was eliminated
    // param: nextHighestTally next highest count total
    // returns: the new object
    BatchElimination(String candidateID, int runningTotal, int nextHighestTally) {
      this.candidateID = candidateID;
      this.runningTotal = runningTotal;
      this.nextHighestTally = nextHighestTally;
    }
  }

  // map of round to TieBreak objects to record how tiebreaks were decided
  private Map<Integer, TieBreak> roundToTieBreak = new HashMap<>();

  // function: Tabulator constructor
  // purpose: assigns input params to member variables and caches the candidateIDlist
  // which will be used when reading input cast vote records
  // param: castVoteRecords list of all cast vote records to be tabulated for this contest
  // param: config describes various tabulation rules to be used for tabulation
  // returns: the new object
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
    log("There are %d candidates for this contest:", config.numCandidates());
    // string indexes over all candidate IDs to log them
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
        getTallyForRound(castVoteRecords, candidateToRoundEliminated, currentRound);
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

        // TODO: rewrite this with explicit logic
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
          // TODO: this should probably be an assert
          throw new Exception("Failed to eliminate any candidates.");
        }
        // store the losers
        for (String loser : eliminated) {
          candidateToRoundEliminated.put(loser, currentRound);
        }
      }
    }
  }

  // function: identifyWinner
  // purpose: determine if a winner has been identified in this round
  // param: currentRoundCandidateToTally map of candidateID to their tally in a particular round
  // param: currntRoundTallyToCandidates map of tally to candidate ID(s) for a particular round
  // return: winning candidateID or nil if no winner yet
  private String identifyWinner(
    Map<String, Integer> currentRoundCandidateToTally,
    SortedMap<Integer, LinkedList<String>> currentRoundTallyToCandidates
  ) {
    // store result here
    String selectedWinner = null;
    // currentRoundTotalVotes is total votes across all candidates in this round
    int currentRoundTotalVotes = 0;
    // numVotes indexes over all vote tallies in this round
    for (int numVotes : currentRoundCandidateToTally.values()) {
      currentRoundTotalVotes += numVotes;
    }
    log("Total votes in round %d: %d.", currentRound, currentRoundTotalVotes);

    // the highest vote total amongst all candidates
    int maxVotes = currentRoundTallyToCandidates.lastKey();
    // Does the leader have a majority of non-exhausted ballots? In other words, is maxVotes
    // greater than half of the total votes counted in this round?
    if (maxVotes > (float)currentRoundTotalVotes / 2.0) {
      // we have a winner
      // votes indexes over all tallies to find the winning one
      for (Integer votes : currentRoundTallyToCandidates.keySet()) {
        // record winner
        if (votes == maxVotes) {
          // set the winner
          selectedWinner = currentRoundTallyToCandidates.get(votes).getFirst();
          break;
        }
      }
      log("%s won in round %d with %d votes.", selectedWinner, currentRound, maxVotes);
    }
    return selectedWinner;
  }

  // function: dropUWI
  // purpose: eliminate all undeclared write in candidates
  // param: on exit eliminated will contain newly eliminated undeclared write in candidate IDs
  // param: currentRoundCandidateToTally map of candidate IDs to their tally for a given round
  // returns: true if any candidates were eliminated
  private boolean dropUWI(
    List<String> eliminated,
    Map<String, Integer> currentRoundCandidateToTally
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

  // function: dropCandidatesBelowThreshold
  // purpose: eliminate all candidates below a certain tally threshold
  // param: eliminated will contain any newly eliminated candidate IDs
  // param: currentRoundTallyToCandidates map of tally to candidate IDs for a given round
  // returns: true if any candidates were eliminated
  private boolean dropCandidatesBelowThreshold(
    List<String> eliminated,
    SortedMap<Integer, LinkedList<String>> currentRoundTallyToCandidates
  ) {
    if (
      eliminated.isEmpty() &&
      config.minimumVoteThreshold() != null &&
      currentRoundTallyToCandidates.firstKey() < config.minimumVoteThreshold()
    ) {
      // tally indexes over all tallies in the current round
      for (int tally : currentRoundTallyToCandidates.keySet()) {
        if (tally < config.minimumVoteThreshold()) {
          // candidate indexes over all candidates who received this tally
          for (String candidate : currentRoundTallyToCandidates.get(tally)) {
            eliminated.add(candidate);
            log(
              "Eliminated %s in round %d because they only had %d vote(s), below the minimum " +
                "threshold of %d.",
              candidate,
              currentRound,
              tally,
              config.minimumVoteThreshold()
            );
          }
        }
      }
    }
    return !eliminated.isEmpty();
  }

  // function: doBatchElimination
  // purpose: eliminate all candidates who are mathematically unable to win
  // param: eliminated will contain any newly eliminated candidate IDs
  // param: currentRoundTallyToCandidates map of tally to candidate IDs for a given round
  // returns: true if any candidates were eliminated
  private boolean doBatchElimination(
    List<String> eliminated,
    SortedMap<Integer, LinkedList<String>> currentRoundTallyToCandidates
  ) {
    if (eliminated.isEmpty() && config.batchElimination()) {
      // container for results
      List<BatchElimination> batchEliminations = runBatchElimination(currentRoundTallyToCandidates);
      if (batchEliminations.size() > 1) {
        // elimination iterates over all BatchElimination objects describing the eliminations
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

  // function: doRegularElimination
  // purpose: eliminate candidate with the lowest tally using tiebreak if necessary
  // param: eliminated will contain newly eliminated candidate ID
  // param: currentRoundTallyToCandidates map of tally to candidate IDs for a given round
  // returns: true if any candidates were eliminated
  private boolean doRegularElimination(
    List<String> eliminated,
    SortedMap<Integer, LinkedList<String>> currentRoundTallyToCandidates
  ) {
    if (eliminated.isEmpty()) {
      // eliminated candidate
      String eliminatedCandidate;
      // lowest tally in this round
      int minVotes = currentRoundTallyToCandidates.firstKey();
      // list of candidates receiving the lowest tally
      LinkedList<String> lastPlaceCandidates = currentRoundTallyToCandidates.get(minVotes);
      if (lastPlaceCandidates.size() > 1) {
        // there was a tie for last place
        // create new tieBreak object to pick a loser
        TieBreak tieBreak =
          new TieBreak(lastPlaceCandidates,
          config.tiebreakMode(),
          currentRound,
          minVotes,
          roundTallies);

        // results of tiebreak stored here
        eliminatedCandidate = tieBreak.loser;
        roundToTieBreak.put(currentRound, tieBreak);
        log(
          "%s lost a tie-breaker in round %d against %s. Each candidate had %d vote(s). %s",
          eliminatedCandidate,
          currentRound,
          tieBreak.nonLosingCandidateDescription(),
          minVotes,
          tieBreak.explanation
        );
      } else {
        // last place candidate will be eliminated
        eliminatedCandidate = lastPlaceCandidates.getFirst();
        log("%s was eliminated in round %d with %d vote(s).",
          eliminatedCandidate,
          currentRound,
          minVotes
        );
      }
      eliminated.add(eliminatedCandidate);
    }
    return !eliminated.isEmpty();
  }

  // function: generateSummarySpreadsheet
  // purpose: create a ResultsWriter object with the tabulation results data and use it
  //  to generate the results spreadsheet
  // param: outputFile path to write the output file to
  public void generateSummarySpreadsheet() {
    // writer object will create the output xls
    ResultsWriter writer = new ResultsWriter().
      setNumRounds(currentRound).
      setRoundTallies(roundTallies).
      setCandidatesToRoundEliminated(candidateToRoundEliminated).
      setWinner(winner).
      setElectionConfig(config);

    writer.generateSummarySpreadsheet();
  }

  // function: log
  // purpose: helper function for logging to console and audit
  // param: s input s to be logged
  // param: var1 ... objects to be formatted into the log output
  static void log(String s, Object... var1) {
    Logger.log(s, var1);
  }

  // Function: runBatchElimination
  // Purpose: applies batch elimination logic to the input vote counts to remove multiple candidates
  //   in a single round if their vote counts are so low that they could not possibly end up winning
  //   Consider, after each round of voting a candidate not eliminated could potentially receive ALL
  //   the votes from candidates who ARE eliminated, keeping them in the race and "leapfrogging"
  //   ahead of candidates who were leading them.
  //   In this algorithm we sum candidate vote totals (low to high) and find where this leapfrogging
  //   is impossible: that is, when the sum of all batch-eliminated candidates' votes fails to equal
  //   or exceed the next-lowest candidate vote total.
  //
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
        // candidate indexes over all seen candidates
        for (String candidate : candidatesSeen) {
          if (!candidatesEliminated.contains(candidate)) {
            candidatesEliminated.add(candidate);
            eliminations.add(new BatchElimination(candidate, runningTotal, currentVoteTally));
          }
        }
      }
      // Add the candidates for the currentVoteTally to the seen list and accumulate their votes.
      // currentCandidates is all candidates receiving the current vote tally
      List<String> currentCandidates = currentRoundTallyToCandidates.get(currentVoteTally);
      runningTotal += currentVoteTally * currentCandidates.size();
      candidatesSeen.addAll(currentCandidates);
    }
    return eliminations;
  }

  // purpose: determine if any overvote has occurred for this ranking set (from a cvr)
  // and if so return how to handle it based on the rules configuration in use
  // param: candidateIDSet all candidates this cvr contains at a particular rank
  // param: candidateIDToRoundEliminated map of candidate IDs to the round in which they were eliminated
  // return: an OvervoteDecision enum to be applied to the cvr under consideration
  private OvervoteDecision getOvervoteDecision(
    Set<String> candidateIDset,
    Map<String, Integer> candidateIDToRoundEliminated
  ) {
    // the resulting decision
    OvervoteDecision decision;
    // if undervote or one vote which is not the overvote label there is no overvote
    if (candidateIDset.size() == 0 ||
        (candidateIDset.size() == 1 && candidateIDset.toArray()[0] != explicitOvervoteLabel)) {
      decision = OvervoteDecision.NONE;
    } else if (config.overvoteRule() == OvervoteRule.EXHAUST_IMMEDIATELY) {
      decision = OvervoteDecision.EXHAUST;
    } else if (config.overvoteRule() == OvervoteRule.ALWAYS_SKIP_TO_NEXT_RANK) {
      decision = OvervoteDecision.SKIP_TO_NEXT_RANK;
    } else {
      // if we got here there is some overvote scenario:
      // multiple candidates in the set or explicitOvervoteLabel
      // TODO: get some clarity on the following scenarios
      // TODO: validate that explicitOvervoteLabel is handled correctly

      // build a list of all continuing candidates from the input set
      List<String> continuingAtThisRank = new LinkedList<>();
      // candidateID indexes over all candidate IDs in this ranking set
      for (String candidateID : candidateIDset) {
        if (candidateIDToRoundEliminated.get(candidateID) == null) {
          continuingAtThisRank.add(candidateID);
        }
      }

      if (continuingAtThisRank.size() > 0) {
        // at least 1 continuing candidate
        if (config.overvoteRule() == OvervoteRule.EXHAUST_IF_ANY_CONTINUING) {
          decision = OvervoteDecision.EXHAUST;
        } else if (config.overvoteRule() == OvervoteRule.IGNORE_IF_ANY_CONTINUING) {
          decision = OvervoteDecision.IGNORE;
        } else if (continuingAtThisRank.size() > 1) {
          // multiple continuing candidates at this rank
          if (config.overvoteRule() == OvervoteRule.EXHAUST_IF_MULTIPLE_CONTINUING) {
            decision = OvervoteDecision.EXHAUST;
          } else {
            // if there's > 1 continuing, OvervoteDecision.NONE is not a valid option
            decision = OvervoteDecision.IGNORE;
          }
        } else {
          // 1 continuing candidate at this rank
          decision = OvervoteDecision.NONE;
        }
      } else {
        // no continuing candidates at this rank
        decision = OvervoteDecision.NONE;
      }
    }

    return decision;
  }

  // function: hasContinuingCnadidates
  // purpose: determine if the input rankings specify a candidate who has not been eliminated
  //   i.e. a continuing candidate
  // param: rankToCandidateIDs ordered map of rankings (most preferred to least) to candidateIDs
  // a set is used to accommodate overvotes
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

  // function: getTallyForRound
  // purpose: return a map of candidate ID to vote tallies for this round
  //   generated based on previously eliminated candidateIDs contained in
  //   candidateToRoundEliminated object.
  //   After each call the candidateToRoundEliminated object will get
  //   more entries as candidateIDs are eliminated.
  //   Conversely the roundTally object returned here will contain fewer entries each
  //   of which will have more votes.
  // param: castVoteRecords contains all castVoteRecords for this election
  // param: candidateToRoundEliminated map of candidateID to round in which they are eliminated
  // param: cvrIndexToRoundExhausted map of ballot index to round in which it was exhausted
  // param: the current round
  // throws: Exception if we failed to process the overvote correctly
  // return: map of candidateID to vote tallies for this round
  private Map<String, Integer> getTallyForRound(
    List<CastVoteRecord> castVoteRecords,
    Map<String, Integer> candidateToRoundEliminated,
    int currentRound
  ) throws Exception {
    // map of candidateID to vote tally to store the results
    Map<String, Integer> roundTally = new HashMap<>();

    // initialize round tallies to 0 for all continuing candidates
    // candidateID indexes through all candidateIDs
    for (String candidateID : candidateIDs) {
      if (candidateToRoundEliminated.get(candidateID) == null) {
        roundTally.put(candidateID, 0);
      }
    }

    // cvr indexes over the cast vote records to count votes for continuing candidateIDs
    for (CastVoteRecord cvr : castVoteRecords) {
      if (cvr.isExhausted()) {
        continue;
      }
      // if this cvr has no continuing candidate exhaust it
      if (!hasContinuingCandidates(cvr.rankToCandidateIDs, candidateToRoundEliminated)) {
        cvr.exhaust(currentRound, "no continuing candidates");
        continue;
      }

      // lastRank tracks the last rank in this rankings set as we iterate through it
      // this is used to determine how many skipped rankings occurred in the case of
      // undervotes
      Integer lastRank = null;
      // loop over all rankings in this cvr from most preferred to least and see how they will
      // be rendered
      // rank iterates through all ranks in this cvr ranking set
      for (int rank : cvr.rankToCandidateIDs.keySet()) {
        // candidateIDSet is all candidates selected at the current rank
        Set<String> candidateIDSet = cvr.rankToCandidateIDs.get(rank);

        // check for an overvote
        // overvoteDecision is the overvote decision for this ranking
        OvervoteDecision overvoteDecision = getOvervoteDecision(candidateIDSet, candidateToRoundEliminated);
        if (overvoteDecision == OvervoteDecision.EXHAUST) {
          cvr.exhaust(currentRound, "overvote");
          break;
        } else if (overvoteDecision == OvervoteDecision.IGNORE) {
          // description of the overvote decision
          String description = String.format("%d|ignored:%s|", currentRound, "overvote");
          cvr.addRoundDescription(description, currentRound);
          break;
        } else if (overvoteDecision == OvervoteDecision.SKIP_TO_NEXT_RANK) {
          continue;
        }

        // check for an undervote
        if ((config.maxSkippedRanksAllowed() != null) &&
            (lastRank != null) &&
            (rank - lastRank > config.maxSkippedRanksAllowed() + 1))
        {
          cvr.exhaust(currentRound, "undervote");
          break;
        }

        // selectedCandidateID for this rank
        String selectedCandidateID = null;
        // candidateID indexes through all candidates selected at this rank
        for (String candidateID : candidateIDSet) {
          // skip eliminated candidates
          if (candidateToRoundEliminated.get(candidateID) == null) {
            // if this fails we failed to handle an overvote with multiple continuing candidates
            assert(selectedCandidateID != null);
            // we found a continuing candidate, so increase their tally by 1
            selectedCandidateID = candidateID;
            // text description of the vote
            String description = String.format("%d|%s|", currentRound, selectedCandidateID);
            cvr.addRoundDescription(description, currentRound);
            roundTally.put(selectedCandidateID, roundTally.get(selectedCandidateID) + 1);
          }
        }

        if (selectedCandidateID != null) {
          // we've found our candidate
          break;
        }
        lastRank = rank;
      } // end looping over the rankings within one ballot
    } // end looping over all ballots

    return roundTally;
  }

  // function: doAudit
  // purpose: log the audit info to console and audit file
  // param: castVoteRecords list of all cvrs which have been tabulated
  public void doAudit(List<CastVoteRecord> castVoteRecords) {
    for(CastVoteRecord cvr : castVoteRecords) {
      log(cvr.getAuditString());
    }
  }

  // function: buildTallyToCandidates
  // purpose: utility function to "invert" the input map of candidateID to tally
  //   into a sorted map of tally to List of candidateIDs.
  //   A list is used because multiple candidates may have the same tally.
  //   This is used to determine when a final winner is picked and for running tiebreak logic.
  // param: roundTally input map of candidateID to tally for a particular round
  // param candidatesToInclude: list of candidateIDs which may be included in the output.
  //   This filters out candidates when running a tiebreak tabulation which relies
  //   on the tied candidate's previous round totals to break the tie.
  // param: shouldLog is set to log to console and log file
  // return: sorted map of tally to List of candidateIDs drawn from the input data and excluding
  //   candidates not appearing in candidatesToInclude)
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
