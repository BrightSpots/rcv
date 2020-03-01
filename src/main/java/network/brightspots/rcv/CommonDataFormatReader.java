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

/*
 * Purpose:
 * Helper class to read and parse a Common Data Format file into cast vote record objects
 * and candidate names.
 */

package network.brightspots.rcv;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javafx.util.Pair;

class CommonDataFormatReader {

  private final String filePath;
  private final ContestConfig config;

  CommonDataFormatReader(String filePath, ContestConfig config) {
    this.filePath = filePath;
    this.config = config;
  }

  // returns map from candidate ID to name parsed from CDF election json
  Map<String, String> getCandidates() {
    Map<String, String> candidates = new HashMap<>();
    try {
      HashMap json = JsonParser.readFromFile(filePath, HashMap.class);
      // top-level election is a list of election objects:
      ArrayList electionArray = (ArrayList) json.get("Election");
      for (Object electionObject : electionArray) {
        HashMap election = (HashMap) electionObject;
        // each election contains one or more contests
        ArrayList contestArray = (ArrayList) election.get("Contest");
        // currently we only support a single contest per file
        assert contestArray.size() == 1;
        for (Object contestObject : contestArray) {
          HashMap contest = (HashMap) contestObject;
          // for each contest get the contest selections
          ArrayList contestSelectionArray = (ArrayList) contest.get("ContestSelection");
          for (Object contestSelectionObject : contestSelectionArray) {
            HashMap contestSelection = (HashMap) contestSelectionObject;
            // selectionId is the candidate ID
            String selectionId = (String) contestSelection.get("@id");
            String selectionName = null;
            ArrayList codeArray = (ArrayList) contestSelection.get("Code");
            if (codeArray != null) {
              for (Object codeObject : codeArray) {
                HashMap code = (HashMap) codeObject;
                String otherType = (String) code.get("OtherType");
                if (otherType != null && otherType.equals("vendor-label")) {
                  selectionName = (String) code.get("Value");
                }
              }
            }
            candidates.put(selectionId, selectionName != null ? selectionName : selectionId);
          }
        }
      }
    } catch (Exception e) {
      Logger.log(Level.SEVERE, "Error parsing candidate data: %s", e.toString());
    }
    return candidates;
  }

  // parse a list of contest selection rankings from a NIST "Snapshot" HashMap
  private List<Pair<Integer, String>> parseRankingsFromSnapshot(HashMap snapshot) {
    List<Pair<Integer, String>> rankings = new ArrayList<>();
    // at the top level is a list of contests each of which contains selections
    ArrayList cvrContests = (ArrayList) snapshot.get("CVRContest");
    for (Object contestObject : cvrContests) {
      HashMap cvrContest = (HashMap) contestObject;
      // each contest contains contestSelections
      ArrayList contestSelections = (ArrayList) cvrContest.get("CVRContestSelection");
      for (Object contestSelectionObject : contestSelections) {
        HashMap contestSelection = (HashMap) contestSelectionObject;
        // selectionId is the candidate/contest ID for this selection position
        String selectionId = (String) contestSelection.get("ContestSelectionId");
        if (selectionId.equals(config.getOvervoteLabel())) {
          selectionId = Tabulator.EXPLICIT_OVERVOTE_LABEL;
        }
        // extract all the positions (ranks) which this selection has been assigned
        ArrayList selectionPositions = (ArrayList) contestSelection.get("SelectionPosition");
        for (Object selectionPositionObject : selectionPositions) {
          // extract the position object
          HashMap selectionPosition = (HashMap) selectionPositionObject;
          // and finally the rank
          Integer rank = (Integer) selectionPosition.get("Rank");
          assert rank != null && rank >= 1;
          rankings.add(new Pair<>(rank, selectionId));
        }
      }
    }
    return rankings;
  }

  // parse the given file into a List of CastVoteRecords for tabulation
  void parseCvrFile(List<CastVoteRecord> castVoteRecords) {
    // cvrIndex and fileName are used to generate IDs for cvrs
    int cvrIndex = 0;
    String fileName = new File(filePath).getName();

    try {
      HashMap json = JsonParser.readFromFile(filePath, HashMap.class);
      // we expect a top-level "CVR" object containing a list of CVR objects
      ArrayList cvrs = (ArrayList) json.get("CVR");
      // for each CVR object extract the current snapshot
      // it will be used to build the ballot data
      for (Object cvr : cvrs) {
        HashMap cvrObject = (HashMap) cvr;
        String currentSnapshotId = (String) cvrObject.get("CurrentSnapshotId");
        String ballotId = (String) cvrObject.get("BallotPrePrintedId");
        String computedCastVoteRecordId = String.format("%s(%d)", fileName, ++cvrIndex);
        ArrayList cvrSnapshots = (ArrayList) cvrObject.get("CVRSnapshot");
        for (Object snapshotObject : cvrSnapshots) {
          HashMap snapshot = (HashMap) snapshotObject;
          if (!snapshot.get("@id").equals(currentSnapshotId)) {
            continue;
          }

          // we found the current CVR snapshot so get rankings and create a new cvr
          List<Pair<Integer, String>> rankings = parseRankingsFromSnapshot(snapshot);
          CastVoteRecord newRecord =
              new CastVoteRecord(computedCastVoteRecordId, ballotId, null, null, rankings);
          castVoteRecords.add(newRecord);

          // provide some user feedback on the CVR count
          if (castVoteRecords.size() % 50000 == 0) {
            Logger.log(Level.INFO, "Parsed %d cast vote records.", castVoteRecords.size());
          }
        }
      }
    } catch (Exception e) {
      Logger.log(Level.SEVERE, "Error parsing CDF data: %s", e.toString());
    }
  }
}
