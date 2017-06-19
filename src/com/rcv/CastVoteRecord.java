package com.rcv;

import java.util.Map;

// first integer is the contest ID
// second integer is Rank
// third integer is the optionID
// e.g. an election containing contests ID 0 and 1:
//  contest 0 has 3 options (candidate IDs 0,1,2)
//  contest 1 has 4 options (candidate IDs 3,4,5,6)
//  A complete CRV which ranks every candidate and contains
//  no overvotes or undervotes could look like this:
//
//  {
//      "0":{"1":"2","2":"1","3":"0"},
//      "1":{"1":"6","2":"5","3":"4","4":"3"}
//  }
//


public class CastVoteRecord {
  Map<Integer, Map<Integer, Integer>> rankings;

  public CastVoteRecord(Map<Integer, Map<Integer, Integer>> rankings) {
    this.rankings = rankings;
  }
}
