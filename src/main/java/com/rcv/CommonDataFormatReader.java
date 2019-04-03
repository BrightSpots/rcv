/*
 * Ranked Choice Voting Universal Tabulator
 * Copyright (c) 2019 Jonathan Moldover, Louis Eisenberg, and Hylton Edingfield
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
 *
 * Purpose:
 * Helper class to read and parse a Common Data Format file into cast vote record objects.
 */

package com.rcv;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javafx.util.Pair;

class CommonDataFormatReader {

  // path of the source file
  private final String filePath;
  // name of the source file
  private final String fileName;
  // map for tracking unrecognized candidates during parsing
  private final Map<String, Integer> unrecognizedCandidateCounts = new HashMap<>();

  // last rankings cell observed for CVR in progress
  private int lastRankSeen;

  // function: StreamingCVRReader
  // purpose: class constructor
  // param: config an ContestConfig object specifying rules for interpreting CVR file data
  // param: source file to read
  CommonDataFormatReader(String filePath) {
    this.filePath = filePath;
    // cvrFileName for generating cvrIDs
    fileName = new File(filePath).getName();
  }

  // returns list of candidateIDs for this election
  Set<String> parseCandidates()
  {
    HashMap<Object, Object> json = JsonParser.readFromFile(this.filePath, HashMap.class);

    Set<String> candidates = new HashSet<String>();

    // election is a list of contests
    ArrayList<Object> electionArray = (ArrayList<Object>) json.get("Election");
    // for each contest get the contest selections
    for(Object electionObject : electionArray) {
      Map<String, Object> election = (Map<String, Object>) electionObject;
      ArrayList<Object> contestArray = (ArrayList<Object>) election.get("Contest");
      for(Object contestObject : contestArray) {
        Map<String, Object> contest = (Map<String, Object>) contestObject;
        ArrayList<Object> contestSelectionArray = (ArrayList<Object>) contest.get("ContestSelection");
        for(Object contestSelectionObject : contestSelectionArray) {
          Map<String, Object> contestSelection = (Map<String, Object>) contestSelectionObject;
          String selectionID = (String) contestSelection.get("@id");
          candidates.add(selectionID);
        }
      }
    }
    return candidates;
  }



  // function: parseCVRFile
  // purpose: parse the given file into a List of CastVoteRecords for tabulation
  // param: castVoteRecords existing list to append new CastVoteRecords to
  // returns: list of parsed CVRs
  List<CastVoteRecord> parseCVRFile(List<CastVoteRecord> castVoteRecords) {

    List<CastVoteRecord> cvrs = new ArrayList<>();

    // use CDF reader for json
    Integer cvrIndex = 0;

    HashMap<Object, Object> json = JsonParser.readFromFile(this.filePath, HashMap.class);
    // we expect a top-level "CVR" object containing a list of CVR objects
    ArrayList<Object> CVRs = (ArrayList<Object>) json.get("CVR");
    // for each CVR object extract the current snapshot
    // it will be used to build the ballot data
    for (Object CVR : CVRs) {
      HashMap<Object, Object> CVRObject = (HashMap<Object, Object>) CVR;
      String currentSnapshotID = (String) CVRObject.get("CurrentSnapshotId");
      // get ballotID
      String ballotID = (String) CVRObject.get("BallotPrePrintedId");
      // compute internal ballot ID
      String computedCastVoteRecordID = String.format("%s(%d)", this.fileName, ++cvrIndex);

      ArrayList<Object> CVRSnapshots = (ArrayList<Object>) CVRObject.get("CVRSnapshot");
      for (Object snapshotObject : CVRSnapshots) {
        HashMap<Object, Object> snapshot = (HashMap<Object, Object>) snapshotObject;
        if (snapshot.get("@id").equals(currentSnapshotID)) {
          // this is the current snapshot for this CVR -- we will create a new CVR from it

          // list to contain rankings as they are parsed
          List<Pair<Integer, String>> rankings = new ArrayList<>();

          // keep drilling down into the data structure to get the actual rankings:
          // at the top level is a list of contests each of which contains selections
          // (there could be multiple contests in CDF format)
          ArrayList<Object> CVRContests = (ArrayList<Object>) snapshot.get("CVRContest");
          for (Object contestObject : CVRContests) {
            // extract the CVRContest
            HashMap<Object, Object> CVRContest = (HashMap<Object, Object>) contestObject;
            // contest contains contestSelections
            ArrayList<Object> contestSelections = (ArrayList<Object>) CVRContest
                .get("CVRContestSelection");
            for (Object contestSelectionObject : contestSelections) {
              // extract the contestSelection
              HashMap<Object, Object> contestSelection = (HashMap<Object, Object>) contestSelectionObject;
              // selectionID is the candidate/contest ID for this selection position
              String selectionID = (String) contestSelection.get("ContestSelectionId");
              // extract all the positions (ranks) which this selection has been assigned
              ArrayList<Object> selectionPositions = (ArrayList<Object>) contestSelection
                  .get("SelectionPosition");
              for (Object selectionPositionObject : selectionPositions) {
                // extract the position object
                HashMap<Object, Object> selectionPosition = (HashMap<Object, Object>) selectionPositionObject;
                // and finally the rank
                Integer rank = (Integer) selectionPosition.get("Rank");
                // create a new ranking object and save it
                rankings.add(new Pair<>(rank, selectionID));
              }
            }
          }
          // create new cast vote record
          CastVoteRecord newRecord = new CastVoteRecord(computedCastVoteRecordID,
              ballotID, null, null, rankings);
          cvrs.add(newRecord);
          castVoteRecords.add(newRecord);

          // provide some user feedback on the CVR count
          if (cvrs.size() % 50000 == 0) {
            Logger.log(Level.INFO, String.format("Parsed %d cast vote records.", cvrs.size()));
          }

        }
      }

    }
    // return the input list with additions
    return castVoteRecords;
  }
}

