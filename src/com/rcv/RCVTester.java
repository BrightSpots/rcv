package com.rcv;

import java.io.IOException;

/**
 * Created by Jon on 6/18/17.
 *
 * Test harness for RCV module
 *
 */
public class RCVTester {

  static String ELECTION_PATH = "./test/Election.json";
  static String CAST_VOTE_RECORD_PATH = "./test/CastVoteRecord.json";
  static String CAST_VOTE_RECORD_LIST_PATH = "./test/CastVoteRecordList.json";

  static String TEST_LOG_PATH = "./test/election_summary_report.txt";

  static final String TEST_PORTLAND_XLSX_CVR_FILE = "./test/Portland-Cast-Vote-Records-July 2015.xlsx";
  static final String TEST_PORTLAND_RESULTS_XLSX_FILE = "./test/Portland-Tabulation-Results.xlsx";

  static final String TEST_MINNEAPOLIS_XLSX_CVR_FILE = "./test/2013-minneapolis-mayor-cvr.xlsx";
  static final String TEST_MINNEAPOLIS_RESULTS_XLSX_FILE = "./test/2013-minneapolis-mayor-tabulation-results.xlsx";

  public static int runTests() {
    try {
      RCVLogger.setup(TEST_LOG_PATH);
    } catch (IOException e) {
      System.out.println("failed to open log file:" + TEST_LOG_PATH);
      return 1;
    }

    if (!testExcel(TEST_PORTLAND_XLSX_CVR_FILE, TEST_PORTLAND_RESULTS_XLSX_FILE, 3, 15)) {
      return 1;
    }

    if (!testExcel(TEST_MINNEAPOLIS_XLSX_CVR_FILE, TEST_MINNEAPOLIS_RESULTS_XLSX_FILE, 1, 3)) {
      return 1;
    }

    Election testElection = JsonParser.parseObjectFromFile(ELECTION_PATH, Election.class);

    CastVoteRecordList cvrList = JsonParser.parseObjectFromFile(CAST_VOTE_RECORD_LIST_PATH, CastVoteRecordList.class);

    for (Contest contest : testElection.getContests()) {
      Tabulator tabulator = new Tabulator(
        cvrList.getRecords(),
        contest.id,
        contest.options,
        testElection.batch_elimination,
        1,
        Tabulator.OvervoteRule.EXHAUST_IF_ANY_CONTINUING,
        null
      );
      try {
        tabulator.tabulate();
      } catch (Exception e) {
        e.printStackTrace();
        return 1;
      }
    }
    
    return 0;
  }

  private static boolean testExcel(String inFile, String outFile, int firstVoteColumnIndex, int allowableRanks) {
    CVRReader reader = new CVRReader();
    if (reader.parseCVRFile(inFile, firstVoteColumnIndex, allowableRanks)) {
      Tabulator testTabulator = new Tabulator(
        reader.castVoteRecords,
        1,
        reader.candidateOptions,
        true,
        1,
        Tabulator.OvervoteRule.EXHAUST_IF_ANY_CONTINUING,
        null
      );
      try {
        testTabulator.tabulate();
        testTabulator.generateSummarySpreadsheet(outFile);
      } catch (Exception e) {
        e.printStackTrace();
        return false;
      }

    } else {
      RCVLogger.log("failed to parse %s!  skipping tabulation!", inFile);
    }
    return true;
  }
}
