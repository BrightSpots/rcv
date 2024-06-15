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

final class CsvCvrReader extends BaseCvrReader {
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
      CSVParser parser =
              CSVParser.parse(
                      inputStream,
                      Charset.defaultCharset(),
                      CSVFormat.Builder.create().setHeader().build());
      List<String> rawCandidateNames = parser.getHeaderNames();
      // Split rawCandidateNames from firstVoteColumnIndex to the end
      return new ArrayList<>(rawCandidateNames.subList(
            firstVoteColumnIndex, rawCandidateNames.size()));
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
      CSVParser parser =
          CSVParser.parse(
              inputStream,
              Charset.defaultCharset(),
              CSVFormat.Builder.create().setHeader().build());
      List<String> candidateIds = parser.getHeaderNames();
      int undeclaredWriteInColumn = candidateIds.indexOf(source.getUndeclaredWriteInLabel());

      parser.stream().skip(firstVoteRowIndex);

      int index = 0;
      for (CSVRecord csvRecord : parser) {
        index++;
        ArrayList<Pair<Integer, String>> rankings = new ArrayList<>();
        for (int col = firstVoteColumnIndex; col < csvRecord.size(); col++) {
          String rankAsString = csvRecord.get(col);
          if (rankAsString.isBlank()) {
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

          String candidateId = candidateIds.get(col);
          if (col == undeclaredWriteInColumn) {
            candidateId = Tabulator.UNDECLARED_WRITE_IN_OUTPUT_LABEL;
          }
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
}
