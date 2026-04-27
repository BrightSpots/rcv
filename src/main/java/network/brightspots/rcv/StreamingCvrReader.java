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
import java.io.InputStream;
import java.security.InvalidParameterException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javafx.util.Pair;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.poi.openxml4j.opc.PackageRelationshipCollection;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

final class StreamingCvrReader extends BaseCvrReader {

  // this indicates a voter did not use this ranking
  private static final String SKIPPED_RANK_STRING = "undervote";
  // this indicates a missing precinct ID in output files
  private static final String MISSING_PRECINCT_ID = "missing_precinct_id";
  // this indicates a missing batch ID in output files
  private static final String MISSING_BATCH_ID = "missing_batch_id";
  // this indicates a write-in on files without exported images
  private static final String UNDECLARED_WRITE_IN = "Write-in";
  // name of the source file
  private final String excelFileName;
  // 0-based column index of first ranking
  private final int firstVoteColumnIndex;
  // 0-based row index of first CVR
  private final int firstVoteRowIndex;
  // 0-based column index of CVR ID (if present)
  private final Integer idColumnIndex;
  // 0-based column index of currentBatch name (if present)
  private final Integer batchColumnIndex;
  // 0-based column index of currentPrecinct name (if present)
  private final Integer precinctColumnIndex;
  // optional delimiter for cells that contain multiple candidates
  private final String overvoteDelimiter;
  private final String overvoteLabel;
  private final String undeclaredWriteInLabel;
  // used for generating CVR IDs
  private int cvrIndex = 0;
  // list of currentRankings for CVR in progress
  private LinkedList<Pair<Integer, String>> currentRankings;
  // list of raw strings for CVR in progress
  private LinkedList<String> currentCvrData;
  // supplied CVR ID for CVR in progress
  private String currentSuppliedCvrId;
  // batch ID for CVR in progress
  private String currentBatch;
  // precinct ID for CVR in progress
  private String currentPrecinct;
  // place to store input CVR list (new CVRs will be appended as we parse)
  private List<CastVoteRecord> cvrList;
  // last rankings cell observed for CVR in progress
  private int lastRankSeen;
  // has this CVR had any blank candidate cells?
  private boolean hasSeenAnyBlankCandidateCells;
  // has this CVR had any non-blank candidate cells?
  private boolean hasSeenAnyNonBlankCandidateCells;
  // total number of rows where there were only blank candidates
  private int numRowsIgnoredBecauseAllBlank;
  // flag indicating data issues during parsing
  private boolean encounteredDataErrors = false;
  // set of packed (col, row) cell addresses that contain images in the drawing layer
  private Set<Long> imageCells = new HashSet<>();
  // 0-based row index of the row currently being parsed
  private int currentRowIndex;

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
    this.batchColumnIndex =
            !isNullOrBlank(source.getBatchColumnIndex())
                    ? Integer.parseInt(source.getBatchColumnIndex()) - 1
                    : null;
    this.precinctColumnIndex =
        !isNullOrBlank(source.getPrecinctColumnIndex())
            ? Integer.parseInt(source.getPrecinctColumnIndex()) - 1
            : null;
    this.overvoteDelimiter = source.getOvervoteDelimiter();
    this.overvoteLabel = source.getOvervoteLabel();
    this.undeclaredWriteInLabel = UNDECLARED_WRITE_IN;
  }

  private static long hashForCell(int row, int col) {
    return ((long) col << 32) | (row & 0xFFFFFFFFL);
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
  // occur in a ranking's cell.
  // param: currentRank the rank at which we stop inferring empty cells for this invocation
  private void handleEmptyCells(int currentRank) {
    for (int rank = lastRankSeen + 1; rank < currentRank; rank++) {
      int col = firstVoteColumnIndex + rank - 1;
      if (imageCells.contains(hashForCell(currentRowIndex, col))) {
        currentCvrData.add(undeclaredWriteInLabel);
        currentRankings.add(new Pair<>(rank, Tabulator.UNDECLARED_WRITE_IN_OUTPUT_LABEL));
        hasSeenAnyNonBlankCandidateCells = true;
      } else {
        hasSeenAnyBlankCandidateCells = true;
        currentCvrData.add("empty cell");
      }
    }
  }

  // setup data structures for parsing a new CVR
  private void beginCvr() {
    cvrIndex++;
    currentRankings = new LinkedList<>();
    currentCvrData = new LinkedList<>();
    currentSuppliedCvrId = null;
    currentBatch = null;
    currentPrecinct = null;
    lastRankSeen = 0;
    hasSeenAnyNonBlankCandidateCells = false;
    hasSeenAnyBlankCandidateCells = false;
  }

  // complete construction of new CVR object
  private void endCvr() {
    // handle any empty cells which may appear at the end of this row
    if (!config.isMaxRankingsSetToMaximum()) {
      handleEmptyCells(config.getMaxRankingsAllowedWhenNotSetToMaximum() + 1);
    }
    String computedCastVoteRecordId =
        String.format("%s-%d", OutputWriter.sanitizeStringForOutput(excelFileName), cvrIndex);

    if (hasSeenAnyNonBlankCandidateCells && hasSeenAnyBlankCandidateCells) {
      Logger.severe("Blank cells are not allowed unless the entire row is blank (CVR %s)",
              computedCastVoteRecordId);
      encounteredDataErrors = true;
    } else if (!hasSeenAnyNonBlankCandidateCells) {
      Logger.auditable(
              "Skipping CVR for irrelevant contest: %s", computedCastVoteRecordId);
      numRowsIgnoredBecauseAllBlank++;
      return;
    }

    // add precinct ID if needed
    if (precinctColumnIndex != null) {
      if (currentPrecinct == null) {
        // group precincts with missing Ids here
        Logger.warning(
            "Precinct identifier not found for cast vote record: %s", computedCastVoteRecordId);
        currentPrecinct = MISSING_PRECINCT_ID;
      }
    }

    // add batch ID if needed
    if (batchColumnIndex != null) {
      if (currentBatch == null) {
        // group batch with missing Ids here
        Logger.warning(
                "Batch identifier not found for cast vote record: %s", computedCastVoteRecordId);
        currentBatch = MISSING_BATCH_ID;
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

    // create new cast vote record
    CastVoteRecord newRecord = new CastVoteRecord(
        computedCastVoteRecordId,
        currentSuppliedCvrId,
        currentPrecinct,
        currentBatch,
        usesLastAllowedRanking(currentRankings, null),
        currentRankings);
    cvrList.add(newRecord);

    // provide some user feedback on the CVR count
    this.logCvrRecordParsed();
  }

  // handle CVR cell data callback
  private void cvrCell(int col, String cellData) {
    currentCvrData.add(cellData);
    if (precinctColumnIndex != null && col == precinctColumnIndex) {
      currentPrecinct = cellData;
    } else if (batchColumnIndex != null && col == batchColumnIndex) {
      currentBatch = cellData;
    } else if (idColumnIndex != null && col == idColumnIndex) {
      currentSuppliedCvrId = cellData;
    } else if (col >= firstVoteColumnIndex
        && (config.isMaxRankingsSetToMaximum()
            || col < firstVoteColumnIndex + config.getMaxRankingsAllowedWhenNotSetToMaximum())) {
      // Unlike other CVRs, where having a ranking over the max number of rankings is an error,
      // in these files it simply defines the "last" column used for rankings.
      // If the max rankings is set to the maximum, we don't need to check the upper bound --
      // we read all columns.
      // Get the current ranking, and update the max ranking
      int currentRank = col - firstVoteColumnIndex + 1;

      // handle any empty cells which may exist between this cell and any previous one
      handleEmptyCells(currentRank);
      String cellString = cellData.trim();

      // There may be multiple candidates in this cell (i.e. an overvote).
      String[] candidates;
      if (!isNullOrBlank(overvoteDelimiter)) {
        candidates = cellString.split(Pattern.quote(overvoteDelimiter));
      } else {
        candidates = new String[]{cellString};
      }

      for (String candidate : candidates) {
        candidate = candidate.trim();
        hasSeenAnyNonBlankCandidateCells |= !candidate.isBlank();
        hasSeenAnyBlankCandidateCells |= candidate.isBlank();
        if (candidates.length > 1 && candidate.isBlank()) {
          Logger.severe(
              "If a cell contains multiple candidates split by the overvote delimiter, "
                  + "it's not valid for any of them to be blank or an explicit skipped ranking.");
          encounteredDataErrors = true;
        } else if (!candidate.isBlank()) {
          if (candidate.equals(SKIPPED_RANK_STRING)) {
            continue;
          } else if (candidate.equals(overvoteLabel)) {
            candidate = Tabulator.EXPLICIT_OVERVOTE_LABEL;
          } else if (isUndeclaredWriteIn(candidate)) {
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

  boolean isUndeclaredWriteIn(String candidateName) {
    return candidateName.equals(undeclaredWriteInLabel);
  }

  @Override
  void readCastVoteRecords(List<CastVoteRecord> castVoteRecords)
      throws CastVoteRecord.CvrParseException, IOException {
    try {
      parseCvrFileInternal(castVoteRecords);
      this.logCvrParsingComplete();
    } catch (OpenXML4JException | SAXException | ParserConfigurationException e) {
      Logger.severe("Error parsing source file %s", cvrPath);
      Logger.info(
          "ES&S cast vote record files must be Microsoft Excel Workbook "
              + "format.\nStrict Open XML and Open Office are not supported.");
      Logger.info("Actual error: " + e.getMessage());
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

    // handle at least 10 million write-in images
    ZipSecureFile.setMaxFileCount(10_000_000);
    // open the zip package
    OPCPackage pkg = OPCPackage.open(cvrPath);
    // pull out strings
    ReadOnlySharedStringsTable sharedStrings = new ReadOnlySharedStringsTable(pkg);
    // XSSF reader is used to extract styles data
    XSSFReader xssfReader = new XSSFReader(pkg);
    // styles data is used for creating ContentHandler
    StylesTable styles = xssfReader.getStylesTable();
    // pre-scan drawing XML to map image positions to cells before streaming
    imageCells = buildImageCellSet(xssfReader, pkg);
    // object for handling Excel parsing callbacks
    SheetContentsHandler sheetContentsHandler =
        new SheetContentsHandler() {
          @Override
          public void startRow(int i) {
            currentRowIndex = i;
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

    if (numRowsIgnoredBecauseAllBlank > 0) {
      Logger.warning("Ignored %d rows with no votes for any candidates.",
          numRowsIgnoredBecauseAllBlank);
    }

    if (encounteredDataErrors) {
      throw new CvrDataFormatException();
    }
  }

  // Pre-scan the drawing XML for the first sheet to find cells containing images.
  // Images in xlsx are floating objects in a separate drawing part, not embedded in cell data,
  // so they never appear in XSSFSheetXMLHandler callbacks. The anchor's "from" element gives the
  // 0-based (col, row) of the cell the image is anchored to.
  private Set<Long> buildImageCellSet(XSSFReader xssfReader, OPCPackage pkg)
      throws OpenXML4JException, IOException, SAXException, ParserConfigurationException {
    Set<Long> cells = new HashSet<>();
    XSSFReader.SheetIterator sheetIter = (XSSFReader.SheetIterator) xssfReader.getSheetsData();
    if (!sheetIter.hasNext()) {
      return cells;
    }
    // Advance to the first sheet so getSheetPart() is populated, then close the stream.
    try (InputStream ignored = sheetIter.next()) {
      // nothing to read; we only need the sheet's package relationships
    }
    PackagePart sheetPart = sheetIter.getSheetPart();
    PackageRelationshipCollection drawingRels = sheetPart.getRelationshipsByType(
        "http://schemas.openxmlformats.org/officeDocument/2006/relationships/drawing");

    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setNamespaceAware(true);
    for (PackageRelationship rel : drawingRels) {
      PackagePart drawingPart = sheetPart.getRelatedPart(rel);
      DrawingImageHandler handler = new DrawingImageHandler(cells);
      try (InputStream is = drawingPart.getInputStream()) {
        factory.newSAXParser().parse(is, handler);
      }
    }
    return cells;
  }

  // SAX handler that parses a spreadsheet drawing XML and records the anchor cell of every picture.
  private static class DrawingImageHandler extends DefaultHandler {
    // namespace for spreadsheet drawing elements (xdr:*)
    private static final String XDR_NS =
        "http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing";

    private final Set<Long> imageCells;
    private boolean inFrom = false;
    private boolean inCol = false;
    private boolean inRow = false;
    private boolean hasPic = false;
    private int fromCol = -1;
    private int fromRow = -1;
    private final StringBuilder text = new StringBuilder();

    DrawingImageHandler(Set<Long> imageCells) {
      this.imageCells = imageCells;
    }

    @Override
    public void startElement(String uri, String localName, String qualifiedName, Attributes attrs) {
      text.setLength(0);
      if (!XDR_NS.equals(uri)) {
        return;
      }
      switch (localName) {
        case "twoCellAnchor", "oneCellAnchor" -> {
          fromCol = -1;
          fromRow = -1;
          hasPic = false;
        }
        case "from" -> {
          inFrom = true;
        }
        case "col" -> {
          if (inFrom) {
            inCol = true;
          }
        }
        case "row" -> {
          if (inFrom) {
            inRow = true;
          }
        }
        case "pic" -> {
          hasPic = true;
        }
        default -> {}
      }
    }

    @Override
    public void endElement(String uri, String localName, String qualifiedName) {
      if (XDR_NS.equals(uri)) {
        switch (localName) {
          case "from" -> {
            inFrom = false;
          }
          case "col" -> {
            if (inCol) {
              fromCol = Integer.parseInt(text.toString().trim());
              inCol = false;
            }
          }
          case "row" -> {
            if (inRow) {
              fromRow = Integer.parseInt(text.toString().trim());
              inRow = false;
            }
          }
          case "twoCellAnchor", "oneCellAnchor" -> {
            if (hasPic && fromCol >= 0 && fromRow >= 0) {
              imageCells.add(hashForCell(fromRow, fromCol));
            }
          }
          default -> {}
        }
      }
      text.setLength(0);
    }

    @Override
    public void characters(char[] ch, int start, int length) {
      text.append(ch, start, length);
    }
  }

  static class CvrDataFormatException extends Exception {}
}
