/**
 * Created by Jonathan Moldover on 7/8/17
 * Copyright 2018 Bright Spots
 * Purpose: Wrapper for RawElectionConfig object
 * This class adds logic for looking up rule enum names
 * candidate names and various configuration utilities
 * cast vote record objects.
 * Version: 1.0
 */
package com.rcv;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class ElectionConfig {
  // underlying rawConfig object data
  RawElectionConfig rawConfig;
  // list of all declared candidate codes
  private ArrayList<String> mCandidateCodeList = null;
  // mapping from candidate code to full name
  private Map<String, String> mCandidateCodeToNameMap = null;

  // function: ElectionConfig
  // purpose: create a new ElectionConfig object
  // param: rawConfig underlying rawConfig object this object wraps
  ElectionConfig(RawElectionConfig rawConfig) {
    this.rawConfig = rawConfig;
    this.processCandidateData();
  }

  // function: auditOutput
  // purpose: getter for auditOutput
  // returns: path to write audit output file
  public String auditOutput() {
    return rawConfig.audit_output;
  }

  // function: visualizerOutput
  // purpose: getter for visualizerOutput
  // returns: path to write visualizer output file
  public String visualizerOutput() {
    return rawConfig.visualizer_output;
  }

  // function: contestName
  // purpose: getter for contestName
  // returns: contest name string
  public String contestName() {
    return rawConfig.contest_name;
  }

  // function: jurisdiction
  // purpose: getter for jurisdiction
  // returns: jurisdiction name string
  public String jurisdiction() {
    return rawConfig.jurisdiction;
  }

  // function: office
  // purpose: getter for office
  // returns: office name string
  public String office() {
    return rawConfig.office;
  }

  // function: electionDate
  // purpose: getter for electionDate
  // returns: election date string
  public String electionDate() {
    return rawConfig.date;
  }

  // function: maxRankingsAllowed
  // purpose: getter for maxRankingsAllowed
  // returns: max rankings allowed
  public Integer maxRankingsAllowed() {
    return rawConfig.max_rankings_allowed;
  }

  // function: description
  // purpose: getter for description
  // returns: description of this rules configuration
  public String description() {
    return rawConfig.rules.description;
  }

  // function: batchElimination
  // purpose: getter for batchElimination
  // returns: true if we should use batch elimination
  public boolean batchElimination() {
    return rawConfig.rules.batch_elimination == null ? false : rawConfig.rules.batch_elimination;
  }

  // function: overvoteRuleForConfigSetting
  // purpose: given setting String return the corresponding rules enum
  // param: setting string read from election config corresponding to the OvervoteRule enum value
  // we will use for handling overvotes
  // returns: the OvervoteRule enum value for the input setting string
  static Tabulator.OvervoteRule overvoteRuleForConfigSetting(String setting) {
    // rule: return value determined by input setting string
    Tabulator.OvervoteRule rule = Tabulator.OvervoteRule.RULE_UNKNOWN;
    switch (setting) {
      case "always_skip_to_next_rank":
        rule = Tabulator.OvervoteRule.ALWAYS_SKIP_TO_NEXT_RANK;
        break;
      case "exhaust_immediately":
        rule = Tabulator.OvervoteRule.EXHAUST_IMMEDIATELY;
        break;
      default:
        Logger.log("Unrecognized overvote rule setting:%s", setting);
        System.exit(1);
    }
    return rule;
  }

  // function: tieBreakModeForConfigSetting
  // purpose: given setting string return corresponding rule enum
  // param: setting string read from election config corresponding to the TieBreakMode enum value
  // we will use for tiebreaks
  // returns: TieBreakMode enum value for the input setting string
  static Tabulator.TieBreakMode tieBreakModeForConfigSetting(String setting) {
    // mode: will contain the return value determined by input setting string
    Tabulator.TieBreakMode mode = Tabulator.TieBreakMode.MODE_UNKNOWN;
    switch (setting) {
      case "random":
        mode = Tabulator.TieBreakMode.RANDOM;
        break;
      case "interactive":
        mode = Tabulator.TieBreakMode.INTERACTIVE;
        break;
      case "previous_round_counts_then_random":
        mode = Tabulator.TieBreakMode.PREVIOUS_ROUND_COUNTS_THEN_RANDOM;
        break;
      case "previous_round_counts_then_interactive":
        mode = Tabulator.TieBreakMode.PREVIOUS_ROUND_COUNTS_THEN_INTERACTIVE;
        break;
      default:
        Logger.log("Unrecognized tiebreaker mode rule setting: %s", setting);
        System.exit(1);
    }
    return mode;
  }

  // function: numCandidates
  // purpose: calculate the number of declared candidates from the election configuration
  // returns: the number of declared candidates from the election configuration
  public int numCandidates() {
    // num will contain the resulting number of candidates
    int num = mCandidateCodeList.size();
    if (undeclaredWriteInLabel()!= null &&
        mCandidateCodeList.contains(undeclaredWriteInLabel())) {
      num--;
    }
    return num;
  }

  // function: overvoteRule
  // purpose: return overvote rule to use
  // returns: overvote rule to use for this election
  public Tabulator.OvervoteRule overvoteRule() {
    // by default we exhaust immediately
    return rawConfig.rules.overvote_rule == null ?
      Tabulator.OvervoteRule.EXHAUST_IMMEDIATELY :
      ElectionConfig.overvoteRuleForConfigSetting(rawConfig.rules.overvote_rule);
  }

  // function: minimumVoteThreshold
  // purpose: return minimumVoteThreshold rule to use
  // returns: minimum vote threshold to use for this election
  public Integer minimumVoteThreshold() {
    return rawConfig.rules.minimum_vote_threshold;
  }

  // function: maxSkippedRanksAllowed
  // purpose: return maxSkippedRanksAllowed rule to use
  // returns: max skipped ranks allowed in this election
  public Integer maxSkippedRanksAllowed() {
    return rawConfig.rules.max_skipped_ranks_allowed;
  }

  // function: undeclaredWriteInLabel
  // purpose: return UWI label to use
  // returns: overvote rule to use for this election
  public String undeclaredWriteInLabel() {
    return rawConfig.rules.undeclared_write_in_label;
  }

  // function: overvoteLabel
  // purpose: return overvote label rule to use
  // returns: overvote label to use for this election
  public String overvoteLabel() {
    return rawConfig.rules.overvote_label;
  }

  // function: undervoteLabel
  // purpose: return undervote label to use
  // returns: undervote label to use for this election
  public String undervoteLabel() {
    return rawConfig.rules.undervote_label;
  }

  // function: tiebreakMode
  // purpose: return tiebreak mode to use
  // returns: tiebreak mode to use for this election
  public Tabulator.TieBreakMode tiebreakMode() {
    // by default we use random tiebreak
    return rawConfig.rules.tiebreak_mode == null ?
      Tabulator.TieBreakMode.RANDOM :
      ElectionConfig.tieBreakModeForConfigSetting(rawConfig.rules.tiebreak_mode);
  }

  // function: treatBlankAsUWI
  // purpose: return true if we are to treat blank cell as UWI
  // returns: return true if we are to treat blank cell as UWI
  public boolean treatBlankAsUWI() {
    // by default we do not treat blank as UWI
    return rawConfig.rules.treat_blank_as_uwi == null ?
      false :
      rawConfig.rules.treat_blank_as_uwi;
  }

  // function: getCandidateCodeList
  // purpose: return list of candidate codes to use for matching in this election
  // returns: return list of candidate codes to use for matching in this election
  public List<String> getCandidateCodeList() {
    return mCandidateCodeList;
  }

  // function: getNameForCandidateID
  // purpose: lookup full candidate name given a candidate ID
  // param: candidateID the ID of the candidate whose name we want to lookup
  // returns: the full name for the given candidateID
  public String getNameForCandidateID(String candidateID) {
    return mCandidateCodeToNameMap.get(candidateID);
  }

  // function: processCandidateData
  // purpose: builds map of candidate ID to candidate name
  private void processCandidateData() {
    mCandidateCodeList = new ArrayList<>();
    mCandidateCodeToNameMap = new HashMap<>();
    // candidate is used to index through all candidates for this election
    for (RawElectionConfig.Candidate candidate : rawConfig.candidates) {
      if(candidate.code != null) {
        mCandidateCodeList.add(candidate.code);
        mCandidateCodeToNameMap.put(candidate.code, candidate.name);
      } else {
        mCandidateCodeList.add(candidate.name);
        mCandidateCodeToNameMap.put(candidate.name, candidate.name);
      }
    }
  }

}
