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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.util.Pair;
import network.brightspots.rcv.CastVoteRecord.CvrParseException;
import network.brightspots.rcv.TabulatorSession.UnrecognizedCandidatesException;

@SuppressWarnings("rawtypes")
class DominionCvrReader {

  // canonical manifest file names
  private static final String PRECINCT_MANIFEST = "PrecinctManifest.json";
  private static final String PRECINCT_PORTION_MANIFEST = "PrecinctPortionManifest.json";
  private static final String CANDIDATE_MANIFEST = "CandidateManifest.json";
  private static final String CONTEST_MANIFEST = "ContestManifest.json";
  private static final String CVR_EXPORT = "CvrExport.json";
  private final ContestConfig config;
  private final String manifestFolder;
  private final String undeclaredWriteInLabel;
  // map for tracking unrecognized candidates during parsing
  private final Map<String, Integer> unrecognizedCandidateCounts = new HashMap<>();
  // map of precinct Id to precinct description
  private Map<Integer, String> precincts;
  // map of precinct portion Id to precinct portion description
  private Map<Integer, String> precinctPortions;
  // map of contest Id to Contest data
  private Map<String, Contest> contests;
  private List<Candidate> candidates;

  DominionCvrReader(ContestConfig config, String manifestFolder, String undeclaredWriteInLabel) {
    this.config = config;
    this.manifestFolder = manifestFolder;
    this.undeclaredWriteInLabel = undeclaredWriteInLabel;
  }

  // returns map of contestId to Contest parsed from input file
  private static Map<String, Contest> parseContestData(String contestPath) {
    Map<String, Contest> contests = new HashMap<>();
    try {
      HashMap json = JsonParser.readFromFile(contestPath, HashMap.class);
      // List is a list of candidate objects:
      ArrayList contestList = (ArrayList) json.get("List");
      for (Object contestObject : contestList) {
        HashMap contestMap = (HashMap) contestObject;
        String name = (String) contestMap.get("Description");
        String id = contestMap.get("Id").toString();
        Integer numCandidates = (Integer) contestMap.get("VoteFor");
        Integer maxRanks = (Integer) contestMap.get("NumOfRanks");
        Contest newContest = new Contest(name, id, numCandidates, maxRanks);
        contests.put(id, newContest);
      }
    } catch (Exception exception) {
      Logger.severe("Error parsing contest manifest:\n%s", exception);
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
    } catch (Exception exception) {
      Logger.severe("Error parsing precinct manifest:\n%s", exception);
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
        String contestId = candidateMap.get("ContestId").toString();
        Candidate newCandidate = new Candidate(name, code, contestId);
        candidates.add(newCandidate);
      }
    } catch (Exception exception) {
      Logger.severe("Error parsing candidate manifest:\n%s", exception);
      candidates = null;
    }
    return candidates;
  }

  Map<String, Contest> getContests() {
    return contests;
  }

  // parse CVR JSON for records matching the specified contestId into CastVoteRecord objects and add
  // them to the input list
  void readCastVoteRecords(List<CastVoteRecord> castVoteRecords, String contestId)
      throws CvrParseException, UnrecognizedCandidatesException {
    // read metadata files for precincts, precinct portions, contest, and candidates

    // Precinct data does not exist for earlier versions of Dominion (only precinct portion)
    // See rcv/reference/dominion/CVR export file format.pdf
    Path precinctPath = Paths.get(manifestFolder, PRECINCT_MANIFEST);
    File precinctFile = precinctPath.toFile();
    if (precinctFile.exists()) {
      this.precincts = getPrecinctData(precinctPath.toString());
      if (this.precincts == null) {
        Logger.severe("No precinct data found!");
        throw new CvrParseException();
      }
    }
    Path precinctPortionPath = Paths.get(manifestFolder, PRECINCT_PORTION_MANIFEST);
    this.precinctPortions = getPrecinctData(precinctPortionPath.toString());
    if (this.precinctPortions == null) {
      Logger.severe("No precinct portion data found!");
      throw new CvrParseException();
    }
    Path contestPath = Paths.get(manifestFolder, CONTEST_MANIFEST);
    this.contests = parseContestData(contestPath.toString());
    if (this.contests == null) {
      Logger.severe("No contest data found!");
      throw new CvrParseException();
    }
    Path candidatePath = Paths.get(manifestFolder, CANDIDATE_MANIFEST);
    this.candidates = getCandidates(candidatePath.toString());
    if (this.candidates == null) {
      Logger.severe("No candidate data found!");
      throw new CvrParseException();
    }
    // parse the cvr
    Path cvrPath = Paths.get(manifestFolder, CVR_EXPORT);
    parseCvrFile(cvrPath.toString(), castVoteRecords, contestId);
    if (castVoteRecords.isEmpty()) {
      Logger.severe("No cast vote record data found!");
      throw new CvrParseException();
    }
    if (unrecognizedCandidateCounts.size() > 0) {
      throw new UnrecognizedCandidatesException(unrecognizedCandidateCounts);
    }
  }

  // parse the given file into a List of CastVoteRecords for tabulation
  private void parseCvrFile(
      String filePath, List<CastVoteRecord> castVoteRecords, String contestIdToLoad) {
    // build a lookup map for candidates codes to optimize Cvr parsing
    Map<String, Set<String>> contestIdToCandidateCodes = new HashMap<>();
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
        String tabulatorId = session.get("TabulatorId").toString();
        String batchId = session.get("BatchId").toString();
        Integer recordId = (Integer) session.get("RecordId");
        String suppliedId = recordId.toString();
        // filter out records which are not current and replace them with adjudicated ones
        HashMap adjudicatedData = (HashMap) session.get("Original");
        boolean isCurrent = (boolean) adjudicatedData.get("IsCurrent");
        if (!isCurrent) {
          if (session.containsKey("Modified")) {
            adjudicatedData = (HashMap) session.get("Modified");
          } else {
            Logger.warning(
                "CVR has no adjudicated rankings, skipping: "
                    + "Tabulator ID: %s Batch ID: %s Record ID: %d",
                tabulatorId, batchId, recordId);
            continue;
          }
        }
        // validate precinct (may not exist for older data sets)
        Integer precinctId = (Integer) adjudicatedData.get("PrecinctId");
        if (precinctId != null
            && (this.precincts == null || !this.precincts.containsKey(precinctId))) {
          Logger.severe("Precinct ID \"%d\" from CVR not found in manifest data!", precinctId);
          throw new CvrParseException();
        }
        String precinct = this.precincts != null ? this.precincts.get(precinctId) : null;
        // validate precinct portion
        Integer precinctPortionId = (Integer) adjudicatedData.get("PrecinctPortionId");
        if (precinctPortionId != null && !this.precinctPortions.containsKey(precinctPortionId)) {
          Logger.severe(
              "Precinct portion ID \"%d\" from CVR not found in manifest data!", precinctPortionId);
          throw new CvrParseException();
        }
        String precinctPortion = this.precinctPortions.get(precinctPortionId);
        String ballotTypeId = adjudicatedData.get("BallotTypeId").toString();

        ArrayList contests;
        // sometimes there is a "Cards" object at this level
        if (adjudicatedData.containsKey("Cards")) {
          ArrayList cardsList = (ArrayList) adjudicatedData.get("Cards");
          HashMap cardsObject = (HashMap) cardsList.get(0);
          contests = (ArrayList) cardsObject.get("Contests");
        } else {
          contests = (ArrayList) adjudicatedData.get("Contests");
        }

        // each contest object is a cvr
        for (Object contestObject : contests) {
          HashMap contest = (HashMap) contestObject;
          String contestId = contest.get("Id").toString();
          // skip this CVR if it's not for the contest we're interested in
          if (!contestId.equals(contestIdToLoad)) {
            continue;
          }
          // validate contest id
          if (!this.contests.containsKey(contestId)
              || !contestIdToCandidateCodes.containsKey(contestId)) {
            Logger.severe("Unknown contest ID '%d' found while parsing CVR!", contestId);
            throw new CvrParseException();
          }
          ArrayList<Pair<Integer, String>> rankings = new ArrayList<>();
          // marks is an array of rankings
          ArrayList marks = (ArrayList) contest.get("Marks");
          for (Object rankingObject : marks) {
            HashMap rankingMap = (HashMap) rankingObject;
            // skip ambiguous rankings
            boolean isAmbiguous = (boolean) rankingMap.get("IsAmbiguous");
            if (isAmbiguous) {
              continue;
            }
            Integer candidateId = (Integer) rankingMap.get("CandidateId");
            String candidateCode = candidateId.toString();
            Set<String> candidates = contestIdToCandidateCodes.get(contestId);
            if (!candidates.contains(candidateCode)) {
              Logger.severe(
                  "Candidate code '%s' is not valid for contest '%d'!", candidateCode, contestId);
              throw new CvrParseException();
            }
            // We also need to throw an error if this candidate doesn't appear in the tabulator's
            // config file for this contest.
            if (candidateCode.equals(undeclaredWriteInLabel)) {
              candidateCode = Tabulator.UNDECLARED_WRITE_IN_OUTPUT_LABEL;
            } else if (!config.getCandidateCodeList().contains(candidateCode)) {
              unrecognizedCandidateCounts.merge(candidateCode, 1, Integer::sum);
            }

            Integer rank = (Integer) rankingMap.get("Rank");
            Pair<Integer, String> ranking = new Pair<>(rank, candidateCode);
            rankings.add(ranking);
          }
          // create the new Cvr
          CastVoteRecord newCvr =
              new CastVoteRecord(
                  contestId,
                  tabulatorId,
                  batchId,
                  suppliedId,
                  precinct,
                  precinctPortion,
                  ballotTypeId,
                  rankings);
          castVoteRecords.add(newCvr);
        }
        // provide some user feedback on the Cvr count
        if (castVoteRecords.size() % 50000 == 0) {
          Logger.info("Parsed %d cast vote records.", castVoteRecords.size());
        }
      }
    } catch (Exception exception) {
      Logger.severe("Error parsing cast vote record:\n%s", exception);
      castVoteRecords.clear();
    }
  }

  // Candidate data from a Dominion candidate manifest Json
  @SuppressWarnings({"FieldCanBeLocal", "unused", "RedundantSuppression"})
  static class Candidate {

    private final String name;
    private final String code;
    private final String contestId;

    Candidate(String name, String code, String contestId) {
      this.name = name;
      this.code = code;
      this.contestId = contestId;
    }

    public String getCode() {
      return code;
    }

    public String getContestId() {
      return contestId;
    }
  }

  // Contest data from a Dominion contest manifest Json
  @SuppressWarnings({"FieldCanBeLocal", "unused", "RedundantSuppression"})
  static class Contest {

    private final String name;
    private final String id;
    private final Integer numCandidates;
    private final Integer maxRanks;

    Contest(String name, String id, Integer numCandidates, Integer maxRanks) {
      this.name = name;
      this.id = id;
      this.numCandidates = numCandidates;
      this.maxRanks = maxRanks;
    }

    String getId() {
      return id;
    }

    Integer getMaxRanks() {
      return maxRanks;
    }
  }
}
