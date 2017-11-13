package com.rcv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

// container for an election configuration

@JsonIgnoreProperties(ignoreUnknown = true)
public class ElectionConfig {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public class ElectionRules {
    public String description;
    public Boolean batch_elimination;
    public String overvote_rule;
    public Integer max_skipped_ranks_allowed;
    public String undeclared_write_in_label;
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
  public Integer max_rankings_allowed;
  public ElectionRules rules;
  public List<String> candidates;
  public List<CVRSource> cvr_file_sources;

  ElectionConfig() {}

}
