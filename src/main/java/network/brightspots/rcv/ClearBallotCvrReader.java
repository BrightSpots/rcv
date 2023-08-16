/*
 * RCTab
 * Copyright (c) 2017-2023 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: Parses Clear Ballot CVR files into CastVoteRecords.
 * Design: Clear Ballot data is stored in .csv files one row per csv.  This class uses a buffered
 * (streaming) file reader which should be able to parse files of any size.
 * Conditions: When reading Clear Ballot CVR data.
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.util.Pair;
import network.brightspots.rcv.CastVoteRecord.CvrParseException;

class ClearBallotCvrReader extends BaseCvrReader {
  ClearBallotCvrReader(ContestConfig config, RawContestConfig.CvrSource source) {
    super(config, source);
  }

  @Override
  public String readerName() {
    return "Clear Ballot";
  }

  // parse Cvr json into CastVoteRecord objects and append them to the input castVoteRecords list
  // see Clear Ballot 2.1 RCV Format Specification for details
  @Override
  void readCastVoteRecords(List<CastVoteRecord> castVoteRecords)
      throws CvrParseException, IOException {
    BufferedReader csvReader;
    try {
      csvReader = new BufferedReader(new FileReader(this.cvrPath, StandardCharsets.UTF_8));
      // each "choice column" in the input Csv corresponds to a unique ranking: candidate+rank pair
      // we parse these rankings from the header row into a map for lookup during CVR parsing
      String firstRow = csvReader.readLine();
      if (firstRow == null) {
        Logger.severe("No header row found in cast vote record file: %s", this.cvrPath);
        throw new CvrParseException();
      }
      String[] headerData = firstRow.split(",");
      if (headerData.length < CvrColumnField.ChoicesBegin.ordinal()) {
        Logger.severe("No choice columns found in cast vote record file: %s", this.cvrPath);
        throw new CvrParseException();
      }
      Map<Integer, Pair<Integer, String>> columnIndexToRanking = new HashMap<>();
      for (int columnIndex = CvrColumnField.ChoicesBegin.ordinal();
          columnIndex < headerData.length;
          columnIndex++) {
        String choiceColumnHeader = headerData[columnIndex];
        String[] choiceFields = choiceColumnHeader.split(":");
        // validate field count
        if (choiceFields.length != RcvChoiceHeaderField.FIELD_COUNT.ordinal()) {
          Logger.severe(
              "Wrong number of choice header fields in cast vote record file: %s", this.cvrPath);
          throw new CvrParseException();
        }
        // filter by contest
        String contestName = choiceFields[RcvChoiceHeaderField.CONTEST_NAME.ordinal()];
        if (!contestName.equals(source.getContestId())) {
          continue;
        }
        // validate and store the ranking associated with this choice column
        String choiceName = choiceFields[RcvChoiceHeaderField.CHOICE_NAME.ordinal()];
        if (choiceName.equals(source.getUndeclaredWriteInLabel())) {
          choiceName = Tabulator.UNDECLARED_WRITE_IN_OUTPUT_LABEL;
        }
        Integer rank = Integer.parseInt(choiceFields[RcvChoiceHeaderField.RANK.ordinal()]);
        if (rank > this.config.getMaxRankingsAllowed()) {
          Logger.severe(
              "Rank: %d exceeds max rankings allowed in config: %d",
              rank, this.config.getMaxRankingsAllowed());
          throw new CvrParseException();
        }
        columnIndexToRanking.put(columnIndex, new Pair<>(rank, choiceName));
      }
      // read all remaining rows and create CastVoteRecords for each one
      while (true) {
        String row = csvReader.readLine();
        if (row == null) {
          break;
        }
        // parse rankings
        String[] cvrData = row.split(",");
        ArrayList<Pair<Integer, String>> rankings = new ArrayList<>();
        for (var entry : columnIndexToRanking.entrySet()) {
          if (Integer.parseInt(cvrData[entry.getKey()]) == 1) {
            // user marked this column
            rankings.add(entry.getValue());
          }
        }
        // create the cast vote record
        CastVoteRecord castVoteRecord =
            new CastVoteRecord(
                source.getContestId(),
                cvrData[CvrColumnField.ScanComputerName.ordinal()],
                null,
                cvrData[CvrColumnField.BallotID.ordinal()],
                cvrData[CvrColumnField.PrecinctID.ordinal()],
                null,
                rankings);

        castVoteRecords.add(castVoteRecord);
        // provide some user feedback on the Cvr count
        if (castVoteRecords.size() % 50000 == 0) {
          Logger.info("Parsed %d cast vote records.", castVoteRecords.size());
        }
      }
      csvReader.close();
    } catch (FileNotFoundException exception) {
      Logger.severe("Cast vote record file not found!\n%s", exception);
    }
  }

  // These values correspond to the data in Clear Vote Cvr Csv columns
  @SuppressWarnings({"unused", "RedundantSuppression"})
  public enum CvrColumnField {
    RowNumber,
    BoxID,
    BoxPosition,
    BallotID,
    PrecinctID,
    BallotStyleID,
    PrecinctStyleName,
    ScanComputerName,
    Status,
    Remade,
    ChoicesBegin
  }

  // These values correspond to the data in Clear Vote Rcv choice column header fields
  @SuppressWarnings({"unused", "RedundantSuppression"})
  public enum RcvChoiceHeaderField {
    HEADER,
    CONTEST_NAME,
    RANK,
    VOTE_RULE,
    CHOICE_NAME,
    PARTY_NAME,
    FIELD_COUNT
  }
}
