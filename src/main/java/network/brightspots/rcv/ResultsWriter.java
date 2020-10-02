/*
 * Universal RCV Tabulator
 * Copyright (c) 2017-2020 Bright Spots Developers.
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
 * Helper class takes tabulation results data as input and generates summary files which
 * contains results summary information.
 * Currently we support a CSV summary file and a JSON summary file.
 */

package network.brightspots.rcv;

import static java.util.Map.entry;
import static network.brightspots.rcv.Utils.isNullOrBlank;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javafx.util.Pair;
import network.brightspots.rcv.DominionCvrReader.Contest;
import network.brightspots.rcv.RawContestConfig.CvrSource;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

class ResultsWriter {

  private static final String CDF_CONTEST_ID = "contest-001";
  private static final String CDF_ELECTION_ID = "election-001";
  private static final String CDF_GPU_ID = "gpu-election";
  private static final String CDF_GPU_ID_FORMAT = "gpu-%d";
  private static final String CDF_REPORTING_DEVICE_ID = "rd-001";

  private static final Map<String, String> cdfCandidateCodeToContestSelectionId = new HashMap<>();
  private static final Map<String, String> cdfCandidateCodeToCandidateId = new HashMap<>();

  // number of rounds needed to elect winner(s)
  private int numRounds;
  // all precinct Ids which may appear in the output cvrs
  private Set<String> precinctIds;
  // precinct to GpUnitId map (CDF only)
  private Map<String, String> gpUnitIds;
  private BigDecimal winningThreshold;
  // map from round number to list of candidates eliminated in that round
  private Map<Integer, List<String>> roundToEliminatedCandidates;
  // map from round number to list of candidates winning in that round
  private Map<Integer, List<String>> roundToWinningCandidates;
  private ContestConfig config;
  private String timestampString;
  // map from round number to residual surplus generated in that round
  private Map<Integer, BigDecimal> roundToResidualSurplus;

  // visible for testing
  @SuppressWarnings("WeakerAccess")
  static String sequentialSuffixForOutputPath(String sequentialTabulationId) {
    return sequentialTabulationId != null ? "_" + sequentialTabulationId : "";
  }

  // visible for testing
  @SuppressWarnings("WeakerAccess")
  static String getOutputFilePath(
      String outputDirectory,
      String outputType,
      String timestampString,
      String sequentialTabulationId) {
    String fileName =
        String.format(
            "%s_%s%s",
            timestampString, outputType, sequentialSuffixForOutputPath(sequentialTabulationId));
    return Paths.get(outputDirectory, fileName).toAbsolutePath().toString();
  }

  static String sanitizeStringForOutput(String s) {
    return s.replaceAll("[^a-zA-Z0-9_\\-.]", "_");
  }

  private static void generateJsonFile(String path, Map<String, Object> json) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    // for improved legibility we sort alphabetically on keys
    mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    SimpleModule module = new SimpleModule();
    module.addSerializer(BigDecimal.class, new ToStringSerializer());
    mapper.registerModule(module);
    ObjectWriter jsonWriter = mapper.writer(new DefaultPrettyPrinter());
    File outFile = new File(path);

    try {
      jsonWriter.writeValue(outFile, json);
    } catch (IOException exception) {
      Logger.severe(
          "Error writing to JSON file: %s\n%s\nPlease check the file path and permissions!",
          path, exception);
      throw exception;
    }
    Logger.info("JSON file generated successfully.");
  }

  private static String generateCvrSnapshotId(String cvrId, Integer round) {
    return round != null && round > 0
        ? String.format("ballot-%s-round-%d", cvrId, round)
        : String.format("ballot-%s", cvrId);
  }

  // generates an internal ContestSelectionId based on a candidate code
  private static String getCdfContestSelectionIdForCandidateCode(String code) {
    String id = cdfCandidateCodeToContestSelectionId.get(code);
    if (id == null) {
      id = String.format("cs-%s", sanitizeStringForOutput(code).toLowerCase());
      cdfCandidateCodeToContestSelectionId.put(code, id);
    }
    return id;
  }

  // generates an internal CandidateId based on a candidate code
  private static String getCdfCandidateIdForCandidateCode(String code) {
    String id = cdfCandidateCodeToCandidateId.get(code);
    if (id == null) {
      id = String.format("c-%s", sanitizeStringForOutput(code).toLowerCase());
      cdfCandidateCodeToCandidateId.put(code, id);
    }
    return id;
  }

  // Instead of a map from rank to list of candidates, we need a sorted list of candidates
  // with the ranks they were given. (Ordinarily a candidate will have only a single rank, but they
  // could have multiple ranks if the ballot duplicates the candidate, i.e. assigns them multiple
  // ranks.
  // We sort by the lowest (best) rank, then alphabetically by name.
  private static List<Map.Entry<String, List<Integer>>> getCandidatesWithRanksList(
      Map<Integer, Set<String>> rankToCandidateIds) {
    Map<String, List<Integer>> candidateIdToRanks = new HashMap<>();
    // first group the ranks by candidate
    for (int rank : rankToCandidateIds.keySet()) {
      for (String candidateId : rankToCandidateIds.get(rank)) {
        candidateIdToRanks.computeIfAbsent(candidateId, k -> new LinkedList<>());
        candidateIdToRanks.get(candidateId).add(rank);
      }
    }
    // we want the ranks for a given candidate in ascending order
    for (List<Integer> list : candidateIdToRanks.values()) {
      Collections.sort(list);
    }
    List<Map.Entry<String, List<Integer>>> sortedCandidatesWithRanks =
        new LinkedList<>(candidateIdToRanks.entrySet());
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

  // return a unique, valid string for this precinct's output spreadsheet filename
  private static String getPrecinctFileString(String precinct, Set<String> filenames) {
    String sanitized = sanitizeStringForOutput(precinct);
    String filename = sanitized;
    // appendNumber is used to find a unique filename (in practice this really shouldn't be
    // necessary because different precinct names shouldn't have the same sanitized name, but we're
    // doing it here to be safe)
    int appendNumber = 2;
    while (filenames.contains(filename)) {
      filename = sanitized + "_" + appendNumber++;
    }
    filenames.add(filename);
    return filename;
  }

  private String getOutputFilePathFromInstance(String outputType) {
    String tabulationSequenceId = null;
    if (config.isMultiSeatSequentialWinnerTakesAllEnabled()) {
      int sequence = config.getSequentialWinners().size() + 1;
      tabulationSequenceId = Integer.toString(sequence);
    }
    return getOutputFilePath(
        config.getOutputDirectory(), outputType, timestampString, tabulationSequenceId);
  }

  ResultsWriter setRoundToResidualSurplus(Map<Integer, BigDecimal> roundToResidualSurplus) {
    this.roundToResidualSurplus = roundToResidualSurplus;
    return this;
  }

  ResultsWriter setNumRounds(int numRounds) {
    this.numRounds = numRounds;
    return this;
  }

  ResultsWriter setWinningThreshold(BigDecimal threshold) {
    this.winningThreshold = threshold;
    return this;
  }

  ResultsWriter setPrecinctIds(Set<String> precinctIds) {
    this.precinctIds = precinctIds;
    return this;
  }

  ResultsWriter setCandidatesToRoundEliminated(Map<String, Integer> candidatesToRoundEliminated) {
    // roundToEliminatedCandidates is the inverse of candidatesToRoundEliminated map
    // so we can look up who got eliminated for each round
    roundToEliminatedCandidates = new HashMap<>();
    for (String candidate : candidatesToRoundEliminated.keySet()) {
      int round = candidatesToRoundEliminated.get(candidate);
      roundToEliminatedCandidates.computeIfAbsent(round, k -> new LinkedList<>());
      roundToEliminatedCandidates.get(round).add(candidate);
    }
    return this;
  }

  ResultsWriter setWinnerToRound(Map<String, Integer> winnerToRound) {
    // very similar to the logic in setCandidatesToRoundEliminated above
    roundToWinningCandidates = new HashMap<>();
    for (String candidate : winnerToRound.keySet()) {
      int round = winnerToRound.get(candidate);
      roundToWinningCandidates.computeIfAbsent(round, k -> new LinkedList<>());
      roundToWinningCandidates.get(round).add(candidate);
    }
    return this;
  }

  ResultsWriter setContestConfig(ContestConfig config) {
    this.config = config;
    return this;
  }

  ResultsWriter setTimestampString(String timestampString) {
    this.timestampString = timestampString;
    return this;
  }

  // creates summary files for the votes in each precinct
  // param: roundTallies is map from precinct to the round-by-round vote count in the precinct
  // param: precinctTallyTransfers is a map from precinct to tally transfers for that precinct
  // param: numBallotsByPrecinct is the total count of ballots per precinct
  void generatePrecinctSummaryFiles(
      Map<String, Map<Integer, Map<String, BigDecimal>>> precinctRoundTallies,
      Map<String, TallyTransfers> precinctTallyTransfers,
      Map<String, Integer> numBallotsByPrecinct)
      throws IOException {
    Set<String> filenames = new HashSet<>();
    for (String precinct : precinctRoundTallies.keySet()) {
      String precinctFileString = getPrecinctFileString(precinct, filenames);
      String outputPath =
          getOutputFilePathFromInstance(String.format("%s_precinct_summary", precinctFileString));
      int numBallots = numBallotsByPrecinct.get(precinct);
      generateSummarySpreadsheet(
          precinctRoundTallies.get(precinct), numBallots, precinct, outputPath);
      generateSummaryJson(
          precinctRoundTallies.get(precinct),
          precinctTallyTransfers.get(precinct),
          precinct,
          outputPath);
    }
  }

  // create a summary spreadsheet .csv file
  // param: roundTallies is the round-by-count count of votes per candidate
  // param: precinct indicates which precinct we're reporting results for (null means all)
  private void generateSummarySpreadsheet(
      Map<Integer, Map<String, BigDecimal>> roundTallies,
      int numBallots,
      String precinct,
      String outputPath)
      throws IOException {
    String csvPath = outputPath + ".csv";
    Logger.info("Generating summary spreadsheet: %s...", csvPath);

    // totalActiveVotesPerRound is a map of round to active votes in each round
    Map<Integer, BigDecimal> totalActiveVotesPerRound = new HashMap<>();
    for (int round = 1; round <= numRounds; round++) {
      // tally is map of candidate to tally for the current round
      Map<String, BigDecimal> tallies = roundTallies.get(round);
      // total will contain total votes for all candidates in this round
      // this is used for calculating other derived data
      BigDecimal total = BigDecimal.ZERO;
      for (BigDecimal tally : tallies.values()) {
        total = total.add(tally);
      }
      totalActiveVotesPerRound.put(round, total);
    }

    CSVPrinter csvPrinter;
    try {
      BufferedWriter writer = Files.newBufferedWriter(Paths.get(csvPath));
      csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT);
    } catch (IOException exception) {
      Logger.severe(
          "Error creating CSV file: %s\n%s\nPlease check the file path and permissions!",
          csvPath, exception);
      throw exception;
    }

    addHeaderRows(csvPrinter, precinct);
    csvPrinter.print("Rounds");
    for (int round = 1; round <= numRounds; round++) {
      String label = String.format("Round %d", round);
      csvPrinter.print(label);
    }
    csvPrinter.println();

    // actions don't make sense in individual precinct results
    if (isNullOrBlank(precinct)) {
      addActionRows(csvPrinter);
    }

    // Get all candidates sorted by their first round tally. This determines the display order.
    Map<String, BigDecimal> firstRoundTally = roundTallies.get(1);
    List<String> sortedCandidates = sortCandidatesByTally(firstRoundTally);

    // For each candidate: for each round: output total votes
    for (String candidate : sortedCandidates) {
      String candidateDisplayName = config.getNameForCandidateCode(candidate);
      csvPrinter.print(candidateDisplayName);
      for (int round = 1; round <= numRounds; round++) {
        BigDecimal thisRoundTally = roundTallies.get(round).get(candidate);
        // not all candidates may have a tally in every round
        if (thisRoundTally == null) {
          thisRoundTally = BigDecimal.ZERO;
        }
        csvPrinter.print(thisRoundTally);
      }
      csvPrinter.println();
    }

    csvPrinter.print("Inactive ballots");
    for (int round = 1; round <= numRounds; round++) {
      // Exhausted/inactive count is the difference between the total ballots and the total votes
      // still active or counting as residual surplus votes in the current round.
      BigDecimal thisRoundInactive =
          new BigDecimal(numBallots).subtract(totalActiveVotesPerRound.get(round));

      if (precinct == null) {
        // We don't have the concept of residual surplus at the precinct level (see comment below),
        // so we'll just incorporate that part (if any) into the inactive count.
        thisRoundInactive = thisRoundInactive.subtract(roundToResidualSurplus.get(round));
      }
      csvPrinter.print(thisRoundInactive);
    }
    csvPrinter.println();

    // row for residual surplus (if needed)
    // We check if we accumulated any residual surplus over the course of the tabulation by testing
    // whether the value in the final round is positive.
    // Note that this concept only makes sense when we're reporting the overall tabulation, so we
    // omit it when generating results at the individual precinct level.
    if (precinct == null && roundToResidualSurplus.get(numRounds).signum() == 1) {
      csvPrinter.print("Residual surplus");
      for (int round = 1; round <= numRounds; round++) {
        csvPrinter.print(roundToResidualSurplus.get(round));
      }
      csvPrinter.println();
    }

    try {
      csvPrinter.flush();
      csvPrinter.close();
    } catch (IOException exception) {
      Logger.severe("Error saving file: %s\n%s", outputPath, exception);
      throw exception;
    }
    Logger.info("Summary spreadsheet generated successfully.");
  }

  // "action" rows describe which candidates were eliminated or elected
  private void addActionRows(CSVPrinter csvPrinter) throws IOException {
    csvPrinter.print("Eliminated");
    for (int round = 1; round <= numRounds; round++) {
      List<String> eliminated = roundToEliminatedCandidates.get(round);
      if (eliminated != null && eliminated.size() > 0) {
        addActionRowCandidates(eliminated, csvPrinter);
      } else {
        csvPrinter.print("");
      }
    }
    csvPrinter.println();

    csvPrinter.print("Elected");
    for (int round = 1; round <= numRounds; round++) {
      List<String> winners = roundToWinningCandidates.get(round);
      if (winners != null && winners.size() > 0) {
        addActionRowCandidates(winners, csvPrinter);
      } else {
        csvPrinter.print("");
      }
    }
    csvPrinter.println();
  }

  // add the given candidate(s) names to the csv file next cell
  private void addActionRowCandidates(List<String> candidates, CSVPrinter csvPrinter)
      throws IOException {
    List<String> candidateDisplayNames = new ArrayList<>();
    for (String candidate : candidates) {
      candidateDisplayNames.add(config.getNameForCandidateCode(candidate));
    }
    // use semicolon as delimiter display in a single cell
    String candidateCellText = String.join("; ", candidateDisplayNames);
    csvPrinter.print(candidateCellText);
  }

  private void addHeaderRows(CSVPrinter csvPrinter, String precinct) throws IOException {
    csvPrinter.printRecord("Contest", config.getContestName());
    csvPrinter.printRecord("Jurisdiction", config.getContestJurisdiction());
    csvPrinter.printRecord("Office", config.getContestOffice());
    csvPrinter.printRecord("Date", config.getContestDate());

    List<String> winners = new LinkedList<>();
    List<Integer> winningRounds = new ArrayList<>(roundToWinningCandidates.keySet());
    // make sure we list them in order of election
    Collections.sort(winningRounds);
    for (int round : winningRounds) {
      for (String candidateCode : roundToWinningCandidates.get(round)) {
        winners.add(config.getNameForCandidateCode(candidateCode));
      }
    }
    csvPrinter.printRecord("Winner(s)", String.join(", ", winners));
    csvPrinter.printRecord("Threshold", winningThreshold);
    if (!isNullOrBlank(precinct)) {
      csvPrinter.printRecord("Precinct", precinct);
    }
    csvPrinter.println();
  }

  // return a list of all input candidates sorted from highest tally to lowest
  private List<String> sortCandidatesByTally(Map<String, BigDecimal> tally) {
    List<Map.Entry<String, BigDecimal>> entries = new ArrayList<>(tally.entrySet());
    entries.sort(
        (firstObject, secondObject) -> {
          int ret;
          if (firstObject.getKey().equals(Tabulator.UNDECLARED_WRITE_IN_OUTPUT_LABEL)) {
            ret = 1;
          } else if (secondObject.getKey().equals(Tabulator.UNDECLARED_WRITE_IN_OUTPUT_LABEL)) {
            ret = -1;
          } else {
            ret = (secondObject.getValue()).compareTo(firstObject.getValue());
          }
          return ret;
        });
    List<String> sortedCandidates = new LinkedList<>();
    for (Map.Entry<String, BigDecimal> entry : entries) {
      sortedCandidates.add(entry.getKey());
    }
    return sortedCandidates;
  }

  // creates a summary spreadsheet and JSON for the full contest (as opposed to a precinct)
  void generateOverallSummaryFiles(
      Map<Integer, Map<String, BigDecimal>> roundTallies,
      TallyTransfers tallyTransfers,
      int numBallots)
      throws IOException {
    String outputPath = getOutputFilePathFromInstance("summary");
    generateSummarySpreadsheet(roundTallies, numBallots, null, outputPath);
    generateSummaryJson(roundTallies, tallyTransfers, null, outputPath);
  }

  // write CastVoteRecords for the specified contest to the provided folder
  // returns a list of files written
  List<String> writeGenericCvrCsv(
      List<CastVoteRecord> castVoteRecords,
      Collection<Contest> contests,
      String csvOutputFolder,
      String contestId,
      String undeclaredWriteInLabel)
      throws IOException {
    List<String> filesWritten = new ArrayList<>();
    try {
      for (Contest contest : contests) {
        if (!contest.getId().equals(contestId)) {
          // We already skipped loading CVRs for the other contests. This just ensures that we
          // don't generate empty CSVs for them.
          continue;
        }
        Path outputPath =
            Paths.get(
                getOutputFilePath(
                    csvOutputFolder,
                    "dominion_conversion_contest",
                    timestampString,
                    contest.getId())
                    + ".csv");
        Logger.info("Writing cast vote records in generic format to file: %s...", outputPath);
        CSVPrinter csvPrinter;
        BufferedWriter writer = Files.newBufferedWriter(outputPath);
        csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT);
        // print header:
        // ContestId, TabulatorId, BatchId, RecordId, Precinct, Precinct Portion, rank 1 selection,
        // rank 2 selection, ... rank maxRanks selection
        csvPrinter.print("Contest Id");
        csvPrinter.print("Tabulator Id");
        csvPrinter.print("Batch Id");
        csvPrinter.print("Record Id");
        csvPrinter.print("Precinct");
        csvPrinter.print("Precinct Portion");
        Integer numRanks = contest.getMaxRanks();
        for (int rank = 1; rank <= numRanks; rank++) {
          String label = String.format("Rank %d", rank);
          csvPrinter.print(label);
        }
        csvPrinter.println();
        // print rows:
        for (CastVoteRecord castVoteRecord : castVoteRecords) {
          csvPrinter.print(castVoteRecord.getContestId());
          csvPrinter.print(castVoteRecord.getTabulatorId());
          csvPrinter.print(castVoteRecord.getBatchId());
          csvPrinter.print(castVoteRecord.getId());
          if (castVoteRecord.getPrecinct() == null) {
            csvPrinter.print("");
          } else {
            csvPrinter.print(castVoteRecord.getPrecinct());
          }
          if (castVoteRecord.getPrecinctPortion() == null) {
            csvPrinter.print("");
          } else {
            csvPrinter.print(castVoteRecord.getPrecinctPortion());
          }
          // for each rank determine what candidate id, overvote, or undervote occurred
          for (Integer rank = 1; rank <= contest.getMaxRanks(); rank++) {
            if (castVoteRecord.rankToCandidateIds.containsKey(rank)) {
              Set<String> candidateSet = castVoteRecord.rankToCandidateIds.get(rank);
              assert !candidateSet.isEmpty();
              if (candidateSet.size() == 1) {
                String selection = candidateSet.iterator().next();
                // We map all undeclared write-ins to our constant string when we read them in,
                // so we need to translate it back to the original candidate ID here.
                if (selection.equals(Tabulator.UNDECLARED_WRITE_IN_OUTPUT_LABEL)) {
                  selection = undeclaredWriteInLabel;
                }
                csvPrinter.print(selection);
              } else {
                csvPrinter.print("overvote");
              }
            } else {
              csvPrinter.print("undervote");
            }
          }
          csvPrinter.println();
        }
        // finalize the file
        csvPrinter.flush();
        csvPrinter.close();
        filesWritten.add(outputPath.toString());
        Logger.info("Successfully wrote: %s", outputPath.toString());
      }
    } catch (IOException exception) {
      Logger.severe(
          "Error writing cast vote records in generic format from input file: %s\n%s",
          csvOutputFolder, exception);
      throw exception;
    }
    return filesWritten;
  }

  // create NIST Common Data Format CVR json
  void generateCdfJson(List<CastVoteRecord> castVoteRecords)
      throws IOException, RoundSnapshotDataMissingException {
    // generate GpUnitIds for precincts "geo-political units" (can be a precinct or jurisdiction)
    gpUnitIds = generateGpUnitIds();

    String outputPath = getOutputFilePathFromInstance("cvr_cdf") + ".json";
    Logger.info("Generating cast vote record CDF JSON file: %s...", outputPath);

    HashMap<String, Object> outputJson = new HashMap<>();
    outputJson.put("CVR", generateCdfMapForCvrs(castVoteRecords));
    outputJson.put("Election", new Map[]{generateCdfMapForElection()});
    outputJson.put(
        "GeneratedDate", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date()));
    outputJson.put("GpUnit", generateCdfMapForGpUnits());
    outputJson.put("ReportGeneratingDeviceIds", new String[]{CDF_REPORTING_DEVICE_ID});
    outputJson.put(
        "ReportingDevice",
        new Map[]{
            Map.ofEntries(
                entry("@id", CDF_REPORTING_DEVICE_ID),
                entry("@type", "CVR.ReportingDevice"),
                entry("Application", Main.APP_NAME),
                entry("Manufacturer", "Bright Spots"))
        });
    outputJson.put("Version", "1.0.0");
    outputJson.put("@type", "CVR.CastVoteRecordReport");

    generateJsonFile(outputPath, outputJson);
  }

  // build map from precinctId => GpUnitId as lookups are done by precinctId during cvr creation
  private Map<String, String> generateGpUnitIds() {
    Map<String, String> gpUnitIdToPrecinctId = new HashMap<>();
    int gpUnitIdIndex = 0;
    for (String precinctId : precinctIds) {
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
            entry("@type", "CVR.GpUnit")));

    // generate GpUnit entries
    for (Entry<String, String> entry : gpUnitIds.entrySet()) {
      gpUnitMaps.add(
          Map.ofEntries(
              entry("@id", entry.getValue()),
              entry("Type", "precinct"),
              entry("Name", entry.getKey()),
              entry("@type", "CVR.GpUnit")));
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
      cvrMap.put("@type", "CVR.CVR");
      // if using precincts add GpUnitId for cvr precinct
      if (config.isTabulateByPrecinctEnabled()) {
        String gpUnitId = gpUnitIds.get(cvr.getPrecinct());
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
        getCandidatesWithRanksList(cvr.rankToCandidateIds);

    for (Map.Entry<String, List<Integer>> candidateWithRanks : candidatesWithRanksList) {
      String candidateCode = candidateWithRanks.getKey();

      String isAllocable = "unknown";
      BigDecimal numberVotes = BigDecimal.ONE;

      if (currentRoundSnapshotData != null) {
        // scanning the list isn't actually expensive because it will almost always be very short
        for (Pair<String, BigDecimal> allocation : currentRoundSnapshotData) {
          if (allocation.getKey().equals(candidateCode)) {
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
              entry("ContestSelectionId", getCdfContestSelectionIdForCandidateCode(candidateCode)),
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
        entry("CVRContest", new Map[]{contestMap}),
        entry("Type", round != null ? "interpreted" : "original"),
        entry("@type", "CVR.CVRSnapshot"));
  }

  private Map<String, Object> generateCdfMapForElection() {
    // containers for election-level data
    List<Map<String, Object>> contestSelections = new LinkedList<>();
    List<Map<String, Object>> candidates = new LinkedList<>();

    // iterate all candidates and create Candidate and ContestSelection objects for them
    List<String> candidateCodes = new LinkedList<>(config.getCandidateCodeList());
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
      candidateCodes.add(Tabulator.EXPLICIT_OVERVOTE_LABEL);
    }
    Collections.sort(candidateCodes);
    for (String candidateCode : candidateCodes) {
      candidates.add(
          Map.ofEntries(
              entry("@id", getCdfCandidateIdForCandidateCode(candidateCode)),
              entry("Name", candidateCode)));

      contestSelections.add(
          Map.ofEntries(
              entry("@id", getCdfContestSelectionIdForCandidateCode(candidateCode)),
              entry("@type", "CVR.ContestSelection"),
              entry(
                  "CandidateIds",
                  new String[]{getCdfCandidateIdForCandidateCode(candidateCode)})));
    }

    Map<String, Object> contestJson =
        Map.ofEntries(
            entry("@id", CDF_CONTEST_ID),
            entry("@type", "CVR.CandidateContest"),
            entry("ContestSelection", contestSelections),
            entry("Name", config.getContestName()));

    HashMap<String, Object> electionMap = new HashMap<>();
    electionMap.put("@id", CDF_ELECTION_ID);
    electionMap.put("Candidate", candidates);
    electionMap.put("Contest", new Map[]{contestJson});
    electionMap.put("ElectionScopeId", CDF_GPU_ID);
    electionMap.put("@type", "CVR.Election");

    return electionMap;
  }

  // create summary json data for use with external visualizer software, unit tests and other tools
  private void generateSummaryJson(
      Map<Integer, Map<String, BigDecimal>> roundTallies,
      TallyTransfers tallyTransfers,
      String precinct,
      String outputPath)
      throws IOException {
    String jsonPath = outputPath + ".json";
    Logger.info("Generating summary JSON file: %s...", jsonPath);

    // config will contain contest configuration info
    HashMap<String, Object> configData = new HashMap<>();
    configData.put("contest", config.getContestName());
    configData.put("jurisdiction", config.getContestJurisdiction());
    configData.put("office", config.getContestOffice());
    configData.put("date", config.getContestDate());
    configData.put("threshold", winningThreshold);
    if (!isNullOrBlank(precinct)) {
      configData.put("precinct", precinct);
    }

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
      results.add(roundData);
    }
    // root outputJson dict will have two entries:
    // results - vote totals, transfers, and candidates elected / eliminated
    // config - global config into
    HashMap<String, Object> outputJson = new HashMap<>();
    outputJson.put("config", configData);
    outputJson.put("results", results);

    generateJsonFile(jsonPath, outputJson);
  }

  private Map<String, BigDecimal> updateCandidateNamesInTally(Map<String, BigDecimal> tally) {
    Map<String, BigDecimal> newTally = new HashMap<>();
    for (String key : tally.keySet()) {
      newTally.put(config.getNameForCandidateCode(key), tally.get(key));
    }
    return newTally;
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
    if (candidates != null && candidates.size() > 0) {
      // transfers contains all vote transfers for this round
      // we add one to the round since transfers are currently stored under the round AFTER
      // the tallies which triggered them
      Map<String, Map<String, BigDecimal>> roundTransfers =
          tallyTransfers.getTransfersForRound(round + 1);

      for (String candidate : candidates) {
        HashMap<String, Object> action = new HashMap<>();
        action.put(actionType, config.getNameForCandidateCode(candidate));
        if (roundTransfers != null) {
          Map<String, BigDecimal> transfersFromCandidate = roundTransfers.get(candidate);
          if (transfersFromCandidate != null) {
            // We want to replace candidate IDs with names here, too.
            Map<String, BigDecimal> translatedTransfers = new HashMap<>();
            for (String candidateId : transfersFromCandidate.keySet()) {
              // candidateName will be null for special values like "exhausted"
              String candidateName = config.getNameForCandidateCode(candidateId);
              translatedTransfers.put(
                  candidateName != null ? candidateName : candidateId,
                  transfersFromCandidate.get(candidateId));
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
