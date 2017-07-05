package com.rcv;

import java.util.*;

/**
 * Created by Jon on 6/18/17.
 *
 * Test harness for RCV module
 *
 */
public class RCVTester {

  static String ELECTION_PATH = "./test/Election.json";
  static String CAST_VOTE_RECORD_PATH = "./test/CastVoteRecord.json";
  static String CONTEST_RANKINGS_PATH = "./test/ContestRankings.json";
  static String CAST_VOTE_RECORD_LIST_PATH = "./test/CastVoteRecordList.json";

  //  public static String TEST_LOG_PATH = "./data/test_results.txt";

  public static void runTests() {

    // test parsing different election objects
    Election election = JsonParser.parseObjectFromFile(ELECTION_PATH, Election.class);
    ContestRankings rankings = JsonParser.parseObjectFromFile(CONTEST_RANKINGS_PATH, ContestRankings.class);
    CastVoteRecord cvr = JsonParser.parseObjectFromFile(CAST_VOTE_RECORD_PATH, CastVoteRecord.class);

    List<Integer> contestOptions = new LinkedList<Integer>();
    contestOptions.add(0);
    contestOptions.add(1);
    contestOptions.add(2);

    // TODO: Tabulator takes election object as input and tabulates all the contests defined in Election
    // use the cvr list as input to tabulator below
    CastVoteRecordList cvrList = JsonParser.parseObjectFromFile(CAST_VOTE_RECORD_LIST_PATH, CastVoteRecordList.class);
    System.out.println("\nContest 0:");
    Tabulator tabulator = new Tabulator(cvrList.records, 0, contestOptions);
    tabulator.tabulate();
    System.out.println("\nContest 1:");
    Tabulator tabulator2 = new Tabulator(cvrList.records, 1, contestOptions);
    tabulator2.tabulate();
    System.out.println("\nContest 2:");
    Tabulator tabulator3 = new Tabulator(cvrList.records, 2, contestOptions);
    tabulator3.setBatchElimination(true);
    tabulator3.tabulate();
  }

}
