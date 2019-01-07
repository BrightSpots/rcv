/*
 * Ranked Choice Voting Universal Tabulator
 * Copyright (c) 2018 Jonathan Moldover, Louis Eisenberg, and Hylton Edingfield
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
 * tabulate contest
 * output results
 */

package com.rcv;

import com.rcv.FileUtils.UnableToCreateDirectoryException;
import com.rcv.StreamingCVRReader.UnrecognizedCandidatesException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.xml.sax.SAXException;

public class Main extends GuiApplication {

  // summaryOutputPath is generated from timestamp + config file
  // we cache it here to help support testing
  private static String summaryOutputPath;

  // function: getSummaryOutputPath
  // purpose: return the last summaryOutputPath generated for tabulation
  // returns: the last getSummaryOutputPath
  public static String getSummaryOutputPath() {
    return summaryOutputPath;
  }

  // function: main
  // purpose: main entry point to the rcv tabulator program
  // param: args command line argument array
  // returns: N/A
  public static void main(String[] args) {
    try {
      Logger.setup();
    } catch (IOException exception) {
      System.err.print(String.format("Failed to start system logging!\n%s", exception.toString()));
    }

    if (args.length == 0) {
      // if no args provided, launch the GUI
      launch(args);
    } else {
      // assume user wants to use CLI
      Logger.log(Level.INFO, "Tabulator is being used via the CLI.");
      // config file for configuring the tabulator
      String configPath = args[0];
      // set config file parent folder as default user folder
      FileUtils.setUserDirectory(new File(configPath).getParent());
      // load configuration
      ContestConfig config = loadContestConfig(configPath);
      if (config != null) {
        executeTabulation(config);
      } else {
        Logger.log(Level.SEVERE, "Aborting because config is invalid.");
      }
    }
    System.exit(0);
  }

  // function: loadContestConfig
  // purpose: attempts to create config object
  // param: path to config file
  // returns: the new ContestConfig object, or null if there was a problem
  public static ContestConfig loadContestConfig(String configPath) {
    // config: the new object
    ContestConfig config = null;

    // rawConfig holds the basic contest config data parsed from json
    // this will be null if there is a problem loading it
    RawContestConfig rawConfig = JsonParser.createRawContestConfigFromFile(configPath);

    // if raw config failed alert user
    if (rawConfig == null) {
      Logger.log(Level.SEVERE, "Failed to load config file:\n%s", configPath);
    } else if (rawConfig.outputSettings == null) {
      Logger.log(
          Level.SEVERE,
          "No 'outputSettings' field specified! Failed to load config file:\n%s",
          configPath);
    } else if (rawConfig.cvrFileSources == null) {
      Logger.log(
          Level.SEVERE,
          "No 'cvrFileSources' field specified! Failed to load config file:\n%s",
          configPath);
    } else if (rawConfig.candidates == null) {
      Logger.log(
          Level.SEVERE,
          "No 'candidates' field specified! Failed to load config file:\n%s",
          configPath);
    } else if (rawConfig.rules == null) {
      Logger.log(
          Level.SEVERE, "No 'rules' field specified! Failed to load config file:\n%s", configPath);
    } else {
      // proceed to create the ContestConfig wrapper
      config = new ContestConfig(rawConfig);
      Logger.log(Level.INFO, "Successfully loaded config file: %s", configPath);
    }

    return config;
  }

  // function: executeTabulation
  // purpose: execute tabulation for given ContestConfig
  // param: config object containing CVR file paths to parse
  // returns: String indicating whether or not execution was successful
  static void executeTabulation(ContestConfig config) {
    Logger.log(Level.INFO, "Starting tabulation process...");

    boolean isTabulationCompleted = false;
    boolean isConfigValid = config.validate();

    if (isConfigValid) {
      boolean isTabulationLogSetUp = false;
      // current date-time formatted as a string used for creating unique output files names
      final String timestampString = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
      // %g format is for log file naming
      String tabulationLogPath =
          Paths.get(config.getOutputDirectory(), String.format("%s_audit_%%g.log",
              timestampString))
              .toAbsolutePath()
              .toString();

      // cache summaryOutputPath for testing
      summaryOutputPath =
          Paths.get(config.getOutputDirectory(), String.format("%s_summary.json",
              timestampString))
              .toAbsolutePath()
              .toString();
      try {
        FileUtils.createOutputDirectory(config.getOutputDirectory());
        Logger.addTabulationFileLogging(tabulationLogPath);
        isTabulationLogSetUp = true;
      } catch (UnableToCreateDirectoryException exception) {
        Logger.log(
            Level.SEVERE,
            "Failed to create output directory: %s\n%s",
            config.getOutputDirectory(),
            exception.toString());
      } catch (IOException exception) {
        Logger.log(
            Level.SEVERE, "Failed to configure tabulation logger!\n%s", exception.toString());
      }

      if (isTabulationLogSetUp) {
        Logger.log(Level.INFO, "Logging tabulation to: %s", tabulationLogPath);
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
            try {
              tabulator.generateSummarySpreadsheet(timestampString);
            } catch (IOException e) {
              Logger.log(Level.SEVERE, "Error writing summary spreadsheet: %s", e.toString());
            }
            isTabulationCompleted = true;
          } else {
            Logger.log(Level.SEVERE, "No cast vote records found.");
          }
        } else {
          Logger.log(Level.SEVERE, "Skipping tabulation due to source file errors.");
        }
        Logger.log(Level.INFO, "Done logging tabulation to: %s", tabulationLogPath);
        Logger.removeTabulationFileLogging();
      }
    }

    if (isTabulationCompleted) {
      Logger.log(Level.INFO, "Tabulation process completed.");
    } else {
      Logger.log(Level.SEVERE, "Unable to complete tabulation process!");
    }
  }

  // function: parseCastVoteRecords
  // purpose: parse CVR files referenced in the ContestConfig object into a list of CastVoteRecords
  // param: config object containing CVR file paths to parse
  // returns: list of all CastVoteRecord objects parsed from CVR files (or null if there's an error)
  private static List<CastVoteRecord> parseCastVoteRecords(ContestConfig config) {
    Logger.log(Level.INFO, "Parsing cast vote records...");

    // castVoteRecords will contain all cast vote records parsed by the reader
    List<CastVoteRecord> castVoteRecords = new ArrayList<>();
    // did we encounter a fatal problem for this source?
    boolean encounteredSourceProblem = false;

    // At each iteration of the following loop, we add records from another source file.
    // source: index over config sources
    for (RawContestConfig.CVRSource source : config.rawConfig.cvrFileSources) {
      Logger.log(Level.INFO, "Reading cast vote record file: %s...", source.getFilePath());
      // the CVRs parsed from this source
      try {
        List<CastVoteRecord> cvrs =
            new StreamingCVRReader(config, source).parseCVRFile(castVoteRecords);
        if (cvrs.isEmpty()) {
          Logger.log(Level.SEVERE, "Source file contains no CVRs: %s", source.getFilePath());
          encounteredSourceProblem = true;
        }
      } catch (UnrecognizedCandidatesException exception) {
        Logger.log(
            Level.SEVERE,
            "Source file contains unrecognized candidate(s): %s",
            source.getFilePath());
        // map from name to number of times encountered
        for (String candidate : exception.candidateCounts.keySet()) {
          Logger.log(
              Level.SEVERE,
              "Unrecognized candidate \"%s\" appears %d time(s).",
              candidate,
              exception.candidateCounts.get(candidate));
        }
        encounteredSourceProblem = true;
      } catch (IOException e) {
        Logger.log(Level.SEVERE, "Error opening source file %s", source.getFilePath());
        encounteredSourceProblem = true;
      } catch (SAXException | OpenXML4JException e) {
        Logger.log(Level.SEVERE, "Error parsing source file %s", source.getFilePath());
        encounteredSourceProblem = true;
      }
    }

    if (!encounteredSourceProblem) {
      Logger.log(Level.INFO, "Parsed %d cast vote records successfully.", castVoteRecords.size());
    } else {
      Logger.log(Level.SEVERE, "Parsing cast vote records failed!");
      castVoteRecords = null;
    }

    return castVoteRecords;
  }
}
