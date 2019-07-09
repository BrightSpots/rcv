/*
 * Universal RCV Tabulator
 * Copyright (c) 2017-2019 Bright Spots Developers.
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
 */

/*
 * Purpose:
 * RawContestConfig defines the data model used for a contest configuration. It is used
 * by JsonParser to map JSON configuration files into Java objects.
 * We use Jackson JSON parser with annotations below to facilitate parsing.
 */

package network.brightspots.rcv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@SuppressWarnings("WeakerAccess")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RawContestConfig {

  // tabulator version that created this file
  public String tabulatorVersion;
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
    public boolean generateCdfJson;
  }

  // CVRSource: encapsulates a source cast vote record file
  @SuppressWarnings("unused")
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class CVRSource {
    // path to the file on disk
    private String filePath;
    // 1-indexed column where rankings data begins
    private String firstVoteColumnIndex;
    // 1-indexed row where first CVR appears
    private String firstVoteRowIndex;
    // 1-indexed column containing CVR ID (if any)
    private String idColumnIndex;
    // 1-indexed column containing precinct (if any)
    private String precinctColumnIndex;
    // provider for this source e.g. "ES&S"
    private String provider;

    public CVRSource() {
    }

    public CVRSource(String filePath, String firstVoteColumnIndex, String firstVoteRowIndex,
        String idColumnIndex, String precinctColumnIndex, String provider) {
      this.filePath = filePath;
      this.firstVoteColumnIndex = firstVoteColumnIndex;
      this.firstVoteRowIndex = firstVoteRowIndex;
      this.idColumnIndex = idColumnIndex;
      this.precinctColumnIndex = precinctColumnIndex;
      this.provider = provider;
    }

    // get underlying file path string
    public String getFilePath() {
      return filePath;
    }

    // set underlying raw path string
    public void setFilePath(String filePath) {
      this.filePath = filePath;
    }

    public String getFirstVoteColumnIndex() {
      return firstVoteColumnIndex;
    }

    public void setFirstVoteColumnIndex(String firstVoteColumnIndex) {
      this.firstVoteColumnIndex = firstVoteColumnIndex;
    }

    public String getFirstVoteRowIndex() {
      return firstVoteRowIndex;
    }

    public void setFirstVoteRowIndex(String firstVoteRowIndex) {
      this.firstVoteRowIndex = firstVoteRowIndex;
    }

    public String getIdColumnIndex() {
      return idColumnIndex;
    }

    public void setIdColumnIndex(String idColumnIndex) {
      this.idColumnIndex = idColumnIndex;
    }

    public String getPrecinctColumnIndex() {
      return precinctColumnIndex;
    }

    public void setPrecinctColumnIndex(String precinctColumnIndex) {
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

    public Candidate() {
    }

    public Candidate(String name, String code, boolean excluded) {
      this.name = name;
      this.code = code;
      this.excluded = excluded;
    }

    // full candidate name
    private String name;
    // candidate ID
    private String code;
    private boolean excluded;

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

    public boolean isExcluded() {
      return excluded;
    }

    public void setExcluded(boolean excluded) {
      this.excluded = excluded;
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
    public String winnerElectionMode;
    public String randomSeed;
    // setting for number of winners
    public String numberOfWinners;
    // how far to round vote values when performing arithmetic
    public String decimalPlacesForVoteArithmetic;
    // minimum votes needed to continue
    public String minimumVoteThreshold;
    // max number of skipped rankings allowed
    public String maxSkippedRanksAllowed;
    // max rankings allowed
    public String maxRankingsAllowed;
    public boolean nonIntegerWinningThreshold;
    public boolean hareQuota;
    // are we using batch elimination?
    public boolean batchElimination;
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
