package com.rcv;
import java.util.*;

// internal representation of a cast vote record
// contains an ID
// contains a mapping of rank (integer) to a set of candidate ID(s) selected at that rank
// a set is used to handle overvotes
public class CastVoteRecord {

  String mSource;
  String mBallotID;
  List<String> mFullCVRData;
  List<ContestRanking> mRankings;
  SortedMap<Integer, Set<String>> mSortedRankings;

  // mDescriptionsByRound contains who this ballot counted for in each round
  // followed by reason for exhaustion if it is ever exhausted
  public Map<Integer, String> mDescriptionsByRound = new HashMap<>();

  // adds the string to this CVR round by round descriptions for auditing
  public void addRoundDescription(String description, int round) {
    mDescriptionsByRound.put(round, description);
  }
  
  // output is our rankings sorted from first to last preference
  // Set is used to accommodate overvotes
  // TODO: build this map during the CVR reader process
  public SortedMap<Integer, Set<String>> sortedRankings() {
    if(mSortedRankings == null) {
      mSortedRankings = new TreeMap<>();
      for (ContestRanking ranking : mRankings) {
        // set of candidates given this rank
        Set<String> optionsAtRank = mSortedRankings.get(ranking.getRank());
        if (optionsAtRank == null) {
          // create the new optionsAtRank and add to the sorted cvr
          optionsAtRank = new HashSet<>();
          mSortedRankings.put(ranking.getRank(), optionsAtRank);
        }
        // add this option into the map
        optionsAtRank.add(ranking.getOptionId());
      }
    }
    return mSortedRankings;
  }

  public CastVoteRecord(String source, String ballotID, List<ContestRanking> rankings, List<String> fullCVRData) {
    mSource = source;
    mBallotID = ballotID;
    mRankings = rankings;
    mFullCVRData = fullCVRData;
  }

  String getAuditString() {
    StringBuilder auditStringBuilder = new StringBuilder();
    auditStringBuilder.append("[CVR Source] ");
    auditStringBuilder.append(mSource);
    auditStringBuilder.append(" [Ballot ID] ");
    auditStringBuilder.append(mBallotID);
    auditStringBuilder.append(" [Round by Round Report] |");
    for(Integer round : mDescriptionsByRound.keySet()) {
      auditStringBuilder.append(mDescriptionsByRound.get(round));
    }
    auditStringBuilder.append(" [Raw Data] ");
    auditStringBuilder.append(mFullCVRData);
    return auditStringBuilder.toString();
  }
}
