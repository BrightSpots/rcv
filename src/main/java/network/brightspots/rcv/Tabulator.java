/*
 * RCTab
 * Copyright (c) 2017-2023 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: Core contest tabulation logic including:
 * Rank choice flows and vote reallocation.
 * Winner selection, loser selection, and tiebreak rules.
 * Overvote / Skipped Ranking rules.
 * Design: On each loop a round is tallied and tabulated according to selected rules.
 * Inputs: CastVoteRecords for the desired contest and ContestConfig with rules configuration.
 * Results are logged to console and audit file.
 * Conditions: During tabulation.
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import static network.brightspots.rcv.CastVoteRecord.StatusForRound;
import static network.brightspots.rcv.Utils.isNullOrBlank;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import javafx.util.Pair;
import network.brightspots.rcv.CastVoteRecord.VoteOutcomeType;
import network.brightspots.rcv.ContestConfig.TabulateBySlice;
import network.brightspots.rcv.ResultsWriter.RoundSnapshotDataMissingException;

final class Tabulator {

  static final String OVERVOTE_RULE_ALWAYS_SKIP_TEXT = "Always skip to next rank";
  static final String OVERVOTE_RULE_EXHAUST_IMMEDIATELY_TEXT = "Exhaust immediately";
  static final String OVERVOTE_RULE_EXHAUST_IF_MULTIPLE_TEXT = "Exhaust if multiple continuing";
  // When the CVR contains an overvote we "normalize" it to use this string
  static final String EXPLICIT_OVERVOTE_LABEL = "overvote";
  // Similarly, we normalize undeclared write-ins to use this string
  static final String UNDECLARED_WRITE_IN_OUTPUT_LABEL = "Undeclared Write-ins";
  // cast vote records parsed from CVR input files
  private final List<CastVoteRecord> castVoteRecords;
  // all candidate IDs for this contest parsed from the contest config
  private final Set<String> candidateNames;
  // contest config contains specific rules and file paths to be used during tabulation
  private final ContestConfig config;
  // roundTallies is a map from round number to a map from candidate ID to vote total for the round
  // e.g. roundTallies[1] contains a map of all candidate ID -> votes for each candidate in round 1
  // this structure is computed over the course of tabulation
  private final RoundTallies roundTallies = new RoundTallies();
  // roundTalliesBySlices is a map from a tabulate-by slice to roundTallies for that slice
  private final BreakdownBySlice<RoundTallies> roundTalliesBySlices = new BreakdownBySlice<>();
  // candidateToRoundEliminated is a map from candidate ID to round in which they were eliminated
  private final Map<String, Integer> candidateToRoundEliminated = new HashMap<>();
  // map from candidate ID to the round in which they won
  private final Map<String, Integer> winnerToRound = new HashMap<>();
  // tracks vote transfer summaries (usable by external visualizer software)
  private final TallyTransfers tallyTransfers = new TallyTransfers();
  private final BreakdownBySlice<TallyTransfers> tallyTransfersBySlice = new BreakdownBySlice<>();
  // tracks residual surplus from multi-seat contest vote transfers
  private final Map<Integer, BigDecimal> roundToResidualSurplus = new HashMap<>();
  // cast vote record metadata on which tabulation can be "split", such as precinct or batch
  private final SliceIdSet sliceIds = new SliceIdSet();
  // tracks the current round (and when tabulation is completed, the total number of rounds)
  private int currentRound = 0;

  Tabulator(List<CastVoteRecord> castVoteRecords, ContestConfig config)
      throws TabulationAbortedException {
    this.castVoteRecords = castVoteRecords;
    this.candidateNames = config.getCandidateNames();
    this.config = config;

    sliceIds.initialize(ContestConfig.TabulateBySlice.BATCH);
    sliceIds.initialize(ContestConfig.TabulateBySlice.PRECINCT);

    for (CastVoteRecord cvr : castVoteRecords) {
      for (ContestConfig.TabulateBySlice slice : config.enabledSlices()) {
        String sliceId = cvr.getSlice(slice);
        if (sliceId != null) {
          sliceIds.add(slice, sliceId);
        }
      }
    }

    for (ContestConfig.TabulateBySlice slice : config.enabledSlices()) {
      if (sliceIds.isEmpty(slice)) {
        Logger.severe(
            "\"Tabulate by %s\" enabled, but CVRs don't list %ss.",
            slice, slice.toString().toLowerCase());
        throw new TabulationAbortedException(false);
      }

      initTabulateBySliceRoundTallies(slice);
    }
  }

  // Utility function to "invert" the input roundTally map into a sorted map of tally
  // to List of candidate IDs. A list is used because multiple candidates may have the same tally.
  // This is used to determine when winners are selected and for running tiebreak logic.
  // param: roundTally input map of candidate ID to tally for a particular round
  // param candidatesToInclude: list of candidate IDs which may be included in the output.
  //   This filters out candidates when running a tiebreak tabulation which relies
  //   on the tied candidate's previous round totals to break the tie.
  // param: shouldLog is set to log to console and log file
  static SortedMap<BigDecimal, LinkedList<String>> buildTallyToCandidates(
      RoundTally roundTally, Set<String> candidatesToInclude, boolean shouldLog) {
    SortedMap<BigDecimal, LinkedList<String>> tallyToCandidates = new TreeMap<>();
    // for each candidate record their vote total into the countToCandidates object
    for (String candidate : candidatesToInclude) {
      BigDecimal votes = roundTally.getCandidateTally(candidate);
      if (shouldLog) {
        Logger.info("Candidate \"%s\" got %s vote(s).", candidate, votes);
      }
      LinkedList<String> candidates =
          tallyToCandidates.computeIfAbsent(votes, k -> new LinkedList<>());
      candidates.add(candidate);
    }
    return tallyToCandidates;
  }

  // run the main tabulation routine to determine contest results
  // returns: set containing winner(s)
  Set<String> tabulate(Progress progress) throws TabulationAbortedException {
    if (config.needsRandomSeed()) {
      Random random = new Random(config.getRandomSeed());
      if (config.getTiebreakMode() == TiebreakMode.GENERATE_PERMUTATION) {
        // sort candidate permutation first for reproducibility
        Collections.sort(config.getCandidatePermutation());
        // every day I'm shuffling
        Collections.shuffle(config.getCandidatePermutation(), random);
      } else {
        Tiebreak.setRandom(random);
      }
    }

    logSummaryInfo();

    // Loop until we've found our winner(s), with a couple exceptions:
    // - If continueUntilTwoCandidatesRemain is true, we loop until only two
    // candidates remain even if we've already found our winner.
    // - If winnerElectionMode is "Bottoms-up using percentage threshold", we loop until all
    // remaining candidates have vote shares that meet or exceed that threshold.
    //
    // At each iteration, we'll either a. identify one or more
    // winners and transfer their votes to the remaining candidates (if we still need to find more
    // winners), or b. eliminate one or more candidates and gradually transfer votes to the
    // remaining candidates.
    while (shouldContinueTabulating()) {
      currentRound++;
      Logger.info("Round: %d", currentRound);

      // currentRoundTally maps candidate IDs to vote tallies for the current round.
      // At each iteration of this loop that involves eliminating candidates, the eliminatedRound
      // object will gain entries.
      // Conversely, the currentRoundTally object returned here will contain fewer
      // entries, each of which will have as many or more votes than they did in prior rounds.
      // Eventually the winner(s) will be chosen.
      RoundTally currentRoundTally = computeTalliesForRound(currentRound);
      roundTallies.put(currentRound, currentRoundTally);
      roundToResidualSurplus.put(
          currentRound,
          currentRound == 1 ? BigDecimal.ZERO : roundToResidualSurplus.get(currentRound - 1));

      // The winning threshold in a standard multi-seat contest is based on the number of active
      // votes in the first round.
      // In a single-seat contest or in the special multi-seat bottoms-up threshold mode, it's based
      // on the number of active votes in the current round.
      boolean shouldRecomputeThreshold;
      if (config.getNumberOfWinners() > 1) {
        // Multi-winner only computes threshold on round 1
        shouldRecomputeThreshold = currentRound == 1;
      } else {
        // Single-Winner recomputes threshold on round 1 always,
        // and on other rounds if First Round Determines Threshold is not set
        shouldRecomputeThreshold =
            !config.isFirstRoundDeterminesThresholdEnabled() || currentRound == 1;
      }
      if (shouldRecomputeThreshold) {
        calculateAndSetWinningThreshold(currentRoundTally, config.getMinimumVoteThreshold());
      } else {
        BigDecimal lastRoundThreshold = roundTallies.get(currentRound - 1).getWinningThreshold();
        setWinningThreshold(currentRound, lastRoundThreshold);
      }

      // "invert" map and look for winners
      SortedMap<BigDecimal, LinkedList<String>> currentRoundTallyToCandidates =
          buildTallyToCandidates(currentRoundTally, currentRoundTally.getCandidates(), true);
      List<String> winners = identifyWinners(currentRoundTally, currentRoundTallyToCandidates);

      if (!winners.isEmpty()) {
        for (String winner : winners) {
          winnerToRound.put(winner, currentRound);
        }
        // In multi-seat contests, we always redistribute the surplus (if any) unless bottoms-up
        // is enabled.
        if (config.usesSurpluses()) {
          for (String winner : winners) {
            BigDecimal candidateVotes = currentRoundTally.getCandidateTally(winner);
            // number that were surplus (beyond the required threshold)
            BigDecimal extraVotes =
                candidateVotes.subtract(currentRoundTally.getWinningThreshold());
            // fractional transfer percentage
            BigDecimal surplusFraction =
                extraVotes.signum() == 1
                    ? config.divide(extraVotes, candidateVotes)
                    : BigDecimal.ZERO;
            Logger.info(
                "Candidate \"%s\" was elected with a surplus fraction of %s.",
                winner, surplusFraction);
            for (CastVoteRecord cvr : castVoteRecords) {
              if (winner.equals(cvr.getCurrentRecipientOfVote())) {
                cvr.recordCurrentRecipientAsWinner(surplusFraction, config);
              }
            }
          }
        }
      } else if (winnerToRound.size() < config.getNumberOfWinners()
          || (config.isContinueUntilTwoCandidatesRemainEnabled()
              && candidateToRoundEliminated.size() < config.getNumCandidates() - 2)
          || config.isMultiSeatBottomsUpWithThresholdEnabled()) {
        // We need to make more eliminations if
        // a) we haven't found all the winners yet, or
        // b) we've found our winner, but we're continuing until we have only two candidates
        // c) not all remaining candidates meet the bottoms-up threshold

        List<String> eliminated;
        // Four mutually exclusive ways to eliminate candidates.
        // 1. Some races contain undeclared write-ins that should be dropped immediately.
        eliminated = dropUndeclaredWriteIns(currentRoundTally);
        // 2. If there's a minimum vote threshold, drop all candidates below that threshold.
        if (eliminated.isEmpty()) {
          eliminated = dropCandidatesBelowThreshold(currentRoundTallyToCandidates);
          // One edge case: if everyone is below the threshold, we can't proceed. This would only
          // happen in the first or (if we drop undeclared write-ins first) second round.
          if (eliminated.size() == config.getNumDeclaredCandidates()) {
            Logger.severe(
                "Tabulation can't proceed because all declared candidates are below "
                    + "the minimum vote threshold.");
            throw new TabulationAbortedException(false);
          }
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

        if (eliminated.isEmpty()) {
          Logger.severe("Failed to eliminate any candidates!");
          throw new TabulationAbortedException(false);
        }
        for (String loser : eliminated) {
          candidateToRoundEliminated.put(loser, currentRound);
        }
        progress.markCandidatesEliminated(eliminated.size());
      }

      if (config.getNumberOfWinners() > 1) {
        updateWinnerTallies();
      }
    }
    return winnerToRound.keySet();
  }

  // log some basic info about the contest before starting tabulation
  private void logSummaryInfo() {
    Logger.info(
        "There are %d declared candidates for this contest:", config.getNumDeclaredCandidates());
    for (String candidate : candidateNames) {
      if (!candidate.equals(UNDECLARED_WRITE_IN_OUTPUT_LABEL)) {
        Logger.info(
            "%s%s",
            candidate, config.candidateIsExcluded(candidate) ? " (excluded from tabulation)" : "");
      }
    }

    if (config.getTiebreakMode() == TiebreakMode.GENERATE_PERMUTATION) {
      Logger.info("Randomly generated candidate permutation for tie-breaking:");
      for (String candidateId : config.getCandidatePermutation()) {
        Logger.info("%s", candidateId);
      }
    }
  }

  // function: updateWinnerTallies
  // purpose: Update the tally for the just-completed round to reflect the tallies for candidates
  // who won in a past round (in a multi-winner contest). We do this because the regular tally
  // logic only accumulates votes for continuing candidates (not past winners).
  // We do this computation once for each previous round winner to account for transfers. In
  // subsequent rounds, we can just copy the number from the previous round, since it won't change.
  private void updateWinnerTallies() {
    RoundTally roundTally = roundTallies.get(currentRound);
    RoundTally previousRoundTally = roundTallies.get(currentRound - 1);
    roundTally.unlockForSurplusCalculation();

    List<String> winnersToProcess = new LinkedList<>();
    Set<String> winnersRequiringComputation = new HashSet<>();
    for (var entry : winnerToRound.entrySet()) {
      // skip someone who won in the current round (we only care about previous round winners)
      if (entry.getValue() == currentRound) {
        continue;
      }
      winnersToProcess.add(entry.getKey());
      if (entry.getValue() == currentRound - 1) {
        winnersRequiringComputation.add(entry.getKey());
      }
    }

    // initialize or populate overall tally
    for (String winner : winnersToProcess) {
      roundTally.setCandidateTallyViaSurplusAdjustment(
          winner,
          winnersRequiringComputation.contains(winner)
              ? BigDecimal.ZERO
              : previousRoundTally.getCandidateTally(winner));
    }

    // initialize or populate tabulate-by slice tallies
    for (ContestConfig.TabulateBySlice slice : config.enabledSlices()) {
      // this is all the tallies for the given slice
      for (var roundTalliesForSlice : roundTalliesBySlices.get(slice).values()) {
        // and this is the tally for the current round for the slice
        RoundTally roundTallyForSlice = roundTalliesForSlice.get(currentRound);
        roundTallyForSlice.unlockForSurplusCalculation();
        for (String winner : winnersToProcess) {
          roundTallyForSlice.setCandidateTallyViaSurplusAdjustment(
              winner,
              winnersRequiringComputation.contains(winner)
                  ? BigDecimal.ZERO
                  : roundTalliesForSlice.get(currentRound - 1).getCandidateTally(winner));
        }
      }
    }

    // process all the CVRs if needed (i.e. if we have any winners from the previous round to
    // process)
    if (!winnersRequiringComputation.isEmpty()) {
      for (CastVoteRecord cvr : castVoteRecords) {
        // the record of winners who got partial votes from this CVR
        Map<String, BigDecimal> winnerToFractionalValue = cvr.getWinnerToFractionalValue();
        for (var entry : winnerToFractionalValue.entrySet()) {
          String winner = entry.getKey();
          if (!winnersRequiringComputation.contains(winner)) {
            continue;
          }
          BigDecimal fractionalTransferValue = entry.getValue();

          roundTally.addToCandidateTallyViaSurplusAdjustment(winner, fractionalTransferValue);
          for (ContestConfig.TabulateBySlice slice : config.enabledSlices()) {
            String sliceId = cvr.getSlice(slice);
            if (sliceId != null) {
              var roundTalliesForSlice = roundTalliesBySlices.get(slice);
              RoundTallies sliceTallies = roundTalliesForSlice.get(sliceId);
              RoundTally sliceRoundTally = sliceTallies.get(currentRound);
              sliceRoundTally.addToCandidateTallyViaSurplusAdjustment(
                  winner, fractionalTransferValue);
            }
          }
        }
      }

      // Re-lock all by-slice tabulations
      for (ContestConfig.TabulateBySlice slice : config.enabledSlices()) {
        for (var roundTalliesForSlice : roundTalliesBySlices.get(slice).values()) {
          RoundTally roundTallyForSlice = roundTalliesForSlice.get(currentRound);
          roundTallyForSlice.relockAfterSurplusCalculation();
        }
      }

      // We need to handle residual surplus (fractional surplus that can't be transferred due to
      // rounding).
      // For each winner from the previous round, record the residual surplus and then update
      // the winner's new total to be exactly the winning threshold value.
      for (String winner : winnersRequiringComputation) {
        BigDecimal winnerTally = roundTally.getCandidateTally(winner);
        BigDecimal winnerResidual = winnerTally.subtract(roundTally.getWinningThreshold());
        if (winnerResidual.signum() == 1) {
          Logger.info("%s had residual surplus of %s.", winner, winnerResidual);
          roundToResidualSurplus.put(
              currentRound, roundToResidualSurplus.get(currentRound).add(winnerResidual));
          roundTally.setCandidateTallyViaSurplusAdjustment(
              winner, roundTally.getWinningThreshold());
          tallyTransfers.addTransfer(
              currentRound, winner, TallyTransfers.RESIDUAL_TARGET, winnerResidual);
        }
      }
    }

    roundTally.relockAfterSurplusCalculation();
  }

  // determine and store the threshold to win
  private void calculateAndSetWinningThreshold(
        RoundTally currentRoundTally, BigDecimal minimumVoteThreshold) {
    BigDecimal currentRoundTotalVotes = currentRoundTally.activeBallotSum();

    BigDecimal winningThreshold;
    if (config.isMultiSeatBottomsUpWithThresholdEnabled()) {
      winningThreshold =
          currentRoundTotalVotes.multiply(config.getMultiSeatBottomsUpPercentageThreshold());
    } else {
      // divisor for threshold is num winners + 1 (unless archaic Hare quota option is enabled, in
      // which case it's just num winners)
      BigDecimal divisor =
          new BigDecimal(
              config.isHareQuotaEnabled()
                  ? config.getNumberOfWinners()
                  : config.getNumberOfWinners() + 1);
      // If we use integers, we shouldn't use any decimal places.
      // Otherwise, we use the amount of decimal places specified by the user.
      int decimals =
          config.isNonIntegerWinningThresholdEnabled()
              ? config.getDecimalPlacesForVoteArithmetic()
              : 0;
      // Augend is the smallest unit compatible with our rounding.
      // If we are only using integers, augend is 1
      // augend = 10^(-1 * decimals)
      BigDecimal augend = BigDecimal.ONE.divide(BigDecimal.TEN.pow(decimals));
      if (config.isHareQuotaEnabled()) {
        // Rounding up simulates "greater than or equal to".
        // threshold = ceiling(votes / num_winners)
        winningThreshold =
            currentRoundTotalVotes.divide(divisor, decimals, java.math.RoundingMode.UP);
      } else {
        // Rounding down then adding augend simulates "greater than".
        // threshold = floor(votes / (numWinners + 1)) + augend
        winningThreshold =
            currentRoundTotalVotes
                .divide(divisor, decimals, java.math.RoundingMode.DOWN)
                .add(augend);
      }
    }

    // We can never set a winning threshold that's less than the minimum vote threshold specified in
    // the config.
    if (minimumVoteThreshold.signum() == 1
        && minimumVoteThreshold.compareTo(winningThreshold) == 1) {
      winningThreshold = minimumVoteThreshold;
    }

    if (currentRoundTally != roundTallies.get(currentRoundTally.getRoundNumber())) {
      throw new RuntimeException("RoundTally object is not the same as the one in the map.");
    }
    setWinningThreshold(currentRoundTally.getRoundNumber(), winningThreshold);
  }

  private void setWinningThreshold(int roundNumber, BigDecimal winningThreshold) {
    RoundTally currentRoundTally = roundTallies.get(roundNumber);
    currentRoundTally.setWinningThreshold(winningThreshold);

    // Do the same for each slice
    for (TabulateBySlice slice : config.enabledSlices()) {
      for (var roundTalliesForSlice : roundTalliesBySlices.get(slice).values()) {
        roundTalliesForSlice.get(roundNumber).setWinningThreshold(winningThreshold);
      }
    }
    Logger.info("Winning threshold set to %s.", winningThreshold);
  }

  // determine if we should continue tabulating based on how many winners have been
  // selected and if continueUntilTwoCandidatesRemain is true.
  private boolean shouldContinueTabulating() {
    boolean keepTabulating;
    int numEliminatedCandidates = candidateToRoundEliminated.size();
    int numWinnersDeclared = winnerToRound.size();
    if (currentRound >= config.getStopTabulationEarlyAfterRound()) {
      keepTabulating = false;
    } else if (numWinnersDeclared == config.getNumDeclaredCandidates()
        && config.getNumberOfWinners() > config.getNumCandidates()) {
      Logger.warning("Config specifies more winners than candidates. Everyone is now elected.");
      keepTabulating = false;
    } else if (config.isContinueUntilTwoCandidatesRemainEnabled()) {
      // Keep going if there are more than two candidates alive. Also make sure we tabulate one last
      // round after we've made our final elimination.
      keepTabulating =
          numEliminatedCandidates + numWinnersDeclared + 1 < config.getNumCandidates()
              || candidateToRoundEliminated.containsValue(currentRound);
    } else if (config.isMultiSeatBottomsUpWithThresholdEnabled()) {
      // in this mode, we're done as soon as we've declared any winners
      keepTabulating = numWinnersDeclared == 0;
    } else {
      // If there are more seats to fill, we should keep going, of course.
      // But also: if we've selected all the winners in a multi-seat contest, we should tabulate one
      // extra round in order to show the effect of redistributing the final surpluses... unless
      // bottoms-up is enabled, in which case we can stop as soon as we've declared the winners.
      keepTabulating =
          numWinnersDeclared < config.getNumberOfWinners()
              || (config.usesSurpluses()
                  && winnerToRound.containsValue(currentRound));
    }
    return keepTabulating;
  }

  // Handles continued tabulation after a winner has been chosen when
  // continueUntilTwoCandidatesRemain is true.
  private boolean isCandidateContinuing(String candidate) {
    CandidateStatus status = getCandidateStatus(candidate);
    return status == CandidateStatus.CONTINUING
        || (status == CandidateStatus.WINNER && config.isContinueUntilTwoCandidatesRemainEnabled());
  }

  // returns candidate status (continuing, eliminated or winner)
  private CandidateStatus getCandidateStatus(String candidate) {
    CandidateStatus status = CandidateStatus.CONTINUING;
    if (config.candidateIsExcluded(candidate)) {
      status = CandidateStatus.EXCLUDED;
    } else if (winnerToRound.containsKey(candidate)) {
      status = CandidateStatus.WINNER;
    } else if (candidateToRoundEliminated.containsKey(candidate)) {
      status = CandidateStatus.ELIMINATED;
    } else if (candidate.equals(EXPLICIT_OVERVOTE_LABEL)) {
      status = CandidateStatus.INVALID;
    }
    return status;
  }

  // determine if one or more winners have been identified in this round
  // param: currentRoundTally round tally for a particular round
  // param: currentRoundTallyToCandidates map of tally to candidate ID(s) for a particular round
  // return: list of winning candidates in this round (if any)
  private List<String> identifyWinners(
      RoundTally currentRoundTally,
      SortedMap<BigDecimal, LinkedList<String>> currentRoundTallyToCandidates)
      throws TabulationAbortedException {
    List<String> selectedWinners = new LinkedList<>();

    if (getCandidateSignum(UNDECLARED_WRITE_IN_OUTPUT_LABEL, currentRoundTally) != 0) {
      // No winners can be selected while undeclared candidates are live
      return selectedWinners;
    }

    if (config.isMultiSeatBottomsUpWithThresholdEnabled()) {
      // if everyone meets the threshold, select them all as winners
      boolean allMeet =
          currentRoundTally
                  .getCandidatesWithMoreVotesThan(currentRoundTally.getWinningThreshold())
                  .size()
              == currentRoundTally.activeCandidateSum();
      if (allMeet) {
        selectedWinners.addAll(currentRoundTally.getCandidates());
      }
    } else {
      // We should only look for more winners if we haven't already filled all the seats.
      int numSeatsUnfilled = config.getNumberOfWinners() - winnerToRound.size();
      if (numSeatsUnfilled > 0) {
        if (currentRoundTally.activeCandidateSum() <= numSeatsUnfilled) {
          // If there are as many or fewer continuing candidates than seats to fill, everyone wins.
          selectedWinners.addAll(currentRoundTally.getCandidates());
        } else if (config.isFirstRoundDeterminesThresholdEnabled()
            && currentRoundTally.activeCandidateSum() - 1 == config.getNumberOfWinners()) {
          // Edge case: if nobody meets the threshold, but we're on the penultimate round when
          // isFirstRoundDeterminesThresholdEnabled is true, select the max vote getters as
          // the winners. If isFirstRoundDeterminesThresholdEnabled isn't enabled, it should be
          // impossible for a single-winner election to end up here.
          BigDecimal maxVotes = currentRoundTallyToCandidates.lastKey();
          selectedWinners = currentRoundTallyToCandidates.get(maxVotes);
        } else if (!config.isMultiSeatBottomsUpUntilNWinnersEnabled()) {
          // Otherwise, select all winners above the threshold
          selectWinners(
              currentRoundTallyToCandidates,
              currentRoundTally.getWinningThreshold(),
              selectedWinners);
        }
      }

      // Edge case: if we've identified multiple winners in this round, but we're only supposed to
      // elect one winner per round, pick the top vote-getter.
      // * If this is a multi-winner election, defer the others to subsequent rounds.
      // * If this is a single-winner election in which it's possible for no candidate to reach the
      //   threshold (i.e. "first round determines threshold" is set), the tiebreaker will choose
      //   the only winner.
      boolean needsTiebreakMultipleWinners =
          selectedWinners.size() > 1
              && (config.isMultiSeatAllowOnlyOneWinnerPerRoundEnabled()
                  || config.isFirstRoundDeterminesThresholdEnabled());
      // Edge case: there are two candidates remaining. To avoid having just one candidate in the
      // final round, we break the tie here. Happens when we have unfilled seats, two candidates
      // remaining, neither meets the threshold, and both have more than the minimum vote threshold.
      // Conditions:
      //  1. Single-winner election
      //  2. There are two remaining candidates
      //  3. There is one seat unfilled (i.e. the seat hasn't already been filled in a previous
      //           round due to "Continue Until Two Remain" config option)
      //  4. All candidates are over the minimum threshold (see no_one_meets_minimum test)
      boolean needsTiebreakNoWinners =
          config.getNumberOfWinners() == 1
              && selectedWinners.isEmpty()
              && currentRoundTally.activeCandidateSum() == 2
              && numSeatsUnfilled == 1
              && currentRoundTallyToCandidates.keySet().stream()
                  .allMatch(x -> x.compareTo(config.getMinimumVoteThreshold()) >= 0);
      if (needsTiebreakMultipleWinners || needsTiebreakNoWinners) {
        // currentRoundTallyToCandidates is sorted from low to high, so just look at the last key
        BigDecimal maxVotes = currentRoundTallyToCandidates.lastKey();
        selectedWinners = currentRoundTallyToCandidates.get(maxVotes);
        // But if there are multiple candidates tied for the max tally, we need to break the tie.
        if (selectedWinners.size() > 1) {
          Tiebreak tiebreak =
              new Tiebreak(
                  true,
                  selectedWinners,
                  config.getTiebreakMode(),
                  currentRound,
                  maxVotes,
                  roundTallies,
                  config.getCandidatePermutation());
          String winner = tiebreak.selectCandidate();
          // replace the list of tied candidates with our single tie-break winner
          selectedWinners = new LinkedList<>();
          selectedWinners.add(winner);
          Logger.info(
              "Candidate \"%s\" won a tie-breaker in round %d against %s. Each candidate had %s "
                  + "vote(s). %s",
              winner,
              currentRound,
              tiebreak.nonSelectedCandidateDescription(),
              maxVotes,
              tiebreak.getExplanation());
        }
      }
    }

    for (String winner : selectedWinners) {
      Logger.info(
          "Candidate \"%s\" was elected in round %d with %s votes.",
          winner, currentRound, currentRoundTally.getCandidateTally(winner));
    }

    return selectedWinners;
  }

  private void selectWinners(
      SortedMap<BigDecimal, LinkedList<String>> currentRoundTallyToCandidates,
      BigDecimal winningThreshold,
      List<String> selectedWinners) {
    // select all candidates which have equaled or exceeded the winning threshold and add them to
    // the selectedWinners List
    for (var entry : currentRoundTallyToCandidates.entrySet()) {
      if (entry.getKey().compareTo(winningThreshold) >= 0) {
        // we have winner(s)
        for (String candidate : entry.getValue()) {
          // The undeclared write-in placeholder can't win
          if (!candidate.equals(UNDECLARED_WRITE_IN_OUTPUT_LABEL)) {
            selectedWinners.add(candidate);
          }
        }
      }
    }
  }

  // function: dropUndeclaredWriteIns
  // purpose: eliminate all undeclared write in candidates
  // param: currentRoundTally map of candidate IDs to their tally for a given round
  // returns: eliminated candidates
  private List<String> dropUndeclaredWriteIns(RoundTally currentRoundTally) {
    List<String> eliminated = new LinkedList<>();
    String label = UNDECLARED_WRITE_IN_OUTPUT_LABEL;
    if (getCandidateSignum(label, currentRoundTally) == 1) {
      eliminated.add(label);
      Logger.info(
          "Eliminated candidate \"%s\" in round %d because it represents undeclared write-ins. It "
              + "had %s votes.",
          label, currentRound, currentRoundTally.getCandidateTally(label));
    }
    return eliminated;
  }

  // Returns the signum of the tally for a given label, or 0 if the label is not
  // in the current round
  // param: label candidate ID
  // param: currentRoundTally map of candidate IDs to their tally for a given round
  // returns: signum of the candidate tally
  private int getCandidateSignum(String label, RoundTally currentRoundTally) {
    BigDecimal tally = currentRoundTally.getCandidateTally(label);
    if (tally == null) {
      return 0;
    }
    return tally.signum();
  }

  // eliminate all candidates below a certain tally threshold
  // param: currentRoundTallyToCandidates map of tally to candidate IDs for a given round
  // returns: eliminated candidates
  private List<String> dropCandidatesBelowThreshold(
      SortedMap<BigDecimal, LinkedList<String>> currentRoundTallyToCandidates) {
    List<String> eliminated = new LinkedList<>();
    // min threshold
    BigDecimal threshold = config.getMinimumVoteThreshold();
    if (threshold.signum() == 1
        && currentRoundTallyToCandidates.firstKey().compareTo(threshold) < 0) {
      for (var entry : currentRoundTallyToCandidates.entrySet()) {
        if (entry.getKey().compareTo(threshold) < 0) {
          for (String candidate : entry.getValue()) {
            eliminated.add(candidate);
            Logger.info(
                "Eliminated candidate \"%s\" in round %d because they only had %s vote(s), below "
                    + "the minimum threshold of %s.",
                candidate, currentRound, entry.getKey(), threshold);
          }
        } else {
          break;
        }
      }
    }
    return eliminated;
  }

  // eliminate all candidates who are mathematically unable to win
  // param: currentRoundTallyToCandidates map of tally to candidate IDs for a given round
  // returns: eliminated candidates
  private List<String> doBatchElimination(
      SortedMap<BigDecimal, LinkedList<String>> currentRoundTallyToCandidates) {
    List<String> eliminated = new LinkedList<>();
    if (config.isBatchEliminationEnabled()) {
      List<BatchElimination> batchEliminations = runBatchElimination(currentRoundTallyToCandidates);
      if (batchEliminations.size() > 1) {
        for (BatchElimination elimination : batchEliminations) {
          eliminated.add(elimination.candidateId);
          Logger.info(
              "Batch-eliminated candidate \"%s\" in round %d. The running total was %s vote(s) and "
                  + "the next-lowest count was %s vote(s).",
              elimination.candidateId,
              currentRound,
              elimination.runningTotal,
              elimination.nextLowestTally);
        }
      }
    }
    return eliminated;
  }

  // eliminate candidate with the lowest tally using tiebreak if necessary
  // param: currentRoundTallyToCandidates map of tally to candidate IDs for a given round
  // returns: eliminated candidates
  private List<String> doRegularElimination(
      SortedMap<BigDecimal, LinkedList<String>> currentRoundTallyToCandidates)
      throws TabulationAbortedException {
    List<String> eliminated = new LinkedList<>();
    String eliminatedCandidate;
    // lowest tally in this round
    BigDecimal minVotes = currentRoundTallyToCandidates.firstKey();
    // list of candidates receiving the lowest tally
    LinkedList<String> lastPlaceCandidates = currentRoundTallyToCandidates.get(minVotes);
    if (lastPlaceCandidates.size() > 1) {
      // there was a tie for last place
      // create new Tiebreak object to pick a loser
      Tiebreak tiebreak =
          new Tiebreak(
              false,
              lastPlaceCandidates,
              config.getTiebreakMode(),
              currentRound,
              minVotes,
              roundTallies,
              config.getCandidatePermutation());

      eliminatedCandidate = tiebreak.selectCandidate();
      Logger.info(
          "Candidate \"%s\" lost a tie-breaker in round %d against %s. Each candidate had %s "
              + "vote(s). %s",
          eliminatedCandidate,
          currentRound,
          tiebreak.nonSelectedCandidateDescription(),
          minVotes,
          tiebreak.getExplanation());
    } else {
      eliminatedCandidate = lastPlaceCandidates.getFirst();
      Logger.info(
          "Candidate \"%s\" was eliminated in round %d with %s vote(s).",
          eliminatedCandidate, currentRound, minVotes);
    }
    eliminated.add(eliminatedCandidate);
    return eliminated;
  }

  // create a ResultsWriter object with the tabulation results data and use it
  // to generate the results spreadsheets
  // param: timestamp string to use when creating output filenames
  void generateSummaryFiles(String timestamp) throws IOException {
    ResultsWriter writer =
        new ResultsWriter()
            .setNumRounds(currentRound)
            .setCandidatesToRoundEliminated(candidateToRoundEliminated)
            .setWinnerToRound(winnerToRound)
            .setContestConfig(config)
            .setTimestampString(timestamp)
            .setSliceIds(sliceIds)
            .setRoundToResidualSurplus(roundToResidualSurplus);

    List<String> candidateOrder = roundTallies.get(1).getSortedCandidatesByTally();
    writer.generateOverallSummaryFiles(roundTallies, tallyTransfers, candidateOrder);
    writer.generateBySliceSummaryFiles(roundTalliesBySlices, tallyTransfersBySlice, candidateOrder);

    if (config.isGenerateCdfJsonEnabled()) {
      try {
        writer.generateCdfJson(castVoteRecords);
      } catch (RoundSnapshotDataMissingException exception) {
        Logger.severe(
            "CDF JSON generation failed due to missing snapshot for %s", exception.getCvrId());
      }
    }
  }

  SliceIdSet getEnabledSliceIds() {
    return sliceIds;
  }

  // Function: runBatchElimination
  // Purpose: applies batch elimination logic to the input vote counts to remove multiple candidates
  //   in a single round if their vote counts are so low that they could not possibly end up winning
  //   Consider, after each round of voting, a candidate not eliminated could potentially receive
  //   ALL the votes from candidates who ARE eliminated, keeping them in the race and "leapfrogging"
  //   ahead of candidates who were leading them.
  //   In this algorithm we sum candidate vote totals (low to high) and find where this leapfrogging
  //   is impossible: that is, when the sum of all batch-eliminated candidates' votes fails to equal
  //   or exceed the next-lowest candidate vote total.
  //   One additional caveat when continueUntilTwoCandidatesRemain is true: make sure we don't
  //   batch-eliminate too many candidates and end up with just the winner.
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
    LinkedList<BatchElimination> eliminations = new LinkedList<>();
    // See the caveat above about continueUntilTwoCandidatesRemain. In this situation, we need to
    // remove the final set of candidates that we had added to the batch, so we hold onto
    // the previous version of the eliminations list whenever an iteration of the loop augments it.
    LinkedList<BatchElimination> previousEliminations = new LinkedList<>();

    // At each iteration, currentVoteTally is the next-lowest vote count received by one or more
    // candidate(s) in the current round.
    for (var entry : currentRoundTallyToCandidates.entrySet()) {
      BigDecimal currentVoteTally = entry.getKey();
      // a shallow copy is sufficient
      LinkedList<BatchElimination> newEliminations = new LinkedList<>(eliminations);
      // Test whether leapfrogging is possible.
      if (runningTotal.compareTo(currentVoteTally) < 0) {
        // Not possible, so eliminate everyone who has been seen and not eliminated yet.
        // candidate indexes over all seen candidates
        for (String candidate : candidatesSeen) {
          if (!candidatesEliminated.contains(candidate)) {
            candidatesEliminated.add(candidate);
            newEliminations.add(new BatchElimination(candidate, runningTotal, currentVoteTally));
          }
        }
      }
      // Add the candidates for the currentVoteTally to the seen list and accumulate their votes.
      // currentCandidates is all candidates receiving the current vote tally
      List<String> currentCandidates = entry.getValue();
      BigDecimal totalForThisRound =
          config.multiply(currentVoteTally, new BigDecimal(currentCandidates.size()));
      runningTotal = runningTotal.add(totalForThisRound);
      candidatesSeen.addAll(currentCandidates);
      if (newEliminations.size() > eliminations.size()) {
        previousEliminations = eliminations;
        eliminations = newEliminations;
      }
    }
    if (config.isContinueUntilTwoCandidatesRemainEnabled()
        && eliminations.size() + candidateToRoundEliminated.size()
            == config.getNumCandidates() - 1) {
      // See the caveat above about continueUntilTwoCandidatesRemain. In this situation, we need to
      // remove the final set of candidates that we had added to the elimination list.
      eliminations = previousEliminations;
    }
    return eliminations;
  }

  // purpose: determine if any overvote has occurred for this ranking set (from a CVR)
  // and if so return how to handle it based on the rule configuration in use
  // param: candidates all candidates this CVR contains at a particular rank
  // return: an OvervoteDecision enum to be applied to the CVR under consideration
  private OvervoteDecision getOvervoteDecision(CandidatesAtRanking candidates)
      throws TabulationAbortedException {
    OvervoteDecision decision;
    OvervoteRule rule = config.getOvervoteRule();
    boolean explicitOvervote = candidates.contains(EXPLICIT_OVERVOTE_LABEL);
    if (explicitOvervote) {
      // we should never have the explicit overvote flag AND other candidates for a given ranking
      if (candidates.count() != 1) {
        Logger.severe("Found multiple candidates when explicit overvote label was provided!");
        throw new TabulationAbortedException(false);
      }

      // if we have an explicit overvote, the only valid rules are exhaust immediately or
      // always skip. (this is enforced when we load the config also)
      if (rule != OvervoteRule.EXHAUST_IMMEDIATELY
          && rule != OvervoteRule.ALWAYS_SKIP_TO_NEXT_RANK) {
        Logger.severe(
            "Invalid overvote rule \"%s\" selected when explicit overvote label was provided!",
            rule);
        throw new TabulationAbortedException(false);
      }

      if (rule == OvervoteRule.EXHAUST_IMMEDIATELY) {
        decision = OvervoteDecision.EXHAUST;
      } else {
        decision = OvervoteDecision.SKIP_TO_NEXT_RANK;
      }
    } else if (candidates.count() <= 1) {
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
      for (String candidate : candidates) {
        if (isCandidateContinuing(candidate)) {
          if (continuingCandidate != null) { // at least two continuing
            decision = OvervoteDecision.EXHAUST;
            break;
          } else {
            continuingCandidate = candidate;
          }
        }
      }
    }

    return decision;
  }

  // recordSelectionForCastVoteRecord:
  //  set new recipient of cvr
  //  logs the results to audit log
  //  update tallyTransfers counts
  private void recordSelectionForCastVoteRecord(
      CastVoteRecord cvr,
      RoundTally currentRoundTally,
      BreakdownBySlice<RoundTally> roundTallyBySlice,
      String selectedCandidate,
      StatusForRound statusForRound,
      String additionalLogText)
      throws TabulationAbortedException {
    // update transfer counts (unless there's no value to transfer, which can happen if someone
    // wins with a tally that exactly matches the winning threshold)
    if (cvr.getFractionalTransferValue().signum() == 1) {
      tallyTransfers.addTransfer(
          currentRoundTally.getRoundNumber(),
          cvr.getCurrentRecipientOfVote(),
          selectedCandidate,
          cvr.getFractionalTransferValue());
      for (ContestConfig.TabulateBySlice slice : config.enabledSlices()) {
        String sliceId = cvr.getSlice(slice);
        TallyTransfers tallyTransferForSlice = tallyTransfersBySlice.get(slice, sliceId);
        if (tallyTransferForSlice == null) {
          Logger.severe(
              "%s \"%s\" is not among the %d known %s.",
              slice, sliceId, sliceIds.size(slice), slice);
          throw new TabulationAbortedException(false);
        }
        tallyTransferForSlice.addTransfer(
            currentRoundTally.getRoundNumber(),
            cvr.getCurrentRecipientOfVote(),
            selectedCandidate,
            cvr.getFractionalTransferValue());
      }
    }

    cvr.setCurrentRecipientOfVote(selectedCandidate);
    if (selectedCandidate == null) {
      cvr.exhaustBy(statusForRound);
    }

    if (statusForRound != StatusForRound.ACTIVE) {
      currentRoundTally.addInactiveBallot(statusForRound, cvr.getFractionalTransferValue());
      for (ContestConfig.TabulateBySlice slice : config.enabledSlices()) {
        String sliceId = cvr.getSlice(slice);
        RoundTally sliceRoundTally = roundTallyBySlice.get(slice).get(sliceId);
        sliceRoundTally.addInactiveBallot(statusForRound, cvr.getFractionalTransferValue());
      }
    }

    final String outcomeDescription = statusForRound == StatusForRound.ACTIVE
            ? selectedCandidate
            : statusForRound.getTitleCaseKey() + additionalLogText;
    final VoteOutcomeType outcomeType =
        selectedCandidate == null ? VoteOutcomeType.EXHAUSTED : VoteOutcomeType.COUNTED;
    cvr.logRoundOutcome(
        currentRoundTally.getRoundNumber(),
        outcomeType,
        outcomeDescription,
        cvr.getFractionalTransferValue());

    if (config.isGenerateCdfJsonEnabled()) {
      cvr.logCdfSnapshotData(currentRoundTally.getRoundNumber());
    }
  }

  // purpose: perform tabulation on all cvrs to determine who they should count for in this round
  //  - exhaust cvrs if they should be exhausted for various reasons
  //  - assign cvrs to continuing candidates if they have been transferred or in the initial count
  // returns a map of candidate ID to vote tallies for this round
  private RoundTally computeTalliesForRound(int currentRound) throws TabulationAbortedException {
    RoundTally roundTally = getNewTally(currentRound);
    BreakdownBySlice<RoundTally> roundTallyBySlice = new BreakdownBySlice();
    for (ContestConfig.TabulateBySlice slice : config.enabledSlices()) {
      for (String sliceId : roundTalliesBySlices.get(slice).keySet()) {
        roundTallyBySlice.initialize(slice, sliceId, getNewTally(currentRound));
      }
    }

    // Loop over ALL cast vote records to determine who they should count for in this round,
    // based on which candidates have already been eliminated and elected.
    // At each iteration a cvr will either:
    //  count for the same candidate it currently does
    //  count for a different candidate
    //  become exhausted
    //  remain exhausted
    for (CastVoteRecord cvr : castVoteRecords) {
      if (cvr.isExhausted()) {
        roundTally.addInactiveBallot(cvr.getBallotStatus(), cvr.getFractionalTransferValue());

        // Add inactive ballot to each slice too
        for (ContestConfig.TabulateBySlice slice : config.enabledSlices()) {
          String sliceId = cvr.getSlice(slice);
          RoundTally sliceRoundTally = roundTallyBySlice.get(slice).get(sliceId);
          sliceRoundTally.addInactiveBallot(
              cvr.getBallotStatus(), cvr.getFractionalTransferValue());
        }
        continue;
      }

      // check for current recipient continuing
      if (cvr.getCurrentRecipientOfVote() != null
          && isCandidateContinuing(cvr.getCurrentRecipientOfVote())) {
        // current candidate is continuing so rollover their vote into the current round
        incrementTallies(
            roundTally,
            cvr,
            cvr.getCurrentRecipientOfVote(),
            roundTallyBySlice);
        continue;
      }

      // check for a CVR with no rankings at all
      if (cvr.candidateRankings.numRankings() == 0) {
        recordSelectionForCastVoteRecord(
            cvr,
            roundTally,
            roundTallyBySlice,
            null,
            StatusForRound.DID_NOT_RANK_ANY_CANDIDATES,
            "");
      }

      // iterate through the rankings in this cvr from most to least preferred.
      // for each ranking:
      //  check if it's a final round surplus
      //  if it results in an overvote or undervote, exhaust the cvr
      //  if a selected candidate is continuing, count cvr for that candidate
      //  if no selected candidate is continuing, look at the next ranking
      //  if there are no more rankings, exhaust the cvr

      // lastRankSeen tracks the last rank in the current rankings set
      // This is used to determine how many skipped rankings occurred.
      int lastRankSeen = 0;
      // candidatesSeen is set of candidates encountered while processing this CVR in this round
      // used to detect duplicate candidates if exhaustOnDuplicateCandidate is enabled
      Set<String> candidatesSeen = new HashSet<>();

      // selectedCandidate holds the new candidate selection if there is one
      String selectedCandidate = null;

      // iterate over all ranks in this cvr from most preferred to least
      for (Pair<Integer, CandidatesAtRanking> rankCandidatesPair : cvr.candidateRankings) {
        Integer rank = rankCandidatesPair.getKey();
        CandidatesAtRanking candidates = rankCandidatesPair.getValue();

        // check for final round surplus
        if (config.usesSurpluses()
            && config.getNumberOfWinners() == winnerToRound.size()) {
          recordSelectionForCastVoteRecord(
                  cvr,
                  roundTally,
                  roundTallyBySlice,
                  null,
                  StatusForRound.FINAL_ROUND_SURPLUS,
                  "");
          break;
        }

        // check for skipped ranking exhaustion
        if (config.getMaxSkippedRanksAllowed() != Integer.MAX_VALUE
            && (rank - lastRankSeen > config.getMaxSkippedRanksAllowed() + 1)) {
          recordSelectionForCastVoteRecord(
              cvr,
              roundTally,
              roundTallyBySlice,
              null,
              StatusForRound.INVALIDATED_BY_SKIPPED_RANKING,
              "");
          break;
        }
        lastRankSeen = rank;

        // check for a duplicate candidate if enabled
        if (config.isExhaustOnDuplicateCandidateEnabled()) {
          String duplicateCandidate = null;
          for (String candidate : candidates) {
            if (candidatesSeen.contains(candidate)) {
              duplicateCandidate = candidate;
              break;
            }
            candidatesSeen.add(candidate);
          }
          // if duplicate was found exhaust cvr
          if (!isNullOrBlank(duplicateCandidate)) {
            recordSelectionForCastVoteRecord(
                cvr,
                roundTally,
                roundTallyBySlice,
                null,
                StatusForRound.INVALIDATED_BY_REPEATED_RANKING,
                " " + duplicateCandidate);
            break;
          }
        }

        // check for an overvote
        OvervoteDecision overvoteDecision = getOvervoteDecision(candidates);
        if (overvoteDecision == OvervoteDecision.EXHAUST) {
          recordSelectionForCastVoteRecord(
              cvr,
              roundTally,
              roundTallyBySlice,
              null,
              StatusForRound.INVALIDATED_BY_OVERVOTE,
              "");
          break;
        } else if (overvoteDecision == OvervoteDecision.SKIP_TO_NEXT_RANK) {
          if (rank == cvr.candidateRankings.maxRankingNumber()) {
            // If the final ranking is an overvote, even if we're trying to skip to the next rank,
            // we consider this inactive by exhausted choices -- not an overvote.
            recordSelectionForCastVoteRecord(
                cvr,
                roundTally,
                roundTallyBySlice,
                null,
                StatusForRound.EXHAUSTED_CHOICE,
                "");
          }
          continue;
        }

        // the current ranking is not inactive by overvote or too many skipped rankings
        // see if any ranked candidates are continuing

        for (String candidate : candidates) {
          String candidateName = config.getNameForCandidate(candidate);
          if (!isCandidateContinuing(candidateName)) {
            continue;
          }

          // we found a continuing candidate so this cvr counts for them
          selectedCandidate = candidateName;

          // transfer cvr to selected candidate
          recordSelectionForCastVoteRecord(
              cvr, roundTally, roundTallyBySlice, selectedCandidate, StatusForRound.ACTIVE, "");

          // This will also update the roundTallyBySlice for each enabled slice
          incrementTallies(roundTally, cvr, selectedCandidate, roundTallyBySlice);

          // There can be at most one continuing candidate in candidates; if there were more than
          // one, we would have already flagged this as an overvote.
          break;
        }

        // if we found a continuing candidate stop looking through rankings
        if (selectedCandidate != null) {
          break;
        }

        // if this is the last ranking we are out of rankings and must exhaust this cvr
        if (rank == cvr.candidateRankings.maxRankingNumber()) {
          recordSelectionForCastVoteRecord(
              cvr, roundTally, roundTallyBySlice, null, StatusForRound.EXHAUSTED_CHOICE, "");
        }
      } // end looping over the rankings within one ballot
    } // end looping over all ballots

    // Take the tallies for this round for each slice and merge them into the main map tracking
    // the tallies by each enabled slice.
    for (ContestConfig.TabulateBySlice slice : config.enabledSlices()) {
      for (var entry : roundTallyBySlice.get(slice).entrySet()) {
        RoundTallies roundTalliesForSlice = roundTalliesBySlices.get(slice).get(entry.getKey());
        roundTalliesForSlice.put(currentRound, entry.getValue());
        roundTalliesForSlice.get(currentRound).lockInRound();
      }
    }
    roundTally.lockInRound();

    return roundTally;
  }

  // create a new initialized tally with all continuing candidates
  private RoundTally getNewTally(int roundNumber) {
    return new RoundTally(roundNumber, candidateNames.stream().filter(this::isCandidateContinuing));
  }

  // transfer vote to round tally and (if valid) the by-slice round tally
  private void incrementTallies(RoundTally roundTally, CastVoteRecord cvr,
        String selectedCandidate, BreakdownBySlice<RoundTally> roundTalliesBySlices) {
    BigDecimal fractionalTransferValue = cvr.getFractionalTransferValue();
    roundTally.addToCandidateTally(selectedCandidate, fractionalTransferValue);
    for (ContestConfig.TabulateBySlice slice : config.enabledSlices()) {
      String sliceId = cvr.getSlice(slice);
      if (!isNullOrBlank(sliceId)) {
        roundTalliesBySlices.get(slice, sliceId)
                .addToCandidateTally(selectedCandidate, fractionalTransferValue);
      }
    }
  }

  private void initTabulateBySliceRoundTallies(ContestConfig.TabulateBySlice slice)
        throws TabulationAbortedException {
    for (String sliceId : sliceIds.get(slice)) {
      if (isNullOrBlank(sliceId)) {
        Logger.severe("Null %s found in %s list: %s", slice, slice, sliceId);
        throw new TabulationAbortedException(false);
      }
      roundTalliesBySlices.initialize(slice, sliceId, new RoundTallies());
      tallyTransfersBySlice.initialize(slice, sliceId, new TallyTransfers());

    }
  }

  // OvervoteRule determines how overvotes are handled
  enum OvervoteRule {
    ALWAYS_SKIP_TO_NEXT_RANK("alwaysSkipToNextRank", OVERVOTE_RULE_ALWAYS_SKIP_TEXT),
    EXHAUST_IMMEDIATELY("exhaustImmediately", OVERVOTE_RULE_EXHAUST_IMMEDIATELY_TEXT),
    EXHAUST_IF_MULTIPLE_CONTINUING(
        "exhaustIfMultipleContinuing", OVERVOTE_RULE_EXHAUST_IF_MULTIPLE_TEXT),
    RULE_UNKNOWN("ruleUnknown", "Unknown rule");

    private final String internalLabel;
    private final String guiLabel;

    OvervoteRule(String internalLabel, String guiLabel) {
      this.internalLabel = internalLabel;
      this.guiLabel = guiLabel;
    }

    static OvervoteRule getByInternalLabel(String labelLookup) {
      return Arrays.stream(OvervoteRule.values())
          .filter(v -> v.internalLabel.equals(labelLookup))
          .findAny()
          .orElse(RULE_UNKNOWN);
    }

    @Override
    public String toString() {
      return guiLabel;
    }

    public String getInternalLabel() {
      return internalLabel;
    }
  }

  // OvervoteDecision is the result of applying an OvervoteRule to a CVR in a particular round
  enum OvervoteDecision {
    NONE,
    EXHAUST,
    SKIP_TO_NEXT_RANK,
  }

  // TiebreakMode determines how ties will be handled
  enum TiebreakMode {
    RANDOM("random", "Random"),
    INTERACTIVE("stopCountingAndAsk", "Stop counting and ask"),
    PREVIOUS_ROUND_COUNTS_THEN_RANDOM(
        "previousRoundCountsThenRandom", "Previous round counts (then random)"),
    PREVIOUS_ROUND_COUNTS_THEN_INTERACTIVE(
        "previousRoundCountsThenAsk", "Previous round counts (then stop counting and ask)"),
    USE_PERMUTATION_IN_CONFIG("useCandidateOrder", "Use candidate order in the config file"),
    GENERATE_PERMUTATION("generatePermutation", "Generate permutation"),
    MODE_UNKNOWN("modeUnknown", "Unknown mode");

    private final String internalLabel;
    private final String guiLabel;

    TiebreakMode(String internalLabel, String guiLabel) {
      this.internalLabel = internalLabel;
      this.guiLabel = guiLabel;
    }

    static TiebreakMode getByInternalLabel(String labelLookup) {
      return Arrays.stream(TiebreakMode.values())
          .filter(v -> v.internalLabel.equals(labelLookup))
          .findAny()
          .orElse(MODE_UNKNOWN);
    }

    @Override
    public String toString() {
      return guiLabel;
    }

    public String getInternalLabel() {
      return internalLabel;
    }
  }

  enum WinnerElectionMode {
    STANDARD_SINGLE_WINNER("singleWinnerMajority", "Single-winner majority determines winner"),
    MULTI_SEAT_ALLOW_ONLY_ONE_WINNER_PER_ROUND(
        "multiWinnerAllowOnlyOneWinnerPerRound", "Multi-winner allow only one winner per round"),
    MULTI_SEAT_ALLOW_MULTIPLE_WINNERS_PER_ROUND(
        "multiWinnerAllowMultipleWinnersPerRound", "Multi-winner allow multiple winners per round"),
    MULTI_SEAT_BOTTOMS_UP_UNTIL_N_WINNERS("bottomsUp", "Bottoms-up"),
    MULTI_SEAT_BOTTOMS_UP_USING_PERCENTAGE_THRESHOLD(
        "bottomsUpUsingPercentageThreshold", "Bottoms-up using percentage threshold"),
    MULTI_SEAT_SEQUENTIAL_WINNER_TAKES_ALL("multiPassIrv", "Multi-pass IRV"),
    MODE_UNKNOWN("modeUnknown", "Unknown mode");

    private final String internalLabel;
    private final String guiLabel;

    WinnerElectionMode(String internalLabel, String guiLabel) {
      this.internalLabel = internalLabel;
      this.guiLabel = guiLabel;
    }

    static WinnerElectionMode getByInternalLabel(String labelLookup) {
      return Arrays.stream(WinnerElectionMode.values())
          .filter(v -> v.internalLabel.equals(labelLookup))
          .findAny()
          .orElse(MODE_UNKNOWN);
    }

    @Override
    public String toString() {
      return guiLabel;
    }

    public String getInternalLabel() {
      return internalLabel;
    }
  }

  enum CandidateStatus {
    CONTINUING,
    WINNER,
    ELIMINATED,
    INVALID,
    EXCLUDED,
  }

  /**
   * A wrapper around Map&lt;Integer, RoundTally&gt;
   * A mapping between a round number and that round's corresponding RoundTally.
   */
  static class RoundTallies {
    private final Map<Integer, RoundTally> roundTallies = new HashMap<>();

    public void put(Integer roundNumber, RoundTally roundTally) {
      roundTallies.put(roundNumber, roundTally);
    }

    public RoundTally get(Integer roundNumber) {
      return roundTallies.get(roundNumber);
    }
  }

  /**
   * A wrapper around Map&lt;TabulateBySlice, Map&lt;String, T&gt;&gt;
   * Breaks down the templated type T by a specific slice.
   * The slice type is the outer map (TabulateBySlice), and the slice ID is the inner map (String).
   */
  static class BreakdownBySlice<T> {
    private final Map<ContestConfig.TabulateBySlice, Map<String, T>> breakdownBySlice
            = new HashMap<>();

    public T initialize(ContestConfig.TabulateBySlice slice, String sliceId, T t) {
      return breakdownBySlice.computeIfAbsent(slice, k -> new HashMap<>()).put(sliceId, t);
    }

    public Map<String, T> get(ContestConfig.TabulateBySlice slice) {
      return breakdownBySlice.get(slice);
    }

    public T get(ContestConfig.TabulateBySlice slice, String sliceId) {
      return get(slice).get(sliceId);
    }
  }

  /**
   * A wrapper around Map&lt;TabulateBySlice, Set&lt;String&gt;&gt;
   * Maps a TabulateBySlice to the set of corresponding slice IDs.
   */
  static class SliceIdSet {
    private final Map<ContestConfig.TabulateBySlice, Set<String>> sliceIds = new HashMap<>();

    public void initialize(ContestConfig.TabulateBySlice slice) {
      sliceIds.put(slice, new HashSet<>());
    }

    public void add(TabulateBySlice slice, String sliceId) {
      sliceIds.get(slice).add(sliceId);
    }

    public boolean isEmpty(ContestConfig.TabulateBySlice slice) {
      return sliceIds.get(slice).isEmpty();
    }

    // Add an enumerator for each Set<String> of a given slice
    public Set<String> get(ContestConfig.TabulateBySlice slice) {
      return sliceIds.get(slice);
    }

    public int size(ContestConfig.TabulateBySlice slice) {
      return sliceIds.get(slice).size();
    }
  }

  /**
   * Container class used during batch elimination to store the results for later logging output.
   *
   * @param candidateId the candidate eliminated
   * @param runningTotal how many total votes we'd seen at the step of batch elimination when we
   *     added this candidate
   * @param nextLowestTally next-lowest count total (validates that we were correctly
   *     batch-eliminated)
   */
  record BatchElimination(
      String candidateId, BigDecimal runningTotal, BigDecimal nextLowestTally) {}

  static class TabulationAbortedException extends Exception {

    final boolean cancelledByUser;

    TabulationAbortedException(boolean cancelledByUser) {
      this.cancelledByUser = cancelledByUser;
    }

    @Override
    public String getMessage() {
      return cancelledByUser
          ? "Tabulation was cancelled by the user!"
          : "Tabulation was cancelled due to a problem with the input data or config.";
    }
  }
}
