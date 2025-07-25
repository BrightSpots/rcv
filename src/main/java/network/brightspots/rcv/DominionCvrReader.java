/*
 * RCTab
 * Copyright (c) 2017-2023 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: Read and parse Dominion election data for a contest into CastVoteRecord objects and
 * other election metadata (candidate names, precinct name, etc.).
 * Design: Dominion uses a set of JSON files to store election data.  This class uses Jackson
 * ObjectMapper to read these JSON files into memory at once.  This simplifies parsing code, but
 * does have the limitation that the memory footprint during parsing is proportional to the size of
 * the CVR JSON file.
 * Conditions: When reading Dominion election data.
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.util.Pair;
import network.brightspots.rcv.CastVoteRecord.CvrParseException;

@SuppressWarnings("rawtypes")
class DominionCvrReader extends BaseCvrReader {

  // canonical manifest file names
  private static final String PRECINCT_MANIFEST = "PrecinctManifest.json";
  private static final String PRECINCT_PORTION_MANIFEST = "PrecinctPortionManifest.json";
  private static final String CANDIDATE_MANIFEST = "CandidateManifest.json";
  private static final String CONTEST_MANIFEST = "ContestManifest.json";
  private static final String CVR_EXPORT = "CvrExport.json";
  private static final String CVR_EXPORT_PATTERN = "CvrExport_%d.json";
  private static final int EXCLUDE_CONTEST_CONDITION_ID = 7;
  // map of precinct ID to precinct description
  private Map<Integer, String> precincts;
  // map of precinct portion ID to precinct portion description
  private Map<Integer, String> precinctPortions;
  // map of contest ID to Contest data
  private Map<String, Contest> contests;
  private Map<String, Candidate> candidateCodesToCandidates;

  DominionCvrReader(ContestConfig config, RawContestConfig.CvrSource source) {
    super(config, source);
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

  // returns map from ID to Description parsed from input file
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

  // returns a map of Codes to Candidate objects parsed from CandidateManifest.json
  private Map<String, Candidate> getCandidates(String candidatePath) {
    Map<String, Candidate> candidateCodesToCandidates = new HashMap<>();
    try {
      HashMap json = JsonParser.readFromFile(candidatePath, HashMap.class);
      ArrayList candidateList = (ArrayList) json.get("List");
      for (Object candidateObject : candidateList) {
        HashMap candidateMap = (HashMap) candidateObject;
        String name = (String) candidateMap.get("Description");
        Integer id = (Integer) candidateMap.get("Id");
        String code = id.toString();
        String contestId = candidateMap.get("ContestId").toString();
        if (!source.getContestId().equals(contestId)) {
          continue;
        }
        Candidate newCandidate = new Candidate(name, code, contestId);
        candidateCodesToCandidates.put(code, newCandidate);
      }
    } catch (Exception exception) {
      Logger.severe("Error parsing candidate manifest:\n%s", exception);
      candidateCodesToCandidates = null;
    }
    return candidateCodesToCandidates;
  }

  @Override
  public String readerName() {
    return "Dominion";
  }

  // parse CVR JSON for records matching the specified contestId into CastVoteRecord objects and add
  // them to the input list
  @Override
  void readCastVoteRecords(List<CastVoteRecord> castVoteRecords) throws CvrParseException {
    loadManifests();

    gatherCvrsForContest(castVoteRecords, source.getContestId());
    if (castVoteRecords.isEmpty()) {
      Logger.severe("No cast vote record data found!");
      throw new CvrParseException();
    }
  }

  private void loadManifests() throws CvrParseException {
    // read metadata files for precincts, precinct portions, contest, and candidates

    // Precinct data does not exist for earlier versions of Dominion (only precinct portion)
    // See rcv/reference/dominion/CVR export file format.pdf
    Path precinctPath = Paths.get(cvrPath, PRECINCT_MANIFEST);
    File precinctFile = precinctPath.toFile();
    if (precinctFile.exists()) {
      this.precincts = getPrecinctData(precinctPath.toString());
      if (this.precincts == null) {
        Logger.severe("No precinct data found!");
        throw new CvrParseException();
      }
    }
    Path precinctPortionPath = Paths.get(cvrPath, PRECINCT_PORTION_MANIFEST);
    this.precinctPortions = getPrecinctData(precinctPortionPath.toString());
    if (this.precinctPortions == null) {
      Logger.severe("No precinct portion data found!");
      throw new CvrParseException();
    }
    Path contestPath = Paths.get(cvrPath, CONTEST_MANIFEST);
    this.contests = parseContestData(contestPath.toString());
    if (this.contests == null) {
      Logger.severe("No contest data found!");
      throw new CvrParseException();
    }
    Path candidatePath = Paths.get(cvrPath, CANDIDATE_MANIFEST);
    this.candidateCodesToCandidates = getCandidates(candidatePath.toString());
    if (this.candidateCodesToCandidates == null) {
      Logger.severe("No candidate data found!");
      throw new CvrParseException();
    }
  }

  @Override
  public void runAdditionalValidations(List<CastVoteRecord> castVoteRecords)
      throws CastVoteRecord.CvrParseException {
    super.runAdditionalValidations(castVoteRecords);
    validateNamesAreInContest(castVoteRecords);
  }

  @Override
  protected boolean isRankingAllowed(int rank, String contestId) {
    return rank > 0
      && rank <= contests.get(contestId).getMaxRanks()
      && config.isRankingAllowed(rank);
  }

  private void validateNamesAreInContest(List<CastVoteRecord> castVoteRecords)
      throws CastVoteRecord.CvrParseException {
    // build a lookup map to optimize CVR parsing
    Map<String, Set<String>> contestIdToCandidateNames = new HashMap<>();
    for (Candidate candidate : this.candidateCodesToCandidates.values()) {
      Set<String> candidateNames;
      if (contestIdToCandidateNames.containsKey(candidate.getContestId())) {
        candidateNames = contestIdToCandidateNames.get(candidate.getContestId());
      } else {
        candidateNames = new HashSet<>();
      }
      candidateNames.add(config.getNameForCandidate(candidate.getCode()));
      contestIdToCandidateNames.put(candidate.getContestId(), candidateNames);
    }

    // Check each candidate exists in the contest
    for (CastVoteRecord cvr : castVoteRecords) {
      String contestId = cvr.getContestId();
      Set<String> candidateNames = contestIdToCandidateNames.get(contestId);
      if (candidateNames == null) {
        Logger.severe("Contest ID '%s' had no candidates!", contestId);
        throw new CastVoteRecord.CvrParseException();
      }

      for (Pair<Integer, CandidatesAtRanking> ranking : cvr.candidateRankings) {
        for (String candidateId : ranking.getValue()) {
          String candidateName = config.getNameForCandidate(candidateId);
          // Note: candidateId is replaced with Tabulator.UNDECLARED_WRITE_IN_OUTPUT_LABEL when
          // reading in the CVRs.
          if (candidateId.equals(Tabulator.UNDECLARED_WRITE_IN_OUTPUT_LABEL)) {
            continue;
          }
          if (!candidateNames.contains(candidateName)) {
            Logger.severe(
                "Candidate ID '%s' is not valid for contest '%s'!", candidateName, contestId);
            throw new CastVoteRecord.CvrParseException();
          }
        }
      }
    }
  }

  // The Candidate Autoload looks only at the CVR file(s) and not the manifest files, so
  // it doesn't know about the mapping between a code and the candidate's name. This function
  // addresses that discrepancy, while also being much faster than actually reading each ballot.
  @Override
  public Set<RawContestConfig.Candidate> gatherUnknownCandidates(
          List<CastVoteRecord> castVoteRecords) {
    try {
      loadManifests();
    } catch (CvrParseException exception) {
      Logger.severe("Error loading manifest data:\n%s", exception);
      return new HashSet<>();
    }

    Set<String> knownNames = config.getCandidateNames();

    // Return the candidate codes that are not in the knownNames set
    return candidateCodesToCandidates.entrySet().stream()
            .filter(entry -> !knownNames.contains(entry.getValue().name))
            .map(entry -> new RawContestConfig.Candidate(entry.getValue().name, entry.getKey()))
            .collect(Collectors.toSet());
  }

  // parse the CVR file or files into a List of CastVoteRecords for tabulation
  private void gatherCvrsForContest(List<CastVoteRecord> castVoteRecords, String contestIdToLoad) {
    try {
      Path singleCvrPath = Paths.get(cvrPath, CVR_EXPORT);

      if (singleCvrPath.toFile().exists()) {
        HashMap json = JsonParser.readFromFile(singleCvrPath.toString(), HashMap.class);
        parseCvrFile(json, castVoteRecords, contestIdToLoad);
      } else {
        // We are expecting multiple CvrExport_N.json files
        String regexPath = CVR_EXPORT_PATTERN.replaceAll("%d", "\\\\d+");
        File cvrDirectory = new File(cvrPath);
        File[] matchedCvrFileArray = cvrDirectory.listFiles((dir, name) -> name.matches(regexPath));

        if (matchedCvrFileArray == null || matchedCvrFileArray.length == 0) {
          String errorMessage = "Error parsing Dominion cast vote records:"
                  + " CvrExport.json file(s) not located";
          throw new FileNotFoundException(errorMessage);
        }

        List<File> matchedCvrFiles = Arrays.asList(matchedCvrFileArray);
        matchedCvrFiles.sort(Comparator.comparing(File::getAbsolutePath));

        int recordsParsed = 0;
        int filesParsed = 0;
        int recordsParsedAtLastLog = 0;

        for (File file : matchedCvrFiles) {
          HashMap json = JsonParser.readFromFile(file.toString(), HashMap.class);
          recordsParsed += parseCvrFile(json, castVoteRecords, contestIdToLoad);
          filesParsed++;

          if (recordsParsed - recordsParsedAtLastLog > 50000) {
            Logger.info("Parsed %d records from %d files", recordsParsed, filesParsed);
            recordsParsedAtLastLog = recordsParsed;
          }
        }

        Logger.info("Parsed %d total records from %d total files", recordsParsed, filesParsed);
      }
    } catch (FileNotFoundException | CvrParseException exception) {
      Logger.severe("Error parsing cast vote record:\n%s", exception);
      castVoteRecords.clear();
    }
  }

  private int parseCvrFile(
      HashMap json, List<CastVoteRecord> castVoteRecords, String contestIdToLoad)
      throws CvrParseException {
    // top-level "Sessions" object contains a lists of Cvr objects from different tabulators
    ArrayList sessions = (ArrayList) json.get("Sessions");
    int recordsParsed = 0;
    // for each Cvr object extract various fields
    for (Object sessionObject : sessions) {
      HashMap session = (HashMap) sessionObject;
      // extract various ids
      String tabulatorId = session.get("TabulatorId").toString();
      String batchId = session.get("BatchId").toString();
      Integer recordId = (Integer) session.get("RecordId");

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

      ArrayList cardsList;
      // sometimes there is a "Cards" object at this level
      if (adjudicatedData.containsKey("Cards")) {
        cardsList = (ArrayList) adjudicatedData.get("Cards");
      } else {
        ArrayList<HashMap> oneCardList = new ArrayList<>(1);
        oneCardList.add(adjudicatedData);
        cardsList = oneCardList;
      }

      for (Object cardObject : cardsList) {
        HashMap card = (HashMap) cardObject;
        ArrayList contests = (ArrayList) card.get("Contests");

        // each contest object is a cvr
        for (Object contestObject : contests) {
          HashMap contest = (HashMap) contestObject;
          String contestId = contest.get("Id").toString();
          // skip this CVR if it's not for the contest we're interested in
          if (!contestId.equals(contestIdToLoad)) {
            continue;
          }
          // validate contest id
          if (!this.contests.containsKey(contestId)) {
            Logger.severe("Unknown contest ID '%s' found while parsing CVR!", contestId);
            throw new CvrParseException();
          }
          ArrayList outstackConditionIds = (ArrayList) contest.get("OutstackConditionIds");
          if (outstackConditionIds.contains(EXCLUDE_CONTEST_CONDITION_ID)) {
            continue;
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
            String dominionCandidateId = rankingMap.get("CandidateId").toString();
            if (dominionCandidateId.equals(source.getUndeclaredWriteInLabel())) {
              dominionCandidateId = Tabulator.UNDECLARED_WRITE_IN_OUTPUT_LABEL;
            }
            Integer rank = (Integer) rankingMap.get("Rank");
            Pair<Integer, String> ranking = new Pair<>(rank, dominionCandidateId);
            rankings.add(ranking);
          }
          // create the new cvr
          String suppliedId = recordId.toString();
          String computedId =
                  Stream.of(tabulatorId, batchId, Integer.toString(recordId))
                          .filter(s -> s != null && !s.isBlank())
                          // dashes are not escaped when writing to file e.g. CDF
                          .collect(Collectors.joining("-"));
          CastVoteRecord newCvr = new CastVoteRecord(
              contestId,
              tabulatorId,
              batchId,
              suppliedId,
              computedId,
              precinct,
              precinctPortion,
              usesLastAllowedRanking(rankings, contestId),
              rankings);
          castVoteRecords.add(newCvr);
        }
      }
      // provide some user feedback on the cvr count
      recordsParsed++;
      if (recordsParsed > 0 && recordsParsed % 50000 == 0) {
        Logger.info("Parsed %d cast vote records.", recordsParsed);
      }
    }
    return recordsParsed;
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
