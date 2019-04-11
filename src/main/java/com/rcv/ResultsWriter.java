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
 * Helper class takes tabulation results data as input and generates summary files which
 * contains results summary information.
 * Currently we support a csv summary file and a json summary file
 */

package com.rcv;

import static java.util.Map.entry;

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
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javafx.util.Pair;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

class ResultsWriter {

  private static final String CDF_CONTEST_ID = "contest-001";
  private static final String CDF_ELECTION_ID = "election-001";
  private static final String CDF_GPU_ID = "gpu-001";
  private static final String CDF_REPORTING_DEVICE_ID = "rd-001";

  // number of rounds needed to elect winner(s)
  private int numRounds;
  // threshold to win
  private BigDecimal winningThreshold;
  // map from round number to list of candidates eliminated in that round
  private Map<Integer, List<String>> roundToEliminatedCandidates;
  // map from round number to list of candidates winning in that round
  private Map<Integer, List<String>> roundToWinningCandidates;
  // configuration file in use for this contest
  private ContestConfig config;
  // timestampString string to use when generating output file names
  private String timestampString;
  // TallyTransfer object contains totals votes transferred each round
  private TallyTransfers tallyTransfers;
  private Map<Integer, BigDecimal> roundToResidualSurplus;

  static String sequentialSuffixForOutputPath(Integer sequentialTabulationNumber) {
    return sequentialTabulationNumber != null ? "_" + sequentialTabulationNumber : "";
  }

  static String getOutputFilePath(
      String outputDirectory,
      String outputType,
      String timestampString,
      Integer sequentialTabulationNumber) {
    String fileName =
        String.format(
            "%s_%s%s",
            timestampString, outputType, sequentialSuffixForOutputPath(sequentialTabulationNumber));
    return Paths.get(outputDirectory, fileName).toAbsolutePath().toString();
  }

  private static void generateJsonFile(String path, Map<String, Object> json) throws IOException {
    // mapper converts java objects to json
    ObjectMapper mapper = new ObjectMapper();
    // set mapper to order keys alphabetically for more legible output
    mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    // create a module to contain a serializer for BigDecimal serialization
    SimpleModule module = new SimpleModule();
    module.addSerializer(BigDecimal.class, new ToStringSerializer());
    // attach serializer to mapper
    mapper.registerModule(module);

    // jsonWriter writes those object to disk
    ObjectWriter jsonWriter = mapper.writer(new DefaultPrettyPrinter());
    File outFile = new File(path);

    try {
      jsonWriter.writeValue(outFile, json);
    } catch (IOException exception) {
      Logger.log(Level.SEVERE, "Error writing to JSON file: %s\n%s", path, exception.toString());
      throw exception;
    }
  }

  private static String generateCvrSnapshotID(String cvrID, Integer round) {
    return round != null && round > 0
        ? String.format("ballot-%s-round-%d", cvrID, round)
        : String.format("ballot-%s", cvrID);
  }

  ResultsWriter setRoundToResidualSurplus(Map<Integer, BigDecimal> roundToResidualSurplus) {
    this.roundToResidualSurplus = roundToResidualSurplus;
    return this;
  }

  // function: setTallyTransfers
  // purpose: setter for tally transfer object used when generating json summary output
  // param: TallyTransfer object
  ResultsWriter setTallyTransfers(TallyTransfers tallyTransfers) {
    this.tallyTransfers = tallyTransfers;
    return this;
  }

  // function: setNumRounds
  // purpose: setter for total number of rounds
  // param: numRounds total number of rounds
  ResultsWriter setNumRounds(int numRounds) {
    this.numRounds = numRounds;
    return this;
  }

  // function: setWinningThreshold
  // purpose: setter for winning threshold
  // param: threshold to win
  ResultsWriter setWinningThreshold(BigDecimal threshold) {
    this.winningThreshold = threshold;
    return this;
  }

  // function: setCandidatesToRoundEliminated
  // purpose: setter for candidatesToRoundEliminated object
  // param: candidatesToRoundEliminated map of candidateID to round in which they were eliminated
  ResultsWriter setCandidatesToRoundEliminated(Map<String, Integer> candidatesToRoundEliminated) {
    // roundToEliminatedCandidates is the inverse of candidatesToRoundEliminated map
    // so we can look up who got eliminated for each round
    roundToEliminatedCandidates = new HashMap<>();
    // candidate is used for indexing over all candidates in candidatesToRoundEliminated
    for (String candidate : candidatesToRoundEliminated.keySet()) {
      // round is the current candidate's round of elimination
      int round = candidatesToRoundEliminated.get(candidate);
      roundToEliminatedCandidates.computeIfAbsent(round, k -> new LinkedList<>());
      roundToEliminatedCandidates.get(round).add(candidate);
    }

    return this;
  }

  // function: setWinnerToRound
  // purpose: setter for the winning candidates
  // param: map from winning candidate name to the round in which they won
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

  // function: setContestConfig
  // purpose: setter for ContestConfig object
  // param: config the ContestConfig object to use when writing results
  ResultsWriter setContestConfig(ContestConfig config) {
    this.config = config;
    return this;
  }

  // function: setTimestampString
  // purpose: setter for timestampString string used for creating output file names
  // param: timestampString string to use for creating output file names
  ResultsWriter setTimestampString(String timestampString) {
    this.timestampString = timestampString;
    return this;
  }

  // function: generatePrecinctSummarySpreadsheet
  // purpose: creates a summary spreadsheet for the votes in a particular precinct
  // param: roundTallies is map from precinct to the round-by-round vote count in the precinct
  void generatePrecinctSummarySpreadsheets(
      Map<String, Map<Integer, Map<String, BigDecimal>>> precinctRoundTallies) throws IOException {
    Set<String> filenames = new HashSet<>();
    for (String precinct : precinctRoundTallies.keySet()) {
      // precinctFileString is a unique filesystem-safe string which can be used for creating
      // the precinct output filename
      String precinctFileString = getPrecinctFileString(precinct, filenames);
      // filename for output
      String outputFileName =
          String.format("%s_%s_precinct_summary", this.timestampString, precinctFileString);
      // full path for output
      String outputPath =
          Paths.get(config.getOutputDirectory(), outputFileName).toAbsolutePath().toString();
      generateSummarySpreadsheet(precinctRoundTallies.get(precinct), precinct, outputPath);

      // generate json output
      generateSummaryJson(precinctRoundTallies.get(precinct), precinct, outputPath);
    }
  }

  // function: generateSummarySpreadsheet
  // purpose: creates a summary spreadsheet .csv file
  // param: roundTallies is the round-by-count count of votes per candidate
  // param: precinct indicates which precinct we're reporting results for (null means all)
  // param: outputPath is the full path of the file to save
  // file access: write / create
  private void generateSummarySpreadsheet(
      Map<Integer, Map<String, BigDecimal>> roundTallies, String precinct, String outputPath)
      throws IOException {
    String csvPath = outputPath + ".csv";
    Logger.log(Level.INFO, "Generating summary spreadsheets: %s...", csvPath);

    // Get all candidates sorted by their first round tally. This determines the display order.
    // container for firstRoundTally
    Map<String, BigDecimal> firstRoundTally = roundTallies.get(1);
    // candidates sorted by first round tally
    List<String> sortedCandidates = sortCandidatesByTally(firstRoundTally);

    // totalActiveVotesPerRound is a map of round to total votes cast in each round
    Map<Integer, BigDecimal> totalActiveVotesPerRound = new HashMap<>();
    // round indexes over all rounds plus final results round
    for (int round = 1; round <= numRounds; round++) {
      // tally is map of candidate to tally for the current round
      Map<String, BigDecimal> tallies = roundTallies.get(round);
      // total will contain total votes for all candidates in this round
      // this is used for calculating other derived data
      BigDecimal total = BigDecimal.ZERO;
      // tally indexes over all tallies for the current round
      for (BigDecimal tally : tallies.values()) {
        total = total.add(tally);
      }
      totalActiveVotesPerRound.put(round, total);
    }

    // csvPrinter will be used to write output to csv file
    CSVPrinter csvPrinter;
    try {
      BufferedWriter writer = Files.newBufferedWriter(Paths.get(csvPath));
      csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT);
    } catch (IOException exception) {
      Logger.log(Level.SEVERE, "Error creating CSV file: %s\n%s", csvPath, exception.toString());
      throw exception;
    }

    // print contest info
    addHeaderRows(csvPrinter, precinct);

    // add a row header for the round column labels
    csvPrinter.print("Rounds");
    // round indexes over all rounds
    for (int round = 1; round <= numRounds; round++) {
      // label string will have the actual text which goes in the cell
      String label = String.format("Round %d", round);
      // cell for round label
      csvPrinter.print(label);
    }
    csvPrinter.println();

    // actions don't make sense in individual precinct results
    if (precinct == null || precinct.isEmpty()) {
      addActionRows(csvPrinter);
    }

    final BigDecimal totalActiveVotesFirstRound = totalActiveVotesPerRound.get(1);

    // For each candidate: for each round: output total votes
    // candidate indexes over all candidates
    for (String candidate : sortedCandidates) {
      // show each candidate row with their totals for each round
      // text for the candidate name
      String candidateDisplayName = this.config.getNameForCandidateID(candidate);
      csvPrinter.print(candidateDisplayName);

      // round indexes over all rounds
      for (int round = 1; round <= numRounds; round++) {
        // vote tally this round
        BigDecimal thisRoundTally = roundTallies.get(round).get(candidate);
        // not all candidates may have a tally in every round
        if (thisRoundTally == null) {
          thisRoundTally = BigDecimal.ZERO;
        }
        // total votes cell
        csvPrinter.print(thisRoundTally.toString());
      }
      // advance to next line
      csvPrinter.println();
    }

    // row for the inactive CVR counts
    // inactive CVR header cell
    csvPrinter.print("Inactive ballots");

    // round indexes through all rounds
    for (int round = 1; round <= numRounds; round++) {
      // count of votes inactive this round
      BigDecimal thisRoundInactive = BigDecimal.ZERO;

      if (round > 1) {
        // Exhausted count is the difference between the total votes in round 1 and the total votes
        // in the current round.
        thisRoundInactive =
            totalActiveVotesFirstRound
                .subtract(totalActiveVotesPerRound.get(round))
                .subtract(roundToResidualSurplus.get(round));
      }
      // total votes cell
      csvPrinter.print(thisRoundInactive.toString());
    }
    csvPrinter.println();

    // row for residual surplus (if needed)
    // We check if we accumulated any residual surplus over the course of the tabulation by testing
    // whether the value in the final round is positive.
    if (roundToResidualSurplus.get(numRounds).signum() == 1) {
      csvPrinter.print("Residual surplus");
      for (int round = 1; round <= numRounds; round++) {
        csvPrinter.print(roundToResidualSurplus.get(round).toString());
      }
      csvPrinter.println();
    }

    // write xls to disk
    try {
      // output stream is used to write data to disk
      csvPrinter.flush();
      csvPrinter.close();
    } catch (IOException exception) {
      Logger.log(Level.SEVERE, "Error saving file: %s\n%s", outputPath, exception.toString());
      throw exception;
    }
  }

  // function: addActionRows
  // "action" rows describe which candidates were eliminated or elected
  // purpose: output rows to csv file describing which actions were taken in each round
  // param: csvPrinter object for writing csv file
  private void addActionRows(CSVPrinter csvPrinter) throws IOException {
    // print eliminated candidates in first action row
    csvPrinter.print("Eliminated");
    // for each round print any candidates who were eliminated
    for (int round = 1; round <= numRounds; round++) {
      // list of all candidates eliminated in this round
      List<String> eliminated = roundToEliminatedCandidates.get(round);
      if (eliminated != null && eliminated.size() > 0) {
        addActionRowCandidates(eliminated, csvPrinter);
      } else {
        csvPrinter.print("");
      }
    }
    csvPrinter.println();

    // print elected candidates in second action row
    csvPrinter.print("Elected");
    // for each round print any candidates who were elected
    for (int round = 1; round <= numRounds; round++) {
      // list of all candidates eliminated in this round
      List<String> winners = roundToWinningCandidates.get(round);
      if (winners != null && winners.size() > 0) {
        addActionRowCandidates(winners, csvPrinter);
      } else {
        csvPrinter.print("");
      }
    }
    csvPrinter.println();
  }

  // function: addActionRowCandidates
  // purpose: add the given candidate(s) names to the csv file next cell
  // param: candidates list of candidate names to add to the next cell
  // param: csvPrinter object for output to csv file
  private void addActionRowCandidates(List<String> candidates, CSVPrinter csvPrinter)
      throws IOException {
    List<String> candidateDisplayNames = new ArrayList<>();
    // build list of display names
    for (String candidate : candidates) {
      candidateDisplayNames.add(config.getNameForCandidateID(candidate));
    }
    // concatenate them using semi-colon for display in a single cell
    String candidateCellText = String.join("; ", candidateDisplayNames);
    // print the candidate name list
    csvPrinter.print(candidateCellText);
  }

  // function: addHeaderRows
  // purpose: add arbitrary header rows and cell to the top of the visualizer spreadsheet
  // param: worksheet to which we will be adding rows and cells
  // param: totalActiveVotesPerRound map of round to votes active in that round
  private void addHeaderRows(CSVPrinter csvPrinter, String precinct) throws IOException {
    csvPrinter.printRecord("Contest", config.getContestName());
    csvPrinter.printRecord("Jurisdiction", config.getContestJurisdiction());
    csvPrinter.printRecord("Office", config.getContestOffice());
    csvPrinter.printRecord("Date", config.getContestDate());
    csvPrinter.printRecord("Threshold", winningThreshold.toString());
    if (precinct != null && !precinct.isEmpty()) {
      csvPrinter.printRecord("Precinct", precinct);
    }
    csvPrinter.println();
  }

  // function: sortCandidatesByTally
  // purpose: given a map of candidates to tally return a list of all input candidates
  // sorted from highest tally to lowest
  // param: tally map of candidateID to tally
  // return: list of all input candidates sorted from highest tally to lowest
  private List<String> sortCandidatesByTally(Map<String, BigDecimal> tally) {
    // entries will contain all the input tally entries in sorted order
    List<Map.Entry<String, BigDecimal>> entries = new ArrayList<>(tally.entrySet());
    // anonymous custom comparator will sort undeclared write in candidates to last place
    entries.sort(
        (firstObject, secondObject) -> {
          // result of the comparison
          int ret;

          if (firstObject.getKey().equals(config.getUndeclaredWriteInLabel())) {
            ret = 1;
          } else if (secondObject.getKey().equals(config.getUndeclaredWriteInLabel())) {
            ret = -1;
          } else {
            ret = (secondObject.getValue()).compareTo(firstObject.getValue());
          }

          return ret;
        });
    // container list for the final results
    List<String> sortedCandidates = new LinkedList<>();
    // index over all entries
    for (Map.Entry<String, BigDecimal> entry : entries) {
      sortedCandidates.add(entry.getKey());
    }
    return sortedCandidates;
  }

  // function: getPrecinctFileString
  // purpose: return a unique, valid string for this precinct's output spreadsheet filename
  // param: precinct is the name of the precinct
  // param: filenames is the set of filenames we've already generated
  // return: the new filename
  private String getPrecinctFileString(String precinct, Set<String> filenames) {
    // sanitized is the precinct name with all special characters converted to underscores
    String sanitized = precinct.replaceAll("[^a-zA-Z0-9._\\-]+", "_");
    // filename is the string that we'll eventually return
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

  // function: generateOverallSummaryFiles
  // purpose: creates a summary spreadsheet and JSON for the full contest
  // param: roundTallies is the round-by-round count of votes per candidate
  void generateOverallSummaryFiles(Map<Integer, Map<String, BigDecimal>> roundTallies)
      throws IOException {
    String outputPath =
        getOutputFilePath(
            config.getOutputDirectory(),
            "summary",
            timestampString,
            config.isSequentialMultiSeatEnabled()
                ? config.getSequentialWinners().size() + 1
                : null);
    // generate the spreadsheet
    generateSummarySpreadsheet(roundTallies, null, outputPath);

    // generate json output
    generateSummaryJson(roundTallies, null, outputPath);
  }

  void generateCdfJson(List<CastVoteRecord> castVoteRecords)
      throws IOException, RoundSnapshotDataMissingException {
    HashMap<String, Object> outputJson = new HashMap<>();

    String outputPath =
        getOutputFilePath(
            config.getOutputDirectory(),
            "cvr_cdf",
            timestampString,
            config.isSequentialMultiSeatEnabled()
                ? config.getSequentialWinners().size() + 1
                : null)
            + ".json";

    Logger.log(Level.INFO, "Generating CVR CDF JSON file: %s...", outputPath);

    outputJson.put("CVR", generateCdfMapForCvrs(castVoteRecords));
    outputJson.put("Election", new Map[]{generateCdfMapForElection()});
    outputJson.put(
        "GeneratedDate", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date()));
    outputJson.put(
        "GpUnit",
        new Map[]{
            Map.ofEntries(
                entry("@id", CDF_GPU_ID),
                entry("OtherType", "election scope gpunit"),
                entry("Type", "other"),
                entry("@type", "CVR.GpUnit"))
        });
    outputJson.put("ReportGeneratingDeviceIds", new String[]{CDF_REPORTING_DEVICE_ID});
    outputJson.put(
        "ReportingDevice",
        new Map[]{
            Map.ofEntries(
                entry("@id", CDF_REPORTING_DEVICE_ID), entry("@type", "CVR.ReportingDevice"))
        });
    outputJson.put("Version", "1.0.0");
    outputJson.put("@type", "CVR.CastVoteRecordReport");

    generateJsonFile(outputPath, outputJson);
  }

  // purpose: helper method for generateCdfJson to compile the data for all the CVR snapshots
  private List<Map<String, Object>> generateCdfMapForCvrs(List<CastVoteRecord> castVoteRecords)
      throws RoundSnapshotDataMissingException {
    List<Map<String, Object>> cvrMaps = new LinkedList<>();

    for (CastVoteRecord cvr : castVoteRecords) {
      List<Map<String, Object>> cvrSnapshots = new LinkedList<>();
      cvrSnapshots.add(generateCvrSnapshotMap(cvr, null, null));

      List<Pair<String, BigDecimal>> previousRoundSnapshotData = null;
      for (int round = 1; round <= numRounds; round++) {
        List<Pair<String, BigDecimal>> currentRoundSnapshotData =
            cvr.getCdfSnapshotData().get(round);
        if (currentRoundSnapshotData == null) {
          if (previousRoundSnapshotData == null) {
            throw new RoundSnapshotDataMissingException(cvr.getID());
          }
          currentRoundSnapshotData = previousRoundSnapshotData;
        }
        cvrSnapshots.add(generateCvrSnapshotMap(cvr, round, currentRoundSnapshotData));
        previousRoundSnapshotData = currentRoundSnapshotData;
      }

      cvrMaps.add(
          Map.ofEntries(
              entry("BallotPrePrintedId", cvr.getID()),
              entry("CurrentSnapshotId", generateCvrSnapshotID(cvr.getID(), numRounds)),
              entry("CVRSnapshot", cvrSnapshots),
              entry("ElectionId", CDF_ELECTION_ID),
              entry("@type", "CVR.CVR")));
    }

    return cvrMaps;
  }

  // purpose: helper for generateCdfMapForCvrs to handle a single CVR in a single round
  private Map<String, Object> generateCvrSnapshotMap(
      CastVoteRecord cvr, Integer round, List<Pair<String, BigDecimal>> currentRoundSnapshotData) {
    List<Map<String, Object>> selectionMapList = new LinkedList<>();
    for (int rank : cvr.rankToCandidateIDs.keySet()) {
      for (String candidate : cvr.rankToCandidateIDs.get(rank)) {
        String isAllocable = "unknown";
        BigDecimal numberVotes = BigDecimal.ONE;
        if (currentRoundSnapshotData != null) {
          // scanning the list isn't actually expensive because it will almost always be very short
          for (Pair<String, BigDecimal> allocation : currentRoundSnapshotData) {
            if (allocation.getKey().equals(candidate)) {
              isAllocable = "yes";
              numberVotes = allocation.getValue();
              break;
            }
          }
          if (isAllocable.equals("unknown")) {
            isAllocable = "no";
            // not sure what numberVotes should be in this situation
          }
        }
        Map<String, Object> selectionPositionMap =
            Map.ofEntries(
                entry("HasIndication", "yes"),
                entry("IsAllocable", isAllocable),
                entry("NumberVotes", numberVotes),
                entry("Rank", rank),
                entry("@type", "CVR.SelectionPosition"));

        selectionMapList.add(
            Map.ofEntries(
                entry("ContestSelectionId", candidate),
                entry("SelectionPosition", new Map[]{selectionPositionMap}),
                entry("@type", "CVR.CVRContestSelection")));
      }
    }

    Map<String, Object> contestMap =
        Map.ofEntries(
            entry("ContestId", CDF_CONTEST_ID),
            entry("CVRContestSelection", selectionMapList),
            entry("@type", "CVR.CVRContest"));

    Map<String, Object> snapshotMap =
        Map.ofEntries(
            entry("@id", generateCvrSnapshotID(cvr.getID(), round)),
            entry("CVRContest", new Map[]{contestMap}),
            entry("Type", round != null ? "interpreted" : "original"),
            entry("@type", "CVR.CVRSnapshot"));

    return snapshotMap;
  }

  private Map<String, Object> generateCdfMapForElection() {
    HashMap<String, Object> electionMap = new HashMap<>();

    List<Map<String, String>> contestSelections = new LinkedList<>();
    for (String candidate : config.getCandidateCodeList()) {
      contestSelections.add(
          Map.ofEntries(entry("@id", candidate), entry("@type", "CVR.ContestSelection")));
    }

    Map<String, Object> contestJson =
        Map.ofEntries(
            entry("@id", CDF_CONTEST_ID),
            entry("ContestSelection", contestSelections),
            entry("@type", "CVR.CandidateContest"));

    electionMap.put("@id", CDF_ELECTION_ID);
    electionMap.put("Contest", new Map[]{contestJson});
    electionMap.put("ElectionScopeId", CDF_GPU_ID);
    electionMap.put("@type", "CVR.Election");

    return electionMap;
  }

  // function: generateSummaryJson
  // purpose: create summary json data for use in visualizer, unit tests and other tools
  // param: outputPath where to write json file
  // param: roundTallies all tally information
  // file access: write to outputPath
  private void generateSummaryJson(
      Map<Integer, Map<String, BigDecimal>> roundTallies, String precinct, String outputPath)
      throws IOException {
    String jsonPath = outputPath + ".json";
    Logger.log(Level.INFO, "Generating summary JSON file: %s...", jsonPath);

    // root outputJson dict will have two entries:
    // results - vote totals, transfers, and candidates elected / eliminated
    // config - global config into
    HashMap<String, Object> outputJson = new HashMap<>();
    // config will contain contest configuration info
    HashMap<String, Object> configData = new HashMap<>();
    // add config header info
    configData.put("contest", config.getContestName());
    configData.put("jurisdiction", config.getContestJurisdiction());
    configData.put("office", config.getContestOffice());
    configData.put("date", config.getContestDate());
    configData.put("threshold", winningThreshold);
    if (precinct != null && !precinct.isEmpty()) {
      configData.put("precinct", precinct);
    }
    // results will be a list of round data objects
    ArrayList<Object> results = new ArrayList<>();
    // for each round create objects for json serialization
    for (int round = 1; round <= numRounds; round++) {
      // container for all json data this round:
      HashMap<String, Object> roundData = new HashMap<>();
      // add round number (this is implied by the ordering but for debugging we are explicit)
      roundData.put("round", round);
      // add actions if this is not a precinct summary
      if (precinct == null || precinct.isEmpty()) {
        // actions is a list of one or more action objects
        ArrayList<Object> actions = new ArrayList<>();
        addActionObjects("elected", roundToWinningCandidates.get(round), round, actions);
        // add any elimination actions
        addActionObjects("eliminated", roundToEliminatedCandidates.get(round), round, actions);
        // add action objects
        roundData.put("tallyResults", actions);
      }
      // add tally object
      roundData.put("tally", updateCandidateNamesInTally(roundTallies.get(round)));
      // add roundData to results list
      results.add(roundData);
    }
    // add config data to root object
    outputJson.put("config", configData);
    // add results to root object
    outputJson.put("results", results);

    // write results to disk
    generateJsonFile(jsonPath, outputJson);
  }

  private Map<String, BigDecimal> updateCandidateNamesInTally(Map<String, BigDecimal> tally) {
    Map<String, BigDecimal> newTally = new HashMap<>();
    for (String key : tally.keySet()) {
      newTally.put(config.getNameForCandidateID(key), tally.get(key));
    }
    return newTally;
  }

  // function: addActionObjects
  // purpose: adds action objects to input action list representing all actions applied this round
  //  each action will have a type followed by a list of 0 or more vote transfers
  //  (sometimes there is no vote transfer if a candidate had no votes to transfer)
  // param: actionType is this an elimination or election action
  // param: candidates list of all candidates action is applied to
  // param: round which this action occurred
  // param: actions list to add new action objects to
  private void addActionObjects(
      String actionType, List<String> candidates, int round, ArrayList<Object> actions) {
    // check for valid candidates:
    // "drop undeclared write-in" may result in no one actually being eliminated
    if (candidates != null && candidates.size() > 0) {
      // transfers contains all vote transfers for this round
      // we add one to the round since transfers are currently stored under the round AFTER
      // the tallies which triggered them
      Map<String, Map<String, BigDecimal>> roundTransfers =
          tallyTransfers.getTransfersForRound(round + 1);

      // candidate iterates over all candidates who had this action applied to them
      for (String candidate : candidates) {
        // for each candidate create an action object
        HashMap<String, Object> action = new HashMap<>();
        // add the specified action type
        action.put(actionType, config.getNameForCandidateID(candidate));
        // check if there are any transfers
        if (roundTransfers != null) {
          Map<String, BigDecimal> transfersFromCandidate = roundTransfers.get(candidate);
          if (transfersFromCandidate != null) {
            // add transfers
            action.put("transfers", transfersFromCandidate);
          }
        }
        if (!action.containsKey("transfers")) {
          // add an empty map
          action.put("transfers", new HashMap<String, BigDecimal>());
        }
        // add the action object to list
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
