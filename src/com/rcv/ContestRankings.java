package com.rcv;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.*;

// Maps contest ID(s) to rankings a voter made for that contest
public class ContestRankings {
  public SortedMap<Integer, Integer> rankings = new TreeMap<Integer, Integer>();

  @JsonAnySetter
  public void add(String key, Integer value) {
    Integer rank = Integer.parseInt(key);
    rankings.put(rank, value);
  }

  public ContestRankings(Map<Integer, Integer> rankings) {
    rankings = rankings;
  }

  public ContestRankings() {}

}
