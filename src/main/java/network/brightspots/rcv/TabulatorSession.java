/*
 * RCTab
 * Copyright (c) 2017-2023 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: High-level flow for tabulation execution.
 * Parse config file.
 * Parse cast vote records.
 * Tabulate contest.
 * Output results
 * Design: TabulatorSession also stores state metadata which exists outside tabulation results:
 * config object, resolved output, and logging paths, tabulation object, and CVR data including
 * precinct IDs discovered while parsing CVR files.
 * Conditions: During tabulation, validation, and conversion.
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import network.brightspots.rcv.CastVoteRecord.CvrParseException;
import network.brightspots.rcv.ContestConfig.Provider;
import network.brightspots.rcv.ContestConfig.UnrecognizedProviderException;
import network.brightspots.rcv.FileUtils.UnableToCreateDirectoryException;
import network.brightspots.rcv.ResultsWriter.RoundSnapshotDataMissingException;
import network.brightspots.rcv.Tabulator.TabulationAbortedException;

@SuppressWarnings("RedundantSuppression")
class TabulatorSession {

  private final String configPath;
  private final String timestampString;
  private String outputPath;
  private String convertedFilePath;

  TabulatorSession(String configPath) {
    this.configPath = configPath;
    // current date-time formatted as a string used for creating unique output files names
    timestampString = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
  }

  // validation will catch a mismatch and abort anyway, but let's log helpful errors for the CLI
  // here also
  private static void checkConfigVersionMatchesApp(ContestConfig config) {
    String version = config.getRawConfig().tabulatorVersion;
    if (!version.equals(ContestConfig.AUTOMATED_TEST_VERSION)) {
      // Below already logs a severe message, so no need to check and add another one
      boolean isConfigVersionNewerThanAppVersion =
          ContestConfigMigration.isConfigVersionNewerThanAppVersion(version);
      if (!isConfigVersionNewerThanAppVersion
          && ContestConfigMigration.isConfigVersionOlderThanAppVersion(version)) {
        Logger.severe(
            "Can't use a config with older version %s in newer version %s of the app! To "
                + "automatically migrate the config to the newer version, load it in the graphical "
                + "version of the app (i.e. don't use the --cli flag when starting the tabulator).",
            version, Main.APP_VERSION);
      }
      // No need to throw errors for these, because they'll be caught by validateTabulatorVersion()
      // during validation
    }
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
  String getConvertedFilePath() {
    return convertedFilePath;
  }

  // special mode to just export the CVR as CDF JSON instead of tabulating
  // returns whether it succeeded
  boolean convertToCdf() {
    Logger.info("Starting CDF conversion session...");
    ContestConfig config = ContestConfig.loadContestConfig(configPath);
    checkConfigVersionMatchesApp(config);
    boolean conversionSuccess = false;

    if (setUpLogging(config.getOutputDirectory()) && config.validate().isEmpty()) {
      Logger.info("Converting CVR(s) to CDF...");
      try {
        FileUtils.createOutputDirectory(config.getOutputDirectory());
        LoadedCvrData castVoteRecords = parseCastVoteRecords(config);
        Tabulator.SliceIdSet sliceIds =
            new Tabulator(castVoteRecords.getCvrs(), config).getEnabledSliceIds();
        ResultsWriter writer =
            new ResultsWriter()
                .setNumRounds(0)
                .setSliceIds(sliceIds)
                .setContestConfig(config)
                .setTimestampString(timestampString);
        writer.generateCdfJson(castVoteRecords.getCvrs());
        conversionSuccess = true;
      } catch (IOException
          | UnableToCreateDirectoryException
          | TabulationAbortedException
          | RoundSnapshotDataMissingException
          | CastVoteRecordGenericParseException exception) {
        Logger.severe("Failed to convert CVR(s) to CDF: %s", exception.getMessage());
      }
    }

    if (conversionSuccess) {
      Logger.info("Successfully converted CVR(s) to CDF.");
    } else {
      Logger.severe("Failed to convert CVR(s) to CDF!");
    }

    Logger.info("CDF conversion session completed.");
    Logger.removeTabulationFileLogging();

    return conversionSuccess;
  }

  LoadedCvrData parseAndCountCastVoteRecords() throws CastVoteRecordGenericParseException {
    ContestConfig config = ContestConfig.loadContestConfig(configPath);
    return parseCastVoteRecords(config);
  }

  // Returns a List of exception class names that were thrown while tabulating.
  // Operator name is required for the audit logs.
  // Note: An exception MUST be returned any time tabulation does not run.
  // In general, that means any Logger.severe in this function should be accompanied
  // by an exceptionsEncountered.add(...) call.
  List<String> tabulate(String operatorName, LoadedCvrData expectedCvrData) {
    Logger.info("Starting tabulation session...");
    List<String> exceptionsEncountered = new LinkedList<>();
    ContestConfig config = ContestConfig.loadContestConfig(configPath);
    checkConfigVersionMatchesApp(config);
    boolean tabulationSuccess = false;
    boolean setUpLoggingSuccess = setUpLogging(config.getOutputDirectory());

    if (operatorName == null || operatorName.isBlank()) {
      Logger.severe("Operator name is required for the audit logs!");
      exceptionsEncountered.add(TabulationAbortedException.class.toString());
    } else if (setUpLoggingSuccess && config.validate().isEmpty()) {
      Logger.info("Computer machine name: %s", Utils.getComputerName());
      Logger.info("Computer user name: %s", Utils.getUserName());
      Logger.info("Operator name: %s", operatorName);
      Logger.info("Config file: %s", configPath);

      try {
        Logger.fine("Begin config file contents:");
        BufferedReader reader =
            new BufferedReader(new FileReader(configPath, StandardCharsets.UTF_8));
        String line = reader.readLine();
        while (line != null) {
          Logger.fine(line);
          line = reader.readLine();
        }
        Logger.fine("End config file contents.");
        reader.close();
      } catch (IOException exception) {
        exceptionsEncountered.add(exception.getClass().toString());
        Logger.severe("Error logging config file: %s\n%s", configPath, exception);
      }
      Logger.info("Tabulating '%s'...", config.getContestName());
      if (config.isMultiSeatSequentialWinnerTakesAllEnabled()) {
        Logger.info("This is a multi-pass IRV contest.");
        int numWinners = config.getNumberOfWinners();
        // temporarily set config to single-seat so that we can run sequential elections
        config.setNumberOfWinners(1);
        while (config.getSequentialWinners().size() < numWinners) {
          Logger.info(
              "Beginning tabulation for seat #%d...", config.getSequentialWinners().size() + 1);
          // Read cast vote records and slice IDs from CVR files
          Set<String> newWinnerSet;
          try {
            LoadedCvrData castVoteRecords = parseCastVoteRecords(config);
            if (config.getSequentialWinners().isEmpty()
                    && !castVoteRecords.metadataMatches(expectedCvrData)) {
              Logger.severe("CVR data has changed between loading the CVRs and reading them!");
              exceptionsEncountered.add(TabulationAbortedException.class.toString());
              break;
            }
            newWinnerSet = runTabulationForConfig(config, castVoteRecords.getCvrs());
          } catch (TabulationAbortedException | CastVoteRecordGenericParseException exception) {
            exceptionsEncountered.add(exception.getClass().toString());
            Logger.severe(exception.getMessage());
            break;
          }
          if (newWinnerSet.size() != 1) {
            Logger.severe(
                "Expected to find exactly one new winner and found %d!", newWinnerSet.size());
            exceptionsEncountered.add(TabulationAbortedException.class.toString());
            break;
          }
          String newWinner = (String) newWinnerSet.toArray()[0];
          config.setCandidateExclusionStatus(newWinner, true);
          config.addSequentialWinner(newWinner);
          Logger.info("Tabulation for seat #%d completed.", config.getSequentialWinners().size());
          if (config.getSequentialWinners().size() < numWinners) {
            Logger.info("Excluding %s from the remaining tabulations.", newWinner);
          }
        }
        // revert config to original state
        config.setNumberOfWinners(numWinners);
        config
            .getSequentialWinners()
            .forEach(winner -> config.setCandidateExclusionStatus(winner, false));
        tabulationSuccess = true;
      } else {
        // normal operation (not multi-pass IRV, a.k.a. sequential multi-seat)
        // Read cast vote records and precinct IDs from CVR files
        try {
          LoadedCvrData castVoteRecords = parseCastVoteRecords(config);
          if (!castVoteRecords.metadataMatches(expectedCvrData)) {
            Logger.severe("CVR data has changed between loading the CVRs and reading them!");
            exceptionsEncountered.add(TabulationAbortedException.class.toString());
          } else {
            runTabulationForConfig(config, castVoteRecords.getCvrs());
            tabulationSuccess = true;
          }
        } catch (CastVoteRecordGenericParseException exception) {
          exceptionsEncountered.add(exception.getClass().toString());
          Logger.severe("Aborting tabulation due to cast vote record errors!");
        } catch (TabulationAbortedException exception) {
          exceptionsEncountered.add(exception.getClass().toString());
          Logger.severe(exception.getMessage());
        }
      }
      Logger.info("Tabulation session completed.");
      if (tabulationSuccess) {
        Logger.info("Results written to: %s", outputPath);
      }
    }
    Logger.removeTabulationFileLogging();
    return exceptionsEncountered;
  }

  List<String> tabulate(String operatorName) {
    return tabulate(operatorName, TabulatorSession.LoadedCvrData.MATCHES_ALL);
  }

  Set<String> loadSliceNamesFromCvrs(ContestConfig.TabulateBySlice slice, ContestConfig config) {
    try {
      List<CastVoteRecord> castVoteRecords = parseCastVoteRecords(config).getCvrs();
      return new Tabulator(castVoteRecords, config).getEnabledSliceIds().get(slice);
    } catch (TabulationAbortedException | CastVoteRecordGenericParseException e) {
      throw new RuntimeException(e);
    }
  }

  private boolean setUpLogging(String outputDirectory) {
    boolean success = false;
    // cache outputPath for testing
    outputPath = outputDirectory;
    try {
      FileUtils.createOutputDirectory(outputDirectory);
      Logger.addTabulationFileLogging(outputDirectory, timestampString);
      success = true;
    } catch (UnableToCreateDirectoryException | IOException exception) {
      Logger.severe("Failed to configure tabulation logger!\n%s", exception);
    }
    if (!success) {
      Logger.severe("Failed to configure logger!");
    }
    return success;
  }

  // execute tabulation for given ContestConfig (a Session may comprise multiple tabulations)
  // returns: set of winners from tabulation
  private Set<String> runTabulationForConfig(
      ContestConfig config, List<CastVoteRecord> castVoteRecords)
      throws TabulationAbortedException {
    Set<String> winners;
    Tabulator tabulator = new Tabulator(castVoteRecords, config);
    winners = tabulator.tabulate();
    try {
      tabulator.generateSummaryFiles(timestampString);
    } catch (IOException exception) {
      Logger.severe("Error writing summary files:\n%s", exception);
    }
    return winners;
  }

  // parse CVR files referenced in the ContestConfig object into a list of CastVoteRecords
  // param: config object containing CVR file paths to parse
  // returns: list of parsed CVRs or null if an error was encountered
  private LoadedCvrData parseCastVoteRecords(ContestConfig config)
        throws CastVoteRecordGenericParseException {
    Logger.info("Parsing cast vote records...");
    List<CastVoteRecord> castVoteRecords = new ArrayList<>();
    boolean encounteredSourceProblem = false;

    // Per-source data for writing generic CSV
    List<ResultsWriter.PerSourceDataForCsv> perSourceDataForCsv = new ArrayList<>();

    // At each iteration of the following loop, we add records from another source file.
    for (int sourceIndex = 0; sourceIndex < config.rawConfig.cvrFileSources.size(); ++sourceIndex) {
      RawContestConfig.CvrSource source  = config.rawConfig.cvrFileSources.get(sourceIndex);
      String cvrPath = config.resolveConfigPath(source.getFilePath());
      Provider provider = ContestConfig.getProvider(source);
      try {
        BaseCvrReader reader = provider.constructReader(config, source);
        Logger.info("Reading %s cast vote records from: %s...", reader.readerName(), cvrPath);
        reader.readCastVoteRecords(castVoteRecords);

        // Update the per-source data for the results writer
        perSourceDataForCsv.add(new ResultsWriter.PerSourceDataForCsv(
                source,
                reader,
                sourceIndex,
                castVoteRecords.size() - 1));

        // Check for unrecognized candidates
        Map<String, Integer> unrecognizedCandidateCounts =
            reader.gatherUnknownCandidates(castVoteRecords, false);

        if (!unrecognizedCandidateCounts.isEmpty()) {
          throw new UnrecognizedCandidatesException(unrecognizedCandidateCounts);
        }

        // Check for any other reader-specific validations
        reader.runAdditionalValidations(castVoteRecords);
      } catch (UnrecognizedCandidatesException exception) {
        Logger.severe("Source file contains unrecognized candidate(s): %s", cvrPath);
        // map from name to number of times encountered
        exception
            .candidateCounts
            .keySet()
            .forEach(
                candidate ->
                    Logger.severe(
                        "Unrecognized candidate \"%s\" appears %d time(s)!",
                        candidate, exception.candidateCounts.get(candidate)));
        // various incorrect settings can lead to UnrecognizedCandidatesException, so it's hard
        // to know exactly what the problem is
        Logger.info(
            "Check config settings for candidate names, firstVoteRowIndex, "
                + "firstVoteColumnIndex, and precinctColumnIndex to make sure they are correct!");
        Logger.info("See config_file_documentation.txt for more details.");
        encounteredSourceProblem = true;
      } catch (IOException exception) {
        Logger.severe("Error opening cast vote record file: %s", cvrPath);
        Logger.info("Check file path and permissions and make sure they are correct!");
        encounteredSourceProblem = true;
      } catch (UnrecognizedProviderException exception) {
        Logger.severe(
            "Unrecognized provider \"%s\" in source file: %s", source.getProvider(), cvrPath);
        encounteredSourceProblem = true;
      } catch (CvrParseException exception) {
        encounteredSourceProblem = true;
      } catch (Exception exception) {
        Logger.severe("Unexpected error parsing source file: %s\n%s", cvrPath, exception);
        encounteredSourceProblem = true;
      }
    }

    // Output the RCTab-CSV CVR
    try {
      ResultsWriter writer =
              new ResultsWriter().setContestConfig(config).setTimestampString(timestampString);
      this.convertedFilePath =
              writer.writeRctabCvrCsv(
                      castVoteRecords,
                      perSourceDataForCsv,
                      config.getOutputDirectory());
    } catch (IOException exception) {
      // error already logged in ResultsWriter
    }

    if (encounteredSourceProblem) {
      Logger.severe("Parsing cast vote records failed!");
      castVoteRecords = null;
    } else if (castVoteRecords.isEmpty()) {
      Logger.severe("No cast vote records found!");
      castVoteRecords = null;
    } else {
      Logger.info("Parsed %d cast vote records successfully.", castVoteRecords.size());
    }

    if (castVoteRecords == null) {
      throw new CastVoteRecordGenericParseException();
    }

    return new LoadedCvrData(castVoteRecords);
  }

  static class UnrecognizedCandidatesException extends Exception {

    // count of how many times each unrecognized candidate was encountered during CVR parsing
    final Map<String, Integer> candidateCounts;

    UnrecognizedCandidatesException(Map<String, Integer> candidateCounts) {
      this.candidateCounts = candidateCounts;
    }
  }

  static class CastVoteRecordGenericParseException extends Exception {
  }

  /**
   * A summary of the cast vote records that have been read.
   * Manages CVR in memory, so you can retain metadata about the loaded CVRs without
   * keeping them all in memory. Use .discard() to free up memory. After memory is freed,
   * all other operations except for getCvrs() are still valid.
   */
  public static class LoadedCvrData {
    public static final LoadedCvrData MATCHES_ALL = new LoadedCvrData();
    public final boolean successfullyReadAll;

    private List<CastVoteRecord> cvrs;
    private final int numCvrs;
    private boolean isDiscarded;
    private final boolean doesMatchAllMetadata;

    public LoadedCvrData(List<CastVoteRecord> cvrs) {
      this.cvrs = cvrs;
      this.successfullyReadAll = cvrs != null;
      this.numCvrs = cvrs != null ? cvrs.size() : 0;
      this.isDiscarded = false;
      this.doesMatchAllMetadata = false;
    }

    /**
     * This constructor will cause metadataMatches to always return true.
     */
    private LoadedCvrData() {
      this.cvrs = null;
      this.successfullyReadAll = false;
      this.numCvrs = 0;
      this.isDiscarded = false;
      this.doesMatchAllMetadata = true;
    }

    /**
     * Currently only checks if the number of CVRs matches, but can be extended to ensure
     * exact matches or meet other needs.
     *
     * @param other The loaded CVRs to compare against
     * @return whether the metadata matches
     */
    public boolean metadataMatches(LoadedCvrData other) {
      return other.doesMatchAllMetadata
              || this.doesMatchAllMetadata
              || other.numCvrs() == this.numCvrs();
    }

    public int numCvrs() {
      return numCvrs;
    }

    public void discard() {
      cvrs = null;
      isDiscarded = true;
    }

    public List<CastVoteRecord> getCvrs() {
      if (isDiscarded) {
        throw new IllegalStateException("CVRs have been discarded from memory.");
      }
      return cvrs;
    }
  }
}