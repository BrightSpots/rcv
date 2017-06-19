package com.rcv;

import java.util.Map;

public class CastVoteRecord {
  Map<Integer, Map<Integer, Integer>> rankings;

  public CastVoteRecord(Map<Integer, Map<Integer, Integer>> rankings) {
    this.rankings = rankings;
  }
}
