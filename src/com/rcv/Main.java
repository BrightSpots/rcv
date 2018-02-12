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
    ElectionConfig config = makeElectionConfig(args);
    if (config != null) {
      try {
        // setup log output
        Logger.setup(config.auditOutput());
        Logger.log("Parsed config file: %s", args[0]);
        Logger.log("Logging to: %s", config.auditOutput());

        // Read cast vote records from cvr files
        // castVoteRecords will contain all cast vote records parsed by the reader
        List<CastVoteRecord> castVoteRecords = parseCastVoteRecords(config);

        // tabulator for tabulation logic
        Tabulator tabulator = new Tabulator(castVoteRecords, config);
        // do the tabulation
        tabulator.tabulate();
        // generate visualizer spreadsheet data
        tabulator.generateSummarySpreadsheet();
        // generate audit data
        tabulator.doAudit(castVoteRecords);

      } catch (IOException exception) {
        System.err.print(
          String.format(
            "Failed to configure logging output to file: %s\n%s",
            config.auditOutput(),
            exception.toString()
          )
        );
      }
    }
  }

  // function: makeElectionConfig
  // purpose: create config object from command line args
  // param: args command line arguments passed into main program
  // returns: the new ElectionConfig object or null if there was a problem
  public static ElectionConfig makeElectionConfig(String [] args) {
    // config: the new object
    ElectionConfig config = null;
    if (args.length > 0) {
      // rawConfig holds the basic election config data parsed from json
      RawElectionConfig rawConfig;
      // parse raw config
      rawConfig = JsonParser.parseObjectFromFile(args[0], RawElectionConfig.class);
      // create and validate new ElectionConfig
      if (rawConfig != null) {
        config = new ElectionConfig(rawConfig);
        if (!config.validate()) {
          Logger.log("There was a problem validating the election configuration.");
          Logger.log("Please see the README.txt for details.");
        }
      }
    } else {
      System.err.println("No election configuration file specified as input.");
      System.err.println("Please see the README.txt for details.");
    }
    return config;
  }

  // function: parseCastVoteRecords
  // purpose: parse cvr files referenced in the ElectionConfig object into a list of CastVoteRecords
  // param: config object containing cvr file paths to parse
  // returns: list of all CastVoteRecord objects parsed from cvr files
  public static List<CastVoteRecord> parseCastVoteRecords(ElectionConfig config) {
    // castVoteRecords will contain all cast vote records parsed by the reader
    List<CastVoteRecord> castVoteRecords = new ArrayList<>();
    // at each iteration of the following loop we add records from another source file
    // source: index over config sources
    for (RawElectionConfig.CVRSource source : config.rawConfig.cvrFileSources) {
      Logger.log("Reading cvr file: %s (provider: %s)", source.filePath, source.provider);
      // reader: read input file into a list of cast vote records
      CVRReader reader = new CVRReader();
      reader.parseCVRFile(
        source.filePath,
        source.firstVoteColumnIndex,
        config.maxRankingsAllowed(),
        config.getCandidateCodeList(),
        config
      );
      // add records to the master list
      castVoteRecords.addAll(reader.castVoteRecords);
    }
    Logger.log("Read %d records", castVoteRecords.size());
    return castVoteRecords;
  }

}
