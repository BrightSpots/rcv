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

  public List<String> candidateOptions = null;
  public List<CastVoteRecord> castVoteRecords = new ArrayList<CastVoteRecord>();

  // call this to parse the given file path into a CastVoteRecordList suitable for tabulation
  // Note: this is specific for the Maine example file we were provided
  public boolean parseCVRFile(String excelFilePath) {

    Sheet contestSheet = getBallotSheet(excelFilePath);
    if (contestSheet == null) {
      RCVLogger.log("invalid RCV format: could not obtain ballot data.");
      return false;
    }

    // validate header
    Iterator<org.apache.poi.ss.usermodel.Row> iterator = contestSheet.iterator();
    org.apache.poi.ss.usermodel.Row headerRow = iterator.next();
    if (headerRow == null || contestSheet.getLastRowNum() < 2) {
      RCVLogger.log("invalid RCV format: not enough rows:%d", contestSheet.getLastRowNum());
      return false;
    }

    // number of ranks user may assign for this contest
    int allowableRanks = headerRow.getLastCellNum() - 3;
    if (allowableRanks <= 0) {
      RCVLogger.log("invalid RCV format: not enough columns: %d ", headerRow.getLastCellNum());
      return false;
    }

    // create list of candidates as we go
    Set<String> candidates = new HashSet<String>();

    // Iterate through all rows and create a CastVoteRecord for each row
    while (iterator.hasNext()) {
      org.apache.poi.ss.usermodel.Row castVoteRecord = iterator.next();

      // create object for this row
      ArrayList<ContestRanking> ballot = new ArrayList<ContestRanking>();

      // parse ID
      Cell idCell = castVoteRecord.getCell(0);
      if(idCell == null) {
        RCVLogger.log("no id for ballot row %d, skipping!", castVoteRecords.size());
        continue;
      }
      double ballotID = idCell.getNumericCellValue();

      // iterate cells in this row.  Offset is used to skip ballot id, precinct and style columns
      for (int cellIndex = 3; cellIndex < 3 + allowableRanks; cellIndex++) {

        // rank for this cell
        int rank = cellIndex - 2;
        // cell for this rank
        Cell cellForRanking = castVoteRecord.getCell(cellIndex);

        // if ballot mark was illegible there will be no cell
        if (cellForRanking == null) {
          RCVLogger.log("no cell at ranking %d ballot %f", rank, ballotID);
          continue;
        }

        if (cellForRanking.getCellType() != Cell.CELL_TYPE_STRING) {
          RCVLogger.log("unexpected cell type at ranking %d ballot %f", rank, ballotID);
          continue;
        }

        String candidate = cellForRanking.getStringCellValue();
        if (candidate.equals("undervote")) {
          RCVLogger.log("undervote at ranking %d ballot %f", rank, ballotID);
          continue;
        }

        if (candidate.equals("overvote")) {
          RCVLogger.log("overvote at ranking %d ballot %f", rank, ballotID);
          continue;
        }

        // create and add ranking to this ballot
        ContestRanking ranking = new ContestRanking(rank, candidate);
        ballot.add(ranking);

        // update the candidates set
        candidates.add(candidate);
        
      }

      // TODO: use an actual contest ID here
      CastVoteRecord cvr = new CastVoteRecord();
      ContestRankings contestRankings = new ContestRankings(ballot);
      cvr.add("1", contestRankings);
      castVoteRecords.add(cvr);

    }

    // parsing succeeded
    candidateOptions = new ArrayList<String>(candidates);
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