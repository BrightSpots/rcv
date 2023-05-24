/*
 * RCTab
 * Copyright (c) 2017-2022 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: Wrapper for RawContestConfig object.  This class adds logic for looking up rule enum
 * names, candidate names, tracking sequential winners, various GUI-related utilities, and
 * significant logic for validating Contest Config data in different tabulation scenarios.
 * Design: Wraps a RawContestConfig object (underlying data) and adds utility methods.
 * Conditions: Used during GUI sessions and during any tabulation.
 * Version history: see https://github.com/BrightSpots/rcv.
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
import java.util.stream.Stream;
import network.brightspots.rcv.RawContestConfig.Candidate;
import network.brightspots.rcv.RawContestConfig.CvrSource;
import network.brightspots.rcv.Tabulator.OvervoteRule;
import network.brightspots.rcv.Tabulator.TiebreakMode;
import network.brightspots.rcv.Tabulator.WinnerElectionMode;

class ContestConfig {

  // If any booleans are unspecified in config file, they should default to false no matter what
  static final String AUTOMATED_TEST_VERSION = "TEST";
  static final String SUGGESTED_OUTPUT_DIRECTORY = "output";
  static final boolean SUGGESTED_TABULATE_BY_PRECINCT = false;
  static final boolean SUGGESTED_GENERATE_CDF_JSON = false;
  static final boolean SUGGESTED_CANDIDATE_EXCLUDED = false;
  static final boolean SUGGESTED_MAX_RANKINGS_ALLOWED_MAXIMUM = true;
  static final boolean SUGGESTED_NON_INTEGER_WINNING_THRESHOLD = false;
  static final boolean SUGGESTED_HARE_QUOTA = false;
  static final boolean SUGGESTED_BATCH_ELIMINATION = false;
  static final boolean SUGGESTED_CONTINUE_UNTIL_TWO_CANDIDATES_REMAIN = false;
  static final boolean SUGGESTED_EXHAUST_ON_DUPLICATE_CANDIDATES = false;
  static final boolean SUGGESTED_TREAT_BLANK_AS_UNDECLARED_WRITE_IN = false;
  static final int SUGGESTED_CVR_FIRST_VOTE_COLUMN = 4;
  static final int SUGGESTED_CVR_FIRST_VOTE_ROW = 2;
  static final int SUGGESTED_CVR_ID_COLUMN = 1;
  static final int SUGGESTED_CVR_PRECINCT_COLUMN = 2;
  static final int SUGGESTED_DECIMAL_PLACES_FOR_VOTE_ARITHMETIC = 4;
  static final int SUGGESTED_MAX_SKIPPED_RANKS_ALLOWED = 1;
  static final boolean SUGGESTED_MAX_SKIPPED_RANKS_ALLOWED_UNLIMITED = false;
  static final String SUGGESTED_OVERVOTE_LABEL = "overvote";
  static final String SUGGESTED_UNDERVOTE_LABEL = "undervote";
  static final String MAX_SKIPPED_RANKS_ALLOWED_UNLIMITED_OPTION = "unlimited";
  static final String MAX_RANKINGS_ALLOWED_NUM_CANDIDATES_OPTION = "max";
  private static final int MIN_COLUMN_INDEX = 1;
  private static final int MAX_COLUMN_INDEX = 1000;
  private static final int MIN_ROW_INDEX = 1;
  private static final int MAX_ROW_INDEX = 100000;
  private static final int MIN_MAX_RANKINGS_ALLOWED = 1;
  private static final int MIN_MAX_SKIPPED_RANKS_ALLOWED = 0;
  private static final int MIN_NUMBER_OF_ROUNDS = 1;
  private static final int MIN_NUMBER_OF_WINNERS = 0;
  private static final int MIN_DECIMAL_PLACES_FOR_VOTE_ARITHMETIC = 1;
  private static final int MAX_DECIMAL_PLACES_FOR_VOTE_ARITHMETIC = 20;
  private static final int MIN_MINIMUM_VOTE_THRESHOLD = 0;
  private static final int MAX_MINIMUM_VOTE_THRESHOLD = 1000000;
  private static final int MIN_MULTI_SEAT_BOTTOMS_UP_PERCENTAGE_THRESHOLD = 1;
  private static final int MAX_MULTI_SEAT_BOTTOMS_UP_PERCENTAGE_THRESHOLD = 100;
  private static final long MIN_RANDOM_SEED = -140737488355328L;
  private static final long MAX_RANDOM_SEED = 140737488355327L;
  // Underlying rawConfig object data
  final RawContestConfig rawConfig;
  // This is used if we have a permutation-based tie-break mode
  private final ArrayList<String> candidatePermutation = new ArrayList<>();
  private final Set<String> excludedCandidates = new HashSet<>();
  // Path from which any relative paths should be resolved
  private final String sourceDirectory;
  // Used to track a sequential multi-seat race
  private final List<String> sequentialWinners = new LinkedList<>();
  // Candidate display names (no aliases or codes)
  private Set<String> candidateNames;
  // Mapping from any candidate alias to the candidate's display name
  private Map<String, String> candidateAliasesToNameMap;
  // Mapping from any candidate alias to the candidate's code
  private Map<String, String> candidateAliasesToCodeMap;
  // A list of any validation errors
  private Set<ValidationError> validationErrors = new HashSet<>();

  private ContestConfig(RawContestConfig rawConfig, String sourceDirectory) {
    this.rawConfig = rawConfig;
    this.sourceDirectory = sourceDirectory;
  }

  static ContestConfig loadContestConfig(RawContestConfig rawConfig, String sourceDirectory) {
    ContestConfig config = new ContestConfig(rawConfig, sourceDirectory);
    try {
      config.processCandidateData();
    } catch (Exception exception) {
      Logger.severe("Error processing candidate data:\n%s", exception);
      config = null;
    }
    return config;
  }

  // create rawContestConfig from file - can fail for IO issues or invalid json
  // returns: new ContestConfig object if checks pass otherwise null
  static ContestConfig loadContestConfig(String configPath, boolean silentMode) {
    ContestConfig config = null;
    if (configPath == null) {
      Logger.severe("No contest config path specified!");
    } else {
      RawContestConfig rawConfig = JsonParser.readFromFile(configPath, RawContestConfig.class);
      if (rawConfig == null) {
        Logger.severe("Failed to load contest config: %s", configPath);
      } else {
        if (!silentMode) {
          Logger.info("Successfully loaded contest config: %s", configPath);
        }
        // parent folder is used as the default source folder
        // if there is no parent folder use current working directory
        String parentFolder = new File(configPath).getParent();
        if (parentFolder == null) {
          parentFolder = System.getProperty("user.dir");
        }
        config = loadContestConfig(rawConfig, parentFolder);
      }
    }
    return config;
  }

  static ContestConfig loadContestConfig(String configPath) {
    return loadContestConfig(configPath, false);
  }

  /* Performs basic validation on CVR sources and returns a set of validation errors. **/
  static Set<ValidationError> performBasicCvrSourceValidation(CvrSource source) {
    Set<ValidationError> validationErrors = new HashSet<>();
    if (isNullOrBlank(source.getFilePath())) {
      validationErrors.add(ValidationError.CVR_FILE_PATH_MISSING);
      Logger.severe("filePath is required for each cast vote record file!");
    } else {
      if (!isNullOrBlank(source.getOvervoteLabel())
          && stringAlreadyInUseElsewhereInSource(
          source.getOvervoteLabel(), source, "overvoteLabel")) {
        validationErrors.add(ValidationError.CVR_OVERVOTE_LABEL_INVALID);
      }
      if (!isNullOrBlank(source.getUndervoteLabel())
          && stringAlreadyInUseElsewhereInSource(
          source.getUndervoteLabel(), source, "undervoteLabel")) {
        validationErrors.add(ValidationError.CVR_UNDERVOTE_LABEL_INVALID);
      }
      if (!isNullOrBlank(source.getUndeclaredWriteInLabel())
          && stringAlreadyInUseElsewhereInSource(
          source.getUndeclaredWriteInLabel(), source, "undeclaredWriteInLabel")) {
        validationErrors.add(ValidationError.CVR_UWI_LABEL_INVALID);
      }

      Provider provider = getProvider(source);
      if (provider == Provider.PROVIDER_UNKNOWN) {
        validationErrors.add(ValidationError.CVR_PROVIDER_INVALID);
        Logger.severe("Invalid provider for source: %s", source.getFilePath());
      } else if (provider == Provider.ESS || provider == Provider.CSV) {
        // Both ESS and CSV require firstVoteColumnIndex and firstVoteRowIndex
        if (fieldOutOfRangeOrNotInteger(
            source.getFirstVoteColumnIndex(),
            "firstVoteColumnIndex",
            MIN_COLUMN_INDEX,
            MAX_COLUMN_INDEX,
            true,
            source.getFilePath())) {
          validationErrors.add(ValidationError.CVR_FIRST_VOTE_COLUMN_INVALID);
        }

        if (fieldOutOfRangeOrNotInteger(
            source.getFirstVoteRowIndex(),
            "firstVoteRowIndex",
            MIN_ROW_INDEX,
            MAX_ROW_INDEX,
            true,
            source.getFilePath())) {
          validationErrors.add(ValidationError.CVR_FIRST_VOTE_ROW_INVALID);
        }

        if (provider == Provider.ESS) {
          // ESS requires idColumnIndex and precinctColumnIndex
          if (fieldOutOfRangeOrNotInteger(
              source.getIdColumnIndex(),
              "idColumnIndex",
              MIN_COLUMN_INDEX,
              MAX_COLUMN_INDEX,
              false,
              source.getFilePath())) {
            validationErrors.add(ValidationError.CVR_ID_COLUMN_INVALID);
          }

          if (fieldOutOfRangeOrNotInteger(
              source.getPrecinctColumnIndex(),
              "precinctColumnIndex",
              MIN_COLUMN_INDEX,
              MAX_COLUMN_INDEX,
              false,
              source.getFilePath())) {
            validationErrors.add(ValidationError.CVR_PRECINCT_COLUMN_INVALID);
          }
        } else {
          // CSV does not allow idColumnIndex or precinctColumnIndex
          if (fieldIsDefinedButShouldNotBeForProvider(
              source.getIdColumnIndex(), "idColumnIndex", provider, source.getFilePath())) {
            validationErrors.add(ValidationError.CVR_ID_COLUMN_UNEXPECTEDLY_DEFINED);
          }

          if (fieldIsDefinedButShouldNotBeForProvider(
              source.getPrecinctColumnIndex(),
              "precinctColumnIndex",
              provider,
              source.getFilePath())) {
            validationErrors.add(ValidationError.CVR_PRECINCT_COLUMN_UNEXPECTEDLY_DEFINED);
          }
        }

        // See the config file documentation for an explanation of this regex
        if (!isNullOrBlank(source.getOvervoteDelimiter())
            && source.getOvervoteDelimiter().matches(".*\\\\.*|[a-zA-Z0-9.',\\-\"\\s]+")) {
          validationErrors.add(ValidationError.CVR_OVERVOTE_DELIMITER_INVALID);
          Logger.severe("overvoteDelimiter is invalid.");
        }

        if (!isNullOrBlank(source.getOvervoteDelimiter())
            && !isNullOrBlank(source.getOvervoteLabel())) {
          validationErrors.add(ValidationError.CVR_OVERVOTE_DELIMITER_AND_LABEL_BOTH_SUPPLIED);
          Logger.severe("overvoteDelimiter and overvoteLabel can't both be supplied.");
        }
      } else {
        if (provider == Provider.CDF) {
          if (!source.getFilePath().toLowerCase().endsWith(".xml")
              && !source.getFilePath().toLowerCase().endsWith(".json")) {
            Logger.severe(
                "CDF source files must be .json or .xml! Unexpected file extension for: %s",
                source.getFilePath());
            validationErrors.add(ValidationError.CVR_CDF_FILE_PATH_INVALID);
          }
        } else {
          if (fieldIsDefinedButShouldNotBeForProvider(
              source.getOvervoteLabel(), "overvoteLabel", provider, source.getFilePath())) {
            validationErrors.add(ValidationError.CVR_OVERVOTE_UNEXPECTEDLY_DEFINED);
          }
        }

        if (fieldIsDefinedButShouldNotBeForProvider(
            source.getFirstVoteColumnIndex(),
            "firstVoteColumnIndex",
            provider,
            source.getFilePath())) {
          validationErrors.add(ValidationError.CVR_FIRST_VOTE_UNEXPECTEDLY_DEFINED);
        }

        if (fieldIsDefinedButShouldNotBeForProvider(
            source.getFirstVoteRowIndex(), "firstVoteRowIndex", provider, source.getFilePath())) {
          validationErrors.add(ValidationError.CVR_FIRST_VOTE_ROW_UNEXPECTEDLY_DEFINED);
        }

        if (fieldIsDefinedButShouldNotBeForProvider(
            source.getIdColumnIndex(), "idColumnIndex", provider, source.getFilePath())) {
          validationErrors.add(ValidationError.CVR_ID_COLUMN_UNEXPECTEDLY_DEFINED);
        }

        if (fieldIsDefinedButShouldNotBeForProvider(
            source.getPrecinctColumnIndex(),
            "precinctColumnIndex",
            provider,
            source.getFilePath())) {
          validationErrors.add(ValidationError.CVR_PRECINCT_COLUMN_UNEXPECTEDLY_DEFINED);
        }

        if (fieldIsDefinedButShouldNotBeForProvider(
            source.getOvervoteDelimiter(), "overvoteDelimiter", provider, source.getFilePath())) {
          validationErrors.add(ValidationError.CVR_OVERVOTE_DELIMITER_UNEXPECTEDLY_DEFINED);
        }

        if (fieldIsDefinedButShouldNotBeForProvider(
            source.getUndervoteLabel(), "undervoteLabel", provider, source.getFilePath())) {
          validationErrors.add(ValidationError.CVR_UNDERVOTE_LABEL_UNEXPECTEDLY_DEFINED);
        }

        if (source.isTreatBlankAsUndeclaredWriteIn()) {
          logErrorWithLocation(
              String.format(
                  "treatBlankAsUndeclaredWriteIn should not be true for CVR source with "
                      + "provider \"%s\"",
                  provider),
              source.getFilePath());
          validationErrors.add(ValidationError.CVR_TREAT_BLANK_AS_UWI_UNEXPECTEDLY_TRUE);
        }
      }

      boolean providerRequiresContestId =
          provider == Provider.DOMINION
              || provider == Provider.HART
              || provider == Provider.CLEAR_BALLOT
              || provider == Provider.CDF;

      if (isNullOrBlank(source.getContestId()) && providerRequiresContestId) {
        validationErrors.add(ValidationError.CVR_CONTEST_ID_INVALID);
        Logger.severe(
            String.format(
                "contestId must be defined for CVR source with provider \"%s\"!",
                getProvider(source)));
      } else if (!providerRequiresContestId
          && fieldIsDefinedButShouldNotBeForProvider(
          source.getContestId(), "contestId", provider, source.getFilePath())) {
        // Helper will log error
        validationErrors.add(ValidationError.CVR_CONTEST_ID_UNEXPECTEDLY_DEFINED);
      }
    }
    return validationErrors;
  }

  // function: stringMatchesAnotherFieldValue(
  // purpose: Checks to make sure string value of one field doesn't match value of another field
  // param: string to check
  // param: field name of provided string
  // param: otherFieldValue string value of the other field
  // param: otherField name of the other field
  private static boolean stringMatchesAnotherFieldValue(
      String string, String field, String otherFieldValue, String otherField) {
    boolean match = false;
    if (!field.equals(otherField)) {
      if (!isNullOrBlank(otherFieldValue) && otherFieldValue.equalsIgnoreCase(string)) {
        match = true;
        Logger.severe(
            "\"%s\" can't be used as %s if it's also being used as %s!", string, field, otherField);
      }
    }
    return match;
  }

  private static void logErrorWithLocation(String message, String inputLocation) {
    message += inputLocation == null ? "!" : " for file source: " + inputLocation;
    Logger.severe(message);
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
      } catch (NumberFormatException exception) {
        if (!isRequired) {
          message += " if supplied";
        }
        stringValid = false;
        logErrorWithLocation(message, inputLocation);
      }
    }
    return !stringValid;
  }

  private static boolean fieldIsDefinedButShouldNotBeForProvider(
      String value, String fieldName, Provider provider, String inputLocation) {
    boolean stringValid = true;
    if (!isNullOrBlank(value)) {
      stringValid = false;
      logErrorWithLocation(
          String.format(
              "%s should not be defined for CVR source with provider \"%s\"", fieldName, provider),
          inputLocation);
    }
    return !stringValid;
  }

  static Provider getProvider(CvrSource cvrSource) {
    return Provider.getByInternalLabel(cvrSource.getProvider());
  }

  /* Performs basic validation on candidate list and returns a set of validation errors. **/
  static Set<ValidationError> performBasicCandidateValidation(Candidate candidate) {
    Set<ValidationError> validationErrors = new HashSet<>();
    if (isNullOrBlank(candidate.getName())) {
      validationErrors.add(ValidationError.CANDIDATE_NAME_MISSING);
      Logger.severe("A name is required for each candidate!");
    }
    return validationErrors;
  }

  private static boolean stringConflictsWithReservedString(String string, String field) {
    boolean reserved = false;
    for (String reservedString : TallyTransfers.RESERVED_STRINGS) {
      if (string.equalsIgnoreCase(reservedString)) {
        reserved = true;
        Logger.severe("\"%s\" is a reserved term and can't be used for %s!", string, field);
        break;
      }
    }
    return reserved;
  }

  // Checks to make sure string isn't reserved or used by other fields
  private static boolean stringAlreadyInUseElsewhereInSource(
      String string, CvrSource source, String field) {
    boolean inUse = stringConflictsWithReservedString(string, field);
    if (!inUse) {
      inUse =
          stringMatchesAnotherFieldValue(string, field, source.getOvervoteLabel(), "overvoteLabel")
              || stringMatchesAnotherFieldValue(
              string, field, source.getUndervoteLabel(), "undervoteLabel")
              || stringMatchesAnotherFieldValue(
              string, field, source.getUndeclaredWriteInLabel(), "undeclaredWriteInLabel");
    }
    return inUse;
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

  /* Performs a full validation of the ContestConfig. **/
  Set<ValidationError> validate() {
    Logger.info("Validating contest config...");
    validationErrors = new HashSet<>();
    validateTabulatorVersion();
    validateOutputSettings();
    validateCvrFileSources();
    validateCandidates();
    validateRules();
    if (validationErrors.isEmpty()) {
      Logger.info("Contest config validation successful.");
    } else {
      Logger.severe(
          "Contest config validation failed! Please modify the contest config file and try again.\n"
              + "See config_file_documentation.txt for more details.");
    }
    return validationErrors;
  }

  // version validation and migration logic goes here
  // e.g. unsupported versions would fail or be migrated
  // in this release we support only the current app version
  private void validateTabulatorVersion() {
    if (isNullOrBlank(getTabulatorVersion())) {
      validationErrors.add(ValidationError.TABULATOR_VERSION_MISSING);
      Logger.severe("tabulatorVersion is required!");
    } else {
      // ignore this check for test data, but otherwise require version to match current app version
      if (!getTabulatorVersion().equals(AUTOMATED_TEST_VERSION)
          && !getTabulatorVersion().equals(Main.APP_VERSION)) {
        validationErrors.add(ValidationError.TABULATOR_VERSION_NOT_SUPPORTED);
        Logger.severe("tabulatorVersion %s not supported!", getTabulatorVersion());
      }
    }
    if (validationErrors.contains(ValidationError.TABULATOR_VERSION_MISSING)
        || validationErrors.contains(ValidationError.TABULATOR_VERSION_NOT_SUPPORTED)) {
      Logger.severe("tabulatorVersion must be set to %s!", Main.APP_VERSION);
    }
  }

  private void validateOutputSettings() {
    if (isNullOrBlank(getContestName())) {
      validationErrors.add(ValidationError.OUTPUT_CONTEST_NAME_MISSING);
      Logger.severe("contestName is required!");
    }
  }

  // checks for conflicts between a candidate name and other name/codes or other reserved strings
  // param: candidateString is a candidate name or code
  // param: field is either "name" or "code"
  // param: candidateStringsSeen is a running set of names/codes we've already encountered
  private boolean candidateStringAlreadyInUseElsewhere(
      String candidateString, String field, Set<String> candidateStringsSeen) {
    boolean inUse = false;
    if (candidateStringsSeen.contains(candidateString)) {
      inUse = true;
      Logger.severe("Duplicate candidate %ss are not allowed: %s", field, candidateString);
    } else {
      for (CvrSource source : getRawConfig().cvrFileSources) {
        inUse =
            stringAlreadyInUseElsewhereInSource(candidateString, source, "a candidate " + field);
        if (inUse) {
          break;
        }
      }
    }
    return inUse;
  }

  private void validateCvrFileSources() {
    if (rawConfig.cvrFileSources == null || rawConfig.cvrFileSources.isEmpty()) {
      validationErrors.add(ValidationError.CVR_NO_FILES_SPECIFIED);
      Logger.severe("Contest config must contain at least 1 cast vote record file!");
    } else {
      HashSet<String> cvrFilePathSet = new HashSet<>();
      for (CvrSource source : rawConfig.cvrFileSources) {
        validationErrors.addAll(performBasicCvrSourceValidation(source));

        String cvrPath =
            isNullOrBlank(source.getFilePath()) ? null : resolveConfigPath(source.getFilePath());

        // Look for duplicate paths
        if (cvrFilePathSet.contains(cvrPath)) {
          validationErrors.add(ValidationError.CVR_DUPLICATE_FILE_PATHS);
          Logger.severe("Duplicate cast vote record filePaths are not allowed: %s", cvrPath);
        } else {
          cvrFilePathSet.add(cvrPath);
        }

        // Ensure file exists
        if (cvrPath != null && !new File(cvrPath).exists()) {
          validationErrors.add(ValidationError.CVR_FILE_PATH_INVALID);
          Logger.severe("Cast vote record file not found: %s", cvrPath);
        }

        if (!isNullOrBlank(source.getOvervoteLabel())
            && getOvervoteRule() != Tabulator.OvervoteRule.EXHAUST_IMMEDIATELY
            && getOvervoteRule() != Tabulator.OvervoteRule.ALWAYS_SKIP_TO_NEXT_RANK) {
          validationErrors.add(ValidationError.CVR_OVERVOTE_LABEL_OVERVOTE_RULE_MISMATCH);
          Logger.severe(
              "When overvoteLabel is supplied, overvoteRule must be either \"%s\" or \"%s\"!",
              Tabulator.OVERVOTE_RULE_ALWAYS_SKIP_TEXT,
              Tabulator.OVERVOTE_RULE_EXHAUST_IMMEDIATELY_TEXT);
        }

        if (getProvider(source) == Provider.CDF) {
          // Perform CDF checks
          if (isTabulateByPrecinctEnabled()) {
            validationErrors.add(ValidationError.CVR_CDF_TABULATE_BY_PRECINCT_DISAGREEMENT);
            Logger.severe("tabulateByPrecinct may not be used with CDF files.");
          }
        } else if (getProvider(source) == Provider.ESS) {
          // Perform ES&S checks
          if (isNullOrBlank(source.getPrecinctColumnIndex()) && isTabulateByPrecinctEnabled()) {
            validationErrors.add(ValidationError.CVR_TABULATE_BY_PRECINCT_REQUIRES_PRECINCT_COLUMN);
            Logger.severe(
                "precinctColumnIndex is required when tabulateByPrecinct is enabled: %s", cvrPath);
          }
          if (isNullOrBlank(source.getOvervoteDelimiter())
              && getOvervoteRule() == OvervoteRule.EXHAUST_IF_MULTIPLE_CONTINUING) {
            validationErrors.add(ValidationError.CVR_OVERVOTE_DELIMITER_MISSING);
            Logger.severe(
                "overvoteDelimiter is required for an ES&S CVR source when overvoteRule "
                    + "is set to \"%s\".",
                Tabulator.OVERVOTE_RULE_EXHAUST_IF_MULTIPLE_TEXT);
          }
        }
      }
    }
  }

  private void validateCandidates() {
    Set<String> candidateNameSet = new HashSet<>();

    for (Candidate candidate : rawConfig.candidates) {
      validationErrors.addAll(performBasicCandidateValidation(candidate));

      // Ensure the candidate name and all aliases are unique, both within each candidate and
      // across candidates.
      candidate.createStreamOfNameAndAllAliases().forEach(nameOrAlias -> {
        if (candidateStringAlreadyInUseElsewhere(nameOrAlias, "name", candidateNameSet)) {
          validationErrors.add(ValidationError.CANDIDATE_DUPLICATE_NAME);
        } else {
          candidateNameSet.add(nameOrAlias);
        }
      });
    }

    if (getNumDeclaredCandidates() < 1) {
      validationErrors.add(ValidationError.CANDIDATE_NO_CANDIDATES_SPECIFIED);
      Logger.severe("Contest config must contain at least 1 declared candidate!");
    } else if (getNumDeclaredCandidates() == excludedCandidates.size()) {
      validationErrors.add(ValidationError.CANDIDATE_ALL_EXCLUDED);
      Logger.severe("Contest config must contain at least 1 non-excluded candidate!");
    }
  }

  private void validateRules() {
    if (getTiebreakMode() == TiebreakMode.MODE_UNKNOWN) {
      validationErrors.add(ValidationError.RULES_TIEBREAK_MODE_INVALID);
      Logger.severe("Invalid tiebreakMode!");
    }

    if (needsRandomSeed() && isNullOrBlank(getRandomSeedRaw())) {
      validationErrors.add(ValidationError.RULES_RANDOM_SEED_MISSING);
      Logger.severe("When tiebreakMode involves a random element, randomSeed must be supplied!");
    }

    if (fieldOutOfRangeOrNotInteger(
        getRandomSeedRaw(), "randomSeed", MIN_RANDOM_SEED, MAX_RANDOM_SEED, false)) {
      validationErrors.add(ValidationError.RULES_RANDOM_SEED_INVALID);
    }

    if (getOvervoteRule() == OvervoteRule.RULE_UNKNOWN) {
      validationErrors.add(ValidationError.RULES_OVERVOTE_RULE_INVALID);
      Logger.severe("Invalid overvoteRule!");
    }

    if (getWinnerElectionMode() == WinnerElectionMode.MODE_UNKNOWN) {
      validationErrors.add(ValidationError.RULES_WINNER_ELECTION_MODE_INVALID);
      Logger.severe("Invalid winnerElectionMode!");
    }

    if (getMaxRankingsAllowed() == null
        || (getNumDeclaredCandidates() >= 1
        && getMaxRankingsAllowed() < MIN_MAX_RANKINGS_ALLOWED)) {
      validationErrors.add(ValidationError.RULES_MAX_RANKINGS_ALLOWED_INVALID);
      Logger.severe(
          "maxRankingsAllowed must either be \"%s\" or an integer from %d to %d!",
          MAX_RANKINGS_ALLOWED_NUM_CANDIDATES_OPTION, MIN_MAX_RANKINGS_ALLOWED, Integer.MAX_VALUE);
    }

    if (getMaxSkippedRanksAllowed() == null
        || getMaxSkippedRanksAllowed() < MIN_MAX_SKIPPED_RANKS_ALLOWED) {
      validationErrors.add(ValidationError.RULES_MAX_SKIPPED_RANKS_ALLOWED_INVALID);
      Logger.severe(
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
      validationErrors.add(ValidationError.RULES_NUMBER_OF_WINNERS_INVALID);
    }

    if (fieldOutOfRangeOrNotInteger(
        getDecimalPlacesForVoteArithmeticRaw(),
        "decimalPlacesForVoteArithmetic",
        MIN_DECIMAL_PLACES_FOR_VOTE_ARITHMETIC,
        MAX_DECIMAL_PLACES_FOR_VOTE_ARITHMETIC,
        true)) {
      validationErrors.add(ValidationError.RULES_MIN_DECIMAL_PLACES_FOR_VOTE_ARITHMETIC_INVALID);
    }

    if (fieldOutOfRangeOrNotInteger(
        getMinimumVoteThresholdRaw(),
        "minimumVoteThreshold",
        MIN_MINIMUM_VOTE_THRESHOLD,
        MAX_MINIMUM_VOTE_THRESHOLD,
        false)) {
      validationErrors.add(ValidationError.RULES_MIN_VOTE_THRESHOLD_INVALID);
    }

    if (fieldOutOfRangeOrNotInteger(
        getMultiSeatBottomsUpPercentageThresholdRaw(),
        "multiSeatBottomsUpPercentageThreshold",
        MIN_MULTI_SEAT_BOTTOMS_UP_PERCENTAGE_THRESHOLD,
        MAX_MULTI_SEAT_BOTTOMS_UP_PERCENTAGE_THRESHOLD,
        false)) {
      validationErrors.add(
          ValidationError.RULES_MULTI_SEAT_BOTTOMS_UP_PERCENTAGE_THRESHOLD_INVALID);
    }

    if (fieldOutOfRangeOrNotInteger(
        getStopTabulationEarlyAfterRoundRaw(),
        "stopEarlyAfterRound",
        MIN_NUMBER_OF_ROUNDS,
        Integer.MAX_VALUE,
        false)) {
      validationErrors.add(ValidationError.RULES_STOP_TABULATION_EARLY_AFTER_ROUND_INVALID);
    }

    WinnerElectionMode winnerMode = getWinnerElectionMode();
    if (Utils.isInt(getNumberOfWinnersRaw())) {
      if (getNumberOfWinners() > 0) {
        if (isMultiSeatBottomsUpWithThresholdEnabled()) {
          validationErrors.add(
              ValidationError.RULES_NUMBER_OF_WINNERS_INVALID_FOR_WINNER_ELECTION_MODE);
          Logger.severe(
              "numberOfWinners must be zero if winnerElectionMode is \"%s\"!", winnerMode);
        }

        if (getNumberOfWinners() > 1) {
          if (winnerMode != WinnerElectionMode.MULTI_SEAT_SEQUENTIAL_WINNER_TAKES_ALL) {
            if (isContinueUntilTwoCandidatesRemainEnabled()) {
              validationErrors.add(
                  ValidationError.RULES_CONTINUE_UNTIL_TWO_CANDIDATES_REMAIN_TRUE_FOR_MULTI_SEAT);
              Logger.severe(
                  "continueUntilTwoCandidatesRemain can't be true in a multi-seat contest unless "
                      + "the winner election mode is multi-pass IRV!"
              );
            }

            if (isBatchEliminationEnabled()) {
              validationErrors.add(ValidationError.RULES_BATCH_ELIMINATION_TRUE_FOR_MULTI_SEAT);
              Logger.severe(
                  "batchElimination can't be true in a multi-seat contest unless the "
                      + "winner election mode is multi-pass IRV!"
              );
            }
          }
        } else { // numberOfWinners == 1
          if (!isSingleWinnerEnabled()) {
            validationErrors.add(
                ValidationError.RULES_WINNER_ELECTION_MODE_INVALID_FOR_SINGLE_SEAT);
            Logger.severe(
                "winnerElectionMode can't be \"%s\" in a single-seat contest!", winnerMode);
          }
        }
      } else { // numberOfWinners == 0
        if (!isMultiSeatBottomsUpWithThresholdEnabled()) {
          validationErrors.add(ValidationError.RULES_ZERO_WINNERS_INVALID_WINNER_ELECTION_MODE);
          Logger.severe(
              "If numberOfWinners is zero, winnerElectionMode must be \"%s\"!",
              WinnerElectionMode.MULTI_SEAT_BOTTOMS_UP_USING_PERCENTAGE_THRESHOLD);
        }

        if (getMultiSeatBottomsUpPercentageThreshold() == null) {
          validationErrors.add(ValidationError.RULES_PERCENTAGE_THRESHOLD_MISSING);
          Logger.severe("If numberOfWinners is zero, multiSeatBottomsUpPercentageThreshold "
              + "must be specified!");
        }
      }
    }

    if (isMultiSeatBottomsUpWithThresholdEnabled() && isBatchEliminationEnabled()) {
      validationErrors.add(
          ValidationError.RULES_BOTTOMS_UP_THRESHOLD_BATCH_ELIMINATION_DISAGREEMENT);
      Logger.severe(
          "batchElimination can't be true when winnerElectionMode is \"%s\"!", winnerMode);
    }

    // nonIntegerWinningThreshold and hareQuota are only allowed for multi-seat elections
    if (!isMultiSeatAllowOnlyOneWinnerPerRoundEnabled()
        && !isMultiSeatAllowMultipleWinnersPerRoundEnabled()) {
      if (isNonIntegerWinningThresholdEnabled()) {
        validationErrors.add(
            ValidationError.RULES_NON_INTEGER_WINNING_THRESHOLD_WINNER_ELECTION_MODE_DISAGREEMENT);
        Logger.severe(
            "nonIntegerWinningThreshold can't be true when winnerElectionMode is \"%s\"!",
            winnerMode);
      }
      if (isHareQuotaEnabled()) {
        validationErrors.add(ValidationError.RULES_HARE_QUOTA_WINNER_ELECTION_MODE_DISAGREEMENT);
        Logger.severe("hareQuota can't be true when winnerElectionMode is \"%s\"!", winnerMode);
      }
    }

    if (isNonIntegerWinningThresholdEnabled() && isHareQuotaEnabled()) {
      validationErrors.add(
          ValidationError.RULES_NON_INTEGER_WINNING_THRESHOLD_HARE_QUOTA_DISAGREEMENT);
      Logger.severe(
          "nonIntegerWinningThreshold and hareQuota can't both be true at the same time!");
    }
  }

  private String getNumberOfWinnersRaw() {
    return rawConfig.rules.numberOfWinners;
  }

  private String getStopTabulationEarlyAfterRoundRaw() {
    return rawConfig.rules.stopTabulationEarlyAfterRound;
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
    return WinnerElectionMode.getByInternalLabel(rawConfig.rules.winnerElectionMode);
  }

  boolean isSingleWinnerEnabled() {
    return getWinnerElectionMode() == WinnerElectionMode.STANDARD_SINGLE_WINNER;
  }

  boolean isMultiSeatAllowOnlyOneWinnerPerRoundEnabled() {
    return getWinnerElectionMode() == WinnerElectionMode.MULTI_SEAT_ALLOW_ONLY_ONE_WINNER_PER_ROUND;
  }

  boolean isMultiSeatAllowMultipleWinnersPerRoundEnabled() {
    return getWinnerElectionMode()
        == WinnerElectionMode.MULTI_SEAT_ALLOW_MULTIPLE_WINNERS_PER_ROUND;
  }

  boolean isMultiSeatBottomsUpUntilNWinnersEnabled() {
    return getWinnerElectionMode() == WinnerElectionMode.MULTI_SEAT_BOTTOMS_UP_UNTIL_N_WINNERS;
  }

  boolean isMultiSeatBottomsUpWithThresholdEnabled() {
    return getWinnerElectionMode()
        == WinnerElectionMode.MULTI_SEAT_BOTTOMS_UP_USING_PERCENTAGE_THRESHOLD;
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
      } catch (NumberFormatException exception) {
        intValue = null;
      }
    }
    return intValue;
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

  boolean isContinueUntilTwoCandidatesRemainEnabled() {
    return rawConfig.rules.continueUntilTwoCandidatesRemain;
  }

  Integer getStopTabulationEarlyAfterRound() {
    return isNullOrBlank(getStopTabulationEarlyAfterRoundRaw())
            ? Integer.MAX_VALUE
            : Integer.parseInt(getStopTabulationEarlyAfterRoundRaw());
  }

  int getNumDeclaredCandidates() {
    int size = getCandidateNames().size();
    if (undeclaredWriteInsEnabled()) {
      // we subtract one for UNDECLARED_WRITE_IN_OUTPUT_LABEL;
      size = size - 1;
    }
    return size;
  }

  int getNumCandidates() {
    return getCandidateNames().size() - excludedCandidates.size();
  }

  boolean candidateIsExcluded(String candidate) {
    return excludedCandidates.contains(candidate);
  }

  OvervoteRule getOvervoteRule() {
    return OvervoteRule.getByInternalLabel(rawConfig.rules.overvoteRule);
  }

  private String getMinimumVoteThresholdRaw() {
    return rawConfig.rules.minimumVoteThreshold;
  }

  BigDecimal getMinimumVoteThreshold() {
    return isNullOrBlank(getMinimumVoteThresholdRaw())
        ? BigDecimal.ZERO
        : new BigDecimal(getMinimumVoteThresholdRaw());
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

  TiebreakMode getTiebreakMode() {
    return TiebreakMode.getByInternalLabel(rawConfig.rules.tiebreakMode);
  }

  private String getRandomSeedRaw() {
    return rawConfig.rules.randomSeed;
  }

  Long getRandomSeed() {
    return Long.parseLong(getRandomSeedRaw());
  }

  boolean needsRandomSeed() {
    return getTiebreakMode() == TiebreakMode.RANDOM
        || getTiebreakMode() == TiebreakMode.PREVIOUS_ROUND_COUNTS_THEN_RANDOM
        || getTiebreakMode() == TiebreakMode.GENERATE_PERMUTATION;
  }

  boolean isExhaustOnDuplicateCandidateEnabled() {
    return rawConfig.rules.exhaustOnDuplicateCandidate;
  }

  Set<String> getCandidateNames() {
    if (candidateNames == null) {
      candidateNames = new HashSet<>();
    }
    return candidateNames;
  }

  String getNameForCandidate(String nameOrAlias) {
    return candidateAliasesToNameMap.get(nameOrAlias);
  }

  String getCodeForCandidate(String nameOrAlias) {
    return candidateAliasesToCodeMap.get(nameOrAlias);
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
  // 1) build map of candidate aliases to candidate name
  // 2) generate tie-break ordering if needed
  // 3) add uwi candidate if needed
  private void processCandidateData() {
    candidateAliasesToNameMap = new HashMap<>();
    candidateAliasesToCodeMap = new HashMap<>();
    candidateNames = new HashSet<>();

    if (rawConfig.candidates != null) {
      for (RawContestConfig.Candidate candidate : rawConfig.candidates) {
        String name = candidate.getName();
        candidateNames.add(name);
        candidatePermutation.add(name);
        if (candidate.isExcluded()) {
          excludedCandidates.add(name);
        }

        Stream<String> aliases = candidate.createStreamOfNameAndAllAliases();
        aliases.forEach(nameOrAlias -> {
          // duplicate names and aliases get caught in validation
          candidateAliasesToNameMap.put(nameOrAlias, name);
          candidateAliasesToCodeMap.put(nameOrAlias, candidate.getCode());
        });
      }
    }

    // If any of the sources support undeclared write-ins, we need to recognize them as a valid
    // "candidate" option.
    if (undeclaredWriteInsEnabled()) {
      candidateNames.add(Tabulator.UNDECLARED_WRITE_IN_OUTPUT_LABEL);
      candidateAliasesToNameMap.put(
          Tabulator.UNDECLARED_WRITE_IN_OUTPUT_LABEL, Tabulator.UNDECLARED_WRITE_IN_OUTPUT_LABEL);
      candidateAliasesToCodeMap.put(
          Tabulator.UNDECLARED_WRITE_IN_OUTPUT_LABEL, Tabulator.UNDECLARED_WRITE_IN_OUTPUT_LABEL);
    }
  }

  private boolean undeclaredWriteInsEnabled() {
    boolean includeUwi = false;
    for (CvrSource source : rawConfig.cvrFileSources) {
      if (!isNullOrBlank(source.getUndeclaredWriteInLabel())
          || source.isTreatBlankAsUndeclaredWriteIn()) {
        includeUwi = true;
        break;
      }
    }
    return includeUwi;
  }

  // Possible validation errors
  enum ValidationError {
    TABULATOR_VERSION_MISSING,
    TABULATOR_VERSION_NOT_SUPPORTED,
    OUTPUT_CONTEST_NAME_MISSING,
    CVR_NO_FILES_SPECIFIED,
    CVR_FILE_PATH_MISSING,
    CVR_OVERVOTE_LABEL_INVALID,
    CVR_UNDERVOTE_LABEL_INVALID,
    CVR_UWI_LABEL_INVALID,
    CVR_PROVIDER_INVALID,
    CVR_FIRST_VOTE_COLUMN_INVALID,
    CVR_FIRST_VOTE_ROW_INVALID,
    CVR_ID_COLUMN_INVALID,
    CVR_PRECINCT_COLUMN_INVALID,
    CVR_OVERVOTE_DELIMITER_INVALID,
    CVR_CDF_FILE_PATH_INVALID,
    CVR_TREAT_BLANK_AS_UWI_UNEXPECTEDLY_TRUE,
    CVR_CONTEST_ID_INVALID,
    CVR_DUPLICATE_FILE_PATHS,
    CVR_FILE_PATH_INVALID,
    CVR_OVERVOTE_LABEL_OVERVOTE_RULE_MISMATCH,
    CVR_CDF_TABULATE_BY_PRECINCT_DISAGREEMENT,
    CVR_TABULATE_BY_PRECINCT_REQUIRES_PRECINCT_COLUMN,
    CVR_OVERVOTE_DELIMITER_AND_LABEL_BOTH_SUPPLIED,
    CVR_OVERVOTE_DELIMITER_MISSING,
    CVR_OVERVOTE_DELIMITER_UNEXPECTEDLY_DEFINED,
    CVR_OVERVOTE_UNEXPECTEDLY_DEFINED,
    CVR_FIRST_VOTE_UNEXPECTEDLY_DEFINED,
    CVR_FIRST_VOTE_ROW_UNEXPECTEDLY_DEFINED,
    CVR_ID_COLUMN_UNEXPECTEDLY_DEFINED,
    CVR_PRECINCT_COLUMN_UNEXPECTEDLY_DEFINED,
    CVR_UNDERVOTE_LABEL_UNEXPECTEDLY_DEFINED,
    CVR_CONTEST_ID_UNEXPECTEDLY_DEFINED,
    CANDIDATE_NAME_MISSING,
    CANDIDATE_CODE_INVALID,
    CANDIDATE_DUPLICATE_NAME,
    CANDIDATE_NO_CANDIDATES_SPECIFIED,
    CANDIDATE_ALL_EXCLUDED,
    RULES_TIEBREAK_MODE_INVALID,
    RULES_RANDOM_SEED_INVALID,
    RULES_RANDOM_SEED_MISSING,
    RULES_OVERVOTE_RULE_INVALID,
    RULES_WINNER_ELECTION_MODE_INVALID,
    RULES_MAX_RANKINGS_ALLOWED_INVALID,
    RULES_MAX_SKIPPED_RANKS_ALLOWED_INVALID,
    RULES_NUMBER_OF_WINNERS_INVALID,
    RULES_MIN_DECIMAL_PLACES_FOR_VOTE_ARITHMETIC_INVALID,
    RULES_MIN_VOTE_THRESHOLD_INVALID,
    RULES_MULTI_SEAT_BOTTOMS_UP_PERCENTAGE_THRESHOLD_INVALID,
    RULES_STOP_TABULATION_EARLY_AFTER_ROUND_INVALID,
    RULES_NUMBER_OF_WINNERS_INVALID_FOR_WINNER_ELECTION_MODE,
    RULES_CONTINUE_UNTIL_TWO_CANDIDATES_REMAIN_TRUE_FOR_MULTI_SEAT,
    RULES_BATCH_ELIMINATION_TRUE_FOR_MULTI_SEAT,
    RULES_WINNER_ELECTION_MODE_INVALID_FOR_SINGLE_SEAT,
    RULES_ZERO_WINNERS_INVALID_WINNER_ELECTION_MODE,
    RULES_PERCENTAGE_THRESHOLD_MISSING,
    RULES_BOTTOMS_UP_THRESHOLD_BATCH_ELIMINATION_DISAGREEMENT,
    RULES_NON_INTEGER_WINNING_THRESHOLD_WINNER_ELECTION_MODE_DISAGREEMENT,
    RULES_HARE_QUOTA_WINNER_ELECTION_MODE_DISAGREEMENT,
    RULES_NON_INTEGER_WINNING_THRESHOLD_HARE_QUOTA_DISAGREEMENT
  }

  enum Provider {
    CDF("cdf", "CDF"),
    CLEAR_BALLOT("clearBallot", "Clear Ballot"),
    DOMINION("dominion", "Dominion"),
    ESS("ess", "ES&S"),
    HART("hart", "Hart"),
    CSV("genericCsv", "CSV"),
    PROVIDER_UNKNOWN("providerUnknown", "Provider unknown");

    private final String internalLabel;
    private final String guiLabel;

    Provider(String internalLabel, String guiLabel) {
      this.internalLabel = internalLabel;
      this.guiLabel = guiLabel;
    }

    BaseCvrReader constructReader(ContestConfig config, CvrSource source)
            throws UnrecognizedProviderException {
      switch (this) {
        case CDF:
          return new CommonDataFormatReader(config, source);
        case CLEAR_BALLOT:
          return new ClearBallotCvrReader(config, source);
        case DOMINION:
          return new DominionCvrReader(config, source);
        case ESS:
          return new StreamingCvrReader(config, source);
        case HART:
          return new HartCvrReader(config, source);
        case CSV:
          return new CsvCvrReader(config, source);
        case PROVIDER_UNKNOWN:
        default:
          throw new UnrecognizedProviderException();
      }
    }

    static Provider getByInternalLabel(String labelLookup) {
      return Arrays.stream(Provider.values())
          .filter(v -> v.internalLabel.equals(labelLookup))
          .findAny()
          .orElse(PROVIDER_UNKNOWN);
    }

    @Override
    public String toString() {
      return guiLabel;
    }

    public String getInternalLabel() {
      return internalLabel;
    }
  }

  static class UnrecognizedProviderException extends Exception {

  }
}
