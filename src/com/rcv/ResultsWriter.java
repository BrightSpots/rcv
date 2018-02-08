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
  // map of round to map of candidateID to their tally for that round
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
  // purpose: setter for candidatesToRoundEliminated object
  // param: candidatesToRoundEliminated map of candidateID to round in which they were eliminated
  public ResultsWriter setCandidatesToRoundEliminated(
    Map<String, Integer> candidatesToRoundEliminated
  ) {
    this.candidatesToRoundEliminated = candidatesToRoundEliminated;
    return this;
  }

  // function: setElectionConfig
  // purpose: setter for ElectionConfig object
  // param: config the ElectionConfig object to use when writing results
  public ResultsWriter setElectionConfig(ElectionConfig config) {
    this.config = config;
    return this;
  }

  // function: setWinner
  // purpose: setter for the winning candidate name
  // param: winner the winning candidate name
  public ResultsWriter setWinner(String winner) {
    this.winner = winner;
    return this;
  }

  // function: generateSummarySpreadsheet
  // purpose: creates the summary spreadsheet xls file
  // file access: write / create
  public void generateSummarySpreadsheet() {
    // some pre-processing on the tabulation data:
    // roundToCandidatesEliminated map is the inverse of candidatesToRoundEliminated map
    // so we can lookup who got eliminated for each round
    Map<Integer, List<String>> roundToCandidatesEliminated = new HashMap<>();
    // candidate is used for indexing over all candidates in candidatesToRoundEliminated
    for (String candidate : candidatesToRoundEliminated.keySet()) {
      // round is the current candidate's round of elimination
      int round = candidatesToRoundEliminated.get(candidate);
      // create a new entry for this round if needed
      if (roundToCandidatesEliminated.get(round) == null) {
        roundToCandidatesEliminated.put(round, new LinkedList<>());
      }
      // add the mapping
      roundToCandidatesEliminated.get(round).add(candidate);
    }

    // Get all candidates sorted by their first round tally. This determines the display order.
    // container for firstRoundTally
    Map<String, Integer> firstRoundTally = roundTallies.get(1);
    // candidates sorted by first round tally
    List<String> sortedCandidates = sortCandidatesByTally(firstRoundTally);

    // totalActiveVotesPerRound is a map of round to total votes cast in each round
    // this will be used to calculate the percentage of total votes each candidate achieves
    Map<Integer, Integer> totalActiveVotesPerRound = new HashMap<>();
    // round indexes over all rounds plus final results round
    for (int round = 1; round <= numRounds; round++) {
      // tally is map of candidate to tally for the current round
      Map<String, Integer> tallies = roundTallies.get(round);
      // total will contain total votes for all candidates in this round
      // this is used for calculating other derived data
      int total = 0;
      // tally indexes over all tallies for the current round
      for (int tally : tallies.values()) {
        total += tally;
      }
      totalActiveVotesPerRound.put(round, total);
    }

    // create the output workbook
    XSSFWorkbook workbook = new XSSFWorkbook();
    // create the output worksheet
    XSSFSheet worksheet = workbook.createSheet(config.jurisdiction() + " "
        + config.office());

    // rowCounter contains the next empty row after all header rows have been created
    // this is where we start adding round-by-round reports
    int rowCounter = addHeaderRows(worksheet, totalActiveVotesPerRound);

    // each round has three columns of data for each candidate:
    //   change in votes from candidate previous round
    //   total votes for candidate in current round
    //   candidate percentage of total active votes
    int COLUMNS_PER_ROUND = 3;

    // column indexes are computed for all cells as we create the output xlsx spreadsheet
    int columnIndex;

    // Round headers:
    // headerRow1 is the row for round headers
    org.apache.poi.ss.usermodel.Row headerRow1 = worksheet.createRow(rowCounter++);
    // the round header title cell will be used to create all the round headers
    Cell roundTitleHeaderCell = headerRow1.createCell(0);
    roundTitleHeaderCell.setCellValue("Round Title");
    // round indexes over all rounds plus final results round
    for (int round = 1; round <= numRounds+1; round++) {
      // compute column index (convert from 1-based to 0-based indexing
      // then multiply by COLUMNS_PER_ROUND to get the start of the grouping
      // then add 1 to skip the row header cell
      columnIndex = ((round-1)*COLUMNS_PER_ROUND)+1;
      // label string will have the actual text which goes in the cell
      String label;
      if(round == 1) {
        label = "Initial Count";
      } else if (round == numRounds+1) {
        label = "Final Results";
      } else {
        label = String.format("Round %d", round);
      }
      for(int i = 0; i < COLUMNS_PER_ROUND; i++) {
        // cell for round label
        Cell roundLabelCell = headerRow1.createCell(columnIndex++);
        roundLabelCell.setCellValue(label);
      }
    }

    // "action" row describes weather an elimination happened or a winner was selected
    // we create the row header and will fill in the action cells while we iterate through
    // the candidate eliminations row since the indexing logic is identical
    // actionRow is the row object which will contain the action cells
    org.apache.poi.ss.usermodel.Row actionRow = worksheet.createRow(rowCounter++);
    // actionHeaderCell is the first cell in the row
    Cell actionHeaderCell = actionRow.createCell(0);
    actionHeaderCell.setCellValue("Action in this round");

    // eliminationsRow will contain the eliminated candidate names
    org.apache.poi.ss.usermodel.Row eliminationsRow = worksheet.createRow(rowCounter++);
    // eliminationsRowHeader is the header cell for the eliminations row
    Cell eliminationsRowHeader = eliminationsRow.createCell(0);
    eliminationsRowHeader.setCellValue("Candidates defeated");
    // round indexes through all rounds plus final winner round
    for (int round = 1; round < numRounds; round++) {
      // list of all candidates eliminated in this round
      List<String> eliminated = roundToCandidatesEliminated.get(round);
      // note we shift the eliminated candidate(s) display and action into the subsequent column
      if(eliminated.size() > 0) {
        // eliminatedCellText contains formatted candidate names
        String eliminatedCellText = String.join("; ", eliminated);
        // here we dont subtract 1 from round because the eliminated text is displayed in the
        // subsequent column
        columnIndex = ((round) * COLUMNS_PER_ROUND) + 1;
        // eliminatedCandidateCell will contain the candidates who were eliminated in this round
        Cell eliminatedCandidateCell = eliminationsRow.createCell(columnIndex);
        eliminatedCandidateCell.setCellValue(eliminatedCellText);
        // actionCell will contain the "elimination" action
        Cell actionCell = actionRow.createCell(columnIndex);
        actionCell.setCellValue("Elimination");
      }
    }

    // Winner -- display is shifted to subsequent round for display
    // electedRow will contain the winning candidate(s) name
    org.apache.poi.ss.usermodel.Row electedRow = worksheet.createRow(rowCounter++);
    // electedCell will contain the elected row header text and the winning candidate name(s)
    Cell electedCell = electedRow.createCell(0);
    electedCell.setCellValue("Winners");
    columnIndex = (numRounds*COLUMNS_PER_ROUND)+1;
    electedCell = electedRow.createCell(columnIndex);
    electedCell.setCellValue(winner);

    // Winner action
    // actionCell is created from the actionRow (above) for the final "winner" action
    Cell actionCell = actionRow.createCell(((numRounds*COLUMNS_PER_ROUND)+1));
    actionCell.setCellValue("Winner");

    // votesRedistributedRow is for the number of votes redistributed per round. We'll fill it
    // in after we tabulate all the candidates' data.
    org.apache.poi.ss.usermodel.Row votesRedistributedRow = worksheet.createRow(rowCounter++);
    // cell for votes redistributed header text
    Cell votesRedistributedHeaderCell = votesRedistributedRow.createCell(0);
    votesRedistributedHeaderCell.setCellValue("Votes redistributed");
    // array for calculating the votes redistributed between rounds
    int[] votesRedistributedEachRound = new int[numRounds+1];

    // headerRow2 will be the row object for vote total, change, percentage headers for each round
    org.apache.poi.ss.usermodel.Row headerRow2 = worksheet.createRow(rowCounter++);
    // container for candidate name
    Cell candidateNameCell = headerRow2.createCell(0);
    candidateNameCell.setCellValue("Candidate Name");
    // round indexes over all rounds plus final results round
    for (int round = 1; round <= numRounds+1; round++) {
      columnIndex = ((round-1)*COLUMNS_PER_ROUND)+1;
      // cell for round delta header
      Cell roundDeltaCell = headerRow2.createCell(columnIndex);
      roundDeltaCell.setCellValue("Vote change");
      columnIndex++;
      // round total header cell
      Cell roundTotalCell = headerRow2.createCell(columnIndex++);
      // text for the round total header cell
      String roundTotalText = (round == 1) ? "First preferences" : "Result of round";
      roundTotalCell.setCellValue(roundTotalText);
      // cell for round percentage header cell
      Cell roundPercentageCell = headerRow2.createCell(columnIndex);
      roundPercentageCell.setCellValue("% of vote");
    }

    // row for for "special cases"
    // TODO: what is the special cases data?  It seems like only the header cell is ever created
    org.apache.poi.ss.usermodel.Row specialCasesHeaderRow = null;

    // Candidate votes [total, delta, percentage]
    // For each candidate: for each round: output total votes, delta votes, and final vote
    // percentage of total.
    // candidate indexes over all candidates
    for (String candidate : sortedCandidates) {
      if (candidate.equals(config.undeclaredWriteInLabel())) {
        specialCasesHeaderRow = worksheet.createRow(rowCounter++);
        populateSpecialCasesHeaderRow(specialCasesHeaderRow);
      }
      // show each candidate row with their totals for each round
      // row for the current candidate
      org.apache.poi.ss.usermodel.Row candidateRow = worksheet.createRow(rowCounter++);
      // header cell for the candidate
      Cell rowHeaderCell = candidateRow.createCell(0);
      // text for the candidate name
      String candidateDisplayName = this.config.getNameForCandidateID(candidate);
      rowHeaderCell.setCellValue(candidateDisplayName);
      // displayRound indexes over all rounds plus final results round
      for (int displayRound = 1; displayRound <= numRounds+1; displayRound++) {
        // flag for the last round results which are special-cased
        boolean isFinalResults = displayRound == numRounds + 1;
        // For the Final Results "round", we're mostly copying the data from the final round.
        // round from which to display data
        int dataUseRound = isFinalResults ? numRounds : displayRound;
        // vote tally this round
        Integer thisRoundTally = roundTallies.get(dataUseRound).get(candidate);
        // not all candidates may have a tally in every round
        if (thisRoundTally == null) {
          thisRoundTally = 0;
        }
        // previous round tally for calculating deltas
        Integer prevRoundTally = 0;
        if (dataUseRound > 1) {
          prevRoundTally = roundTallies.get(dataUseRound - 1).get(candidate);
          if (prevRoundTally == null) {
            prevRoundTally = 0;
          }
        }
        // vote tally delta
        int deltaVotes = isFinalResults ? 0 : thisRoundTally - prevRoundTally;

        // accumulate total votes redistributed
        if (deltaVotes > 0) {
          votesRedistributedEachRound[dataUseRound] += deltaVotes;
        }
        //  total active votes in this round
        Integer totalActiveVotes = totalActiveVotesPerRound.get(dataUseRound);
        // percentage of active votes
        float percentage = ((float)thisRoundTally / (float)totalActiveVotes) * 100f;
        columnIndex = ((displayRound-1)*COLUMNS_PER_ROUND)+1;
        // delta votes cell
        Cell deltaVotesCell = candidateRow.createCell(columnIndex++);
        deltaVotesCell.setCellValue(deltaVotes);
        // total votes cell
        Cell totalVotesCell = candidateRow.createCell(columnIndex++);
        totalVotesCell.setCellValue(thisRoundTally);
        // percentage active cell
        Cell percentageCell = candidateRow.createCell(columnIndex);
        // percentage text
        String percentageText = String.format("%.2f%%", percentage);
        percentageCell.setCellValue(percentageText);
      }
    }

    // create a special cases divider row
    if (specialCasesHeaderRow == null) {
      specialCasesHeaderRow = worksheet.createRow(rowCounter++);
      populateSpecialCasesHeaderRow(specialCasesHeaderRow);
    }

    // row for the exhausted cvr counts
    org.apache.poi.ss.usermodel.Row exhaustedCVRRow = worksheet.createRow(rowCounter++);
    // exhausted cvr header cell
    Cell exhaustedRowHeaderCell = exhaustedCVRRow.createCell(0);
    exhaustedRowHeaderCell.setCellValue("Inactive ballots");
    // active votes are calculated wrt active votes in the first round
    int totalActiveVotesFirstRound = totalActiveVotesPerRound.get(1);
    // displayRound indexes through all rounds plus final results round
    for (int displayRound = 1; displayRound <= numRounds+1; displayRound++) {
      // flag for final round special cases
      boolean isFinalResults = displayRound == numRounds+1;
      // data to display for this round
      int dataUseRound = isFinalResults ? numRounds : displayRound;
      // count of votes exhausted this round
      int thisRoundExhausted = 0;
      // vote change over previous round
      int deltaExhausted = 0;
      // Exhausted count is the difference between the total votes in round 1 and the total votes in
      // the current round.
      if (dataUseRound > 1) {
        thisRoundExhausted = totalActiveVotesFirstRound -
            totalActiveVotesPerRound.get(dataUseRound);
        // save previous round exhausted votes to calculate exhausted vote change
        int prevRoundExhausted = totalActiveVotesFirstRound -
            totalActiveVotesPerRound.get(dataUseRound - 1);
        deltaExhausted = isFinalResults ? 0 : thisRoundExhausted - prevRoundExhausted;

        // add exhausted votes to the votes redistributed totals
        votesRedistributedEachRound[dataUseRound] += deltaExhausted;
      }

      // Exhausted votes as percentage of ALL votes (note: this differs from the candidate vote
      // percentages which are percentage of ACTIVE votes for the given round.
      float percentage = ((float)thisRoundExhausted / (float)totalActiveVotesFirstRound) * 100f;
      columnIndex = ((displayRound-1)*COLUMNS_PER_ROUND)+1;
      // delta votes cell
      Cell deltaVotesCell = exhaustedCVRRow.createCell(columnIndex++);
      deltaVotesCell.setCellValue(deltaExhausted);
      // total votes cell
      Cell totalVotesCell = exhaustedCVRRow.createCell(columnIndex++);
      totalVotesCell.setCellValue(thisRoundExhausted);
      // formatted percentage text
      String percentageText = String.format("%.2f%%", percentage);
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
    for (int displayRound = 1; displayRound <= numRounds+1; displayRound++) {
      columnIndex = ((displayRound-1)*COLUMNS_PER_ROUND)+2;
      // total votes cell
      Cell totalVotesCell = totalVotesRow.createCell(columnIndex);
      totalVotesCell.setCellValue(totalActiveVotesFirstRound);
    }

    // Total active votes in this round
    // row for total active votes
    org.apache.poi.ss.usermodel.Row totalActiveVotesRow = worksheet.createRow(rowCounter++);
    // total active votes header cell
    Cell totalActiveVotesHeader = totalActiveVotesRow.createCell(0);
    totalActiveVotesHeader.setCellValue("Active votes");
    // displayRound indexes over all rounds plus final results round
    for (int displayRound = 1; displayRound <= numRounds+1; displayRound++) {
      // flag for final round special cases
      boolean isFinalResults = displayRound == numRounds+1;
      // total votes in this round
      int total = totalActiveVotesPerRound.get(isFinalResults ? displayRound - 1 : displayRound);
      columnIndex = ((displayRound-1)*COLUMNS_PER_ROUND)+2;
      // total votes cell
      Cell totalVotesCell = totalActiveVotesRow.createCell(columnIndex);
      totalVotesCell.setCellValue(total);
    }

    // total votes redistributed i.e. exhausted in this round
    // this calculation happens last because it depends on the inactive vote totals
    // round indexes from second round (since no transfer happens in first round
    // to last round + 1 for the final winner round
    for (int round = 2; round <= numRounds+1; round++) {
      columnIndex = ((round - 1) * COLUMNS_PER_ROUND) + 1;
      // votes redistributed cell
      Cell votesRedistributedRowCell = votesRedistributedRow.createCell(columnIndex);
      if (round <= numRounds) {
        // tally of votes redistributed this round
        int votesRedistributed = votesRedistributedEachRound[round];
        votesRedistributedRowCell.setCellValue(votesRedistributed);
      } else {
        votesRedistributedRowCell.setCellValue("NA");
      }
    }

    // write xls to disk
    try {
      // output stream is used to write data to disk
      FileOutputStream outputStream = new FileOutputStream(config.visualizerOutput());
      workbook.write(outputStream);
      outputStream.close();
    } catch (IOException e) {
      e.printStackTrace();
      Logger.log("failed to write " + config.visualizerOutput() + " to disk!");
    }
  }

  // function: populateSpecialCasesHeaderRow
  // purpose: good question
  // param: row to add header cell to
  private void populateSpecialCasesHeaderRow(org.apache.poi.ss.usermodel.Row row) {
    // cell for header text
    Cell cell = row.createCell(0);
    cell.setCellValue("Special Cases Data");
  }

  // function: addHeaderRows
  // purpose: add header rows and cell to the top of the visualizer spreadsheet
  // param: worksheet to which we will be adding rows and cells
  // param: totalActiveVotesPerRound map of round to votes active in that round
  // returns: the next (empty) row index
  private int addHeaderRows(XSSFSheet worksheet, Map<Integer, Integer> totalActiveVotesPerRound) {
    // total active votes in this round
    int totalActiveVotesFirstRound = totalActiveVotesPerRound.get(1);
    // dateFormat helps create a formatted date string with the current date
    DateFormat dateFormat = new SimpleDateFormat("M/d/yyyy");
    // string for formatted date
    String dateString = dateFormat.format(new Date());
    // literal array to structure output cell text data
    // first cell is header text
    // second cell contains a value or null
    // third cell is true if second cell is numeric data, false if string
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
    // count the row we create so we can return the next empty row
    int rowCounter = 0;
    // index over all fields in the row structure
    for (Object[] rowFields : fields) {
      // row for the next row
      org.apache.poi.ss.usermodel.Row row = worksheet.createRow(rowCounter++);
      // create a cell if any text in the first element
      if (rowFields[0] != null) {
        row.createCell(0).setCellValue((String)rowFields[0]);
      }
      // if second element is non-null create a cell for it
      // type (string or numeric) is determined by third element
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

  // function: sortCandidatesByTally
  // purpose: given a map of candidates to tally return a list of all input candidates
  // sorted from highest tally to lowest
  // param: tally map of candidateID to tally
  // return: list of all input candidates sorted from highest tally to lowest
  private List<String> sortCandidatesByTally(Map<String, Integer> tally) {
    // entries will contain all the input tally entries in sorted order
    List<Map.Entry<String, Integer>> entries =
        new ArrayList<>(tally.entrySet());
    // anonymous custom comparator will sort undeclared write in candidates to last place
    Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
      public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
        // result of the comparison
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
    // container list for the final results
    List<String> sortedCandidates = new LinkedList<>();
    // index over all entries
    for (Map.Entry<String, Integer> entry : entries) {
      sortedCandidates.add(entry.getKey());
    }
    return sortedCandidates;
  }
}
