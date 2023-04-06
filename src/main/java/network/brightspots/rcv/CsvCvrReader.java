/*
 * RCTab
 * Copyright (c) 2017-2022 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: Read and parse generic CSV files
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
import java.util.Set;
import javafx.util.Pair;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

class CsvCvrReader extends BaseCvrReader {
  // 0-based column index of first ranking
  private final int firstVoteColumnIndex;
  // 0-based row index of first CVR
  private final int firstVoteRowIndex;
  // map for tracking unrecognized candidates during parsing
  private final Map<String, Integer> unrecognizedCandidateCounts = new HashMap<>();

  CsvCvrReader(ContestConfig config, RawContestConfig.CvrSource source) {
    super(config, source);
    this.firstVoteColumnIndex = Integer.parseInt(source.getFirstVoteColumnIndex()) - 1;
    this.firstVoteRowIndex = Integer.parseInt(source.getFirstVoteRowIndex()) - 1;
  }

  @Override
  public String readerName() {
    return "Generic CSV";
  }

  // parse Cvr xml file into CastVoteRecord objects and add them to the input List<CastVoteRecord>
  @Override
  void readCastVoteRecords(List<CastVoteRecord> castVoteRecords, Set<String> precinctIds) throws
          CastVoteRecord.CvrParseException,
          IOException {
    Logger.info("Reading CSV cast vote record file: %s...", cvrPath);

    try (FileInputStream inputStream = new FileInputStream(Path.of(cvrPath).toFile())) {
      CSVParser parser = CSVParser.parse(inputStream,
              Charset.defaultCharset(), CSVFormat.newFormat(',').withHeader());
      List<String> candidateIds = parser.getHeaderNames();
      int undeclaredWriteInColumn = candidateIds.indexOf(source.getUndeclaredWriteInLabel());

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
          } else if (!config.getCandidateCodeList().contains(candidateId)) {
            unrecognizedCandidateCounts.merge(candidateId, 1, Integer::sum);
          }
          rankings.add(new Pair<>(rankAsInt, candidateId));
        }

        // create the new CastVoteRecord
        CastVoteRecord newCvr =
                new CastVoteRecord(
                        source.getContestId(),
                        "no supplied ID",
                        "no precinct",
                        null,
                        rankings);
        castVoteRecords.add(newCvr);
      }
    }
  }
}