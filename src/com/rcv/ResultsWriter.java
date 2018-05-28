/*
 * Created by Jonathan Moldover, Louis Eisenberg, and Hylton Edingfield
 * Copyright 2018 Bright Spots
 * Helper class takes tabulation results data as input and generates results xls file which
 * contains results summary information.
 * Currently we support an xlsx spreadsheet which can be visualized in a web browser
 * Version: 1.0
 */

package com.rcv;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

class ResultsWriter {

  // each round has three columns of data for each candidate:
  // - change in votes for candidate compared to previous round
  // - total votes for candidate in current round
  // - candidate percentage of total active votes in current round
  private static final int COLUMNS_PER_ROUND = 3;
  // number of round needed to declare a winner
  private int numRounds;
  // map from round number to list of candidates eliminated in that round
  private Map<Integer, List<String>> roundToEliminatedCandidates;
  // map from round number to list of candidates winning in that round
  private Map<Integer, List<String>> roundToWinningCandidates;
  // configuration file in use for this election
  private ElectionConfig config;
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
  ResultsWriter setCandidatesToRoundEliminated(
      Map<String, Integer> candidatesToRoundEliminated
  ) {
    // roundToEliminatedCandidates is the inverse of candidatesToRoundEliminated map
    // so we can look up who got eliminated for each round
    roundToEliminatedCandidates = new HashMap<>();
    // candidate is used for indexing over all candidates in candidatesToRoundEliminated
    for (String candidate : candidatesToRoundEliminated.keySet()) {
      // round is the current candidate's round of elimination
      int round = candidatesToRoundEliminated.get(candidate);
      if (roundToEliminatedCandidates.get(round) == null) {
        roundToEliminatedCandidates.put(round, new LinkedList<>());
      }
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
      if (roundToWinningCandidates.get(round) == null) {
        roundToWinningCandidates.put(round, new LinkedList<>());
      }
      roundToWinningCandidates.get(round).add(candidate);
    }
    return this;
  }

  // function: setElectionConfig
  // purpose: setter for ElectionConfig object
  // param: config the ElectionConfig object to use when writing results
  ResultsWriter setElectionConfig(ElectionConfig config) {
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
  // purpose: creates a summary spreadsheet for the full election
  // param: roundTallies is the round-by-round count of votes per candidate
  void generateOverallSummarySpreadsheet(
      Map<Integer, Map<String, BigDecimal>> roundTallies
  ) {
    // filename for output
    String outputFileName = String.format("%s.xlsx", this.timestampString);
    // full path for output
    String outputPath = Paths.get(config.getOutputDirectory(), outputFileName ).toString();
    // generate the spreadsheet
    generateSummarySpreadsheet(roundTallies, null, outputPath);
  }

  // function: generatePrecinctSummarySpreadsheet
  // purpose: creates a summary spreadsheet for the votes in a particular precinct
  // param: roundTallies is map from precinct to the round-by-round vote count in the precinct
  void generatePrecinctSummarySpreadsheets(
      Map<String, Map<Integer, Map<String, BigDecimal>>> precinctRoundTallies
  ) {
    Set<String> filenames = new HashSet<>();
    for (String precinct : precinctRoundTallies.keySet()) {
      // precinctFileString is a unique filesystem-safe string which can be used for creating
      // the precinct output filename
      String precinctFileString = getPrecinctFileString(precinct, filenames);
      // filename for output
      String outputFileName = String.format("%s_%s.xlsx", this.timestampString, precinctFileString);
      // full path for output
      String outputPath = Paths.get(config.getOutputDirectory(), outputFileName).toString();
      generateSummarySpreadsheet(
          precinctRoundTallies.get(precinct),
          precinct,
          outputPath
      );
    }
  }

  // function: generateSummarySpreadsheet
  // purpose: creates a summary spreadsheet .xlsx file
  // param: roundTallies is the round-by-count count of votes per candidate
  // param: precinct indicates which precinct we're reporting results for (null means all)
  // param: outputPath is the full path of the file to save
  // file access: write / create
  private void generateSummarySpreadsheet(
      Map<Integer, Map<String, BigDecimal>> roundTallies,
      String precinct,
      String outputPath
  ) {
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

    // rowCounter contains the next empty row after all the general header rows have been created.
    // This is where we start adding round-by-round reports. For precinct sheets, there are no
    // general header rows, so we just start with the round-by-round reports.
    int rowCounter = precinct != null ? 0 : addHeaderRows(worksheet, totalActiveVotesPerRound);

    // column indexes are computed for all cells as we create the output xlsx spreadsheet
    int columnIndex;

    // Round headers:
    // firstHeaderRow is the row for round headers
    org.apache.poi.ss.usermodel.Row firstHeaderRow = worksheet.createRow(rowCounter++);
    // the round header title cell will be used to create all the round headers
    Cell roundTitleHeaderCell = firstHeaderRow.createCell(0);
    roundTitleHeaderCell.setCellValue("Round Title");

    // round indexes over all rounds plus final results round
    for (int round = 1; round <= numRounds + 1; round++) {
      // Compute column index: convert from 1-based to 0-based indexing for rounds,
      // then multiply by COLUMNS_PER_ROUND to get the start of the grouping,
      // then add 1 to skip the row header cell.
      columnIndex = ((round - 1) * COLUMNS_PER_ROUND) + 1;
      // label string will have the actual text which goes in the cell
      String label;
      if (round == 1) {
        label = "Initial Count";
      } else if (round == numRounds + 1) {
        label = "Final Results";
      } else {
        label = String.format("Round %d", round);
      }
      for (int i = 0; i < COLUMNS_PER_ROUND; i++) {
        // cell for round label
        Cell roundLabelCell = firstHeaderRow.createCell(columnIndex++);
        roundLabelCell.setCellValue(label);
      }
    }

    if (precinct == null) { // actions don't make sense in individual precinct results
      rowCounter = addActionRows(worksheet, rowCounter);
    }

    // votesRedistributedRow is for the number of votes redistributed per round. We'll fill it
    // in after we tabulate all the candidates' data.
    org.apache.poi.ss.usermodel.Row votesRedistributedRow = worksheet.createRow(rowCounter++);
    // cell for votes redistributed header text
    Cell votesRedistributedHeaderCell = votesRedistributedRow.createCell(0);
    votesRedistributedHeaderCell.setCellValue("Votes redistributed");
    // array for calculating the votes redistributed between rounds
    BigDecimal[] votesRedistributedEachRound = new BigDecimal[numRounds + 1];
    Arrays.fill(votesRedistributedEachRound, BigDecimal.ZERO);

    // secondHeaderRow will be the row object for vote total, change, percentage headers for each round
    org.apache.poi.ss.usermodel.Row secondHeaderRow = worksheet.createRow(rowCounter++);
    // container for candidate name
    Cell candidateNameCell = secondHeaderRow.createCell(0);
    candidateNameCell.setCellValue("Candidate Name");
    // round indexes over all rounds plus final results round
    for (int round = 1; round <= numRounds + 1; round++) {
      columnIndex = ((round - 1) * COLUMNS_PER_ROUND) + 1;
      // cell for round delta header
      Cell roundDeltaCell = secondHeaderRow.createCell(columnIndex);
      roundDeltaCell.setCellValue("Vote change");
      columnIndex++;
      // round total header cell
      Cell roundTotalCell = secondHeaderRow.createCell(columnIndex++);
      // text for the round total header cell
      String roundTotalText = (round == 1) ? "First preferences" : "Result of round";
      roundTotalCell.setCellValue(roundTotalText);
      // cell for round percentage header cell
      Cell roundPercentageCell = secondHeaderRow.createCell(columnIndex);
      roundPercentageCell.setCellValue("% of vote");
    }

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
        // previous round tally for calculating deltas
        BigDecimal prevRoundTally = BigDecimal.ZERO;
        if (dataUseRound > 1) {
          prevRoundTally = roundTallies.get(dataUseRound - 1).get(candidate);
          if (prevRoundTally == null) {
            prevRoundTally = BigDecimal.ZERO;
          }
        }
        // vote tally delta
        BigDecimal deltaVotes = BigDecimal.ZERO;
        if (!isFinalResults && thisRoundTally.compareTo(prevRoundTally) != 0) {
          deltaVotes = thisRoundTally.subtract(prevRoundTally);
        }

        // accumulate total votes redistributed
        if (deltaVotes.signum() == 1) { // count all the positive redistributions
          votesRedistributedEachRound[dataUseRound] =
              votesRedistributedEachRound[dataUseRound].add(deltaVotes);
        }
        //  total active votes in this round
        BigDecimal totalActiveVotes = totalActiveVotesPerRound.get(dataUseRound);
        // fractional percent
        BigDecimal fraction = config.divide(thisRoundTally, totalActiveVotes);
        // percentage of active votes
        BigDecimal percentage = fraction.signum() == 1 ?
            fraction.multiply(new BigDecimal(100)) :
            BigDecimal.ZERO;
        columnIndex = ((displayRound - 1) * COLUMNS_PER_ROUND) + 1;
        // delta votes cell
        Cell deltaVotesCell = candidateRow.createCell(columnIndex++);
        deltaVotesCell.setCellValue(deltaVotes.toString());
        // total votes cell
        Cell totalVotesCell = candidateRow.createCell(columnIndex++);
        totalVotesCell.setCellValue(thisRoundTally.toString());
        // percentage active cell
        Cell percentageCell = candidateRow.createCell(columnIndex);
        // percentage text
        String percentageText = String.format("%s%%", percentage.toString());
        percentageCell.setCellValue(percentageText);
      }
    }

    // row for the exhausted CVR counts
    org.apache.poi.ss.usermodel.Row exhaustedCVRRow = worksheet.createRow(rowCounter++);
    // exhausted CVR header cell
    Cell exhaustedRowHeaderCell = exhaustedCVRRow.createCell(0);
    exhaustedRowHeaderCell.setCellValue("Inactive ballots");
    // active votes are calculated wrt active votes in the first round
    BigDecimal totalActiveVotesFirstRound = totalActiveVotesPerRound.get(1);
    // displayRound indexes through all rounds plus final results round
    for (int displayRound = 1; displayRound <= numRounds + 1; displayRound++) {
      // flag for final round special cases
      boolean isFinalResults = displayRound == numRounds + 1;
      // data to display for this round
      int dataUseRound = isFinalResults ? numRounds : displayRound;
      // count of votes exhausted this round
      BigDecimal thisRoundExhausted = BigDecimal.ZERO;
      // vote change over previous round
      BigDecimal deltaExhausted = BigDecimal.ZERO;

      if (dataUseRound > 1) {
        // Exhausted count is the difference between the total votes in round 1 and the total votes
        // in the current round.
        thisRoundExhausted =
            totalActiveVotesFirstRound.subtract(totalActiveVotesPerRound.get(dataUseRound));
        // save previous round exhausted votes to calculate exhausted vote change
        BigDecimal prevRoundExhausted = totalActiveVotesFirstRound.subtract(
            totalActiveVotesPerRound.get(dataUseRound - 1)
        );
        deltaExhausted = isFinalResults ?
            BigDecimal.ZERO :
            thisRoundExhausted.subtract(prevRoundExhausted);

        // add exhausted votes to the votes redistributed totals
        votesRedistributedEachRound[dataUseRound] =
            votesRedistributedEachRound[dataUseRound].add(deltaExhausted);
      }

      // Exhausted votes as percentage of ALL votes (note: this differs from the candidate vote
      // percentages which are percentage of ACTIVE votes for the given round.
      BigDecimal decimalPercentage = config.divide(thisRoundExhausted, totalActiveVotesFirstRound);
      BigDecimal percentage = decimalPercentage.multiply(new BigDecimal(100));
      columnIndex = ((displayRound - 1) * COLUMNS_PER_ROUND) + 1;
      // delta votes cell
      Cell deltaVotesCell = exhaustedCVRRow.createCell(columnIndex++);
      deltaVotesCell.setCellValue(deltaExhausted.toString());
      // total votes cell
      Cell totalVotesCell = exhaustedCVRRow.createCell(columnIndex++);
      totalVotesCell.setCellValue(thisRoundExhausted.toString());
      // formatted percentage text
      String percentageText = String.format("%s%%", percentage.toString());
      // percentage cell
      Cell percentageCell = exhaustedCVRRow.createCell(columnIndex);
      percentageCell.setCellValue(percentageText);
    }

    // Total votes in this round
    // row for total votes
    org.apache.poi.ss.usermodel.Row totalVotesRow = worksheet.createRow(rowCounter++);
    // total votes header cell
    Cell totalVotesHeader = totalVotesRow.createCell(0);
    totalVotesHeader.setCellValue("Balance check");
    // displayRound indexes over all rounds plus final results round
    for (int displayRound = 1; displayRound <= numRounds + 1; displayRound++) {
      // Add 2 to the index because total votes is the third column in each round group
      columnIndex = ((displayRound - 1) * COLUMNS_PER_ROUND) + 2;
      // total votes cell
      Cell totalVotesCell = totalVotesRow.createCell(columnIndex);
      totalVotesCell.setCellValue(totalActiveVotesFirstRound.toString());
    }

    // Total active votes in this round
    // row for total active votes
    org.apache.poi.ss.usermodel.Row totalActiveVotesRow = worksheet.createRow(rowCounter++);
    // total active votes header cell
    Cell totalActiveVotesHeader = totalActiveVotesRow.createCell(0);
    totalActiveVotesHeader.setCellValue("Active votes");
    // displayRound indexes over all rounds plus final results round
    for (int displayRound = 1; displayRound <= numRounds + 1; displayRound++) {
      // flag for final round special cases
      boolean isFinalResults = displayRound == numRounds + 1;
      // total votes in this round
      BigDecimal total =
          totalActiveVotesPerRound.get(isFinalResults ? displayRound - 1 : displayRound);
      // Add 2 to the index because total votes is the third column in each round group
      columnIndex = ((displayRound - 1) * COLUMNS_PER_ROUND) + 2;
      // total votes cell
      Cell totalVotesCell = totalActiveVotesRow.createCell(columnIndex);
      totalVotesCell.setCellValue(total.toString());
    }

    // Total votes redistributed:
    // this calculation happens last because it depends on the inactive vote totals.
    // Round index starts at round 2 (since no transfer happens in first round) and continues to
    // last round + 1 (the final special winner reporting round)
    for (int round = 2; round <= numRounds + 1; round++) {
      columnIndex = ((round - 1) * COLUMNS_PER_ROUND) + 1;
      // votes redistributed cell
      Cell votesRedistributedRowCell = votesRedistributedRow.createCell(columnIndex);
      if (round <= numRounds) {
        // tally of votes redistributed this round
        BigDecimal votesRedistributed = votesRedistributedEachRound[round];
        votesRedistributedRowCell.setCellValue(votesRedistributed.toString());
      } else {
        votesRedistributedRowCell.setCellValue("NA");
      }
    }

    // write xls to disk
    try {
      // output stream is used to write data to disk
      FileOutputStream outputStream = new FileOutputStream(outputPath);
      workbook.write(outputStream);
      outputStream.close();
    } catch (IOException e) {
      e.printStackTrace();
      Logger.severe("failed to write " + outputPath + " to disk!");
    }
  }

  private int addActionRows(XSSFSheet worksheet, int rowCounter) {
    // "action" row describes whether eliminations happened or one or more winners were selected
    // we create the row header and will fill in the action cells while we iterate through
    // the candidate eliminations row since the indexing logic is identical
    // actionRow is the row object which will contain the action cells
    org.apache.poi.ss.usermodel.Row actionRow = worksheet.createRow(rowCounter++);
    // actionHeaderCell is the first cell in the row
    Cell actionHeaderCell = actionRow.createCell(0);
    actionHeaderCell.setCellValue("Action in this round");

    // eliminationsRow will contain the eliminated candidate names
    org.apache.poi.ss.usermodel.Row eliminationsRow = worksheet.createRow(rowCounter++);
    eliminationsRow.createCell(0).setCellValue("Candidates defeated");

    // Winner -- display is shifted to subsequent round for display
    // electedRow will contain the winning candidate(s) name
    org.apache.poi.ss.usermodel.Row winnersRow = worksheet.createRow(rowCounter++);
    winnersRow.createCell(0).setCellValue("Winners");

    for (int round = 1; round <= numRounds; round++) {
      // list of all candidates eliminated in this round
      List<String> eliminated = roundToEliminatedCandidates.get(round);
      List<String> winners = roundToWinningCandidates.get(round);
      // note we shift the eliminated candidate(s) display and action into the subsequent column
      if (eliminated != null && eliminated.size() > 0) {
        // we should never have both winners and losers in the same round
        assert !(winners != null && winners.size() > 0);

        addActionRowCandidates(round, eliminationsRow, eliminated, actionRow, "Elimination");
      } else if (winners != null && winners.size() > 0) {
        addActionRowCandidates(round, winnersRow, winners, actionRow, "Winner");
      }
    }

    return rowCounter;
  }

  private void addActionRowCandidates(
      int round,
      org.apache.poi.ss.usermodel.Row candidateRow,
      List<String> candidates,
      org.apache.poi.ss.usermodel.Row actionRow,
      String action
  ) {
    String candidateCellText = String.join("; ", candidates);
    // here we don't subtract 1 from round because the eliminated text is displayed in the
    // subsequent column
    int columnIndex = ((round) * COLUMNS_PER_ROUND) + 1;
    Cell candidateCell = candidateRow.createCell(columnIndex);
    candidateCell.setCellValue(candidateCellText);
    Cell actionCell = actionRow.createCell(columnIndex);
    actionCell.setCellValue(action);
  }

  // function: addHeaderRows
  // purpose: add header rows and cell to the top of the visualizer spreadsheet
  // param: worksheet to which we will be adding rows and cells
  // param: totalActiveVotesPerRound map of round to votes active in that round
  // returns: the next (empty) row index
  private int addHeaderRows(
      XSSFSheet worksheet,
      Map<Integer, BigDecimal> totalActiveVotesPerRound
  ) {
    // total active votes in this round
    BigDecimal totalActiveVotesFirstRound = totalActiveVotesPerRound.get(1);
    // dateFormat helps create a formatted date string with the current date
    DateFormat dateFormat = new SimpleDateFormat("M/d/yyyy");
    // string for formatted date
    String dateString = dateFormat.format(new Date());
    // string indicating single- or multi-winner
    String electionType = config.getNumberOfWinners() > 1 ? "multi-winner" : "single-winner";
    // number reflecting threshold percentage to win
    BigDecimal thresholdPercentage = config.divide(
        new BigDecimal(100),
        new BigDecimal(config.getNumberOfWinners() + 1)
    );
    // string for threshold percentage
    String thresholdString = thresholdPercentage.toString() + "%";
    // formula string
    String formula =
        "More than 1/" + (config.getNumberOfWinners() + 1) + " of total votes cast for office";

    // literal array to structure output cell text data
    // first cell is header text
    // second cell contains a value or null
    // third cell is true if second cell is numeric data, false if string
    Object[][] fields = {
        {"About this contest", null, OutputType.STRING},
        {"Date/time or version", "Updated " + dateString, OutputType.STRING},
        {"Contest information", null, OutputType.STRING},
        {"Enter information about the contest as it will be displayed", null, OutputType.STRING},
        {"Contest name", config.getContestName(), OutputType.STRING},
        {"Jurisdiction name", config.getJurisdiction(), OutputType.STRING},
        {"Office name", config.getOffice(), OutputType.STRING},
        {"Election date", config.getElectionDate(), OutputType.STRING},
        {null, null, OutputType.STRING},
        {"Counting information", null, OutputType.STRING},
        {"Details of the tally to be used in the display screens", null, OutputType.STRING},
        {"Counting method", "Ranked-choice voting", OutputType.STRING},
        {"Formula for winning", formula, OutputType.STRING},
        {"Formula example", "n/a", OutputType.STRING},
        {"Threshold number", thresholdString, OutputType.STRING},
        {"Graph threshold label", thresholdString + " of votes required to win", OutputType.STRING},
        {null, null, OutputType.STRING},
        {"Contest summary data", null, OutputType.STRING},
        {"Tally detail", null, OutputType.STRING},
        {"Single-winner/multi-winner", electionType, OutputType.STRING},
        {"Number to be elected", config.getNumberOfWinners(), OutputType.INT},
        {"Number of declared candidates", config.getNumDeclaredCandidates(), OutputType.INT},
        {"Number of votes cast", totalActiveVotesFirstRound.toString(), OutputType.STRING},
        {"Undervotes", 0, OutputType.INT},
        {"Total # of rounds", totalActiveVotesPerRound.size(), OutputType.INT},
        {null, null, OutputType.STRING},
        {"Tally Data Starts Here", null, OutputType.STRING}
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
    List<Map.Entry<String, BigDecimal>> entries =
        new ArrayList<>(tally.entrySet());
    // anonymous custom comparator will sort undeclared write in candidates to last place
    Collections.sort(entries, new Comparator<Map.Entry<String, BigDecimal>>() {
      public int compare(
          Map.Entry<String, BigDecimal> firstObject,
          Map.Entry<String, BigDecimal> secondObject
      ) {
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
      }
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
