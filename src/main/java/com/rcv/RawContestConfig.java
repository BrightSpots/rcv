/*
 * Ranked Choice Voting Universal Tabulator
 * Copyright (c) 2018 Jonathan Moldover, Louis Eisenberg, and Hylton Edingfield
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
 * RawContestConfig defines the data model used for a contest configuration. It is used
 * by JsonParser to map JSON configuration files into Java objects. We use Jackson JSON parser with \
 * annotations below to facilitate parsing (see JsonParser.java).
 */

package com.rcv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@SuppressWarnings("WeakerAccess")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RawContestConfig {

  // output settings object
  public OutputSettings outputSettings;
  // list of all cast vote record source files
  public List<CVRSource> cvrFileSources;
  // list of all Candidates
  public List<Candidate> candidates;
  // rules object
  public ContestRules rules;

  // function: RawContestConfig
  // purpose: create a new RawContestConfig object
  // returns: the newly created RawContestConfig object
  RawContestConfig() {
  }

  // OutputSettings: encapsulates the output settings
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class OutputSettings {
    // contest name
    public String contestName;
    // directory for output files
    public String outputDirectory;
    // contest date
    public String contestDate;
    // contest jurisdiction
    public String contestJurisdiction;
    // contest office
    public String contestOffice;
    // should we report round-by-round results by precinct also?
    public boolean tabulateByPrecinct;
  }

  // CVRSource: encapsulates a source cast vote record file
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class CVRSource {
    // path to the file on disk
    private String filePath;
    // column where rankings data begins
    private Integer firstVoteColumnIndex;
    // column containing CVR ID (if any)
    private Integer idColumnIndex;
    // column containing precinct (if any)
    private Integer precinctColumnIndex;
    // provider for this source e.g. "ES&S"
    private String provider;

    public String getFilePath() {
      return filePath;
    }

    public void setFilePath(String filePath) {
      this.filePath = filePath;
    }

    public Integer getFirstVoteColumnIndex() {
      return firstVoteColumnIndex;
    }

    public void setFirstVoteColumnIndex(Integer firstVoteColumnIndex) {
      this.firstVoteColumnIndex = firstVoteColumnIndex;
    }

    public Integer getIdColumnIndex() {
      return idColumnIndex;
    }

    public void setIdColumnIndex(Integer idColumnIndex) {
      this.idColumnIndex = idColumnIndex;
    }

    public Integer getPrecinctColumnIndex() {
      return precinctColumnIndex;
    }

    public void setPrecinctColumnIndex(Integer precinctColumnIndex) {
      this.precinctColumnIndex = precinctColumnIndex;
    }

    public String getProvider() {
      return provider;
    }

    public void setProvider(String provider) {
      this.provider = provider;
    }
  }

  // Candidate: contains a full candidate name and optionally a candidate ID
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonInclude(JsonInclude.Include.NON_NULL)
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

  // ContestRules: encapsulates the set of rules required to perform contest tabulation
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class ContestRules {
    // tiebreak mode to use
    public String tiebreakMode;
    // which overvote rule to use
    public String overvoteRule;
    // max rankings allowed
    public Integer maxRankingsAllowed;
    // max number of skipped rankings allowed
    public Integer maxSkippedRanksAllowed;
    // setting for number of winners
    public Integer numberOfWinners;
    // how far to round vote values when performing arithmetic
    public Integer decimalPlacesForVoteArithmetic;
    // minimum votes needed to continue
    public Integer minimumVoteThreshold;
    // are we using batch elimination?
    public boolean batchElimination;
    // keep tabulating beyond selecting winner until only two candidates remain
    // used to provide additional context for the strength of support for the winner
    // only valid for single-winner contests
    public boolean continueUntilTwoCandidatesRemain;
    // should we exhaust a ballot when we hit a duplicate candidate while traversing its rankings?
    public boolean exhaustOnDuplicateCandidate;
    // shall we treat blank cells as UWIs?
    public boolean treatBlankAsUndeclaredWriteIn;
    // overvote label
    public String overvoteLabel;
    // undervote label
    public String undervoteLabel;
    // UWI label
    public String undeclaredWriteInLabel;
    // human description of this rules set
    public String rulesDescription;
  }
}
