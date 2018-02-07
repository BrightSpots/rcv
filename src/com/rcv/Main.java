/**
 * Created by Jonathan Moldover on 7/8/17
 * Copyright 2018 Bright Spots
 * Purpose: Main entry point for the rcv module
 * Controls high-level flow for program execution:
 * parse command line
 * parse config file
 * read cast vote records
 * tabulate election
 * output results
 * Version: 1.0
 */

package com.rcv;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {

  // function: main
  // purpose: main entry point to the rcv tabulator program
  // param: args command line argument array
  public static void main(String[] args) {
    // validate at least one arg passed in
    if(args.length < 1) {
      System.err.print("No config file specified.  Exiting.");
      System.exit(1);
    }
    // rawConfig holds the basic election config data parsed from json
    RawElectionConfig rawConfig = null;
    try {
      // parse raw config
      rawConfig = JsonParser.parseObjectFromFile(args[0], RawElectionConfig.class);
    } catch (Exception exception) {
      System.err.print("Error parsing config file.  Exiting");
      System.exit(1);
    }
    // config wraps the rawConfig and provides helper logic to handle various election parameters
    ElectionConfig config = new ElectionConfig(rawConfig);
    try {
      // setup log output
      Logger.setup(config.auditOutput());
      Logger.log("parsed config file:%s", args[0]);
      Logger.log("logging to:%s", config.auditOutput());
    } catch (IOException exception) {
      System.err.print(String.format("failed to configure logging output: %s",
          exception.toString()));
      System.err.print("Exiting");
      System.exit(1);
    }

    // Read cast vote records from cvr files

    // castVoteRecords will contain all cast vote records parsed by the reader
    List<CastVoteRecord> castVoteRecords = new ArrayList<>();
    // at each iteration of the following loop we add records from another source file
    // source: index over config sources
    for (RawElectionConfig.CVRSource source : rawConfig.cvr_file_sources) {
      Logger.log("reading cvr file:%s provider:%s",source.file_path, source.provider);
      // reader: read input file into a list of cast vote records
      CVRReader reader = new CVRReader();
      reader.parseCVRFile(
        source.file_path,
        source.first_vote_column_index,
        config.maxRankingsAllowed(),
        config.getCandidateCodeList(),
        config
      );
      // add records to the master list
      castVoteRecords.addAll(reader.castVoteRecords);
    }

    Logger.log("read %d records",castVoteRecords.size());

    // tabulate the election results

    // tabulator: handles most tabulation logic
    Tabulator tabulator = new Tabulator(castVoteRecords, config);
    try {
      // TODO: break these steps into separate try / catch blocks and provide error messages
      
      // do the tabulation
      tabulator.tabulate();
      // generate the visualizer spreadsheet data
      tabulator.generateSummarySpreadsheet();
      // generate audit data
      tabulator.doAudit(castVoteRecords);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
