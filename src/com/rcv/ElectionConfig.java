package com.rcv;

public class ElectionConfig {
  RawElectionConfig rawConfig;

  ElectionConfig(RawElectionConfig rawConfig) {
    this.rawConfig = rawConfig;
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

  public Tabulator.OvervoteRule overvoteRule() {
    return rawConfig.rules.overvote_rule == null ?
      Tabulator.OvervoteRule.EXHAUST_IMMEDIATELY :
      Tabulator.overvoteRuleForConfigSetting(rawConfig.rules.overvote_rule);
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
      Tabulator.tieBreakModeForConfigSetting(rawConfig.rules.tiebreak_mode);
  }

  public boolean treatBlankAsUWI() {
    return rawConfig.rules.treat_blank_as_uwi == null ?
      false :
      rawConfig.rules.treat_blank_as_uwi;
  }
}
