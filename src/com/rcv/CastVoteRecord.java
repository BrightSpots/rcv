package com.rcv;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

// maps contest IDs to ContestRankings objects
public class CastVoteRecord {
  private Map<Integer, ContestRankings> rankings = new HashMap<Integer, ContestRankings>();

  @JsonAnySetter
  public void add(String key, ContestRankings value) {
    Integer rank = Integer.parseInt(key);
    rankings.put(rank, value);
  }

  public CastVoteRecord() {}

  public CastVoteRecord(Map<Integer, ContestRankings> rankings) {
    this.rankings = rankings;
  }

  SortedMap<Integer, Integer> getRankingsForContest(int contestId) {
    return rankings.get(contestId).rankings;
  }
}
