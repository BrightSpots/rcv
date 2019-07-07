/*
 * Universal RCV Tabulator
 * Copyright (c) 2017-2019 Bright Spots Developers.
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
 */

/*
 * Purpose:
 * Helper class to read and parse an XLS cast vote record file into cast vote record objects.
 */

package network.brightspots.rcv;

import static network.brightspots.rcv.Utils.isNullOrBlank;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javafx.util.Pair;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import network.brightspots.rcv.RawContestConfig.CVRSource;
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

class StreamingCVRReader {

  // this indicates a missing precinct Id in output files
  private final String MISSING_PRECINCT_ID = "missing_precinct_id";
  // config for the contest
  private final ContestConfig config;
  // path of the source file
  private final String excelFilePath;
  // name of the source file
  private final String excelFileName;
  // column index of first ranking
  private final int firstVoteColumnIndex;
  // row index of first CVR
  private final int firstVoteRowIndex;
  // column index of CVR ID (if present)
  private final Integer idColumnIndex;
  // column index of currentPrecinct name (if present)
  private final Integer precinctColumnIndex;
  // map for tracking unrecognized candidates during parsing
  private final Map<String, Integer> unrecognizedCandidateCounts = new HashMap<>();
  // used for generating CVR IDs
  private int cvrIndex = 0;
  // list of currentRankings for CVR in progress
  private LinkedList<Pair<Integer, String>> currentRankings;
  // list of raw strings for CVR in progress
  private LinkedList<String> currentCVRData;
  // supplied CVR ID for CVR in progress
  private String currentSuppliedCvrId;
  // precinct ID for CVR in progress
  private String currentPrecinct;
  // place to store input CVR list (new CVRs will be appended as we parse)
  private List<CastVoteRecord> cvrList;
  // store precinctIDs (new IDs will be added as we parse)
  private Set<String> precinctIDs;
  // last rankings cell observed for CVR in progress
  private int lastRankSeen;
  // flag indicating data issues during parsing
  private boolean encounteredDataErrors = false;

  // function: StreamingCVRReader
  // purpose: class constructor
  // param: config an ContestConfig object specifying rules for interpreting CVR file data
  // param: source file to read
  StreamingCVRReader(ContestConfig config, CVRSource source) {
    this.config = config;
    this.excelFilePath = config.resolveConfigPath(source.getFilePath());
    // cvrFileName for generating cvrIDs
    this.excelFileName = new File(excelFilePath).getName();

    // to keep our code simple, we convert 1-indexed user-supplied values to 0-indexed here
    this.firstVoteColumnIndex = source.getFirstVoteColumnIndex() - 1;
    this.firstVoteRowIndex = source.getFirstVoteRowIndex() - 1;
    this.idColumnIndex =
        isNullOrBlank(source.getIdColumnIndex()) ? null
            : Integer.parseInt(source.getIdColumnIndex()) - 1;
    this.precinctColumnIndex = !isNullOrBlank(source.getPrecinctColumnIndex()) ?
        Integer.parseInt(source.getPrecinctColumnIndex()) - 1 : null;
  }

  // given Excel-style address string return the cell address as a pair of Integers
  // representing zero-based column and row of the cell address
  private static Pair<Integer, Integer> getCellAddress(String address) {
    // this regex will parse a string into
    // a sequence of one or more non-digits followed by a sequence of one or more digits
    // and store these substrings into addressParts array
    String[] addressParts = address.split("(?<=\\D)(?=\\d)");
    if (addressParts.length != 2) {
      Logger.log(Level.SEVERE, "Invalid cell address: %s", address);
      throw new InvalidParameterException();
    }
    // row is the 0-based row of the cell
    Integer row = Integer.parseInt(addressParts[1]) - 1;
    // col is the 0-based column of the cell
    Integer col = getColumnIndex(addressParts[0]);
    // return the result as a Pair
    return new Pair<>(col, row);
  }

  // function: getColumnIndex
  // purpose: given alphabetic representation of an Excel columnAddress returns the zero-based
  // integer index of the column, e.g. "A" returns 0 and "AB" returns 27
  // param: columnAddress the column portion of an Excel cell address string
  // return: column index
  private static int getColumnIndex(String columnAddress) {
    // result is column index
    int result = 0;
    // i indexes over the "digits" of the columnAddress string
    for (int i = 0; i < columnAddress.length(); i++) {
      // at each iteration the current total will be multiplied by 26, "shifting" it left one place
      result *= 26;
      // charValue maps the current character to a value between 1 and 26
      int charValue = columnAddress.charAt(i) - '@';
      if (charValue < 1 || charValue > 26) {
        Logger.log(Level.SEVERE, "Invalid cell address: %s", columnAddress);
        throw new InvalidParameterException();
      }
      result += charValue;
    }
    // finally subtract one to convert to zero-based index
    return result - 1;
  }

  // function: handleEmptyCells
  // purpose: Handle empty cells encountered while parsing a CVR.  Unlike empty rows, empty cells
  // do not trigger parsing callbacks so their existence must be inferred and handled when they
  // occur in a rankings cell.
  // param: currentRank the rank at which we stop inferring empty cells.
  private void handleEmptyCells(int currentRank) {
    // rank iterates between lastRankSeen and currentRank adding audit data and UWI rankings
    for (int rank = lastRankSeen + 1; rank < currentRank; rank++) {
      // add data to audit log
      currentCVRData.add("empty cell");
      // add UWI ranking if required by settings
      if (config.isTreatBlankAsUndeclaredWriteInEnabled()) {
        // add the new ranking
        currentRankings.add(new Pair<>(rank, config.getUndeclaredWriteInLabel()));
      }
    }
  }

  // function: beginCVR
  // purpose: prepare to begin parsing a new CVR
  private void beginCVR() {
    // setup data structures for parsing a new CVR
    cvrIndex++;
    currentRankings = new LinkedList<>();
    currentCVRData = new LinkedList<>();
    currentSuppliedCvrId = null;
    currentPrecinct = null;
    lastRankSeen = 0;
  }

  // function: endCVR
  // purpose: complete construction of new CVR object
  private void endCVR() {
    // handle any empty cells which may appear at the end of this row
    handleEmptyCells(config.getMaxRankingsAllowed() + 1);
    // determine what the new cvr ID will be
    String computedCastVoteRecordID =
        String.format("%s-%d", ResultsWriter.sanitizeStringForOutput(excelFileName), cvrIndex);

    // add precinct ID if needed
    if (precinctColumnIndex != null) {
      if (currentPrecinct == null) {
        // group precincts with missing Ids here
        Logger.log(
            Level.WARNING,
            "Precinct identifier not found for cast vote record: %s",
            computedCastVoteRecordID);
        currentPrecinct = MISSING_PRECINCT_ID;
      }
      precinctIDs.add(currentPrecinct);
    }

    // look for missing Cvr Id
    if (idColumnIndex != null && currentSuppliedCvrId == null) {
      Logger.log(
          Level.SEVERE, "Cast vote record identifier not found for: %s", computedCastVoteRecordID);
      encounteredDataErrors = true;
    }

    // create new cast vote record
    CastVoteRecord newRecord =
        new CastVoteRecord(
            computedCastVoteRecordID,
            currentSuppliedCvrId,
            currentPrecinct,
            currentCVRData,
            currentRankings);
    // add it to overall list
    cvrList.add(newRecord);

    // provide some user feedback on the CVR count
    if (cvrList.size() % 50000 == 0) {
      Logger.log(Level.INFO, "Parsed %d cast vote records.", cvrList.size());
    }
  }

  // function: cvrCell
  // purpose: handle CVR cell data callback
  // param: col column of this cell
  // param: cellData data contained in this cell
  private void cvrCell(int col, String cellData) {

    // add cell data to "full" audit string
    currentCVRData.add(cellData);

    // check for a currentPrecinct string or CVR ID string
    if (precinctColumnIndex != null && col == precinctColumnIndex) {
      currentPrecinct = cellData;
    } else if (idColumnIndex != null && col == idColumnIndex) {
      currentSuppliedCvrId = cellData;
    }

    // see if this column is in the ranking range
    if (col >= firstVoteColumnIndex
        && col < firstVoteColumnIndex + config.getMaxRankingsAllowed()) {

      // rank for this column
      Integer currentRank = col - firstVoteColumnIndex + 1;
      // handle any empty cells which may exist between this cell and any previous one
      handleEmptyCells(currentRank);
      // get the candidate name
      String candidate = cellData.trim();
      // skip undervotes
      if (!candidate.equals(config.getUndervoteLabel())) {
        // map overvotes to our internal overvote string
        if (candidate.equals(config.getOvervoteLabel())) {
          candidate = Tabulator.EXPLICIT_OVERVOTE_LABEL;
        } else if (!config.getCandidateCodeList().contains(candidate)
            && !candidate.equals(config.getUndeclaredWriteInLabel())) {
          // this is an unrecognized candidate so add it to the unrecognized candidate map
          // this helps identify problems with CVRs
          unrecognizedCandidateCounts.merge(candidate, 1, Integer::sum);
        }
        // create and add the new ranking
        Pair<Integer, String> ranking = new Pair<>(currentRank, candidate);
        currentRankings.add(ranking);
      }
      // update lastRankSeen - used to handle empty ranking cells
      lastRankSeen = currentRank;
    }
  }

  // function: parseCVRFile
  // purpose: parse the given file into a List of CastVoteRecords for tabulation
  // param: castVoteRecords existing list to append new CastVoteRecords to
  // param: precinctIDs existing set of precinctIDs discovered during CVR parsing
  void parseCVRFile(List<CastVoteRecord> castVoteRecords, Set<String> precinctIDs)
      throws UnrecognizedCandidatesException, OpenXML4JException, SAXException, IOException,
      ParserConfigurationException, CvrDataFormatException {

    // cache the cvr list so it is accessible in callbacks
    cvrList = castVoteRecords;
    // cache precinctIDs set so it is accessible in callbacks
    this.precinctIDs = precinctIDs;

    // open the zip package
    OPCPackage pkg = OPCPackage.open(excelFilePath);
    // pull out strings
    ReadOnlySharedStringsTable sharedStrings = new ReadOnlySharedStringsTable(pkg);
    // XSSF reader is used to extract styles data
    XSSFReader xssfReader = new XSSFReader(pkg);
    // styles data is used for creating ContentHandler
    StylesTable styles = xssfReader.getStylesTable();
    // SheetContentsHandler is used to handle parsing callbacks
    SheetContentsHandler sheetContentsHandler =
        new SheetContentsHandler() {
          // function: startRow
          // purpose: startRow callback handler during xml parsing
          // param: i the row which has started
          @Override
          public void startRow(int i) {
            if (i >= firstVoteRowIndex) {
              beginCVR();
            }
          }

          // function: endRow
          // purpose: endRow callback handler during xml parsing
          // row has completed, we will create a new cvr object
          // param: i the row which has ended
          @Override
          public void endRow(int i) {
            if (i >= firstVoteRowIndex) {
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
            Pair<Integer, Integer> address = getCellAddress(s);
            int col = address.getKey();
            int row = address.getValue();
            if (row >= firstVoteRowIndex) {
              cvrCell(col, s1);
            }
          }

          // function: headerFooter
          // purpose: header footer callback from xml parsing - unused
          // param: s header footer data
          // param: b header footer data
          // param: s1 header footer data
          @Override
          public void headerFooter(String s, boolean b, String s1) {
            Logger.log(Level.WARNING, "Unexpected XML data: %s %b %s", s, b, s1);
          }
        };

    // create the ContentHandler to handle parsing callbacks
    ContentHandler handler =
        new XSSFSheetXMLHandler(styles, sharedStrings, sheetContentsHandler, true);

    // create the XML reader and set content handler
    SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
    saxParserFactory.setNamespaceAware(true);
    SAXParser saxParser = saxParserFactory.newSAXParser();
    XMLReader xmlReader = saxParser.getXMLReader();
    xmlReader.setContentHandler(handler);
    // trigger parsing
    xmlReader.parse(new InputSource(xssfReader.getSheetsData().next()));
    // close zip file
    pkg.close();

    // throw if there were any unrecognized candidates -- this is considered bad
    if (unrecognizedCandidateCounts.size() > 0) {
      throw new UnrecognizedCandidatesException(unrecognizedCandidateCounts);
    }

    if (encounteredDataErrors) {
      throw new CvrDataFormatException();
    }
  }

  // exception class for miscellaneous unexpected data errors encountered during Cvr parsing
  // e.g. missing Cvr Id
  static class CvrDataFormatException extends Exception {

  }

  // exception class used when an unrecognized candidate is encountered during cvr parsing
  // purpose is to help identify issues with cast vote record files and configuration files
  static class UnrecognizedCandidatesException extends Exception {

    // candidateCounts maps an unrecognized candidate name to the count of how many times it was
    // encountered during CVR parsing
    final Map<String, Integer> candidateCounts;

    // function: UnrecognizedCandidatesException
    // purpose: constructor
    // param: candidateCounts maps unrecognized candidates to the number of times they were
    // encountered during parsing
    UnrecognizedCandidatesException(Map<String, Integer> candidateCounts) {
      this.candidateCounts = candidateCounts;
    }
  }
}
