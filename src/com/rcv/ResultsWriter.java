package com.rcv;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Jon Moldover on 9/4/17.
 *
 * Helper class takes tabulation results data as input and generates results file
 * Currently we support an xlsx spreadsheet which can be visualized in a web browser
 *
 */
public class ResultsWriter {

  private int numRounds;
  private Map<Integer, Map<String, Integer>> roundTallies;
  private Map<String, Integer> candidatesToRoundEliminated;
  private String outputFilePath;
  private String contestName;
  private String jurisdiction;
  private String office;
  private String electionDate;
  private int numCandidates;
  private String winner;

  public ResultsWriter setNumRounds(int numRounds) {
    this.numRounds = numRounds;
    return this;
  }

  public ResultsWriter setRoundTallies(Map<Integer, Map<String, Integer>> roundTallies) {
    this.roundTallies = roundTallies;
    return this;
  }

  public ResultsWriter setCandidatesToRoundEliminated(Map<String, Integer> candidatesToRoundEliminated) {
    this.candidatesToRoundEliminated = candidatesToRoundEliminated;
    return this;
  }

  public ResultsWriter setOutputFilePath(String outputFilePath) {
    this.outputFilePath = outputFilePath;
    return this;
  }

  public ResultsWriter setContestName(String contestName) {
    this.contestName = contestName;
    return this;
  }

  public ResultsWriter setJurisdiction(String jurisdiction) {
    this.jurisdiction = jurisdiction;
    return this;
  }

  public ResultsWriter setOffice(String office) {
    this.office = office;
    return this;
  }

  public ResultsWriter setElectionDate(String electionDate) {
    this.electionDate = electionDate;
    return this;
  }

  public ResultsWriter setNumCandidates(int numCandidates) {
    this.numCandidates = numCandidates;
    return this;
  }

  public ResultsWriter setWinner(String winner) {
    this.winner = winner;
    return this;
  }

  // takes tabulation round tallies and generates a spreadsheet from them
  public boolean generateSummarySpreadsheet() {
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

    // get a list of all candidates sorted by their first round tally -- that determines the display order
    Map<String, Integer> firstRoundTally = roundTallies.get(1);
    List<String> sortedCandidates = sortTally(firstRoundTally);

    // build map of total votes cast in each round -- this will be used to calculate
    // the percentage of total votes each candidate achieves
    Map<Integer, Integer> totalActiveVotesPerRound = new HashMap<Integer, Integer>();
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
    XSSFSheet worksheet = workbook.createSheet(jurisdiction + " " + office);

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
    for(int round = 1; round <= numRounds; round++) {
      columnIndex = ((round-1)*COLUMNS_PER_ROUND)+1;
      Cell roundLabelCell = headerRow1.createCell(columnIndex);
      String label = String.format("ROUND %d", round);
      if(round ==1) {
        label = "INITIAL COUNT";
      }
      roundLabelCell.setCellValue(label);
    }

    // Eliminations for each round
    StringBuilder sb = new StringBuilder("Eliminations: ");
    org.apache.poi.ss.usermodel.Row eliminationsRow = worksheet.createRow(rowCounter++);
    Cell eliminationsRowHeader = eliminationsRow.createCell(0);
    eliminationsRowHeader.setCellValue("DEFEATED: ");
    for (int round = 1; round <= numRounds; round++) {
      sb.append(round).append(": ");
      List<String> eliminated = roundToCandidatesEliminated.get(round);
      String cellText = String.join(", ", eliminated);
      // note we shift the eliminated candidate(s) display into the subsequent column
      columnIndex = ((round-1+1)*COLUMNS_PER_ROUND)+1;
      Cell cell = eliminationsRow.createCell(columnIndex);
      cell.setCellValue(cellText);
      sb.append(cellText);
      sb.append(", ");

    }
    RCVLogger.log(sb.toString());

    // Winners for each round
    org.apache.poi.ss.usermodel.Row electedRow = worksheet.createRow(rowCounter++);
    Cell electedCell = electedRow.createCell(0);
    electedCell.setCellValue("ELECTED:");
    columnIndex = ((numRounds-1)*COLUMNS_PER_ROUND)+1;
    electedCell = electedRow.createCell(columnIndex);
    electedCell.setCellValue(winner);


    // create a row for votes redistributed total -- we will fill it in after we tabulate all the candidates' data
    org.apache.poi.ss.usermodel.Row votesRedistributedRow = worksheet.createRow(rowCounter++);
    Cell votesRedistributedHeaderCell = votesRedistributedRow.createCell(0);
    votesRedistributedHeaderCell.setCellValue("VOTES REDISTRIBUTED");
    int[] votesRedistributedEachRound = new int[numRounds+1];

    // Headers for total, change, percentage for each round
    org.apache.poi.ss.usermodel.Row headerRow2 = worksheet.createRow(rowCounter++);
    for(int round = 1; round <= numRounds; round++) {
      columnIndex = ((round-1)*COLUMNS_PER_ROUND)+1;
      String roundDeltaText = String.format("Change");
      Cell roundDeltaCell = headerRow2.createCell(columnIndex);
      roundDeltaCell.setCellValue(roundDeltaText);
      columnIndex++;
      Cell roundTotalCell = headerRow2.createCell(columnIndex++);
      String roundTotalText = String.format("Total");
      roundTotalCell.setCellValue(roundTotalText);
      String roundPercentageText = String.format("Percentage (active votes)");
      Cell roundPercentageCell = headerRow2.createCell(columnIndex);
      roundPercentageCell.setCellValue(roundPercentageText);
    }
    
    // Candidate votes [total, delta, percentage]
    // for each candidate: for each round: output total votes, delta votes, and final vote percentage of total
    for (String candidate : sortedCandidates) {
      // show each candidate row with their totals for each round
      org.apache.poi.ss.usermodel.Row candidateRow = worksheet.createRow(rowCounter++);
      Cell rowHeaderCell = candidateRow.createCell(0);
      rowHeaderCell.setCellValue(candidate);
      sb = new StringBuilder(candidate).append(": ");
      for (int round = 1; round <= numRounds; round++) {

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
        int delta = total - prevTotal;

        // sum total votes redistributed
        if(delta > 0) {
          votesRedistributedEachRound[round] += delta;
        }
        // percentage
        Integer totalActiveVotes = totalActiveVotesPerRound.get(round);
        float percentage = ((float)total / (float)totalActiveVotes) * 100f;

        // log output
        sb.append("Round ").append(round - 1).append(" change: ").append(delta).append(", ");
        sb.append("Round ").append(round - 1).append(" total: ").append(total).append(", ");
        sb.append("Round ").append(round - 1).append(" percentage: ").append(percentage).append(", ");

        // create cells for spreadsheet
        columnIndex = ((round-1)*COLUMNS_PER_ROUND)+1;
        Cell deltaVotesCell = candidateRow.createCell(columnIndex);
        deltaVotesCell.setCellValue(delta);
        columnIndex++;
        Cell totalVotesCell = candidateRow.createCell(columnIndex++);
        totalVotesCell.setCellValue(total);
        Cell percentageCell = candidateRow.createCell(columnIndex);
        String percentageText = String.format("%.2f%%", percentage);
        percentageCell.setCellValue(percentageText);
      }

    }

    RCVLogger.log(sb.toString());

    // total votes redistributed
    for (int round = 1; round <= numRounds; round++) {
      columnIndex = ((round-1)*COLUMNS_PER_ROUND)+1;
      Cell votesRedistributedRowCell = votesRedistributedRow.createCell(columnIndex);
      int votesRedistributed = votesRedistributedEachRound[round];
      votesRedistributedRowCell.setCellValue(votesRedistributed);
    }


    /////////////////
    // Bottom rows:
    ////////////////

    // exhausted ballots for each round
    org.apache.poi.ss.usermodel.Row exhaustedRow = worksheet.createRow(rowCounter++);
    Cell exhaustedRowHeaderCell = exhaustedRow.createCell(0);
    exhaustedRowHeaderCell.setCellValue("Exhausted Votes:");

    int totalActiveVotesFirstRound = totalActiveVotesPerRound.get(1);

    sb = new StringBuilder("Exhausted Votes: ");
    for (int round = 1; round <= numRounds; round++) {
      int thisRoundExhausted = 0;
      int deltaExhausted = 0;
      // exhausted count is the difference between the total votes in round 1 and total votes in current round
      if (round > 1) {
        thisRoundExhausted = totalActiveVotesFirstRound - totalActiveVotesPerRound.get(round);
        int prevRoundExhausted = totalActiveVotesFirstRound - totalActiveVotesPerRound.get(round - 1);
        deltaExhausted = thisRoundExhausted - prevRoundExhausted;
      }

      // exhausted votes as percentage of ALL votes (note: this differs from the candidate vote percentages
      // which are percentage of ACTIVE votes for the given round
      float percentage = ((float)thisRoundExhausted / (float)totalActiveVotesFirstRound) * 100f;

      // log file output
      sb.append("Round ").append(round - 1).append(" delta: ").append(deltaExhausted).append(", ");
      sb.append("Round ").append(round - 1).append(" total: ").append(thisRoundExhausted).append(", ");
      sb.append("Round ").append(round - 1).append(" percentage: ").append(percentage).append(", ");

      // xls output
      columnIndex = ((round-1)*COLUMNS_PER_ROUND)+1;
      Cell deltaVotesCell = exhaustedRow.createCell(columnIndex);
      deltaVotesCell.setCellValue(deltaExhausted);
      columnIndex++;
      Cell totalVotesCell = exhaustedRow.createCell(columnIndex++);
      totalVotesCell.setCellValue(thisRoundExhausted);
      String percentageText = String.format("%.2f%%", percentage);
      Cell percentageCell = exhaustedRow.createCell(columnIndex);
      percentageCell.setCellValue(percentageText);
    }

    // Total votes in this round
    org.apache.poi.ss.usermodel.Row totalVotesRow = worksheet.createRow(rowCounter++);
    Cell totalVotesHeader = totalVotesRow.createCell(0);
    totalVotesHeader.setCellValue("Total Votes:");
    sb = new StringBuilder("Total Votes:");

    for (int round = 1; round <= numRounds; round++) {
      // TODO: actually calculate this value
      sb.append("Round ").append(round - 1).append(" total: ").append(totalActiveVotesFirstRound).append(", ");

      // xls output
      columnIndex = ((round-1)*COLUMNS_PER_ROUND)+2;
      Cell totalVotesCell = totalVotesRow.createCell(columnIndex);
      totalVotesCell.setCellValue(totalActiveVotesFirstRound);
    }

    // Total active votes in this round
    org.apache.poi.ss.usermodel.Row totalActiveVotesRow = worksheet.createRow(rowCounter++);
    Cell totalActiveVotesHeader = totalActiveVotesRow.createCell(0);
    totalActiveVotesHeader.setCellValue("Active Votes:");
    sb = new StringBuilder("Active Votes:");

    for (int round = 1; round <= numRounds; round++) {
      int total = totalActiveVotesPerRound.get(round);
      sb.append("Round ").append(round - 1).append(" active: ").append(total).append(", ");

      // xls output
      columnIndex = ((round-1)*COLUMNS_PER_ROUND)+2;
      Cell totalVotesCell = totalActiveVotesRow.createCell(columnIndex);
      totalVotesCell.setCellValue(total);
    }

    // write to log
    RCVLogger.log(sb.toString());
    
    // write xls to disk
    try {
      FileOutputStream outputStream = new FileOutputStream(outputFilePath);
      workbook.write(outputStream);
      outputStream.close();
      return true;
    } catch (IOException e) {
      e.printStackTrace();
      RCVLogger.log("failed to write " + outputFilePath + " to disk!");
      return false;
    }
    
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
      {"Contest name", contestName, false},
      {"Jurisdiction name", jurisdiction, false},
      {"Office name", office, false},
      {"Election date", electionDate, false},
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
      {"Number of candidates", numCandidates, true},
      {"Number of votes cast", totalActiveVotesFirstRound, true},
      {"Undervotes", 0, true},
      {"Total # of rounds", totalActiveVotesPerRound.size(), true},
      {null, null, false}
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
        new LinkedList<Map.Entry<String, Integer>>(tally.entrySet());
    Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
      public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
        return (o2.getValue()).compareTo(o1.getValue());
      }
    });
    List<String> sortedCandidates = new LinkedList<String>();
    for (Map.Entry<String, Integer> entry : entries) {
      sortedCandidates.add(entry.getKey());
    }
    return sortedCandidates;
  }
}