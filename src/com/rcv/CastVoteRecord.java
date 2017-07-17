package com.rcv;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.*;

// map of contest IDs to ContestRankings objects
// i.e. a Ballot
public class CastVoteRecord {
  private Map<Integer, ContestRankings> rankings = new HashMap<Integer, ContestRankings>();

  @JsonAnySetter
  public void add(String key, ContestRankings value) {
    Integer electionId = Integer.parseInt(key);
    rankings.put(electionId, value);
  }

  public CastVoteRecord() {}

  public CastVoteRecord(Map<Integer, ContestRankings> rankings) {
    this.rankings = rankings;
  }

  List<ContestRanking> getRankingsForContest(int contestId) {
    return rankings.get(contestId).rankings;
  }
}
