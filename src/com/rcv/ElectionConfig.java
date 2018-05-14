/*
 * Created by Jonathan Moldover, Louis Eisenberg, and Hylton Edingfield
 * Copyright 2018 Bright Spots
 * Purpose: Wrapper for RawElectionConfig object
 * This class adds logic for looking up rule enum names
 * candidate names and various configuration utilities
 * cast vote record objects.
 * Version: 1.0
 */

package com.rcv;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ElectionConfig {

  // underlying rawConfig object data
  final RawElectionConfig rawConfig;
  // list of all declared candidate codes
  private ArrayList<String> candidateCodeList;
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

  // function: overvoteRuleForConfigSetting
  // purpose: given setting String return the corresponding rules enum
  // param: OvervoteRule setting string from election config
  // returns: the OvervoteRule enum value for the input setting string
  private static Tabulator.MultiSeatTransferRule multiSeatTransferRuleForConfigSetting(
      String setting
  ) {
    // rule: return value determined by input setting string
    Tabulator.MultiSeatTransferRule rule = Tabulator.MultiSeatTransferRule.TRANSFER_RULE_UNKNOWN;

    switch (setting) {
      case "transferFractionalSurplus":
        rule = Tabulator.MultiSeatTransferRule.TRANSFER_FRACTIONAL_SURPLUS;
        break;
      case "transferWholeSurplus":
        rule = Tabulator.MultiSeatTransferRule.TRANSFER_WHOLE_SURPLUS;
        break;
      default:
        Logger.log("Unrecognized MultiSeatTransferRule setting: %s", setting);
    }
    return rule;
  }

  // function: overvoteRuleForConfigSetting
  // purpose: given setting String return the corresponding rules enum
  // param: OvervoteRule setting string from election config
  // returns: the OvervoteRule enum value for the input setting string
  private static Tabulator.OvervoteRule overvoteRuleForConfigSetting(String setting) {
    // rule: return value determined by input setting string
    Tabulator.OvervoteRule rule = Tabulator.OvervoteRule.RULE_UNKNOWN;

    switch (setting) {
      case "alwaysSkipToNextRank":
        rule = Tabulator.OvervoteRule.ALWAYS_SKIP_TO_NEXT_RANK;
        break;
      case "exhaustImmediately":
        rule = Tabulator.OvervoteRule.EXHAUST_IMMEDIATELY;
        break;
      case "exhaustIfAnyContinuing":
        rule = Tabulator.OvervoteRule.EXHAUST_IF_ANY_CONTINUING;
        break;
      case "ignoreIfAnyContinuing":
        rule = Tabulator.OvervoteRule.IGNORE_IF_ANY_CONTINUING;
        break;
      case "exhaustIfMultipleContinuing":
        rule = Tabulator.OvervoteRule.EXHAUST_IF_MULTIPLE_CONTINUING;
        break;
      case "ignoreIfMultipleContinuing":
        rule = Tabulator.OvervoteRule.IGNORE_IF_MULTIPLE_CONTINUING;
        break;
      default:
        Logger.log("Unrecognized overvote rule setting: %s", setting);
    }
    return rule;
  }

  // function: tieBreakModeForConfigSetting
  // purpose: given setting string return corresponding rule enum
  // param: TieBreakMode setting string read from election config
  // returns: TieBreakMode enum value for the input setting string
  private static Tabulator.TieBreakMode tieBreakModeForConfigSetting(String setting) {
    // mode: return value determined by input setting string
    Tabulator.TieBreakMode mode = Tabulator.TieBreakMode.MODE_UNKNOWN;
    switch (setting) {
      case "random":
        mode = Tabulator.TieBreakMode.RANDOM;
        break;
      case "interactive":
        mode = Tabulator.TieBreakMode.INTERACTIVE;
        break;
      case "previousRoundCountsThenRandom":
        mode = Tabulator.TieBreakMode.PREVIOUS_ROUND_COUNTS_THEN_RANDOM;
        break;
      case "previousRoundCountsThenInteractive":
        mode = Tabulator.TieBreakMode.PREVIOUS_ROUND_COUNTS_THEN_INTERACTIVE;
        break;
      default:
        Logger.log("Unrecognized tiebreaker mode rule setting: %s", setting);
    }
    return mode;
  }

  // function: validate
  // purpose: validate the correctness of the config data
  // returns false if there was a problem
  boolean validate() {
    // does this config meet our validation standards?
    boolean valid = true;

    if (this.getOvervoteRule() == Tabulator.OvervoteRule.RULE_UNKNOWN) {
      valid = false;
    } else if (this.getTiebreakMode() == Tabulator.TieBreakMode.MODE_UNKNOWN) {
      valid = false;
    } else if (getOvervoteLabel() != null &&
        getOvervoteRule() != Tabulator.OvervoteRule.EXHAUST_IMMEDIATELY &&
        getOvervoteRule() != Tabulator.OvervoteRule.ALWAYS_SKIP_TO_NEXT_RANK
    ) {
      valid = false;
    } else if (getMaxSkippedRanksAllowed() != null && getMaxSkippedRanksAllowed() < 0) {
      valid = false;
    } else if (getMaxRankingsAllowed() != null && getMaxRankingsAllowed() < 1) {
      valid = false;
    } else if (rawConfig.rules.batchElimination == null) {
      valid = false;
    }

    // if continueUntilTwoCandidatesRemain is selected
    // this must be a single-winner election
    if (this.continueUntilTwoCandidatesRemain() && this.getNumberOfWinners() > 1) {
      valid = false;
    }

    // if multi-seat is indicated we validate decimal count and rules style
    //
    if (this.getNumberOfWinners() > 1) {
      if (
          this.getDecimalPlacesForVoteArithmetic() < 0 ||
          this.getDecimalPlacesForVoteArithmetic() > 20
      ) {
        valid = false;
      }
      if (multiSeatTransferRule() == Tabulator.MultiSeatTransferRule.TRANSFER_RULE_UNKNOWN) {
        valid = false;
      }
    }
    return valid;
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
    return rawConfig.rules.decimalPlacesForVoteArithmetic == null ?
        4 :
        rawConfig.rules.decimalPlacesForVoteArithmetic;
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
  // returns: directory string
  String getOutputDirectory() {
    return rawConfig.outputDirectory;
  }

  // function: getAuditOutputFilename
  // purpose: getter for auditOutputFilename
  // returns: filename for audit output
  String getAuditOutputFilename() {
    return rawConfig.auditOutputFilename;
  }

  // function: getVisualizerOutputFilename
  // purpose: getter for visualizerOutputFilename
  // returns: filename for visualizer output
  String getVisualizerOutputFilename() {
    return rawConfig.visualizerOutputFilename;
  }

  // function: continueUntilTwoCandidatesRemain
  // purpose: getter for setting to keep tabulating beyond selecting winner till two candidates remain
  // returns: whether to keep tabulating untill two candidates remain
  public boolean continueUntilTwoCandidatesRemain() {
    return rawConfig.rules.continueUntilTwoCandidatesRemain != null ?
        rawConfig.rules.continueUntilTwoCandidatesRemain :
        false;
  }

  // function: contestName
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
    return rawConfig.date;
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
  public int numDeclaredCandidates() {
    // num will contain the resulting number of candidates
    int num = candidateCodeList.size();
    if (getUndeclaredWriteInLabel()!= null &&
        candidateCodeList.contains(getUndeclaredWriteInLabel())) {
      num--;
    }
    return num;
  }

  // function: numCandidates
  // purpose: return number of candidates including UWIs as a candidate if they are in use
  // num will contain the resulting number of candidates
  public int numCandidates() {
    return candidateCodeList.size();
  }


  // function: getOvervoteRule
  // purpose: return overvote rule enum to use
  // returns: overvote rule to use for this config
  Tabulator.OvervoteRule getOvervoteRule() {
    // by default we exhaust immediately
    return rawConfig.rules.overvoteRule == null ?
        Tabulator.OvervoteRule.EXHAUST_IMMEDIATELY :
        ElectionConfig.overvoteRuleForConfigSetting(rawConfig.rules.overvoteRule);
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
    return rawConfig.rules.tiebreakMode == null ?
        Tabulator.TieBreakMode.RANDOM :
        ElectionConfig.tieBreakModeForConfigSetting(rawConfig.rules.tiebreakMode);
  }

  // function: isTreatBlankAsUWIEnabled
  // purpose: getter for treatBlankAsUWI rule
  // returns: return true if we are to treat blank cell as UWI
  boolean isTreatBlankAsUWIEnabled() {
    // by default we do not treat blank as UWI
    return rawConfig.rules.treatBlankAsUwi == null ?
        false :
        rawConfig.rules.treatBlankAsUwi;
  }

  // function: getCandidateCodeList
  // purpose: return list of candidate codes for this config
  // returns: return list of candidate codes for this config
  List<String> getCandidateCodeList() {
    return candidateCodeList;
  }

  // function: getNameForCandidateID
  // purpose: lookup full candidate name given a candidate ID
  // param: candidateID the ID of the candidate whose name we want to lookup
  // returns: the full name for the given candidateID
  String getNameForCandidateID(String candidateID) {
    return candidateCodeToNameMap.get(candidateID);
  }

  // function: processCandidateData
  // purpose: builds map of candidate ID to candidate name
  private void processCandidateData() {
    candidateCodeList = new ArrayList<>();
    candidateCodeToNameMap = new HashMap<>();
    // candidate is used to index through all candidates for this election
    for (RawElectionConfig.Candidate candidate : rawConfig.candidates) {
      if (candidate.code != null) {
        candidateCodeList.add(candidate.code);
        candidateCodeToNameMap.put(candidate.code, candidate.name);
      } else {
        candidateCodeList.add(candidate.name);
        candidateCodeToNameMap.put(candidate.name, candidate.name);
      }
    }
  }

}
