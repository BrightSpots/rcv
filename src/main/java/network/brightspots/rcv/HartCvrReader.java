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

package network.brightspots.rcv;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.util.Pair;
import network.brightspots.rcv.TabulatorSession.UnrecognizedCandidatesException;

class HartCvrReader {

  private final String cvrPath;
  private final String contestId;
  private final String undeclaredWriteInLabel;
  private final ContestConfig contestConfig;
  // map for tracking unrecognized candidates during parsing
  private final Map<String, Integer> unrecognizedCandidateCounts = new HashMap<>();

  HartCvrReader(
      String cvrPath,
      String contestId,
      ContestConfig contestConfig,
      String undeclaredWriteInLabel) {
    this.cvrPath = cvrPath;
    this.contestId = contestId;
    this.contestConfig = contestConfig;
    this.undeclaredWriteInLabel = undeclaredWriteInLabel;
  }

  // iterate all xml files in the source input folder
  void readCastVoteRecordsFromFolder(List<CastVoteRecord> castVoteRecords)
      throws IOException, UnrecognizedCandidatesException {
    File cvrRoot = new File(this.cvrPath);
    File[] children = cvrRoot.listFiles();
    if (children != null) {
      for (File child : children) {
        if (child.getName().toLowerCase().endsWith("xml")) {
          readCastVoteRecord(castVoteRecords, child.toPath());
        }
      }
    } else {
      Logger.severe("Unable to find any files in directory: %s", cvrRoot.getAbsolutePath());
      throw new IOException();
    }

    if (unrecognizedCandidateCounts.size() > 0) {
      throw new UnrecognizedCandidatesException(unrecognizedCandidateCounts);
    }
  }

  // parse Cvr xml file into CastVoteRecord objects and add them to the input List<CastVoteRecord>
  void readCastVoteRecord(List<CastVoteRecord> castVoteRecords, Path path) throws IOException {
    try {
      Logger.info("Reading Hart cast vote record file: %s...", path.getFileName());

      XmlMapper xmlMapper = new XmlMapper();
      xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      FileInputStream inputStream = new FileInputStream(path.toFile());
      HartCvrXml xmlCvr = xmlMapper.readValue(inputStream, HartCvrXml.class);

      for (Contest contest : xmlCvr.Contests) {
        if (!contest.Id.equals(this.contestId)) {
          continue;
        }

        ArrayList<Pair<Integer, String>> rankings = new ArrayList<>();
        if (contest.Options != null) {
          for (Option option : contest.Options) {
            String candidateId = option.Id;
            if (candidateId.equals(undeclaredWriteInLabel)) {
              candidateId = Tabulator.UNDECLARED_WRITE_IN_OUTPUT_LABEL;
            } else if (!contestConfig.getCandidateCodeList().contains(candidateId)) {
              unrecognizedCandidateCounts.merge(candidateId, 1, Integer::sum);
            }
            // Hart RCV election ranks are indicated by a string read left to right:
            // each digit corresponds to a rank and is set to 1 if that rank was voted:
            // 0100 indicates rank 2 was voted
            // 0000 indicates no rank was voted (undervote)
            // 0101 indicates ranks 2 and 4 are voted (overvote)
            for (int rank = 1; rank < option.Value.length() + 1; rank++) {
              String rankValue = option.Value.substring(rank - 1, rank);
              if (rankValue.equals("1")) {
                rankings.add(new Pair<>(rank, candidateId));
              }
            }
          }
        }

        CastVoteRecord cvr =
            new CastVoteRecord(
                contest.Id,
                null,
                xmlCvr.BatchNumber,
                xmlCvr.CvrGuid,
                xmlCvr.PrecinctSplit.Name,
                xmlCvr.PrecinctSplit.Id,
                null,
                rankings);
        castVoteRecords.add(cvr);

        // provide some user feedback on the Cvr count
        if (castVoteRecords.size() % 50000 == 0) {
          Logger.info("Parsed %d cast vote records.", castVoteRecords.size());
        }
      }
    } catch (Exception e) {
      Logger.severe("Error parsing cast vote record:\n%s", e);
      throw e;
    }
  }

  @SuppressWarnings({"unused", "RedundantSuppression"})
  static class WriteInData {

    public String ImageId;
    public String WriteInDataStatus;
  }

  // a voter selection
  @SuppressWarnings({"unused", "RedundantSuppression"})
  static class Option {

    public String Name;
    public String Id;
    public String Value;
    public WriteInData WriteInData;
  }

  // voter selections for a contest
  @SuppressWarnings({"unused", "RedundantSuppression"})
  static class Contest {

    public String Name;
    public String Id;
    public ArrayList<Option> Options;
  }

  @SuppressWarnings({"unused", "RedundantSuppression"})
  static class PrecinctSplit {

    public String Name;
    public String Id;
  }

  @SuppressWarnings({"unused", "RedundantSuppression"})
  static class Party {

    public String Name;
    public String ID;
  }

  @SuppressWarnings({"unused", "RedundantSuppression"})
  static class HartCvrXml {

    public String BatchSequence;
    public String SheetNumber;
    public String BatchNumber;
    public String CvrGuid;
    public PrecinctSplit PrecinctSplit;
    public Party Party;
    public ArrayList<Contest> Contests;
  }
}
