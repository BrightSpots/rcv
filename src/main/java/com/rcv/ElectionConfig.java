/*
 * Ranked Choice Voting Universal Tabulator
 * Copyright (C) 2018 Jonathan Moldover, Louis Eisenberg, and Hylton Edingfield
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
 * Wrapper for RawElectionConfig object. This class adds logic for looking up rule enum
 * names, candidate names, various configuration utilities, and cast vote record objects.
 */

package com.rcv;

import com.rcv.Tabulator.TieBreakMode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

class ElectionConfig {

  // underlying rawConfig object data
  final RawElectionConfig rawConfig;
  // this is used if we have a permutation-based tie-break mode
  private final ArrayList<String> candidatePermutation = new ArrayList<>();
  // mapping from candidate code to full name
  private Map<String, String> candidateCodeToNameMap;
  // minimum vote threshold if one is specified
  private BigDecimal minimumVoteThreshold;

  // function: ElectionConfig
  // purpose: create a new ElectionConfig object
  // param: rawConfig underlying rawConfig object this object wraps
  ElectionConfig(RawElectionConfig rawConfig) {
    this.rawConfig = rawConfig;
    this.processCandidateData();
  }

  // function: multiSeatTransferRuleForConfigSetting
  // purpose: given setting String return the corresponding rules enum
  // param: MultiSeatTransferRule setting string from election config
  // returns: the MultiSeatTransferRule enum value for the input setting string
  private static Tabulator.MultiSeatTransferRule multiSeatTransferRuleForConfigSetting(
      String setting) {
    // rule: return value determined by input setting string
    Tabulator.MultiSeatTransferRule rule = Tabulator.MultiSeatTransferRule.getByLabel(setting);
    if (rule == null) {
      rule = Tabulator.MultiSeatTransferRule.TRANSFER_RULE_UNKNOWN;
    }
    return rule;
  }

  // function: overvoteRuleForConfigSetting
  // purpose: given setting String return the corresponding rules enum
  // param: OvervoteRule setting string from election config
  // returns: the OvervoteRule enum value for the input setting string
  private static Tabulator.OvervoteRule overvoteRuleForConfigSetting(String setting) {
    // rule: return value determined by input setting string
    Tabulator.OvervoteRule rule = Tabulator.OvervoteRule.getByLabel(setting);
    if (rule == null) {
      rule = Tabulator.OvervoteRule.RULE_UNKNOWN;
    }
    return rule;
  }

  // function: tieBreakModeForConfigSetting
  // purpose: given setting string return corresponding rule enum
  // param: TieBreakMode setting string read from election config
  // returns: TieBreakMode enum value for the input setting string
  private static Tabulator.TieBreakMode tieBreakModeForConfigSetting(String setting) {
    // mode: return value determined by input setting string
    Tabulator.TieBreakMode mode = Tabulator.TieBreakMode.getByLabel(setting);
    if (mode == null) {
      mode = Tabulator.TieBreakMode.MODE_UNKNOWN;
    }
    return mode;
  }

  // function: getValidationErrors
  // purpose: validate the correctness of the config data
  // returns any detected problems
  List<String> getValidationErrors() {
    // detected errors
    List<String> errors = new LinkedList<>();

    // TODO: need to add checks that all required String fields !.equals("")

    if (getNumDeclaredCandidates() == 0) {
      errors.add("Config must contain at least one declared candidate.");
    }

    if (getNumberOfWinners() < 1 || getNumberOfWinners() > 100) {
      errors.add("Number of winners must be between 1 and 100");
    }

    if (getOvervoteRule() == Tabulator.OvervoteRule.RULE_UNKNOWN) {
      // TODO: report what the invalid value was?
      errors.add("Invalid overvote rule.");
    } else if (getOvervoteLabel() != null
        && getOvervoteRule() != Tabulator.OvervoteRule.EXHAUST_IMMEDIATELY
        && getOvervoteRule() != Tabulator.OvervoteRule.ALWAYS_SKIP_TO_NEXT_RANK) {
      errors.add(
          "When overvoteLabel is supplied, overvoteRule must be either exhaustImmediately or "
              + "alwaysSkipToNextRank.");
    }

    if (getTiebreakMode() == Tabulator.TieBreakMode.MODE_UNKNOWN) {
      errors.add("Invalid tie-break mode.");
    }

    if (getMaxSkippedRanksAllowed() != null && getMaxSkippedRanksAllowed() < 0) {
      errors.add("maxSkippedRanksAllowed can't be negative.");
    }

    if (getMaxRankingsAllowed() != null && getMaxRankingsAllowed() < 1) {
      errors.add("maxRankingsAllowed must be positive.");
    }

    // If this is a multi-seat election, we validate a number of extra parameters.
    //
    if (getNumberOfWinners() > 1) {
      if (willContinueUntilTwoCandidatesRemain()) {
        errors.add("continueUntilTwoCandidatesRemain can't be true in a multi-winner election.");
      }

      if (isBatchEliminationEnabled()) {
        errors.add("batchElimination can't be true in a multi-winner election.");
      }

      if (getDecimalPlacesForVoteArithmetic() < 0 || getDecimalPlacesForVoteArithmetic() > 20) {
        errors.add("decimalPlacesForVoteArithmetic must be between 0 and 20 (inclusive).");
      }

      if (multiSeatTransferRule() == Tabulator.MultiSeatTransferRule.TRANSFER_RULE_UNKNOWN) {
        errors.add("Invalid multiSeatTransferRule.");
      }
    }

    return errors;
  }

  // function: getNumberWinners
  // purpose: how many winners for this election
  // returns from settings config or 1 of no setting is specified
  Integer getNumberOfWinners() {
    return rawConfig.rules.numberOfWinners == null ? 1 : rawConfig.rules.numberOfWinners;
  }

  // function: getDecimalPlacesForVoteArithmetic
  // purpose: how many places to round votes to after performing fractional vote transfers
  // returns: number of places to round to or 0 if no setting is specified
  private Integer getDecimalPlacesForVoteArithmetic() {
    // we default to using 4 places for fractional transfer vote arithmetic
    return rawConfig.rules.decimalPlacesForVoteArithmetic == null
        ? 4
        : rawConfig.rules.decimalPlacesForVoteArithmetic;
  }

  // function: divide
  // purpose: perform a division operation according to the config settings
  // param: dividend is the numerator in the division operation
  // param: divisor is the denominator in the division operation
  // returns: the quotient
  BigDecimal divide(BigDecimal dividend, BigDecimal divisor) {
    return dividend.divide(divisor, getDecimalPlacesForVoteArithmetic(), RoundingMode.HALF_EVEN);
  }

  // function: roundDecimal
  // purpose: round a number according to the config settings
  // returns: the rounded value
  BigDecimal roundDecimal(BigDecimal bd) {
    return bd.setScale(getDecimalPlacesForVoteArithmetic(), RoundingMode.HALF_EVEN);
  }

  // function: multiSeatTransferRule
  // purpose: which surplus transfer rule to use in multi-seat elections
  // returns: enum indicating which transfer rule to use
  private Tabulator.MultiSeatTransferRule multiSeatTransferRule() {
    return multiSeatTransferRuleForConfigSetting(rawConfig.rules.multiSeatTransferRule);
  }

  // function: getOutputDirectory
  // purpose: getter for outputDirectory
  // returns: directory string from config or falls back to working directory
  String getOutputDirectory() {
    // outputDirectory is where output files should be written
    String outputDirectory = System.getProperty("user.dir");
    if (rawConfig.outputDirectory != null) {
      outputDirectory = rawConfig.outputDirectory;
    }
    return outputDirectory;
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
    return rawConfig.contestName;
  }

  // function: getJurisdiction
  // purpose: getter for jurisdiction
  // returns: jurisdiction name
  String getJurisdiction() {
    return rawConfig.jurisdiction;
  }

  // function: getOffice
  // purpose: getter for office
  // returns: office name
  String getOffice() {
    return rawConfig.office;
  }

  // function: electionDate
  // purpose: getter for electionDate
  // returns: election date
  String getElectionDate() {
    return rawConfig.contestDate;
  }

  // function: isTabulateByPrecinctEnabled
  // purpose: getter for tabulateByPrecinct
  // returns: true if and only if we should tabulate by precinct
  boolean isTabulateByPrecinctEnabled() {
    return rawConfig.tabulateByPrecinct;
  }

  // function: getMaxRankingsAllowed
  // purpose: getter for maxRankingsAllowed
  // returns: max rankings allowed
  Integer getMaxRankingsAllowed() {
    return rawConfig.rules.maxRankingsAllowed;
  }

  // function: getDescription
  // purpose: getter for description
  // returns: description
  String getDescription() {
    return rawConfig.rules.description;
  }

  // function: isBatchEliminationEnabled
  // purpose: getter for batchElimination
  // returns: true if and only if we should use batch elimination
  boolean isBatchEliminationEnabled() {
    return rawConfig.rules.batchElimination;
  }

  // function: numDeclaredCandidates
  // purpose: calculate the number of declared candidates from the election configuration
  // returns: the number of declared candidates from the election configuration
  int getNumDeclaredCandidates() {
    // num will contain the resulting number of candidates
    int num = getCandidateCodeList().size();
    if (getUndeclaredWriteInLabel() != null
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

  // function: getOvervoteRule
  // purpose: return overvote rule enum to use
  // returns: overvote rule to use for this config
  Tabulator.OvervoteRule getOvervoteRule() {
    // by default we exhaust immediately
    return rawConfig.rules.overvoteRule == null
        ? Tabulator.OvervoteRule.EXHAUST_IMMEDIATELY
        : ElectionConfig.overvoteRuleForConfigSetting(rawConfig.rules.overvoteRule);
  }

  // function: getMinimumVoteThreshold
  // purpose: getter for minimumVoteThreshold rule
  // returns: minimum vote threshold to use for this config
  BigDecimal getMinimumVoteThreshold() {
    if (minimumVoteThreshold == null) {
      if (rawConfig.rules.minimumVoteThreshold == null) {
        minimumVoteThreshold = BigDecimal.ZERO;
      } else {
        minimumVoteThreshold = new BigDecimal(rawConfig.rules.minimumVoteThreshold);
      }
    }
    return minimumVoteThreshold;
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
    // by default we use random tiebreak
    return rawConfig.rules.tiebreakMode == null
        ? Tabulator.TieBreakMode.RANDOM
        : ElectionConfig.tieBreakModeForConfigSetting(rawConfig.rules.tiebreakMode);
  }

  // function: isTreatBlankAsUndeclaredWriteInEnabled
  // purpose: getter for treatBlankAsUndeclaredWriteIn rule
  // returns: true if we are to treat blank cell as UWI
  boolean isTreatBlankAsUndeclaredWriteInEnabled() {
    return rawConfig.treatBlankAsUndeclaredWriteIn;
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
      // candidate is used to index through all candidates for this election
      for (RawElectionConfig.Candidate candidate : rawConfig.candidates) {
        if (candidate.getCode() != null) {
          candidateCodeToNameMap.put(candidate.getCode(), candidate.getName());
          candidatePermutation.add(candidate.getCode());
        } else {
          candidateCodeToNameMap.put(candidate.getName(), candidate.getName());
          candidatePermutation.add(candidate.getName());
        }
      }

      if (getTiebreakMode() == TieBreakMode.GENERATE_PERMUTATION) {
        Collections.shuffle(candidatePermutation);
      }

      String uwiLabel = getUndeclaredWriteInLabel();
      if (uwiLabel != null) {
        candidateCodeToNameMap.put(uwiLabel, uwiLabel);
      }
    }
  }
}
