/**
 * Created by Jonathan Moldover on 7/8/17
 * Copyright 2018 Bright Spots
 * Helper class takes tabulation results data as input and generates results xls file which
 * contains results summary information.
 * Currently we support an xlsx spreadsheet which can be visualized in a web browser
 * Version: 1.0
 */

package com.rcv;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class ResultsWriter {
  // number of round needed to declare a winner
  private int numRounds;
  // map of round to map of candidateID to tally
  private Map<Integer, Map<String, Integer>> roundTallies;
  // map of candidate to round in which they were eliminated
  private Map<String, Integer> candidatesToRoundEliminated;
  // the winning candidateID
  private String winner;
  // configuration file in use for this election
  private ElectionConfig config;
  // function: setNumRound
  // purpose: setter for total number of rounds to declare a winner
  // param: numRounds total number of rounds to declare a winner
  public ResultsWriter setNumRounds(int numRounds) {
    this.numRounds = numRounds;
    return this;
  }

  // function: setRoundTallies
  // purpose: setter for round to tally object
  // param: roundTallies map of round to map of candidateID to their tally for that round
  public ResultsWriter setRoundTallies(Map<Integer, Map<String, Integer>> roundTallies) {
    this.roundTallies = roundTallies;
    return this;
  }

  // function: setCandidatesToRoundEliminated
  // purpose: setter for map of candidateID to round in which they were eliminated
  // param: candidatesToRoundEliminated map of candidateID to round in which they were eliminated
  public ResultsWriter setCandidatesToRoundEliminated(
    Map<String, Integer> candidatesToRoundEliminated
  ) {
    this.candidatesToRoundEliminated = candidatesToRoundEliminated;
    return this;
  }

  public ResultsWriter setElectionConfig(ElectionConfig config) {
    this.config = config;
    return this;
  }

  public ResultsWriter setWinner(String winner) {
    this.winner = winner;
    return this;
  }

  // takes tabulation round tallies and generates a spreadsheet from them
  public void generateSummarySpreadsheet() {
    // some pre-processing on the tabulation data:
    // invert candidatesToRoundEliminated map so we can lookup who got eliminated for each round
    Map<Integer, List<String>> roundToCandidatesEliminated = new HashMap<Integer, List<String>>();
    for (String candidate : candidatesToRoundEliminated.keySet()) {
      int round = candidatesToRoundEliminated.get(candidate);
      if (roundToCandidatesEliminated.get(round) == null) {
        roundToCandidatesEliminated.put(round, new LinkedList<String>());
      }
      roundToCandidatesEliminated.get(round).add(candidate);
    }

    // Get a list of all candidates sorted by their first round tally. This determines the display
    // order.
    Map<String, Integer> firstRoundTally = roundTallies.get(1);
    List<String> sortedCandidates = sortTally(firstRoundTally);

    // build map of total votes cast in each round -- this will be used to calculate
    // the percentage of total votes each candidate achieves
    Map<Integer, Integer> totalActiveVotesPerRound = new HashMap<>();
    for (int round = 1; round <= numRounds; round++) {
      Map<String, Integer> tally = roundTallies.get(round);
      int total = 0;
      for (int votes : tally.values()) {
        total += votes;
      }
      totalActiveVotesPerRound.put(round, total);
    }

    // create the workbook and worksheet
    XSSFWorkbook workbook = new XSSFWorkbook();
    XSSFSheet worksheet = workbook.createSheet(config.jurisdiction() + " "
        + config.office());

    ////////////////////////////
    // Global Header
    /////////////////////////
    int rowCounter = addHeaderRows(worksheet, totalActiveVotesPerRound);

    /////////////////////////////////////
    // Round-by-round reports
    /////////////////////////////////////

    // each round has three pieces of data for each candidate row:
    // - change in votes from previous round
    // - total votes in current round
    // - percentage of total active votes
    int COLUMNS_PER_ROUND = 3;

    // column indexes are computed for all cells in the output xlsx spreadsheet
    // columnIndex is (round - 1) because rounds are 1-based (cells are 0-based)
    // columnIndex is offset by 1 to account for row "headers" which are the candidate names
    int columnIndex;

    // Headers for each round
    org.apache.poi.ss.usermodel.Row headerRow1 = worksheet.createRow(rowCounter++);
    Cell roundTitleHeaderCell = headerRow1.createCell(0);
    roundTitleHeaderCell.setCellValue("Round Title");
    for (int round = 1; round <= numRounds+1; round++) {
      columnIndex = ((round-1)*COLUMNS_PER_ROUND)+1;
      String label;
      if(round == 1) {
        label = "Initial Count";
      } else if (round == numRounds+1) {
        label = "Final Results";
      } else {
        label = String.format("Round %d", round);
      }
      for(int i = 0; i < COLUMNS_PER_ROUND; i++) {
        Cell roundLabelCell = headerRow1.createCell(columnIndex++);
        roundLabelCell.setCellValue(label);
      }
    }

    // Winner: show them in final round and in final results
    // Action row and header
    org.apache.poi.ss.usermodel.Row actionRow = worksheet.createRow(rowCounter++);
    Cell actionLabelCell = actionRow.createCell(0);
    actionLabelCell.setCellValue("Action in this round");

    // Candidate eliminations for each round
    org.apache.poi.ss.usermodel.Row eliminationsRow = worksheet.createRow(rowCounter++);
    Cell eliminationsRowHeader = eliminationsRow.createCell(0);
    eliminationsRowHeader.setCellValue("Candidates defeated");
    for (int round = 1; round < numRounds; round++) {
      List<String> eliminated = roundToCandidatesEliminated.get(round);
      // note we shift the eliminated candidate(s) display and action into the subsequent column
      if(eliminated.size() > 0) {
        String eliminatedCellText = String.join("; ", eliminated);
        columnIndex = ((round - 1 + 1) * COLUMNS_PER_ROUND) + 1;
        Cell cell = eliminationsRow.createCell(columnIndex);
        cell.setCellValue(eliminatedCellText);
        Cell actionCell = actionRow.createCell(columnIndex);
        actionCell.setCellValue("Elimination");
      }
    }

    // Winner -- note display is shifted to subsequent round for display
    org.apache.poi.ss.usermodel.Row electedRow = worksheet.createRow(rowCounter++);
    Cell electedCell = electedRow.createCell(0);
    electedCell.setCellValue("Winners");
    columnIndex = (numRounds*COLUMNS_PER_ROUND)+1;
    electedCell = electedRow.createCell(columnIndex);
    electedCell.setCellValue(winner);

    // Winner action
    Cell actionCell = actionRow.createCell(((numRounds*COLUMNS_PER_ROUND)+1));
    actionCell.setCellValue("Winner");

    // Create a row for the number of votes redistributed per round. We'll fill it in after we
    // tabulate all the candidates' data.
    org.apache.poi.ss.usermodel.Row votesRedistributedRow = worksheet.createRow(rowCounter++);
    Cell votesRedistributedHeaderCell = votesRedistributedRow.createCell(0);
    votesRedistributedHeaderCell.setCellValue("Votes redistributed");
    int[] votesRedistributedEachRound = new int[numRounds+1];

    // Headers for total, change, percentage for each round
    org.apache.poi.ss.usermodel.Row headerRow2 = worksheet.createRow(rowCounter++);
    Cell candidateNameCell = headerRow2.createCell(0);
    candidateNameCell.setCellValue("Candidate Name");
    for (int round = 1; round <= numRounds+1; round++) {
      columnIndex = ((round-1)*COLUMNS_PER_ROUND)+1;
      String roundDeltaText = "Vote change";
      Cell roundDeltaCell = headerRow2.createCell(columnIndex);
      roundDeltaCell.setCellValue(roundDeltaText);
      columnIndex++;
      Cell roundTotalCell = headerRow2.createCell(columnIndex++);
      String roundTotalText = (round == 1) ? "First preferences" : "Result of round";
      roundTotalCell.setCellValue(roundTotalText);
      String roundPercentageText = "% of vote";
      Cell roundPercentageCell = headerRow2.createCell(columnIndex);
      roundPercentageCell.setCellValue(roundPercentageText);
    }

    org.apache.poi.ss.usermodel.Row specialCasesHeaderRow = null;
    // Candidate votes [total, delta, percentage]
    // For each candidate: for each round: output total votes, delta votes, and final vote
    // percentage of total.
    for (String candidate : sortedCandidates) {
      if (candidate.equals(config.undeclaredWriteInLabel())) {
        specialCasesHeaderRow = worksheet.createRow(rowCounter++);
        populateSpecialCasesHeaderRow(specialCasesHeaderRow);
      }
      // show each candidate row with their totals for each round
      org.apache.poi.ss.usermodel.Row candidateRow = worksheet.createRow(rowCounter++);
      Cell rowHeaderCell = candidateRow.createCell(0);
      String candidateDisplayName = this.config.getNameForCandidateID(candidate);
      rowHeaderCell.setCellValue(candidateDisplayName);

      for (int displayRound = 1; displayRound <= numRounds+1; displayRound++) {
        boolean isFinalResults = displayRound == numRounds + 1;
        // For the Final Results "round", we're mostly copying the data from the final round.
        int round = isFinalResults ? numRounds : displayRound;
        // not all candidates may have a tally in every round
        Integer total = roundTallies.get(round).get(candidate);
        if (total == null) {
          total = 0;
        }
        // get previous tally to calculate delta
        Integer prevTotal = 0;
        if (round > 1) {
          prevTotal = roundTallies.get(round - 1).get(candidate);
          if (prevTotal == null) {
            prevTotal = 0;
          }
        }
        int delta = isFinalResults ? 0 : total - prevTotal;

        // sum total votes redistributed
        if (delta > 0) {
          votesRedistributedEachRound[round] += delta;
        }
        // percentage
        Integer totalActiveVotes = totalActiveVotesPerRound.get(round);
        float percentage = ((float)total / (float)totalActiveVotes) * 100f;

        // create cells for spreadsheet
        columnIndex = ((displayRound-1)*COLUMNS_PER_ROUND)+1;
        Cell deltaVotesCell = candidateRow.createCell(columnIndex++);
        deltaVotesCell.setCellValue(delta);
        Cell totalVotesCell = candidateRow.createCell(columnIndex++);
        totalVotesCell.setCellValue(total);
        Cell percentageCell = candidateRow.createCell(columnIndex);
        String percentageText = String.format("%.2f%%", percentage);
        percentageCell.setCellValue(percentageText);
      }
    }

    /////////////////
    // Bottom rows:
    ////////////////

    if (specialCasesHeaderRow == null) {
      specialCasesHeaderRow = worksheet.createRow(rowCounter++);
      populateSpecialCasesHeaderRow(specialCasesHeaderRow);
    }

    // exhausted ballots for each round
    org.apache.poi.ss.usermodel.Row inactiveBallotRow = worksheet.createRow(rowCounter++);
    Cell exhaustedRowHeaderCell = inactiveBallotRow.createCell(0);
    exhaustedRowHeaderCell.setCellValue("Inactive ballots");

    int totalActiveVotesFirstRound = totalActiveVotesPerRound.get(1);

    for (int displayRound = 1; displayRound <= numRounds+1; displayRound++) {
      boolean isFinalResults = displayRound == numRounds+1;
      int round = isFinalResults ? numRounds : displayRound;
      int thisRoundExhausted = 0;
      int deltaExhausted = 0;
      // Exhausted count is the difference between the total votes in round 1 and the total votes in
      // the current round.
      if (round > 1) {
        thisRoundExhausted = totalActiveVotesFirstRound - totalActiveVotesPerRound.get(round);
        int prevRoundExhausted = totalActiveVotesFirstRound - totalActiveVotesPerRound.get(round - 1);
        deltaExhausted = isFinalResults ? 0 : thisRoundExhausted - prevRoundExhausted;

        // add exhausted votes to the votes redistributed totals
        votesRedistributedEachRound[round] += deltaExhausted;
      }

      // Exhausted votes as percentage of ALL votes (note: this differs from the candidate vote
      // percentage, which are the percentage of ACTIVE votes for the given round.
      float percentage = ((float)thisRoundExhausted / (float)totalActiveVotesFirstRound) * 100f;

      // xls output
      columnIndex = ((displayRound-1)*COLUMNS_PER_ROUND)+1;
      Cell deltaVotesCell = inactiveBallotRow.createCell(columnIndex++);
      deltaVotesCell.setCellValue(deltaExhausted);
      Cell totalVotesCell = inactiveBallotRow.createCell(columnIndex++);
      totalVotesCell.setCellValue(thisRoundExhausted);
      String percentageText = String.format("%.2f%%", percentage);
      Cell percentageCell = inactiveBallotRow.createCell(columnIndex);
      percentageCell.setCellValue(percentageText);
    }

    // Total votes in this round
    org.apache.poi.ss.usermodel.Row totalVotesRow = worksheet.createRow(rowCounter++);
    Cell totalVotesHeader = totalVotesRow.createCell(0);
    totalVotesHeader.setCellValue("Balance check");

    for (int displayRound = 1; displayRound <= numRounds+1; displayRound++) {
      columnIndex = ((displayRound-1)*COLUMNS_PER_ROUND)+2;
      Cell totalVotesCell = totalVotesRow.createCell(columnIndex);
      totalVotesCell.setCellValue(totalActiveVotesFirstRound);
    }

    // Total active votes in this round
    org.apache.poi.ss.usermodel.Row totalActiveVotesRow = worksheet.createRow(rowCounter++);
    Cell totalActiveVotesHeader = totalActiveVotesRow.createCell(0);
    totalActiveVotesHeader.setCellValue("Active votes");

    for (int displayRound = 1; displayRound <= numRounds+1; displayRound++) {
      boolean isFinalResults = displayRound == numRounds+1;
      int total = totalActiveVotesPerRound.get(isFinalResults ? displayRound - 1 : displayRound);

      // xls output
      columnIndex = ((displayRound-1)*COLUMNS_PER_ROUND)+2;
      Cell totalVotesCell = totalActiveVotesRow.createCell(columnIndex);
      totalVotesCell.setCellValue(total);
    }

    // total votes redistributed
    // this calculation happens last because it depends on the inactive vote totals
    for (int round = 2; round <= numRounds+1; round++) {
      columnIndex = ((round - 1) * COLUMNS_PER_ROUND) + 1;
      Cell votesRedistributedRowCell = votesRedistributedRow.createCell(columnIndex);
      if (round <= numRounds) {
        int votesRedistributed = votesRedistributedEachRound[round];
        votesRedistributedRowCell.setCellValue(votesRedistributed);
      } else {
        votesRedistributedRowCell.setCellValue("NA");
      }
    }

    // write xls to disk
    try {
      FileOutputStream outputStream = new FileOutputStream(config.visualizerOutput());
      workbook.write(outputStream);
      outputStream.close();
    } catch (IOException e) {
      e.printStackTrace();
      Logger.log("failed to write " + config.visualizerOutput() + " to disk!");
    }
  }

  private void populateSpecialCasesHeaderRow(org.apache.poi.ss.usermodel.Row row) {
    Cell cell = row.createCell(0);
    cell.setCellValue("Special Cases Data");
  }

  private int addHeaderRows(XSSFSheet worksheet, Map<Integer, Integer> totalActiveVotesPerRound) {
    int totalActiveVotesFirstRound = totalActiveVotesPerRound.get(1);
    DateFormat dateFormat = new SimpleDateFormat("M/d/yyyy");
    String dateString = dateFormat.format(new Date());

    Object[][] fields = {
      {"About this contest", null, false},
      {"Date/time or version", "Updated " + dateString, false},
      {"Contest information", null, false},
      {"Enter information about the contest as it will be displayed", null, false},
      {"Contest name", config.contestName(), false},
      {"Jurisdiction name", config.jurisdiction(), false},
      {"Office name", config.office(), false},
      {"Election date", config.electionDate(), false},
      {null, null, false},
      {"Counting information", null, false},
      {"Details of the tally to be used in the display screens", null, false},
      {"Counting method", "Ranked-choice voting", false},
      {"Formula for winning", "Half of total votes cast for office + 1", false},
      {"Formula example", "n/a", false},
      {"Threshold number", "50%", false},
      {"Graph threshold label", "50% of votes required to win", false},
      {null, null, false},
      {"Contest summary data", null, false},
      {"Tally detail", null, false},
      {"Single/multi winner", "single-winner", false},
      {"Number to be elected", 1, true},
      {"Number of candidates", config.numCandidates(), true},
      {"Number of votes cast", totalActiveVotesFirstRound, true},
      {"Undervotes", 0, true},
      {"Total # of rounds", totalActiveVotesPerRound.size(), true},
      {null, null, false},
      {"Tally Data Starts Here", null, false}
    };

    int rowCounter = 0;
    for (Object[] rowFields : fields) {
      org.apache.poi.ss.usermodel.Row row = worksheet.createRow(rowCounter++);
      if (rowFields[0] != null) {
        row.createCell(0).setCellValue((String)rowFields[0]);
      }
      if (rowFields[1] != null) {
        if ((boolean)rowFields[2]) {
          row.createCell(1).setCellValue((int)rowFields[1]);
        } else {
          row.createCell(1).setCellValue((String)rowFields[1]);
        }
      }
    }

    return rowCounter;
  }

  // helper
  private List<String> sortTally(Map<String, Integer> tally) {
    List<Map.Entry<String, Integer>> entries =
        new LinkedList<>(tally.entrySet());
    Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
      public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
        int ret;
        if (o1.getKey().equals(config.undeclaredWriteInLabel())) {
          ret = 1;
        } else if (o2.getKey().equals(config.undeclaredWriteInLabel())) {
          ret = -1;
        } else {
          ret = (o2.getValue()).compareTo(o1.getValue());
        }
        return ret;
      }
    });
    List<String> sortedCandidates = new LinkedList<String>();
    for (Map.Entry<String, Integer> entry : entries) {
      sortedCandidates.add(entry.getKey());
    }
    return sortedCandidates;
  }
}
