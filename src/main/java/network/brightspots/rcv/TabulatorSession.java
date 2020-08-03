/*
 * Universal RCV Tabulator
 * Copyright (c) 2017-2020 Bright Spots Developers.
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

import java.io.BufferedReader;
import java.io.FileReader;
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
import network.brightspots.rcv.CastVoteRecord.CvrParseException;
import network.brightspots.rcv.ContestConfig.Provider;
import network.brightspots.rcv.ContestConfig.UnrecognizedProviderException;
import network.brightspots.rcv.FileUtils.UnableToCreateDirectoryException;
import network.brightspots.rcv.ResultsWriter.RoundSnapshotDataMissingException;
import network.brightspots.rcv.StreamingCvrReader.CvrDataFormatException;
import network.brightspots.rcv.StreamingCvrReader.UnrecognizedCandidatesException;
import network.brightspots.rcv.Tabulator.TabulationCancelledException;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.xml.sax.SAXException;

class TabulatorSession {

  private final String configPath;
  // precinct IDs discovered during CVR parsing to support testing
  private final Set<String> precinctIds = new HashSet<>();
  private final String timestampString;
  private String outputPath;
  private List<String> convertedFilesWritten;

  TabulatorSession(String configPath) {
    this.configPath = configPath;
    // current date-time formatted as a string used for creating unique output files names
    timestampString = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
  }

  // given a dominion style folder path:
  // read associated manifest data
  // read Dominion cvr json into CastVoteRecords
  // write CastVoteRecords to generic cvr csv files: one per contest
  // return list of files written or null if there was a problem
  void convertDominionCvrJsonToGenericCsv(String dominionDataFolder) {
    DominionCvrReader dominionCvrReader = new DominionCvrReader(dominionDataFolder);
    List<CastVoteRecord> castVoteRecords = new ArrayList<>();
    List<String> filesWritten;
    try {
      dominionCvrReader.readCastVoteRecords(castVoteRecords, null);
      ResultsWriter writer = new ResultsWriter().setTimestampString(timestampString);
      filesWritten = writer
          .writeGenericCvrCsv(castVoteRecords, dominionCvrReader.getContests().values(),
              dominionDataFolder, null);
    } catch (Exception exception) {
      Logger.log(Level.SEVERE, "Failed to convert Dominion CVR to CSV:\n%s", exception.toString());
      filesWritten = null;
    }
    this.convertedFilesWritten = filesWritten;
  }

  // Visible for testing
  @SuppressWarnings("unused")
  String getOutputPath() {
    return outputPath;
  }

  // Visible for testing
  @SuppressWarnings("unused")
  String getTimestampString() {
    return timestampString;
  }

  // Visible for testing
  @SuppressWarnings("unused")
  List<String> getConvertedFilesWritten() {
    return convertedFilesWritten;
  }

  // special mode to just export the CVR as CDF JSON instead of tabulating
  void convertToCdf() {
    ContestConfig config = ContestConfig.loadContestConfig(configPath);
    if (config != null && config.validate()) {
      try {
        FileUtils.createOutputDirectory(config.getOutputDirectory());
        List<CastVoteRecord> castVoteRecords = parseCastVoteRecords(config, precinctIds);
        if (castVoteRecords == null) {
          Logger.log(Level.SEVERE, "Aborting conversion due to cast vote record errors!");
        } else {
          ResultsWriter writer =
              new ResultsWriter()
                  .setNumRounds(0)
                  .setContestConfig(config)
                  .setTimestampString(timestampString)
                  .setPrecinctIds(precinctIds);
          try {
            writer.generateCdfJson(castVoteRecords);
          } catch (RoundSnapshotDataMissingException e) {
            // This will never actually happen because no snapshots are involved when you're just
            // translating the input to CDF, not the tabulation results.
          }
        }
      } catch (IOException | UnableToCreateDirectoryException exception) {
        Logger.log(Level.SEVERE, "CDF JSON generation failed.");
      }
    } else {
      Logger.log(Level.SEVERE, "Failed to load config.");
    }
  }

  void tabulate() {
    Logger.log(Level.INFO, "Starting tabulation session...");
    ContestConfig config = ContestConfig.loadContestConfig(configPath);
    boolean tabulationSuccess = false;
    if (config != null && config.validate() && setUpLogging(config)) {
      try {
        Logger.log(Level.INFO, "Computer name: %s", Utils.getComputerName());
        Logger.log(Level.INFO, "User name: %s", Utils.getUserName());
        Logger.log(Level.INFO, "Begin config file contents:");
        BufferedReader reader = new BufferedReader(new FileReader(configPath));
        String line = reader.readLine();
        while (line != null) {
          Logger.log(Level.INFO, line);
          line = reader.readLine();
        }
        Logger.log(Level.INFO, "End config file contents.");
        reader.close();
      } catch (IOException e) {
        Logger.log(Level.SEVERE, "Error logging config file: %s\n%s", configPath, e.toString());
      }
      Logger.log(Level.INFO, "Tabulating '%s'...", config.getContestName());
      if (config.isMultiSeatSequentialWinnerTakesAllEnabled()) {
        Logger.log(Level.INFO, "This is a sequential multi-seat contest.");
        int numWinners = config.getNumberOfWinners();
        // temporarily set config to single-seat so we can run sequential elections
        config.setNumberOfWinners(1);
        while (config.getSequentialWinners().size() < numWinners) {
          Logger.log(
              Level.INFO,
              "Beginning tabulation for seat #%d...",
              config.getSequentialWinners().size() + 1);
          // Read cast vote records and precinct IDs from CVR files
          List<CastVoteRecord> castVoteRecords = parseCastVoteRecords(config, precinctIds);
          if (castVoteRecords == null) {
            Logger.log(Level.SEVERE, "Aborting tabulation due to cast vote record errors!");
            break;
          }
          Set<String> newWinnerSet;
          try {
            newWinnerSet = runTabulationForConfig(config, castVoteRecords);
          } catch (TabulationCancelledException e) {
            Logger.log(Level.SEVERE, "Tabulation was cancelled by the user!");
            break;
          }
          assert newWinnerSet.size() == 1;
          String newWinner = (String) newWinnerSet.toArray()[0];
          config.setCandidateExclusionStatus(newWinner, true);
          config.addSequentialWinner(newWinner);
          Logger.log(
              Level.INFO,
              "Tabulation for seat #%d completed.",
              config.getSequentialWinners().size());
          if (config.getSequentialWinners().size() < numWinners) {
            Logger.log(Level.INFO, "Excluding %s from the remaining tabulations.", newWinner);
          }
        }
        // revert config to original state
        config.setNumberOfWinners(numWinners);
        for (String winner : config.getSequentialWinners()) {
          config.setCandidateExclusionStatus(winner, false);
        }
        tabulationSuccess = true;
      } else {
        // normal operation (not sequential multi-seat)
        // Read cast vote records and precinct IDs from CVR files
        List<CastVoteRecord> castVoteRecords = parseCastVoteRecords(config, precinctIds);
        if (castVoteRecords == null) {
          Logger.log(Level.SEVERE, "Aborting tabulation due to cast vote record errors!");
        } else {
          try {
            runTabulationForConfig(config, castVoteRecords);
            tabulationSuccess = true;
          } catch (TabulationCancelledException e) {
            Logger.log(Level.SEVERE, "Tabulation was cancelled by the user!");
          }
        }
      }
      Logger.log(Level.INFO, "Tabulation session completed.");
      if (tabulationSuccess) {
        Logger.log(Level.INFO, "Results written to: %s", outputPath);
      }
      Logger.removeTabulationFileLogging();
    }
  }

  private boolean setUpLogging(ContestConfig config) {
    boolean success = false;

    // %g format is for log file naming
    String tabulationLogPath =
        Paths.get(config.getOutputDirectory(), String.format("%s_audit_%%g.log", timestampString))
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
      Logger.log(Level.SEVERE, "Failed to configure logger!");
    }
    return success;
  }

  // execute tabulation for given ContestConfig (a Session may comprise multiple tabulations)
  // returns: set of winners from tabulation
  private Set<String> runTabulationForConfig(
      ContestConfig config, List<CastVoteRecord> castVoteRecords)
      throws TabulationCancelledException {
    Set<String> winners;
    Tabulator tabulator = new Tabulator(castVoteRecords, config, precinctIds);
    winners = tabulator.tabulate();
    try {
      tabulator.generateSummaryFiles(timestampString);
    } catch (IOException e) {
      Logger.log(Level.SEVERE, "Error writing summary files:\n%s", e.toString());
    }
    return winners;
  }

  // parse CVR files referenced in the ContestConfig object into a list of CastVoteRecords
  // param: config object containing CVR file paths to parse
  // param: precinctIds a set of precinct IDs which will be populated during cvr parsing
  // returns: list of parsed CVRs or null if an error was encountered
  private List<CastVoteRecord> parseCastVoteRecords(ContestConfig config, Set<String> precinctIds) {
    Logger.log(Level.INFO, "Parsing cast vote records...");
    List<CastVoteRecord> castVoteRecords = new ArrayList<>();
    boolean encounteredSourceProblem = false;

    // At each iteration of the following loop, we add records from another source file.
    for (RawContestConfig.CvrSource source : config.rawConfig.cvrFileSources) {
      String cvrPath = config.resolveConfigPath(source.getFilePath());
      Provider provider = ContestConfig.getProvider(source);
      try {
        if (ContestConfig.isCdf(source)) {
          Logger.log(Level.INFO, "Reading CDF cast vote record file: %s...", cvrPath);
          new CommonDataFormatReader(cvrPath, config, source).parseCvrFile(castVoteRecords);
          continue;
        } else if (ContestConfig.getProvider(source) == Provider.CLEAR_BALLOT) {
          Logger
              .log(Level.INFO, "Reading Clear Ballot cast vote records from file: %s...", cvrPath);
          new ClearBallotCvrReader(cvrPath, config).readCastVoteRecords(castVoteRecords, source.getContestId());
          continue;
        } else if (provider == Provider.DOMINION) {
          Logger.log(Level.INFO, "Reading Dominion cast vote records from folder: %s...", cvrPath);
          DominionCvrReader reader = new DominionCvrReader(cvrPath);
          reader.readCastVoteRecords(castVoteRecords, source.getContestId());
          // Before we tabulate, we output a converted generic CSV for the CVRs.
          try {
            ResultsWriter writer = new ResultsWriter().setTimestampString(timestampString);
            this.convertedFilesWritten = writer.writeGenericCvrCsv(
                castVoteRecords,
                reader.getContests().values(),
                config.getOutputDirectory(),
                source.getContestId());
          } catch (IOException e) {
            // error already logged in ResultsWriter
          }
          continue;
        } else if (provider == Provider.ESS) {
          Logger.log(Level.INFO, "Reading ES&S cast vote record file: %s...", cvrPath);
          new StreamingCvrReader(config, source).parseCvrFile(castVoteRecords, precinctIds);
          continue;
        } else if (provider == Provider.HART) {
          Logger.log(Level.INFO, "Reading Hart cast vote records from folder: %s...", cvrPath);
          new HartCvrReader(cvrPath, source.getContestId(), config)
              .readCastVoteRecordsFromFolder(castVoteRecords);
          continue;
        }
        throw new UnrecognizedProviderException();
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
        // various incorrect settings can lead to UnrecognizedCandidatesException so it's hard
        // to know exactly what the problem is
        Logger.log(
            Level.INFO,
            "Check config settings for candidate names, firstVoteRowIndex, "
                + "firstVoteColumnIndex, and precinctColumnIndex to make sure they are correct!");
        Logger.log(Level.INFO, "See config_file_documentation.txt for more details.");
        encounteredSourceProblem = true;
      } catch (IOException e) {
        Logger.log(Level.SEVERE, "Error opening cast vote record file: %s", cvrPath);
        Logger.log(Level.INFO, "Check file path and permissions and make sure they are correct!");
        encounteredSourceProblem = true;
      } catch (ParserConfigurationException
          | SAXException
          | OpenXML4JException
          | POIXMLException e) {
        Logger.log(Level.SEVERE, "Error parsing source file %s", cvrPath);
        Logger.log(
            Level.INFO,
            "ES&S cast vote record files must be Microsoft Excel Workbook "
                + "format.\nStrict Open XML and Open Office are not supported.");
        encounteredSourceProblem = true;
      } catch (CvrDataFormatException e) {
        Logger.log(Level.SEVERE, "Data format error while parsing source file: %s", cvrPath);
        Logger.log(Level.INFO, "See the log for details.");
        encounteredSourceProblem = true;
      } catch (UnrecognizedProviderException e) {
        Logger.log(Level.SEVERE, "Unrecognized provider \"%s\" in source file: %s",
            source.getProvider(), cvrPath);
        encounteredSourceProblem = true;
      } catch (CvrParseException e) {
        encounteredSourceProblem = true;
      }
    }

    if (encounteredSourceProblem) {
      Logger.log(Level.SEVERE, "Parsing cast vote records failed!");
      castVoteRecords = null;
    } else if (castVoteRecords.isEmpty()) {
      Logger.log(Level.SEVERE, "No cast vote records found!");
      castVoteRecords = null;
    } else {
      Logger.log(Level.INFO, "Parsed %d cast vote records successfully.", castVoteRecords.size());
    }
    return castVoteRecords;
  }
}
