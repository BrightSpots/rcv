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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import javafx.util.Pair;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

class CVRReader {

  // config for the election
  private final ElectionConfig config;
  // path of the source file
  private final String excelFilePath;
  // column index of first ranking
  private final int firstVoteColumnIndex;
  // column index of CVR ID (if present)
  private final Integer idColumnIndex;
  // column index of precinct name (if present)
  private final Integer precinctColumnIndex;

  // function: CVRReader
  // param: config an ElectionConfig object specifying rules for interpreting CVR file data
  // param: excelFilePath path to location of input cast vote record file
  // param: firstVoteColumnIndex the 0-based index where rankings begin for this ballot style
  // param: idColumnIndex the column containing the CVR ID (possibly null)
  // param: precinctColumnIndex the column containing precinct names (possibly null)
  CVRReader(
      ElectionConfig config,
      String excelFilePath,
      int firstVoteColumnIndex,
      Integer idColumnIndex,
      Integer precinctColumnIndex) {
    this.config = config;
    this.excelFilePath = excelFilePath;
    this.firstVoteColumnIndex = firstVoteColumnIndex;
    this.idColumnIndex = idColumnIndex;
    this.precinctColumnIndex = precinctColumnIndex;
  }

  // function: getFirstSheet
  // purpose: helper function to wrap file IO with error handling
  // param: excelFilePath path to file for parsing
  // file access: read
  // returns: the first xls sheet object in the file or null if there was a problem
  private static Sheet getFirstSheet(String excelFilePath) {
    // container for result
    Sheet firstSheet = null;
    try {
      // inputStream for parsing file data into memory
      FileInputStream inputStream = new FileInputStream(new File(excelFilePath));
      // excel workbook object to access to sheet objects it contains
      Workbook workbook = new XSSFWorkbook(inputStream);
      firstSheet = workbook.getSheetAt(0);
      inputStream.close();
      workbook.close();
    } catch (IOException exception) {
      Logger.severe("Failed to open CVR file: %s\n%s", excelFilePath, exception.getMessage());
    }
    return firstSheet;
  }

  // function: parseCVRFile
  // purpose: parse the given file path into a List of CastVoteRecords suitable for tabulation
  // returns: list of parsed CVRs
  List<CastVoteRecord> parseCVRFile() throws UnrecognizedCandidateException {
    // contestSheet contains all the CVR data we will be parsing
    Sheet contestSheet = getFirstSheet(excelFilePath);
    // container for all CastVoteRecords parsed from the input file
    List<CastVoteRecord> castVoteRecords = new LinkedList<>();

    if (contestSheet != null) {
      // validate header
      // Row iterator is used to iterate through a row of data from the sheet object
      Iterator<Row> iterator = contestSheet.iterator();
      // headerRow contains the first row
      Row headerRow = iterator.next();
      // require at least one non-header row
      if (headerRow == null || contestSheet.getLastRowNum() < 2) {
        Logger.severe(
            "Invalid CVR source file %s: not enough rows (%d)",
            this.excelFilePath, contestSheet.getLastRowNum());
      }

      // cvrFileName for generating cvrIDs
      String cvrFileName = new File(excelFilePath).getName();
      // cvrIndex for generating cvrIDs
      int cvrIndex = 1;
      int unrecognizedCandidateCount = 0;

      // Iterate through all rows and create a CastVoteRecord for each row
      while (iterator.hasNext()) {
        // cvr is the object parsed from the row
        try {
          CastVoteRecord cvr = parseRow(iterator.next(), cvrFileName, cvrIndex++);
          castVoteRecords.add(cvr);
        } catch (UnrecognizedCandidateException e) {
          unrecognizedCandidateCount++;
        }
      }

      if (unrecognizedCandidateCount > 0) {
        throw new UnrecognizedCandidateException();
      }
    }

    return castVoteRecords;
  }

  // function: parseRow
  // purpose: parse a single row into a CastVoteRecord
  // returns: a CastVoteRecord object
  private CastVoteRecord parseRow(
      Row castVoteRecordRow,
      String cvrFileName,
      int cvrIndex) throws UnrecognizedCandidateException {
    // row object is used to iterate CVR file data for this CVR
    // computed unique ID for this CVR
    String computedCastVoteRecordID = String.format("%s(%d)", cvrFileName, cvrIndex);
    // supplied (by input file) unique ID for this CVR
    String suppliedCastVoteRecordID = null;
    // list of rankings read from this row
    LinkedList<Pair<Integer, String>> rankings = new LinkedList<>();
    // list of raw strings read from this row, for the audit log
    LinkedList<String> fullCVRData = new LinkedList<>();
    // the precinct for this ballot
    String precinct = null;

    // Iterate over all expected cells in this row storing cvrData and rankings as we go.
    // cellIndex ranges from 0 to the last expected rank column index
    for (int cellIndex = 0;
        cellIndex < firstVoteColumnIndex + config.getMaxRankingsAllowed();
        cellIndex++) {
      // cell object contains data for the current cell
      Cell cvrDataCell = castVoteRecordRow.getCell(cellIndex);
      String cellString = getStringFromCell(cvrDataCell);

      fullCVRData.add(Objects.requireNonNullElse(cellString, "empty cell"));

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
              "unexpected cell type at ranking %d ballot %s", rank, computedCastVoteRecordID);
          continue;
        }
        candidate = cvrDataCell.getStringCellValue().trim();

        if (candidate.equals(config.getUndervoteLabel())) {
          continue;
        } else if (candidate.equals(config.getOvervoteLabel())) {
          candidate = Tabulator.explicitOvervoteLabel;
        } else if (!config.getCandidateCodeList().contains(candidate)
            && !candidate.equals(config.getUndeclaredWriteInLabel())) {
          Logger.severe("no match for candidate: %s", candidate);
          throw new UnrecognizedCandidateException();
        }
      }
      // create and add new ranking pair to the rankings list
      Pair<Integer, String> ranking = new Pair<>(rank, candidate);
      rankings.add(ranking);
    }

    // we now have all required data for the new CastVoteRecord object
    // create it and add to the list of all CVRs
    return new CastVoteRecord(
        computedCastVoteRecordID, suppliedCastVoteRecordID, precinct, fullCVRData, rankings);
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

  static class UnrecognizedCandidateException extends Exception {

    UnrecognizedCandidateException() {
      super();
    }
  }
}
