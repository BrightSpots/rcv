/*
 * Ranked Choice Voting Universal Tabulator
 * Copyright (c) 2017-2019 Bright Spots Developers.
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
 */

/*
 * Purpose:
 * TabulatorSession contains the high-level flow for tabulation execution:
 * parse config file
 * parse cast vote records
 * tabulate contest
 * output results
 *
 * TabulatorSession also stores state metadata which exists outside tabulation results including:
 * config object, resolved output, and logging paths, tabulation object, and CVR data including
 * precinct codes discovered while parsing CVR files.
 */

package network.brightspots.rcv;

import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import javax.xml.parsers.ParserConfigurationException;
import network.brightspots.rcv.FileUtils.UnableToCreateDirectoryException;
import network.brightspots.rcv.ResultsWriter.RoundSnapshotDataMissingException;
import network.brightspots.rcv.StreamingCVRReader.UnrecognizedCandidatesException;
import network.brightspots.rcv.Tabulator.TabulationCancelledException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.xml.sax.SAXException;

class TabulatorSession {

  // configPath points to config file we use for configuring tabulation
  private final String configPath;
  // precinct IDs discovered during CVR parsing to support testing
  private final Set<String> precinctIDs = new HashSet<>();
  // Visible for testing: cache output path location
  String outputPath;
  private final String timestampString;

  // function: TabulatorSession
  // purpose: TabulatorSession constructor
  // param: configPath path to config json file
  TabulatorSession(String configPath) {
    this.configPath = configPath;
    // current date-time formatted as a string used for creating unique output files names
    timestampString = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
  }

  String getTimestampString() {
    return timestampString;
  }

  // purpose: special mode to just export the CVR as CDF JSON instead of tabulating
  void convertToCdf() {
    ContestConfig config = ContestConfig.loadContestConfig(configPath);
    if (config != null && config.validate()) {
      try {
        FileUtils.createOutputDirectory(config.getOutputDirectory());
        List<CastVoteRecord> castVoteRecords = parseCastVoteRecords(config, precinctIDs);
        ResultsWriter writer =
            new ResultsWriter()
                .setNumRounds(0)
                .setContestConfig(config)
                .setTimestampString(timestampString)
                .setPrecinctIds(precinctIDs);
        try {
          writer.generateCdfJson(castVoteRecords);
        } catch (RoundSnapshotDataMissingException e) {
          // This will never actually happen because no snapshots are involved when you're just
          // translating the input to CDF, not the tabulation results.
        }
      } catch (IOException | UnableToCreateDirectoryException exception) {
        Logger.log(Level.SEVERE, "CDF JSON generation failed.");
      }
    } else {
      Logger.log(Level.SEVERE, "Failed to load config.");
    }
  }

  // function: tabulate
  // purpose: run tabulation
  // returns: list of winners
  void tabulate() throws TabulationCancelledException {
    Logger.log(Level.INFO, "Starting tabulation session...");
    ContestConfig config = ContestConfig.loadContestConfig(configPath);
    if (config != null && config.validate() && setUpLogging(config)) {
      if (config.isSequentialMultiSeatEnabled()) {
        int numWinners = config.getNumberOfWinners();
        // temporarily set config to single-seat so we can run sequential elections
        config.setNumberOfWinners(1);
        while (config.getSequentialWinners().size() < numWinners) {
          Set<String> newWinnerSet = runTabulationForConfig(config);
          assert (newWinnerSet.size() == 1);
          String newWinner = (String) newWinnerSet.toArray()[0];
          config.setCandidateExclusionStatus(newWinner, true);
          config.addSequentialWinner(newWinner);
        }
        // revert config to original state
        config.setNumberOfWinners(numWinners);
        for (String winner : config.getSequentialWinners()) {
          config.setCandidateExclusionStatus(winner, false);
        }
      } else {
        // normal operation (not sequential multi-seat)
        runTabulationForConfig(config);
      }
      Logger.removeTabulationFileLogging();
    } else {
      Logger.log(Level.SEVERE, "Aborting tabulation!");
    }
  }

  private boolean setUpLogging(ContestConfig config) {
    boolean success = false;

    // %g format is for log file naming
    String tabulationLogPath = Paths
        .get(config.getOutputDirectory(), String.format("%s_audit_%%g.log", timestampString))
        .toAbsolutePath()
        .toString();

    // cache outputPath for testing
    outputPath = config.getOutputDirectory();
    try {
      FileUtils.createOutputDirectory(config.getOutputDirectory());
      Logger.addTabulationFileLogging(tabulationLogPath);
      success = true;
    } catch (UnableToCreateDirectoryException | IOException exception) {
      Logger.log(Level.SEVERE, "Failed to configure tabulation logger!\n%s", exception.toString());
    }
    if (!success) {
      System.out.println("Failed to configure logger!");
      Logger.log(Level.SEVERE, "Failed to configure logger!");
    }
    return success;
  }

  // function: executeTabulation
  // purpose: execute tabulation for given ContestConfig
  // param: config object containing CVR file paths to parse
  // returns: set of winners from tabulation
  private Set<String> runTabulationForConfig(ContestConfig config)
      throws TabulationCancelledException {
    Logger.log(Level.INFO, "Beginning tabulation for config: %s", configPath);
    Set<String> winners = new HashSet<>();
    // Read cast vote records and precinct IDs from CVR files
    List<CastVoteRecord> castVoteRecords = parseCastVoteRecords(config, precinctIDs);
    // parse the cast vote records
    if (castVoteRecords != null) {
      if (!castVoteRecords.isEmpty()) {
        // tabulator for tabulation logic
        Tabulator tabulator = new Tabulator(castVoteRecords, config, precinctIDs);
        // do the tabulation
        winners = tabulator.tabulate();
        // generate visualizer spreadsheet data
        try {
          tabulator.generateSummaryFiles(timestampString);
        } catch (IOException e) {
          Logger.log(Level.SEVERE, "Error writing summary files:\n%s", e.toString());
        }
      } else {
        Logger.log(Level.SEVERE, "No cast vote records found!");
      }
    } else {
      Logger.log(Level.SEVERE, "Skipping tabulation due to source CVR file errors!");
    }

    if (winners.size() > 0) {
      Logger.log(Level.INFO, "Tabulation session completed.  Results written to: %s", outputPath);
    } else {
      Logger.log(Level.SEVERE, "Unable to complete tabulation session!");
    }
    return winners;
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
      try {
        if (source.getProvider().equals("CDF")) {
          CommonDataFormatReader reader = new CommonDataFormatReader(cvrPath, config);
          reader.parseCVRFile(castVoteRecords);
        } else {
          // use xlsx reader for ES&S
          new StreamingCVRReader(config, source).parseCVRFile(castVoteRecords, precinctIDs);
        }
      } catch (UnrecognizedCandidatesException exception) {
        Logger.log(Level.SEVERE, "Source file contains unrecognized candidate(s): %s", cvrPath);
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
      } catch (ParserConfigurationException | SAXException | OpenXML4JException e) {
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
