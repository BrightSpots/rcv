/*
 * Created by Jonathan Moldover, Louis Eisenberg, and Hylton Edingfield
 * Copyright 2018 Bright Spots
 * Purpose:
 * internal representation of a single cast vote record including rankings ID and
 * state (exhausted or not).  Conceptually this is a ballot.
 * Version: 1.0
 */

package com.rcv;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
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
  // contains what happened to this CVR in each round
  private final List<VoteOutcome> roundOutcomes = new LinkedList<>();
  // map of round to all candidates selected for that round
  // a set is used to handle overvotes
  SortedMap<Integer, Set<String>> rankToCandidateIDs;
  // whether this CVR is exhausted or not
  private boolean isExhausted;
  // records winners to whom some fraction of this vote has been allocated
  private Map<String, BigDecimal> winnerToFractionalValue = new HashMap<>();
  // tells us which candidate is currently receiving this CVR's vote (or fractional vote)
  private String currentRecipientOfVote = null;

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
      List<Pair<Integer, String>> rankings
  ) {
    this.computedID = computedID;
    this.suppliedID = suppliedID;
    this.precinct = precinct;
    this.fullCVRData = fullCVRData;
    sortRankings(rankings);
  }

  // function: addRoundOutcome
  // purpose: adds the outcome for this CVR for this round (for auditing purposes)
  // param: outcomeType indicates what happened
  // param: detail reflects who (if anyone) received the vote or why it was exhausted/ignored
  // param: fractionalTransferValue if someone received the vote (not exhausted/ignored)
  void addRoundOutcome(
      VoteOutcomeType outcomeType,
      String detail,
      BigDecimal fractionalTransferValue
  ) {
    roundOutcomes.add(new VoteOutcome(outcomeType, detail, fractionalTransferValue));
  }

  // function: exhaust
  // purpose: transition the CVR into exhausted state with the given reason
  // param: round the exhaustion occurs
  // param: reason: the reason for exhaustion
  void exhaust(String reason) {
    assert !isExhausted;
    isExhausted = true;
    addRoundOutcome(VoteOutcomeType.EXHAUSTED, reason, null);
  }

  // function: isExhausted
  // purpose: getter for exhausted state
  // returns: true if CVR is exhausted otherwise false
  boolean isExhausted() {
    return isExhausted;
  }

  // function: getFractionalTransferValue
  // purpose: getter for fractionalTransferValue
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
  // purpose: stores the current recipient as a winner using the specified surplus fraction
  void recordCurrentRecipientAsWinner(BigDecimal surplusFraction) {
    // take the current FTV of this vote and allocate (1 - surplusFraction) of that amount to the
    // new winner
    BigDecimal newAllocatedValue =
        getFractionalTransferValue().multiply(BigDecimal.ONE.subtract(surplusFraction));
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
      Set<String> candidatesAtRank = rankToCandidateIDs.get(ranking.getKey());
      if (candidatesAtRank == null) {
        // create the new optionsAtRank and add to the sorted CVR
        candidatesAtRank = new HashSet<>();
        rankToCandidateIDs.put(ranking.getKey(), candidatesAtRank);
      }
      // add this option into the map
      candidatesAtRank.add(ranking.getValue());
    }
  }

  // function: getAuditString
  // purpose: return a formatted string describing this CVR and how state changes over
  //  the course of the tabulation.  Used for audit output.
  // returns: the formatted string for audit output
  String getAuditString() {
    // use a string builder for more efficient string creation
    StringBuilder auditStringBuilder = new StringBuilder();
    auditStringBuilder.append(" [Computed ID] ");
    auditStringBuilder.append(computedID);
    auditStringBuilder.append(" [Supplied ID] ");
    auditStringBuilder.append(suppliedID);
    if (precinct != null) {
      auditStringBuilder.append(" [Precinct] ");
      auditStringBuilder.append(precinct);
    }
    auditStringBuilder.append(" [Round by Round Report] |");
    // round is an index to iterate over all round outcomes
    int round = 1;
    for (VoteOutcome roundOutcome : roundOutcomes) {
      auditStringBuilder.append(round).append('|');
      if (roundOutcome.outcomeType == VoteOutcomeType.IGNORED) {
        auditStringBuilder.append("ignored:");
      } else if (roundOutcome.outcomeType == VoteOutcomeType.EXHAUSTED) {
        auditStringBuilder.append("exhausted:");
      }
      auditStringBuilder.append(roundOutcome.detail);
      // the fractional transfer value of the vote in this round
      BigDecimal ftv = roundOutcome.fractionalTransferValue;
      if (ftv != null && !ftv.equals(BigDecimal.ONE)) {
        auditStringBuilder.append(" (").append(ftv).append(')');
      }
      auditStringBuilder.append('|');
      round++;
    }
    auditStringBuilder.append(" [Raw Data] ");
    auditStringBuilder.append(fullCVRData);
    return auditStringBuilder.toString();
  }

  enum VoteOutcomeType {
    COUNTED,
    IGNORED,
    EXHAUSTED,
  }

  private class VoteOutcome {

    // what type of outcome (counted, ignored, exhausted)
    VoteOutcomeType outcomeType;
    // more detail on the outcome (who got the vote or why it was ignored/exhausted)
    String detail;
    // if someone received the vote, what fraction of it they got
    BigDecimal fractionalTransferValue;

    VoteOutcome(VoteOutcomeType outcomeType, String detail, BigDecimal fractionalTransferValue) {
      this.outcomeType = outcomeType;
      this.detail = detail;
      this.fractionalTransferValue = fractionalTransferValue;
    }
  }
}
