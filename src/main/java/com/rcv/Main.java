/*
 * Ranked Choice Voting Universal Tabulator
 * Copyright (C) 2018 Jonathan Moldover, Louis Eisenberg, and Hylton Edingfield
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See
 * the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this
 * program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Purpose:
 * Main entry point for the RCV module. Controls high-level flow for program execution:
 * parse command line
 * parse config file
 * read cast vote records
 * tabulate election
 * output results
 */

package com.rcv;

import com.rcv.CVRReader.SourceWithUnrecognizedCandidatesException;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

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
        Logger.executionLog(Level.INFO, "Tabulator is being used via the CLI.");
        executeTabulation(config);
      } else {
        Logger.executionLog(Level.SEVERE, "Aborting because config is invalid.");
      }
    }
  }

  // function: loadElectionConfig
  // purpose: attempts to create config object
  // param: path to config file
  // returns: the new ElectionConfig object, or null if there was a problem
  static ElectionConfig loadElectionConfig(String configPath) {
    // config: the new object
    ElectionConfig config = null;

    // rawConfig holds the basic election config data parsed from json
    // this will be null if there is a problem loading it
    RawElectionConfig rawConfig =
        JsonParser.parseObjectFromFile(configPath, RawElectionConfig.class);

    // if raw config failed alert user
    if (rawConfig == null) {
      Logger.executionLog(Level.SEVERE, "Failed to load config file: %s", configPath);
    } else {
      // proceed to create the ElectionConfig wrapper
      config = new ElectionConfig(rawConfig);
      Logger.executionLog(Level.INFO, "Successfully loaded config file: %s", configPath);
    }

    return config;
  }

  // function: executeTabulation
  // purpose: execute tabulation for given ElectionConfig
  // param: config object containing CVR file paths to parse
  // returns: String indicating whether or not execution was successful
  static void executeTabulation(ElectionConfig config) {
    boolean isConfigValid = config.validate();

    if (isConfigValid) {
      Logger.allLogs(Level.INFO, "Starting tabulation...");

      // flag indicating tabulation success
      boolean encounteredError = false;
      // current date-time formatted as a string used for creating unique output files names
      String timestampString = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
      // create tabulation log file name and path
      String logFileName = String.format("%s_audit.log", timestampString);
      String tabulationLogPath = Paths.get(config.getOutputDirectory(), logFileName).toString();
      try {
        // add tabulation logger
        Logger.addTabulationFileLogging(tabulationLogPath);
        Logger.allLogs(Level.INFO, "Logging tabulation to: %s", tabulationLogPath);
      } catch (IOException exception) {
        Logger.executionLog(
            Level.SEVERE, "Failed to configure tabulation logger: %s", exception.toString());
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
            Logger.tabulationLog(Level.SEVERE, "No cast vote records found.");
          }
        } else {
          Logger.tabulationLog(Level.SEVERE, "Skipping tabulation due to source file errors.");
        }
      } else {
        Logger.executionLog(Level.SEVERE, "Unable to complete tabulation.");
      }
      Logger.allLogs(Level.INFO, "Done logging tabulation to: %s", tabulationLogPath);
      Logger.removeTabulationFileLogging();
    }
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
      Logger.tabulationLog(Level.SEVERE, "Config doesn't contain any CVR input files.");
    } else {
      // At each iteration of the following loop, we add records from another source file.
      // source: index over config sources
      for (RawElectionConfig.CVRSource source : config.rawConfig.cvrFileSources) {
        Logger.tabulationLog(
            Level.INFO,
            "Reading CVR file: %s (provider: %s)",
            source.getFilePath(),
            source.getProvider());

        // did we encounter a fatal problem for this source?
        boolean encounteredProblemForThisSource = false;

        if (source.getFilePath() == null || source.getFilePath().isEmpty()) {
          Logger.tabulationLog(Level.SEVERE, "Invalid source file: missing filePath");
          encounteredProblemForThisSource = true;
        }

        if (source.getFirstVoteColumnIndex() == null || source.getFirstVoteColumnIndex() < 0) {
          Logger.tabulationLog(
              Level.SEVERE,
              "Invalid source file: missing or invalid firstVoteColumnIndex: %s",
              source.getFilePath());
          encounteredProblemForThisSource = true;
        }

        if (config.isTabulateByPrecinctEnabled()
            && (source.getPrecinctColumnIndex() == null || source.getPrecinctColumnIndex() < 0)) {
          Logger.tabulationLog(
              Level.SEVERE,
              "Invalid source file: missing or invalid precinctColumnIndex when "
                  + "tabulateByPrecinct is enabled: %s",
              source.getFilePath());
          encounteredProblemForThisSource = true;
        }

        if (!encounteredProblemForThisSource) {
          // reader: read input file into a list of cast vote records
          CVRReader reader =
              new CVRReader(
                  config,
                  source.getFilePath(),
                  source.getFirstVoteColumnIndex(),
                  source.getIdColumnIndex(),
                  source.getPrecinctColumnIndex());
          // the CVRs parsed from this source
          try {
            List<CastVoteRecord> cvrs = reader.parseCVRFile();
            if (cvrs.isEmpty()) {
              Logger.tabulationLog(
                  Level.SEVERE, "Source file contains no CVRs: %s", source.getFilePath());
              encounteredProblemForThisSource = true;
            }
            // add records to the master list
            castVoteRecords.addAll(cvrs);
          } catch (SourceWithUnrecognizedCandidatesException e) {
            Logger.tabulationLog(
                Level.SEVERE,
                "Source file contains unrecognized candidate(s): %s",
                source.getFilePath());
            // map from name to number of times encountered
            Map<String, Integer> candidateCounts = e.getCandidateCounts();
            for (String candidate : candidateCounts.keySet()) {
              Logger.tabulationLog(
                  Level.SEVERE,
                  "Unrecognized candidate \"%s\" appears %d time(s)",
                  candidate,
                  candidateCounts.get(candidate));
            }
            encounteredProblemForThisSource = true;
          }
        }

        if (encounteredProblemForThisSource) {
          numInvalidSources++;
        }
      }
      if (numInvalidSources == 0) {
        Logger.tabulationLog(Level.INFO, "Read %d cast vote records", castVoteRecords.size());
      }
    }

    if (numInvalidSources > 0) {
      Logger.tabulationLog(Level.SEVERE, "Encountered %d invalid source(s)", numInvalidSources);
      castVoteRecords = null;
    }

    return castVoteRecords;
  }
}
