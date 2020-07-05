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

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import javafx.util.Pair;
import network.brightspots.rcv.CastVoteRecord.CvrParseException;

class HartCvrReader {

  private final String cvrPath;
  private final ContestConfig contestConfig;

  HartCvrReader(String cvrPath, ContestConfig contestConfig) {
    this.cvrPath = cvrPath;
    this.contestConfig = contestConfig;
  }


  // iterate all xml files in the source input folder
  void readCastVoteRecordsFromFolder(List<CastVoteRecord> castVoteRecords)
      throws IOException, CvrParseException {
    File cvrRoot = new File(this.cvrPath);
    File[] children = cvrRoot.listFiles();
    for (File child : children) {
      if (child.getName().endsWith("xml")) {
        readCastVoteRecord(castVoteRecords, child.toPath());
      }
    }
  }

  // parse Cvr xml file into CastVoteRecord objects and add them to the input List<CastVoteRecord>
  void readCastVoteRecord(List<CastVoteRecord> castVoteRecords, Path path)
      throws IOException, CvrParseException {
    try {
      XmlMapper xmlMapper = new XmlMapper();
      String xmlString = new String(Files.readAllBytes(path));
      HashMap cvrData = xmlMapper.readValue(xmlString, HashMap.class);
      ArrayList<String> extraCvrData = new ArrayList<>();

      extraCvrData.add((String) cvrData.get("BatchSequence"));
      extraCvrData.add((String) cvrData.get("IsElectronic"));
      extraCvrData.add((String) cvrData.get("SheetNumber"));

      String precinctName = null;
      String precinctId = null;
      HashMap precinctSplit = (HashMap) cvrData.get("PrecinctSplit");
      if (precinctSplit != null) {
        precinctName = (String) precinctSplit.get("Name");
        precinctId = (String) precinctSplit.get("ID");
      }
      extraCvrData.add(precinctName);
      extraCvrData.add(precinctId);

      String partyName = null;
      String partyId = null;
      HashMap party = (HashMap) cvrData.get("Party");
      if (party != null) {
        partyName = (String) party.get("Name");
        partyId = (String) party.get("ID");
      }
      extraCvrData.add(partyName);
      extraCvrData.add(partyId);

      String batchNumber = (String) cvrData.get("BatchNumber");
      String cvrId = (String) cvrData.get("CvrGuid");
      HashMap contests = (HashMap) cvrData.get("Contests");
      for (Object contestObject : contests.values()) {
        HashMap contest = (HashMap) contestObject;
        String contestName = (String) contest.get("Name");
        // TODO: once implemented change to config.contestID
        if (!contestName.equals(contestConfig.getContestName())) {
          continue;
        }
        String contestId = (String) contest.get("Id");
        Boolean invalidVote = (Boolean) contest.get("InvalidVote");
        if (invalidVote != null && invalidVote == false) {
          continue;
        }

        ArrayList<Pair<Integer, String>> rankings = new ArrayList<>();
        HashMap contestOptions = (HashMap) contest.get("Options");
        if (contestOptions != null) {
          for (Object optionObject : contestOptions.values()) {
            HashMap option = (HashMap) optionObject;
            String optionName = (String) option.get("Name");
            String optionId = (String) option.get("Id");
            if (!this.contestConfig.getCandidateCodeList().contains(optionId)) {
              Logger.log(
                  Level.SEVERE,
                  "Candidate ID: \"%s\" name: \"%s\" from CVR is not in the config file!",
                  optionId, optionName);
              throw new CvrParseException();
            }

            String optionValue = (String) option.get("Value");
            // for RCV elections ranks are indicated by a '1' digit in each place where an option
            // was selected.  so the value 0101 indicates ranks 2 and 3 are selected (an overvote)
            for (int i = 0; i < optionValue.length(); i++) {
              String rankValue = optionValue.substring(i, i + 1);
              if (rankValue.equals("1")) {
                rankings.add(new Pair<>(i + 1, optionId));
              }
            }
          }
        }

        CastVoteRecord cvr = new CastVoteRecord(
            contestId,
            null,
            batchNumber,
            cvrId,
            precinctName,
            precinctId,
            null,
            rankings,
            extraCvrData);
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

}
