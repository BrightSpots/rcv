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
 * Helper class takes tabulation results data as input and generates results xls file which
 * contains results summary information. Currently we support an xlsx spreadsheet which can be
 * visualized in a web browser.
 */

package com.rcv;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;


class ResultsWriter {

  // number of round needed to elect winner(s)
  private int numRounds;
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

  // function: generateOverallSummarySpreadsheet
  // purpose: creates a summary spreadsheet for the full contest
  // param: roundTallies is the round-by-round count of votes per candidate
  void generateOverallSummarySpreadsheet(Map<Integer, Map<String, BigDecimal>> roundTallies)
      throws IOException {
    // filename for output
    String outputFileName = String.format("%s_summary", this.timestampString);
    // full path for output
    String outputPath =
        Paths.get(config.getOutputDirectory(), outputFileName).toAbsolutePath().toString();
    // generate the spreadsheet
    generateSummarySpreadsheet(roundTallies, null, outputPath);

    // generate json output
    generateSummaryJson(outputPath, roundTallies);
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
    }
  }

  // function: generateSummarySpreadsheet
  // purpose: creates a summary spreadsheet .xlsx file
  // param: roundTallies is the round-by-count count of votes per candidate
  // param: precinct indicates which precinct we're reporting results for (null means all)
  // param: outputPath is the full path of the file to save
  // file access: write / create
  private void generateSummarySpreadsheet(
      Map<Integer, Map<String, BigDecimal>> roundTallies, String precinct, String outputPath)
      throws IOException {
    String csvPath = outputPath+".csv";
    Logger.log(Level.INFO, "Generating summary spreadsheets: %s", csvPath);

    // Get all candidates sorted by their first round tally. This determines the display order.
    // container for firstRoundTally
    Map<String, BigDecimal> firstRoundTally = roundTallies.get(1);
    // candidates sorted by first round tally
    List<String> sortedCandidates = sortCandidatesByTally(firstRoundTally);

    // totalActiveVotesPerRound is a map of round to total votes cast in each round
    // this will be used to calculate the percentage of total votes each candidate achieves
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
      Logger.log(Level.SEVERE, "Error creating csv file: %s\n%s", csvPath, exception.toString());
      throw exception;
    }

    // print contest info
    addHeaderRows(csvPrinter, precinct);

    // add a row header for the round column labels
    csvPrinter.print("rounds");
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

    // Candidate votes [total, delta, percentage]
    // For each candidate: for each round: output total votes, delta votes, and final vote
    // percentage of total.
    // candidate indexes over all candidates
    for (String candidate : sortedCandidates) {
      // show each candidate row with their totals for each round
      // row for the current candidate
      // text for the candidate name
      String candidateDisplayName = this.config.getNameForCandidateID(candidate);
      csvPrinter.print(candidateDisplayName);

      // displayRound indexes over all rounds plus final results round
      for (int displayRound = 1; displayRound <= numRounds + 1; displayRound++) {
        // flag for the last round results which are special-cased
        boolean isFinalResults = displayRound == numRounds + 1;
        // For the Final Results "round", we're mostly copying the data from the final round.
        // round from which to display data
        int dataUseRound = isFinalResults ? numRounds : displayRound;
        // vote tally this round
        BigDecimal thisRoundTally = roundTallies.get(dataUseRound).get(candidate);
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

    // row for the exhausted CVR counts
    // exhausted CVR header cell
    csvPrinter.print("Exhausted ballots");

    // displayRound indexes through all rounds
    for (int displayRound = 1; displayRound <= numRounds; displayRound++) {
      // count of votes exhausted this round
      BigDecimal thisRoundExhausted = BigDecimal.ZERO;

      if (displayRound > 1) {
        // Exhausted count is the difference between the total votes in round 1 and the total votes
        // in the current round.
        thisRoundExhausted =
            totalActiveVotesFirstRound.subtract(totalActiveVotesPerRound.get(displayRound));
      }
      // total votes cell
      csvPrinter.print(thisRoundExhausted.toString());
    }
    csvPrinter.println();

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

  // "action" rows describe which candidates were eliminated or elected
  private void addActionRows(CSVPrinter csvPrinter) throws IOException {

    // losers
    csvPrinter.print("Eliminated");
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

    // winners
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

  private void addActionRowCandidates(List<String> candidates,
      CSVPrinter csvPrinter)
      throws IOException {
    List<String> candidateDisplayNames = new ArrayList<>();
    // build list of display names
    for(String candidate : candidates) {
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

  // function: generateSummaryJson
  // purpose: create summary json data for use in visualizer, unit tests and other tools
  // param: outputPath where to write json file
  // param: roundTallies all tally information
  void generateSummaryJson(String outputPath, Map<Integer, Map<String, BigDecimal>> roundTallies)
      throws IOException {
    // mapper converts java objects to json
    ObjectMapper mapper = new ObjectMapper();
    // jsonWriter writes those object to disk
    ObjectWriter jsonWriter = mapper.writer(new DefaultPrettyPrinter());
    // jsonPath for output json summary
    String jsonPath = outputPath+".json";
    // log output location
    Logger.log(Level.INFO, "Generating summary json: %s", jsonPath);
    // outFile is the target file
    File outFile = new File(jsonPath);

    // root object dict will have two entries:
    // results - vote totals, transfers, and candidates elected / defeated
    // TODO: add needed config info
    // config - global config into
    HashMap<String, Object> outputJson = new HashMap<>();
    // results will be a list of round data objects
    ArrayList<Object> results = new ArrayList<>();
    // for each round create objects for json serialization
    for(Integer round = 1; round <= numRounds; round++) {
      // actions is a list of one or more action objects
      ArrayList<Object> actions = new ArrayList<>();
      // add any winning actions
      addActionObjects("elected", roundToWinningCandidates.get(round), round, actions);
      // add any defeated actions
      addActionObjects("defeated", roundToEliminatedCandidates.get(round), round, actions);

      // container for all json data this round:
      HashMap<String, Object> roundData = new HashMap<>();
      // add round number (this is implied by the ordering but for debugging we are explicit)
      roundData.put("round",round);
      // add actions
      roundData.put("actions", actions);
      // add tally
      roundData.put("tallies", roundTallies.get(round));
      // add roundData to results list
      results.add(roundData);
    }
    // add results to root object
    outputJson.put("results", results);

    // write results to disk
    try {
      jsonWriter.writeValue(outFile, outputJson);
    } catch (IOException exception) {
      Logger.log(Level.SEVERE, "Error writing to json file:%s\n%s", jsonPath, exception.toString());
      throw exception;
    }
  }

  // adds action objects to input action list representing all actions applied this round
  // each action will have a type followed by a list of 0 or more vote transfers
  // (sometimes there is no vote transfer if a candidate had no votes to transfer)
  private void addActionObjects(String actionType,
      List<String> candidates,
      Integer round,
      ArrayList<Object> actions) {

    // check for valid candidates:
    // "drop undeclared write-in" may result in no one actually being eliminated
    if (candidates != null && candidates.size() > 0) {

      // transfers contains all vote transfers for this round
      // we add one to the round since transfers are currently stored under the round AFTER
      // the tallies which triggered them
      Map<String, Map<String, BigDecimal>> roundTransfers =
          this.tallyTransfers.getTransfersForRound(round+1);

      // candidate iterates over all candidates who had this action applied to them
      for(String candidate : candidates) {
        // for each candidate create an action object
        HashMap<String, Object> action = new HashMap<>();
        // add the specified action type
        action.put(actionType, config.getNameForCandidateID(candidate));
        // check if there are any transfers
        if(roundTransfers != null) {
          Map<String, BigDecimal> transfersFromCandidate = roundTransfers.get(candidate);
          if(transfersFromCandidate != null) {
            // add transfers
            action.put("transfers", transfersFromCandidate);
          }
        }
        // add the action object to list
        actions.add(action);
      }
    }
  }

}
