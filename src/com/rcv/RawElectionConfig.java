/**
 * Created by Jonathan Moldover and Louis Eisenberg
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
    // max rankings allowed
    public Integer maxRankingsAllowed;
    // are we using batch elimination
    public Boolean batchElimination;
    // which overvote rule to use
    public String overvoteRule;
    // max number of skipped rankings allowed
    public Integer maxSkippedRanksAllowed;
    // minimum votes needed to continue
    public Integer minimumVoteThreshold;
    // UWI label
    public String undeclaredWriteInLabel;
    // overvote label
    public String overvoteLabel;
    // undervote label
    public String undervoteLabel;
    // tiebreak mode to use
    public String tiebreakMode;
    // shall we treat blank cells as UWIs
    public Boolean treatBlankAsUwi;
  }

  // CVRSource: encapsulates a source cast vote record file
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CVRSource {
    // provider for this source e.g. "ES&S"
    public String provider;
    // path to the file on disk
    public String filePath;
    // column where rankings data begins
    public Integer firstVoteColumnIndex;
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
  public String auditOutput;
  // location to write visualizer output
  public String visualizerOutput;
  // contest name
  public String contestName;
  // jurisdiction
  public String jurisdiction;
  // office
  public String office;
  // election date
  public String date;
  // rules object
  public ElectionRules rules;

  // list of all Candidates
  public List<Candidate> candidates;
  // list of all cast vote record source files
  public List<CVRSource> cvrFileSources;

  // function: RawElectionConfig
  // purpose: create a new RawElectionConfig object
  // returns: the newly created RawElectionConfig object
  RawElectionConfig() {}

}
