/*
 * Created by Jonathan Moldover, Louis Eisenberg, and Hylton Edingfield
 * Copyright 2018 Bright Spots
 * Purpose: Helper class to read and parse an xls cast vote record file into
 * cast vote record objects.
 * Version: 1.0
 */

package com.rcv;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javafx.util.Pair;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

class CVRReader {

  // container for all CastVoteRecords parsed from the input file
  final List<CastVoteRecord> castVoteRecords = new ArrayList<>();

  // purpose: helper function to wrap file IO with error handling
  // param: excelFilePath path to file for parsing
  // file access: read
  // throws: IOException if there was a problem opening or reading the file
  // returns: the first xls sheet object in the file or null if there was a problem
  private static Sheet getFirstSheet(String excelFilePath) throws IOException {
    // container for result
    Sheet firstSheet;
    try {
      // inputStream for parsing file data into memory
      FileInputStream inputStream = new FileInputStream(new File(excelFilePath));
      // excel workbook object to access to sheet objects it contains
      Workbook workbook = new XSSFWorkbook(inputStream);
      firstSheet = workbook.getSheetAt(0);
      inputStream.close();
      workbook.close();
    } catch (IOException exception) {
      Logger.severe("failed to open CVR file: %s, %s", excelFilePath, exception.getMessage());
      throw exception;
    }
    return firstSheet;
  }

  // purpose: parse the given file path into a CastVoteRecordList suitable for tabulation
  // Note: this is specific for the Maine example file we were provided
  // param: excelFilePath path to location of input cast vote record file
  // param: firstVoteColumnIndex the 0-based index where rankings begin for this ballot style
  // param: precinctColumnIndex the column containing precinct names (possibly null)
  // param: allowableRanks how many ranks are allowed for each cast vote record
  // param: candidateIDs list of all declared candidate IDs
  // param: config an ElectionConfig object specifying rules for interpreting CVR file data
  void parseCVRFile(
      String excelFilePath,
      int firstVoteColumnIndex,
      Integer idColumnIndex,
      Integer precinctColumnIndex,
      ElectionConfig config
  ) throws Exception {
    // contestSheet contains all the CVR data we will be parsing
    Sheet contestSheet = getFirstSheet(excelFilePath);

    // validate header
    // Row iterator is used to iterate through a row of data from the sheet object
    Iterator<org.apache.poi.ss.usermodel.Row> iterator = contestSheet.iterator();
    // headerRow contains the first row
    org.apache.poi.ss.usermodel.Row headerRow = iterator.next();
    // require at least one row
    if (headerRow == null || contestSheet.getLastRowNum() < 2) {
      Logger.severe("invalid RCV format: not enough rows:%d", contestSheet.getLastRowNum());
      throw new Exception();
    }

    // cvrFileName for generating cvrIDs
    String cvrFileName = new File(excelFilePath).getName();
    // cvrIndex for generating cvrIDs
    int cvrIndex = 1;

    // Iterate through all rows and create a CastVoteRecord for each row
    while (iterator.hasNext()) {
      // row object is used to iterate CVR file data for this CVR
      org.apache.poi.ss.usermodel.Row castVoteRecordRow = iterator.next();
      // computed unique ID for this CVR
      String computedCastVoteRecordID = String.format("%s(%d)", cvrFileName, cvrIndex++);
      // supplied (by input file) unique ID for this CVR
      String suppliedCastVoteRecordID = null;
      // list of rankings read from this row
      ArrayList<Pair<Integer, String>> rankings = new ArrayList<>();
      // list of raw strings read from this row, for the audit log
      ArrayList<String> fullCVRData = new ArrayList<>();
      // the precinct for this ballot
      String precinct = null;

      // Iterate over all expected cells in this row storing cvrData and rankings as we go.
      // cellIndex ranges from 0 to the last expected rank column index
      for (
          int cellIndex = 0;
          cellIndex < firstVoteColumnIndex + config.getMaxRankingsAllowed();
          cellIndex++
      ) {
        // cell object contains data for the current cell
        Cell cvrDataCell = castVoteRecordRow.getCell(cellIndex);
        String cellString = getStringFromCell(cvrDataCell);

        if (cellString == null) {
          fullCVRData.add("empty cell");
        } else {
          fullCVRData.add(cellString);
        }

        if (cellString != null) {
          if (precinctColumnIndex != null && cellIndex == precinctColumnIndex) {
            precinct = cellString;
          } else if (idColumnIndex != null && cellIndex == idColumnIndex) {
            suppliedCastVoteRecordID = cellString;
          }
        }

        // if we haven't reached a vote cell continue to the next cell
        if (cellIndex < firstVoteColumnIndex) {
          continue;
        }

        // rank for this cell
        int rank = cellIndex - firstVoteColumnIndex + 1;
        // candidate will be the candidate selected at this rank
        String candidate;
        if (cvrDataCell == null) {
          // empty cells are sometimes treated as undeclared write-ins (Portland / ES&S)
          if (config.isTreatBlankAsUndeclaredWriteInEnabled()) {
            candidate = config.getUndeclaredWriteInLabel();
            Logger.warn("Empty cell -- treating as UWI");
          } else {
            // just ignore this cell
            continue;
          }
        } else {
          if (cvrDataCell.getCellTypeEnum() != CellType.STRING) {
            Logger.warn(
                "unexpected cell type at ranking %d ballot %s",
                rank,
                computedCastVoteRecordID
            );
            continue;
          }
          candidate = cvrDataCell.getStringCellValue().trim();

          if (candidate.equals(config.getUndervoteLabel())) {
            continue;
          } else if (candidate.equals(config.getOvervoteLabel())) {
            candidate = Tabulator.explicitOvervoteLabel;
          } else if (!config.getCandidateCodeList().contains(candidate)) {
            if (!candidate.equals(config.getUndeclaredWriteInLabel())) {
              Logger.warn("no match for candidate: %s", candidate);
            }
            candidate = config.getUndeclaredWriteInLabel();
          }
        }
        // create and add new ranking pair to the rankings list
        Pair<Integer, String> ranking = new Pair<>(rank, candidate);
        rankings.add(ranking);
      }

      // we now have all required data for the new CastVoteRecord object
      // create it and add to the list of all CVRs
      CastVoteRecord cvr = new CastVoteRecord(
          computedCastVoteRecordID,
          suppliedCastVoteRecordID,
          precinct,
          fullCVRData,
          rankings
      );
      castVoteRecords.add(cvr);
    }
  }

  // function: getStringFromCell
  // purpose: parses a cell's contents into a String object
  // param: cvrDataCell is the spreadsheet cell
  // returns: the string
  private String getStringFromCell(Cell cvrDataCell) {
    // value to return
    String cellString;

    if (cvrDataCell == null) {
      cellString = null;
    } else if (cvrDataCell.getCellTypeEnum() == CellType.NUMERIC) {
      // parsed numeric data (we only expect integers)
      Integer intValue = (int) cvrDataCell.getNumericCellValue();
      // convert back to String (we only store String data from CVR files)
      cellString = intValue.toString();
    } else if (cvrDataCell.getCellTypeEnum() == CellType.STRING) {
      cellString = cvrDataCell.getStringCellValue();
    } else {
      cellString = null;
    }

    return cellString;
  }
}
