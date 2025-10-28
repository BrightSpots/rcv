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
import java.io.File;
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
import java.util.function.BiConsumer;
import network.brightspots.rcv.CastVoteRecord.CvrParseException;
import network.brightspots.rcv.ContestConfig.Provider;
import network.brightspots.rcv.ContestConfig.UnrecognizedProviderException;
import network.brightspots.rcv.FileUtils.UnableToCreateDirectoryException;
import network.brightspots.rcv.OutputWriter.RoundSnapshotDataMissingException;
import network.brightspots.rcv.RawContestConfig.Candidate;
import network.brightspots.rcv.Tabulator.TabulationAbortedException;

@SuppressWarnings("RedundantSuppression")
class TabulatorSession {

  private final String configPath;
  private final String timestampString;
  private String outputPath;
  private String rctabCvrFilePath;

  TabulatorSession(String configPath) {
    this.configPath = configPath;

    // current date-time formatted as a string used for creating unique output files names
    String timestampPattern = "yyyy-MM-dd_HH-mm";
    String baseTimestampString = new SimpleDateFormat(timestampPattern).format(new Date());
    String currTimestampString = baseTimestampString;

    // If there are multiple runs in the same minute, resolve collisions
    // with a dash and an increment.
    ContestConfig config = ContestConfig.loadContestConfig(configPath);
    int count = 1;
    while (new File(config.getOutputDirectory(currTimestampString)).exists()) {
      currTimestampString = baseTimestampString +  "-" + count;
      count++;
    }

    this.timestampString = currTimestampString;
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
  String getRctabCvrFilePath() {
    return rctabCvrFilePath;
  }

  // special mode to just export the CVR as CDF JSON instead of tabulating
  // returns whether it succeeded
  boolean convertToCdf(BiConsumer<Double, Double> progressUpdate) {
    Logger.info("Starting CDF conversion session...");
    ContestConfig config = ContestConfig.loadContestConfig(configPath);
    checkConfigVersionMatchesApp(config);
    boolean conversionSuccess = false;

    Progress progress = new Progress(config, 0, progressUpdate);

    if (setUpLogging(config.getOutputDirectory(timestampString))
        && config.validate().isEmpty()) {
      Logger.info("Converting CVR(s) to CDF...");
      try {
        FileUtils.createOutputDirectory(config.getOutputDirectory(timestampString));
        LoadedCvrData castVoteRecords = parseCastVoteRecords(config, progress, false);
        if (!castVoteRecords.successfullyReadAll) {
          Logger.severe("Aborting conversion due to cast vote record errors!");
        } else {
          Tabulator.SliceIdSet sliceIds =
              new Tabulator(castVoteRecords.getCvrs(), config).getEnabledSliceIds();
          OutputWriter writer =
              new OutputWriter()
                  .setNumRounds(0)
                  .setSliceIds(sliceIds)
                  .setContestConfig(config)
                  .setTimestampString(timestampString);
          writer.generateCdfJson(castVoteRecords.getCvrs());
          conversionSuccess = true;
        }
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

  void convertToCdf() {
    convertToCdf(null);
  }

  LoadedCvrData parseAndCountCastVoteRecords(BiConsumer<Double, Double> progressUpdate)
      throws CastVoteRecordGenericParseException {
    ContestConfig config = ContestConfig.loadContestConfig(configPath);
    Progress progress = new Progress(config, 0, progressUpdate);
    return parseCastVoteRecords(config, progress, false);
  }

  // Returns a List of exception class names that were thrown while tabulating.
  // Operator name is required for the audit logs.
  // Note: An exception MUST be returned any time tabulation does not run.
  // In general, that means any Logger.severe in this function should be accompanied
  // by an exceptionsEncountered.add(...) call.
  List<String> tabulate(
      String operatorName,
      LoadedCvrData expectedCvrData,
      BiConsumer<Double, Double> progressUpdate) {
    Logger.info("Starting tabulation session...");
    List<String> exceptionsEncountered = new LinkedList<>();
    ContestConfig config = ContestConfig.loadContestConfig(configPath);
    checkConfigVersionMatchesApp(config);
    boolean tabulationSuccess = false;
    boolean setUpLoggingSuccess = setUpLogging(config.getOutputDirectory(timestampString));

    if (operatorName == null || operatorName.isBlank()) {
      Logger.severe("Operator name is required for the audit logs!");
      exceptionsEncountered.add(TabulationAbortedException.class.toString());
    } else if (setUpLoggingSuccess && config.validate().isEmpty()) {
      Logger.info("Computer machine name: %s", Utils.getComputerName());
      Logger.info("Computer user name: %s", Utils.getUserName());
      Logger.info("Operator name: %s", operatorName);
      Logger.info("Config file: %s", configPath);

      try {
        Logger.auditable("Begin config file contents:");
        BufferedReader reader =
            new BufferedReader(new FileReader(configPath, StandardCharsets.UTF_8));
        String line = reader.readLine();
        while (line != null) {
          Logger.auditable(line);
          line = reader.readLine();
        }
        Logger.auditable("End config file contents.");
        reader.close();
      } catch (IOException exception) {
        exceptionsEncountered.add(exception.getClass().toString());
        Logger.severe("Error logging config file: %s\n%s", configPath, exception);
      }

      Progress progress = new Progress(config, 0.5f, progressUpdate);
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
            LoadedCvrData castVoteRecords = parseCastVoteRecords(config, progress, true);
            if (config.getSequentialWinners().isEmpty()
                && !castVoteRecords.metadataMatches(expectedCvrData)) {
              Logger.severe("CVR data has changed between loading the CVRs and reading them!");
              exceptionsEncountered.add(TabulationAbortedException.class.toString());
              break;
            }
            newWinnerSet = runTabulationForConfig(config, castVoteRecords.getCvrs(), progress);
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
          LoadedCvrData castVoteRecords = parseCastVoteRecords(config, progress, true);

          try {
            HeapDumpUtil.dumpHeapWithTimestamp(".", "after-cvr-parse", true);
          } catch (IOException e) {
            Logger.warning("Failed to create heap dump: %s", e.getMessage());
          }
          if (!castVoteRecords.metadataMatches(expectedCvrData)) {
            Logger.severe("CVR data has changed between loading the CVRs and reading them!");
            exceptionsEncountered.add(TabulationAbortedException.class.toString());
          } else {
            runTabulationForConfig(config, castVoteRecords.getCvrs(), progress);
            castVoteRecords.printSummary();
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
    return tabulate(operatorName, TabulatorSession.LoadedCvrData.MATCHES_ALL, null);
  }

  Set<String> loadSliceNamesFromCvrs(ContestConfig.TabulateBySlice slice, ContestConfig config) {
    Progress progress = new Progress(config, 0, null);
    try {
      List<CastVoteRecord> castVoteRecords =
          parseCastVoteRecords(config, progress, false).getCvrs();
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
      Logger.addTabulationFileLogging(outputDirectory, timestampString);
      success = true;
    } catch (IOException exception) {
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
      ContestConfig config, List<CastVoteRecord> castVoteRecords, Progress progress)
      throws TabulationAbortedException {
    Set<String> winners;
    Tabulator tabulator = new Tabulator(castVoteRecords, config);
    winners = tabulator.tabulate(progress);
    try {
      tabulator.generateSummaryFiles(timestampString);
    } catch (IOException exception) {
      Logger.severe("Error writing summary files:\n%s", exception);
    }
    return winners;
  }

  /**
   * Parse CVR files referenced in the ContestConfig object into a list of CastVoteRecords.
   *
   * @param config Object containing CVR file paths to parse.
   * @param progress Object tracking progress of parsing the CVRs.
   * @param shouldOutputRcTabCvr Whether to output the simplified RCTab CVR CSV file.
   * @return List of parsed CVRs or null if an error was encountered.
   * @throws CastVoteRecordGenericParseException If any failure occurs when parsing CVRs.
   */
  private LoadedCvrData parseCastVoteRecords(
      ContestConfig config, Progress progress, boolean shouldOutputRcTabCvr)
      throws CastVoteRecordGenericParseException {
    Logger.info("Beginning parsing of all cast vote records from %d configured sources...",
            config.rawConfig.cvrFileSources.size());
    List<CastVoteRecord> castVoteRecords = new ArrayList<>();
    boolean encounteredSourceProblem = false;

    // Per-source data for writing generic CSV
    List<OutputWriter.CvrSourceData> cvrSourceData = new ArrayList<>();

    // At each iteration of the following loop, we add records from another source file.
    for (int sourceIndex = 0; sourceIndex < config.rawConfig.cvrFileSources.size(); ++sourceIndex) {
      RawContestConfig.CvrSource source = config.rawConfig.cvrFileSources.get(sourceIndex);
      String cvrPath = config.resolveConfigPath(source.getFilePath());
      Provider provider = ContestConfig.getProvider(source);
      try {
        BaseCvrReader reader = provider.constructReader(config, source);
        Logger.info("CVR Source %d | Reading %s cast vote records from: %s..."
                ,sourceIndex + 1, reader.readerName(), cvrPath);
        final int startIndex = castVoteRecords.size();
        reader.readCastVoteRecords(castVoteRecords);

        // Update the per-source data for the results writer
        cvrSourceData.add(
            new OutputWriter.CvrSourceData(
                source, reader, sourceIndex, startIndex, castVoteRecords.size() - 1));

          Logger.info("CVR Source %d | Parsed %,d valid cast vote records.",
                  sourceIndex + 1, castVoteRecords.size() - startIndex);

        // Check for unrecognized candidates
        Map<Candidate, Integer> unrecognizedCandidateCounts =
            reader.gatherUnknownCandidateCounts(castVoteRecords, false);

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
                        candidate.getName(), exception.candidateCounts.get(candidate)));
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

      // Update the service % complete
      progress.markFileRead();
    }

    if (encounteredSourceProblem) {
      Logger.severe("Parsing cast vote records failed!");
      castVoteRecords = null;
    } else {
      if (castVoteRecords.isEmpty()) {
        Logger.severe("No cast vote records found!");
        castVoteRecords = null;
      } else {
        Logger.info("Completed parsing all cast vote records. Parsed %,d valid cast vote records successfully"
                + " from %d configured CVR sources."
                , castVoteRecords.size(), config.rawConfig.cvrFileSources.size());

        // Output the RCTab-CSV CVR
        if (shouldOutputRcTabCvr) {
          try {
            OutputWriter writer =
                  new OutputWriter().setContestConfig(config).setTimestampString(timestampString);
            this.rctabCvrFilePath =
                  writer.writeRcTabCvrCsv(
                          castVoteRecords,
                          cvrSourceData,
                          config.getOutputDirectory(timestampString));
          } catch (IOException exception) {
            // error already logged in ResultsWriter
          }
        }
      }
    }

    if (castVoteRecords == null) {
      throw new CastVoteRecordGenericParseException();
    }

    return new LoadedCvrData(castVoteRecords, cvrSourceData);
  }

  static class UnrecognizedCandidatesException extends Exception {

    // count of how many times each unrecognized candidate was encountered during CVR parsing
    final Map<Candidate, Integer> candidateCounts;

    UnrecognizedCandidatesException(Map<Candidate, Integer> candidateCounts) {
      this.candidateCounts = candidateCounts;
    }
  }

  static class CastVoteRecordGenericParseException extends Exception {}

  /**
   * A summary of the cast vote records that have been read. Manages CVR in memory, so you can
   * retain metadata about the loaded CVRs without keeping them all in memory. Use .discard() to
   * free up memory. After memory is freed, all other operations except for getCvrs() are still
   * valid.
   */
  public static class LoadedCvrData {
    public static final LoadedCvrData MATCHES_ALL = new LoadedCvrData();
    public final boolean successfullyReadAll;

    private List<CastVoteRecord> cvrs;
    private final int numCvrs;
    private final List<OutputWriter.CvrSourceData> cvrSourcesData;
    private boolean isDiscarded;
    private final boolean doesMatchAllMetadata;

    LoadedCvrData(List<CastVoteRecord> cvrs, List<OutputWriter.CvrSourceData> cvrSourcesData) {
      this.cvrs = cvrs;
      this.successfullyReadAll = cvrs != null;
      this.numCvrs = cvrs != null ? cvrs.size() : 0;
      this.isDiscarded = false;
      this.doesMatchAllMetadata = false;
      this.cvrSourcesData = cvrSourcesData;
    }

    /**
     * This constructor will cause metadataMatches to always return true, and contains no true
     * statistics.
     */
    private LoadedCvrData() {
      this.cvrs = null;
      this.successfullyReadAll = false;
      this.numCvrs = 0;
      this.isDiscarded = false;
      this.doesMatchAllMetadata = true;
      this.cvrSourcesData = new ArrayList<>();
    }

    /**
     * Currently only checks if the number of CVRs matches, but can be extended to ensure exact
     * matches or meet other needs.
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

    List<OutputWriter.CvrSourceData> getCvrSourcesData() {
      return cvrSourcesData;
    }

    public void discard() {
      cvrs = null;
      isDiscarded = true;
    }

    List<CastVoteRecord> getCvrs() {
      if (isDiscarded) {
        throw new IllegalStateException("CVRs have been discarded from memory.");
      }
      return cvrs;
    }

    public void printSummary() {
      Logger.info("Cast Vote Record summary:");
      for (OutputWriter.CvrSourceData sourceData : cvrSourcesData) {
        Logger.info("Source %d: %s", sourceData.sourceIndex + 1, sourceData.source.getFilePath());
        Logger.info("  uses provider: %s", sourceData.source.getProvider());
        Logger.info("  read %d cast vote records", sourceData.getNumCvrs());
      }
    }
  }
}
