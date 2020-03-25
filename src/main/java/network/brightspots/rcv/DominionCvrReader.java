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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javafx.util.Pair;
import network.brightspots.rcv.CastVoteRecord.CvrParseException;
import network.brightspots.rcv.RawContestConfig.Candidate;

class DominionCvrReader {

  // canonical manifest file names
  private static final String PRECINCT_MANIFEST = "PrecinctManifest.json";
  private static final String PRECINCT_PORTION_MANIFEST = "PrecinctPortionManifest.json";
  private static final String CANDIDATE_MANIFEST = "CandidateManifest.json";
  private static final String CONTEST_MANIFEST = "ContestManifest.json";
  private static final String CVR_EXPORT = "CvrExport.json";

  private final String manifestFolder;
  // map of precinct Id to precinct description
  private Map<Integer, String> precincts;
  // map of precinct portion Id to precinct portion description
  private Map<Integer, String> precinctPortions;
  // map of contest Id to Contest data
  private Map<Integer, Contest> contests;
  private List<Candidate> candidates;

  DominionCvrReader(String manifestFolder) {
    this.manifestFolder = manifestFolder;
  }

  // returns map of contestId to Contest parsed from input file
  private static Map<Integer, Contest> getContests(String contestPath) {
    Map<Integer, Contest> contests = new HashMap<>();
    try {
      HashMap json = JsonParser.readFromFile(contestPath, HashMap.class);
      // List is a list of candidate objects:
      ArrayList contestList = (ArrayList) json.get("List");
      for (Object contestObject : contestList) {
        HashMap contestMap = (HashMap) contestObject;
        String name = (String) contestMap.get("Description");
        Integer id = (Integer) contestMap.get("Id");
        Integer numCandidates = (Integer) contestMap.get("VoteFor");
        Integer maxRanks = (Integer) contestMap.get("NumOfRanks");
        Contest newContest = new Contest(name, id, numCandidates, maxRanks);
        contests.put(id, newContest);
      }
    } catch (Exception e) {
      Logger.log(Level.SEVERE, "Error parsing contest manifest:\n%s", e.toString());
      contests = null;
    }
    return contests;
  }

  // returns map from Id to Description parsed from input file
  // PrecinctManifest.json and PrecinctPortionManifest.json use this structure
  private static Map<Integer, String> getPrecinctData(String precinctPath) {
    Map<Integer, String> precinctsById = new HashMap<>();
    try {
      HashMap json = JsonParser.readFromFile(precinctPath, HashMap.class);
      ArrayList precinctList = (ArrayList) json.get("List");
      for (Object precinctObject : precinctList) {
        HashMap precinctMap = (HashMap) precinctObject;
        String name = (String) precinctMap.get("Description");
        Integer id = (Integer) precinctMap.get("Id");
        precinctsById.put(id, name);
      }
    } catch (Exception e) {
      Logger.log(Level.SEVERE, "Error parsing precinct manifest:\n%s", e.toString());
      precinctsById = null;
    }
    return precinctsById;
  }

  // returns list of Candidate objects parsed from CandidateManifest.json
  private static List<Candidate> getCandidates(String candidatePath) {
    ArrayList<Candidate> candidates = new ArrayList<>();
    try {
      HashMap json = JsonParser.readFromFile(candidatePath, HashMap.class);
      ArrayList candidateList = (ArrayList) json.get("List");
      for (Object candidateObject : candidateList) {
        HashMap candidateMap = (HashMap) candidateObject;
        String name = (String) candidateMap.get("Description");
        Integer id = (Integer) candidateMap.get("Id");
        String code = id.toString();
        Integer contestId = (Integer) candidateMap.get("ContestId");
        Candidate newCandidate = new Candidate(name, code, false, contestId);
        candidates.add(newCandidate);
      }
    } catch (Exception e) {
      Logger.log(Level.SEVERE, "Error parsing candidate manifest:\n%s", e.toString());
      candidates = null;
    }
    return candidates;
  }

  Map<Integer, Contest> getContests() {
    return contests;
  }

  // parse Cvr json into CastVoteRecord objects and add them to the input list
  void readCastVoteRecords(List<CastVoteRecord> castVoteRecords) throws CvrParseException {
    // read metadata files for precincts, precinct portions, contest, and candidates
    Path precinctPath = Paths.get(manifestFolder, PRECINCT_MANIFEST);
    this.precincts = getPrecinctData(precinctPath.toString());
    if (this.precincts == null) {
      Logger.log(Level.SEVERE, "No precinct data found!");
      throw new CvrParseException();
    }
    Path precinctPortionPath = Paths.get(manifestFolder, PRECINCT_PORTION_MANIFEST);
    this.precinctPortions = getPrecinctData(precinctPortionPath.toString());
    if (this.precinctPortions == null) {
      Logger.log(Level.SEVERE, "No precinct portion data found!");
      throw new CvrParseException();
    }
    Path contestPath = Paths.get(manifestFolder, CONTEST_MANIFEST);
    this.contests = getContests(contestPath.toString());
    if (this.contests == null) {
      Logger.log(Level.SEVERE, "No contest data found!");
      throw new CvrParseException();
    }
    Path candidatePath = Paths.get(manifestFolder, CANDIDATE_MANIFEST);
    this.candidates = getCandidates(candidatePath.toString());
    if (this.candidates == null) {
      Logger.log(Level.SEVERE, "No candidate data found!");
      throw new CvrParseException();
    }
    // parse the cvr
    Path cvrPath = Paths.get(manifestFolder, CVR_EXPORT);
    parseCvrFile(cvrPath.toString(), castVoteRecords);
    if (castVoteRecords.isEmpty()) {
      Logger.log(Level.SEVERE, "No cast vote record data found!");
      throw new CvrParseException();
    }
  }

  // parse the given file into a List of CastVoteRecords for tabulation
  private void parseCvrFile(String filePath, List<CastVoteRecord> castVoteRecords) {

    // build a lookup map for candidates codes to optimize Cvr parsing
    Map<Integer, Set<String>> contestIdToCandidateCodes = new HashMap<>();
    for (Candidate candidate : this.candidates) {
      Set<String> candidates;
      if (contestIdToCandidateCodes.containsKey(candidate.getContestId())) {
        candidates = contestIdToCandidateCodes.get(candidate.getContestId());
      } else {
        candidates = new HashSet<>();
      }
      candidates.add(candidate.getCode());
      contestIdToCandidateCodes.put(candidate.getContestId(), candidates);
    }

    try {
      HashMap json = JsonParser.readFromFile(filePath, HashMap.class);
      // top-level "Sessions" object contains a lists of Cvr objects from different tabulators
      ArrayList sessions = (ArrayList) json.get("Sessions");
      // for each Cvr object extract various fields
      for (Object sessionObject : sessions) {
        HashMap session = (HashMap) sessionObject;
        // extract various ids
        Integer tabulatorId = (Integer) session.get("TabulatorId");
        Integer batchId = (Integer) session.get("BatchId");
        Integer recordId = (Integer) session.get("RecordId");
        String suppliedId = recordId.toString();
        HashMap originalObject = (HashMap) session.get("Original");
        // filter out records which are not current (these result from adjudication)
        Boolean isCurrent = (Boolean) originalObject.get("IsCurrent");
        if (!isCurrent) {
          continue;
        }
        // validate precinct
        Integer precinctId = (Integer) originalObject.get("PrecinctId");
        if (precinctId != null && !this.precincts.containsKey(precinctId)) {
          Logger.log(
              Level.SEVERE,
              "Precinct ID \"%d\" from CVR not found in manifest data!",
              precinctId);
          throw new CvrParseException();
        }
        String precinct = this.precincts.get(precinctId);
        // validate precinct portion
        Integer precinctPortionId = (Integer) originalObject.get("PrecinctPortionId");
        if (precinctPortionId != null && !this.precinctPortions.containsKey(precinctPortionId)) {
          Logger.log(
              Level.SEVERE,
              "Precinct portion Id '%d' from Cvr not found in manifest data!",
              precinctPortionId);
          throw new CvrParseException();
        }
        String precinctPortion = this.precinctPortions.get(precinctPortionId);
        Integer ballotTypeId = (Integer) originalObject.get("BallotTypeId");

        ArrayList contests;
        // sometimes there is a "Cards" object at this level
        if (originalObject.containsKey("Cards")) {
          ArrayList cardsList = (ArrayList) originalObject.get("Cards");
          HashMap cardsObject = (HashMap) cardsList.get(0);
          contests = (ArrayList) cardsObject.get("Contests");
        } else {
          contests = (ArrayList) originalObject.get("Contests");
        }

        // each contest object is a cvr
        for (Object contestObject : contests) {
          HashMap contest = (HashMap) contestObject;
          Integer contestId = (Integer) contest.get("Id");
          // validate contest id
          if (!this.contests.containsKey(contestId)
              || !contestIdToCandidateCodes.containsKey(contestId)) {
            Logger.log(Level.SEVERE, "Unknown contest ID '%d' found while parsing CVR!", contestId);
            throw new CvrParseException();
          }
          ArrayList<Pair<Integer, String>> rankings = new ArrayList<>();
          // marks is an array of rankings
          ArrayList marks = (ArrayList) contest.get("Marks");
          for (Object rankingObject : marks) {
            HashMap rankingMap = (HashMap) rankingObject;
            Integer candidateId = (Integer) rankingMap.get("CandidateId");
            String candidateCode = candidateId.toString();
            Set<String> candidates = contestIdToCandidateCodes.get(contestId);
            if (!candidates.contains(candidateCode)) {
              Logger.log(
                  Level.SEVERE,
                  "Candidate code '%s' is not valid for contest '%d'!",
                  candidateCode,
                  contestId);
              throw new CvrParseException();
            }

            Integer rank = (Integer) rankingMap.get("Rank");
            Pair<Integer, String> ranking = new Pair<>(rank, candidateCode);
            rankings.add(ranking);
          }
          // create the new Cvr
          CastVoteRecord newCvr =
              new CastVoteRecord(
                  contestId, tabulatorId, batchId, suppliedId, precinct, precinctPortion,
                  ballotTypeId, rankings);
          castVoteRecords.add(newCvr);
        }
        // provide some user feedback on the Cvr count
        if (castVoteRecords.size() % 50000 == 0) {
          Logger.log(Level.INFO, "Parsed %d cast vote records.", castVoteRecords.size());
        }
      }
    } catch (Exception e) {
      Logger.log(Level.SEVERE, "Error parsing cast vote record:\n%s", e.toString());
      castVoteRecords.clear();
    }
  }

  // Simple container class for contest data
  static class Contest {

    private final String name;
    private final Integer id;
    private final Integer numCandidates;
    private final Integer maxRanks;

    Contest(String name, Integer id, Integer numCandidates, Integer maxRanks) {
      this.name = name;
      this.id = id;
      this.numCandidates = numCandidates;
      this.maxRanks = maxRanks;
    }

    Integer getId() {
      return id;
    }

    Integer getMaxRanks() {
      return maxRanks;
    }
  }
}
