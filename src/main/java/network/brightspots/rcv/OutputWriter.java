/*
 * RCTab
 * Copyright (c) 2017-2023 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: Ingests tabulation results and generates various summary report files.
 * Design: Generates per-slice files if specified.
 * CSV summary file(s) with round by round counts.
 * JSON summary file(s) with additional data on transfer counts.
 * Also converts CVR sources into CDF format and writes them to disk.
 * Conditions: During tabulation and conversion.
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import static java.util.Map.entry;
import static network.brightspots.rcv.CastVoteRecord.StatusForRound;
import static network.brightspots.rcv.Utils.isNullOrBlank;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.util.Pair;
import network.brightspots.rcv.ContestConfig.TabulateBySlice;
import network.brightspots.rcv.RawContestConfig.CvrSource;
import network.brightspots.rcv.Tabulator.RoundTallies;
import network.brightspots.rcv.Tabulator.SliceIdSet;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

class OutputWriter {
  private static final String CDF_CONTEST_ID = "contest-001";
  private static final String CDF_ELECTION_ID = "election-001";
  private static final String CDF_GPU_ID = "gpu-election";
  private static final String CDF_GPU_ID_FORMAT = "gpu-%d";
  private static final String CDF_REPORTING_DEVICE_ID = "rd-001";

  private static final Map<String, String> cdfCandidateNameToContestSelectionId = new HashMap<>();
  private static final Map<String, String> cdfCandidateNameToCandidateId = new HashMap<>();

  // number of rounds needed to elect winner(s)
  private int numRounds;
  // all Slice Ids that may appear in the output cvrs
  private SliceIdSet sliceIds;
  // precinct to GpUnitId map (CDF only)
  private Map<String, String> gpUnitIds;
  // map from round number to list of candidates eliminated in that round
  private Map<Integer, List<String>> roundToEliminatedCandidates;
  // map from round number to list of candidates winning in that round
  private Map<Integer, List<String>> roundToWinningCandidates;
  private ContestConfig config;
  private String timestampString;
  // map from round number to residual surplus generated in that round
  private Map<Integer, BigDecimal> roundToResidualSurplus;
  // statuses to print in all summary files
  // (additional fields are added if needed in specific summary filetypes)
  private static final List<StatusForRound> STATUSES_TO_PRINT = List.of(
          StatusForRound.INVALIDATED_BY_OVERVOTE,
          StatusForRound.INVALIDATED_BY_SKIPPED_RANKING,
          StatusForRound.EXHAUSTED_CHOICE,
          StatusForRound.INVALIDATED_BY_REPEATED_RANKING);

  public enum OutputType {
    SUMMARY_CSV("summary_report", "csv"),
    DETAILED_CSV("detailed_report", "csv"),
    DETAILED_JSON("detailed_report", "json"),
    CDF_CVR("cdf_cvr", "json"),
    RCTAB_CVR("rctab_cvr", "csv");

    private final String basename;
    private final String extension;

    OutputType(String basename, String extension) {
      this.basename = basename;
      this.extension = extension;
    }

    public String getBasename() {
      return basename;
    }

    public String getExtension() {
      return extension;
    }
  }

  // Core traits of any output file
  // Includes helpers for the work we need to do with the Identifiers to write the actual files
  public static class OutputFileIdentifiers {
    // Since sanitizing Slice IDs can cause filename collisions, ensure each non-sanitized
    // Slice ID is given a unique sanitized name.
    private static final Dictionary<String, String> sliceIdToUniqueSanitizedId = new Hashtable<>();
    // This is a set of the values in sanitizerCollisionResolution to ensure there are no collisions
    // in the resulting sanitized names.
    private static final HashSet<String> uniqueSanitizedIds = new HashSet<>();
    private final OutputType outputType;
    private final TabulateBySlice slice;
    private final String sliceId;

    OutputFileIdentifiers(OutputType outputType) {
      this.outputType = outputType;
      slice = null;
      sliceId = null;
    }

    OutputFileIdentifiers(OutputType outputType, TabulateBySlice slice, String sliceId) {
      this.outputType = outputType;
      this.slice = slice;
      this.sliceId = sliceId;
    }

    public boolean isSlice() {
      return slice != null;
    }

    // getPath helper without the modifier argument
    public Path getPath(String directory, String prefix, Integer sequentialId) {
      return getPath(directory, prefix, null, sequentialId);
    }

    // The filename is an underscore-separated string containing all components needed to create a
    // unique output file. At the time of writing this comment, the modifier is only used in tests,
    // where it is set to "expected" to distinguish expected output files from actual output files.
    public Path getPath(String directory, String prefix, String modifier, Integer sequentialId) {
      List<String> parts = new ArrayList<>();
      parts.add(prefix);
      if (sequentialId != null) {
        parts.add(sequentialId.toString());
      }
      if (modifier != null) {
        parts.add(modifier);
      }
      if (isSlice()) {
        parts.add(sanitizeSliceWithoutCollisions(sliceId));
        parts.add(slice.toLowerString());
        directory = Path.of(directory, "Tabulate by " + slice).toString();

        try {
          FileUtils.createOutputDirectory(directory);
        } catch (FileUtils.UnableToCreateDirectoryException e) {
          Logger.severe("Could not create directory %s: %s", directory, e.getMessage());
        }
      }
      parts.add(outputType.getBasename());

      String filenameWithoutExt = String.join("_", parts);
      return Path.of(directory, "%s.%s".formatted(filenameWithoutExt, outputType.getExtension()));
    }

    private String sanitizeSliceWithoutCollisions(String sliceId) {
      String previousSanitizedSliceId = sliceIdToUniqueSanitizedId.get(sliceId);
      String sanitizedSliceId;

      if (previousSanitizedSliceId != null) {
        sanitizedSliceId = previousSanitizedSliceId;
      } else {
        sanitizedSliceId = sanitizeStringForOutput(sliceId);

        // In most cases, sanitizedSliceId will be unique, and we can use it as-is.
        // Here, we handle the case where it is not unique.
        int increment = 1;
        while (uniqueSanitizedIds.contains(sanitizedSliceId)) {
          sanitizedSliceId = String.format("%s_%d", sanitizeStringForOutput(sliceId), increment);
          increment++;
        }

        if (increment > 1) {
          Logger.warning("The sanitized filename for Precinct %s results is not unique"
                  + " and has been renamed to %s", sliceId, sanitizedSliceId);
        }

        uniqueSanitizedIds.add(sanitizedSliceId);
        sliceIdToUniqueSanitizedId.put(sliceId, sanitizedSliceId);
      }

      return sanitizedSliceId;
    }

  }

  static String sanitizeStringForOutput(String s) {
    return s == null ? "" : s.replaceAll("[^a-zA-Z0-9_\\-.]", "_");
  }

  private static void generateJsonFile(AuditableFile outFile, Map<String, Object> json)
          throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    // for improved legibility we sort alphabetically on keys
    mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    SimpleModule module = new SimpleModule();
    module.addSerializer(BigDecimal.class, new ToStringSerializer());
    mapper.registerModule(module);
    ObjectWriter jsonWriter = mapper.writer(new DefaultPrettyPrinter());

    try {
      jsonWriter.writeValue(outFile, json);
      outFile.finalizeAndHash();
    } catch (IOException exception) {
      Logger.severe(
          "Error writing to JSON file: %s\n%s\nCheck the file path and permissions!",
          outFile.getAbsolutePath(), exception);
      throw exception;
    }
    Logger.info("JSON file generated successfully.");
  }

  private static String generateCvrSnapshotId(String cvrId, Integer round) {
    return round != null && round > 0
        ? String.format("ballot-%s-round-%d", cvrId, round)
        : String.format("ballot-%s", cvrId);
  }

  // generates an internal ContestSelectionId based on a candidate name
  private static String getCdfContestSelectionIdForCandidateName(String name) {
    return cdfCandidateNameToContestSelectionId.computeIfAbsent(
        name, c -> String.format("cs-%s", sanitizeStringForOutput(c).toLowerCase()));
  }

  // generates an internal CandidateId based on a candidate name
  private static String getCdfCandidateIdForCandidateName(String name) {
    return cdfCandidateNameToCandidateId.computeIfAbsent(
        name, c -> String.format("c-%s", sanitizeStringForOutput(c).toLowerCase()));
  }

  // Instead of a list mapping ranks to list of candidates, we need a sorted list of candidates
  // with the ranks they were given. (Ordinarily a candidate will have only a single rank, but they
  // could have multiple ranks if the ballot duplicates the candidate, i.e. assigns them multiple
  // ranks.)
  // We sort by the lowest (best) rank, then alphabetically by name.
  private static List<Map.Entry<String, List<Integer>>> getCandidatesWithRanksList(
      CandidateRankingsList candidateRankings) {
    Map<String, List<Integer>> candidateNameToRanks = new HashMap<>();
    // first group the ranks by candidate
    for (Pair<Integer, CandidatesAtRanking> rankCandidatesPair : candidateRankings) {
      Integer rank = rankCandidatesPair.getKey();
      CandidatesAtRanking candidatesAtRanking = rankCandidatesPair.getValue();
      for (String candidateName : candidatesAtRanking) {
        candidateNameToRanks.computeIfAbsent(candidateName, k -> new LinkedList<>());
        candidateNameToRanks.get(candidateName).add(rank);
      }
    }
    // we want the ranks for a given candidate in ascending order
    for (List<Integer> list : candidateNameToRanks.values()) {
      Collections.sort(list);
    }
    List<Map.Entry<String, List<Integer>>> sortedCandidatesWithRanks =
        new LinkedList<>(candidateNameToRanks.entrySet());
    // and now we sort the list of candidates with ranks
    sortedCandidatesWithRanks.sort(
        (firstObject, secondObject) -> {
          int ret;
          Integer firstRank = firstObject.getValue().get(0);
          Integer secondRank = secondObject.getValue().get(0);
          if (!firstRank.equals(secondRank)) {
            ret = firstRank.compareTo(secondRank);
          } else {
            ret = firstObject.getKey().compareTo(secondObject.getKey());
          }
          return ret;
        });
    return sortedCandidatesWithRanks;
  }

  OutputWriter setRoundToResidualSurplus(Map<Integer, BigDecimal> roundToResidualSurplus) {
    this.roundToResidualSurplus = roundToResidualSurplus;
    return this;
  }

  OutputWriter setNumRounds(int numRounds) {
    this.numRounds = numRounds;
    return this;
  }

  OutputWriter setSliceIds(SliceIdSet sliceIds) {
    this.sliceIds = sliceIds;
    return this;
  }

  OutputWriter setCandidatesToRoundEliminated(Map<String, Integer> candidatesToRoundEliminated) {
    // roundToEliminatedCandidates is the inverse of candidatesToRoundEliminated map,
    // so we can look up who got eliminated for each round
    roundToEliminatedCandidates = new HashMap<>();
    for (var entry : candidatesToRoundEliminated.entrySet()) {
      roundToEliminatedCandidates.computeIfAbsent(entry.getValue(), k -> new LinkedList<>());
      roundToEliminatedCandidates.get(entry.getValue()).add(entry.getKey());
    }
    return this;
  }

  OutputWriter setWinnerToRound(Map<String, Integer> winnerToRound) {
    // very similar to the logic in setCandidatesToRoundEliminated above
    roundToWinningCandidates = new HashMap<>();
    for (var entry : winnerToRound.entrySet()) {
      roundToWinningCandidates.computeIfAbsent(entry.getValue(), k -> new LinkedList<>());
      roundToWinningCandidates.get(entry.getValue()).add(entry.getKey());
    }
    return this;
  }

  OutputWriter setContestConfig(ContestConfig config) {
    this.config = config;
    return this;
  }

  OutputWriter setTimestampString(String timestampString) {
    this.timestampString = timestampString;
    return this;
  }

  // creates results files for the votes split by a TabulateBySlice
  // param: roundTalliesBySlice is map from a slice type to the round-by-round vote tallies
  // param: tallyTransfersBySlice is a map from a slice type to tally transfers for that slice
  // param: candidateOrder is to allow a consistent ordering of candidates, including across slices
  void generateBySliceResultsFiles(
        Tabulator.BreakdownBySlice<RoundTallies> roundTalliesBySlice,
        Tabulator.BreakdownBySlice<TallyTransfers> tallyTransfersBySlice,
        List<String> candidateOrder)
      throws IOException {
    for (ContestConfig.TabulateBySlice slice : config.enabledSlices()) {
      for (var entry : roundTalliesBySlice.get(slice).entrySet()) {
        String sliceId = entry.getKey();
        RoundTallies roundTallies = entry.getValue();
        generateCsvReport(
                roundTallies,
                candidateOrder,
                new OutputFileIdentifiers(OutputType.DETAILED_CSV, slice, sliceId));
        generateJsonReport(
                roundTallies,
                tallyTransfersBySlice.get(slice, sliceId),
                new OutputFileIdentifiers(OutputType.DETAILED_JSON, slice, sliceId));
      }
    }
  }

  private AuditableFile createAuditableFile(OutputFileIdentifiers outputFileIdentifiers) {
    Integer sequentialId = null;
    if (config.isMultiSeatSequentialWinnerTakesAllEnabled()) {
      sequentialId = config.getSequentialWinners().size() + 1;
    }
    return new AuditableFile(outputFileIdentifiers.getPath(
            config.getOutputDirectory(timestampString), timestampString, sequentialId));
  }

  // create a results .csv file
  // param: roundTallies is the round-by-count count of votes per candidate
  // param: candidateOrder is to allow a consistent ordering of candidates, including across slices
  // param: outputFileIdentifiers must only have type DETAILED_CSV or SUMMARY_CSV
  private void generateCsvReport(
          RoundTallies roundTallies,
          List<String> candidateOrder,
          OutputFileIdentifiers outputFileIdentifiers) throws IOException {
    if (outputFileIdentifiers.outputType != OutputType.SUMMARY_CSV
            && outputFileIdentifiers.outputType != OutputType.DETAILED_CSV) {
      throw new IllegalArgumentException("ResultFile provided non-CSV Report Type "
              + outputFileIdentifiers.outputType);
    }
    // Check that all candidates are included in the candidate order
    Set<String> expectedCandidates = roundTallies.get(1).getCandidates();
    Set<String> providedCandidates = new HashSet<>(candidateOrder);
    if (!expectedCandidates.equals(providedCandidates)) {
      throw new IllegalArgumentException(
              "Candidate order must include all candidates in the contest. "
                      + "\nExpected: " + expectedCandidates
                      + "\nProvided: " + providedCandidates);
    }

    AuditableFile csvFile = createAuditableFile(outputFileIdentifiers);
    Logger.info("Generating summary spreadsheet: %s...", csvFile.getAbsolutePath());

    CSVPrinter csvPrinter;
    try {
      BufferedWriter writer = Files.newBufferedWriter(csvFile.toPath());
      csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT);
    } catch (IOException exception) {
      Logger.severe(
          "Error creating CSV file: %s\n%s\nCheck the file path and permissions!",
          csvFile.getAbsolutePath(), exception);
      throw exception;
    }

    BigDecimal winningThreshold = roundTallies.get(numRounds).getWinningThreshold();
    addContestInformationRows(csvPrinter, winningThreshold,
            outputFileIdentifiers.slice, outputFileIdentifiers.sliceId);
    addContestSummaryRows(csvPrinter, roundTallies.get(1));
    csvPrinter.print("Rounds");
    for (int round = 1; round <= numRounds; round++) {
      csvPrinter.print(String.format("Round %d Votes", round));
      csvPrinter.print("% of vote");
      csvPrinter.print("transfer");
    }
    csvPrinter.println();

    csvPrinter.print(outputFileIdentifiers.isSlice() ? "Eliminated*" : "Eliminated");
    printActionSummary(csvPrinter, roundToEliminatedCandidates);

    csvPrinter.print(outputFileIdentifiers.isSlice() ? "Elected*" : "Elected");
    printActionSummary(csvPrinter, roundToWinningCandidates);

    // For each candidate: for each round: output total votes
    for (String candidate : candidateOrder) {
      String candidateDisplayName = config.getNameForCandidate(candidate);
      csvPrinter.print(candidateDisplayName);
      for (int round = 1; round <= numRounds; round++) {
        BigDecimal thisRoundTally = roundTallies.get(round).getCandidateTally(candidate);
        // not all candidates may have a tally in every round
        if (thisRoundTally == null) {
          thisRoundTally = BigDecimal.ZERO;
        }

        // Vote count
        csvPrinter.print(thisRoundTally);

        // Vote % (divisor is 1st round total in STV or 1st round determines threshold)
        BigDecimal votePctDivisor;
        if (config.isSingleWinnerEnabled() && !config.isFirstRoundDeterminesThresholdEnabled()) {
          votePctDivisor = roundTallies.get(round).activeAndLockedInBallotSum();
        } else {
          votePctDivisor = roundTallies.get(1).activeAndLockedInBallotSum();
        }
        if (!votePctDivisor.equals(BigDecimal.ZERO)) {
          // Turn a decimal into a human-readable percentage (e.g. 0.1234 -> 12.34%)
          BigDecimal divDecimal = thisRoundTally.divide(votePctDivisor, MathContext.DECIMAL32);
          csvPrinter.print(divDecimal.scaleByPowerOfTen(4).intValue() / 100.0 + "%");
        } else {
          csvPrinter.print("");
        }

        // Transfer
        if (round < numRounds) {
          BigDecimal nextRoundTally = roundTallies.get(round + 1).getCandidateTally(candidate);
          if (nextRoundTally == null) {
            nextRoundTally = BigDecimal.ZERO;
          }
          csvPrinter.print(nextRoundTally.subtract(thisRoundTally));
        } else {
          csvPrinter.print(0);
        }
      }
      csvPrinter.println();
    }

    csvPrinter.print("Active Ballots");
    for (int round = 1; round <= numRounds; round++) {
      // While internally we separate out an "active" and a "locked in" ballot,
      // externally, the difference is not important.
      csvPrinter.print(roundTallies.get(round).activeAndLockedInBallotSum());
      csvPrinter.print("");
      csvPrinter.print("");
    }
    csvPrinter.println();

    if (!outputFileIdentifiers.isSlice()) {
      csvPrinter.print("Current Round Threshold");
      for (int round = 1; round <= numRounds; round++) {
        csvPrinter.print(roundTallies.get(round).getWinningThreshold());
        csvPrinter.print("");
        csvPrinter.print("");
      }
      csvPrinter.println();
    }

    if (outputFileIdentifiers.outputType == OutputType.DETAILED_CSV) {
      for (StatusForRound status : STATUSES_TO_PRINT) {
        csvPrinter.print(status.getTitleCaseKey());

        for (int round = 1; round <= numRounds; round++) {
          BigDecimal thisRoundInactive = roundTallies.get(round).getBallotStatusTally(status);
          csvPrinter.print(thisRoundInactive);

          // Don't display percentage of inactive ballots
          csvPrinter.print("");

          // Do display transfer of inactive ballots
          if (round != numRounds) {
            BigDecimal nextRoundInactive = roundTallies.get(round + 1).getBallotStatusTally(status);
            BigDecimal diff = nextRoundInactive.subtract(thisRoundInactive);
            csvPrinter.print(diff);
          } else {
            csvPrinter.print(0);
          }
        }
        csvPrinter.println();
      }
    }

    csvPrinter.print("Inactive Ballots Total");
    BigDecimal numNoRankings =
        roundTallies.get(1).getBallotStatusTally(StatusForRound.DID_NOT_RANK_ANY_CANDIDATES);
    for (int round = 1; round <= numRounds; round++) {
      BigDecimal thisRoundInactive = roundTallies.get(round).inactiveBallotSum();
      csvPrinter.print(thisRoundInactive.subtract(numNoRankings));

      // Don't display percentage of inactive ballots
      csvPrinter.print("");

      // Do display transfer of inactive ballots
      if (round != numRounds) {
        // Note: we don't need to subtract num undervotes here since we'd be subtracting the
        // same value from both sides of the equation, so it cancels out.
        BigDecimal nextRoundInactive = roundTallies.get(round + 1).inactiveBallotSum();
        BigDecimal diff = nextRoundInactive.subtract(thisRoundInactive);
        csvPrinter.print(diff);
      } else {
        csvPrinter.print(0);
      }
    }
    csvPrinter.println();

    // row for residual surplus (if needed)
    // We check if we accumulated any residual surplus over the course of the tabulation by testing
    // whether the value in the final round is positive.
    // Note that this concept only makes sense when we're reporting the overall tabulation, so we
    // omit it when generating results at the individual by-slice level.
    if (!outputFileIdentifiers.isSlice() && roundToResidualSurplus.get(numRounds).signum() == 1) {
      csvPrinter.print("Residual surplus");
      for (int round = 1; round <= numRounds; round++) {
        csvPrinter.print(roundToResidualSurplus.get(round));

        // Don't display transfer or percentage of residual surplus
        csvPrinter.print("");
        csvPrinter.print("");
      }
      csvPrinter.println();
    }

    if (config.usesSurpluses()) {
      // row for final round surplus (if needed)
      csvPrinter.print(StatusForRound.FINAL_ROUND_SURPLUS.getTitleCaseKey());
      for (int round = 1; round <= numRounds; round++) {
        BigDecimal finalRoundSurplus =
                roundTallies.get(round).getBallotStatusTally(StatusForRound.FINAL_ROUND_SURPLUS);
        csvPrinter.print(finalRoundSurplus.equals(BigDecimal.ZERO) ? "" : finalRoundSurplus);

        // Don't display transfer or percentage of residual surplus
        csvPrinter.print("");
        csvPrinter.print("");
      }
      csvPrinter.println();
    }

    if (outputFileIdentifiers.isSlice()) {
      csvPrinter.println();
      csvPrinter.print(String.format("*Elect/Eliminate decisions are from the full contest. "
              + "All other results on this report are at the %s level.",
              outputFileIdentifiers.slice.toLowerString()));
      csvPrinter.println();
    }

    try {
      csvPrinter.flush();
      csvPrinter.close();
      csvFile.finalizeAndHash();
    } catch (IOException exception) {
      Logger.severe("Error saving file: %s\n%s", csvFile.getAbsolutePath(), exception);
      throw exception;
    }
    Logger.info("Summary spreadsheet generated successfully.");
  }

  private void addContestSummaryRows(CSVPrinter csvPrinter, RoundTally round1Tally)
      throws IOException {
    BigDecimal numNoRankings =
        round1Tally.getBallotStatusTally(StatusForRound.DID_NOT_RANK_ANY_CANDIDATES);
    BigDecimal totalNumberBallots =
        round1Tally.activeBallotSum().add(round1Tally.inactiveBallotSum());
    csvPrinter.printRecord("Contest Summary");
    csvPrinter.printRecord("Number to be Elected", config.getNumberOfWinners());
    csvPrinter.printRecord("Number of Candidates", config.getNumCandidates());
    csvPrinter.printRecord("Total Number of Ballots", totalNumberBallots);
    csvPrinter.printRecord("Number of Undervotes (No Rankings)", numNoRankings);
    csvPrinter.println();
  }

  private void printActionSummary(
      CSVPrinter csvPrinter, Map<Integer, List<String>> roundToCandidates) throws IOException {
    for (int round = 1; round <= numRounds; round++) {
      List<String> winners = roundToCandidates.get(round);
      if (winners != null && winners.size() > 0) {
        addActionRowCandidates(winners, csvPrinter);
      } else {
        csvPrinter.print("");
      }

      // Empty % of vote and transfer columns
      csvPrinter.print("");
      csvPrinter.print("");
    }
    csvPrinter.println();
  }

  // add the given candidate(s) names to the csv file next cell
  private void addActionRowCandidates(List<String> candidates, CSVPrinter csvPrinter)
      throws IOException {
    List<String> candidateDisplayNames = new ArrayList<>();
    for (String candidate : candidates) {
      candidateDisplayNames.add(config.getNameForCandidate(candidate));
    }
    // use semicolon as delimiter display in a single cell
    String candidateCellText = String.join("; ", candidateDisplayNames);
    csvPrinter.print(candidateCellText);
  }

  private void addContestInformationRows(CSVPrinter csvPrinter,
                                         BigDecimal winningThreshold,
                                         ContestConfig.TabulateBySlice slice,
                                         String sliceId) throws IOException {
    csvPrinter.printRecord("Contest Information");
    csvPrinter.printRecord("Generated By", "RCTab " + Main.APP_VERSION);
    csvPrinter.printRecord("CSV Format Version", "1");
    csvPrinter.printRecord(
        "Type of Election", config.isSingleWinnerEnabled() ? "Single-Winner" : "Multi-Winner");
    csvPrinter.printRecord("Contest", config.getContestName());
    csvPrinter.printRecord("Jurisdiction", config.getContestJurisdiction());
    csvPrinter.printRecord("Office", config.getContestOffice());
    csvPrinter.printRecord("Date", config.getContestDate());

    List<String> winners = new LinkedList<>();
    List<Integer> winningRounds = new ArrayList<>(roundToWinningCandidates.keySet());
    // make sure we list them in order of election
    Collections.sort(winningRounds);
    for (int round : winningRounds) {
      for (String candidateName : roundToWinningCandidates.get(round)) {
        winners.add(config.getNameForCandidate(candidateName));
      }
    }

    csvPrinter.printRecord("Winner(s)", String.join(", ", winners));

    if (!isNullOrBlank(sliceId)) {
      // Only slices print the slice information
      csvPrinter.printRecord(slice, sliceId);
    } else {
      // Only non-slices print threshold information
      csvPrinter.printRecord("Final Threshold", winningThreshold);
    }

    csvPrinter.println();
  }

  // creates a summary spreadsheet and JSON for the full contest (as opposed to a specific slice)
  void generateContestResultFiles(
      RoundTallies roundTallies,
      TallyTransfers tallyTransfers,
      List<String> candidateOrder) throws IOException {
    generateCsvReport(roundTallies, candidateOrder,
            new OutputFileIdentifiers(OutputType.SUMMARY_CSV));
    generateCsvReport(roundTallies, candidateOrder,
            new OutputFileIdentifiers(OutputType.DETAILED_CSV));
    generateJsonReport(roundTallies, tallyTransfers,
            new OutputFileIdentifiers(OutputType.DETAILED_JSON));
  }

  // Write CastVoteRecords for the specified contest to the provided folder,
  // using the simplified RCTab format.
  // Note that the castVoteRecords list MUST be stable, as cvrSourceData
  // relies on its exact ordering to determine which source each record came from.
  // Returns the filepath written
  String writeRcTabCvrCsv(
      List<CastVoteRecord> castVoteRecords,
      List<CvrSourceData> cvrSourceData,
      String csvOutputFolder)
      throws IOException {
    String fileWritten;
    // Put the input filename in the output filename in case contestId isn't unique --
    // knowing that it's possible that if both the filename AND the contestId isn't unique,
    // this will fail.
    OutputFileIdentifiers outputFileIdentifiers = new OutputFileIdentifiers(OutputType.RCTAB_CVR);
    AuditableFile auditableFile = createAuditableFile(outputFileIdentifiers);
    try {
      Logger.info("Writing cast vote records in generic format to file: %s...",
              auditableFile.getAbsolutePath());
      CSVPrinter csvPrinter;
      BufferedWriter writer = Files.newBufferedWriter(auditableFile.toPath());
      CSVFormat format = CSVFormat.DEFAULT.builder().setNullString("").build();
      csvPrinter = new CSVPrinter(writer, format);
      // print header:
      // RCTab CVR Id, ContestId, TabulatorId, BatchId, RecordId, Precinct, Precinct Portion,
      csvPrinter.print("Source Filepath"); // CvrSource.getFilePath()
      csvPrinter.print("CVR Provider"); // CvrSource.getProvider()
      csvPrinter.print("Contest Id"); // CastVoteRecord.getContestId()
      csvPrinter.print("RCTab CVR Id"); // CastVoteRecord.getId()
      csvPrinter.print("Tabulator Id"); // CastVoteRecord.getTabulatorId
      csvPrinter.print("Batch Id"); // CastVoteRecord.getSlice(TabulateBySlice.BATCH)
      csvPrinter.print("Vendor Id"); // CastVoteRecord.getSuppliedId
      csvPrinter.print("Precinct"); // CastVoteRecord.getSlice(TabulateBySlice.PRECINCT)
      csvPrinter.print("Precinct Portion"); // CastVoteRecord.castVoteRecord.getPrecinctPortion()

      // rank 1 selection, rank 2 selection, ... rank maxRanks selection
      int maxRank;
      if (config.isMaxRankingsSetToMaximum()) {
        maxRank = config.getNumDeclaredCandidates();
      } else {
        maxRank = config.getMaxRankingsAllowedWhenNotSetToMaximum();
      }
      for (int rank = 1; rank <= maxRank; rank++) {
        String label = String.format("Rank %d", rank);
        csvPrinter.print(label);
      }
      csvPrinter.println();

      // While the cast vote records are in a flattened list,
      // we can use the CvrSourceData to determine which source each record came from.
      int currentSourceIndex = 0;
      CvrSourceData currentSourceData = cvrSourceData.get(currentSourceIndex);

      // print rows:
      for (int i = 0; i < castVoteRecords.size(); i++) {
        if (i > currentSourceData.lastIndexInCvrList) {
          // we've moved on to a new contest, so we need to switch to the next source
          currentSourceIndex++;
          currentSourceData = cvrSourceData.get(currentSourceIndex);

          if (currentSourceData.sourceIndex != currentSourceIndex) {
            throw new RuntimeException("Source list must be sorted by sourceIndex!");
          }
        }

        CastVoteRecord castVoteRecord = castVoteRecords.get(i);
        csvPrinter.print(currentSourceData.source.getFilePath());
        csvPrinter.print(currentSourceData.source.getProvider());
        csvPrinter.print(castVoteRecord.getContestId());
        csvPrinter.print(castVoteRecord.getId());
        csvPrinter.print(castVoteRecord.getTabulatorId());
        csvPrinter.print(castVoteRecord.getSlice(TabulateBySlice.BATCH));
        csvPrinter.print(Objects.toString(castVoteRecord.getSuppliedId(), ""));
        csvPrinter.print(castVoteRecord.getSlice(ContestConfig.TabulateBySlice.PRECINCT));
        csvPrinter.print(castVoteRecord.getPrecinctPortion());
        printRankings(currentSourceData.source.getUndeclaredWriteInLabel(), maxRank,
            currentSourceData.reader, currentSourceData.source, csvPrinter, castVoteRecord);
        csvPrinter.println();
      }
      // finalize the file
      csvPrinter.flush();
      csvPrinter.close();
      fileWritten = auditableFile.getAbsolutePath();
      Logger.info("Successfully wrote: %s", auditableFile.getAbsolutePath());

      auditableFile.finalizeAndHash();
    } catch (IOException exception) {
      Logger.severe(
          "Error writing cast vote records in generic format to output path: %s\n%s",
          auditableFile.getAbsolutePath(), exception);
      throw exception;
    }
    return fileWritten;
  }

  private void printRankings(
      String undeclaredWriteInLabel,
      Integer maxRanks,
      BaseCvrReader reader,
      CvrSource source,
      CSVPrinter csvPrinter,
      CastVoteRecord castVoteRecord)
      throws IOException {
    // for each rank determine what candidate id, overvote, or undervote occurred and print it
    for (int rank = 1; rank <= maxRanks; rank++) {
      if (castVoteRecord.candidateRankings.hasRankingAt(rank)) {
        CandidatesAtRanking candidates = castVoteRecord.candidateRankings.get(rank);
        // We list all candidates at a given ranking on separate lines
        // This allows algorithms which accept overvotes.
        List<String> allCandidatesAtRanking = new ArrayList<>(candidates.count());
        for (String candidate : candidates) {
          String selection = candidate;
          // We map all undeclared write-ins to our constant string when we read them in,
          // so we need to translate it back to the original candidate ID here.
          if (selection.equals(Tabulator.UNDECLARED_WRITE_IN_OUTPUT_LABEL)) {
            selection = undeclaredWriteInLabel;
          } else if (selection.equals(Tabulator.EXPLICIT_OVERVOTE_LABEL)) {
            selection = Tabulator.EXPLICIT_OVERVOTE_LABEL;
          } else {
            selection = config.getNameForCandidate(selection);
          }
          allCandidatesAtRanking.add(selection);
        }
        csvPrinter.print(String.join("\n", allCandidatesAtRanking));
      } else {
        csvPrinter.print("undervote");
      }
    }
  }

  // create NIST Common Data Format CVR json
  void generateCdfJson(List<CastVoteRecord> castVoteRecords)
      throws IOException, RoundSnapshotDataMissingException {
    // generate GpUnitIds for precincts "geopolitical units" (can be a precinct or jurisdiction)
    gpUnitIds = generateGpUnitIds();

    OutputFileIdentifiers outputFileIdentifiers = new OutputFileIdentifiers(OutputType.CDF_CVR);
    AuditableFile auditableFile = createAuditableFile(outputFileIdentifiers);
    Logger.info("Generating cast vote record CDF JSON file: %s...",
            auditableFile.getAbsolutePath());

    HashMap<String, Object> outputJson = new HashMap<>();
    outputJson.put("CVR", generateCdfMapForCvrs(castVoteRecords));
    outputJson.put("Election", new Map[] {generateCdfMapForElection()});
    outputJson.put(
        "GeneratedDate", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date()));
    outputJson.put("GpUnit", generateCdfMapForGpUnits());
    outputJson.put("ReportGeneratingDeviceIds", new String[] {CDF_REPORTING_DEVICE_ID});
    outputJson.put(
        "ReportingDevice",
        new Map[] {
          Map.ofEntries(
              entry("@id", CDF_REPORTING_DEVICE_ID),
              entry("@type", "CVR.ReportingDevice"),
              entry("Application", Main.APP_NAME),
              entry("Manufacturer", "Bright Spots"))
        });
    outputJson.put("Version", "1.0.0");
    outputJson.put("@type", "CVR.CastVoteRecordReport");

    generateJsonFile(auditableFile, outputJson);
  }

  // build map from precinctId => GpUnitId, as lookups are done by precinctId during cvr creation
  private Map<String, String> generateGpUnitIds() {
    Map<String, String> gpUnitIdToPrecinctId = new HashMap<>();
    int gpUnitIdIndex = 0;
    for (String precinctId : sliceIds.get(ContestConfig.TabulateBySlice.PRECINCT)) {
      gpUnitIdToPrecinctId.put(precinctId, String.format(CDF_GPU_ID_FORMAT, ++gpUnitIdIndex));
    }
    return gpUnitIdToPrecinctId;
  }

  // generates json containing GpUnit definitions
  private List<Map<String, Object>> generateCdfMapForGpUnits() {
    List<Map<String, Object>> gpUnitMaps = new LinkedList<>();
    // election-scope entry for the election jurisdiction
    gpUnitMaps.add(
        Map.ofEntries(
            entry("@id", CDF_GPU_ID),
            entry("Type", "other"),
            entry("OtherType", "Election Scope Jurisdiction"),
            entry("Name", config.getContestJurisdiction()),
            entry("@type", "GpUnit")));

    // generate GpUnit entries
    for (Entry<String, String> entry : gpUnitIds.entrySet()) {
      gpUnitMaps.add(
          Map.ofEntries(
              entry("@id", entry.getValue()),
              entry("Type", "precinct"),
              entry("Name", entry.getKey()),
              entry("@type", "GpUnit")));
    }
    return gpUnitMaps;
  }

  // helper method for generateCdfJson to compile the data for all the CVR snapshots
  private List<Map<String, Object>> generateCdfMapForCvrs(List<CastVoteRecord> castVoteRecords)
      throws RoundSnapshotDataMissingException {
    List<Map<String, Object>> cvrMaps = new LinkedList<>();

    for (CastVoteRecord cvr : castVoteRecords) {
      List<Map<String, Object>> cvrSnapshots = new LinkedList<>();
      cvrSnapshots.add(generateCvrSnapshotMap(cvr, null, null));
      String sanitizedId = sanitizeStringForOutput(cvr.getId());
      // copy most recent round snapshot data to subsequent rounds
      // until more snapshot data is available
      List<Pair<String, BigDecimal>> previousRoundSnapshotData = null;
      for (int round = 1; round <= numRounds; round++) {
        List<Pair<String, BigDecimal>> currentRoundSnapshotData =
            cvr.getCdfSnapshotData().get(round);

        if (currentRoundSnapshotData == null) {
          if (previousRoundSnapshotData == null) {
            throw new RoundSnapshotDataMissingException(sanitizedId);
          }
          currentRoundSnapshotData = previousRoundSnapshotData;
        }
        cvrSnapshots.add(generateCvrSnapshotMap(cvr, round, currentRoundSnapshotData));
        previousRoundSnapshotData = currentRoundSnapshotData;
      }

      // create new cvr map entry
      Map<String, Object> cvrMap = new HashMap<>();
      cvrMap.put("BallotPrePrintedId", sanitizedId);
      cvrMap.put("CurrentSnapshotId", generateCvrSnapshotId(sanitizedId, numRounds));
      cvrMap.put("CVRSnapshot", cvrSnapshots);
      cvrMap.put("ElectionId", CDF_ELECTION_ID);
      cvrMap.put("@type", "CVR");
      // if using precincts add GpUnitId for cvr precinct
      if (config.isTabulateByEnabled(TabulateBySlice.PRECINCT)) {
        String gpUnitId = gpUnitIds.get(cvr.getSlice(TabulateBySlice.PRECINCT));
        cvrMap.put("BallotStyleUnitId", gpUnitId);
      }
      cvrMaps.add(cvrMap);
    }
    return cvrMaps;
  }

  // helper for generateCdfMapForCvrs to handle a single CVR in a single round
  private Map<String, Object> generateCvrSnapshotMap(
      CastVoteRecord cvr, Integer round, List<Pair<String, BigDecimal>> currentRoundSnapshotData) {
    List<Map<String, Object>> selectionMapList = new LinkedList<>();

    List<Map.Entry<String, List<Integer>>> candidatesWithRanksList =
        getCandidatesWithRanksList(cvr.candidateRankings);
    for (Map.Entry<String, List<Integer>> candidateWithRanks : candidatesWithRanksList) {
      String candidateName = candidateWithRanks.getKey();

      String isAllocable = "unknown";
      BigDecimal numberVotes = BigDecimal.ONE;

      if (currentRoundSnapshotData != null) {
        // scanning the list isn't actually expensive because it will almost always be very short
        for (Pair<String, BigDecimal> allocation : currentRoundSnapshotData) {
          if (allocation.getKey().equals(candidateName)) {
            isAllocable = "yes";
            numberVotes = allocation.getValue();
            break;
          }
        }
        // didn't find an allocation, i.e. this ballot didn't contribute all or part of a vote to
        // this candidate in this round
        if (isAllocable.equals("unknown")) {
          isAllocable = "no";
          // not sure what numberVotes should be in this situation
        }
      }

      String fractionalVotes = null;
      if (!numberVotes.equals(BigDecimal.ONE)) {
        BigDecimal remainder = numberVotes.remainder(BigDecimal.ONE);
        if (remainder.signum() == 1) {
          fractionalVotes = remainder.toString().substring(1); // remove the 0 before the decimal
        }
      }

      List<Map<String, Object>> selectionPositionMapList = new LinkedList<>();
      for (int rank : candidateWithRanks.getValue()) {
        Map<String, Object> selectionPositionMap = new HashMap<>();
        selectionPositionMap.put("HasIndication", "yes");
        selectionPositionMap.put("IsAllocable", isAllocable);
        selectionPositionMap.put("NumberVotes", numberVotes.intValue());
        selectionPositionMap.put("Rank", rank);
        selectionPositionMap.put("@type", "CVR.SelectionPosition");
        if (fractionalVotes != null) {
          selectionPositionMap.put("FractionalVotes", fractionalVotes);
        }
        selectionPositionMapList.add(selectionPositionMap);
        if (isAllocable.equals("yes")) {
          // If there are duplicate rankings for the candidate on this ballot, only the first one
          // can be allocable.
          isAllocable = "no";
        }
      }

      selectionMapList.add(
          Map.ofEntries(
              entry("ContestSelectionId", getCdfContestSelectionIdForCandidateName(candidateName)),
              entry("SelectionPosition", selectionPositionMapList),
              entry("@type", "CVR.CVRContestSelection")));
    }

    Map<String, Object> contestMap =
        Map.ofEntries(
            entry("ContestId", CDF_CONTEST_ID),
            entry("CVRContestSelection", selectionMapList),
            entry("@type", "CVR.CVRContest"));

    return Map.ofEntries(
        entry("@id", generateCvrSnapshotId(sanitizeStringForOutput(cvr.getId()), round)),
        entry("CVRContest", new Map[] {contestMap}),
        entry("Type", round != null ? "interpreted" : "original"),
        entry("@type", "CVR.CVRSnapshot"));
  }

  private Map<String, Object> generateCdfMapForElection() {
    // iterate all candidates and create Candidate and ContestSelection objects for them
    List<String> candidateNames = new LinkedList<>(config.getCandidateNames());
    // if any of the sources have overvote labels, we also need to register the explicit overvote
    // as a valid candidate/contest selection
    boolean includeExplicitOvervote = false;
    for (CvrSource source : config.getRawConfig().cvrFileSources) {
      if (!isNullOrBlank(source.getOvervoteLabel())) {
        includeExplicitOvervote = true;
        break;
      }
    }
    if (includeExplicitOvervote) {
      candidateNames.add(Tabulator.EXPLICIT_OVERVOTE_LABEL);
    }
    Collections.sort(candidateNames);
    List<Map<String, Object>> candidates = new LinkedList<>();
    List<Map<String, Object>> contestSelections = new LinkedList<>();
    for (String candidateName : candidateNames) {
      candidates.add(
          Map.ofEntries(
              entry("@id", getCdfCandidateIdForCandidateName(candidateName)),
              entry("Name", candidateName)));

      contestSelections.add(
          Map.ofEntries(
              entry("@id", getCdfContestSelectionIdForCandidateName(candidateName)),
              entry("@type", "ContestSelection"),
              entry(
                  "CandidateIds",
                  new String[] {getCdfCandidateIdForCandidateName(candidateName)})));
    }

    Map<String, Object> contestJson =
        Map.ofEntries(
            entry("@id", CDF_CONTEST_ID),
            entry("@type", "CandidateContest"),
            entry("ContestSelection", contestSelections),
            entry("Name", config.getContestName()));

    HashMap<String, Object> electionMap = new HashMap<>();
    electionMap.put("@id", CDF_ELECTION_ID);
    electionMap.put("Candidate", candidates);
    electionMap.put("Contest", new Map[] {contestJson});
    electionMap.put("ElectionScopeId", CDF_GPU_ID);
    electionMap.put("@type", "Election");

    return electionMap;
  }

  // create summary json data for use with external visualizer software, unit tests and other tools
  private void generateJsonReport(
      RoundTallies roundTallies,
      TallyTransfers tallyTransfers,
      OutputFileIdentifiers outputFileIdentifiers)
      throws IOException {
    AuditableFile jsonFile = createAuditableFile(outputFileIdentifiers);
    Logger.info("Generating summary JSON file: %s...", jsonFile.getAbsolutePath());

    // config will contain contest configuration info
    HashMap<String, Object> configData = new HashMap<>();
    configData.put("generatedBy", "RCTab " + Main.APP_VERSION);
    configData.put("contest", config.getContestName());
    configData.put("jurisdiction", config.getContestJurisdiction());
    configData.put("office", config.getContestOffice());
    configData.put("date", config.getContestDate());
    if (outputFileIdentifiers.isSlice()) {
      configData.put(outputFileIdentifiers.slice.toLowerString(), outputFileIdentifiers.sliceId);
    }

    BigDecimal numNoRankings =
        roundTallies.get(1).getBallotStatusTally(StatusForRound.DID_NOT_RANK_ANY_CANDIDATES);
    BigDecimal totalNumberBallots =
        roundTallies.get(1).activeBallotSum().add(roundTallies.get(1).inactiveBallotSum());
    BigDecimal lastRoundThreshold = roundTallies.get(numRounds).getWinningThreshold();

    HashMap<String, Object> summaryData = new HashMap<>();
    summaryData.put("finalThreshold", lastRoundThreshold);
    summaryData.put("numWinners", config.getNumberOfWinners());
    summaryData.put("numCandidates", config.getCandidateNames().size());
    summaryData.put("totalNumBallots", totalNumberBallots);
    summaryData.put("undervotes", numNoRankings.toBigInteger());

    ArrayList<Object> results = new ArrayList<>();
    for (int round = 1; round <= numRounds; round++) {
      HashMap<String, Object> roundData = new HashMap<>();
      roundData.put("round", round);
      ArrayList<Object> actions = new ArrayList<>();
      addActionObjects(
          "elected", roundToWinningCandidates.get(round), round, actions, tallyTransfers);
      addActionObjects(
          "eliminated", roundToEliminatedCandidates.get(round), round, actions, tallyTransfers);
      roundData.put("tallyResults", actions);
      roundData.put("tally", updateCandidateNamesInTally(roundTallies.get(round)));
      roundData.put("threshold", roundTallies.get(round).getWinningThreshold());
      roundData.put("inactiveBallots", getInactiveJsonMap(roundTallies.get(round)));
      results.add(roundData);
    }
    // root outputJson dict will have two entries:
    // results - vote totals, transfers, and candidates elected / eliminated
    // config - global config into
    HashMap<String, Object> outputJson = new HashMap<>();
    outputJson.put("jsonFormatVersion", "1");
    outputJson.put("summary", summaryData);
    outputJson.put("config", configData);
    outputJson.put("results", results);

    generateJsonFile(jsonFile, outputJson);
  }

  private Map<String, BigDecimal> updateCandidateNamesInTally(RoundTally roundSummary) {
    Map<String, BigDecimal> newTally = new HashMap<>();
    for (String candidateName : roundSummary.getCandidates()) {
      newTally.put(
          config.getNameForCandidate(candidateName), roundSummary.getCandidateTally(candidateName));
    }
    return newTally;
  }

  private Map<String, BigDecimal> getInactiveJsonMap(RoundTally roundTally) {
    Map<String, BigDecimal> result = STATUSES_TO_PRINT.stream()
            .collect(Collectors.toMap(StatusForRound::getCamelCaseKey,
                    roundTally::getBallotStatusTally));

    if (config.usesSurpluses() && roundTally.getRoundNumber() == numRounds) {
      result.put(StatusForRound.FINAL_ROUND_SURPLUS.getCamelCaseKey(),
              roundTally.getBallotStatusTally(StatusForRound.FINAL_ROUND_SURPLUS));
    }

    return result;
  }

  // adds action objects to input action list representing all actions applied this round
  //  each action will have a type followed by a list of 0 or more vote transfers
  //  (sometimes there is no vote transfer if a candidate had no votes to transfer)
  // param: actionType is this an elimination or election action
  // param: candidates list of all candidates action is applied to
  // param: round which this action occurred
  // param: actions list to add new action objects to
  // param: tallyTransfers record of vote transfers
  private void addActionObjects(
      String actionType,
      List<String> candidates,
      int round,
      ArrayList<Object> actions,
      TallyTransfers tallyTransfers) {
    // check for valid candidates:
    // "drop undeclared write-in" may result in no one actually being eliminated
    if (candidates != null && !candidates.isEmpty()) {
      // transfers contains all vote transfers for this round
      // we add one to the round since transfers are currently stored under the round AFTER
      // the tallies which triggered them
      Map<String, Map<String, BigDecimal>> roundTransfers =
          tallyTransfers.getTransfersForRound(round + 1);

      for (String candidate : candidates) {
        HashMap<String, Object> action = new HashMap<>();
        action.put(actionType, config.getNameForCandidate(candidate));
        if (roundTransfers != null) {
          Map<String, BigDecimal> transfersFromCandidate = roundTransfers.get(candidate);
          if (transfersFromCandidate != null) {
            // We want to replace candidate IDs with names here, too.
            Map<String, BigDecimal> translatedTransfers = new HashMap<>();
            for (var entry : transfersFromCandidate.entrySet()) {
              // candidateName will be null for special values like "exhausted"
              String candidateName = config.getNameForCandidate(entry.getKey());
              translatedTransfers.put(
                  candidateName != null ? candidateName : entry.getKey(), entry.getValue());
            }
            action.put("transfers", translatedTransfers);
          }
        }
        if (!action.containsKey("transfers")) {
          action.put("transfers", new HashMap<String, BigDecimal>());
        }
        actions.add(action);
      }
    }
  }

  // Per-source data to be used in the CSV CVR export
  static class CvrSourceData {
    public final CvrSource source;
    public final BaseCvrReader reader;
    public final int sourceIndex;
    public final int firstIndexInCvrList;
    public final int lastIndexInCvrList;

    CvrSourceData(
        CvrSource source, BaseCvrReader reader,
        int sourceIndex, int firstIndexInCvrList, int lastIndexInCvrList) {
      this.source = source;
      this.reader = reader;
      this.sourceIndex = sourceIndex;
      this.firstIndexInCvrList = firstIndexInCvrList;
      this.lastIndexInCvrList = lastIndexInCvrList;
    }

    /**
     * Get the number of CVRs in this source.
     *
     * @return The number of CVRs in this source.
     */
    public int getNumCvrs() {
      return lastIndexInCvrList - firstIndexInCvrList + 1;
    }
  }

  // Exception class used when we're unexpectedly missing snapshot data for a cast vote record
  // during CDF JSON generation. If this happens, there's a bug in the tabulation code.
  static class RoundSnapshotDataMissingException extends Exception {

    private final String cvrId;

    RoundSnapshotDataMissingException(String cvrId) {
      this.cvrId = cvrId;
    }

    String getCvrId() {
      return cvrId;
    }
  }
}
