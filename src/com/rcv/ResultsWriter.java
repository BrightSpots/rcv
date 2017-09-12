package com.rcv;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Created by Jon on 9/4/17.
 *
 * Helper class takes tabulation results data as input and generates results file
 * Currently we support an xlsx spreadsheet which can be visualized in a web browser
 *
 */
public class ResultsWriter {

  public boolean writeXLSX(String outputFilePath) {

     String contestName = "2018 Supreme Allied Commander";
      Integer totalVotesCast = 300;
      Integer numToBeElected = 1;
      Integer threshold = (int)(Math.floor(totalVotesCast / (numToBeElected + 1))) + 1;
      Integer undervotes = 50;
      Integer roundsTabulated = 3;
      Integer numDeclaredCandidates = 4;
    
      // for now some fake output data listing first round total followed by DELTAS
      Object[][] allCandidateVotesByRound = {
          {"Betsy Hodges", 28962, 79, -35, 0.23},
          {"Louis Eisenberg", 399111, 6, 0, 14.4},
          {"Jonathan Moldover", 80030303, 13, 0, 56.56},
          {"Tzipora Lederman", 292929292, 4, 4, 19.25},
      };

      String[][] candidatesEliminated = new String[][] {
          {"undeclared", "Jonathan Moldover"},
          {"Louis Eisenberg"},
          {"Tzipora Lederman"},
          {}
      };
      String[][] candidatesElected = new String[][] {
          {},
          {},
          {},
          {"Tzipora Lederman"}
      };

    XSSFWorkbook workbook = new XSSFWorkbook();
    XSSFSheet sheet = workbook.createSheet("Election Results");

    int candidate = 0;
    for (Object[] candidateVotesByRound : allCandidateVotesByRound) {
      // create new row to contain this candidate info
        org.apache.poi.ss.usermodel.Row row = sheet.createRow(++candidate);
        // add cells for each column
        int round = 0;

        for (Object field : candidateVotesByRound) {
          Cell cell = row.createCell(++round);
          if (field instanceof String) {
            cell.setCellValue((String) field);
          } else if (field instanceof Integer) {
            cell.setCellValue((Integer) field);
          } else if (field instanceof  Double) {
            cell.setCellValue((Double) field);
          } else {
            RCVLogger.log("Unexpected data!");
            return false;
          }
        }

      }

    // write this puppy to disk
    try {
      FileOutputStream outputStream = new FileOutputStream(outputFilePath);
        workbook.write(outputStream);
    } catch (IOException e) {
      e.printStackTrace();
      RCVLogger.log("failed to write " + outputFilePath + " to disk!");
      return false;
    }

    return true;
  }

  // takes tabulation round tallies and generates a spreadsheet from them
  private void generateSummarySpreadsheet(
      int finalRound,
      Map<Integer, Map<String, Integer>> roundTallies,
      Map<String, Integer> eliminatedRound
  ) {
    // build map of all candidates eliminated in each round
    Map<Integer, List<String>> eliminationsByRound = new HashMap<Integer, List<String>>();
    for (String candidate : eliminatedRound.keySet()) {
      // for each round we build a string showing which candidates were eliminated
      int round = eliminatedRound.get(candidate);
      if (eliminationsByRound.get(round) == null) {
        eliminationsByRound.put(round, new LinkedList<String>());
      }
      eliminationsByRound.get(round).add(candidate);
    }
    // build string to display the eliminations
    StringBuilder sb = new StringBuilder("Eliminations: ");
    for (int round = 1; round <= finalRound; round++) {
      sb.append(round).append(": ");
      List<String> eliminated = eliminationsByRound.get(round);
      if (eliminated != null) {
        sb.append(String.join(", ", eliminated));
      } else {
        sb.append("none");
      }
      sb.append(", ");
    }
    RCVLogger.log(sb.toString());
    // build map of total votes cast in each round
    Map<Integer, Integer> totalVotesPerRound = new HashMap<Integer, Integer>();
    for (int round = 1; round <= finalRound; round++) {
      Map<String, Integer> tally = roundTallies.get(round);
      int total = 0;
      for (int votes : tally.values()) {
        total += votes;
      }
      totalVotesPerRound.put(round, total);
    }

    // map of candidates to their first round tally, sorted from most votes to least
    Map<String, Integer> initialTally = roundTallies.get(1);
    List<String> sortedCandidates = sortTally(initialTally);

    // iterate through the list of candidates (ordered by initial round tallies)
    // spit out delta votes and total votes
    for (String candidate : sortedCandidates) {
      sb = new StringBuilder(candidate).append(": ");
      sb.append("Initial count: ").append(roundTallies.get(1).get(candidate)).append(", ");
      for (int round = 2; round <= finalRound; round++) {
        Integer total = roundTallies.get(round).get(candidate);
        if (total == null) {
          total = 0;
        }
        Integer prevTotal = roundTallies.get(round - 1).get(candidate);
        if (prevTotal == null) {
          prevTotal = 0;
        }
        int delta = total - prevTotal;

        sb.append("Round ").append(round - 1).append(" delta: ").append(delta).append(", ");
        sb.append("Round ").append(round - 1).append(" total: ").append(total).append(", ");
      }
      RCVLogger.log(sb.toString());
    }

    // bottom row we output exhausted ballots on each round
    sb = new StringBuilder("Exhausted: ");
    int totalVotes = totalVotesPerRound.get(1);
    for (int round = 2; round <= finalRound; round++) {
      int total = totalVotes - totalVotesPerRound.get(round);
      int prevTotal = totalVotes - totalVotesPerRound.get(round - 1);
      int delta = total - prevTotal;

      sb.append("Round ").append(round - 1).append(" delta: ").append(delta).append(", ");
      sb.append("Round ").append(round - 1).append(" total: ").append(total).append(", ");
    }
    RCVLogger.log(sb.toString());
  }


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