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
 * Wrapper for RawContestConfig object. This class adds logic for looking up rule enum
 * names, candidate names, various configuration utilities, and cast vote record objects.
 */

package com.rcv;

import com.rcv.RawContestConfig.CVRSource;
import com.rcv.RawContestConfig.Candidate;
import com.rcv.Tabulator.TieBreakMode;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

class ContestConfig {

  // If any booleans are unspecified in config file, they should default to false no matter what
  static final boolean SUGGESTED_TABULATE_BY_PRECINCT = false;
  static final boolean SUGGESTED_CANDIDATE_EXCLUDED = false;
  static final boolean SUGGESTED_NON_INTEGER_WINNING_THRESHOLD = false;
  static final boolean SUGGESTED_HARE_QUOTA = false;
  static final boolean SUGGESTED_BATCH_ELIMINATION = false;
  static final boolean SUGGESTED_CONTINUE_UNTIL_TWO_CANDIDATES_REMAIN = false;
  static final boolean SUGGESTED_EXHAUST_ON_DUPLICATE_CANDIDATES = false;
  static final boolean SUGGESTED_TREAT_BLANK_AS_UNDECLARED_WRITE_IN = false;
  static final int SUGGESTED_NUMBER_OF_WINNERS = 1;
  static final int SUGGESTED_DECIMAL_PLACES_FOR_VOTE_ARITHMETIC = 4;
  static final BigDecimal SUGGESTED_MINIMUM_VOTE_THRESHOLD = BigDecimal.ZERO;
  static final int SUGGESTED_MAX_SKIPPED_RANKS_ALLOWED = 1;

  private static final int MIN_COLUMN_INDEX = 1;
  private static final int MAX_COLUMN_INDEX = 1000;
  private static final int MIN_ROW_INDEX = 1;
  private static final int MAX_ROW_INDEX = 100000;
  private static final int MIN_MAX_RANKINGS_ALLOWED = 1;
  private static final int MIN_MAX_SKIPPED_RANKS_ALLOWED = 0;
  private static final int MIN_NUMBER_OF_WINNERS = 1;
  private static final int MIN_DECIMAL_PLACES_FOR_VOTE_ARITHMETIC = 1;
  private static final int MAX_DECIMAL_PLACES_FOR_VOTE_ARITHMETIC = 20;
  private static final int MIN_MINIMUM_VOTE_THRESHOLD = 0;
  private static final int MAX_MINIMUM_VOTE_THRESHOLD = 1000000;

  // underlying rawConfig object data
  final RawContestConfig rawConfig;
  // this is used if we have a permutation-based tie-break mode
  private final ArrayList<String> candidatePermutation = new ArrayList<>();
  private final Set<String> excludedCandidates = new HashSet<>();
  // path from which any relative paths should be resolved
  private final String sourceDirectory;
  // mapping from candidate code to full name
  private Map<String, String> candidateCodeToNameMap;
  // whether or not there are any validation errors
  private boolean isValid;

  // function: ContestConfig
  // purpose: create a new ContestConfig object
  // param: rawConfig underlying rawConfig object this object wraps
  // param: sourceDirectory folder to use for resolving relative paths
  ContestConfig(RawContestConfig rawConfig, String sourceDirectory) {
    this.rawConfig = rawConfig;
    this.sourceDirectory = sourceDirectory;
    this.processCandidateData();
  }

  // function: loadContestConfig
  // purpose: factory method to create ContestConfig from configPath
  // - create rawContestConfig from file - can fail for IO issues or invalid json
  // - validate rawContestConfig - can fail if certain elements do not exist
  // - if the above succeed create and return ContestConfig wrapping rawContestConfig (cannot fail)
  // returns: new ContestConfig object if checks pass otherwise null
  static ContestConfig loadContestConfig(String configPath) {
    if (configPath == null) {
      Logger.log(Level.SEVERE, "No contest config path specified!");
      return null;
    }
    // config will hold the new ContestConfig if construction succeeds
    ContestConfig config = null;

    // rawConfig holds the basic contest config data parsed from json
    // this will be null if there is a problem loading it
    RawContestConfig rawConfig = JsonParser.readFromFile(configPath, RawContestConfig.class);
    if (rawConfig == null) {
      Logger.log(Level.SEVERE, "Failed to load contest config: %s", configPath);
    } else {
      Logger.log(Level.INFO, "Successfully loaded contest config: %s", configPath);
      // perform some additional sanity checks
      if (rawConfig.validate()) {
        // checks passed so create the ContestConfig
        config = new ContestConfig(rawConfig, new File(configPath).getParent());
      } else {
        Logger.log(Level.SEVERE, "Failed to create contest config!");
      }
    }
    return config;
  }

  // function: resolveConfigPath
  // purpose: given a path returns absolute path for use in File IO
  // param: path from this config file (cvr or output folder)
  // returns: resolved path
  String resolveConfigPath(String configPath) {
    // create File for IO operations
    File userFile = new File(configPath);
    // resolvedPath will be returned to caller
    String resolvedPath;
    if (userFile.isAbsolute()) {
      // path is already absolute so use as-is
      resolvedPath = userFile.getAbsolutePath();
    } else {
      // return sourceDirectory/configPath
      resolvedPath = Paths.get(sourceDirectory, configPath).toAbsolutePath().toString();
    }
    return resolvedPath;
  }

  RawContestConfig getRawConfig() {
    return rawConfig;
  }

  // function: validate
  // purpose: validate the correctness of the config data
  // returns any detected problems
  boolean validate() {
    Logger.log(Level.INFO, "Validating contest config...");
    isValid = true;
    validateOutputSettings();
    validateCvrFileSources();
    validateCandidates();
    validateRules();
    if (isValid) {
      Logger.log(Level.INFO, "Contest config validation successful.");
    } else {
      Logger.log(
          Level.SEVERE,
          "Contest config validation failed! Please modify the contest config file and try again.");
    }

    return isValid;
  }

  private void validateOutputSettings() {
    if (getContestName() == null || getContestName().isEmpty()) {
      isValid = false;
      Logger.log(Level.SEVERE, "Contest name is required!");
    }
  }

  private void validateCvrFileSources() {
    if (rawConfig.cvrFileSources == null || rawConfig.cvrFileSources.isEmpty()) {
      isValid = false;
      Logger.log(Level.SEVERE, "Contest config must contain at least 1 CVR file!");
    } else {
      HashSet<String> cvrFilePathSet = new HashSet<>();
      for (CVRSource source : rawConfig.cvrFileSources) {
        // perform checks on source input path
        if (source.getFilePath() == null || source.getFilePath().isEmpty()) {
          isValid = false;
          Logger.log(Level.SEVERE, "filePath is required for each CVR file!");
          continue;
        }
        // full path to CVR
        String cvrPath = resolveConfigPath(source.getFilePath());

        // look for duplicate paths
        if (cvrFilePathSet.contains(cvrPath)) {
          isValid = false;
          Logger.log(Level.SEVERE, "Duplicate CVR filePaths are not allowed: %s", cvrPath);
        } else {
          cvrFilePathSet.add(cvrPath);
        }

        // ensure file exists
        if (!new File(cvrPath).exists()) {
          isValid = false;
          Logger.log(Level.SEVERE, "CVR file not found: %s", cvrPath);
        }

        // ensure valid first vote column value
        if (source.getFirstVoteColumnIndex() == null) {
          isValid = false;
          Logger.log(Level.SEVERE, "firstVoteColumnIndex is required: %s", cvrPath);
        } else if (source.getFirstVoteColumnIndex() < MIN_COLUMN_INDEX
            || source.getFirstVoteColumnIndex() > MAX_COLUMN_INDEX) {
          isValid = false;
          Logger.log(
              Level.SEVERE,
              "firstVoteColumnIndex must be from %d to %d: %s",
              MIN_COLUMN_INDEX,
              MAX_COLUMN_INDEX,
              cvrPath);
        }

        // ensure valid first vote row value
        if (source.getFirstVoteRowIndex() == null) {
          isValid = false;
          Logger.log(Level.SEVERE, "firstVoteRowIndex is required: %s", cvrPath);
        } else if (source.getFirstVoteRowIndex() < MIN_ROW_INDEX
            || source.getFirstVoteRowIndex() > MAX_ROW_INDEX) {
          isValid = false;
          Logger.log(
              Level.SEVERE,
              "firstVoteRowIndex must be from %d to %d: %s",
              MIN_ROW_INDEX,
              MAX_ROW_INDEX,
              cvrPath);
        }

        // ensure valid id column value
        if (source.getIdColumnIndex() != null
            && (source.getIdColumnIndex() < MIN_COLUMN_INDEX
            || source.getIdColumnIndex() > MAX_COLUMN_INDEX)) {
          isValid = false;
          Logger.log(
              Level.SEVERE,
              "idColumnIndex must be from %d to %d: %s",
              MIN_COLUMN_INDEX,
              MAX_COLUMN_INDEX,
              cvrPath);
        }

        // ensure valid precinct column value
        if (isTabulateByPrecinctEnabled()) {
          if (source.getPrecinctColumnIndex() == null) {
            isValid = false;
            Logger.log(
                Level.SEVERE,
                "precinctColumnIndex is required when tabulateByPrecinct is enabled: %s",
                cvrPath);
          } else if (source.getPrecinctColumnIndex() < MIN_COLUMN_INDEX
              || source.getPrecinctColumnIndex() > MAX_COLUMN_INDEX) {
            isValid = false;
            Logger.log(
                Level.SEVERE,
                "precinctColumnIndex must be from %d to %d: %s",
                MIN_COLUMN_INDEX,
                MAX_COLUMN_INDEX,
                cvrPath);
          }
        }
      }
    }
  }

  private void validateCandidates() {
    HashSet<String> candidateNameSet = new HashSet<>();
    HashSet<String> candidateCodeSet = new HashSet<>();
    for (Candidate candidate : rawConfig.candidates) {
      if (candidate.getName() == null || candidate.getName().isEmpty()) {
        isValid = false;
        Logger.log(Level.SEVERE, "Name is required for each candidate!");
      } else if (candidateNameSet.contains(candidate.getName())) {
        isValid = false;
        Logger.log(
            Level.SEVERE, "Duplicate candidate names are not allowed: %s", candidate.getName());
      } else {
        candidateNameSet.add(candidate.getName());
      }

      if (candidate.getCode() != null && !candidate.getCode().isEmpty()) {
        if (candidateCodeSet.contains(candidate.getCode())) {
          isValid = false;
          Logger.log(
              Level.SEVERE, "Duplicate candidate codes are not allowed: %s", candidate.getCode());
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
    if (getTiebreakMode() == Tabulator.TieBreakMode.MODE_UNKNOWN) {
      isValid = false;
      Logger.log(Level.SEVERE, "Invalid tie-break mode!");
    }

    if (getOvervoteRule() == Tabulator.OvervoteRule.RULE_UNKNOWN) {
      isValid = false;
      Logger.log(Level.SEVERE, "Invalid overvote rule!");
    } else if ((getOvervoteLabel() != null && !getOvervoteLabel().isEmpty())
        && getOvervoteRule() != Tabulator.OvervoteRule.EXHAUST_IMMEDIATELY
        && getOvervoteRule() != Tabulator.OvervoteRule.ALWAYS_SKIP_TO_NEXT_RANK) {
      isValid = false;
      Logger.log(
          Level.SEVERE,
          "When overvoteLabel is supplied, overvoteRule must be either exhaustImmediately "
              + "or alwaysSkipToNextRank!");
    }

    if (getNumDeclaredCandidates() >= 1 && getMaxRankingsAllowed() < MIN_MAX_RANKINGS_ALLOWED) {
      isValid = false;
      Logger.log(
          Level.SEVERE, "maxRankingsAllowed must be %d or higher!", MIN_MAX_RANKINGS_ALLOWED);
    }

    if (getMaxSkippedRanksAllowed() != null
        && getMaxSkippedRanksAllowed() < MIN_MAX_SKIPPED_RANKS_ALLOWED) {
      isValid = false;
      Logger.log(
          Level.SEVERE,
          "maxSkippedRanksAllowed must be %d or higher if it's supplied!",
          MIN_MAX_SKIPPED_RANKS_ALLOWED);
    }

    if (getNumberOfWinners() == null
        || getNumberOfWinners() < MIN_NUMBER_OF_WINNERS
        || getNumberOfWinners() > getNumDeclaredCandidates()) {
      isValid = false;
      Logger.log(
          Level.SEVERE,
          "numberOfWinners must be at least %d and no more than the number "
              + "of declared candidates!",
          MIN_NUMBER_OF_WINNERS);
    }

    if (getDecimalPlacesForVoteArithmetic() == null
        || getDecimalPlacesForVoteArithmetic() < MIN_DECIMAL_PLACES_FOR_VOTE_ARITHMETIC
        || getDecimalPlacesForVoteArithmetic() > MAX_DECIMAL_PLACES_FOR_VOTE_ARITHMETIC) {
      isValid = false;
      Logger.log(
          Level.SEVERE,
          "decimalPlacesForVoteArithmetic must be from %d to %d!",
          MIN_DECIMAL_PLACES_FOR_VOTE_ARITHMETIC,
          MAX_DECIMAL_PLACES_FOR_VOTE_ARITHMETIC);
    }

    if (getMinimumVoteThreshold() == null
        || getMinimumVoteThreshold().intValue() < MIN_MINIMUM_VOTE_THRESHOLD
        || getMinimumVoteThreshold().intValue() > MAX_MINIMUM_VOTE_THRESHOLD) {
      isValid = false;
      Logger.log(
          Level.SEVERE,
          "minimumVoteThreshold must be from %d to %d!",
          MIN_MINIMUM_VOTE_THRESHOLD,
          MAX_MINIMUM_VOTE_THRESHOLD);
    }

    // If this is a multi-seat contest, we validate a couple extra parameters.
    if (getNumberOfWinners() != null && getNumberOfWinners() > 1) {
      if (willContinueUntilTwoCandidatesRemain()) {
        isValid = false;
        Logger.log(
            Level.SEVERE,
            "continueUntilTwoCandidatesRemain can't be true in a multi-winner contest!");
      }

      if (isBatchEliminationEnabled()) {
        isValid = false;
        Logger.log(Level.SEVERE, "batchElimination can't be true in a multi-winner contest!");
      }
    } else {
      if (isHareQuotaEnabled()) {
        isValid = false;
        Logger.log(Level.SEVERE, "hareQuota can't be true in a single-seat contest!");
      }
    }
  }

  // function: getNumberWinners
  // purpose: how many winners for this contest
  // returns: number of winners
  Integer getNumberOfWinners() {
    return rawConfig.rules.numberOfWinners;
  }

  // function: getDecimalPlacesForVoteArithmetic
  // purpose: how many places to round votes to after performing fractional vote transfers
  // returns: number of places to round to
  Integer getDecimalPlacesForVoteArithmetic() {
    return rawConfig.rules.decimalPlacesForVoteArithmetic;
  }

  boolean isNonIntegerWinningThresholdEnabled() {
    return rawConfig.rules.nonIntegerWinningThreshold;
  }

  boolean isHareQuotaEnabled() {
    return rawConfig.rules.hareQuota;
  }

  // function: divide
  // purpose: perform a division operation according to the config settings
  // param: dividend is the numerator in the division operation
  // param: divisor is the denominator in the division operation
  // returns: the quotient
  BigDecimal divide(BigDecimal dividend, BigDecimal divisor) {
    return dividend.divide(divisor, getDecimalPlacesForVoteArithmetic(), RoundingMode.DOWN);
  }

  BigDecimal multiply(BigDecimal multiplier, BigDecimal multiplicand) {
    return multiplier
        .multiply(multiplicand)
        .setScale(getDecimalPlacesForVoteArithmetic(), RoundingMode.DOWN);
  }

  // function: getOutputDirectoryRaw
  // purpose: getter for outputDirectory
  // returns: raw string from config or falls back to user folder if none is set
  String getOutputDirectoryRaw() {
    // outputDirectory is where output files should be written
    return (rawConfig.outputSettings.outputDirectory != null
        && !rawConfig.outputSettings.outputDirectory.isEmpty())
        ? rawConfig.outputSettings.outputDirectory
        : FileUtils.getUserDirectory();
  }

  // function: getOutputDirectory
  // purpose: get the directory location where output files should be written
  // returns: path to directory where output files should be written
  String getOutputDirectory() {
    return resolveConfigPath(getOutputDirectoryRaw());
  }

  // function: willContinueUntilTwoCandidatesRemain
  // purpose: getter for setting to keep tabulating beyond selecting winner until two candidates
  // remain
  // returns: whether to keep tabulating until two candidates remain
  boolean willContinueUntilTwoCandidatesRemain() {
    return rawConfig.rules.continueUntilTwoCandidatesRemain;
  }

  // function: getContestName
  // purpose: getter for contestName
  // returns: contest name
  String getContestName() {
    return rawConfig.outputSettings.contestName;
  }

  // function: getContestJurisdiction
  // purpose: getter for contestJurisdiction
  // returns: contest jurisdiction name
  String getContestJurisdiction() {
    return rawConfig.outputSettings.contestJurisdiction;
  }

  // function: getContestOffice
  // purpose: getter for contestOffice
  // returns: contest office name
  String getContestOffice() {
    return rawConfig.outputSettings.contestOffice;
  }

  // function: getContestDate
  // purpose: getter for contestDate
  // returns: contest date
  String getContestDate() {
    return rawConfig.outputSettings.contestDate;
  }

  // function: isTabulateByPrecinctEnabled
  // purpose: getter for tabulateByPrecinct
  // returns: true if and only if we should tabulate by precinct
  boolean isTabulateByPrecinctEnabled() {
    return rawConfig.outputSettings.tabulateByPrecinct;
  }

  // function: getMaxRankingsAllowed
  // purpose: getter for maxRankingsAllowed
  // returns: max rankings allowed (or falls back to the number of candidates)
  int getMaxRankingsAllowed() {
    return rawConfig.rules.maxRankingsAllowed != null
        ? rawConfig.rules.maxRankingsAllowed
        : getNumDeclaredCandidates();
  }

  // function: isBatchEliminationEnabled
  // purpose: getter for batchElimination
  // returns: true if and only if we should use batch elimination
  boolean isBatchEliminationEnabled() {
    return rawConfig.rules.batchElimination;
  }

  // function: numDeclaredCandidates
  // purpose: calculate the number of declared candidates from the contest configuration
  // returns: the number of declared candidates from the contest configuration
  int getNumDeclaredCandidates() {
    // num will contain the resulting number of candidates
    int num = getCandidateCodeList().size();
    if ((getUndeclaredWriteInLabel() != null && !getUndeclaredWriteInLabel().isEmpty())
        && getCandidateCodeList().contains(getUndeclaredWriteInLabel())) {
      num--;
    }
    return num;
  }

  // function: numCandidates
  // purpose: return number of candidates including UWIs as a candidate if they are in use
  // num will contain the resulting number of candidates
  int getNumCandidates() {
    return getCandidateCodeList().size();
  }

  boolean candidateIsExcluded(String candidate) {
    return excludedCandidates.contains(candidate);
  }

  // function: getOvervoteRule
  // purpose: return overvote rule enum to use
  // returns: overvote rule to use for this config
  Tabulator.OvervoteRule getOvervoteRule() {
    Tabulator.OvervoteRule rule = Tabulator.OvervoteRule.getByLabel(rawConfig.rules.overvoteRule);
    return rule == null ? Tabulator.OvervoteRule.RULE_UNKNOWN : rule;
  }

  // function: getMinimumVoteThreshold
  // purpose: getter for minimumVoteThreshold rule
  // returns: minimum vote threshold to use or default value if it's not specified
  BigDecimal getMinimumVoteThreshold() {
    return rawConfig.rules.minimumVoteThreshold != null
        ? new BigDecimal(rawConfig.rules.minimumVoteThreshold)
        : null;
  }

  // function: getMaxSkippedRanksAllowed
  // purpose: getter for maxSkippedRanksAllowed rule
  // returns: max skipped ranks allowed in this config (possibly null)
  Integer getMaxSkippedRanksAllowed() {
    return rawConfig.rules.maxSkippedRanksAllowed;
  }

  // function: getUndeclaredWriteInLabel
  // purpose: getter for UWI label
  // returns: UWI label for this config
  String getUndeclaredWriteInLabel() {
    return rawConfig.rules.undeclaredWriteInLabel;
  }

  // function: getOvervoteLabel
  // purpose: getter for overvote label rule
  // returns: overvote label for this config
  String getOvervoteLabel() {
    return rawConfig.rules.overvoteLabel;
  }

  // function: getUndervoteLabel
  // purpose: getter for undervote label
  // returns: undervote label for this config
  String getUndervoteLabel() {
    return rawConfig.rules.undervoteLabel;
  }

  // function: getTiebreakMode
  // purpose: return tiebreak mode to use
  // returns: tiebreak mode to use for this config
  Tabulator.TieBreakMode getTiebreakMode() {
    Tabulator.TieBreakMode mode = Tabulator.TieBreakMode.getByLabel(rawConfig.rules.tiebreakMode);
    return mode == null ? Tabulator.TieBreakMode.MODE_UNKNOWN : mode;
  }

  // function: isTreatBlankAsUndeclaredWriteInEnabled
  // purpose: getter for treatBlankAsUndeclaredWriteIn rule
  // returns: true if we are to treat blank cell as UWI
  boolean isTreatBlankAsUndeclaredWriteInEnabled() {
    return rawConfig.rules.treatBlankAsUndeclaredWriteIn;
  }

  // function: isExhaustOnDuplicateCandidateEnabled
  // purpose: getter for exhaustOnDuplicateCandidate rule
  // returns: true if tabulation should exhaust ballot when encountering a duplicate candidate
  boolean isExhaustOnDuplicateCandidateEnabled() {
    return rawConfig.rules.exhaustOnDuplicateCandidate;
  }

  // function: getCandidateCodeList
  // purpose: return list of candidate codes for this config
  // returns: return list of candidate codes for this config
  Set<String> getCandidateCodeList() {
    return candidateCodeToNameMap.keySet();
  }

  // function: getNameForCandidateID
  // purpose: lookup full candidate name given a candidate ID
  // param: candidateID the ID of the candidate whose name we want to lookup
  // returns: the full name for the given candidateID
  String getNameForCandidateID(String candidateID) {
    return getUndeclaredWriteInLabel() != null && getUndeclaredWriteInLabel().equals(candidateID)
        ? "Undeclared"
        : candidateCodeToNameMap.get(candidateID);
  }

  // function: getCandidatePermutation
  // purpose: getter for ordered list of candidates for tie-breaking
  // returns: ordered list of candidates
  ArrayList<String> getCandidatePermutation() {
    return candidatePermutation;
  }

  // function: processCandidateData
  // purpose: builds map of candidate ID to candidate name and possibly generates tie-break ordering
  private void processCandidateData() {
    candidateCodeToNameMap = new HashMap<>();

    if (rawConfig.candidates != null) {
      // candidate is used to index through all candidates for this contest
      for (RawContestConfig.Candidate candidate : rawConfig.candidates) {
        String code = candidate.getCode();
        String name = candidate.getName();
        if (code == null || code.isEmpty()) {
          code = name;
        }

        // duplicate names or codes get caught in validation
        candidateCodeToNameMap.put(code, name);
        candidatePermutation.add(code);
        if (candidate.isExcluded()) {
          excludedCandidates.add(code);
        }
      }

      if (getTiebreakMode() == TieBreakMode.GENERATE_PERMUTATION) {
        Collections.shuffle(candidatePermutation);
      }

      String uwiLabel = getUndeclaredWriteInLabel();
      if (uwiLabel != null && !uwiLabel.isEmpty()) {
        candidateCodeToNameMap.put(uwiLabel, uwiLabel);
      }
    }
  }
}
