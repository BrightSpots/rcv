package com.rcv;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Created by Jon Moldover on 9/4/17.
 *
 * Helper class takes tabulation results data as input and generates results file
 * Currently we support an xlsx spreadsheet which can be visualized in a web browser
 *
 */
public class ResultsWriter {

  // takes tabulation round tallies and generates a spreadsheet from them
  public boolean generateSummarySpreadsheet(
    int finalRound,
    Map<Integer, Map<String, Integer>> roundTallies,
    Map<String, Integer> candidatesToRoundEliminated,
    String outputFilePath
  ) {
    XSSFWorkbook workbook = new XSSFWorkbook();
    XSSFSheet worksheet = workbook.createSheet("Election Results");

    // TODO: write the election header info here (contest name, total votes cast, threhold, etc..)

    /////////////////////////////////////
    // Round-by-round elections results
    /////////////////////////////////////

    // Row 0: Round headers
    int rowCounter = 0;
    org.apache.poi.ss.usermodel.Row headerRow = worksheet.createRow(rowCounter++);
    for(int round = 1; round <= finalRound; round++) {
    // cellIndex is (round - 1) because rounds are 1-based while cell is 0-based
    // cellIndex is multiplied by two because for each round we create two entries
    // cellIndex is offset by 1 to account for row headers
      int cellIndex = ((round-1)*2)+1;
      Cell roundTotalCell = headerRow.createCell(cellIndex++);
      String roundTotalText = String.format("Round %d total",round);
      roundTotalCell.setCellValue(roundTotalText);
      String roundDeltaText = String.format("Round %d delta",round);
      Cell roundDeltaCell = headerRow.createCell(cellIndex);
      roundDeltaCell.setCellValue(roundDeltaText);
    }

    // Row 1: Defeated candidate lists

    // invert candidatesToRoundEliminated so we can lookup who got eliminated for each round
    Map<Integer, List<String>> roundToCandidatesEliminated = new HashMap<Integer, List<String>>();
    for (String candidate : candidatesToRoundEliminated.keySet()) {
      int round = candidatesToRoundEliminated.get(candidate);
      if (roundToCandidatesEliminated.get(round) == null) {
        roundToCandidatesEliminated.put(round, new LinkedList<String>());
      }
      roundToCandidatesEliminated.get(round).add(candidate);
    }

    // show eliminations for each round
    StringBuilder sb = new StringBuilder("Eliminations: ");
    org.apache.poi.ss.usermodel.Row eliminationsRow = worksheet.createRow(rowCounter++);
    Cell eliminationsRowHeader = eliminationsRow.createCell(0);
    eliminationsRowHeader.setCellValue("eliminations");
    for (int round = 1; round <= finalRound; round++) {
      sb.append(round).append(": ");
      List<String> eliminated = roundToCandidatesEliminated.get(round);
      if (eliminated != null) {
        String cellText = String.join(", ", eliminated);
        int cellIndex = ((round-1)*2)+1;
        Cell cell = eliminationsRow.createCell(cellIndex);
        cell.setCellValue(cellText);
        sb.append(cellText);
      } else {
        sb.append("none");
      }
      sb.append(", ");
    }
    RCVLogger.log(sb.toString());

    // TODO: elected candidate lists

    // Rows 2..n: Candidate votes [total, delta] | Final percentage

    // display order of candidates is determined by their first round tally
    Map<String, Integer> initialTally = roundTallies.get(1);
    List<String> sortedCandidates = sortTally(initialTally);

    // build map of total votes cast in each round -- this will be used to calculate
    // the percentage of total votes each candidate achieves
    Map<Integer, Integer> totalVotesPerRound = new HashMap<Integer, Integer>();
    for (int round = 1; round <= finalRound; round++) {
      Map<String, Integer> tally = roundTallies.get(round);
      int total = 0;
      for (int votes : tally.values()) {
        total += votes;
      }
      totalVotesPerRound.put(round, total);
    }
    Integer totalVotesCast = totalVotesPerRound.get(1);

    // for each candidate: for each round: output total votes, delta votes, and final vote percentage of total
    for (String candidate : sortedCandidates) {
      org.apache.poi.ss.usermodel.Row candidateRow = worksheet.createRow(rowCounter++);
      Cell rowHeaderCell = candidateRow.createCell(0);
      rowHeaderCell.setCellValue(candidate);
      sb = new StringBuilder(candidate).append(": ");
      for (int round = 1; round <= finalRound; round++) {

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

        // output delta and total counts
        sb.append("Round ").append(round - 1).append(" delta: ").append(delta).append(", ");
        sb.append("Round ").append(round - 1).append(" total: ").append(total).append(", ");
        int cellIndex = ((round-1)*2)+1;
        Cell totalVotesCell = candidateRow.createCell(cellIndex++);
        totalVotesCell.setCellValue(total);
        Cell deltaVotesCell = candidateRow.createCell(cellIndex);
        deltaVotesCell.setCellValue(delta);
      }

      // final cell for each row contains the percentage of overall vote each candidate received in the final round
      int percentageCellIndex = (finalRound*2)+1;
      Cell percentageCell = candidateRow.createCell(percentageCellIndex);
      Integer lastRoundTally = roundTallies.get(finalRound).get(candidate);
      float lastRoundPercentage = 0;
      if(lastRoundTally != null) {
        lastRoundPercentage = ((float)lastRoundTally / (float)totalVotesCast) * 100f;
      }
      String percentageText = String.format("%.2f%%", lastRoundPercentage);
      sb.append(percentageText);
      percentageCell.setCellValue(percentageText);
    }

    RCVLogger.log(sb.toString());

    // Bottom row: output exhausted ballots for each round
    org.apache.poi.ss.usermodel.Row exhaustedRow = worksheet.createRow(rowCounter++);
    Cell exhaustedRowHeaderCell = exhaustedRow.createCell(0);
    exhaustedRowHeaderCell.setCellValue("Exhausted:");

    sb = new StringBuilder("Exhausted: ");
    int totalVotesInElection = totalVotesPerRound.get(1);
    for (int round = 1; round <= finalRound; round++) {
      int total = 0;
      int delta = 0;

      // in round 1 there are never any exhausted ballots
      // after round 1 the exhausted count is delta from previous round's total count
      if (round > 1) {
        total = totalVotesInElection - totalVotesPerRound.get(round);
        int prevTotal = totalVotesInElection - totalVotesPerRound.get(round - 1);
        delta = total - prevTotal;
      }
      int cellIndex = ((round-1)*2)+1;
      Cell totalVotesCell = exhaustedRow.createCell(cellIndex++);
      totalVotesCell.setCellValue(total);
      Cell deltaVotesCell = exhaustedRow.createCell(cellIndex);
      deltaVotesCell.setCellValue(delta);

      sb.append("Round ").append(round - 1).append(" delta: ").append(delta).append(", ");
      sb.append("Round ").append(round - 1).append(" total: ").append(total).append(", ");

      if (round == finalRound) {
        // add trailer percentage cell
        int percentageCellIndex = (finalRound*2)+1;
        Cell percentageCell = exhaustedRow.createCell(percentageCellIndex);
        float lastRoundPercentage = ((float)total / (float)totalVotesCast) * 100f;
        String percentageText = String.format("%.2f%%", lastRoundPercentage);
        percentageCell.setCellValue(percentageText);
        sb.append(percentageText);
      }
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