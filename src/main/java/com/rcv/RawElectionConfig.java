/*
 * Ranked Choice Voting Universal Tabulator
 * Copyright (C) 2018 Jonathan Moldover, Louis Eisenberg, and Hylton Edingfield
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See
 * the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this
 * program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Purpose:
 * RawElectionConfig defines the data model used for an election configuration. It is used
 * by JsonParser to map JSON configuration files into Java objects. We use Jackson JSON parser with \
 * annotations below to facilitate parsing (see JsonParser.java).
 */

package com.rcv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

// TODO: add getters and setters for everything to address issue #73

@JsonIgnoreProperties(ignoreUnknown = true)
public class RawElectionConfig {

  // directory for output files
  public String outputDirectory;
  // contest name
  public String contestName;
  // contest jurisdiction
  public String contestJurisdiction;
  // contest office
  public String contestOffice;
  // contest date
  public String contestDate;
  // rules object
  public ElectionRules rules;
  // list of all Candidates
  public List<Candidate> candidates;
  // list of all cast vote record source files
  public List<CVRSource> cvrFileSources;
  // should we report round-by-round results by precinct also?
  public boolean tabulateByPrecinct;

  // function: RawElectionConfig
  // purpose: create a new RawElectionConfig object
  // returns: the newly created RawElectionConfig object
  RawElectionConfig() {
  }

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
    // are we using batch elimination?
    public boolean batchElimination;
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
    // setting for number of winners
    public Integer numberOfWinners;
    // how far to round vote values when performing arithmetic
    public Integer decimalPlacesForVoteArithmetic;
    // which transfer rule to use on surplus votes in multi-seat elections
    public String multiSeatTransferRule;
    // keep tabulating beyond selecting winner until only two candidates remain
    // used to provide additional context for the strength of support for the winner
    // only valid for single-winner contests
    public boolean continueUntilTwoCandidatesRemain;
    // should we exhaust a ballot when we hit a duplicate candidate while traversing its rankings?
    public boolean exhaustOnDuplicateCandidate;
    // shall we treat blank cells as UWIs?
    public boolean treatBlankAsUndeclaredWriteIn;
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
    // column containing CVR ID (if any)
    public Integer idColumnIndex;
    // column containing precinct (if any)
    public Integer precinctColumnIndex;
  }

  // Candidate: contains a full candidate name and optionally a candidate ID
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Candidate {

    // full candidate name
    private String name;
    // candidate ID
    private String code;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getCode() {
      return code;
    }

    public void setCode(String code) {
      this.code = code;
    }
  }
}
