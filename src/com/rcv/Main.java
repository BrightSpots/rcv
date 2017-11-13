package com.rcv;

/*
 * Main entry point for the rcv module
 *
 * TODO: define command line parameters to control testing
 *
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {

  public static void main(String[] args) {
    if(args.length < 1) {
      System.out.print("Usage: rcv [path-to-config-file]");
      System.exit(0);
    }
    // parse config object
    ElectionConfig config = JsonParser.parseObjectFromFile(args[0], ElectionConfig.class);
    try {
      // setup log output
      RCVLogger.setup(config.audit_output);
      RCVLogger.log("parsed config file:%s",args[0]);

      // parse input files
      CVRReader reader = null;
      List<CastVoteRecord> castVoteRecords = new ArrayList<>();
      for (String source : config.sources) {
        RCVLogger.log("reading RCV:%s",source);
        reader = new CVRReader();
        reader.parseCVRFile(source, 3,15);
        castVoteRecords.addAll(reader.castVoteRecords);
      }

      RCVLogger.log("read %d records",castVoteRecords.size());
      // tabulate
      Tabulator tabulator = new Tabulator(
          castVoteRecords,
          1,
          reader.candidateOptions,
          true,
          2,
          Tabulator.OvervoteRule.IMMEDIATE_EXHAUSTION,
          null,
          null
      ).setContestName(config.contest_name).
          setJurisdiction("jurisdiction").
          setOffice("office").
          setElectionDate("date");
      try {
        tabulator.tabulate();
        tabulator.generateSummarySpreadsheet(config.visualizer_output);
      } catch (Exception e) {
        e.printStackTrace();
      }
    } catch (IOException e) {
      System.out.println("failed to open log file:" + config.audit_output);
      System.exit(1);
    }

    // disable test for prototype
    //RCVTester.runTests();
  }


}  // Main
