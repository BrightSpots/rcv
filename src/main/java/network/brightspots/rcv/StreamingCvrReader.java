/*
 * RCTab
 * Copyright (c) 2017-2023 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: Read and parse ES&S election data into CastVoteRecord objects.
 * Design: ES&S uses an xlsx (Excel) file to store CVR data.  This class uses Apache POI to stream
 * the xlsx file, which triggers callbacks at the beginning and end of every row, and every cell.
 * Conditions: Used when reading ES&S election data.
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import static network.brightspots.rcv.Utils.isNullOrBlank;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import javafx.util.Pair;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
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

class StreamingCvrReader extends BaseCvrReader {

  // this indicates a missing precinct ID in output files
  private static final String MISSING_PRECINCT_ID = "missing_precinct_id";
  // name of the source file
  private final String excelFileName;
  // 0-based column index of first ranking
  private final int firstVoteColumnIndex;
  // 0-based row index of first CVR
  private final int firstVoteRowIndex;
  // 0-based column index of CVR ID (if present)
  private final Integer idColumnIndex;
  // 0-based column index of currentPrecinct name (if present)
  private final Integer precinctColumnIndex;
  // optional delimiter for cells that contain multiple candidates
  private final String overvoteDelimiter;
  private final String overvoteLabel;
  private final String skippedRankLabel;
  private final String undeclaredWriteInLabel;
  private final boolean treatBlankAsUndeclaredWriteIn;
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
  // last rankings cell observed for CVR in progress
  private int lastRankSeen;
  // flag indicating data issues during parsing
  private boolean encounteredDataErrors = false;

  StreamingCvrReader(ContestConfig config, RawContestConfig.CvrSource source) {
    super(config, source);
    this.excelFileName = new File(cvrPath).getName();

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
    this.overvoteLabel = source.getOvervoteLabel();
    this.skippedRankLabel = source.getSkippedRankLabel();
    this.undeclaredWriteInLabel = source.getUndeclaredWriteInLabel();
    this.treatBlankAsUndeclaredWriteIn = source.getTreatBlankAsUndeclaredWriteIn();
  }

  // given Excel-style address string return the cell address as a pair of Integers
  // representing zero-based column and row of the cell address
  private static Pair<Integer, Integer> getCellAddress(String address) {
    // this regex will parse a string into
    // a sequence of one or more non-digits followed by a sequence of one or more digits
    String[] addressParts = address.split("(?<=\\D)(?=\\d)");
    if (addressParts.length != 2) {
      Logger.severe("Invalid cell address: %s", address);
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
        Logger.severe("Invalid cell address: %s", columnAddress);
        throw new InvalidParameterException();
      }
      result += charValue;
    }
    return result - 1;
  }

  @Override
  public String readerName() {
    return "ES&S";
  }

  // purpose: Handle empty cells encountered while parsing a CVR. Unlike empty rows, empty cells
  // do not trigger parsing callbacks so their existence must be inferred and handled when they
  // occur in a rankings cell.
  // param: currentRank the rank at which we stop inferring empty cells for this invocation
  private void handleEmptyCells(int currentRank) {
    for (int rank = lastRankSeen + 1; rank < currentRank; rank++) {
      currentCvrData.add("empty cell");
      // add UWI ranking if required by settings
      if (treatBlankAsUndeclaredWriteIn) {
        currentRankings.add(new Pair<>(rank, Tabulator.UNDECLARED_WRITE_IN_OUTPUT_LABEL));
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
        Logger.warning(
            "Precinct identifier not found for cast vote record: %s", computedCastVoteRecordId);
        currentPrecinct = MISSING_PRECINCT_ID;
      }
    }

    if (idColumnIndex != null && currentSuppliedCvrId == null) {
      Logger.severe(
          "Cast vote record identifier missing on row %d in file %s. This may be due to an "
              + "incorrectly formatted xlsx file. Try copying your cvr data into a new xlsx file "
              + "to fix this.",
          cvrIndex + firstVoteRowIndex, excelFileName);
      encounteredDataErrors = true;
    }

    // Log the raw data for auditing
    Logger.fine("[Raw Data]: " + currentCvrData.toString());

    // create new cast vote record
    CastVoteRecord newRecord =
        new CastVoteRecord(
            computedCastVoteRecordId, currentSuppliedCvrId, currentPrecinct, currentRankings);
    cvrList.add(newRecord);

    // provide some user feedback on the CVR count
    if (cvrList.size() % 50000 == 0) {
      Logger.info("Parsed %d cast vote records.", cvrList.size());
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
        candidates = cellString.split(Pattern.quote(overvoteDelimiter));
      } else {
        candidates = new String[] {cellString};
      }

      for (String candidate : candidates) {
        candidate = candidate.trim();
        if (candidates.length > 1 && (candidate.equals("") || candidate.equals(skippedRankLabel))) {
          Logger.severe(
              "If a cell contains multiple candidates split by the overvote delimiter, it's not "
                  + "valid for any of them to be blank or an explicit skipped ranking.");
          encounteredDataErrors = true;
        } else if (!candidate.equals(skippedRankLabel)) {
          // map overvotes to our internal overvote string
          if (candidate.equals(overvoteLabel)) {
            candidate = Tabulator.EXPLICIT_OVERVOTE_LABEL;
          } else if (candidate.equals(undeclaredWriteInLabel)) {
            candidate = Tabulator.UNDECLARED_WRITE_IN_OUTPUT_LABEL;
          }
          Pair<Integer, String> ranking = new Pair<>(currentRank, candidate);
          currentRankings.add(ranking);
        }
      }
      // update lastRankSeen - used to handle empty ranking cells
      lastRankSeen = currentRank;
    }
  }

  @Override
  void readCastVoteRecords(List<CastVoteRecord> castVoteRecords)
      throws CastVoteRecord.CvrParseException, IOException {
    try {
      parseCvrFileInternal(castVoteRecords);
    } catch (OpenXML4JException | SAXException | ParserConfigurationException e) {
      Logger.severe("Error parsing source file %s", cvrPath);
      Logger.info(
          "ES&S cast vote record files must be Microsoft Excel Workbook "
              + "format.\nStrict Open XML and Open Office are not supported.");
      throw new CastVoteRecord.CvrParseException();
    } catch (CvrDataFormatException exception) {
      Logger.severe("Data format error while parsing source file: %s", cvrPath);
      Logger.info("See the log for details.");
      throw new CastVoteRecord.CvrParseException();
    }
  }

  // parse the given file into a List of CastVoteRecords for tabulation
  // param: castVoteRecords existing list to append new CastVoteRecords to
  // param: precinctIDs existing set of precinctIDs discovered during CVR parsing
  private void parseCvrFileInternal(List<CastVoteRecord> castVoteRecords)
      throws OpenXML4JException,
          SAXException,
          IOException,
          ParserConfigurationException,
          CvrDataFormatException {

    cvrList = castVoteRecords;

    // open the zip package
    OPCPackage pkg = OPCPackage.open(cvrPath);
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
            Logger.warning("Unexpected XML data: %s %b %s", s, b, s1);
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

    if (encounteredDataErrors) {
      throw new CvrDataFormatException();
    }
  }

  static class CvrDataFormatException extends Exception {}
}
