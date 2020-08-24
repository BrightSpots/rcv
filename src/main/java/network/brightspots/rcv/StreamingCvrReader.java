/*
 * Universal RCV Tabulator
 * Copyright (c) 2017-2020 Bright Spots Developers.
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
import network.brightspots.rcv.RawContestConfig.CvrSource;
import network.brightspots.rcv.TabulatorSession.UnrecognizedCandidatesException;
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

class StreamingCvrReader {

  // this indicates a missing precinct ID in output files
  private static final String MISSING_PRECINCT_ID = "missing_precinct_id";
  // config for the contest
  private final ContestConfig config;
  // path of the source file
  private final String excelFilePath;
  // name of the source file
  private final String excelFileName;
  // 1-based column index of first ranking
  private final int firstVoteColumnIndex;
  // 1-based row index of first CVR
  private final int firstVoteRowIndex;
  // 1-based column index of CVR ID (if present)
  private final Integer idColumnIndex;
  // 1-based column index of currentPrecinct name (if present)
  private final Integer precinctColumnIndex;
  // optional delimiter for cells that contain multiple candidates
  private final String overvoteDelimiter;
  // map for tracking unrecognized candidates during parsing
  private final Map<String, Integer> unrecognizedCandidateCounts = new HashMap<>();
  // used for generating CVR IDs
  private int cvrIndex = 0;
  // list of currentRankings for CVR in progress
  private LinkedList<Pair<Integer, String>> currentRankings;
  // list of raw strings for CVR in progress
  private LinkedList<String> currentCvrData;
  // supplied CVR ID for CVR in progress
  private String currentSuppliedCvrId;
  // precinct ID for CVR in progress
  private String currentPrecinct;
  // place to store input CVR list (new CVRs will be appended as we parse)
  private List<CastVoteRecord> cvrList;
  // store precinct IDs (new IDs will be added as we parse)
  private Set<String> precinctIds;
  // last rankings cell observed for CVR in progress
  private int lastRankSeen;
  // flag indicating data issues during parsing
  private boolean encounteredDataErrors = false;

  StreamingCvrReader(ContestConfig config, CvrSource source) {
    this.config = config;
    this.excelFilePath = config.resolveConfigPath(source.getFilePath());
    this.excelFileName = new File(excelFilePath).getName();

    // to keep our code simple, we convert 1-indexed user-supplied values to 0-indexed here
    this.firstVoteColumnIndex = Integer.parseInt(source.getFirstVoteColumnIndex()) - 1;
    this.firstVoteRowIndex = Integer.parseInt(source.getFirstVoteRowIndex()) - 1;
    this.idColumnIndex =
        isNullOrBlank(source.getIdColumnIndex())
            ? null
            : Integer.parseInt(source.getIdColumnIndex()) - 1;
    this.precinctColumnIndex =
        !isNullOrBlank(source.getPrecinctColumnIndex())
            ? Integer.parseInt(source.getPrecinctColumnIndex()) - 1
            : null;
    this.overvoteDelimiter = source.getOvervoteDelimiter();
  }

  // given Excel-style address string return the cell address as a pair of Integers
  // representing zero-based column and row of the cell address
  private static Pair<Integer, Integer> getCellAddress(String address) {
    // this regex will parse a string into
    // a sequence of one or more non-digits followed by a sequence of one or more digits
    String[] addressParts = address.split("(?<=\\D)(?=\\d)");
    if (addressParts.length != 2) {
      Logger.log(Level.SEVERE, "Invalid cell address: %s", address);
      throw new InvalidParameterException();
    }
    // row is the 0-based row of the cell
    Integer row = Integer.parseInt(addressParts[1]) - 1;
    // col is the 0-based column of the cell
    Integer col = getColumnIndex(addressParts[0]);
    return new Pair<>(col, row);
  }

  // given an Excel columnAddress returns the zero-based
  // integer index of the column, e.g. "A" returns 0 and "AB" returns 27
  private static int getColumnIndex(String columnAddress) {
    int result = 0;
    for (int i = 0; i < columnAddress.length(); i++) {
      result *= 26;
      int charValue = columnAddress.charAt(i) - '@';
      if (charValue < 1 || charValue > 26) {
        Logger.log(Level.SEVERE, "Invalid cell address: %s", columnAddress);
        throw new InvalidParameterException();
      }
      result += charValue;
    }
    return result - 1;
  }

  // purpose: Handle empty cells encountered while parsing a CVR.  Unlike empty rows, empty cells
  // do not trigger parsing callbacks so their existence must be inferred and handled when they
  // occur in a rankings cell.
  // param: currentRank the rank at which we stop inferring empty cells for this invocation
  private void handleEmptyCells(int currentRank) {
    for (int rank = lastRankSeen + 1; rank < currentRank; rank++) {
      currentCvrData.add("empty cell");
      // add UWI ranking if required by settings
      if (config.isTreatBlankAsUndeclaredWriteInEnabled()) {
        currentRankings.add(new Pair<>(rank, config.getUndeclaredWriteInLabel()));
      }
    }
  }

  // setup data structures for parsing a new CVR
  private void beginCvr() {
    cvrIndex++;
    currentRankings = new LinkedList<>();
    currentCvrData = new LinkedList<>();
    currentSuppliedCvrId = null;
    currentPrecinct = null;
    lastRankSeen = 0;
  }

  // complete construction of new CVR object
  private void endCvr() {
    // handle any empty cells which may appear at the end of this row
    handleEmptyCells(config.getMaxRankingsAllowed() + 1);
    String computedCastVoteRecordId =
        String.format("%s-%d", ResultsWriter.sanitizeStringForOutput(excelFileName), cvrIndex);

    // add precinct ID if needed
    if (precinctColumnIndex != null) {
      if (currentPrecinct == null) {
        // group precincts with missing Ids here
        Logger.log(
            Level.WARNING,
            "Precinct identifier not found for cast vote record: %s",
            computedCastVoteRecordId);
        currentPrecinct = MISSING_PRECINCT_ID;
      }
      precinctIds.add(currentPrecinct);
    }

    if (idColumnIndex != null && currentSuppliedCvrId == null) {
      Logger.log(
          Level.SEVERE,
          "Cast vote record identifier missing on row %d in file %s.  This may be due to an "
              + "incorrectly formatted xlsx file.  Try copying your cvr data into a new xlsx file "
              + "to fix this.",
          cvrIndex + firstVoteRowIndex, excelFileName);
      encounteredDataErrors = true;
    }

    // create new cast vote record
    CastVoteRecord newRecord =
        new CastVoteRecord(
            computedCastVoteRecordId,
            currentSuppliedCvrId,
            currentPrecinct,
            currentCvrData,
            currentRankings);
    cvrList.add(newRecord);

    // provide some user feedback on the CVR count
    if (cvrList.size() % 50000 == 0) {
      Logger.log(Level.INFO, "Parsed %d cast vote records.", cvrList.size());
    }
  }

  // handle CVR cell data callback
  private void cvrCell(int col, String cellData) {
    currentCvrData.add(cellData);
    if (precinctColumnIndex != null && col == precinctColumnIndex) {
      currentPrecinct = cellData;
    } else if (idColumnIndex != null && col == idColumnIndex) {
      currentSuppliedCvrId = cellData;
    }

    // see if this column is in the ranking range
    if (col >= firstVoteColumnIndex
        && col < firstVoteColumnIndex + config.getMaxRankingsAllowed()) {
      int currentRank = col - firstVoteColumnIndex + 1;
      // handle any empty cells which may exist between this cell and any previous one
      handleEmptyCells(currentRank);
      String cellString = cellData.trim();

      // There may be multiple candidates in this cell (i.e. an overvote).
      String[] candidates;
      if (!isNullOrBlank(overvoteDelimiter)) {
        candidates = cellString.split(overvoteDelimiter);
      } else {
        candidates = new String[]{cellString};
      }

      for (String candidate : candidates) {
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
          Pair<Integer, String> ranking = new Pair<>(currentRank, candidate);
          currentRankings.add(ranking);
        }
      }
      // update lastRankSeen - used to handle empty ranking cells
      lastRankSeen = currentRank;
    }
  }

  // parse the given file into a List of CastVoteRecords for tabulation
  // param: castVoteRecords existing list to append new CastVoteRecords to
  // param: precinctIDs existing set of precinctIDs discovered during CVR parsing
  void parseCvrFile(List<CastVoteRecord> castVoteRecords, Set<String> precinctIds)
      throws UnrecognizedCandidatesException, OpenXML4JException, SAXException, IOException,
      ParserConfigurationException, CvrDataFormatException {

    cvrList = castVoteRecords;
    this.precinctIds = precinctIds;

    // open the zip package
    OPCPackage pkg = OPCPackage.open(excelFilePath);
    // pull out strings
    ReadOnlySharedStringsTable sharedStrings = new ReadOnlySharedStringsTable(pkg);
    // XSSF reader is used to extract styles data
    XSSFReader xssfReader = new XSSFReader(pkg);
    // styles data is used for creating ContentHandler
    StylesTable styles = xssfReader.getStylesTable();
    // object for handling Excel parsing callbacks
    SheetContentsHandler sheetContentsHandler =
        new SheetContentsHandler() {
          @Override
          public void startRow(int i) {
            if (i >= firstVoteRowIndex) {
              beginCvr();
            }
          }

          @Override
          public void endRow(int i) {
            if (i >= firstVoteRowIndex) {
              endCvr();
            }
          }

          // param: s cell address encoded as col,row
          // param: s1 cell data
          // param: xssfComment additional cell data (apparently unused in ES&S files)
          @Override
          public void cell(String s, String s1, XSSFComment xssfComment) {
            Pair<Integer, Integer> address = getCellAddress(s);
            int col = address.getKey();
            int row = address.getValue();
            if (row >= firstVoteRowIndex) {
              cvrCell(col, s1);
            }
          }

          @Override
          public void headerFooter(String s, boolean b, String s1) {
            Logger.log(Level.WARNING, "Unexpected XML data: %s %b %s", s, b, s1);
          }
        };

    // create the ContentHandler to handle parsing callbacks
    ContentHandler handler =
        new XSSFSheetXMLHandler(styles, sharedStrings, sheetContentsHandler, true);

    SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
    saxParserFactory.setNamespaceAware(true);
    SAXParser saxParser = saxParserFactory.newSAXParser();
    XMLReader xmlReader = saxParser.getXMLReader();
    xmlReader.setContentHandler(handler);
    // parse
    xmlReader.parse(new InputSource(xssfReader.getSheetsData().next()));
    // close zip file without saving
    pkg.revert();

    if (unrecognizedCandidateCounts.size() > 0) {
      throw new UnrecognizedCandidatesException(unrecognizedCandidateCounts);
    }

    if (encounteredDataErrors) {
      throw new CvrDataFormatException();
    }
  }

  static class CvrDataFormatException extends Exception {

  }
}
