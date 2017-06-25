package com.rcv;

import java.util.*;

/**
 * Created by Jon on 6/18/17.
 *
 * Test harness for RCV module
 *
 */
public class RCVTester {

  public static String ELECTION_PATH = "./test/Election.json";
  public static String CAST_VOTE_RECORD_PATH = "./test/CastVoteRecord.json";
  public static String CONTEST_RANKINGS_PATH = "./test/ContestRankings.json";

  public static String CAST_VOTE_RECORD_LIST_PATH = "./test/CastVoteRecordList.json";

  //  public static String TEST_LOG_PATH = "./data/test_results.txt";

  public static void runTests() {

    // test parsing different election objects
    Election election = JsonParser.parseObjectFromFile(ELECTION_PATH, Election.class);
    ContestRankings rankings = JsonParser.parseObjectFromFile(CONTEST_RANKINGS_PATH, ContestRankings.class);
    CastVoteRecord cvr = JsonParser.parseObjectFromFile(CAST_VOTE_RECORD_PATH, CastVoteRecord.class);


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

    // TODO: Tabulator takes election object as input and tabulates all the contests defined in Election
    // use the cvr list as input to tabulator below
    CastVoteRecordList cvrList = JsonParser.parseObjectFromFile(CAST_VOTE_RECORD_LIST_PATH, CastVoteRecordList.class);
    Tabulator tabulator = new Tabulator(cvrList.records, contestId, contestOptions);
    tabulator.tabulate();
    Tabulator tabulator2 = new Tabulator(cvrList.records, 1, contestOptions);
    tabulator2.tabulate();
  }

}
