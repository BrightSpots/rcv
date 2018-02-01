package com.rcv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

// container for an election configuration

@JsonIgnoreProperties(ignoreUnknown = true)
public class RawElectionConfig {

  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public class ElectionRules {
    public String description;
    public Boolean batch_elimination;
    public String overvote_rule;
    public Integer max_skipped_ranks_allowed;
    public Integer minimum_vote_threshold;
    public String undeclared_write_in_label;
    public String overvote_label;
    public String undervote_label;
    public String tiebreak_mode;
    public Boolean treat_blank_as_uwi;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CVRSource {
    public String provider;
    public String file_path;
    public Integer first_vote_column_index;
  }

  public String audit_output;
  public String visualizer_output;
  public String contest_name;
  public String jurisdiction;
  public String office;
  public String date;
  public Integer max_rankings_allowed;
  public ElectionRules rules;

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Candidate {
    public String name;
    public String code;
  }

  public List<Candidate> candidates;
  public List<CVRSource> cvr_file_sources;

  RawElectionConfig() {}
}
