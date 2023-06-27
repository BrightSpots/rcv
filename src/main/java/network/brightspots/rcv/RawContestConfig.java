/*
 * RCTab
 * Copyright (c) 2017-2023 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: RawContestConfig defines the data model used for a contest configuration.  It is used
 * by JsonParser with Jackson to map JSON configuration files to Java objects and back to disk.
 * Design: Simple container classes with Jackson annotations.
 * Conditions: During config load, save, or validation from the GUI, tabulation, and conversion.
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import static network.brightspots.rcv.Utils.isNullOrBlank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Contest configuration that can be serialized and deserialized.
 */
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

  /**
   * Output settings that can be serialized and deserialized.
   */
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

  /**
   * Source cast vote record file that can be serialized and deserialized.
   *
   * <p>All indexes are 1-based. </p>
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class CvrSource {

    private String filePath;
    private String contestId;
    private String firstVoteColumnIndex;
    private String firstVoteRowIndex;
    private String idColumnIndex;
    private String precinctColumnIndex;
    private String overvoteDelimiter;
    private String provider;
    private String overvoteLabel;
    private String undervoteLabel;
    private String undeclaredWriteInLabel;
    private boolean treatBlankAsUndeclaredWriteIn;

    CvrSource() {
    }

    CvrSource(
        String filePath,
        String firstVoteColumnIndex,
        String firstVoteRowIndex,
        String idColumnIndex,
        String precinctColumnIndex,
        String overvoteDelimiter,
        String provider,
        String contestId,
        String overvoteLabel,
        String undervoteLabel,
        String undeclaredWriteInLabel,
        boolean treatBlankAsUndeclaredWriteIn) {
      this.filePath = filePath;
      this.firstVoteColumnIndex = firstVoteColumnIndex;
      this.firstVoteRowIndex = firstVoteRowIndex;
      this.idColumnIndex = idColumnIndex;
      this.precinctColumnIndex = precinctColumnIndex;
      this.overvoteDelimiter = overvoteDelimiter;
      this.provider = provider;
      this.contestId = contestId;
      this.overvoteLabel = overvoteLabel;
      this.undervoteLabel = undervoteLabel;
      this.undeclaredWriteInLabel = undeclaredWriteInLabel;
      this.treatBlankAsUndeclaredWriteIn = treatBlankAsUndeclaredWriteIn;
    }

    public String getFilePath() {
      return filePath;
    }

    public void setFilePath(String filePath) {
      this.filePath = filePath;
    }

    public String getContestId() {
      return contestId;
    }

    public void setContestId(String contestId) {
      this.contestId = contestId;
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

    public String getOvervoteDelimiter() {
      return overvoteDelimiter;
    }

    public void setOvervoteDelimiter(String overvoteDelimiter) {
      this.overvoteDelimiter = overvoteDelimiter;
    }

    public String getProvider() {
      return provider;
    }

    public void setProvider(String provider) {
      this.provider = provider;
    }

    public String getOvervoteLabel() {
      return overvoteLabel;
    }

    public void setOvervoteLabel(String overvoteLabel) {
      this.overvoteLabel = overvoteLabel;
    }

    public String getUndervoteLabel() {
      return undervoteLabel;
    }

    public void setUndervoteLabel(String undervoteLabel) {
      this.undervoteLabel = undervoteLabel;
    }

    public String getUndeclaredWriteInLabel() {
      return undeclaredWriteInLabel;
    }

    public void setUndeclaredWriteInLabel(String undeclaredWriteInLabel) {
      this.undeclaredWriteInLabel = undeclaredWriteInLabel;
    }

    public boolean isTreatBlankAsUndeclaredWriteIn() {
      return treatBlankAsUndeclaredWriteIn;
    }

    public void setTreatBlankAsUndeclaredWriteIn(boolean treatBlankAsUndeclaredWriteIn) {
      this.treatBlankAsUndeclaredWriteIn = treatBlankAsUndeclaredWriteIn;
    }
  }

  /**
   * Contest candidate data that can be serialized and deserialized.
   */
  @JsonIgnoreProperties(ignoreUnknown = true, value = {"semicolonSeparatedAliases"})
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class Candidate {
    private String name;
    private boolean excluded;
    private List<String> aliases = new ArrayList<String>();

    Candidate() {
    }

    Candidate(String name, String newlineSeparatedAliases, boolean excluded) {
      this.name = name;
      this.excluded = excluded;

      if (newlineSeparatedAliases != null) {
        // Split by newline, and also trim whitespace
        this.aliases = Arrays.asList(newlineSeparatedAliases.split("\\s*\\r?\\n\\s*"));
      }
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public List<String> getAliases() {
      return List.copyOf(aliases);
    }

    public void setAliases(List<String> aliases) {
      this.aliases = new ArrayList<>(aliases);
    }

    public boolean isExcluded() {
      return excluded;
    }

    public void setExcluded(boolean excluded) {
      this.excluded = excluded;
    }


    // This is deprecated and replaced by aliases, but we need to leave it in place
    // here for the purpose of supporting automatic migration from older config versions.
    private void setCode(String code) {
      if (code != null && !code.isEmpty()) {
        this.aliases.add(code);
      }
    }

    /**
     * A stream of all aliases (which is guaranteed to be unique) and the candidate name
     * (which is not guaranteed to be unique, i.e. it may exist in the list twice)
     *
     * @return a stream containing the candidate name and all aliases, with no null elements
     */
    public Stream<String> createStreamOfNameAndAllAliases() {
      List<String> otherNames = new ArrayList<>();
      if (!isNullOrBlank(this.name)) {
        otherNames.add(this.name);
      }

      return Stream.concat(this.aliases.stream(), otherNames.stream());
    }

    /**
     * For display purposes, get a semicolon-separated list of aliases.
     *
     * @return a potentially-empty string
     */
    public String getSemicolonSeparatedAliases() {
      Stream<String> s = this.aliases.stream();
      return String.join("; ", s.toList());
    }

    public void setSemicolonSeparatedAliases(String semicolonSeparatedAliases) {
      this.aliases = Arrays.asList(semicolonSeparatedAliases.split("\\W*;\\W*"));
    }

    /**
     * Removes whitespace around all name and alias strings.
     */
    public void trimNameAndAllAliases() {
      if (name != null) {
        name = name.trim();
      }
      if (aliases != null) {
        aliases.replaceAll(s -> s.trim());
      }
    }
  }

  /**
   * Contest rules necessary for tabulation that can be serialized and deserialized.
   */
  @SuppressWarnings({"unused", "RedundantSuppression"})
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class ContestRules {

    public String tiebreakMode;
    public String overvoteRule;
    public String winnerElectionMode;
    public String randomSeed;
    public String numberOfWinners;
    public String multiSeatBottomsUpPercentageThreshold;
    public String decimalPlacesForVoteArithmetic;
    public String minimumVoteThreshold;
    public String maxSkippedRanksAllowed;
    public String maxRankingsAllowed;
    public boolean nonIntegerWinningThreshold;
    public boolean doesFirstRoundDetermineThreshold;
    public boolean hareQuota;
    public boolean batchElimination;
    public boolean continueUntilTwoCandidatesRemain;
    public String stopTabulationEarlyAfterRound;
    public boolean exhaustOnDuplicateCandidate;
    public String rulesDescription;

    // These are deprecated (moved to individual CVRs), but we need to leave them in place here for
    // the purpose of supporting automatic migration from older config versions.
    public boolean treatBlankAsUndeclaredWriteIn;
    public String overvoteLabel;
    public String undervoteLabel;
    public String undeclaredWriteInLabel;
  }
}
