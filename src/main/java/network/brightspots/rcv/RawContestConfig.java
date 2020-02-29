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

  public String tabulatorVersion;
  public OutputSettings outputSettings;
  public List<CvrSource> cvrFileSources;
  public List<Candidate> candidates;
  public ContestRules rules;

  RawContestConfig() {
  }

  // OutputSettings: encapsulates the output settings
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class OutputSettings {

    public String contestName;
    public String outputDirectory;
    public String contestDate;
    public String contestJurisdiction;
    public String contestOffice;
    public boolean tabulateByPrecinct;
    public boolean generateCdfJson;
  }

  // CvrSource: encapsulates a source cast vote record file
  // all indexes are 1-based
  @SuppressWarnings("unused")
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class CvrSource {

    private String filePath;
    private String firstVoteColumnIndex;
    private String firstVoteRowIndex;
    private String idColumnIndex;
    private String precinctColumnIndex;
    private String provider;

    CvrSource() {
    }

    CvrSource(
        String filePath,
        String firstVoteColumnIndex,
        String firstVoteRowIndex,
        String idColumnIndex,
        String precinctColumnIndex,
        String provider) {
      this.filePath = filePath;
      this.firstVoteColumnIndex = firstVoteColumnIndex;
      this.firstVoteRowIndex = firstVoteRowIndex;
      this.idColumnIndex = idColumnIndex;
      this.precinctColumnIndex = precinctColumnIndex;
      this.provider = provider;
    }

    public String getFilePath() {
      return filePath;
    }

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

  // Candidate encapsulates data for each candidate in a contest
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class Candidate {

    private String name;
    private String code;
    private boolean excluded;
    private Integer contestId;

    Candidate() {
    }

    Candidate(String name, String code, boolean excluded) {
      this.name = name;
      this.code = code;
      this.excluded = excluded;
    }

    Candidate(String name, String code, boolean excluded, Integer contestId) {
      this.name = name;
      this.code = code;
      this.excluded = excluded;
      this.contestId = contestId;
    }

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
    public Integer getContestId() {
      return contestId;
    }

  }

  // ContestRules: encapsulates the set of rules required to perform contest tabulation
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class ContestRules {

    public String tiebreakMode;
    public String overvoteRule;
    public String winnerElectionMode;
    public String randomSeed;
    public String numberOfWinners;
    public String decimalPlacesForVoteArithmetic;
    public String minimumVoteThreshold;
    public String maxSkippedRanksAllowed;
    public String maxRankingsAllowed;
    public boolean nonIntegerWinningThreshold;
    public boolean hareQuota;
    public boolean batchElimination;
    public boolean exhaustOnDuplicateCandidate;
    public boolean treatBlankAsUndeclaredWriteIn;
    public String overvoteLabel;
    public String undervoteLabel;
    public String undeclaredWriteInLabel;
    public String rulesDescription;
  }
}
