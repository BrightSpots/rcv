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
 * Helper class to read and parse a Common Data Format file into cast vote record objects
 * and candidate names.
 */

package com.rcv;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import javafx.util.Pair;

class CommonDataFormatReader {

  // path of the source file
  private final String filePath;

  // function: CommonDataFormatReader
  // purpose: class constructor
  // param: filePath source file to read
  CommonDataFormatReader(String filePath) {
    this.filePath = filePath;
  }

  // function: getCandidates
  // purpose: returns list of candidates parsed from CDF election json
  Set<String> getCandidates()
  {
    // container for results
    Set<String> candidates = new HashSet<>();
    try {
      // try to read in the file
      HashMap json = JsonParser.readFromFile(filePath, HashMap.class);
      // top-level election is a list of election objects:
      ArrayList electionArray = (ArrayList) json.get("Election");
      for(Object electionObject : electionArray) {
        HashMap election = (HashMap) electionObject;
        // for each election get the contests it contains
        ArrayList contestArray = (ArrayList) election.get("Contest");
        for(Object contestObject : contestArray) {
          HashMap contest = (HashMap) contestObject;
          // for each contest get the contest selections
          ArrayList contestSelectionArray = (ArrayList) contest.get("ContestSelection");
          for(Object contestSelectionObject : contestSelectionArray) {
            HashMap contestSelection = (HashMap) contestSelectionObject;
            // selectionID is the candidate name
            String selectionID = (String) contestSelection.get("@id");
            candidates.add(selectionID);
          }
        }
      }
    } catch (Exception e) {
      Logger.log(Level.SEVERE, String.format("Error parsing candidate data: %s", e.toString()));
    }
    return candidates;
  }


  // function: parseCVRFile
  // purpose: parse the given file into a List of CastVoteRecords for tabulation
  // param: castVoteRecords existing list to append new CastVoteRecords to
  // returns: list of parsed CVRs
  List<CastVoteRecord> parseCVRFile(List<CastVoteRecord> castVoteRecords) {

    List<CastVoteRecord> cvrs = new ArrayList<>();
    // cvrIndex and fileName are used to generate IDs for cvrs
    Integer cvrIndex = 0;
    String fileName = new File(filePath).getName();

    try {
      HashMap json = JsonParser.readFromFile(filePath, HashMap.class);
      // we expect a top-level "CVR" object containing a list of CVR objects
      ArrayList CVRs = (ArrayList) json.get("CVR");
      // for each CVR object extract the current snapshot
      // it will be used to build the ballot data
      for (Object CVR : CVRs) {
        HashMap CVRObject = (HashMap) CVR;
        String currentSnapshotID = (String) CVRObject.get("CurrentSnapshotId");
        // get ballotID
        String ballotID = (String) CVRObject.get("BallotPrePrintedId");
        // compute internal ballot ID
        String computedCastVoteRecordID = String.format("%s(%d)", fileName, ++cvrIndex);

        ArrayList CVRSnapshots = (ArrayList) CVRObject.get("CVRSnapshot");
        for (Object snapshotObject : CVRSnapshots) {
          HashMap snapshot = (HashMap) snapshotObject;
          if (snapshot.get("@id").equals(currentSnapshotID)) {
            // we found the current CVR snapshot
            // parse out the rankings and create a new cvr

            // list to contain rankings as they are parsed
            List<Pair<Integer, String>> rankings = new ArrayList<>();

            // at the top level is a list of contests each of which contains selections
            ArrayList CVRContests = (ArrayList) snapshot.get("CVRContest");
            for (Object contestObject : CVRContests) {
              // extract the CVRContest
              HashMap CVRContest = (HashMap) contestObject;
              // contest contains contestSelections
              ArrayList contestSelections = (ArrayList) CVRContest
                  .get("CVRContestSelection");
              for (Object contestSelectionObject : contestSelections) {
                // extract the contestSelection
                HashMap contestSelection = (HashMap) contestSelectionObject;
                // selectionID is the candidate/contest ID for this selection position
                String selectionID = (String) contestSelection.get("ContestSelectionId");
                // extract all the positions (ranks) which this selection has been assigned
                ArrayList selectionPositions = (ArrayList) contestSelection
                    .get("SelectionPosition");
                for (Object selectionPositionObject : selectionPositions) {
                  // extract the position object
                  HashMap selectionPosition = (HashMap) selectionPositionObject;
                  // and finally the rank
                  Integer rank = (Integer) selectionPosition.get("Rank");
                  assert rank != null;
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
    } catch (Exception e) {
      Logger.log(Level.SEVERE, String.format("Error parsing CDF data: %s", e.toString()));
    }

    // return the input list with additions
    return castVoteRecords;
  }
}

