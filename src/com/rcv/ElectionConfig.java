package com.rcv;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class ElectionConfig {
  RawElectionConfig rawConfig;

  private ArrayList<String> mCandidateCodeList = null;
  private Map<String, String> mCandidateCodeToNameMap = null;

  ElectionConfig(RawElectionConfig rawConfig) {
    this.rawConfig = rawConfig;
    this.processCandidateData();
  }

  public String auditOutput() {
    return rawConfig.audit_output;
  }

  public String visualizerOutput() {
    return rawConfig.visualizer_output;
  }

  public String contestName() {
    return rawConfig.contest_name;
  }

  public String jurisdiction() {
    return rawConfig.jurisdiction;
  }

  public String office() {
    return rawConfig.office;
  }

  public String electionDate() {
    return rawConfig.date;
  }

  public Integer maxRankingsAllowed() {
    return rawConfig.max_rankings_allowed;
  }

  // rules

  public String description() {
    return rawConfig.rules.description;
  }

  public boolean batchElimination() {
    return rawConfig.rules.batch_elimination == null ? false : rawConfig.rules.batch_elimination;
  }

  // returns the OvervoteRule enum value for the input setting string
  // param setting: string read from election config corresponding to the OvervoteRule enum value
  // we will use for handling overvotes
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

  // returns the TieBreakMode enum value for the input setting string
  // param setting: string read from election config corresponding to the TieBreakMode enum value
  // we will use for tiebreaks
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


  public Tabulator.OvervoteRule overvoteRule() {
    return rawConfig.rules.overvote_rule == null ?
      Tabulator.OvervoteRule.EXHAUST_IMMEDIATELY :
      ElectionConfig.overvoteRuleForConfigSetting(rawConfig.rules.overvote_rule);
  }

  public Integer minimumVoteThreshold() {
    return rawConfig.rules.minimum_vote_threshold;
  }

  public Integer maxSkippedRanksAllowed() {
    return rawConfig.rules.max_skipped_ranks_allowed;
  }

  public String undeclaredWriteInLabel() {
    return rawConfig.rules.undeclared_write_in_label;
  }

  public String overvoteLabel() {
    return rawConfig.rules.overvote_label;
  }

  public String undervoteLabel() {
    return rawConfig.rules.undervote_label;
  }

  public Tabulator.TieBreakMode tiebreakMode() {
    return rawConfig.rules.tiebreak_mode == null ?
      Tabulator.TieBreakMode.RANDOM :
      ElectionConfig.tieBreakModeForConfigSetting(rawConfig.rules.tiebreak_mode);
  }

  public boolean treatBlankAsUWI() {
    return rawConfig.rules.treat_blank_as_uwi == null ?
      false :
      rawConfig.rules.treat_blank_as_uwi;
  }

  // returns list of strings for use in matching CVR cells to candidates
  // looks for candidate code first and fallback to candidate name to support both variants
  public List<String> getCandidateCodeList() {
    return mCandidateCodeList;
  }

  // returns String for display in the visualizer
  public String getNameForCandidateID(String candidateID) {
    return mCandidateCodeToNameMap.get(candidateID);
  }

  // generate list of matching IDs for CVR parsing
  // and mapping of IDs back to names for visualizer output
  private void processCandidateData() {
    mCandidateCodeList = new ArrayList<>();
    mCandidateCodeToNameMap = new HashMap<>();
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
