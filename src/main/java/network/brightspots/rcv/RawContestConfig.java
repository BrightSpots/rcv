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
import java.util.List;
import java.util.stream.Stream;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/** Contest configuration that can be serialized and deserialized. */
@SuppressWarnings("WeakerAccess")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RawContestConfig {

  public String tabulatorVersion;
  public OutputSettings outputSettings;
  public List<CvrSource> cvrFileSources;
  public List<Candidate> candidates;
  public ContestRules rules;

  RawContestConfig() {}

  /** Output settings that can be serialized and deserialized. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class OutputSettings {

    public String contestName;
    public String outputDirectory;
    public String contestDate;
    public String contestJurisdiction;
    public String contestOffice;
    public boolean tabulateByBatch;
    public boolean tabulateByPrecinct;
    public boolean generateCdfJson;
  }

  /**
   * Source cast vote record file that can be serialized and deserialized.
   *
   * <p>All indexes are 1-based.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class CvrSource {

    private final SimpleStringProperty filePath = new SimpleStringProperty();
    private final SimpleStringProperty contestId = new SimpleStringProperty();
    private final SimpleStringProperty firstVoteColumnIndex = new SimpleStringProperty();
    private final SimpleStringProperty firstVoteRowIndex = new SimpleStringProperty();
    private final SimpleStringProperty idColumnIndex = new SimpleStringProperty();
    private final SimpleStringProperty batchColumnIndex = new SimpleStringProperty();
    private final SimpleStringProperty precinctColumnIndex = new SimpleStringProperty();
    private final SimpleStringProperty overvoteDelimiter = new SimpleStringProperty();
    private final SimpleStringProperty provider = new SimpleStringProperty();
    private final SimpleStringProperty overvoteLabel = new SimpleStringProperty();
    private final SimpleStringProperty skippedRankLabel = new SimpleStringProperty();
    private final SimpleStringProperty undeclaredWriteInLabel = new SimpleStringProperty();
    private final SimpleBooleanProperty treatBlankAsUndeclaredWriteIn = new SimpleBooleanProperty();

    // Deprecated fields
    private String undervoteLabel;

    CvrSource() {}

    CvrSource(
        String filePath,
        String firstVoteColumnIndex,
        String firstVoteRowIndex,
        String idColumnIndex,
        String batchColumnIndex,
        String precinctColumnIndex,
        String overvoteDelimiter,
        String provider,
        String contestId,
        String overvoteLabel,
        String skippedRankLabel,
        String undeclaredWriteInLabel,
        boolean treatBlankAsUndeclaredWriteIn) {
      this.filePath.set(filePath);
      this.firstVoteColumnIndex.set(firstVoteColumnIndex);
      this.firstVoteRowIndex.set(firstVoteRowIndex);
      this.idColumnIndex.set(idColumnIndex);
      this.batchColumnIndex.set(batchColumnIndex);
      this.precinctColumnIndex.set(precinctColumnIndex);
      this.overvoteDelimiter.set(overvoteDelimiter);
      this.provider.set(provider);
      this.contestId.set(contestId);
      this.overvoteLabel.set(overvoteLabel);
      this.skippedRankLabel.set(skippedRankLabel);
      this.undeclaredWriteInLabel.set(undeclaredWriteInLabel);
      this.treatBlankAsUndeclaredWriteIn.set(treatBlankAsUndeclaredWriteIn);
    }

    public String getFilePath() {
      return filePath.get();
    }

    public void setFilePath(String filePath) {
      this.filePath.set(filePath);
    }

    public String getContestId() {
      return contestId.get();
    }

    public void setContestId(String contestId) {
      this.contestId.set(contestId);
    }

    public String getFirstVoteColumnIndex() {
      return firstVoteColumnIndex.get();
    }

    public void setFirstVoteColumnIndex(String firstVoteColumnIndex) {
      this.firstVoteColumnIndex.set(firstVoteColumnIndex);
    }

    public String getFirstVoteRowIndex() {
      return firstVoteRowIndex.get();
    }

    public void setFirstVoteRowIndex(String firstVoteRowIndex) {
      this.firstVoteRowIndex.set(firstVoteRowIndex);
    }

    public String getIdColumnIndex() {
      return idColumnIndex.get();
    }

    public void setIdColumnIndex(String idColumnIndex) {
      this.idColumnIndex.set(idColumnIndex);
    }

    public String getBatchColumnIndex() {
      return batchColumnIndex.get();
    }

    public void setBatchColumnIndex(String batchColumnIndex) {
      this.batchColumnIndex.set(batchColumnIndex);
    }

    public String getPrecinctColumnIndex() {
      return precinctColumnIndex.get();
    }

    public void setPrecinctColumnIndex(String precinctColumnIndex) {
      this.precinctColumnIndex.set(precinctColumnIndex);
    }

    public String getOvervoteDelimiter() {
      return overvoteDelimiter.get();
    }

    public void setOvervoteDelimiter(String overvoteDelimiter) {
      this.overvoteDelimiter.set(overvoteDelimiter);
    }

    public String getProvider() {
      return provider.get();
    }

    /** Set the provider by its GUI label. */
    public void setProvider(String providerString) {
      // First, try to get the provider by its public name
      ContestConfig.Provider provider = ContestConfig.Provider.getByInternalLabel(providerString);

      this.provider.set(provider.getInternalLabel());
    }

    public String getOvervoteLabel() {
      return overvoteLabel.get();
    }

    public void setOvervoteLabel(String overvoteLabel) {
      this.overvoteLabel.set(overvoteLabel);
    }

    public String getSkippedRankLabel() {
      return skippedRankLabel.get();
    }

    public void setSkippedRankLabel(String skippedRankLabel) {
      this.skippedRankLabel.set(skippedRankLabel);
    }

    public String getUndeclaredWriteInLabel() {
      return undeclaredWriteInLabel.get();
    }

    public void setUndeclaredWriteInLabel(String undeclaredWriteInLabel) {
      this.undeclaredWriteInLabel.set(undeclaredWriteInLabel);
    }

    public boolean getTreatBlankAsUndeclaredWriteIn() {
      return treatBlankAsUndeclaredWriteIn.get();
    }

    public void setTreatBlankAsUndeclaredWriteIn(Boolean treatBlankAsUndeclaredWriteIn) {
      this.treatBlankAsUndeclaredWriteIn.set(treatBlankAsUndeclaredWriteIn);
    }

    /**
     * The following properties might be marked as unused by an IDE, but are necessary to save edits
     * to a cell. See PropertyValueFactory.
     */
    public SimpleStringProperty filePathProperty() {
      return filePath;
    }

    public SimpleStringProperty contestIdProperty() {
      return contestId;
    }

    public SimpleStringProperty firstVoteColumnIndexProperty() {
      return firstVoteColumnIndex;
    }

    public SimpleStringProperty firstVoteRowIndexProperty() {
      return firstVoteRowIndex;
    }

    public SimpleStringProperty idColumnIndexProperty() {
      return idColumnIndex;
    }

    public SimpleStringProperty batchColumnIndexProperty() {
      return batchColumnIndex;
    }

    public SimpleStringProperty precinctColumnIndexProperty() {
      return precinctColumnIndex;
    }

    public SimpleStringProperty overvoteDelimiterProperty() {
      return overvoteDelimiter;
    }

    public SimpleStringProperty providerProperty() {
      return provider;
    }

    public SimpleStringProperty overvoteLabelProperty() {
      return overvoteLabel;
    }

    public SimpleStringProperty skippedRankLabelProperty() {
      return skippedRankLabel;
    }

    public SimpleStringProperty undeclaredWriteInLabelProperty() {
      return undeclaredWriteInLabel;
    }

    public SimpleBooleanProperty treatBlankAsUndeclaredWriteInProperty() {
      return treatBlankAsUndeclaredWriteIn;
    }

    // Deprecated fields
    public String getUndervoteLabel() {
      return undervoteLabel;
    }
  }

  /** Contest candidate data that can be serialized and deserialized. */
  @JsonIgnoreProperties(
      ignoreUnknown = true,
      value = {"observableAliases"})
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class Candidate {
    private final SimpleStringProperty name = new SimpleStringProperty();
    private final SimpleBooleanProperty excluded = new SimpleBooleanProperty();
    // The actual list of aliases, observable by the UI
    private final ObservableList<String> observableAliases = FXCollections.observableArrayList();
    // A property that wraps the observable list, so that it can be serialized
    private final SimpleListProperty<String> aliases = new SimpleListProperty<>(observableAliases);

    Candidate() {}

    Candidate(String name) {
      this(name, null, false);
    }

    Candidate(String name, String newlineSeparatedAliases) {
      this(name, newlineSeparatedAliases, false);
    }

    Candidate(String name, String newlineSeparatedAliases, boolean excluded) {
      this.name.setValue(name);
      this.excluded.setValue(excluded);

      if (newlineSeparatedAliases != null && !newlineSeparatedAliases.isEmpty()) {
        // Split by newline, and also trim whitespace
        this.aliases.setAll(Utils.splitByNewline(newlineSeparatedAliases));
      }
    }

    /** Making debugging easier to provide all info in one line. */
    public String toString() {
      String aliases = this.getAliases().size() == 0 ? "[]" : String.join(", ", this.getAliases());
      String name = this.getName() == null ? null : "\"" + this.getName() + "\"";
      return "Name: " + name + " Aliases: " + aliases;
    }

    public String getName() {
      return name.getValue();
    }

    public void setName(String name) {
      this.name.setValue(name);
    }

    public List<String> getAliases() {
      return List.copyOf(aliases);
    }

    public void setAliases(List<String> aliases) {
      this.aliases.setAll(aliases);
    }

    public boolean getExcluded() {
      return excluded.getValue();
    }

    public void setExcluded(Boolean excluded) {
      this.excluded.setValue(excluded);
    }

    // This is deprecated and replaced by aliases, but we need to leave it in place
    // here for the purpose of supporting automatic migration from older config versions.
    private void setCode(String code) {
      if (code != null && !code.isBlank()) {
        this.aliases.add(code);
      }
    }

    /**
     * A stream of all aliases (which is guaranteed to be unique) and the candidate name (which is
     * not guaranteed to be unique, i.e. it may exist in the list twice)
     *
     * @return a stream containing the candidate name and all aliases, with no null elements
     */
    public Stream<String> createStreamOfNameAndAllAliases() {
      List<String> otherNames = new ArrayList<>();
      if (!isNullOrBlank(getName())) {
        otherNames.add(getName());
      }

      return Stream.concat(this.aliases.stream(), otherNames.stream());
    }

    /** Removes whitespace around all name and alias strings. */
    public void trimNameAndAllAliases() {
      if (name != null) {
        name.setValue(getName().trim());
      }
      if (aliases != null) {
        aliases.replaceAll(String::trim);
      }
    }

    /**
     * The following properties might be marked as unused by an IDE, but are necessary to save edits
     * to a cell. See PropertyValueFactory.
     */
    public SimpleStringProperty nameProperty() {
      return name;
    }

    public SimpleListProperty<String> aliasesProperty() {
      return aliases;
    }

    public SimpleBooleanProperty excludedProperty() {
      return excluded;
    }
  }

  /** Contest rules necessary for tabulation that can be serialized and deserialized. */
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
    public String maxSkippedRanksAllowed;
    public String maxRankingsAllowed;
    public boolean nonIntegerWinningThreshold;
    public boolean doesFirstRoundDetermineThreshold;
    public boolean hareQuota;
    public boolean batchElimination;
    public boolean bulkElimination;
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
