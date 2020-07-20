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
 * Wrapper for RawContestConfig object. This class adds logic for looking up rule enum
 * names, candidate names, various configuration utilities, and cast vote record objects.
 */

package network.brightspots.rcv;

import static network.brightspots.rcv.Utils.isNullOrBlank;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import network.brightspots.rcv.RawContestConfig.Candidate;
import network.brightspots.rcv.RawContestConfig.CvrSource;
import network.brightspots.rcv.Tabulator.OvervoteRule;
import network.brightspots.rcv.Tabulator.TieBreakMode;
import network.brightspots.rcv.Tabulator.WinnerElectionMode;

class ContestConfig {

  // If any booleans are unspecified in config file, they should default to false no matter what
  static final String AUTOMATED_TEST_VERSION = "TEST";
  static final String SUGGESTED_OUTPUT_DIRECTORY = "output";
  static final boolean SUGGESTED_TABULATE_BY_PRECINCT = false;
  static final boolean SUGGESTED_GENERATE_CDF_JSON = false;
  static final boolean SUGGESTED_CANDIDATE_EXCLUDED = false;
  static final boolean SUGGESTED_NON_INTEGER_WINNING_THRESHOLD = false;
  static final boolean SUGGESTED_HARE_QUOTA = false;
  static final boolean SUGGESTED_BATCH_ELIMINATION = false;
  static final boolean SUGGESTED_EXHAUST_ON_DUPLICATE_CANDIDATES = false;
  static final boolean SUGGESTED_TREAT_BLANK_AS_UNDECLARED_WRITE_IN = false;
  static final int SUGGESTED_NUMBER_OF_WINNERS = 1;
  static final int SUGGESTED_DECIMAL_PLACES_FOR_VOTE_ARITHMETIC = 4;
  static final BigDecimal SUGGESTED_MINIMUM_VOTE_THRESHOLD = BigDecimal.ZERO;
  static final int SUGGESTED_MAX_SKIPPED_RANKS_ALLOWED = 1;
  static final WinnerElectionMode SUGGESTED_WINNER_ELECTION_MODE = WinnerElectionMode.STANDARD;
  private static final int MIN_COLUMN_INDEX = 1;
  private static final int MAX_COLUMN_INDEX = 1000;
  private static final int MIN_ROW_INDEX = 1;
  private static final int MAX_ROW_INDEX = 100000;
  private static final int MIN_MAX_RANKINGS_ALLOWED = 1;
  private static final int MIN_MAX_SKIPPED_RANKS_ALLOWED = 0;
  private static final int MIN_NUMBER_OF_WINNERS = 0;
  private static final int MIN_DECIMAL_PLACES_FOR_VOTE_ARITHMETIC = 1;
  private static final int MAX_DECIMAL_PLACES_FOR_VOTE_ARITHMETIC = 20;
  private static final int MIN_MINIMUM_VOTE_THRESHOLD = 0;
  private static final int MAX_MINIMUM_VOTE_THRESHOLD = 1000000;
  private static final int MIN_MULTI_SEAT_BOTTOMS_UP_PERCENTAGE_THRESHOLD = 1;
  private static final int MAX_MULTI_SEAT_BOTTOMS_UP_PERCENTAGE_THRESHOLD = 100;
  private static final long MIN_RANDOM_SEED = -140737488355328L;
  private static final long MAX_RANDOM_SEED = 140737488355327L;
  private static final String JSON_EXTENSION = ".json";
  private static final String MAX_SKIPPED_RANKS_ALLOWED_UNLIMITED_OPTION = "unlimited";
  private static final String MAX_RANKINGS_ALLOWED_NUM_CANDIDATES_OPTION = "max";
  static final String SUGGESTED_MAX_RANKINGS_ALLOWED = MAX_RANKINGS_ALLOWED_NUM_CANDIDATES_OPTION;

  static final String UNDECLARED_WRITE_INS = "Undeclared Write-ins";

  static boolean isCdf(CvrSource source) {
    return getProvider(source) == Provider.CDF
        && source.getFilePath() != null
        && source.getFilePath().toLowerCase().endsWith(JSON_EXTENSION);
  }

  // underlying rawConfig object data
  final RawContestConfig rawConfig;
  // this is used if we have a permutation-based tie-break mode
  private final ArrayList<String> candidatePermutation = new ArrayList<>();
  private final Set<String> excludedCandidates = new HashSet<>();
  // path from which any relative paths should be resolved
  private final String sourceDirectory;
  // used to track a sequential multi-seat race
  private final List<String> sequentialWinners = new LinkedList<>();
  // mapping from candidate code to full name
  private Map<String, String> candidateCodeToNameMap;
  // whether or not there are any validation errors
  private boolean isValid;

  private ContestConfig(RawContestConfig rawConfig, String sourceDirectory) {
    this.rawConfig = rawConfig;
    this.sourceDirectory = sourceDirectory;
  }

  static ContestConfig loadContestConfig(RawContestConfig rawConfig, String sourceDirectory) {
    ContestConfig config = new ContestConfig(rawConfig, sourceDirectory);
    try {
      config.processCandidateData();
    } catch (Exception e) {
      Logger.log(Level.SEVERE, "Error processing candidate data:\n%s", e.toString());
      config = null;
    }
    return config;
  }

  // create rawContestConfig from file - can fail for IO issues or invalid json
  // returns: new ContestConfig object if checks pass otherwise null
  static ContestConfig loadContestConfig(String configPath, boolean silentMode) {
    if (configPath == null) {
      Logger.log(Level.SEVERE, "No contest config path specified!");
      return null;
    }
    ContestConfig config = null;
    RawContestConfig rawConfig = JsonParser.readFromFile(configPath, RawContestConfig.class);
    if (rawConfig == null) {
      Logger.log(Level.SEVERE, "Failed to load contest config: %s", configPath);
    } else {
      if (!silentMode) {
        Logger.log(Level.INFO, "Successfully loaded contest config: %s", configPath);
      }
      // parent folder is used as the default source folder
      // if there is no parent folder use current working directory
      String parentFolder = new File(configPath).getParent();
      if (parentFolder == null) {
        parentFolder = System.getProperty("user.dir");
      }
      config = loadContestConfig(rawConfig, parentFolder);
    }
    return config;
  }

  static ContestConfig loadContestConfig(String configPath) {
    return loadContestConfig(configPath, false);
  }

  static boolean passesBasicCvrSourceValidation(CvrSource source) {
    boolean sourceValid = true;
    // perform checks on source input path
    if (isNullOrBlank(source.getFilePath())) {
      sourceValid = false;
      Logger.log(Level.SEVERE, "filePath is required for each cast vote record file!");
    } else {
      Provider provider = getProvider(source);
      if (provider == Provider.PROVIDER_UNKNOWN) {
        sourceValid = false;
        Logger.log(Level.SEVERE, "Invalid provider for source: %s", source.getFilePath());
      } else if (provider == Provider.ESS) {
        // ensure valid first vote column value
        if (fieldOutOfRangeOrNotInteger(
            source.getFirstVoteColumnIndex(),
            "firstVoteColumnIndex",
            MIN_COLUMN_INDEX,
            MAX_COLUMN_INDEX,
            true,
            source.getFilePath())) {
          sourceValid = false;
        }

        // ensure valid first vote row value
        if (fieldOutOfRangeOrNotInteger(
            source.getFirstVoteRowIndex(),
            "firstVoteRowIndex",
            MIN_ROW_INDEX,
            MAX_ROW_INDEX,
            true,
            source.getFilePath())) {
          sourceValid = false;
        }

        // ensure valid id column value
        if (fieldOutOfRangeOrNotInteger(
            source.getIdColumnIndex(),
            "idColumnIndex",
            MIN_COLUMN_INDEX,
            MAX_COLUMN_INDEX,
            false,
            source.getFilePath())) {
          sourceValid = false;
        }

        // ensure valid precinct column value
        if (fieldOutOfRangeOrNotInteger(
            source.getPrecinctColumnIndex(),
            "precinctColumnIndex",
            MIN_COLUMN_INDEX,
            MAX_COLUMN_INDEX,
            false,
            source.getFilePath())) {
          sourceValid = false;
        }
      } else {
        if (fieldIsDefinedButShouldNotBeForProvider(
            source.getFirstVoteColumnIndex(),
            "firstVoteColumnIndex",
            provider,
            source.getFilePath())) {
          sourceValid = false;
        }

        if (fieldIsDefinedButShouldNotBeForProvider(
            source.getFirstVoteRowIndex(),
            "firstVoteRowIndex",
            provider,
            source.getFilePath())) {
          sourceValid = false;
        }

        if (fieldIsDefinedButShouldNotBeForProvider(
            source.getIdColumnIndex(),
            "idColumnIndex",
            provider,
            source.getFilePath())) {
          sourceValid = false;
        }

        if (fieldIsDefinedButShouldNotBeForProvider(
            source.getPrecinctColumnIndex(),
            "precinctColumnIndex",
            provider,
            source.getFilePath())) {
          sourceValid = false;
        }
      }
    }
    return sourceValid;
  }

  // function: stringMatchesAnotherFieldValue(
  // purpose: Checks to make sure string value of one field doesn't match value of another field
  // param: string string to check
  // param: field field name of provided string
  // param: otherFieldValue string value of the other field
  // param: otherField name of the other field
  private static boolean stringMatchesAnotherFieldValue(
      String string, String field, String otherFieldValue, String otherField) {
    boolean match = false;
    if (!field.equals(otherField)) {
      if (!isNullOrBlank(otherFieldValue) && otherFieldValue.equalsIgnoreCase(string)) {
        match = true;
        Logger.log(
            Level.SEVERE,
            "\"%s\" can't be used as %s if it's also being used as %s!",
            string,
            field,
            otherField);
      }
    }
    return match;
  }

  private static void logErrorWithLocation(String message, String inputLocation) {
    message += inputLocation == null ? "!" : ": " + inputLocation;
    Logger.log(Level.SEVERE, message);
  }

  // Returns true if field value can't be converted to a long or isn't within supplied boundaries
  private static boolean fieldOutOfRangeOrNotInteger(
      String value, String fieldName, long lowerBoundary, long upperBoundary, boolean isRequired) {
    return fieldOutOfRangeOrNotInteger(
        value, fieldName, lowerBoundary, upperBoundary, isRequired, null);
  }

  // Returns true if field value can't be converted to a long or isn't within supplied boundaries
  private static boolean fieldOutOfRangeOrNotInteger(
      String value,
      String fieldName,
      long lowerBoundary,
      long upperBoundary,
      boolean isRequired,
      String inputLocation) {
    // "integer" in the mathematical sense, not the Java sense
    String message = String.format("%s must be an integer", fieldName);
    if (lowerBoundary == upperBoundary) {
      message += String.format(" equal to %d", lowerBoundary);
    } else {
      message += String.format(" from %d to %d", lowerBoundary, upperBoundary);
    }
    boolean stringValid = true;
    if (isNullOrBlank(value)) {
      if (isRequired) {
        stringValid = false;
        logErrorWithLocation(message, inputLocation);
      }
    } else {
      try {
        long stringLong = Long.parseLong(value);
        if (stringLong < lowerBoundary || stringLong > upperBoundary) {
          if (!isRequired) {
            message += " if supplied";
          }
          stringValid = false;
          logErrorWithLocation(message, inputLocation);
        }
      } catch (NumberFormatException e) {
        if (!isRequired) {
          message += " if supplied";
        }
        stringValid = false;
        logErrorWithLocation(message, inputLocation);
      }
    }
    return !stringValid;
  }

  private static boolean fieldIsDefinedButShouldNotBeForProvider(String value, String fieldName,
      Provider provider, String inputLocation) {
    boolean stringValid = true;
    if (!isNullOrBlank(value)) {
      stringValid = false;
      logErrorWithLocation(String
          .format("%s should not be defined for CVR source with provider \"%s\"", fieldName,
              provider.toString()), inputLocation);
    }
    return !stringValid;
  }


  static Provider getProvider(CvrSource cvrSource) {
    Provider provider = Provider.getByLabel(cvrSource.getProvider());
    return provider == null ? Provider.PROVIDER_UNKNOWN : provider;
  }

  static boolean passesBasicCandidateValidation(Candidate candidate) {
    boolean candidateValid = true;
    if (isNullOrBlank(candidate.getName())) {
      candidateValid = false;
      Logger.log(Level.SEVERE, "A name is required for each candidate!");
    }
    return candidateValid;
  }

  // given a relative or absolute path returns absolute path for use in File IO
  String resolveConfigPath(String configPath) {
    File userFile = new File(configPath);
    String resolvedPath;
    if (userFile.isAbsolute()) {
      resolvedPath = userFile.getAbsolutePath();
    } else {
      resolvedPath = Paths.get(sourceDirectory, configPath).toAbsolutePath().toString();
    }
    return resolvedPath;
  }

  RawContestConfig getRawConfig() {
    return rawConfig;
  }

  boolean validate() {
    Logger.log(Level.INFO, "Validating contest config...");
    isValid = true;
    validateTabulatorVersion();
    validateOutputSettings();
    validateCvrFileSources();
    validateCandidates();
    validateRules();
    if (isValid) {
      Logger.log(Level.INFO, "Contest config validation successful.");
    } else {
      Logger.log(
          Level.SEVERE,
          "Contest config validation failed! Please modify the contest config file and try again.\n"
              + "See config_file_documentation.txt for more details.");
    }

    return isValid;
  }

  // version validation and migration logic goes here
  // e.g. unsupported versions would fail or be migrated
  // in this release we support only the current app version
  private void validateTabulatorVersion() {
    if (isNullOrBlank(getTabulatorVersion())) {
      isValid = false;
      Logger.log(Level.SEVERE, "tabulatorVersion is required!");
    } else {
      // ignore this check for test data, but otherwise require version to match current app version
      if (!getTabulatorVersion().equals(AUTOMATED_TEST_VERSION) && !getTabulatorVersion()
          .equals(Main.APP_VERSION)) {
        isValid = false;
        Logger.log(Level.SEVERE, "tabulatorVersion %s not supported!", getTabulatorVersion());
      }
    }
    if (!isValid) {
      Logger.log(Level.SEVERE, "tabulatorVersion must be set to %s!", Main.APP_VERSION);
    }
  }

  private void validateOutputSettings() {
    if (isNullOrBlank(getContestName())) {
      isValid = false;
      Logger.log(Level.SEVERE, "contestName is required!");
    }
  }

  // function: stringAlreadyInUseElsewhere
  // purpose: Checks to make sure string isn't reserved or used by other fields
  // param: string string to check
  // param: field field name of provided string
  private boolean stringAlreadyInUseElsewhere(String string, String field) {
    boolean inUse = false;
    for (String reservedString : TallyTransfers.RESERVED_STRINGS) {
      if (string.equalsIgnoreCase(reservedString)) {
        inUse = true;
        Logger.log(
            Level.SEVERE, "\"%s\" is a reserved term and can't be used for %s!", string, field);
        break;
      }
    }
    if (!inUse) {
      inUse =
          stringMatchesAnotherFieldValue(string, field, getOvervoteLabel(), "overvoteLabel")
              || stringMatchesAnotherFieldValue(
              string, field, getUndervoteLabel(), "undervoteLabel")
              || stringMatchesAnotherFieldValue(
              string, field, getUndeclaredWriteInLabel(), "undeclaredWriteInLabel");
    }
    return inUse;
  }

  // checks for conflicts between a candidate name and other name/codes or other reserved strings
  // param: candidateString is a candidate name or code
  // param: field is either "name" or "code"
  // param: candidateStringsSeen is a running set of names/codes we've already encountered
  private boolean candidateStringAlreadyInUseElsewhere(
      String candidateString, String field, Set<String> candidateStringsSeen) {
    boolean inUse;
    if (candidateStringsSeen.contains(candidateString)) {
      inUse = true;
      Logger.log(
          Level.SEVERE, "Duplicate candidate %ss are not allowed: %s", field, candidateString);
    } else {
      inUse = stringAlreadyInUseElsewhere(candidateString, "a candidate " + field);
    }
    return inUse;
  }

  private void validateCvrFileSources() {
    if (rawConfig.cvrFileSources == null || rawConfig.cvrFileSources.isEmpty()) {
      isValid = false;
      Logger.log(Level.SEVERE, "Contest config must contain at least 1 cast vote record file!");
    } else {
      HashSet<String> cvrFilePathSet = new HashSet<>();
      for (CvrSource source : rawConfig.cvrFileSources) {
        if (!passesBasicCvrSourceValidation(source)) {
          isValid = false;
        }

        String cvrPath =
            isNullOrBlank(source.getFilePath()) ? null : resolveConfigPath(source.getFilePath());

        // look for duplicate paths
        if (cvrFilePathSet.contains(cvrPath)) {
          isValid = false;
          Logger.log(
              Level.SEVERE, "Duplicate cast vote record filePaths are not allowed: %s", cvrPath);
        } else {
          cvrFilePathSet.add(cvrPath);
        }

        // ensure file exists
        if (cvrPath != null && !new File(cvrPath).exists()) {
          isValid = false;
          Logger.log(Level.SEVERE, "Cast vote record file not found: %s", cvrPath);
        }

        if (isCdf(source)) {
          // perform CDF checks
          if (rawConfig.cvrFileSources.size() != 1) {
            isValid = false;
            Logger.log(Level.SEVERE, "CDF files must be tabulated individually.");
          }
          if (isTabulateByPrecinctEnabled()) {
            isValid = false;
            Logger.log(Level.SEVERE, "tabulateByPrecinct may not be used with CDF files.");
          }
        } else {
          // perform ES&S checks
          if (isNullOrBlank(source.getPrecinctColumnIndex()) && isTabulateByPrecinctEnabled()) {
            isValid = false;
            Logger.log(
                Level.SEVERE,
                "precinctColumnIndex is required when tabulateByPrecinct is enabled: %s",
                cvrPath);
          }
        }

        Provider provider = getProvider(source);

        if (isNullOrBlank(getContestId()) &&
            (provider == Provider.DOMINION || provider == Provider.HART)) {
          isValid = false;
          Logger.log(
              Level.SEVERE,
              "contestId is required for Dominion and Hart files.");
        } else if (!isNullOrBlank(getContestId()) &&
            !(provider == Provider.DOMINION || provider == Provider.HART)) {
          isValid = false;
          Logger.log(
              Level.SEVERE,
              "contestId may not be used with this type of CVR file.");
        }
      }
    }
  }

  private void validateCandidates() {
    Set<String> candidateNameSet = new HashSet<>();
    Set<String> candidateCodeSet = new HashSet<>();

    for (Candidate candidate : rawConfig.candidates) {
      if (!passesBasicCandidateValidation(candidate)) {
        isValid = false;
      }

      if (!isNullOrBlank(candidate.getName())) {
        if (candidateStringAlreadyInUseElsewhere(candidate.getName(), "name", candidateNameSet)) {
          isValid = false;
        } else {
          candidateNameSet.add(candidate.getName());
        }
      }

      if (!isNullOrBlank(candidate.getCode())) {
        if (candidateStringAlreadyInUseElsewhere(candidate.getCode(), "code", candidateCodeSet)) {
          isValid = false;
        } else {
          candidateCodeSet.add(candidate.getCode());
        }
      }
    }

    if (candidateCodeSet.size() > 0 && candidateCodeSet.size() != candidateNameSet.size()) {
      isValid = false;
      Logger.log(
          Level.SEVERE,
          "If candidate codes are used, a unique code is required for each candidate!");
    }

    if (getNumDeclaredCandidates() < 1) {
      isValid = false;
      Logger.log(Level.SEVERE, "Contest config must contain at least 1 declared candidate!");
    } else if (getNumDeclaredCandidates() == excludedCandidates.size()) {
      isValid = false;
      Logger.log(Level.SEVERE, "Contest config must contain at least 1 non-excluded candidate!");
    }
  }

  private void validateRules() {
    if (getTiebreakMode() == TieBreakMode.MODE_UNKNOWN) {
      isValid = false;
      Logger.log(Level.SEVERE, "Invalid tiebreakMode!");
    }

    if (needsRandomSeed() && isNullOrBlank(getRandomSeedRaw())) {
      isValid = false;
      Logger.log(
          Level.SEVERE,
          "When tiebreakMode involves a random element, randomSeed must be supplied.");
    }
    if (fieldOutOfRangeOrNotInteger(
        getRandomSeedRaw(), "randomSeed", MIN_RANDOM_SEED, MAX_RANDOM_SEED, false)) {
      isValid = false;
    }

    if (getOvervoteRule() == OvervoteRule.RULE_UNKNOWN) {
      isValid = false;
      Logger.log(Level.SEVERE, "Invalid overvoteRule!");
    } else if (!isNullOrBlank(getOvervoteLabel())
        && getOvervoteRule() != Tabulator.OvervoteRule.EXHAUST_IMMEDIATELY
        && getOvervoteRule() != Tabulator.OvervoteRule.ALWAYS_SKIP_TO_NEXT_RANK) {
      isValid = false;
      Logger.log(
          Level.SEVERE,
          "When overvoteLabel is supplied, overvoteRule must be either exhaustImmediately "
              + "or alwaysSkipToNextRank!");
    }

    if (getWinnerElectionMode() == WinnerElectionMode.MODE_UNKNOWN) {
      isValid = false;
      Logger.log(Level.SEVERE, "Invalid winnerElectionMode!");
    }

    if (getMaxRankingsAllowed() == null
        || (getNumDeclaredCandidates() >= 1
        && getMaxRankingsAllowed() < MIN_MAX_RANKINGS_ALLOWED)) {
      isValid = false;
      Logger.log(
          Level.SEVERE,
          "maxRankingsAllowed must either be \"%s\" or an integer from %d to %d!",
          MAX_RANKINGS_ALLOWED_NUM_CANDIDATES_OPTION,
          MIN_MAX_RANKINGS_ALLOWED,
          Integer.MAX_VALUE);
    }

    if (getMaxSkippedRanksAllowed() == null
        || getMaxSkippedRanksAllowed() < MIN_MAX_SKIPPED_RANKS_ALLOWED) {
      isValid = false;
      Logger.log(
          Level.SEVERE,
          "maxSkippedRanksAllowed must either be \"%s\" or an integer from %d to %d!",
          MAX_SKIPPED_RANKS_ALLOWED_UNLIMITED_OPTION,
          MIN_MAX_SKIPPED_RANKS_ALLOWED,
          Integer.MAX_VALUE);
    }

    if (fieldOutOfRangeOrNotInteger(
        getNumberOfWinnersRaw(),
        "numberOfWinners",
        MIN_NUMBER_OF_WINNERS,
        getNumDeclaredCandidates() < 1 ? Integer.MAX_VALUE : getNumDeclaredCandidates(),
        true)) {
      isValid = false;
    }

    if (fieldOutOfRangeOrNotInteger(
        getDecimalPlacesForVoteArithmeticRaw(),
        "decimalPlacesForVoteArithmetic",
        MIN_DECIMAL_PLACES_FOR_VOTE_ARITHMETIC,
        MAX_DECIMAL_PLACES_FOR_VOTE_ARITHMETIC,
        true)) {
      isValid = false;
    }

    if (fieldOutOfRangeOrNotInteger(
        getMinimumVoteThresholdRaw(),
        "minimumVoteThreshold",
        MIN_MINIMUM_VOTE_THRESHOLD,
        MAX_MINIMUM_VOTE_THRESHOLD,
        true)) {
      isValid = false;
    }

    if (fieldOutOfRangeOrNotInteger(
        getMultiSeatBottomsUpPercentageThresholdRaw(),
        "multiSeatBottomsUpPercentageThreshold",
        MIN_MULTI_SEAT_BOTTOMS_UP_PERCENTAGE_THRESHOLD,
        MAX_MULTI_SEAT_BOTTOMS_UP_PERCENTAGE_THRESHOLD,
        false)) {
      isValid = false;
    }

    if (Utils.isInt(getNumberOfWinnersRaw())) {
      if (getNumberOfWinners() > 0) {
        if (getMultiSeatBottomsUpPercentageThreshold() != null) {
          isValid = false;
          Logger.log(
              Level.SEVERE,
              "numberOfWinners must be zero if multiSeatBottomsUpPercentageThreshold is "
                  + "specified!");
        }

        if (getNumberOfWinners() > 1) {
          if (isSingleSeatContinueUntilTwoCandidatesRemainEnabled()) {
            isValid = false;
            Logger.log(
                Level.SEVERE,
                "winnerElectionMode can't be singleSeatContinueUntilTwoCandidatesRemain in a "
                    + "multi-seat contest!");
          }

          if (isBatchEliminationEnabled()) {
            isValid = false;
            Logger.log(Level.SEVERE, "batchElimination can't be true in a multi-seat contest!");
          }
        } else { // numberOfWinners == 1
          if (isMultiSeatSequentialWinnerTakesAllEnabled()) {
            isValid = false;
            Logger.log(
                Level.SEVERE,
                "winnerElectionMode can't be multiSeatSequentialWinnerTakesAll in a single-seat "
                    + "contest!");
          } else if (isMultiSeatBottomsUpEnabled()) {
            isValid = false;
            Logger.log(
                Level.SEVERE,
                "winnerElectionMode can't be multiSeatBottomsUp in a single-seat contest!");
          } else if (isMultiSeatAllowOnlyOneWinnerPerRoundEnabled()) {
            isValid = false;
            Logger.log(
                Level.SEVERE,
                "winnerElectionMode can't be multiSeatAllowOnlyOneWinnerPerRound in a single-seat "
                    + "contest!");
          }

          if (isHareQuotaEnabled()) {
            isValid = false;
            Logger.log(Level.SEVERE, "hareQuota can only be true in a multi-seat contest!");
          }
        }
      } else { // numberOfWinners == 0
        if (!isMultiSeatBottomsUpEnabled()) {
          isValid = false;
          Logger.log(
              Level.SEVERE,
              "numberOfWinners can't be zero unless winnerElectionMode is multiSeatBottomsUp!");
        } else if (getMultiSeatBottomsUpPercentageThreshold() == null) {
          isValid = false;
          Logger.log(
              Level.SEVERE,
              "If winnerElectionMode is multiSeatBottomsUp and numberOfWinners is zero, "
                  + "multiSeatBottomsUpPercentageThreshold must be specified.");
        }
      }
    }

    if (isMultiSeatBottomsUpEnabled() && isBatchEliminationEnabled()) {
      isValid = false;
      Logger.log(
          Level.SEVERE,
          "batchElimination can't be true when winnerElectionMode is multiSeatBottomsUp!");
    }

    if (!isNullOrBlank(getOvervoteLabel())
        && stringAlreadyInUseElsewhere(getOvervoteLabel(), "overvoteLabel")) {
      isValid = false;
    }
    if (!isNullOrBlank(getUndervoteLabel())
        && stringAlreadyInUseElsewhere(getUndervoteLabel(), "undervoteLabel")) {
      isValid = false;
    }
    if (!isNullOrBlank(getUndeclaredWriteInLabel())
        && stringAlreadyInUseElsewhere(getUndeclaredWriteInLabel(), "undeclaredWriteInLabel")) {
      isValid = false;
    }

    if (isTreatBlankAsUndeclaredWriteInEnabled() && isNullOrBlank(getUndeclaredWriteInLabel())) {
      isValid = false;
      Logger.log(
          Level.SEVERE,
          "undeclaredWriteInLabel must be supplied if treatBlankAsUndeclaredWriteIn is true!");
    }
  }

  private String getNumberOfWinnersRaw() {
    return rawConfig.rules.numberOfWinners;
  }

  Integer getNumberOfWinners() {
    return Integer.parseInt(getNumberOfWinnersRaw());
  }

  void setNumberOfWinners(int numberOfWinners) {
    rawConfig.rules.numberOfWinners = Integer.toString(numberOfWinners);
  }

  private String getMultiSeatBottomsUpPercentageThresholdRaw() {
    return rawConfig.rules.multiSeatBottomsUpPercentageThreshold;
  }

  BigDecimal getMultiSeatBottomsUpPercentageThreshold() {
    return getMultiSeatBottomsUpPercentageThresholdRaw() != null
        && !getMultiSeatBottomsUpPercentageThresholdRaw().isBlank()
        ? divide(new BigDecimal(getMultiSeatBottomsUpPercentageThresholdRaw()), new BigDecimal(100))
        : null;
  }

  List<String> getSequentialWinners() {
    return sequentialWinners;
  }

  void addSequentialWinner(String winner) {
    sequentialWinners.add(winner);
  }

  private String getDecimalPlacesForVoteArithmeticRaw() {
    return rawConfig.rules.decimalPlacesForVoteArithmetic;
  }

  Integer getDecimalPlacesForVoteArithmetic() {
    return Integer.parseInt(getDecimalPlacesForVoteArithmeticRaw());
  }

  WinnerElectionMode getWinnerElectionMode() {
    WinnerElectionMode mode = WinnerElectionMode.getByLabel(rawConfig.rules.winnerElectionMode);
    return mode == null ? WinnerElectionMode.MODE_UNKNOWN : mode;
  }

  boolean isSingleSeatContinueUntilTwoCandidatesRemainEnabled() {
    return getWinnerElectionMode()
        == WinnerElectionMode.SINGLE_SEAT_CONTINUE_UNTIL_TWO_CANDIDATES_REMAIN;
  }

  boolean isMultiSeatAllowOnlyOneWinnerPerRoundEnabled() {
    return getWinnerElectionMode() == WinnerElectionMode.MULTI_SEAT_ALLOW_ONLY_ONE_WINNER_PER_ROUND;
  }

  boolean isMultiSeatBottomsUpEnabled() {
    return getWinnerElectionMode() == WinnerElectionMode.MULTI_SEAT_BOTTOMS_UP;
  }

  boolean isMultiSeatBottomsUpWithThresholdEnabled() {
    return getWinnerElectionMode() == WinnerElectionMode.MULTI_SEAT_BOTTOMS_UP
        && getMultiSeatBottomsUpPercentageThreshold() != null;
  }

  boolean isMultiSeatSequentialWinnerTakesAllEnabled() {
    return getWinnerElectionMode() == WinnerElectionMode.MULTI_SEAT_SEQUENTIAL_WINNER_TAKES_ALL;
  }

  boolean isNonIntegerWinningThresholdEnabled() {
    return rawConfig.rules.nonIntegerWinningThreshold;
  }

  boolean isHareQuotaEnabled() {
    return rawConfig.rules.hareQuota;
  }

  // perform a division operation according to the config settings
  BigDecimal divide(BigDecimal dividend, BigDecimal divisor) {
    return dividend.divide(divisor, getDecimalPlacesForVoteArithmetic(), RoundingMode.DOWN);
  }

  BigDecimal multiply(BigDecimal multiplier, BigDecimal multiplicand) {
    return multiplier
        .multiply(multiplicand)
        .setScale(getDecimalPlacesForVoteArithmetic(), RoundingMode.DOWN);
  }

  // returns output directory from config file
  String getOutputDirectoryRaw() {
    return rawConfig.outputSettings.outputDirectory;
  }

  // path to directory where output files should be written
  String getOutputDirectory() {
    return resolveConfigPath(getOutputDirectoryRaw());
  }

  private String getTabulatorVersion() {
    return rawConfig.tabulatorVersion;
  }

  String getContestName() {
    return rawConfig.outputSettings.contestName;
  }

  String getContestId() {
    return rawConfig.outputSettings.contestId;
  }

  String getContestJurisdiction() {
    return rawConfig.outputSettings.contestJurisdiction;
  }

  String getContestOffice() {
    return rawConfig.outputSettings.contestOffice;
  }

  String getContestDate() {
    return rawConfig.outputSettings.contestDate;
  }

  boolean isTabulateByPrecinctEnabled() {
    return rawConfig.outputSettings.tabulateByPrecinct;
  }

  boolean isGenerateCdfJsonEnabled() {
    return rawConfig.outputSettings.generateCdfJson;
  }

  // Converts a String to an Integer and also allows for an additional option as valid input
  private Integer stringToIntWithOption(String rawInput, String optionFlag, Integer optionResult) {
    Integer intValue;
    if (isNullOrBlank(rawInput)) {
      intValue = null;
    } else if (rawInput.equalsIgnoreCase(optionFlag)) {
      intValue = optionResult;
    } else {
      try {
        intValue = Integer.parseInt(rawInput);
      } catch (NumberFormatException e) {
        intValue = null;
      }
    }
    return intValue;
  }

  enum Provider {
    CDF("CDF"),
    DOMINION("DOMINION"),
    ESS("ES&S"),
    HART("Hart"),
    PROVIDER_UNKNOWN("Provider unknown");

    private final String label;

    Provider(String label) {
      this.label = label;
    }

    static Provider getByLabel(String labelLookup) {
      return Arrays.stream(Provider.values())
          .filter(v -> v.label.equals(labelLookup))
          .findAny()
          .orElse(null);
    }

    @Override
    public String toString() {
      return label;
    }
  }

  static class UnrecognizedProviderException extends Exception {

  }

  private String getMaxRankingsAllowedRaw() {
    return rawConfig.rules.maxRankingsAllowed;
  }

  Integer getMaxRankingsAllowed() {
    return stringToIntWithOption(
        getMaxRankingsAllowedRaw(),
        MAX_RANKINGS_ALLOWED_NUM_CANDIDATES_OPTION,
        getNumDeclaredCandidates());
  }

  boolean isBatchEliminationEnabled() {
    return rawConfig.rules.batchElimination;
  }

  int getNumDeclaredCandidates() {
    int num = getCandidateCodeList().size();
    if (!isNullOrBlank(getUndeclaredWriteInLabel())
        && getCandidateCodeList().contains(getUndeclaredWriteInLabel())) {
      num--;
    }
    return num;
  }

  int getNumCandidates() {
    return getCandidateCodeList().size();
  }

  boolean candidateIsExcluded(String candidate) {
    return excludedCandidates.contains(candidate);
  }

  OvervoteRule getOvervoteRule() {
    OvervoteRule rule = OvervoteRule.getByLabel(rawConfig.rules.overvoteRule);
    return rule == null ? OvervoteRule.RULE_UNKNOWN : rule;
  }

  private String getMinimumVoteThresholdRaw() {
    return rawConfig.rules.minimumVoteThreshold;
  }

  BigDecimal getMinimumVoteThreshold() {
    return new BigDecimal(getMinimumVoteThresholdRaw());
  }

  private String getMaxSkippedRanksAllowedRaw() {
    return rawConfig.rules.maxSkippedRanksAllowed;
  }

  Integer getMaxSkippedRanksAllowed() {
    return stringToIntWithOption(
        getMaxSkippedRanksAllowedRaw(),
        MAX_SKIPPED_RANKS_ALLOWED_UNLIMITED_OPTION,
        Integer.MAX_VALUE);
  }

  String getUndeclaredWriteInLabel() {
    return rawConfig.rules.undeclaredWriteInLabel;
  }

  String getOvervoteLabel() {
    return rawConfig.rules.overvoteLabel;
  }

  String getUndervoteLabel() {
    return rawConfig.rules.undervoteLabel;
  }

  TieBreakMode getTiebreakMode() {
    TieBreakMode mode = TieBreakMode.getByLabel(rawConfig.rules.tiebreakMode);
    return mode == null ? TieBreakMode.MODE_UNKNOWN : mode;
  }

  private String getRandomSeedRaw() {
    return rawConfig.rules.randomSeed;
  }

  Long getRandomSeed() {
    return Long.parseLong(getRandomSeedRaw());
  }

  boolean needsRandomSeed() {
    return getTiebreakMode() == TieBreakMode.RANDOM
        || getTiebreakMode() == TieBreakMode.PREVIOUS_ROUND_COUNTS_THEN_RANDOM
        || getTiebreakMode() == TieBreakMode.GENERATE_PERMUTATION;
  }

  boolean isTreatBlankAsUndeclaredWriteInEnabled() {
    return rawConfig.rules.treatBlankAsUndeclaredWriteIn;
  }

  boolean isExhaustOnDuplicateCandidateEnabled() {
    return rawConfig.rules.exhaustOnDuplicateCandidate;
  }

  Set<String> getCandidateCodeList() {
    return candidateCodeToNameMap.keySet();
  }

  String getNameForCandidateCode(String code) {
    return candidateCodeToNameMap.get(code);
  }

  ArrayList<String> getCandidatePermutation() {
    return candidatePermutation;
  }

  void setCandidateExclusionStatus(String candidateCode, boolean excluded) {
    if (excluded) {
      excludedCandidates.add(candidateCode);
    } else {
      excludedCandidates.remove(candidateCode);
    }
  }

  // perform pre-processing on candidates:
  // 1) if there are any CDF input sources extract candidates names from them
  // 2) build map of candidate ID to candidate name
  // 3) generate tie-break ordering if needed
  private void processCandidateData() {
    candidateCodeToNameMap = new HashMap<>();

    for (RawContestConfig.CvrSource source : rawConfig.cvrFileSources) {
      // for any CDF sources extract candidate names
      if (isCdf(source)) {
        String cvrPath = resolveConfigPath(source.getFilePath());
        CommonDataFormatReader reader = new CommonDataFormatReader(cvrPath, this);
        candidateCodeToNameMap = reader.getCandidates();
        candidatePermutation.addAll(candidateCodeToNameMap.keySet());
      }
    }

    if (rawConfig.candidates != null) {
      for (RawContestConfig.Candidate candidate : rawConfig.candidates) {
        String code = candidate.getCode();
        String name = candidate.getName();
        if (isNullOrBlank(code)) {
          code = name;
        }

        // duplicate names or codes get caught in validation
        candidateCodeToNameMap.put(code, name);
        candidatePermutation.add(code);
        if (candidate.isExcluded()) {
          excludedCandidates.add(code);
        }
      }
    }

    String uwiLabel = getUndeclaredWriteInLabel();
    if (!isNullOrBlank(uwiLabel)) {
      candidateCodeToNameMap.put(uwiLabel, UNDECLARED_WRITE_INS);
    }
  }
}
