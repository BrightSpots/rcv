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
import java.math.BigInteger;
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

  // name of the vendor, this becomes part of the audit output but is not used in tabulation
  private final String sourceName;
  // unique identifier for this cast vote record
  private final String cvrID;
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
  // For multi-winner elections that use fractional vote transfers, this represents the current
  // fractional value of this CVR.
  private BigDecimal fractionalTransferValue = new BigDecimal(BigInteger.ONE);
  // tells us which candidate is currently receiving this CVR's vote (or fractional vote)
  private String currentRecipientOfVote = null;

  // function: CastVoteRecord
  // purpose: create a new CVR object
  // param: source what vendor created the CVR file from which this CVR was parsed
  // param: ballotID unique ID of this ballot
  // param: rankings list of rank->candidateID selections parsed for this CVR
  // param: fullCVRData list of strings containing ALL data parsed for this CVR
  CastVoteRecord(
      String sourceName,
      String cvrID,
      String precinct,
      List<String> fullCVRData,
      List<Pair<Integer, String>> rankings
  ) {
    this.sourceName = sourceName;
    this.cvrID = cvrID;
    this.precinct = precinct;
    this.fullCVRData = fullCVRData;
    sortRankings(rankings);
  }

  // function: addRoundOutcome
  // purpose: adds the outcome for this CVR for this round (for auditing purposes)
  // param: outcomeType indicates what happened
  // param: detail reflects who (if anyone) received the vote or why it was exhausted/ignored
  void addRoundOutcome(VoteOutcomeType outcomeType, String detail) {
    roundOutcomes.add(new VoteOutcome(outcomeType, detail));
  }

  // function: exhaust
  // purpose: transition the CVR into exhausted state with the given reason
  // param: round the exhaustion occurs
  // param: reason: the reason for exhaustion
  void exhaust(int round, String reason) {
    assert !isExhausted;
    isExhausted = true;
    addRoundOutcome(VoteOutcomeType.EXHAUSTED, reason);
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
    return fractionalTransferValue;
  }

  // function: setFractionalTransferValue
  // purpose: setter for fractionalTransferValue
  // param: new value of field
  void setFractionalTransferValue(BigDecimal fractionalTransferValue) {
    this.fractionalTransferValue = fractionalTransferValue;
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
  // purpose: getter for precicnt
  // returns: value of field
  String getPrecinct() {
    return precinct;
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
    auditStringBuilder.append("[CVR Source] ");
    auditStringBuilder.append(sourceName);
    auditStringBuilder.append(" [Ballot ID] ");
    auditStringBuilder.append(cvrID);
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
      auditStringBuilder.append(roundOutcome.detail).append('|');
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

    VoteOutcome(VoteOutcomeType outcomeType, String detail) {
      this.outcomeType = outcomeType;
      this.detail = detail;
    }
  }
}
