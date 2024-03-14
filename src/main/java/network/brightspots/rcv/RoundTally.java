/*
 * RCTab
 * Copyright (c) 2017-2023 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: Contain all the information about what happened during a round, including
 * the number of votes each candidate received, the number of ballots which were inactive,
 * and how they became inactive. Includes functionality to build a round as CVRs are parsed.
 * Design: Maps from candidate name to number of votes received, and from ballot status to
 * number of ballots with that status on this round.
 * Conditions: During tabulation, validation, conversion, and results writing.
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import static network.brightspots.rcv.CastVoteRecord.StatusForRound;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

class RoundTally {
  private final int roundNumber;
  private final Map<String, BigDecimal> candidateTallies;
  private final Map<StatusForRound, BigDecimal> ballotStatusTallies;
  private BigDecimal winningThreshold;
  private BigDecimal numActiveBallots;
  private BigDecimal numInactiveBallots;
  private BigDecimal numLockedInBallots;

  private boolean isFinalized = false;
  private boolean unlockedForSurplusCalculation = false;

  RoundTally(int roundNumber, Stream<String> candidateNames) {
    this.roundNumber = roundNumber;
    candidateTallies = new HashMap<>();
    candidateNames.forEach(
        (String candidateName) -> candidateTallies.put(candidateName, BigDecimal.ZERO));

    ballotStatusTallies = new HashMap<>();
    numLockedInBallots = BigDecimal.ZERO;
    for (StatusForRound statusForRound : StatusForRound.values()) {
      ballotStatusTallies.put(statusForRound, BigDecimal.ZERO);
    }
  }

  int getRoundNumber() {
    return roundNumber;
  }

  // Call this to prevent future modifications to the round, and allow retrieval of data
  // from the round.
  void lockInRound() {
    ensureNotFinalized();
    isFinalized = true;
    countBallots();
  }

  // Surplus computation allows writing to the tally after the round is locked in, and
  // it does not affect the number of ACTIVE ballots when adjusted.
  void unlockForSurplusCalculation() {
    unlockedForSurplusCalculation = true;
  }

  // Close the surplus calculation window and recompute ballot totals.
  void relockAfterSurplusCalculation() {
    unlockedForSurplusCalculation = false;
    countBallots();
  }

  // Get the number of votes this candidate has this round
  BigDecimal getCandidateTally(String candidateId) {
    ensureFinalized();
    return candidateTallies.get(candidateId);
  }

  // Adds to the votes for this candidate
  void addToCandidateTally(String candidateId, BigDecimal tally) {
    ensureNotFinalized();
    addBallotWithStatus(StatusForRound.ACTIVE, tally);
    candidateTallies.put(candidateId, candidateTallies.get(candidateId).add(tally));
  }

  // Adds votes without adjusting the number of BallotStatus.ACTIVE ballots.
  void addToCandidateTallyViaSurplusAdjustment(String candidateId, BigDecimal tally) {
    ensureIsMakingSurplusAdjustment();
    BigDecimal prevTally = candidateTallies.get(candidateId);
    setCandidateTallyViaSurplusAdjustment(candidateId, prevTally.add(tally));
  }

  // Sets vote totals without adjusting the number of BallotStatus.ACTIVE ballots.
  void setCandidateTallyViaSurplusAdjustment(String candidateId, BigDecimal tally) {
    ensureIsMakingSurplusAdjustment();
    BigDecimal diff = tally.subtract(candidateTallies.getOrDefault(candidateId, BigDecimal.ZERO));
    candidateTallies.put(candidateId, tally);

    // We don't add to BallotStatus.ACTIVE, but we still need to track this to get
    // the correct percentages when reporting results externally.
    numLockedInBallots = numLockedInBallots.add(diff);
  }

  // Gets the winning threshold for this round.
  BigDecimal getWinningThreshold() {
    return winningThreshold;
  }

  // Sets the winning threshold for this round.
  void setWinningThreshold(BigDecimal winningThreshold) {
    this.winningThreshold = winningThreshold;
  }

  // Adds to the count of inactive ballots
  void addInactiveBallot(StatusForRound statusForRound, BigDecimal value) {
    ensureNotFinalized();
    if (statusForRound == StatusForRound.ACTIVE) {
      throw new RuntimeException("Cannot add an active ballot as inactive");
    }
    addBallotWithStatus(statusForRound, value);
  }

  // Adds to the votes for this round
  private void addBallotWithStatus(StatusForRound statusForRound, BigDecimal value) {
    ensureNotFinalized();
    BigDecimal newVal = ballotStatusTallies.get(statusForRound).add(value);
    ballotStatusTallies.put(statusForRound, newVal);
  }

  // Get the number of inactive ballots by type
  BigDecimal getBallotStatusTally(StatusForRound statusForRound) {
    ensureFinalized();
    return ballotStatusTallies.get(statusForRound);
  }

  // Get the number of active ballots in this round
  BigDecimal numActiveBallots() {
    ensureFinalized();
    return numActiveBallots;
  }

  // Get the number of active ballots plus the fractional amount of "locked in" ballots.
  BigDecimal numActiveOrLockedInBallots() {
    ensureFinalized();
    return numActiveBallots.add(numLockedInBallots);
  }

  // Get the number of inactive ballots in this round
  BigDecimal numInactiveBallots() {
    ensureFinalized();
    return numInactiveBallots;
  }

  // Get all candidate names
  public Set<String> getCandidates() {
    return candidateTallies.keySet();
  }

  // Get all candidate names
  public int numActiveCandidates() {
    ensureFinalized();
    return candidateTallies.size();
  }

  // Return a list of all candidates, if any, with votes greater than the given threshold
  public List<String> getCandidatesWithMoreVotesThan(BigDecimal threshold) {
    ensureFinalized();
    return getCandidates().stream()
        .filter(candidate -> getCandidateTally(candidate).compareTo(threshold) > 0)
        .toList();
  }

  // Return a list of all input candidates sorted from the highest tally to lowest
  public List<String> getSortedCandidatesByTally() {
    ensureFinalized();
    List<Map.Entry<String, BigDecimal>> entries = new ArrayList<>(candidateTallies.entrySet());
    entries.sort(
        (firstObject, secondObject) -> {
          int ret;
          if (firstObject.getKey().equals(Tabulator.UNDECLARED_WRITE_IN_OUTPUT_LABEL)) {
            ret = 1;
          } else if (secondObject.getKey().equals(Tabulator.UNDECLARED_WRITE_IN_OUTPUT_LABEL)) {
            ret = -1;
          } else {
            ret = (secondObject.getValue()).compareTo(firstObject.getValue());
          }
          return ret;
        });
    List<String> sortedCandidates = new LinkedList<>();
    for (var entry : entries) {
      sortedCandidates.add(entry.getKey());
    }
    return sortedCandidates;
  }

  private void countBallots() {
    numInactiveBallots = BigDecimal.ZERO;
    ballotStatusTallies.forEach(
        (statusForRound, tally) -> {
          if (statusForRound != StatusForRound.ACTIVE) {
            numInactiveBallots = numInactiveBallots.add(tally);
          }
        });

    numActiveBallots = ballotStatusTallies.get(StatusForRound.ACTIVE);
  }

  private void ensureFinalized() {
    if (!isFinalized) {
      throw new RuntimeException("Cannot retrieve data until round is finalized.");
    }
  }

  private void ensureNotFinalized() {
    if (isFinalized) {
      throw new RuntimeException("Cannot set data after round is finalized.");
    }
  }

  private void ensureIsMakingSurplusAdjustment() {
    if (!unlockedForSurplusCalculation) {
      throw new RuntimeException("This action is only available during surplus adjustment.");
    }
  }
}
