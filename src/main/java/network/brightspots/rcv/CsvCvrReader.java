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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.util.Pair;
import network.brightspots.rcv.RawContestConfig.CvrSource;
import network.brightspots.rcv.TabulatorSession.UnrecognizedCandidatesException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

class CsvCvrReader {
  private final String cvrPath;
  private final String undeclaredWriteInLabel;
  private final ContestConfig contestConfig;
  // 0-based column index of first ranking
  private final int firstVoteColumnIndex;
  // 0-based row index of first CVR
  private final int firstVoteRowIndex;
  // map for tracking unrecognized candidates during parsing
  private final Map<String, Integer> unrecognizedCandidateCounts = new HashMap<>();

  CsvCvrReader(
      String cvrPath,
      ContestConfig contestConfig,
      CvrSource source) {
    this.cvrPath = cvrPath;
    this.contestConfig = contestConfig;
    this.undeclaredWriteInLabel = source.getUndeclaredWriteInLabel();
    this.firstVoteColumnIndex = Integer.parseInt(source.getFirstVoteColumnIndex()) - 1;
    this.firstVoteRowIndex = Integer.parseInt(source.getFirstVoteRowIndex()) - 1;
  }

  // parse CVR CSV file into CastVoteRecord objects and add them to the input List<CastVoteRecord>
  void readCastVoteRecords(List<CastVoteRecord> castVoteRecords)
          throws CastVoteRecord.CvrParseException, IOException, UnrecognizedCandidatesException {
    Logger.info("Reading CSV cast vote record file: %s...", cvrPath);

    try (FileInputStream inputStream = new FileInputStream(Path.of(cvrPath).toFile())) {
      CSVParser parser = CSVParser.parse(inputStream,
              Charset.defaultCharset(), CSVFormat.newFormat(',').withHeader());
      List<String> candidateIds = parser.getHeaderNames();
      int undeclaredWriteInColumn = candidateIds.indexOf(undeclaredWriteInLabel);

      parser.stream().skip(firstVoteRowIndex);

      for (CSVRecord csvRecord : parser) {
        ArrayList<Pair<Integer, String>> rankings = new ArrayList<>();
        for (int col = firstVoteColumnIndex; col < csvRecord.size(); col++) {
          String rankAsString = csvRecord.get(col);
          if (rankAsString.isBlank()) {
            continue;
          }
          int rankAsInt;
          try {
            rankAsInt = Integer.parseInt(rankAsString);
          } catch (NumberFormatException e) {
            Logger.severe("Row %s expected number at column %d, but got \"%s\" instead.",
                    csvRecord.get(0), col, rankAsString);
            throw new CastVoteRecord.CvrParseException();
          }

          String candidateId = candidateIds.get(col);
          if (col == undeclaredWriteInColumn) {
            candidateId = Tabulator.UNDECLARED_WRITE_IN_OUTPUT_LABEL;
          } else if (contestConfig.getNameForCandidate(candidateId) == null) {
            unrecognizedCandidateCounts.merge(candidateId, 1, Integer::sum);
          }
          rankings.add(new Pair<>(rankAsInt, candidateId));
        }

        // create the new CastVoteRecord
        CastVoteRecord newCvr =
                new CastVoteRecord(
                        "no contest ID",
                        "no supplied ID",
                        "no precinct",
                        rankings);
        castVoteRecords.add(newCvr);
      }
    } catch (IOException exception) {
      Logger.severe("Error parsing cast vote record:\n%s", exception);
      throw exception;
    }

    if (unrecognizedCandidateCounts.size() > 0) {
      throw new UnrecognizedCandidatesException(unrecognizedCandidateCounts);
    }
  }
}
