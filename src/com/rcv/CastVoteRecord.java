package com.rcv;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

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

  // create a cast vote record with a single contest and the specified rankings
  public CastVoteRecord(int contestId, int rank1, int rank2, int rank3) {
    SortedMap<Integer, Integer> rankings = new TreeMap<Integer, Integer>();
    rankings.put(1, rank1);
    rankings.put(2, rank2);
    rankings.put(3, rank3);
    ContestRankings contestRankings = new ContestRankings(rankings);
    this.rankings.put(contestId, contestRankings);
  }


  SortedMap<Integer, Integer> getRankingsForContest(int contestId) {
    return rankings.get(contestId).rankings;
  }
}
