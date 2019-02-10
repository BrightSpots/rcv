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
 * TabulatorSession contains the high-level flow for tabulation execution:
 * parse config file
 * parse cast vote records
 * tabulate contest
 * output results
 *
 * TabulatorSession also stores state metadata which exists outside tabulation results including:
 * config object, resolved output, and logging paths, tabulation object,  and CVR data including
 * precinct codes discovered while parsing CVR files
 */
package com.rcv;

import com.rcv.FileUtils.UnableToCreateDirectoryException;
import com.rcv.StreamingCVRReader.UnrecognizedCandidatesException;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.xml.sax.SAXException;

class TabulatorSession {

  // configPath points to config file we use for configuring tabulation
  private String configPath;
  // summaryOutputPath is generated from timestamp + config file
  String summaryOutputPath;
  // cache output path location
  String outputPath;

  // precinct IDs discovered during CVR parsing to support testing
  private Set<String> precinctIDs = new HashSet<>();

  // function: TabulatorSession
  // purpose: TabulatorSession constructor
  // param: configPath path to config json file
  TabulatorSession(String configPath) {
    this.configPath = configPath;
  }

  // function: tabulate
  // purpose: run tabulation
  void tabulate() {
    // load configuration
    ContestConfig config = ContestConfig.loadContestConfig(configPath);
    if (config != null) {
      executeTabulation(config);
    } else {
      Logger.log(Level.SEVERE, "Aborting because contest config is invalid!");
    }
  }

  // function: executeTabulation
  // purpose: execute tabulation for given ContestConfig
  // param: config object containing CVR file paths to parse
  // returns: String indicating whether or not execution was successful
  private void executeTabulation(ContestConfig config) {
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

      // cache outputPath for testing
      outputPath = config.getOutputDirectory();
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
        // Read cast vote records and precinct IDs from CVR files
        List<CastVoteRecord> castVoteRecords = parseCastVoteRecords(config, precinctIDs);
        // parse the cast vote records
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
              Logger.log(Level.SEVERE, "Error writing summary spreadsheet:\n%s", e.toString());
            }
            isTabulationCompleted = true;
          } else {
            Logger.log(Level.SEVERE, "No cast vote records found!");
          }
        } else {
          Logger.log(Level.SEVERE, "Skipping tabulation due to source file errors!");
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
  // param: precinctIDs a set of precinctIDs which will be populated during cvr parsing
  // returns: list of parsed CVRs
  private List<CastVoteRecord> parseCastVoteRecords(ContestConfig config, Set<String> precinctIDs) {
    Logger.log(Level.INFO, "Parsing cast vote records...");

    // castVoteRecords will contain all cast vote records parsed by the reader
    List<CastVoteRecord> castVoteRecords = new ArrayList<>();
    // did we encounter a fatal problem for this source?
    boolean encounteredSourceProblem = false;

    // At each iteration of the following loop, we add records from another source file.
    // source: index over config sources
    for (RawContestConfig.CVRSource source : config.rawConfig.cvrFileSources) {
      // cvrPath is the resolved path to this source
      String cvrPath = config.resolveConfigPath(source.getFilePath());

      Logger.log(Level.INFO, "Reading cast vote record file: %s...", cvrPath);
      // the CVRs parsed from this source
      try {
        List<CastVoteRecord> cvrs =
            new StreamingCVRReader(config, source).parseCVRFile(castVoteRecords, precinctIDs);
        if (cvrs.isEmpty()) {
          Logger.log(Level.SEVERE, "Source file contains no CVRs: %s", cvrPath);
          encounteredSourceProblem = true;
        }
      } catch (UnrecognizedCandidatesException exception) {
        Logger.log(
            Level.SEVERE,
            "Source file contains unrecognized candidate(s): %s",
            cvrPath);
        // map from name to number of times encountered
        for (String candidate : exception.candidateCounts.keySet()) {
          Logger.log(
              Level.SEVERE,
              "Unrecognized candidate \"%s\" appears %d time(s)!",
              candidate,
              exception.candidateCounts.get(candidate));
        }
        encounteredSourceProblem = true;
      } catch (IOException e) {
        Logger.log(Level.SEVERE, "Error opening source file %s", cvrPath);
        encounteredSourceProblem = true;
      } catch (SAXException | OpenXML4JException e) {
        Logger.log(Level.SEVERE, "Error parsing source file %s", cvrPath);
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
