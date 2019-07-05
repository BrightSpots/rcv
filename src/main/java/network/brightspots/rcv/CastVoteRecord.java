/*
 * Ranked Choice Voting Universal Tabulator
 * Copyright (c) 2017-2019 Bright Spots Developers.
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
 */

/*
 * Purpose:
 * Internal representation of a single cast vote record including rankings ID and state (exhausted
 * or not). Conceptually this is a ballot.
 */

package network.brightspots.rcv;

import static network.brightspots.rcv.Utils.isNullOrBlank;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import javafx.util.Pair;

class CastVoteRecord {

  // computed unique ID for this CVR (source file + line number)
  private final String computedID;
  // supplied unique ID for this CVR
  private final String suppliedID;
  // which precinct this ballot came from
  private final String precinct;
  // container for ALL CVR data parsed from the source CVR file
  private final List<String> fullCVRData;
  // records winners to whom some fraction of this vote has been allocated
  private final Map<String, BigDecimal> winnerToFractionalValue = new HashMap<>();
  // map of round to all candidates selected for that round
  // a set is used to handle overvotes
  SortedMap<Integer, Set<String>> rankToCandidateIDs;
  // whether this CVR is exhausted or not
  private boolean isExhausted;
  // tells us which candidate is currently receiving this CVR's vote (or fractional vote)
  private String currentRecipientOfVote = null;
  // If CVR CDF output is enabled, we store the necessary info here: for each round, the list of
  // candidates this ballot is counting toward (0 or 1 in a single-seat contest; 0 to n in a
  // multi-seat contest because of fractional vote transfers), and how much of the vote each is
  // getting. As a memory optimization, if the data is unchanged from the previous round, we don't
  // add a new entry.
  private final Map<Integer, List<Pair<String, BigDecimal>>> cdfSnapshotData = new HashMap<>();

  // function: CastVoteRecord
  // purpose: create a new CVR object
  // param: computedID is our computed unique ID for this CVR
  // param: suppliedID is the (ostensibly unique) ID from the input data
  // param: rankings list of rank->candidateID selections parsed for this CVR
  // param: fullCVRData list of strings containing ALL data parsed for this CVR
  CastVoteRecord(
      String computedID,
      String suppliedID,
      String precinct,
      List<String> fullCVRData,
      List<Pair<Integer, String>> rankings) {
    this.computedID = computedID;
    this.suppliedID = suppliedID;
    this.precinct = precinct;
    this.fullCVRData = fullCVRData;
    sortRankings(rankings);
  }

  String getID() {
    return suppliedID != null ? suppliedID : computedID;
  }

  // function: logRoundOutcome
  // purpose: logs the outcome for this CVR for this round for auditing purposes
  // param: outcomeType indicates what happened
  // param: detail reflects who received the vote OR why it was exhausted/ignored
  // param: fractionalTransferValue if someone received the vote (not exhausted/ignored)
  void logRoundOutcome(
      int round, VoteOutcomeType outcomeType, String detail, BigDecimal fractionalTransferValue) {

    StringBuilder logStringBuilder = new StringBuilder();
    // add round and ID
    logStringBuilder.append("[Round] ").append(round).append(" [CVR] ");
    if (!isNullOrBlank(suppliedID)) {
      logStringBuilder.append(suppliedID);
    } else {
      logStringBuilder.append(computedID);
    }
    // add outcome type
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
    // add detail: either candidate ID or more explanation for other outcomes
    logStringBuilder.append(detail);

    // add fractional transfer value of the vote if it is fractional
    if (fractionalTransferValue != null && !fractionalTransferValue.equals(BigDecimal.ONE)) {
      logStringBuilder.append(" [value] ").append(fractionalTransferValue.toString());
    }

    // add complete data for round 1 only
    if (round == 1) {
      logStringBuilder.append(" [Raw Data] ");
      logStringBuilder.append(fullCVRData);
    }

    // output with level FINE routes to audit log
    Logger.log(Level.FINE, logStringBuilder.toString());
  }

  Map<Integer, List<Pair<String, BigDecimal>>> getCdfSnapshotData() {
    return cdfSnapshotData;
  }

  // purpose: store info that we'll need in order to generate the CVR JSON snapshots in the Common
  // Data Format at the end of the tabulation (if this option is enabled)
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

  // function: exhaust
  // purpose: transition the CVR into exhausted state
  void exhaust() {
    assert !isExhausted;
    isExhausted = true;
  }

  // function: isExhausted
  // purpose: getter for exhausted state
  // returns: true if CVR is exhausted otherwise false
  boolean isExhausted() {
    return isExhausted;
  }

  // function: getFractionalTransferValue
  // purpose: getter for fractionalTransferValue
  // the FTV for this cast vote record (by default the FTV is exactly one vote, but it
  // could be less in a multi-winner contest if this CVR already helped elect a winner)
  // returns: value of field
  BigDecimal getFractionalTransferValue() {
    // remainingValue starts at one, and we subtract all the parts that are already allocated
    BigDecimal remainingValue = BigDecimal.ONE;
    for (BigDecimal allocatedValue : winnerToFractionalValue.values()) {
      remainingValue = remainingValue.subtract(allocatedValue);
    }
    return remainingValue;
  }

  // function: recordCurrentRecipientAsWinner
  // purpose: calculate and store new vote value for current (newly elected) recipient
  // param: surplusFraction fraction of this vote's current value which is now surplus and will
  // be transferred
  // param: config used for vote math
  void recordCurrentRecipientAsWinner(BigDecimal surplusFraction, ContestConfig config) {
    // Calculate transfer amount rounding DOWN to ensure we leave more of the vote with
    // the winner. This avoids transferring more than intended which could leave the winner with
    // less than the winning threshold.
    BigDecimal transferAmount = config.multiply(getFractionalTransferValue(), surplusFraction);
    // calculate newAllocatedValue counted to the current winner and store it
    BigDecimal newAllocatedValue = getFractionalTransferValue().subtract(transferAmount);
    winnerToFractionalValue.put(getCurrentRecipientOfVote(), newAllocatedValue);
  }

  // function: getCurrentRecipientOfVote
  // purpose: getter for currentRecipientOfVote
  // returns: value of field
  String getCurrentRecipientOfVote() {
    return currentRecipientOfVote;
  }

  // function: setCurrentRecipientOfVote
  // purpose: setter for currentRecipientOfVote
  // param: new value of field
  void setCurrentRecipientOfVote(String currentRecipientOfVote) {
    this.currentRecipientOfVote = currentRecipientOfVote;
  }

  // function: getPrecinct
  // purpose: getter for precinct
  // returns: value of field
  String getPrecinct() {
    return precinct;
  }

  // function: getWinnerToFractionalValue
  // purpose: getter for winnerToFractionalValue
  // returns: value of field
  Map<String, BigDecimal> getWinnerToFractionalValue() {
    return winnerToFractionalValue;
  }

  // function: sortRankings
  // purpose: create a map of ranking to candidates selected at that rank
  // param: rankings list of rankings (rank, candidateID pairs) to be sorted
  private void sortRankings(List<Pair<Integer, String>> rankings) {
    rankToCandidateIDs = new TreeMap<>();
    // index for iterating over all rankings
    for (Pair<Integer, String> ranking : rankings) {
      // set of candidates given this rank
      Set<String> candidatesAtRank =
          rankToCandidateIDs.computeIfAbsent(ranking.getKey(), k -> new HashSet<>());
      // create the new optionsAtRank and add to the sorted CVR
      // add this option into the map
      candidatesAtRank.add(ranking.getValue());
    }
  }

  enum VoteOutcomeType {
    COUNTED,
    IGNORED,
    EXHAUSTED,
  }
}
