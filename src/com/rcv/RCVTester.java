package com.rcv;

import java.io.IOException;

/**
 * Created by Jon on 6/18/17.
 *
 * Test harness for RCV module
 *
 */
public class RCVTester {

  private static String ELECTION_PATH = "./test/Election.json";
  private static String CAST_VOTE_RECORD_LIST_PATH = "./test/CastVoteRecordList.json";

  private static String TEST_LOG_PATH = "./test/election_summary_report.txt";

  private static final String TEST_PORTLAND_XLSX_CVR_FILE = "./test/Portland-Cast-Vote-Records-July 2015.xlsx";
  private static final String TEST_PORTLAND_RESULTS_XLSX_FILE = "./test/Portland-Tabulation-Results.xlsx";

  private static final String TEST_MINNEAPOLIS_XLSX_CVR_FILE = "./test/2013-minneapolis-mayor-cvr.xlsx";
  private static final String TEST_MINNEAPOLIS_RESULTS_XLSX_FILE = "./test/2013-minneapolis-mayor-tabulation-results.xlsx";

  public static int runTests() {
    try {
      RCVLogger.setup(TEST_LOG_PATH);
    } catch (IOException e) {
      System.out.println("failed to open log file:" + TEST_LOG_PATH);
      return 1;
    }

    if (!testExcel(
      TEST_PORTLAND_XLSX_CVR_FILE,
      TEST_PORTLAND_RESULTS_XLSX_FILE,
      3,
      15,
      null,
      1,
      Tabulator.OvervoteRule.EXHAUST_IF_ANY_CONTINUING,
      "2013 Portland Mayoral Election",
      "City of Portland",
      "Mayor",
      "November 5, 2013"
    )) {
      return 1;
    }

    if (!testExcel(
      TEST_MINNEAPOLIS_XLSX_CVR_FILE,
      TEST_MINNEAPOLIS_RESULTS_XLSX_FILE,
      1,
      3,
      "UWI",
      null,
      Tabulator.OvervoteRule.IGNORE_IF_MULTIPLE_CONTINUING,
      "2013 Minneapolis Mayor Election",
      "City of Minneapolis",
      "Mayor",
      "November 5, 2013"
    )) {
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
        null,
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

  private static boolean testExcel(
    String inFile,
    String outFile,
    int firstVoteColumnIndex,
    int allowableRanks,
    String undeclaredWriteInString,
    Integer maxNumberOfSkippedRanks,
    Tabulator.OvervoteRule overvoteRule,
    String contestName,
    String jurisdiction,
    String office,
    String electionDate
  ) {
    CVRReader reader = new CVRReader();
    if (reader.parseCVRFile(inFile, firstVoteColumnIndex, allowableRanks)) {
      Tabulator tabulator = new Tabulator(
        reader.castVoteRecords,
        1,
        reader.candidateOptions,
        true,
        maxNumberOfSkippedRanks,
        overvoteRule,
        null,
        undeclaredWriteInString
      ).setContestName(contestName).
        setJurisdiction(jurisdiction).
        setOffice(office).
        setElectionDate(electionDate);
      try {
        tabulator.tabulate();
        tabulator.generateSummarySpreadsheet(outFile);
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
