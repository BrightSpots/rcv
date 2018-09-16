/*
 * Ranked Choice Voting Universal Tabulator
 * Copyright (c) 2018 Jonathan Moldover, Louis Eisenberg, and Hylton Edingfield
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See
 * the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this
 * program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Purpose:
 * Perform ranked choice tabulation calculations. Round-by-round tabulation of votes to
 * each candidate. Handles overvote / undervote decisions batch elimination and tiebreaks. Results
 * are logged to console and audit file.
 */

package com.rcv;

import com.rcv.CastVoteRecord.VoteOutcomeType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;

class Tabulator {

  // When the CVR contains an overvote we "normalize" it to use this string
  static final String EXPLICIT_OVERVOTE_LABEL = "overvote";
  // cast vote records parsed from CVR input files
  private final List<CastVoteRecord> castVoteRecords;
  // all candidateIDs for this contest parsed from the contest config
  private final Set<String> candidateIDs;
  // contest config contains specific rules and file paths to be used during tabulation
  private final ContestConfig config;
  // roundTallies is a map from round number to a map from candidate ID to vote total for the round
  // e.g. roundTallies[1] contains a map of all candidate ID -> votes for each candidate in round 1
  // this structure is computed over the course of tabulation
  private final Map<Integer, Map<String, BigDecimal>> roundTallies = new HashMap<>();
  // precinctRoundTallies is a map from precinct to roundTallies for that precinct
  private final Map<String, Map<Integer, Map<String, BigDecimal>>> precinctRoundTallies =
      new HashMap<>();
  // candidateToRoundEliminated is a map from candidate ID to round in which they were eliminated
  private final Map<String, Integer> candidateToRoundEliminated = new HashMap<>();
  // map from candidate ID to the round in which they won
  private final Map<String, Integer> winnerToRound = new HashMap<>();
  // tracks the current round (and when tabulation is complete, the total number of rounds)
  private int currentRound = 0;
  // tracks required winning threshold
  private BigDecimal winningThreshold;

  // function: Tabulator constructor
  // purpose: assigns input params to member variables and caches the candidateID list
  // which will be used when reading input cast vote records
  // param: castVoteRecords list of all cast vote records to be tabulated for this contest
  // param: config describes various tabulation rules to be used for tabulation
  // returns: the new object
  Tabulator(List<CastVoteRecord> castVoteRecords, ContestConfig config) {
    this.castVoteRecords = castVoteRecords;
    this.candidateIDs = config.getCandidateCodeList();
    this.config = config;
    if (config.isTabulateByPrecinctEnabled()) {
      initPrecinctRoundTallies();
    }
  }

  // function: buildTallyToCandidates
  // purpose: utility function to "invert" the input map of candidateID to tally
  //   into a sorted map of tally to List of candidateIDs.
  //   A list is used because multiple candidates may have the same tally.
  //   This is used to determine when winners are selected and for running tiebreak logic.
  // param: roundTally input map of candidateID to tally for a particular round
  // param candidatesToInclude: list of candidateIDs which may be included in the output.
  //   This filters out candidates when running a tiebreak tabulation which relies
  //   on the tied candidate's previous round totals to break the tie.
  // param: shouldLog is set to log to console and log file
  // return: sorted map of tally to List of candidateIDs drawn from the input data and excluding
  //   candidates not appearing in candidatesToInclude)
  static SortedMap<BigDecimal, LinkedList<String>> buildTallyToCandidates(
      Map<String, BigDecimal> roundTally, Set<String> candidatesToInclude, boolean shouldLog) {
    // output map structure containing the map of vote tally to candidate(s)
    SortedMap<BigDecimal, LinkedList<String>> tallyToCandidates = new TreeMap<>();
    // for each candidate record their vote total into the countToCandidates object
    // candidate is the current candidate as we iterate all candidates under consideration
    for (String candidate : candidatesToInclude) {
      // vote count for this candidate
      BigDecimal votes = roundTally.get(candidate);
      if (shouldLog) {
        Logger.log(Level.INFO, "Candidate %s got %s votes.", candidate, votes.toString());
      }
      // all candidates in the existing output structure (if any) who received the same vote tally
      LinkedList<String> candidates =
          tallyToCandidates.computeIfAbsent(votes, k -> new LinkedList<>());
      // new container list for candidates who received this vote tally
      candidates.add(candidate);
    }
    return tallyToCandidates;
  }

  // function: tabulate
  // purpose: run the main tabulation routine to determine contest results
  //  this is the high-level control of the tabulation algorithm
  void tabulate() {
    logSummaryInfo();
    Logger.log(Level.INFO, "Starting tabulation...");

    // Loop until we've found our winner(s) unless using continueUntilTwoCandidatesRemain, in which
    // case we loop until only two candidates remain.
    // At each iteration, we'll either a) identify one or more
    // winners and transfer their votes to the remaining candidates (if we still need to find more
    // winners), or b) eliminate one or more candidates and gradually transfer votes to the
    // remaining candidates.
    while (shouldContinueTabulating()) {
      currentRound++;
      Logger.log(Level.INFO, "Round: %d", currentRound);

      // currentRoundCandidateToTally is a map from candidateID to vote tally for the current round.
      // At each iteration of this loop that involves eliminating candidates, the eliminatedRound
      // object will gain entries.
      // Conversely, the currentRoundCandidateToTally object returned here will contain fewer
      // entries, each of which will have as many or more votes than they did in prior rounds.
      // Eventually the winner(s) will be chosen.
      Map<String, BigDecimal> currentRoundCandidateToTally = getTallyForRound(currentRound);
      roundTallies.put(currentRound, currentRoundCandidateToTally);

      // The winning threshold in a multi-seat contest is based on the number of active votes in the
      // first round.
      // In a single-seat contest, it's based on the number of active votes in the current round.
      if (currentRound == 1 || config.getNumberOfWinners() == 1) {
        setWinningThreshold(currentRoundCandidateToTally);
      }

      // currentRoundTallyToCandidates is a sorted map from tally to candidate(s) with that tally.
      SortedMap<BigDecimal, LinkedList<String>> currentRoundTallyToCandidates =
          buildTallyToCandidates(
              currentRoundCandidateToTally, currentRoundCandidateToTally.keySet(), true);
      // see if a winner is determined in this iteration
      List<String> winners =
          identifyWinners(currentRoundCandidateToTally, currentRoundTallyToCandidates);

      if (winners.size() > 0) {
        for (String winner : winners) {
          winnerToRound.put(winner, currentRound);
        }
        for (String winner : winners) {
          // are there still more winners to select in future rounds?
          if (winnerToRound.size() < config.getNumberOfWinners()) {
            // number of votes the candidate got this round
            BigDecimal candidateVotes = currentRoundCandidateToTally.get(winner);
            // number that were surplus (beyond the required threshold)
            BigDecimal extraVotes = candidateVotes.subtract(winningThreshold);
            // fractional transfer percentage
            BigDecimal surplusFraction = config.divide(extraVotes, candidateVotes);
            for (CastVoteRecord cvr : castVoteRecords) {
              if (winner.equals(cvr.getCurrentRecipientOfVote())) {
                cvr.recordCurrentRecipientAsWinner(surplusFraction);
              }
            }
          }
        }
      } else { // if no winners in this round, determine who will be eliminated
        // container for eliminated candidate(s)
        List<String> eliminated;

        // Four mutually exclusive ways to eliminate candidates.

        // 1. Some races contain undeclared write-ins that should be dropped immediately.
        eliminated = dropUndeclaredWriteIns(currentRoundCandidateToTally);
        // 2. If there's a minimum vote threshold, drop all candidates below that threshold.
        if (eliminated.isEmpty()) {
          eliminated = dropCandidatesBelowThreshold(currentRoundTallyToCandidates);
        }
        // 3. Otherwise, try batch elimination.
        if (eliminated.isEmpty()) {
          eliminated = doBatchElimination(currentRoundTallyToCandidates);
        }
        // 4. If we didn't do batch elimination, eliminate the remaining candidate with the lowest
        //    tally, breaking a tie if needed.
        if (eliminated.isEmpty()) {
          eliminated = doRegularElimination(currentRoundTallyToCandidates);
        }

        // If we failed to eliminate anyone, there's a bug in the code.
        assert !eliminated.isEmpty();

        // store the losers
        for (String loser : eliminated) {
          candidateToRoundEliminated.put(loser, currentRound);
        }
      }

      updatePastWinnerTallies();
    }

    Logger.log(Level.INFO, "Tabulation completed.");
  }

  // function: logSummaryInfo
  // purpose: log some basic info about the contest before starting tabulation
  private void logSummaryInfo() {
    Logger.log(
        Level.INFO,
        "There are %d declared candidates for this contest:",
        config.getNumDeclaredCandidates());
    // candidateID indexes over all candidate IDs to log them
    for (String candidateID : candidateIDs) {
      Logger.log(Level.INFO, "%s", candidateID);
    }

    if (config.getTiebreakMode() == TieBreakMode.GENERATE_PERMUTATION) {
      Logger.log(Level.INFO, "Randomly generated candidate permutation for tie-breaking:");
      // candidateID indexes over all candidates in ordered list
      for (String candidateID : config.getCandidatePermutation()) {
        Logger.log(Level.INFO, "%s", candidateID);
      }
    }
  }

  // function: updateWinnerTallies
  // purpose: Update the tally for the just-completed round to reflect the tallies for candidates
  // who won in a past round (in a multi-winner contest). We do this because the regular tally
  // logic only considers continuing candidates, so it won't assign any votes to past winners -- but
  // in reality they continue to hold their winning margins for the rest of the rounds, so we need
  // to fill in those values here.
  // TODO: instead of recomputing these winning tallies each round, we could just read the values
  // from the previous round. Once someone wins and we redistribute their extra votes, their tally
  // should be frozen for the rest of the rounds.
  private void updatePastWinnerTallies() {
    // pastWinners contains winners from rounds that preceded the current round
    Set<String> pastWinners = new HashSet<>();
    for (String winner : winnerToRound.keySet()) {
      // skip someone who won in the current round, because we already have that tally filled in
      if (winnerToRound.get(winner) < currentRound) {
        pastWinners.add(winner);
      }
    }

    // initialize main tally
    // roundTally is the main tally
    Map<String, BigDecimal> roundTally = roundTallies.get(currentRound);
    for (String pastWinner : pastWinners) {
      roundTally.put(pastWinner, BigDecimal.ZERO);
    }

    // initialize precinct tallies
    if (config.isTabulateByPrecinctEnabled()) {
      for (String precinct : precinctRoundTallies.keySet()) {
        // this is all the tallies for the given precinct
        Map<Integer, Map<String, BigDecimal>> roundTalliesForPrecinct =
            precinctRoundTallies.get(precinct);
        // and this is the tally for the current round for the precinct
        Map<String, BigDecimal> roundTallyForPrecinct = roundTalliesForPrecinct.get(currentRound);
        for (String pastWinner : pastWinners) {
          roundTallyForPrecinct.put(pastWinner, BigDecimal.ZERO);
        }
      }
    }

    // process all the CVRs
    for (CastVoteRecord cvr : castVoteRecords) {
      // the record of winners who got partial votes from this CVR
      Map<String, BigDecimal> winnerToFractionalValue = cvr.getWinnerToFractionalValue();
      for (String winner : winnerToFractionalValue.keySet()) {
        if (!pastWinners.contains(winner)) {
          continue; // this is someone who just won this round, so we can skip them
        }
        // the fractional value we should use when incrementing
        BigDecimal fractionalTransferValue = winnerToFractionalValue.get(winner);
        incrementTally(roundTally, fractionalTransferValue, winner);
        for (String precinct : precinctRoundTallies.keySet()) {
          // all the tallies for the given precinct
          Map<Integer, Map<String, BigDecimal>> roundTalliesForPrecinct =
              precinctRoundTallies.get(precinct);
          // the tally for the current round for the precinct
          Map<String, BigDecimal> roundTallyForPrecinct = roundTalliesForPrecinct.get(currentRound);
          incrementTally(roundTallyForPrecinct, fractionalTransferValue, winner);
        }
      }
    }
  }

  // function: setWinningThreshold
  // purpose: determine and store the threshold to win
  // param: currentRoundCandidateToTally map of candidateID to their tally for a particular round
  private void setWinningThreshold(Map<String, BigDecimal> currentRoundCandidateToTally) {
    // currentRoundTotalVotes holds total active votes in this round
    BigDecimal currentRoundTotalVotes = BigDecimal.ZERO;
    // numVotes indexes over all vote tallies in this round
    for (BigDecimal numVotes : currentRoundCandidateToTally.values()) {
      currentRoundTotalVotes = currentRoundTotalVotes.add(numVotes);
    }

    // divisor for threshold is num winners + 1
    BigDecimal divisor = new BigDecimal(config.getNumberOfWinners() + 1);
    // threshold = floor(votes / (num_winners + 1)) + 1
    winningThreshold =
        currentRoundTotalVotes.divide(divisor, RoundingMode.DOWN).add(BigDecimal.ONE);
  }

  // purpose: determine if we should continue tabulating based on how many winners have been
  // selected and if continueUntilTwoCandidatesRemain flag is in use.
  // return: true if we should continue tabulating
  private boolean shouldContinueTabulating() {
    // how many candidates have already been eliminated
    int numEliminatedCandidates = candidateToRoundEliminated.keySet().size();
    // how many winners have been selected
    int numWinners = winnerToRound.size();
    // apply config setting if specified
    if (config.willContinueUntilTwoCandidatesRemain()) {
      return numEliminatedCandidates + numWinners + 1 < config.getNumCandidates();
    } else {
      return numWinners < config.getNumberOfWinners();
    }
  }

  // function: isCandidateContinuing
  // purpose: returns true if candidate is continuing with respect to tabulation
  // this handles continued tabulation after a winner has been chosen for the
  // continueUntilTwoCandidatesRemain setting
  // returns: true if we should continue tabulating
  private boolean isCandidateContinuing(String candidate) {
    CandidateStatus status = getCandidateStatus(candidate);
    return status == CandidateStatus.CONTINUING
        || (status == CandidateStatus.WINNER && config.willContinueUntilTwoCandidatesRemain());
  }

  // function: getCandidateStatus
  // purpose: returns candidate status (continuing, eliminated or winner)
  // returns: candidate status
  private CandidateStatus getCandidateStatus(String candidate) {
    CandidateStatus status = CandidateStatus.CONTINUING;
    if (winnerToRound.containsKey(candidate)) {
      status = CandidateStatus.WINNER;
    } else if (candidateToRoundEliminated.containsKey(candidate)) {
      status = CandidateStatus.ELIMINATED;
    } else if (candidate.equals(config.getOvervoteLabel())) {
      status = CandidateStatus.INVALID;
    }
    return status;
  }

  // function: identifyWinners
  // purpose: determine if one or more winners have been identified in this round
  // param: currentRoundCandidateToTally map of candidateID to their tally in a particular round
  // param: currentRoundTallyToCandidates map of tally to candidate ID(s) for a particular round
  // return: list of winning candidates in this round (if any)
  private List<String> identifyWinners(
      Map<String, BigDecimal> currentRoundCandidateToTally,
      SortedMap<BigDecimal, LinkedList<String>> currentRoundTallyToCandidates) {
    // store result here
    List<String> selectedWinners = new LinkedList<>();

    // If the number of continuing candidates equals the number of seats to fill, everyone wins.
    if (currentRoundCandidateToTally.size() == config.getNumberOfWinners() - winnerToRound.size()) {
      selectedWinners.addAll(currentRoundCandidateToTally.keySet());
    } else { // see if anyone has exceeded the threshold
      // tally indexes over all tallies to find any winners
      for (BigDecimal tally : currentRoundTallyToCandidates.keySet()) {
        if (tally.compareTo(winningThreshold) > 0) {
          // we have winner(s)
          List<String> winningCandidates = currentRoundTallyToCandidates.get(tally);
          selectedWinners.addAll(winningCandidates);
        }
      }
    }

    for (String winner : selectedWinners) {
      Logger.log(
          Level.INFO,
          "%s won in round %d with %s votes.",
          winner,
          currentRound,
          currentRoundCandidateToTally.get(winner).toString());
    }

    return selectedWinners;
  }

  // function: dropUndeclaredWriteIns
  // purpose: eliminate all undeclared write in candidates
  // param: currentRoundCandidateToTally map of candidate IDs to their tally for a given round
  // returns: eliminated candidates
  private List<String> dropUndeclaredWriteIns(
      Map<String, BigDecimal> currentRoundCandidateToTally) {
    List<String> eliminated = new LinkedList<>();
    // undeclared label
    String label = config.getUndeclaredWriteInLabel();
    if (currentRound == 1
        && label != null
        && !label.isEmpty()
        && candidateIDs.contains(label)
        && currentRoundCandidateToTally.get(label).signum() == 1) {
      eliminated.add(label);
      Logger.log(
          Level.INFO,
          "Eliminated %s in round %d because it represents undeclared write-ins. It had "
              + "%s votes.",
          label,
          currentRound,
          currentRoundCandidateToTally.get(label).toString());
    }
    return eliminated;
  }

  // function: dropCandidatesBelowThreshold
  // purpose: eliminate all candidates below a certain tally threshold
  // param: currentRoundTallyToCandidates map of tally to candidate IDs for a given round
  // returns: eliminated candidates
  private List<String> dropCandidatesBelowThreshold(
      SortedMap<BigDecimal, LinkedList<String>> currentRoundTallyToCandidates) {
    List<String> eliminated = new LinkedList<>();
    // min threshold
    BigDecimal threshold = config.getMinimumVoteThreshold();
    if (threshold.signum() == 1
        && currentRoundTallyToCandidates.firstKey().compareTo(threshold) < 0) {
      // tally indexes over all tallies in the current round
      for (BigDecimal tally : currentRoundTallyToCandidates.keySet()) {
        if (tally.compareTo(threshold) < 0) {
          // candidate indexes over all candidates who received this tally
          for (String candidate : currentRoundTallyToCandidates.get(tally)) {
            eliminated.add(candidate);
            Logger.log(
                Level.INFO,
                "Eliminated %s in round %d because they only had %s vote(s), below the "
                    + "minimum threshold of %s.",
                candidate,
                currentRound,
                tally.toString(),
                threshold.toString());
          }
        } else {
          break;
        }
      }
    }
    return eliminated;
  }

  // function: doBatchElimination
  // purpose: eliminate all candidates who are mathematically unable to win
  // param: currentRoundTallyToCandidates map of tally to candidate IDs for a given round
  // returns: eliminated candidates
  private List<String> doBatchElimination(
      SortedMap<BigDecimal, LinkedList<String>> currentRoundTallyToCandidates) {
    List<String> eliminated = new LinkedList<>();
    if (config.isBatchEliminationEnabled()) {
      // container for results
      List<BatchElimination> batchEliminations = runBatchElimination(currentRoundTallyToCandidates);
      if (batchEliminations.size() > 1) {
        // elimination iterates over all BatchElimination objects describing the eliminations
        for (BatchElimination elimination : batchEliminations) {
          eliminated.add(elimination.candidateID);
          Logger.log(
              Level.INFO,
              "Batch-eliminated %s in round %d. The running total was %s vote(s) and the "
                  + "next-highest count was %s vote(s).",
              elimination.candidateID,
              currentRound,
              elimination.runningTotal.toString(),
              elimination.nextHighestTally.toString());
        }
      }
    }
    return eliminated;
  }

  // function: doRegularElimination
  // purpose: eliminate candidate with the lowest tally using tiebreak if necessary
  // param: currentRoundTallyToCandidates map of tally to candidate IDs for a given round
  // returns: eliminated candidates
  private List<String> doRegularElimination(
      SortedMap<BigDecimal, LinkedList<String>> currentRoundTallyToCandidates) {
    List<String> eliminated = new LinkedList<>();
    // eliminated candidate
    String eliminatedCandidate;
    // lowest tally in this round
    BigDecimal minVotes = currentRoundTallyToCandidates.firstKey();
    // list of candidates receiving the lowest tally
    LinkedList<String> lastPlaceCandidates = currentRoundTallyToCandidates.get(minVotes);
    if (lastPlaceCandidates.size() > 1) {
      // there was a tie for last place
      // create new TieBreak object to pick a loser
      TieBreak tieBreak =
          new TieBreak(
              lastPlaceCandidates,
              config.getTiebreakMode(),
              currentRound,
              minVotes,
              roundTallies,
              config.getCandidatePermutation());

      // results of tiebreak stored here
      eliminatedCandidate = tieBreak.selectLoser();
      // TODO: If returned eliminatedCandidate is null, infinite loop!
      Logger.log(
          Level.INFO,
          "%s lost a tie-breaker in round %d against %s. Each candidate had %s vote(s). %s",
          eliminatedCandidate,
          currentRound,
          tieBreak.nonLosingCandidateDescription(),
          minVotes.toString(),
          tieBreak.getExplanation());
    } else {
      // last place candidate will be eliminated
      eliminatedCandidate = lastPlaceCandidates.getFirst();
      Logger.log(
          Level.INFO,
          "%s was eliminated in round %d with %s vote(s).",
          eliminatedCandidate,
          currentRound,
          minVotes.toString());
    }
    eliminated.add(eliminatedCandidate);
    return eliminated;
  }

  // function: generateSummarySpreadsheet
  // purpose: create a ResultsWriter object with the tabulation results data and use it
  // to generate the results spreadsheets
  // param: timestamp string to use when creating output filenames
  void generateSummarySpreadsheet(String timestamp) {
    // writer object will create the output xls
    ResultsWriter writer =
        new ResultsWriter()
            .setNumRounds(currentRound)
            .setCandidatesToRoundEliminated(candidateToRoundEliminated)
            .setWinnerToRound(winnerToRound)
            .setContestConfig(config)
            .setTimestampString(timestamp);

    writer.generateOverallSummarySpreadsheet(roundTallies);

    if (config.isTabulateByPrecinctEnabled()) {
      writer.generatePrecinctSummarySpreadsheets(precinctRoundTallies);
    }
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
      SortedMap<BigDecimal, LinkedList<String>> currentRoundTallyToCandidates) {
    // The sum total of all vote counts examined. This must equal or exceed the next-lowest
    // candidate tally to prevent batch elimination.
    BigDecimal runningTotal = BigDecimal.ZERO;
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
    for (BigDecimal currentVoteTally : currentRoundTallyToCandidates.keySet()) {
      // Test whether leapfrogging is possible.
      if (runningTotal.compareTo(currentVoteTally) < 0) {
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
      BigDecimal totalForThisRound =
          currentVoteTally.multiply(new BigDecimal(currentCandidates.size()));
      runningTotal = runningTotal.add(totalForThisRound);
      candidatesSeen.addAll(currentCandidates);
    }
    return eliminations;
  }

  // purpose: determine if any overvote has occurred for this ranking set (from a CVR)
  // and if so return how to handle it based on the rules configuration in use
  // param: candidateIDSet all candidates this CVR contains at a particular rank
  // return: an OvervoteDecision enum to be applied to the CVR under consideration
  private OvervoteDecision getOvervoteDecision(Set<String> candidateSet) {
    // the resulting decision
    OvervoteDecision decision;
    // the rule we're using
    OvervoteRule rule = config.getOvervoteRule();

    // does this set include the explicit overvote label?
    boolean explicitOvervote = candidateSet.contains(EXPLICIT_OVERVOTE_LABEL);
    if (explicitOvervote) {
      // we should never have the explicit overvote flag AND other candidates for a given ranking
      assert candidateSet.size() == 1;

      // if we have an explicit overvote, the only valid rules are exhaust immediately or
      // always skip. (this is enforced when we load the config also)
      assert rule == OvervoteRule.EXHAUST_IMMEDIATELY
          || rule == OvervoteRule.ALWAYS_SKIP_TO_NEXT_RANK;

      if (rule == OvervoteRule.EXHAUST_IMMEDIATELY) {
        decision = OvervoteDecision.EXHAUST;
      } else {
        decision = OvervoteDecision.SKIP_TO_NEXT_RANK;
      }
    } else if (candidateSet.size() <= 1) {
      // if undervote or one vote which is not the overvote label, then there is no overvote
      decision = OvervoteDecision.NONE;
    } else if (rule == OvervoteRule.EXHAUST_IMMEDIATELY) {
      decision = OvervoteDecision.EXHAUST;
    } else if (rule == OvervoteRule.ALWAYS_SKIP_TO_NEXT_RANK) {
      decision = OvervoteDecision.SKIP_TO_NEXT_RANK;
    } else {
      // if we got here, there are multiple candidates and our rule must be
      // EXHAUST_IF_MULTIPLE_CONTINUING, so the decision depends on how many are continuing

      // default is no overvote unless we encounter multiple continuing
      decision = OvervoteDecision.NONE;
      // keep track if we encounter a continuing candidate
      String continuingCandidate = null;
      for (String candidate : candidateSet) {
        if (isCandidateContinuing(candidate)) {
          if (continuingCandidate != null) { // at least two continuing
            decision = OvervoteDecision.EXHAUST;
            break;
          }
        } else {
          continuingCandidate = candidate;
        }
      }
    }

    return decision;
  }

  // function: getTallyForRound
  // purpose: return a map of candidate ID to vote tallies for this round
  //   generated based on previously eliminated candidateIDs contained in
  //   candidateToRoundEliminated object.
  //   After each call the candidateToRoundEliminated object will get
  //   more entries as candidateIDs are eliminated.
  //   Conversely the roundTally object returned here will contain fewer entries each
  //   of which will have more votes.
  // param: the current round
  // return: map of candidateID to vote tallies for this round
  private Map<String, BigDecimal> getTallyForRound(int currentRound) {
    // map of candidateID to vote tally to store the results
    Map<String, BigDecimal> roundTally = getNewTally();

    // map of tallies per precinct for this round
    Map<String, Map<String, BigDecimal>> roundTallyByPrecinct = new HashMap<>();
    if (config.isTabulateByPrecinctEnabled()) {
      for (String precinct : precinctRoundTallies.keySet()) {
        roundTallyByPrecinct.put(precinct, getNewTally());
      }
    }

    // CVR indexes over the cast vote records to count votes for continuing candidateIDs
    for (CastVoteRecord cvr : castVoteRecords) {
      // If this CVR was assigned to a candidate last round and that candidate is still continuing,
      // we can safely assume that the CVR should still be assigned to that candidate in this round.
      if (!cvr.isExhausted() &&
          (cvr.getCurrentRecipientOfVote() != null) &&
          isCandidateContinuing(cvr.getCurrentRecipientOfVote())) {

        // this vote stays with its current recipient
        // we don't log this as we only want to capture when cvr recipient changes, is exhausted
        // or skipped
        incrementTallies(roundTally,
            cvr.getFractionalTransferValue(),
            cvr.getCurrentRecipientOfVote(),
            roundTallyByPrecinct,
            cvr.getPrecinct());
        continue;
      }

      // check for exhaustion
      cvr.setCurrentRecipientOfVote(null);
      if (cvr.isExhausted()) {
        continue;
      }

      // lastRankSeen tracks the last rank in this rankings set as we iterate through it.
      // This is used to determine how many skipped rankings occurred in the case of undervotes.
      int lastRankSeen = 0;
      // candidatesSeen is the set of candidates we've encountered while processing this CVR
      // in this round; only relevant if exhaustOnDuplicateCandidate is enabled
      Set<String> candidatesSeen = new HashSet<>();
      // rank iterates over all ranks in this CVR ranking set, from most preferred to least
      for (int rank : cvr.rankToCandidateIDs.keySet()) {
        // check for undervote exhaustion (too many consecutive skipped ranks)
        if (config.getMaxSkippedRanksAllowed() != null
            && (rank - lastRankSeen > config.getMaxSkippedRanksAllowed() + 1)) {
          cvr.exhaust();
          cvr.logRoundOutcome(currentRound, VoteOutcomeType.EXHAUSTED, "undervote", null);

          break;
        }
        lastRankSeen = rank;

        // candidateSet is all candidates selected at the current rank
        Set<String> candidateSet = cvr.rankToCandidateIDs.get(rank);

        // possibly check for a duplicate candidate
        if (config.isExhaustOnDuplicateCandidateEnabled()) {
          // the identity of the duplicate candidate, if found
          String duplicateCandidate = null;
          for (String candidate : candidateSet) {
            if (candidatesSeen.contains(candidate)) {
              duplicateCandidate = candidate;
              break; // finding one duplicate is enough
            }
            candidatesSeen.add(candidate);
          }
          if (duplicateCandidate != null && !duplicateCandidate.isEmpty()) {
            cvr.exhaust();
            cvr.logRoundOutcome(currentRound,
                VoteOutcomeType.EXHAUSTED,
                "duplicate candidate: " + duplicateCandidate,
                null);
            break;
          }
        }

        // overvoteDecision is the overvote decision for this ranking
        OvervoteDecision overvoteDecision = getOvervoteDecision(candidateSet);
        if (overvoteDecision == OvervoteDecision.EXHAUST) {
          cvr.exhaust();
          cvr.logRoundOutcome(currentRound, VoteOutcomeType.EXHAUSTED, "overvote", null);
          break;
        } else if (overvoteDecision == OvervoteDecision.SKIP_TO_NEXT_RANK) {
          continue;
        }

        // selectedCandidate for this rank
        String selectedCandidate = null;
        // candidateID indexes through all candidates selected at this rank
        for (String candidate : candidateSet) {
          if (!isCandidateContinuing(candidate)) {
            continue;
          }
          // If this fails, it means the code failed to handle an overvote with multiple
          // continuing candidates.
          assert selectedCandidate == null;
          // we found a continuing candidate
          selectedCandidate = candidate;
          // the FTV for this cast vote record (by default the FTV is exactly one vote, but it
          // could be less in a multi-winner contest if this CVR already helped elect a winner.)
          BigDecimal fractionalTransferValue = cvr.getFractionalTransferValue();

          // We set this in case we need to redistribute votes if this is a multi-winner race and
          // this candidate wins, but there are still more winners to come.
          cvr.setCurrentRecipientOfVote(selectedCandidate);

          // Increment round tally for this candidate by the fractional transfer value of the CVR
          // If enabled, this will also update the roundTallyByPrecinct
          incrementTallies(roundTally,
              fractionalTransferValue,
              selectedCandidate,
              roundTallyByPrecinct,
              cvr.getPrecinct());

          // log the vote transfer to new recipient
          cvr.logRoundOutcome(currentRound,
              VoteOutcomeType.COUNTED,
              selectedCandidate,
              fractionalTransferValue);
        }

        if (selectedCandidate != null) {
          // we've found our candidate
          break;
        }
      } // end looping over the rankings within one ballot

      // Once we get here (having iterated through some or all of this CVR's rankings), either:
      // a) the ballot is exhausted,
      // b) we've counted it toward a candidate, or
      // c) neither.

      // If (c) is the case, we definitely want to exhaust the ballot, and the only thing to resolve
      // is the official reason for exhaustion: did it skip too many rankings at the end of the
      // ballot, or did it just no longer have any continuing candidates?
      if (!cvr.isExhausted() && cvr.getCurrentRecipientOfVote() == null) {
        if (config.getMaxSkippedRanksAllowed() != null
            && config.getMaxRankingsAllowed() - lastRankSeen > config.getMaxSkippedRanksAllowed()) {
          cvr.exhaust();
          cvr.logRoundOutcome(currentRound, VoteOutcomeType.EXHAUSTED, "undervote", null);
        } else {
          cvr.exhaust();
          cvr.logRoundOutcome(currentRound, VoteOutcomeType.EXHAUSTED, "no continuing candidates",
              null);
        }
      }
    } // end looping over all ballots

    // Take the tallies for this round for each precinct and merge them into the main map tracking
    // the tallies by precinct.
    if (config.isTabulateByPrecinctEnabled()) {
      for (String precinct : roundTallyByPrecinct.keySet()) {
        // the set of round tallies that we've built up so far for this precinct
        Map<Integer, Map<String, BigDecimal>> roundTalliesForPrecinct =
            precinctRoundTallies.get(precinct);
        roundTalliesForPrecinct.put(currentRound, roundTallyByPrecinct.get(precinct));
      }
    }

    return roundTally;
  }

  // function: getNewTally
  // purpose: create a new initialized tally with all continuing candidates
  // returns: initialized tally
  private Map<String, BigDecimal> getNewTally() {
    Map<String, BigDecimal> tally = new HashMap<>();
    // initialize tallies to 0 for all continuing candidates
    for (String candidateID : candidateIDs) {
      if (isCandidateContinuing(candidateID)) {
        tally.put(candidateID, BigDecimal.ZERO);
      }
    }
    return tally;
  }

  // function: incrementTally
  // purpose: add a vote (or fractional share of a vote) to a tally
  // param: tally is the round tally we are computing
  // param: cvr is a single cast vote record
  // param: selectedCandidate is the candidate this CVR's vote is going to in this round
  private void incrementTally(
      Map<String, BigDecimal> tally,
      BigDecimal fractionalTransferValue,
      String selectedCandidate) {
    // current tally for this candidate
    BigDecimal currentTally = tally.get(selectedCandidate);
    // new tally after adding this vote
    BigDecimal newTally = currentTally.add(fractionalTransferValue);
    tally.put(selectedCandidate, newTally);
  }

  // function: incrementTallies
  // purpose: transfer vote to round tally and (if valid) the precinct round tally
  // param: roundTally is round tally we are computing
  // param: cvr is a single cast vote record
  // param: selectedCandidate is the candidate this CVR's vote is going to in this round
  // param: roundTallyByPrecinct map of precinct IDs to roundTallies
  // param: precinct ID of precinct for current CVR
  private void incrementTallies(
      Map<String, BigDecimal> roundTally,
      BigDecimal fractionalTransferValue,
      String selectedCandidate,
      Map<String, Map<String, BigDecimal>> roundTallyByPrecinct,
      String precinct) {
    // transfer vote value to round tally
    incrementTally(roundTally, fractionalTransferValue, selectedCandidate);
    // if enabled and there is a valid precinct string transfer vote value to precinct tally
    if (config.isTabulateByPrecinctEnabled() && precinct != null && !precinct.isEmpty()) {
      incrementTally(roundTallyByPrecinct.get(precinct),
          fractionalTransferValue,
          selectedCandidate);
    }
  }

  // function: initPrecinctRoundTallies
  // purpose: initialize the map tracking per-precinct round tallies
  private void initPrecinctRoundTallies() {
    for (CastVoteRecord cvr : castVoteRecords) {
      // the precinct for this cast vote record
      String precinct = cvr.getPrecinct();
      if (precinct != null && !precinct.isEmpty() && !precinctRoundTallies.containsKey(precinct)) {
        precinctRoundTallies.put(precinct, new HashMap<>());
      }
    }
  }

  // OvervoteRule determines how overvotes are handled
  enum OvervoteRule {
    EXHAUST_IMMEDIATELY("exhaustImmediately"),
    ALWAYS_SKIP_TO_NEXT_RANK("alwaysSkipToNextRank"),
    EXHAUST_IF_MULTIPLE_CONTINUING("exhaustIfMultipleContinuing"),
    RULE_UNKNOWN("ruleUnknown");

    private final String label;

    OvervoteRule(String label) {
      this.label = label;
    }

    static OvervoteRule getByLabel(String labelLookup) {
      return Arrays.stream(OvervoteRule.values())
          .filter(v -> v.label.equals(labelLookup))
          .findAny()
          .orElse(null);
    }

    @Override
    public String toString() {
      return label;
    }
  }

  // OvervoteDecision is the result of applying an OvervoteRule to a CVR in a particular round
  enum OvervoteDecision {
    NONE,
    EXHAUST,
    SKIP_TO_NEXT_RANK,
  }

  // TieBreakMode determines how ties will be handled
  enum TieBreakMode {
    RANDOM("random"),
    INTERACTIVE("interactive"),
    PREVIOUS_ROUND_COUNTS_THEN_RANDOM("previousRoundCountsThenRandom"),
    PREVIOUS_ROUND_COUNTS_THEN_INTERACTIVE("previousRoundCountsThenInteractive"),
    USE_PERMUTATION_IN_CONFIG("usePermutationInConfig"),
    GENERATE_PERMUTATION("generatePermutation"),
    MODE_UNKNOWN("modeUnknown");

    private final String label;

    TieBreakMode(String label) {
      this.label = label;
    }

    static TieBreakMode getByLabel(String labelLookup) {
      return Arrays.stream(TieBreakMode.values())
          .filter(v -> v.label.equals(labelLookup))
          .findAny()
          .orElse(null);
    }

    @Override
    public String toString() {
      return label;
    }
  }

  enum CandidateStatus {
    CONTINUING,
    WINNER,
    ELIMINATED,
    INVALID,
  }

  // simple container class used during batch elimination process to store the results
  // for later logging output
  static class BatchElimination {

    // the candidate eliminated
    final String candidateID;
    // how many total votes were totaled when this candidate was eliminated
    final BigDecimal runningTotal;
    // next highest count total (validates that we were correctly batch eliminated)
    final BigDecimal nextHighestTally;

    // function: BatchElimination constructor
    // purpose: create a new BatchElimination object simple container
    // param: candidateID the candidate eliminated
    // param: runningTotal how many total votes were totaled when this candidate was eliminated
    // param: nextHighestTally next highest count total
    // returns: the new object
    BatchElimination(String candidateID, BigDecimal runningTotal, BigDecimal nextHighestTally) {
      this.candidateID = candidateID;
      this.runningTotal = runningTotal;
      this.nextHighestTally = nextHighestTally;
    }
  }
}
