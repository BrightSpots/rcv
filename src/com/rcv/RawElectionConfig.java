/**
 * Created by Jonathan Moldover on 7/8/17
 * Copyright 2018 Bright Spots
 * Purpose: RawElectionConfig defines the data model used for an election configuration.
 * It is used by JsonParser to map json configuration files into Java objects.
 * We use jackson json parser with annotations below to facilitate parsing (see JsonParser.java)
 * Version: 1.0
 */
package com.rcv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RawElectionConfig {

  // ElectionRules: encapsulates the set of rules required to perform election tabulation
  // See Tabulator.java for more info on rules enums
  // Note: all jackson parsed variables names must match name exactly
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class ElectionRules {
    // human description of this rules set
    public String description;
    // are we using batch elimination
    public Boolean batch_elimination;
    // which overvote rule to use
    public String overvote_rule;
    // max number of skipped rankings allowed
    public Integer max_skipped_ranks_allowed;
    // minimum votes needed to continue
    public Integer minimum_vote_threshold;
    // UWI label
    public String undeclared_write_in_label;
    // overvote label
    public String overvote_label;
    // undervote label
    public String undervote_label;
    // tiebreak mode to use
    public String tiebreak_mode;
    // shall we treat blank cells as UWIs
    public Boolean treat_blank_as_uwi;
  }

  // CVRSource: encapsulates a source cast vote record file
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CVRSource {
    // provider for this source e.g. "ES&S"
    public String provider;
    // path to the file on disk
    public String file_path;
    // column where rankings data begins
    public Integer first_vote_column_index;
  }

  // Candidate: contains a full candidate name and optionally a candidate ID
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Candidate {
    // full candidate name
    public String name;
    // candidate ID
    public String code;
  }

  // location to write audit output
  public String audit_output;
  // location to write visualizer output
  public String visualizer_output;
  // contest name
  public String contest_name;
  // jurisdiction
  public String jurisdiction;
  // office
  public String office;
  // election date
  public String date;
  // max rankings allowed
  public Integer max_rankings_allowed;
  // rules object
  public ElectionRules rules;

  // list of all Candidates
  public List<Candidate> candidates;
  // list of all cast vote record source files
  public List<CVRSource> cvr_file_sources;

  // function: RawElectionConfig
  // purpose: create a new RawElectionConfig object
  // returns: the newly created RawElectionConfig object
  RawElectionConfig() {}

}
