/*
 * Ranked Choice Voting Universal Tabulator
 * Copyright (c) 2018 Jonathan Moldover, Louis Eisenberg, and Hylton Edingfield
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See
 * the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this
 * program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Purpose:
 * Helper class to read and parse an xls cast vote record file into cast vote record objects.
 */

package com.rcv;

import com.rcv.RawContestConfig.CVRSource;
import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javafx.util.Pair;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

class StreamingCVRReader {

  // config for the contest
  private final ContestConfig config;
  // path of the source file
  private final String excelFilePath;
  // name of the source file
  private final String excelFileName;
  // column index of first ranking
  private final int firstVoteColumnIndex;
  // column index of CVR ID (if present)
  private final Integer idColumnIndex;
  // column index of currentPrecinct name (if present)
  private final Integer precinctColumnIndex;
  // used for generating CVR IDs
  private int CVRIndex = 0;
  // map for tracking unrecognized candidates during parsing
  Map<String, Integer> unrecognizedCandidateCounts = new HashMap<>();
  // list of currentRankings read from this row
  LinkedList<Pair<Integer, String>> currentRankings;
  // list of raw strings read from this row, for the audit log
  LinkedList<String> currentCVRData;
  // current vote supplied ID
  String currentSuppliedCVRID;
  // current currentPrecinct ID
  String currentPrecinct;
  // place to store input CVR list for appending new CVRs
  List<CastVoteRecord> CVRList;
  // last cell parsed for this CVR
  int lastRankSeen;

  // function: CVRReader
  // purpose: class constructor
  // param: config an ContestConfig object specifying rules for interpreting CVR file data
  // param: source file to read
  StreamingCVRReader(ContestConfig config, CVRSource source) {
    this.config = config;
    this.excelFilePath = source.getFilePath();
    // cvrFileName for generating cvrIDs
    this.excelFileName = new File(excelFilePath).getName();

    // to keep our code simple, we convert 1-indexed user-supplied values to 0-indexed here
    this.firstVoteColumnIndex = source.getFirstVoteColumnIndex() - 1;
    this.idColumnIndex = source.getIdColumnIndex() != null ? source.getIdColumnIndex() - 1 : null;
    this.precinctColumnIndex =
        source.getPrecinctColumnIndex() != null ? source.getPrecinctColumnIndex() - 1 : null;
  }

  // given Excel-style address string return the address as a pair of Integers
  // representing zero-based column and row of the cell address
  public static Pair<Integer, Integer> getCellAddress(String address) {
    // this regex will parse a string into
    // a sequence of one or more non-digits followed by a sequence of one or more digits
    // and store these substrings into addressParts array
    String[] addressParts = address.split("(?<=\\D)(?=\\d)");
    if(addressParts.length != 2) {
      Logger.log(Level.SEVERE, "invalid cell address:" + address);
      throw new InvalidParameterException();
    }
    // row is the row of the cell
    Integer row = Integer.parseInt(addressParts[1]) - 1;
    // col is the 0-based column of the cell
    Integer col = getColumnIndex(addressParts[0]);
    // return the result as a Pair
    return new Pair<>(col, row);
  }
  // function: getColumnIndex
  // purpose: given alphabetic representation of an Excel columnAddress returns the zero-based
  // integer index of the column, e.g. "A" returns 0 and "AB" returns 27
  // param: columnAddress the input columnAddress String
  // return: column index
  public static int getColumnIndex(String columnAddress) {
    // result is column index
    int result = 0;
    // i indexes over the "digits" of the columnAddress string
    for (int i = 0; i < columnAddress.length(); i++) {
      // at each iteration the current total will be multiplied by 26, "shifting" it left one place
      result *= 26;
      // charValue maps the current character to a value between 0 and 25
      int charValue = columnAddress.charAt(i) - 'A';
      assert(charValue >= 0 && charValue < 26);
      result += charValue;
    }
    return result;
  }

  // function: handleEmptyCells
  // purpose: Handle empty cells encountered while parsing a CVR.  Unlike empty rows, empty cells
  // do not trigger parsing callbacks so their existence must be inferred.
  // param: currentRank the rank at which we should stop handling empty cells
  void handleEmptyCells(int currentRank) {
      // previousRank is the rank after which we should start handling empty cells
      // rank iterates between previousRank and endRank adding audit data and possibly UWI rankings
      for (int rank = lastRankSeen + 1;  rank < currentRank; rank++) {
        // add data to audit log
        currentCVRData.add("empty cell");
        // add UWI ranking if required by settings
        if (config.isTreatBlankAsUndeclaredWriteInEnabled()) {
          Logger.log(Level.WARNING, "Empty cell -- treating as UWI");
          // add the new ranking
          currentRankings.add(new Pair<>(rank, config.getUndeclaredWriteInLabel()));
        }
      }
  }

  // function: beginCVR
  // purpose: prepare to begin parsing a new CVR
  void beginCVR() {
    // setup containers for parsing a new cvr from this row
    CVRIndex++;
    currentRankings = new LinkedList<>();
    currentCVRData = new LinkedList<>();
    currentSuppliedCVRID = null;
    currentPrecinct = null;
    lastRankSeen = 0;
    Logger.log(Level.INFO, "new cvr:" + CVRIndex);
  }

  // function: endCVR
  // purpose: complete construction of new CVR object
  void endCVR() {
    // handle any empty cells which may appear at the end of this row
    handleEmptyCells(config.getMaxRankingsAllowed() + 1);
    // determine what the new cvr ID will be
    String computedCastVoteRecordID = String
        .format("%s(%d)", StreamingCVRReader.this.excelFileName, CVRIndex);
    // create new cast vote record
    CastVoteRecord newRecord = new CastVoteRecord(
        computedCastVoteRecordID, currentSuppliedCVRID, currentPrecinct,
        currentCVRData, currentRankings);
    // add it to overall list
    CVRList.add(newRecord);
  }

  // function: CVRCell
  // purpose: handle a new CVR cell
  // param: col column of this cell
  // param: row of this cell (unused)
  // param: cellData data contained in this cell
  void CVRCell(int col, int row, String cellData) {

    // add cell data to full audit string
    currentCVRData.add(cellData);

    // check for a currentPrecinct string or CVR ID string
    if (precinctColumnIndex != null && col == precinctColumnIndex) {
      currentPrecinct = cellData;
    } else if (idColumnIndex != null && col == idColumnIndex) {
      currentSuppliedCVRID = cellData;
    }

    // check for unexpected column value
    if (col >= firstVoteColumnIndex + config.getMaxRankingsAllowed()) {
      Logger.log(Level.WARNING,String.format("unexpected cell data at col:%d: %s", col, cellData));
    } else if (col >= firstVoteColumnIndex ) {
      // this column is in valid range

      // handle any empty cells which may exist between this cell and any previous one
      Integer currentRank = col - firstVoteColumnIndex + 1;
      handleEmptyCells(currentRank);
      // get the candidate name
      String candidate = cellData.trim();
      // check for an undervote label -- it will not result in a ranking
      if(!candidate.equals(config.getUndervoteLabel())) {
        // if candidate is overvote map it to our internal overvote string
        if (candidate.equals(config.getOvervoteLabel())) {
          candidate = Tabulator.EXPLICIT_OVERVOTE_LABEL;
        } else if (!config.getCandidateCodeList().contains(candidate)
            && !candidate.equals(config.getUndeclaredWriteInLabel())) {
          // add unrecognized candidate to the map
          unrecognizedCandidateCounts.merge(candidate, 1, Integer::sum);
        }
        // create and add the new ranking
        Pair<Integer, String> ranking = new Pair<>(currentRank, candidate);
        currentRankings.add(ranking);
      }
      lastRankSeen = currentRank;
    }

  }


  // function: parseCVRFile
  // purpose: parse the given file into a List of CastVoteRecords for tabulation
  // param: castVoteRecords existing list to append new CastVoteRecords to
  // returns: list of parsed CVRs
  List<CastVoteRecord> parseCVRFile(List<CastVoteRecord> castVoteRecords)
      throws UnrecognizedCandidatesException, OpenXML4JException, SAXException, IOException {

    // cache the cvr list so it is accessible in callbacks
    CVRList = castVoteRecords;

    // open the zip package
    OPCPackage pkg = OPCPackage.open(excelFilePath);
    // pull out strings
    ReadOnlySharedStringsTable sharedStrings = new ReadOnlySharedStringsTable(pkg);
    // reader is used to extract styles data
    XSSFReader reader = new XSSFReader(pkg);
    // styles data is used for creating ContentHandler
    StylesTable styles = reader.getStylesTable();
    // SheetContentsHandler is used to handle parsing callbacks
    SheetContentsHandler sheetContentsHandler = new SheetContentsHandler() {
      // function: startRow
      // purpose: startRow callback handler during xml parsing
      // param: i the row which has started
      @Override
      public void startRow(int i) {
        // we assume exactly one header row so skip the first row
        if(i > 0) {
          beginCVR();
        }
      }

      // function: endRow
      // purpose: endRow callback handler during xml parsing
      // row has completed, we will create a new cvr object
      // param: i the row which has ended
      @Override
      public void endRow(int i) {
        // we assume exactly one header row so skip the first row
        if(i > 0) {
          endCVR();
        }
      }

      // function: cell
      // purpose: cell callback handler during xml parsing
      // param: s cell address encoded as col,row
      // param: s1 cell data
      // param: xssfComment additional cell data (unused)
      @Override
      public void cell(String s, String s1, XSSFComment xssfComment) {
        // address contains the row and col of this cell
        Pair<Integer,Integer> address = getCellAddress(s);
        int col = address.getKey();
        int row = address.getValue();
        // we assume exactly one header row so skip cells in the first row
        if(row > 0) {
          CVRCell(col, row, s1);
        }
      }

      // function: headerFooter
      // purpose: header footer callback from xml parsing - unused
      // param: s header footer data
      // param: b header footer data
      // param: s1 header footer data
      @Override
      public void headerFooter(String s, boolean b, String s1) {
        Logger.log(Level.WARNING, String.format("unexpected xml data: %s %d %s", s, b, s1));
      }
    };

    // create the ContentHandler to handle parsing callbacks
    ContentHandler handler = new XSSFSheetXMLHandler(styles,
        sharedStrings,
        sheetContentsHandler,
        true);

    Logger.log(Level.INFO,"Streaming XML reader");

    // create the xml parser and set content handler
    XMLReader parser = XMLReaderFactory.createXMLReader();
    parser.setContentHandler(handler);
    // trigger parsing
    parser.parse(new InputSource(reader.getSheetsData().next()));

    // throw if there were any unrecognized candidates -- this is considered bad
    if (this.unrecognizedCandidateCounts.size() > 0) {
      throw new UnrecognizedCandidatesException(unrecognizedCandidateCounts);
    }

    // return the input list with additions
    return CVRList;
  }


  static class UnrecognizedCandidatesException extends Exception {

    public final Map<String, Integer> candidateCounts;

    UnrecognizedCandidatesException(Map<String, Integer> candidateCounts) {
      this.candidateCounts = candidateCounts;
    }

  }
}
