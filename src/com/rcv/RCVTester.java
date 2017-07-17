package com.rcv;

import java.io.IOException;
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

  static String TEST_LOG_PATH = "./test/election_summary_report.txt";

  public static int runTests() {
    try {
      RCVLogger.setup(TEST_LOG_PATH);
    } catch (IOException e) {
      e.printStackTrace();
      System.out.println("failed to open log file:" + TEST_LOG_PATH);
      return 1;
    }
    List<String> contestOptions = new LinkedList<String>();
    contestOptions.add("Eric Clapton");
    contestOptions.add("Santana");
    contestOptions.add("Van Halen");

    // TODO: Tabulator takes election object as input and tabulates all the contests defined in Election
    // use the cvr list as input to tabulator below
    CastVoteRecordList cvrList = JsonParser.parseObjectFromFile(CAST_VOTE_RECORD_LIST_PATH, CastVoteRecordList.class);

    Tabulator tabulator = new Tabulator(cvrList.getRecords(), 0, contestOptions);
    tabulator.tabulate();

    Tabulator tabulator2 = new Tabulator(cvrList.getRecords(), 1, contestOptions);
    tabulator2.tabulate();

    Tabulator tabulator3 = new Tabulator(cvrList.getRecords(), 2, contestOptions);
    tabulator3.setBatchElimination(true);
    tabulator3.tabulate();

    return 0;
  }

}
