package com.rcv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

// container for an election configuration

@JsonIgnoreProperties(ignoreUnknown = true)
public class ElectionConfig {

  public String audit_output;
  public String visualizer_output;
  public String contest_name;

  public Map<String, ?> rules;
  public List<String> candidates;
  public List<String> sources;

  ElectionConfig() {}

}
