/*
 * RCTab
 * Copyright (c) 2017-2023 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: Read and parse generic CSV files.
 * Design: Parses a CSV with candidates in columns, cast vote records in rows,
 * and vote rankings in cells.
 * Conditions: The CSV must contain only one contest.
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import static network.brightspots.rcv.Utils.isNullOrBlank;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javafx.util.Pair;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

class CsvCvrReader extends BaseCvrReader {
  // 0-based column index of first ranking
  private final int firstVoteColumnIndex;
  // 0-based row index of first CVR
  private final int firstVoteRowIndex;

  CsvCvrReader(ContestConfig config, RawContestConfig.CvrSource source) {
    super(config, source);
    this.firstVoteColumnIndex = Integer.parseInt(source.getFirstVoteColumnIndex()) - 1;
    this.firstVoteRowIndex = Integer.parseInt(source.getFirstVoteRowIndex()) - 1;
  }

  @Override
  public String readerName() {
    return "generic CSV";
  }

  @Override
  public List<String> readCandidateListFromCvr(List<CastVoteRecord> castVoteRecords)
      throws IOException {
    try (FileInputStream inputStream = new FileInputStream(Path.of(cvrPath).toFile())) {
      return getCandidateNamesAndInitializeParser(getCsvParser(inputStream));
    } catch (CastVoteRecord.CvrParseException exception) {
      Logger.severe("Error reading candidate names:\n%s", exception);
      throw new IOException(exception);
    } catch (IOException exception) {
      Logger.severe("Error parsing cast vote record:\n%s", exception);
      throw exception;
    }
  }

  // parse CVR CSV file into CastVoteRecord objects and add them to the input List<CastVoteRecord>
  @Override
  void readCastVoteRecords(List<CastVoteRecord> castVoteRecords)
      throws CastVoteRecord.CvrParseException, IOException {
    try (FileInputStream inputStream = new FileInputStream(Path.of(cvrPath).toFile())) {
      CSVParser parser = getCsvParser(inputStream);
      List<String> candidateNames = getCandidateNamesAndInitializeParser(parser);
      int undeclaredWriteInColumn = candidateNames.indexOf(source.getUndeclaredWriteInLabel());

      int index = 0;
      for (CSVRecord csvRecord : parser) {
        index++;
        ArrayList<Pair<Integer, String>> rankings = new ArrayList<>();
        for (int col = firstVoteColumnIndex; col < csvRecord.size(); col++) {
          String rankAsString = csvRecord.get(col);
          if (isNullOrBlank(rankAsString)) {
            continue;
          }
          int rankAsInt;
          try {
            rankAsString = rankAsString.trim();
            rankAsInt = Integer.parseInt(rankAsString);
          } catch (NumberFormatException e) {
            Logger.severe(
                "Row %s expected number at column %d, but got \"%s\" instead.",
                csvRecord.get(0), col, rankAsString);
            throw new CastVoteRecord.CvrParseException();
          }

          int candidateIndex = col - firstVoteColumnIndex;
          String candidateId = candidateIndex == undeclaredWriteInColumn
              ? Tabulator.UNDECLARED_WRITE_IN_OUTPUT_LABEL
              : candidateNames.get(candidateIndex);
          rankings.add(new Pair<>(rankAsInt, candidateId));
        }

        // create the new CastVoteRecord
        CastVoteRecord newCvr = new CastVoteRecord(
            Integer.toString(index),
            "no supplied ID",
            "no precinct",
            "no batch ID",
            rankings);
        castVoteRecords.add(newCvr);
      }
    } catch (IOException exception) {
      Logger.severe("Error parsing cast vote record:\n%s", exception);
      throw exception;
    }
  }

  private CSVParser getCsvParser(FileInputStream inputStream) throws IOException {
    CSVFormat format = CSVFormat.Builder.create()
        .setAllowMissingColumnNames(true)
        .setNullString("")
        .build();
    return CSVParser.parse(
      inputStream,
      Charset.defaultCharset(),
      format);
  }

  /**
   * This must be called before CVRs are read. It will return candidate names and skip
   * the appropriate number of rows.
   */
  private List<String> getCandidateNamesAndInitializeParser(CSVParser parser)
          throws CastVoteRecord.CvrParseException {
    List<String> candidateNames = new ArrayList<>();
    CSVRecord csvRecord = parser.iterator().next();
    for (int col = firstVoteColumnIndex; col < csvRecord.size(); col++) {
      String candidateName = csvRecord.get(col);
      if (isNullOrBlank(candidateName)) {
        Logger.severe("Candidate name on column %d cannot be empty", col);
        throw new CastVoteRecord.CvrParseException();
      }
      candidateNames.add(candidateName);
    }

    parser.stream().skip(firstVoteRowIndex - 1);

    return candidateNames;
  }
}
