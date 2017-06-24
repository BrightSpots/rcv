package com.rcv;

import java.util.*;

/**
 * Created by Jon on 6/18/17.
 */
public class RCVTester {


  public static String TEST_ELECTION_PATH = "./data/test_election_0.json";
  public static String TEST_CVR_PATH = "./data/test_CastVoteRecordList.json";
  public static String TEST_LOG_PATH = "./data/test_results.txt";

  public RCVTester() {
  }

  public void runTests() {

    JacksonParser jacksonParser = new JacksonParser(TEST_ELECTION_PATH, TEST_CVR_PATH);

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
//    RCVParser parser = new RCVParser(TEST_ELECTION_PATH, TEST_CVR_PATH);
    int contestId = 0;
    Tabulator tabulator = new Tabulator(jacksonParser.getCastVoteRecords(), contestId, contestOptions);
    tabulator.tabulate();
    Tabulator tabulator2 = new Tabulator(jacksonParser.getCastVoteRecords(), 1, contestOptions);
    tabulator2.tabulate();
  }

  private CastVoteRecord makeCVR(int contestId, int rank1, int rank2, int rank3) {
    SortedMap<Integer, Integer> rankings = new TreeMap<Integer, Integer>();
    rankings.put(1, rank1);
    rankings.put(2, rank2);
    rankings.put(3, rank3);
    ContestRankings contestRankings = new ContestRankings(rankings);
    Map<Integer, ContestRankings> cvrMap = new HashMap<Integer, ContestRankings>();
    cvrMap.put(contestId, contestRankings);
    return new CastVoteRecord(cvrMap);
  }
}
