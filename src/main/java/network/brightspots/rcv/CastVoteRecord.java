/*
 * RCTab
 * Copyright (c) 2017-2023 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: Internal representation of a single cast vote record, including to whom it counts, over
 * the course of a tabulation (can be multiple candidates for a multi-winner election).
 * Design: Simple container class for individual CVR data and associated utils.
 * Conditions: Always.
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import static network.brightspots.rcv.Utils.isNullOrBlank;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javafx.util.Pair;

class CastVoteRecord {
  // StatusForRound represents the ballot's status on a given round.
  // This CastVoteRecord will have different statuses each round,
  // and this provides a more detailed breakdown than a simple
  // active/inactive binary. It is only useful in results reporting;
  // as far as tabulation is concerned, all that matters is whether
  // it is active or not.
  enum StatusForRound {
    // The candidate just received this CVR's vote
    ACTIVE_JUST_RECEIVED,
    // This CVR voted for this candidate in a previous round
    ACTIVE_REMAINED_ON_CANDIDATE,
    // The following fields represent inactive ballots' status
    INACTIVE_BY_UNDERVOTE,
    INACTIVE_BY_OVERVOTE,
    INACTIVE_BY_SKIPPED_RANKING,
    INACTIVE_BY_REPEATED_RANKING,
    INACTIVE_BY_EXHAUSTED_CHOICES
  }

  // computed unique ID for this CVR (source file + line number)
  private final String computedId;
  // supplied unique ID for this CVR
  private final String suppliedId;
  // which precinct this ballot came from
  private final String precinct;
  // which precinct portion this ballot came from
  private final String precinctPortion;
  // records winners to whom some fraction of this vote has been allocated
  private final Map<String, BigDecimal> winnerToFractionalValue = new HashMap<>();
  // If CVR CDF output is enabled, we store the necessary info here: for each round, the list of
  // candidates this ballot is counting toward (0 or 1 in a single-seat contest; 0 to n in a
  // multi-seat contest because of fractional vote transfers), and how much of the vote each is
  // getting. As a memory optimization, if the data is unchanged from the previous round, we don't
  // add a new entry.
  private final Map<Integer, List<Pair<String, BigDecimal>>> cdfSnapshotData = new HashMap<>();
  // map of round to all candidates selected for that round
  // a set is used to handle overvotes
  CandidateRankingsList candidateRankings;
  // contest associated with this CVR
  private String contestId;
  // tabulatorId parsed from Dominion CVR data
  private String tabulatorId;
  // batchId parsed from Dominion CVR data
  private String batchId;
  // ballotTypeId parsed from Dominion CVR data
  private String ballotTypeId;
  // whether this CVR is exhausted or not. This will change as tabulation progresses.
  private StatusForRound currentRoundStatus = StatusForRound.ACTIVE_JUST_RECEIVED;
  // tells us which candidate is currently receiving this CVR's vote (or fractional vote)
  private String currentRecipientOfVote = null;

  CastVoteRecord(
      String contestId,
      String tabulatorId,
      String batchId,
      String suppliedId,
      String precinct,
      String precinctPortion,
      String ballotTypeId,
      List<Pair<Integer, String>> rankings) {
    this.contestId = contestId;
    this.tabulatorId = tabulatorId;
    this.batchId = batchId;
    this.computedId = null;
    this.suppliedId = suppliedId;
    this.precinct = precinct;
    this.precinctPortion = precinctPortion;
    this.ballotTypeId = ballotTypeId;
    this.candidateRankings = new CandidateRankingsList(rankings);
  }

  CastVoteRecord(
      String computedId,
      String suppliedId,
      String precinct,
      List<Pair<Integer, String>> rankings) {
    this.computedId = computedId;
    this.suppliedId = suppliedId;
    this.precinct = precinct;
    this.precinctPortion = null;
    this.candidateRankings = new CandidateRankingsList(rankings);
  }

  String getContestId() {
    return contestId;
  }

  String getTabulatorId() {
    return tabulatorId;
  }

  String getBatchId() {
    return batchId;
  }

  String getBallotTypeId() {
    return ballotTypeId;
  }

  String getPrecinct() {
    return precinct;
  }

  String getPrecinctPortion() {
    return precinctPortion;
  }

  String getId() {
    return suppliedId != null ? suppliedId : computedId;
  }

  // logs the outcome for this CVR for this round for auditing purposes
  void logRoundOutcome(
      int round, VoteOutcomeType outcomeType, String detail, BigDecimal fractionalTransferValue) {

    StringBuilder logStringBuilder = new StringBuilder();
    logStringBuilder.append("[Round] ").append(round).append(" [CVR] ");
    if (!isNullOrBlank(suppliedId)) {
      logStringBuilder.append(suppliedId);
    } else {
      logStringBuilder.append(computedId);
    }
    if (outcomeType == VoteOutcomeType.IGNORED) {
      logStringBuilder.append(" [was ignored] ");
    } else if (outcomeType == VoteOutcomeType.EXHAUSTED) {
      logStringBuilder.append(" [became inactive] ");
    } else {
      if (round == 1) {
        logStringBuilder.append(" [counted for] ");
      } else {
        logStringBuilder.append(" [transferred to] ");
      }
    }
    logStringBuilder.append(detail);

    // add vote value if not 1
    if (fractionalTransferValue != null && !fractionalTransferValue.equals(BigDecimal.ONE)) {
      logStringBuilder.append(" [value] ").append(fractionalTransferValue);
    }

    Logger.fine(logStringBuilder.toString());
  }

  Map<Integer, List<Pair<String, BigDecimal>>> getCdfSnapshotData() {
    return cdfSnapshotData;
  }

  // store info needed to generate the CVR JSON snapshots in Common Data Format
  void logCdfSnapshotData(int round) {
    List<Pair<String, BigDecimal>> data = new LinkedList<>();
    for (Entry<String, BigDecimal> entry : winnerToFractionalValue.entrySet()) {
      data.add(new Pair<>(entry.getKey(), entry.getValue()));
    }
    if (currentRecipientOfVote != null) {
      data.add(new Pair<>(currentRecipientOfVote, getFractionalTransferValue()));
    }

    cdfSnapshotData.put(round, data);
  }

  void exhaustBy(StatusForRound status) {
    this.currentRoundStatus = status;
  }

  boolean isExhausted() {
    return currentRoundStatus != StatusForRound.ACTIVE_JUST_RECEIVED
        && currentRoundStatus != StatusForRound.ACTIVE_REMAINED_ON_CANDIDATE;
  }

  StatusForRound getBallotStatus() {
    return currentRoundStatus;
  }

  // fractional transfer value is one by default but can be less if this
  // CVR already helped elect winner(s) (multi-winner contest only)
  BigDecimal getFractionalTransferValue() {
    BigDecimal remainingValue = BigDecimal.ONE;
    for (BigDecimal allocatedValue : winnerToFractionalValue.values()) {
      remainingValue = remainingValue.subtract(allocatedValue);
    }
    return remainingValue;
  }

  // calculate and store new vote value for current (newly elected) recipient
  // param: surplusFraction fraction of this vote's current value which is now surplus and will
  // be transferred
  void recordCurrentRecipientAsWinner(BigDecimal surplusFraction, ContestConfig config) {
    // Calculate transfer amount rounding DOWN to ensure we leave more of the vote with
    // the winner. This avoids transferring more than intended which could leave the winner with
    // less than the winning threshold.
    BigDecimal transferAmount = config.multiply(getFractionalTransferValue(), surplusFraction);
    // calculate newAllocatedValue counted to the current winner and store it
    BigDecimal newAllocatedValue = getFractionalTransferValue().subtract(transferAmount);
    winnerToFractionalValue.put(getCurrentRecipientOfVote(), newAllocatedValue);
  }

  String getCurrentRecipientOfVote() {
    return currentRecipientOfVote;
  }

  void setCurrentRecipientOfVote(String currentRecipientOfVote) {
    this.currentRecipientOfVote = currentRecipientOfVote;
  }

  Map<String, BigDecimal> getWinnerToFractionalValue() {
    return winnerToFractionalValue;
  }

  enum VoteOutcomeType {
    COUNTED,
    IGNORED,
    EXHAUSTED,
  }

  static class CvrParseException extends Exception {

  }
}
