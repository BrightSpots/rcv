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
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import static java.math.RoundingMode.HALF_EVEN;

public class ElectionConfig {
  // underlying rawConfig object data
  RawElectionConfig rawConfig;
  // list of all declared candidate codes
  private ArrayList<String> candidateCodeList;
  // mapping from candidate code to full name
  private Map<String, String> candidateCodeToNameMap;
  // MathContext to be shared by all vote calculations
  private MathContext contextForVoteArithmetic;
  // minimum vote threshold if one is specified
  private BigDecimal minimumVoteThreshold;

  // function: ElectionConfig
  // purpose: create a new ElectionConfig object
  // param: rawConfig underlying rawConfig object this object wraps
  ElectionConfig(RawElectionConfig rawConfig) {
    this.rawConfig = rawConfig;
    this.processCandidateData();
  }

  // function: validate
  // purpose: validate the correctness of the config data
  // returns false if there was a problem
  public boolean validate() {
    // does this config meet our validation standards?
    boolean valid = true;

    if (this.overvoteRule() == Tabulator.OvervoteRule.RULE_UNKNOWN) {
      valid = false;
    } else if (this.tiebreakMode() == Tabulator.TieBreakMode.MODE_UNKNOWN) {
      valid = false;
    } else if (
      overvoteLabel() != null &&
      overvoteRule() != Tabulator.OvervoteRule.EXHAUST_IMMEDIATELY &&
      overvoteRule() != Tabulator.OvervoteRule.ALWAYS_SKIP_TO_NEXT_RANK
    ) {
      valid = false;
    } else if (maxSkippedRanksAllowed() == null || maxSkippedRanksAllowed() < 0) {
      valid = false;
    } else if (maxRankingsAllowed() == null || maxRankingsAllowed() < 1) {
      valid = false;
    } else if (rawConfig.rules.batchElimination == null) {
      valid = false;
    }

    // if multi-seat is indicated we validate decimal count and rules style
    //
    if (this.numberOfWinners() > 1) {
      if (this.decimalPlacesForVoteArithmetic() < 0 || this.decimalPlacesForVoteArithmetic() > 1000) {
        valid = false;
      }
      if (multiSeatTransferRule() == Tabulator.MultiSeatTransferRule.TRANSFER_RULE_UNKNOWN) {
        valid = false;
      }
    }
    return valid;
  }

  // function: overvoteRuleForConfigSetting
  // purpose: given setting String return the corresponding rules enum
  // param: OvervoteRule setting string from election config
  // returns: the OvervoteRule enum value for the input setting string
  public static Tabulator.MultiSeatTransferRule multiSeatTransferRuleForConfigSetting(String setting) {
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


  // function: numberWinners
  // purpose: how many winners for this election
  // returns from settings config or 1 of no setting is specified
  public Integer numberOfWinners() {
    return rawConfig.rules.numberOfWinners == null ? 1 : rawConfig.rules.numberOfWinners;
  }

  // function: mathContext
  // purpose: getter for mathContext for vote tally arithmetic
  // return: context for doing vote tally arithmetic to be used with BigDecimal
  public MathContext mathContext() {
    if (contextForVoteArithmetic == null) {
      contextForVoteArithmetic = new MathContext(decimalPlacesForVoteArithmetic().intValue(), HALF_EVEN);
    }
    return contextForVoteArithmetic;
  }

  // function: decimalPlacesForVoteArithmetic
  // purpose: how many places to round votes to after performing fractional vote transfers
  // returns: number of places to round to or 0 if no setting is specified
  public Integer decimalPlacesForVoteArithmetic() {
    // w default to using 4 places for fractional transfer vote arithmetic
    return rawConfig.rules.decimalPlacesForVoteArithmetic == null ? 4 :
        rawConfig.rules.decimalPlacesForVoteArithmetic;
  }

  // function: multiSeatTransferRule
  // purpose: which surplus transfer rule to use in multi-seat elections
  // returns: enum indicating which transfer rule to use
  public Tabulator.MultiSeatTransferRule multiSeatTransferRule() {
    return multiSeatTransferRuleForConfigSetting( rawConfig.rules.multiSeatTransferRule );
  }

  // function: auditOutput
  // purpose: getter for auditOutput
  // returns: path to audit output file
  public String auditOutput() {
    return rawConfig.auditOutput;
  }

  // function: visualizerOutput
  // purpose: getter for visualizerOutput
  // returns: path to write visualizer output file
  public String visualizerOutput() {
    return rawConfig.visualizerOutput;
  }

  // function: contestName
  // purpose: getter for contestName
  // returns: contest name
  public String contestName() {
    return rawConfig.contestName;
  }

  // function: jurisdiction
  // purpose: getter for jurisdiction
  // returns: jurisdiction name
  public String jurisdiction() {
    return rawConfig.jurisdiction;
  }

  // function: office
  // purpose: getter for office
  // returns: office name
  public String office() {
    return rawConfig.office;
  }

  // function: electionDate
  // purpose: getter for electionDate
  // returns: election date
  public String electionDate() {
    return rawConfig.date;
  }

  // function: maxRankingsAllowed
  // purpose: getter for maxRankingsAllowed
  // returns: max rankings allowed
  public Integer maxRankingsAllowed() {
    return rawConfig.rules.maxRankingsAllowed;
  }

  // function: description
  // purpose: getter for description
  // returns: description
  public String description() {
    return rawConfig.rules.description;
  }

  // function: batchElimination
  // purpose: getter for batchElimination
  // returns: true if we should use batch elimination
  public boolean batchElimination() {
    return rawConfig.rules.batchElimination;
  }

  // function: overvoteRuleForConfigSetting
  // purpose: given setting String return the corresponding rules enum
  // param: OvervoteRule setting string from election config
  // returns: the OvervoteRule enum value for the input setting string
  static Tabulator.OvervoteRule overvoteRuleForConfigSetting(String setting) {
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
  static Tabulator.TieBreakMode tieBreakModeForConfigSetting(String setting) {
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

  // function: numCandidates
  // purpose: calculate the number of declared candidates from the election configuration
  // returns: the number of declared candidates from the election configuration
  public int numCandidates() {
    // num will contain the resulting number of candidates
    int num = candidateCodeList.size();
    if (undeclaredWriteInLabel()!= null &&
        candidateCodeList.contains(undeclaredWriteInLabel())) {
      num--;
    }
    return num;
  }

  // function: overvoteRule
  // purpose: return overvote rule enum to use
  // returns: overvote rule to use for this config
  public Tabulator.OvervoteRule overvoteRule() {
    // by default we exhaust immediately
    return rawConfig.rules.overvoteRule == null ?
      Tabulator.OvervoteRule.EXHAUST_IMMEDIATELY :
      ElectionConfig.overvoteRuleForConfigSetting(rawConfig.rules.overvoteRule);
  }

  // function: minimumVoteThreshold
  // purpose: getter for minimumVoteThreshold rule
  // returns: minimum vote threshold to use for this config
  public BigDecimal minimumVoteThreshold() {
    if (minimumVoteThreshold == null) {
      if (rawConfig.rules.minimumVoteThreshold == null) {
        minimumVoteThreshold = BigDecimal.ZERO;
      } else {
        minimumVoteThreshold = new BigDecimal(rawConfig.rules.minimumVoteThreshold.intValue());
      }
    }
    return minimumVoteThreshold;
  }

  // function: maxSkippedRanksAllowed
  // purpose: getter for maxSkippedRanksAllowed rule
  // returns: max skipped ranks allowed in this config
  public Integer maxSkippedRanksAllowed() {
    return rawConfig.rules.maxSkippedRanksAllowed;
  }

  // function: undeclaredWriteInLabel
  // purpose: getter for UWI label
  // returns: overvote rule for this config
  public String undeclaredWriteInLabel() {
    return rawConfig.rules.undeclaredWriteInLabel;
  }

  // function: overvoteLabel
  // purpose: getter for overvote label rule
  // returns: overvote label for this config
  public String overvoteLabel() {
    return rawConfig.rules.overvoteLabel;
  }

  // function: undervoteLabel
  // purpose: getter for undervote label
  // returns: undervote label for this config
  public String undervoteLabel() {
    return rawConfig.rules.undervoteLabel;
  }

  // function: tiebreakMode
  // purpose: return tiebreak mode to use
  // returns: tiebreak mode to use for this config
  public Tabulator.TieBreakMode tiebreakMode() {
    // by default we use random tiebreak
    return rawConfig.rules.tiebreakMode == null ?
      Tabulator.TieBreakMode.RANDOM :
      ElectionConfig.tieBreakModeForConfigSetting(rawConfig.rules.tiebreakMode);
  }

  // function: treatBlankAsUWI
  // purpose: getter for treatBlankAsUWI rule
  // returns: return true if we are to treat blank cell as UWI
  public boolean treatBlankAsUWI() {
    // by default we do not treat blank as UWI
    return rawConfig.rules.treatBlankAsUwi == null ?
      false :
      rawConfig.rules.treatBlankAsUwi;
  }

  // function: getCandidateCodeList
  // purpose: return list of candidate codes for this config
  // returns: return list of candidate codes for this config
  public List<String> getCandidateCodeList() {
    return candidateCodeList;
  }

  // function: getNameForCandidateID
  // purpose: lookup full candidate name given a candidate ID
  // param: candidateID the ID of the candidate whose name we want to lookup
  // returns: the full name for the given candidateID
  public String getNameForCandidateID(String candidateID) {
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
