package com.rcv;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Created by Jon on 8/27/17.
 *
 * Helper class to read and parse an xlsl "Maine Style" Cast Vote Record File?
 *
 * Whole lotta assumptions going on here:
 * We assume one contest per file
 * we assume the first sheet is the only one we're interested in
 * we assume the first column contains ballot ids, second column contains precinct id and third column contains ballot style.
 * we assume columns after ballot style are the ballot selections ordered by rank, low to high, left to right
 * we assume the strings "undervote" or "overvote" mean no vote
 * we assume a non-existant cell (image of a ballot mark when workbook is opened in excel?) means no vote
 *
 */

public class CVRReader {

  public List<CastVoteRecord> castVoteRecords = new ArrayList<>();

  // call this to parse the given file path into a CastVoteRecordList suitable for tabulation
  // Note: this is specific for the Maine example file we were provided
  public boolean parseCVRFile(
    String excelFilePath,
    int firstVoteColumnIndex,
    int allowableRanks,
    List<String>options,
    String undeclaredOption,
    String overvoteFlag,
    String undervoteFlag) {

    Sheet contestSheet = getBallotSheet(excelFilePath);
    if (contestSheet == null) {
      RCVLogger.log("invalid RCV format: could not obtain ballot data.");
      System.exit(1);
    }

    // validate header
    Iterator<org.apache.poi.ss.usermodel.Row> iterator = contestSheet.iterator();
    org.apache.poi.ss.usermodel.Row headerRow = iterator.next();
    if (headerRow == null || contestSheet.getLastRowNum() < 2) {
      RCVLogger.log("invalid RCV format: not enough rows:%d", contestSheet.getLastRowNum());
      System.exit(1);
    }

    // extract file name -- this along with ballot index will be used to generate ballot IDs
    File inFile = new File(excelFilePath);
    String cvrFileName = inFile.getName();
    int ballotIndex = 1;
    // Iterate through all rows and create a CastVoteRecord for each row
    while (iterator.hasNext()) {
      org.apache.poi.ss.usermodel.Row castVoteRecord = iterator.next();

      // TODO: determine how ballot IDs will be handled for different ballot styles
      String ballotID =  String.format("%s(%d)",cvrFileName,ballotIndex++);

      // create object for this row
      ArrayList<ContestRanking> rankings = new ArrayList<>();

      // Iterate cells in this row. Offset is used to skip ballot ID etc.
      for (int cellIndex = firstVoteColumnIndex; cellIndex < firstVoteColumnIndex + allowableRanks; cellIndex++) {

        // rank for this cell
        int rank = cellIndex - 2;
        // cell for this rank
        Cell cellForRanking = castVoteRecord.getCell(cellIndex);

        String candidate;
        if (cellForRanking == null) {
          // empty cells are treated as undeclared write-ins (for Portland / ES&S)
          candidate = undeclaredOption;
          RCVLogger.log("Empty cell -- treating as UWI");
        } else {
          if (cellForRanking.getCellType() != Cell.CELL_TYPE_STRING) {
            RCVLogger.log("unexpected cell type at ranking %d ballot %f", rank, ballotID);
            continue;
          }

          candidate = cellForRanking.getStringCellValue().trim();

          if (candidate.equals(undervoteFlag)) {
            continue;
          } else if (candidate.equals(overvoteFlag)) {
            candidate = Tabulator.explicitOvervoteFlag;
          } else if (!options.contains(candidate)) {
            if (!candidate.equals(undeclaredOption)) {
              RCVLogger.log("no match for candidate: %s", candidate);
            }
            candidate = undeclaredOption;
          }
        }

        // create and add ranking to this ballot
        ContestRanking ranking = new ContestRanking(rank, candidate);
        rankings.add(ranking);
      }

      CastVoteRecord cvr = new CastVoteRecord(ballotID, rankings);
      castVoteRecords.add(cvr);
    }

    // parsing succeeded
    return true;
  }

  // helper function to wrap file IO with error handling
  private static Sheet getBallotSheet(String excelFilePath) {
    FileInputStream inputStream;
    try {
      inputStream = new FileInputStream(new File(excelFilePath));
    } catch (IOException ex) {
      RCVLogger.log("failed to open CVR file: %s, %s", excelFilePath, ex.getMessage());
      return null;
    }

    Workbook workbook;
    try {
      workbook = new XSSFWorkbook(inputStream);
    } catch (IOException ex) {
      RCVLogger.log("failed to parse CVR file: %s, %s", excelFilePath, ex.getMessage());
      return null;
    }
    Sheet firstSheet = workbook.getSheetAt(0);
    try {
      inputStream.close();
      workbook.close();
    } catch (IOException ex) {
      RCVLogger.log("error closing CVR file: %s, %s", excelFilePath, ex.getMessage());
      return null;
    }
    return firstSheet;
  }

}