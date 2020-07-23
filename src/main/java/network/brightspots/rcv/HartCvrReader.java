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
import java.util.List;
import java.util.logging.Level;
import javafx.util.Pair;
import network.brightspots.rcv.CastVoteRecord.CvrParseException;

class HartCvrReader {

  private final String cvrPath;
  private final String contestId;
  private final ContestConfig contestConfig;

  HartCvrReader(String cvrPath, String contestId, ContestConfig contestConfig) {
    this.cvrPath = cvrPath;
    this.contestId = contestId;
    this.contestConfig = contestConfig;
  }

  // iterate all xml files in the source input folder
  void readCastVoteRecordsFromFolder(List<CastVoteRecord> castVoteRecords)
      throws IOException, CvrParseException {
    File cvrRoot = new File(this.cvrPath);
    File[] children = cvrRoot.listFiles();
    if (children != null) {
      for (File child : children) {
        if (child.getName().toLowerCase().endsWith("xml")) {
          readCastVoteRecord(castVoteRecords, child.toPath());
        }
      }
    } else {
      Logger.log(Level.SEVERE, "Unable to find any files in directory: %s",
          cvrRoot.getAbsolutePath());
      throw new IOException();
    }
  }

  // parse Cvr xml file into CastVoteRecord objects and add them to the input List<CastVoteRecord>
  void readCastVoteRecord(List<CastVoteRecord> castVoteRecords, Path path)
      throws IOException, CvrParseException {
    try {
      Logger.log(Level.INFO, "Reading Hart cast vote record file: %s...", path.getFileName());

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
            if (!this.contestConfig.getCandidateCodeList().contains(option.Id)) {
              Logger.log(
                  Level.SEVERE,
                  "Candidate ID: \"%s\" name: \"%s\" from CVR is not in the config file!",
                  option.Id, option.Name);
              throw new CvrParseException();
            }

            // Hart RCV election ranks are indicated by a string read left to right:
            // each digit corresponds to a rank and is set to 1 if that rank was voted:
            // 0100 indicates rank 2 was voted
            // 0000 indicates no rank was voted (undervote)
            // 0101 indicates ranks 2 and 4 are voted (overvote)
            for (int rank = 1; rank < option.Value.length() + 1; rank++) {
              String rankValue = option.Value.substring(rank - 1, rank);
              if (rankValue.equals("1")) {
                rankings.add(new Pair<>(rank, option.Id));
              }
            }
          }
        }

        CastVoteRecord cvr = new CastVoteRecord(
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
          Logger.log(Level.INFO, "Parsed %d cast vote records.", castVoteRecords.size());
        }
      }
    } catch (Exception e) {
      Logger.log(Level.SEVERE, "Error parsing cast vote record:\n%s", e.toString());
      throw e;
    }
  }

  static class WriteInData {

    public String ImageId;
    public String WriteInDataStatus;
  }

  // a voter selection
  static class Option {

    public String Name;
    public String Id;
    public String Value;
    public WriteInData WriteInData;
  }

  // voter selections for a contest
  static class Contest {

    public String Name;
    public String Id;
    public ArrayList<Option> Options;
  }

  static class PrecinctSplit {

    public String Name;
    public String Id;
  }

  static class Party {

    public String Name;
    public String ID;
  }

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
