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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

class ContestConfig {

  // TODO: if any booleans are unspecified in config file, they default to false no matter what
  static final boolean DEFAULT_TABULATE_BY_PRECINCT = false;
  static final boolean DEFAULT_NON_INTEGER_WINNING_THRESHOLD = false;
  static final boolean DEFAULT_BATCH_ELIMINATION = false;
  static final boolean DEFAULT_EXHAUST_ON_DUPLICATE_CANDIDATES = false;
  static final boolean DEFAULT_CONTINUE_UNTIL_TWO_CANDIDATES_REMAIN = false;
  static final boolean DEFAULT_TREAT_BLANK_AS_UNDECLARED_WRITE_IN = false;
  static final int DEFAULT_DECIMAL_PLACES_FOR_VOTE_ARITHMETIC = 4;
  static final int DEFAULT_NUMBER_OF_WINNERS = 1;
  static final BigDecimal DEFAULT_MINIMUM_VOTE_THRESHOLD = BigDecimal.ZERO;

  static final boolean DEFAULT_CANDIDATE_EXCLUDED = false;

  // underlying rawConfig object data
  private final RawContestConfig rawConfig;
  // this is used if we have a permutation-based tie-break mode
  private final ArrayList<String> candidatePermutation = new ArrayList<>();
  private final Set<String> excludedCandidates = new HashSet<>();
  // mapping from candidate code to full name
  private Map<String, String> candidateCodeToNameMap;
  // whether or not there are any validation errors
  private boolean isValid;

  // function: ContestConfig
  // purpose: create a new ContestConfig object
  // param: rawConfig underlying rawConfig object this object wraps
  ContestConfig(RawContestConfig rawConfig) {
    this.rawConfig = rawConfig;
    this.processCandidateData();
  }

  RawContestConfig getRawConfig() {
    return rawConfig;
  }

  // function: validate
  // purpose: validate the correctness of the config data
  // returns any detected problems
  boolean validate() {
    Logger.log(Level.INFO, "Validating config...");
    isValid = true;

    validateOutputSettings();
    validateCvrFileSources();
    validateCandidates();
    validateRules();

    if (isValid) {
      Logger.log(Level.INFO, "Config validation successful.");
    } else {
      Logger.log(
          Level.SEVERE, "Config validation failed! Please modify the config file and try again.");
    }

    return isValid;
  }

  private void validateOutputSettings() {
    if (getContestName() == null || getContestName().isEmpty()) {
      isValid = false;
      Logger.log(Level.SEVERE, "Contest name is required.");
    }

    if (getOutputDirectoryRaw() == null || getOutputDirectoryRaw().isEmpty()) {
      isValid = false;
      Logger.log(Level.SEVERE, "Output directory is required.");
    }
  }

  private void validateCvrFileSources() {
    if (rawConfig.cvrFileSources == null || rawConfig.cvrFileSources.isEmpty()) {
      isValid = false;
      Logger.log(Level.SEVERE, "Config doesn't contain any CVR files.");
    } else {
      HashSet<String> cvrFilePathSet = new HashSet<>();
      for (CVRSource source : rawConfig.cvrFileSources) {
        // perform checks on source input path
        if (source.getFilePath() == null || source.getFilePath().isEmpty()) {
          isValid = false;
          Logger.log(Level.SEVERE, "filePath is required for each CVR file.");
          continue;
        }

        // look for duplicate paths
        if (cvrFilePathSet.contains(source.getFullFilePath())) {
          isValid = false;
          Logger.log(
              Level.SEVERE, "Duplicate CVR filePaths are not allowed: %s",
              source.getFullFilePath());
        } else {
          cvrFilePathSet.add(source.getFullFilePath());
        }

        // ensure file exists
        if (!new File(source.getFullFilePath()).exists()) {
          isValid = false;
          Logger.log(Level.SEVERE, "CVR file not found: %s", source.getFullFilePath());
        }

        // ensure valid first vote column value
        if (source.getFirstVoteColumnIndex() == null) {
          isValid = false;
          Logger
              .log(Level.SEVERE, "firstVoteColumnIndex is required: %s", source.getFullFilePath());
        } else if (source.getFirstVoteColumnIndex() < 1
            || source.getFirstVoteColumnIndex() > 1000) {
          isValid = false;
          Logger.log(
              Level.SEVERE,
              "firstVoteColumnIndex must be from 1 to 1000: %s",
              source.getFullFilePath());
        }

        // ensure valid first vote row value
        if (source.getFirstVoteRowIndex() == null) {
          isValid = false;
          Logger.log(Level.SEVERE, "firstVoteRowIndex is required: %s", source.getFullFilePath());
        } else if (source.getFirstVoteRowIndex() < 1 || source.getFirstVoteRowIndex() > 1000) {
          isValid = false;
          Logger.log(
              Level.SEVERE, "firstVoteRowIndex must be from 1 to 1000: %s",
              source.getFullFilePath());
        }

        // ensure valid id column value
        if (source.getIdColumnIndex() != null
            && (source.getIdColumnIndex() < 1 || source.getIdColumnIndex() > 1000)) {
          isValid = false;
          Logger.log(
              Level.SEVERE, "idColumnIndex must be from 1 to 1000: %s", source.getFullFilePath());
        }

        // ensure valid precinct column value
        if (isTabulateByPrecinctEnabled()) {
          if (source.getPrecinctColumnIndex() == null) {
            isValid = false;
            Logger.log(
                Level.SEVERE,
                "precinctColumnIndex is required when tabulateByPrecinct is enabled: %s",
                source.getFullFilePath());
          } else if (source.getPrecinctColumnIndex() < 1
              || source.getPrecinctColumnIndex() > 1000) {
            isValid = false;
            Logger.log(
                Level.SEVERE,
                "precinctColumnIndex must be from 1 to 1000: %s",
                source.getFullFilePath());
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
        Logger.log(Level.SEVERE, "Name is required for each candidate.");
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
          "If candidate codes are used, a unique code is required for each candidate.");
    }

    if (getNumDeclaredCandidates() < 1) {
      isValid = false;
      Logger.log(Level.SEVERE, "Config must contain at least one declared candidate.");
    }

    if (getNumDeclaredCandidates() == excludedCandidates.size()) {
      isValid = false;
      Logger.log(Level.SEVERE, "Config must contain at least one non-excluded candidate.");
    }
  }

  private void validateRules() {
    if (getTiebreakMode() == Tabulator.TieBreakMode.MODE_UNKNOWN) {
      isValid = false;
      Logger.log(Level.SEVERE, "Invalid tie-break mode.");
    }

    if (getOvervoteRule() == Tabulator.OvervoteRule.RULE_UNKNOWN) {
      isValid = false;
      Logger.log(Level.SEVERE, "Invalid overvote rule.");
    } else if ((getOvervoteLabel() != null && !getOvervoteLabel().isEmpty())
        && getOvervoteRule() != Tabulator.OvervoteRule.EXHAUST_IMMEDIATELY
        && getOvervoteRule() != Tabulator.OvervoteRule.ALWAYS_SKIP_TO_NEXT_RANK) {
      isValid = false;
      Logger.log(
          Level.SEVERE,
          "When overvoteLabel is supplied, overvoteRule must be either exhaustImmediately "
              + "or alwaysSkipToNextRank.");
    }

    if (getMaxRankingsAllowed() < 1 || getMaxRankingsAllowed() > 100) {
      isValid = false;
      Logger.log(Level.SEVERE, "maxRankingsAllowed must be from 1 to 100.");
    }

    if (getMaxSkippedRanksAllowed() != null
        && (getMaxSkippedRanksAllowed() < 0 || getMaxSkippedRanksAllowed() > 100)) {
      isValid = false;
      Logger.log(Level.SEVERE, "maxSkippedRanksAllowed must be from 0 to 100.");
    }

    if (getNumberOfWinners() < 1 || getNumberOfWinners() > 100) {
      isValid = false;
      Logger.log(Level.SEVERE, "numberOfWinners must be from 1 to 100.");
    }

    if (getMinimumVoteThreshold().intValue() < 0 || getMinimumVoteThreshold().intValue() > 10000) {
      isValid = false;
      Logger.log(Level.SEVERE, "minimumVoteThreshold must be from 0 to 10000.");
    }

    // If this is a multi-seat contest, we validate a number of extra parameters.
    if (getNumberOfWinners() > 1) {
      if (willContinueUntilTwoCandidatesRemain()) {
        isValid = false;
        Logger.log(
            Level.SEVERE,
            "continueUntilTwoCandidatesRemain can't be true in a multi-winner contest.");
      }

      if (isBatchEliminationEnabled()) {
        isValid = false;
        Logger.log(Level.SEVERE, "batchElimination can't be true in a multi-winner contest.");
      }

      if (getDecimalPlacesForVoteArithmetic() < 0 || getDecimalPlacesForVoteArithmetic() > 20) {
        isValid = false;
        Logger.log(Level.SEVERE, "decimalPlacesForVoteArithmetic must be from 0 to 20.");
      }
    }
  }

  // function: getNumberWinners
  // purpose: how many winners for this contest
  // returns: number of winners or default value if it's not specified
  Integer getNumberOfWinners() {
    return rawConfig.rules.numberOfWinners == null
        ? DEFAULT_NUMBER_OF_WINNERS
        : rawConfig.rules.numberOfWinners;
  }

  // function: getDecimalPlacesForVoteArithmetic
  // purpose: how many places to round votes to after performing fractional vote transfers
  // returns: number of places to round to or default value if it's not specified
  Integer getDecimalPlacesForVoteArithmetic() {
    return rawConfig.rules.decimalPlacesForVoteArithmetic == null
        ? DEFAULT_DECIMAL_PLACES_FOR_VOTE_ARITHMETIC
        : rawConfig.rules.decimalPlacesForVoteArithmetic;
  }

  boolean isNonIntegerWinningThresholdEnabled() {
    return rawConfig.rules.nonIntegerWinningThreshold;
  }

  // function: divide
  // purpose: perform a division operation according to the config settings
  // param: dividend is the numerator in the division operation
  // param: divisor is the denominator in the division operation
  // returns: the quotient
  BigDecimal divide(BigDecimal dividend, BigDecimal divisor) {
    return dividend.divide(divisor, getDecimalPlacesForVoteArithmetic(), RoundingMode.HALF_EVEN);
  }

  BigDecimal multiply(BigDecimal multiplier, BigDecimal multiplicand) {
    return multiplier
        .multiply(multiplicand)
        .setScale(getDecimalPlacesForVoteArithmetic(), RoundingMode.HALF_EVEN);
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
    return FileUtils.resolveUserPath(getOutputDirectoryRaw());
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

  // function: getRulesDescription
  // purpose: getter for rules description
  // returns: rules description
  String getRulesDescription() {
    return rawConfig.rules.rulesDescription;
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
    return rawConfig.rules.minimumVoteThreshold == null
        ? DEFAULT_MINIMUM_VOTE_THRESHOLD
        : new BigDecimal(rawConfig.rules.minimumVoteThreshold);
  }

  // function: getMaxSkippedRanksAllowed
  // purpose: getter for maxSkippedRanksAllowed rule
  // returns: max skipped ranks allowed in this config
  Integer getMaxSkippedRanksAllowed() {
    return rawConfig.rules.maxSkippedRanksAllowed;
  }

  // function: getUndeclaredWriteInLabel
  // purpose: getter for UWI label
  // returns: overvote rule for this config
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
    if (getUndeclaredWriteInLabel() != null && getUndeclaredWriteInLabel().equals(candidateID)) {
      return "Undeclared";
    }
    return candidateCodeToNameMap.get(candidateID);
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
