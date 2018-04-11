/*
 * Created by Jonathan Moldover, Louis Eisenberg, and Hylton Edingfield
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
  // returns: N/A
  public static void main(String[] args) {
    if (args.length == 0) {
      // if no args provided, assume user wants to use the GUI
      System.out.println("No arguments provided; starting GUI...");
      // graphical user interface (GUI)
      RcvGui gui = new RcvGui();
      gui.launch();
    } else {
      // assume user wants to use CLI
      String configPath = args[0];
      // config file for running the tabulator
      ElectionConfig config = loadElectionConfig(configPath);
      Logger.log("Tabulator is being used via the CLI.");
      executeTabulation(config);
    }
  }

  // function: loadElectionConfig
  // purpose: create config object
  // param: path to config file
  // returns: the new ElectionConfig object or null if there was a problem
  static ElectionConfig loadElectionConfig(String configPath) {
    // config: the new object
    ElectionConfig config = null;
    try {
      // rawConfig holds the basic election config data parsed from json
      RawElectionConfig rawConfig;
      // parse raw config
      rawConfig = JsonParser.parseObjectFromFile(configPath, RawElectionConfig.class);
      // create and validate new ElectionConfig
      if (rawConfig != null) {
        config = new ElectionConfig(rawConfig);
        // set up log output
        Logger.setup(config.auditOutput());
        Logger.log("Parsed config file: %s", configPath);
        Logger.log("Logging to: %s", config.auditOutput());
        if (!config.validate()) {
          Logger.log("There was a problem validating the election configuration.");
          Logger.log("Please see the README.txt for details.");
          config = null;
        }
      }
    } catch (IOException exception) {
      System.err.print(
          String.format(
              "Failed to configure logging output to file: %s\n%s",
              config.auditOutput(),
              exception.toString()
          )
      );
    }
    // TODO: Should probably add either check for null config here (and throw exception?), or whenever it's called
    return config;
  }

  // function: executeTabulation
  // purpose: execute tabulation for given ElectionConfig
  // param: config object containing CVR file paths to parse
  // returns: String indicating whether or not execution was successful
  static String executeTabulation(ElectionConfig config) {
    // Read cast vote records from CVR files
    // castVoteRecords will contain all cast vote records parsed by the reader
    List<CastVoteRecord> castVoteRecords;
    // String indicating whether or not execution was successful
    String response = "Tabulation successful!";
    try {
      // parse the cast vote records
      castVoteRecords = parseCastVoteRecords(config);
      // tabulator for tabulation logic
      Tabulator tabulator = new Tabulator(castVoteRecords, config);
      // do the tabulation
      tabulator.tabulate();
      // generate visualizer spreadsheet data
      tabulator.generateSummarySpreadsheet();
      // generate audit data
      tabulator.doAudit(castVoteRecords);
    } catch (Exception exception) {
      response = String.format("ERROR during tabulation: %s", exception.toString());
      Logger.log(response);
    }
    // TODO: Redesign this later so as not to return a user-facing status string
    return response;
  }

  // function: parseCastVoteRecords
  // purpose: parse CVR files referenced in the ElectionConfig object into a list of CastVoteRecords
  // param: config object containing CVR file paths to parse
  // returns: list of all CastVoteRecord objects parsed from CVR files
  private static List<CastVoteRecord> parseCastVoteRecords(ElectionConfig config) throws Exception {
    // castVoteRecords will contain all cast vote records parsed by the reader
    List<CastVoteRecord> castVoteRecords = new ArrayList<>();
    // at each iteration of the following loop we add records from another source file
    // source: index over config sources
    for (RawElectionConfig.CVRSource source : config.rawConfig.cvrFileSources) {
      Logger.log("Reading CVR file: %s (provider: %s)", source.filePath, source.provider);
      // reader: read input file into a list of cast vote records
      CVRReader reader = new CVRReader();
      reader.parseCVRFile(
        source.filePath,
        source.firstVoteColumnIndex,
        source.precinctColumnIndex,
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
