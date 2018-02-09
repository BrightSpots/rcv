/**
 * Created by Jonathan Moldover on 7/8/17
 * Copyright 2018 Bright Spots
 * Purpose:
 * internal representation of a single cast vote record including rankings ID and
 * state (exhausted or not).  Conceptually this is a ballot.
 * Version: 1.0
 */

package com.rcv;

import com.sun.tools.javac.util.Pair;
import java.util.*;

public class CastVoteRecord {
  // name of the vendor, this becomes part of the audit output but is not used in tabulation
  private String sourceName;
  // unique identifier for this cast vote record
  private String cvrID;
  // container for ALL cvr data parsed from the source cvr file
  private List<String> fullCVRData;
  // map of round to all candidates selected for that round
  // a set is used to handle overvotes
  public SortedMap<Integer, Set<String>> rankToCandidateIDs;
  // weather this cvr is exhausted or not
  private boolean isExhausted;

  // contains who this cvr counted for in each round
  // followed by reason for exhaustion if it is ever exhausted
  private Map<Integer, String> descriptionsByRound = new HashMap<>();

  // function: CastVoteRecord
  // purpose: create a new cvr object
  // param: source what vendor created the cvr file from which this cvr was parsed
  // param: ballotID unique ID of this ballot
  // param: rankings list of rank->candidateID selections parsed for this cvr
  // param: fullCVRData list of strings containting ALL data parsed for this cvr
  public CastVoteRecord(
      String source,
      String ballotID,
      List<Pair<Integer, String>> rankings,
      List<String> fullCVRData
  ) {
    sourceName = source;
    cvrID = ballotID;
    this.fullCVRData = fullCVRData;
    sortRankings(rankings);
  }

  // function: addRoundDescription
  // purpose: adds the string to this CVR round by round descriptions for auditing
  // param: description what happened (exhaustion or who the vote counted towards)
  // param: round associated with the description
  public void addRoundDescription(String description, int round) {
    descriptionsByRound.put(round, description);
  }

  // function: exhaust
  // purpose: transition the cvr into exhausted state with the given reason
  // param: round the exhaustion occurs
  // param: reason: the reason for exhaustion
  public void exhaust(int round, String reason) {
    assert(!isExhausted);
    isExhausted = true;
    // formatted description string
    String description = String.format("%d|exhausted:%s|", round, reason);
    addRoundDescription(description, round);
  }

  // function: isExhausted
  // purpose: getter for exhausted state
  // returns: true of cvr is exhausted otherwise false
  public boolean isExhausted() {
    return isExhausted;
  }

  // function: sortRankings
  // purpose: create a map of ranking to candidates selected at that rank
  // param: rankings list of rankings (rank, candidateID pairs) to be sorted
  private void sortRankings(List<Pair<Integer, String>> rankings) {
      rankToCandidateIDs = new TreeMap<>();
      // index for iterating over all rankings
      for (Pair<Integer, String> ranking : rankings) {
        // set of candidates given this rank
        Set<String> candidatesAtRank = rankToCandidateIDs.get(ranking.fst);
        if (candidatesAtRank == null) {
          // create the new optionsAtRank and add to the sorted cvr
          candidatesAtRank = new HashSet<>();
          rankToCandidateIDs.put(ranking.fst, candidatesAtRank);
        }
        // add this option into the map
        candidatesAtRank.add(ranking.snd);
      }
  }

  // function: getAuditString
  // purpose: return a formatted string describing this cvr and how state changes over
  //  the course of the tabulation.  Used for audit output.
  // returns: the formatted string for audit output
  String getAuditString() {
    // use a string builder for more efficient string creation
    StringBuilder auditStringBuilder = new StringBuilder();
    auditStringBuilder.append("[CVR Source] ");
    auditStringBuilder.append(sourceName);
    auditStringBuilder.append(" [Ballot ID] ");
    auditStringBuilder.append(cvrID);
    auditStringBuilder.append(" [Round by Round Report] |");
    // index to to iterate over all round descriptions
    for(Integer round : descriptionsByRound.keySet()) {
      auditStringBuilder.append(descriptionsByRound.get(round));
    }
    auditStringBuilder.append(" [Raw Data] ");
    auditStringBuilder.append(fullCVRData);
    return auditStringBuilder.toString();
  }
}
