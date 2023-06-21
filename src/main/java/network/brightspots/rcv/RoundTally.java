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
  private BigDecimal numActiveBallots;
  private BigDecimal numInactiveBallots;

  private boolean isFinalized = false;
  private boolean unlockedForSurplusCalculation = false;

  RoundTally(int roundNumber, Stream<String> candidateNames) {
    this.roundNumber = roundNumber;
    candidateTallies = new HashMap<>();
    candidateNames.forEach((String candidateName) -> {
      candidateTallies.put(candidateName, BigDecimal.ZERO);
    });

    ballotStatusTallies = new HashMap<>();
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

  // Surplus computation requires both reading and writing -- temporarily allow that
  void unlockForSurplusCalculation() {
    unlockedForSurplusCalculation = true;
  }

  // Revert to standard functionality when surplus computation is done
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
  BigDecimal addToCandidateTally(String candidateId, BigDecimal tally) {
    ensureNotFinalized();
    addBallotWithStatus(StatusForRound.ACTIVE, tally);
    return candidateTallies.put(candidateId, candidateTallies.get(candidateId).add(tally));
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
    candidateTallies.put(candidateId, tally);
  }

  // Adds to the votes for this candidate
  BigDecimal addInactiveBallot(StatusForRound statusForRound, BigDecimal value) {
    if (statusForRound == StatusForRound.ACTIVE) {
      throw new RuntimeException("Cannot add an active ballot as inactive");
    }
    return addBallotWithStatus(statusForRound, value);
  }

  // Adds to the votes for this candidate
  private BigDecimal addBallotWithStatus(StatusForRound statusForRound, BigDecimal value) {
    ensureNotFinalized();
    BigDecimal newVal = ballotStatusTallies.get(statusForRound).add(value);
    return ballotStatusTallies.put(statusForRound, newVal);
  }

  // Get the number of inactive ballots by type
  BigDecimal getBallotStatusTally(StatusForRound statusForRound) {
    ensureFinalized();
    return ballotStatusTallies.get(statusForRound);
  }

  // Get the number af active ballots in this round
  BigDecimal numActiveBallots() {
    ensureFinalized();
    return numActiveBallots;
  }

  // Get the number af active ballots in this round
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

  // return a list of all candidates, if any, with votes greater than the given threshold
  public List<String> getCandidatesWithMoreVotesThan(BigDecimal threshold) {
    ensureFinalized();

    return getCandidates().stream().filter(
        candidate -> getCandidateTally(candidate).compareTo(threshold) > 0).toList();
  }

  // return a list of all input candidates sorted from the highest tally to lowest
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
    ballotStatusTallies.forEach((statusForRound, tally) -> {
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
