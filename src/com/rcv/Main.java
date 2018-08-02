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

import com.rcv.CVRReader.SourceWithUnrecognizedCandidatesException;
import com.rcv.FileUtils.UnableToCreateDirectoryException;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class Main extends GuiApplication {

  // function: main
  // purpose: main entry point to the rcv tabulator program
  // param: args command line argument array
  // returns: N/A
  public static void main(String[] args) {
    try {
      Logger.setup();
    } catch (IOException exception) {
      System.err.print(String.format("Failed to start system logging: %s", exception.toString()));
    }

    if (args.length == 0) {
      // if no args provided, launch the GUI
      launch(args);
    } else {
      // assume user wants to use CLI
      String configPath = args[0];
      // config file for running the tabulator
      ElectionConfig config = loadElectionConfig(configPath);
      if (config != null) {
        Logger.info("Tabulator is being used via the CLI.");
        executeTabulation(config);
      } else {
        Logger.severe("Aborting because config is invalid.");
      }
    }
  }

  // function: loadElectionConfig
  // purpose: create config object
  // param: path to config file
  // returns: the new ElectionConfig object, or null if there was a problem
  static ElectionConfig loadElectionConfig(String configPath) {
    // config: the new object
    ElectionConfig config = null;

    // tracks whether we encountered any critical file system errors
    boolean encounteredFileError = false;

    // rawConfig holds the basic election config data parsed from json
    RawElectionConfig rawConfig =
        JsonParser.parseObjectFromFile(configPath, RawElectionConfig.class);

    if (rawConfig == null) {
      System.err.println(String.format("Failed to load config file: %s", configPath));
    } else {
      config = new ElectionConfig(rawConfig);

      try {
        FileUtils.createOutputDirectory(config.getOutputDirectory());
      } catch (UnableToCreateDirectoryException exception) {
        System.err.println(
            String.format(
                "Failed to create output directory: %s\n%s",
                config.getOutputDirectory(), exception.toString()));
        encounteredFileError = true;
      }

      List<String> validationErrors = config.getValidationErrors();
      if (!validationErrors.isEmpty()) {
        for (String error : validationErrors) {
          Logger.severe("Invalid config: %s", error);
        }
        config = null;
      }
    }

    if (encounteredFileError) {
      config = null;
    }

    return config;
  }

  // function: executeTabulation
  // purpose: execute tabulation for given ElectionConfig
  // param: config object containing CVR file paths to parse
  // returns: String indicating whether or not execution was successful
  static String executeTabulation(ElectionConfig config) {
    // String indicating user message
    String response = "Tabulation successful!";
    // Error message for user and log
    String errorMessage;
    // flag indicating tabulation success
    boolean encounteredError = false;
    // current date-time formatted as a string used for creating unique output files names
    String timestampString = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
    // create audit log file name
    String logFileName = String.format("%s_audit.log", timestampString);
    // audit log path
    String auditLogPath = Paths.get(config.getOutputDirectory(), logFileName).toString();
    try {
      // audit logger
      Logger.addTabulationFileLogging(auditLogPath);
      Logger.info("Logging tabulation to: %s", auditLogPath);
    } catch (IOException exception) {
      errorMessage =
          String.format("Failed to configure tabulation logger: %s", exception.toString());
      Logger.severe(errorMessage);
      response = errorMessage;
      encounteredError = true;
    }

    if (!encounteredError) {
      // Read cast vote records from CVR files
      // castVoteRecords will contain all cast vote records parsed by the reader
      List<CastVoteRecord> castVoteRecords;
      // parse the cast vote records
      castVoteRecords = parseCastVoteRecords(config);
      if (castVoteRecords != null) {
        if (!castVoteRecords.isEmpty()) {
          // tabulator for tabulation logic
          Tabulator tabulator = new Tabulator(castVoteRecords, config);
          // do the tabulation
          tabulator.tabulate();
          // generate visualizer spreadsheet data
          tabulator.generateSummarySpreadsheet(timestampString);
          // generate audit data
          tabulator.doAudit(castVoteRecords);
        } else {
          errorMessage = "No cast vote records found.";
          Logger.severe(errorMessage);
          response = errorMessage;
        }
      } else {
        errorMessage = "Skipping tabulation due to source file errors.";
        Logger.severe(errorMessage);
        response = errorMessage;
      }
    }
    Logger.info("Done logging tabulation to %s", auditLogPath);
    Logger.removeTabulationFileLogging();
    // TODO: Redesign this later so as not to return a user-facing status string
    return response;
  }

  // function: parseCastVoteRecords
  // purpose: parse CVR files referenced in the ElectionConfig object into a list of CastVoteRecords
  // param: config object containing CVR file paths to parse
  // returns: list of all CastVoteRecord objects parsed from CVR files (or null if there's an error)
  private static List<CastVoteRecord> parseCastVoteRecords(ElectionConfig config) {
    // castVoteRecords will contain all cast vote records parsed by the reader
    List<CastVoteRecord> castVoteRecords = new ArrayList<>();
    // how many sources had fatal problems?
    int numInvalidSources = 0;

    if (config.rawConfig.cvrFileSources == null || config.rawConfig.cvrFileSources.isEmpty()) {
      Logger.severe("Config doesn't contain any CVR input files.");
    } else {
      // At each iteration of the following loop, we add records from another source file.
      // source: index over config sources
      for (RawElectionConfig.CVRSource source : config.rawConfig.cvrFileSources) {
        Logger.info("Reading CVR file: %s (provider: %s)", source.filePath, source.provider);

        // did we encounter a fatal problem for this source?
        boolean encounteredProblemForThisSource = false;

        if (source.filePath == null || source.filePath.isEmpty()) {
          Logger.severe("Invalid source file: missing filePath");
          encounteredProblemForThisSource = true;
        }

        if (source.firstVoteColumnIndex == null || source.firstVoteColumnIndex < 0) {
          Logger.severe(
              "Invalid source file: missing or invalid firstVoteColumnIndex: %s", source.filePath);
          encounteredProblemForThisSource = true;
        }

        if (config.isTabulateByPrecinctEnabled()
            && (source.precinctColumnIndex == null || source.precinctColumnIndex < 0)) {
          Logger.severe(
              "Invalid source file: missing or invalid precinctColumnIndex when "
                  + "tabulateByPrecinct is enabled: %s",
              source.filePath);
          encounteredProblemForThisSource = true;
        }

        if (!encounteredProblemForThisSource) {
          // reader: read input file into a list of cast vote records
          CVRReader reader =
              new CVRReader(
                  config,
                  source.filePath,
                  source.firstVoteColumnIndex,
                  source.idColumnIndex,
                  source.precinctColumnIndex);
          // the CVRs parsed from this source
          try {
            List<CastVoteRecord> cvrs = reader.parseCVRFile();
            if (cvrs.isEmpty()) {
              Logger.severe("Source file contains no CVRs: %s", source.filePath);
              encounteredProblemForThisSource = true;
            }
            // add records to the master list
            castVoteRecords.addAll(cvrs);
          } catch (SourceWithUnrecognizedCandidatesException e) {
            Logger.severe("Source file contains unrecognized candidate(s): %s", source.filePath);
            // map from name to number of times encountered
            Map<String, Integer> candidateCounts = e.getCandidateCounts();
            for (String candidate : candidateCounts.keySet()) {
              Logger.severe(
                  "Unrecognized candidate \"%s\" appears %d time(s)",
                  candidate, candidateCounts.get(candidate));
            }
            encounteredProblemForThisSource = true;
          }
        }

        if (encounteredProblemForThisSource) {
          numInvalidSources++;
        }
      }
      if (numInvalidSources == 0) {
        Logger.info("Read %d cast vote records", castVoteRecords.size());
      }
    }

    if (numInvalidSources > 0) {
      Logger.severe("Encountered %d invalid source(s)", numInvalidSources);
      castVoteRecords = null;
    }

    return castVoteRecords;
  }
}
