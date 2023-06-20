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
  private final Map<CastVoteRecord.BallotStatus, BigDecimal> ballotStatusTallies;
  private BigDecimal numBallots;

  private boolean isFinalized = false;
  private boolean unlockedForSurplusCalculation = false;

  RoundTally(int roundNumber, Stream<String> candidateNames) {
    this.roundNumber = roundNumber;
    candidateTallies = new HashMap<>();
    candidateNames.forEach((String candidateName) -> {
      candidateTallies.put(candidateName, BigDecimal.ZERO);
    });

    ballotStatusTallies = new HashMap<>();
    for (CastVoteRecord.BallotStatus ballotStatus : CastVoteRecord.BallotStatus.values()) {
      ballotStatusTallies.put(ballotStatus, BigDecimal.ZERO);
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
    this.numBallots = countBallots();
  }

  // Surplus computation requires both reading and writing -- temporarily allow that
  void unlockForSurplusCalculation() {
    unlockedForSurplusCalculation = true;
  }

  // Revert to standard functionality when surplus computation is done
  void relockAfterSurplusCalculation() {
    unlockedForSurplusCalculation = false;
    this.numBallots = countBallots();
  }

  // Get the number of votes this candidate has this round
  BigDecimal getCandidateTally(String candidateId) {
    ensureFinalized();
    return candidateTallies.get(candidateId);
  }

  // Adds to the votes for this candidate
  BigDecimal addToCandidateTally(String candidateId, BigDecimal tally) {
    ensureNotFinalized();
    return candidateTallies.put(candidateId, candidateTallies.get(candidateId).add(tally));
  }

  // Sets the number of votes this candidate has this round
  BigDecimal setCandidateTally(String candidateId, BigDecimal tally) {
    ensureNotFinalized();
    return candidateTallies.put(candidateId, tally);
  }

  // Adds to the votes for this candidate
  BigDecimal addBallotWithStatus(CastVoteRecord.BallotStatus ballotStatus) {
    ensureNotFinalized();
    BigDecimal newVal = ballotStatusTallies.get(ballotStatus).add(BigDecimal.ONE);
    return ballotStatusTallies.put(ballotStatus, newVal);
  }

  // Get the number of inactive ballots by type
  BigDecimal getBallotStatusTally(CastVoteRecord.BallotStatus ballotStatus) {
    ensureFinalized();
    return ballotStatusTallies.get(ballotStatus);
  }

  // Get the number af active ballots in this round
  BigDecimal numActiveBallots() {
    ensureFinalized();
    return numBallots;
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

  private BigDecimal countBallots() {
    BigDecimal total = BigDecimal.ZERO;
    for (BigDecimal tally : candidateTallies.values()) {
      total = total.add(tally);
    }
    return total;
  }

  private void ensureFinalized() {
    if (!isFinalized && !unlockedForSurplusCalculation) {
      throw new RuntimeException("Cannot retrieve data until round is finalized.");
    }
  }

  private void ensureNotFinalized() {
    if (isFinalized && !unlockedForSurplusCalculation) {
      throw new RuntimeException("Cannot set data after round is finalized.");
    }
  }
}
