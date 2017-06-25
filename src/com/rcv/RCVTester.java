package com.rcv;

import java.util.*;

/**
 * Created by Jon on 6/18/17.
 */
public class RCVTester {


  public static String TEST_ELECTION_PATH = "./data/Election.json";
  public static String TEST_CVR_LIST_PATH = "./data/CastVoteRecordList.json";
  public static String TEST_CVR_PATH = "./data/CastVoteRecord.json";
  public static String TEST_CONTEST_RANKINGS = "./data/ContestRankings.json";
//  public static String TEST_LOG_PATH = "./data/test_results.txt";

  public RCVTester() {
  }

  public void runTests() {

    // test parsing different election objects
    Election election = JsonParser.parseObjectFromFile(TEST_ELECTION_PATH, Election.class);
    ContestRankings rankings = JsonParser.parseObjectFromFile(TEST_CONTEST_RANKINGS, ContestRankings.class);
    CastVoteRecord cvr = JsonParser.parseObjectFromFile(TEST_CVR_PATH, CastVoteRecord.class);
    // use the cvr list as input to the tabulator below
    CastVoteRecordList cvrList = JsonParser.parseObjectFromFile(TEST_CVR_LIST_PATH, CastVoteRecordList.class);

//    int contestId = 1;
//    List<CastVoteRecord> list = new LinkedList<CastVoteRecord>();
//    list.add(new CastVoteRecord(contestId, 1, 2, 3));
//    list.add(new CastVoteRecord(contestId, 2, 1, 3));
//    list.add(new CastVoteRecord(contestId, 1, 2, 3));
//    list.add(new CastVoteRecord(contestId, 2, 1, 3));
//    list.add(new CastVoteRecord(contestId, 1, 3, 2));
//    list.add(new CastVoteRecord(contestId, 2, 1, 3));
    List<Integer> contestOptions = new LinkedList<Integer>();
    contestOptions.add(0);
    contestOptions.add(1);
    contestOptions.add(2);
    int contestId = 0;

    // TODO: Tabulator takes an election object as input and tabulates all the contests defined in Election
    Tabulator tabulator = new Tabulator(cvrList.records, contestId, contestOptions);
    tabulator.tabulate();
    Tabulator tabulator2 = new Tabulator(cvrList.records, 1, contestOptions);
    tabulator2.tabulate();
  }

}
