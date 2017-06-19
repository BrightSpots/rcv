package com.rcv;

import java.util.*;

/**
 * Created by Jon on 6/18/17.
 */
public class RCVTester {


  public static String TEST_ELECTION_PATH = "./data/test_election_0.json";
  public static String TEST_CVR_PATH = "./data/test_cvr_0.json";
  public static String TEST_LOG_PATH = "./data/test_results.txt";

  public RCVTester() {
  }

  public void runTests() {
//    int contestId = 1;
//    List<CastVoteRecord> list = new LinkedList<CastVoteRecord>();
//    list.add(makeCVR(contestId, 1, 2, 3));
//    list.add(makeCVR(contestId, 2, 1, 3));
//    list.add(makeCVR(contestId, 1, 2, 3));
//    list.add(makeCVR(contestId, 2, 1, 3));
//    list.add(makeCVR(contestId, 1, 3, 2));
//    list.add(makeCVR(contestId, 2, 1, 3));
    List<Integer> contestOptions = new LinkedList<Integer>();
    contestOptions.add(0);
    contestOptions.add(1);
    contestOptions.add(2);
    RCVParser parser = new RCVParser(TEST_ELECTION_PATH, TEST_CVR_PATH);
    int contestId = 0;
    Tabulator tabulator = new Tabulator(parser.getCastVoteRecords(), contestId, contestOptions);
    tabulator.tabulate();
    Tabulator tabulator2 = new Tabulator(parser.getCastVoteRecords(), 1, contestOptions);
    tabulator2.tabulate();
  }

  private CastVoteRecord makeCVR(int contestId, int rank1, int rank2, int rank3) {
    SortedMap<Integer, Integer> rankings = new TreeMap<Integer, Integer>();
    rankings.put(1, rank1);
    rankings.put(2, rank2);
    rankings.put(3, rank3);
    Map<Integer, SortedMap<Integer, Integer>> map = new HashMap<Integer, SortedMap<Integer, Integer>>();
    map.put(contestId, rankings);
    return new CastVoteRecord(map);
  }
}
