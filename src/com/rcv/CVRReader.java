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
 * Helper class to read and parse an xls Cast Vote Record File
 *
 * Assumptions:
 * - one contest per file
 * - the first sheet is the only one we're interested in
 * - columns after ballot style are the ballot selections ordered by rank (most to least preferred)
 * - a non-existent cell (image of a ballot mark when workbook is opened in excel?) means no vote
 *
 */

public class CVRReader {

  public List<CastVoteRecord> castVoteRecords = new ArrayList<>();

  // Call this to parse the given file path into a CastVoteRecordList suitable for tabulation
  // Note: this is specific for the Maine example file we were provided
  public void parseCVRFile(
    String excelFilePath,
    int firstVoteColumnIndex,
    int allowableRanks,
    List<String>options,
    ElectionConfig config
  ) {
    Sheet contestSheet = getBallotSheet(excelFilePath);
    if (contestSheet == null) {
      Logger.log("invalid RCV format: could not obtain ballot data.");
      System.exit(1);
    }

    // validate header
    Iterator<org.apache.poi.ss.usermodel.Row> iterator = contestSheet.iterator();
    org.apache.poi.ss.usermodel.Row headerRow = iterator.next();
    if (headerRow == null || contestSheet.getLastRowNum() < 2) {
      Logger.log("invalid RCV format: not enough rows:%d", contestSheet.getLastRowNum());
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

      // create an object to store CVR data for auditing
      ArrayList<String> fullCVRData = new ArrayList<>();

      // Iterate all expected cells in this row:
      for (int cellIndex = 0; cellIndex < firstVoteColumnIndex + allowableRanks; cellIndex++) {

        // cache all cvr cell data for audit report
        Cell cvrDataCell = castVoteRecord.getCell(cellIndex);
        if(cvrDataCell == null) {
          fullCVRData.add("empty cell");
        } else if(cvrDataCell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
          double data = cvrDataCell.getNumericCellValue();
          fullCVRData.add(Double.toString(data));
        } else if (cvrDataCell.getCellType() == Cell.CELL_TYPE_STRING) {
          fullCVRData.add(cvrDataCell.getStringCellValue());
        } else {
          fullCVRData.add("unexpected data type");
        }

        // if we haven't reached a vote cell continue to the next cell
        if(cellIndex < firstVoteColumnIndex) {
          continue;
        }

        // rank for this cell
        int rank = cellIndex - firstVoteColumnIndex + 1;

        String candidate;
        if (cvrDataCell == null) {
          // empty cells are sometimes treated as undeclared write-ins (for Portland / ES&S)
          if (config.treatBlankAsUWI()) {
            candidate = config.undeclaredWriteInLabel();
            Logger.log("Empty cell -- treating as UWI");
          } else {
            continue; // just ignore this cell
          }
        } else {
          if (cvrDataCell.getCellType() != Cell.CELL_TYPE_STRING) {
            Logger.log("unexpected cell type at ranking %d ballot %f", rank, ballotID);
            continue;
          }

          candidate = cvrDataCell.getStringCellValue().trim();

          if (candidate.equals(config.undervoteLabel())) {
            continue;
          } else if (candidate.equals(config.overvoteLabel())) {
            candidate = Tabulator.explicitOvervoteLabel;
          } else if (!options.contains(candidate)) {
            if (!candidate.equals(config.undeclaredWriteInLabel())) {
              Logger.log("no match for candidate: %s", candidate);
            }
            candidate = config.undeclaredWriteInLabel();
          }
        }

        // create and add ranking to this ballot
        ContestRanking ranking = new ContestRanking(rank, candidate);
        rankings.add(ranking);
      }

      CastVoteRecord cvr = new CastVoteRecord(cvrFileName, ballotID, rankings, fullCVRData);
      castVoteRecords.add(cvr);
    }
    // parsing complete
  }

  // helper function to wrap file IO with error handling
  private static Sheet getBallotSheet(String excelFilePath) {
    Sheet firstSheet = null;
    try {
      FileInputStream inputStream = new FileInputStream(new File(excelFilePath));
      Workbook workbook = new XSSFWorkbook(inputStream);
      firstSheet = workbook.getSheetAt(0);
      inputStream.close();
      workbook.close();
    } catch (IOException ex) {
      Logger.log("failed to process CVR file: %s, %s", excelFilePath, ex.getMessage());
    }
    return firstSheet;
  }

}
