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

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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
  void generateOverallSummarySpreadsheet(Map<Integer, Map<String, BigDecimal>> roundTallies) {
    // filename for output
    String outputFileName = String.format("%s_summary.xlsx", this.timestampString);
    // full path for output
    String outputPath =
        Paths.get(config.getOutputDirectory(), outputFileName).toAbsolutePath().toString();
    // generate the spreadsheet
    generateSummarySpreadsheet(roundTallies, null, outputPath);
  }

  // function: generatePrecinctSummarySpreadsheet
  // purpose: creates a summary spreadsheet for the votes in a particular precinct
  // param: roundTallies is map from precinct to the round-by-round vote count in the precinct
  void generatePrecinctSummarySpreadsheets(
      Map<String, Map<Integer, Map<String, BigDecimal>>> precinctRoundTallies) {
    Set<String> filenames = new HashSet<>();
    for (String precinct : precinctRoundTallies.keySet()) {
      // precinctFileString is a unique filesystem-safe string which can be used for creating
      // the precinct output filename
      String precinctFileString = getPrecinctFileString(precinct, filenames);
      // filename for output
      String outputFileName =
          String.format("%s_%s_precinct_summary.xlsx", this.timestampString, precinctFileString);
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
      Map<Integer, Map<String, BigDecimal>> roundTallies, String precinct, String outputPath) {
    Logger.log(Level.INFO, "Generating summary spreadsheet: %s", outputPath);

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

    // create the output workbook
    XSSFWorkbook workbook = new XSSFWorkbook();
    // create the output worksheet
    XSSFSheet worksheet = workbook.createSheet("Results");
    // set column widths for better readability
    worksheet.setDefaultColumnWidth(20);

    // rowCounter contains the next empty row after all the general header rows have been created.
    // This is where we start adding round-by-round reports. For precinct sheets, there are no
    // general header rows, so we just start with the round-by-round reports.
    int rowCounter = precinct != null && !precinct.isEmpty() ? 0 : addHeaderRows(worksheet);

    // Round headers:
    // firstHeaderRow is the row for round headers
    org.apache.poi.ss.usermodel.Row firstHeaderRow = worksheet.createRow(rowCounter++);

    // round indexes over all rounds plus final results round
    for (int round = 1; round <= numRounds + 1; round++) {
      // label string will have the actual text which goes in the cell
      String label = String.format("Round %d", round);
      // cell for round label
      Cell roundLabelCell = firstHeaderRow.createCell(round);
      roundLabelCell.setCellValue(label);
    }

    // actions don't make sense in individual precinct results
    if (precinct == null || precinct.isEmpty()) {
      rowCounter = addActionRows(worksheet, rowCounter);
    }

    final BigDecimal totalActiveVotesFirstRound = totalActiveVotesPerRound.get(1);

    // Candidate votes [total, delta, percentage]
    // For each candidate: for each round: output total votes, delta votes, and final vote
    // percentage of total.
    // candidate indexes over all candidates
    for (String candidate : sortedCandidates) {
      // show each candidate row with their totals for each round
      // row for the current candidate
      org.apache.poi.ss.usermodel.Row candidateRow = worksheet.createRow(rowCounter++);
      // header cell for the candidate
      Cell rowHeaderCell = candidateRow.createCell(0);
      // text for the candidate name
      String candidateDisplayName = this.config.getNameForCandidateID(candidate);
      rowHeaderCell.setCellValue(candidateDisplayName);
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
        Cell totalVotesCell = candidateRow.createCell(displayRound);
        totalVotesCell.setCellValue(thisRoundTally.toString());
      }
    }

    // row for the exhausted CVR counts
    org.apache.poi.ss.usermodel.Row exhaustedCVRRow = worksheet.createRow(rowCounter++);
    // exhausted CVR header cell
    Cell exhaustedRowHeaderCell = exhaustedCVRRow.createCell(0);
    exhaustedRowHeaderCell.setCellValue("Exhausted ballots");
    // displayRound indexes through all rounds plus final results round
    for (int displayRound = 1; displayRound <= numRounds + 1; displayRound++) {
      // flag for final round special cases
      boolean isFinalResults = displayRound == numRounds + 1;
      // data to display for this round
      int dataUseRound = isFinalResults ? numRounds : displayRound;
      // count of votes exhausted this round
      BigDecimal thisRoundExhausted = BigDecimal.ZERO;

      if (dataUseRound > 1) {
        // Exhausted count is the difference between the total votes in round 1 and the total votes
        // in the current round.
        thisRoundExhausted =
            totalActiveVotesFirstRound.subtract(totalActiveVotesPerRound.get(dataUseRound));
      }
      // total votes cell
      Cell totalVotesCell = exhaustedCVRRow.createCell(displayRound);
      totalVotesCell.setCellValue(thisRoundExhausted.toString());
    }

    // write xls to disk
    try {
      // output stream is used to write data to disk
      FileOutputStream outputStream = new FileOutputStream(outputPath);
      workbook.write(outputStream);
      outputStream.close();
    } catch (IOException exception) {
      Logger.log(Level.SEVERE, "Error saving file: %s\n%s", outputPath, exception.toString());
    }
  }

  private int addActionRows(XSSFSheet worksheet, int rowCounter) {
    // "action" row describes whether elimination(s) happened or winner(s) were selected
    // we will fill in the action cells while we iterate through
    // the candidate eliminations row since the indexing logic is identical

    // eliminationsRow will contain the eliminated candidate names
    org.apache.poi.ss.usermodel.Row eliminationsRow = worksheet.createRow(rowCounter++);
    eliminationsRow.createCell(0).setCellValue("Defeated");

    // Winner -- display is shifted to subsequent round for display
    // electedRow will contain the winning candidate(s) name
    org.apache.poi.ss.usermodel.Row winnersRow = worksheet.createRow(rowCounter++);
    winnersRow.createCell(0).setCellValue("Elected");

    for (int round = 1; round <= numRounds; round++) {
      // list of all candidates eliminated in this round
      List<String> eliminated = roundToEliminatedCandidates.get(round);
      List<String> winners = roundToWinningCandidates.get(round);
      // note we shift the eliminated candidate(s) display and action into the subsequent column
      if (eliminated != null && eliminated.size() > 0) {
        // we should never have both winners and losers in the same round
        assert !(winners != null && winners.size() > 0);

        addActionRowCandidates(round, eliminationsRow, eliminated);
      } else if (winners != null && winners.size() > 0) {
        addActionRowCandidates(round, winnersRow, winners);
      }
    }

    return rowCounter;
  }

  private void addActionRowCandidates(
      int round,
      org.apache.poi.ss.usermodel.Row candidateRow,
      List<String> candidates)
  {
    List<String> candidateDisplayNames = new ArrayList<>();
    for(String candidate : candidates) {
      candidateDisplayNames.add(config.getNameForCandidateID(candidate));
    }

    String candidateCellText = String.join("; ", candidateDisplayNames);

    // add 1 to account for the row header cell
    int columnIndex = (round) + 1;
    Cell candidateCell = candidateRow.createCell(columnIndex);
    candidateCell.setCellValue(candidateCellText);
  }

  // function: addHeaderRows
  // purpose: add header rows and cell to the top of the visualizer spreadsheet
  // param: worksheet to which we will be adding rows and cells
  // param: totalActiveVotesPerRound map of round to votes active in that round
  // returns: the next (empty) row index
  private int addHeaderRows(XSSFSheet worksheet) {

    // literal array to structure output cell text data
    // first cell is header text
    // second cell contains a value or null
    // third cell is true if second cell is numeric data, false if string
    Object[][] fields = {
      {"Contest", config.getContestName(), OutputType.STRING},
      {"Jurisdiction", config.getContestJurisdiction(), OutputType.STRING},
      {"Office", config.getContestOffice(), OutputType.STRING},
      {"Date", config.getContestDate(), OutputType.STRING},
      {null, null, OutputType.STRING},
    };
    // count the row we create so we can return the next empty row
    int rowCounter = 0;
    // index over all fields in the row structure
    for (Object[] rowFields : fields) {
      // row for the next row
      org.apache.poi.ss.usermodel.Row row = worksheet.createRow(rowCounter++);
      // create a cell if any text in the first element
      if (rowFields[0] != null) {
        row.createCell(0).setCellValue((String) rowFields[0]);
      }
      // if second element is non-null create a cell for it
      if (rowFields[1] != null) {
        if (rowFields[2] == OutputType.INT) {
          row.createCell(1).setCellValue((int) rowFields[1]);
        } else if (rowFields[2] == OutputType.FLOAT) {
          row.createCell(1).setCellValue((Float) rowFields[1]);
        } else {
          row.createCell(1).setCellValue((String) rowFields[1]);
        }
      }
    }

    return rowCounter;
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

  private enum OutputType {
    STRING,
    INT,
    FLOAT,
  }
}
