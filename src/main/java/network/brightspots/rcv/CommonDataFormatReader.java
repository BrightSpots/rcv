/*
 * RCTab
 * Copyright (c) 2017-2023 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: Read and parse a Common Data Format (xml) file into cast vote record objects, and other
 * contest metadata (notably candidate names).  CDF uses internal ids to map candidate names (i.e.
 * contest "options") and geographical units to CVR options and precinct names respectively.
 * Building this mapping happens before records can be parsed.
 * Design: This class uses Jackson xmlmapper to read each CDF file into memory at once.  This
 * simplifies parsing code a bit, but also means that (for now) larger CDF files will result in
 * larger memory consumption during parsing.
 * Conditions: Used when reading and tabulating CDF election data.
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javafx.util.Pair;
import network.brightspots.rcv.CastVoteRecord.CvrParseException;

@SuppressWarnings({"rawtypes", "unused", "RedundantSuppression"})
class CommonDataFormatReader extends BaseCvrReader {
  private static final String STATUS_NO = "no";
  private static final String BOOLEAN_TRUE = "true";

  CommonDataFormatReader(ContestConfig config, RawContestConfig.CvrSource source) {
    super(config, source);
  }

  /**
   * Sets map[key] = value if map does not contain key already. If it does, returns false. The
   * caller is responsible for throwing an error after aggregating all keys that were not unique.
   *
   * @param map The map to modify
   * @param key Key to check for uniqueness
   * @param value Value to place if unique
   * @param keyName Human-readable name of the key, used to make the error message more useful
   * @return true if there was an error
   */
  private static <K, V> boolean putIfUnique(HashMap<K, V> map, K key, V value, String keyName) {
    if (map.containsKey(key)) {
      Logger.severe("%s \"%s\" appears multiple times", keyName, key);
      return true;
    }

    map.put(key, value);
    return false;
  }

  @Override
  public String readerName() {
    return "CDF";
  }

  // Each CVRSnapshot contains one or more CVRContest objects.
  // Find the CVRContest in the snapshot corresponding to the Contest we are tabulating
  CVRContest getCvrContestXml(CVR cvr, Contest contestToTabulate) {
    CVRContest cvrContestToTabulate = null;
    // find current snapshot
    CVRSnapshot currentSnapshot = null;
    if (cvr.CVRSnapshot != null) {
      for (CVRSnapshot cvrSnapshot : cvr.CVRSnapshot) {
        if (cvrSnapshot.ObjectId.equals(cvr.CurrentSnapshotId)) {
          currentSnapshot = cvrSnapshot;
          break;
        }
      }
    }
    // find CVRContest which matches the Contest we are tabulating
    if (currentSnapshot != null && currentSnapshot.CVRContest != null) {
      for (CVRContest cvrContest : currentSnapshot.CVRContest) {
        if (cvrContest.ContestId.equals(contestToTabulate.ObjectId)) {
          cvrContestToTabulate = cvrContest;
          break;
        }
      }
    }
    return cvrContestToTabulate;
  }

  @Override
  void readCastVoteRecords(List<CastVoteRecord> castVoteRecords, Set<String> precinctIds)
      throws CvrParseException {
    try {
      if (cvrPath.endsWith(".xml")) {
        parseXml(castVoteRecords);
      } else if (cvrPath.endsWith(".json")) {
        parseJson(castVoteRecords);
      }
    } catch (CvrParseException e) {
      throw e;
    } catch (Exception e) {
      Logger.severe("Unknown error. Cannot load file.");
      throw new CvrParseException();
    }
  }

  private HashMap getCvrContestJson(HashMap cvr, String contestIdToTabulate)
      throws CvrParseException {
    HashMap cvrContestToTabulate = null;
    String currentSnapshotId = (String) cvr.get("CurrentSnapshotId");
    ArrayList cvrSnapshots = (ArrayList) cvr.get("CVRSnapshot");
    HashMap currentSnapshot = null;
    for (Object snapshotObject : cvrSnapshots) {
      HashMap snapshot = (HashMap) snapshotObject;
      if (snapshot.get("@id").equals(currentSnapshotId)) {
        currentSnapshot = snapshot;
        break;
      }
    }
    // find the cvr contest in this snapshot
    if (!Objects.requireNonNull(currentSnapshot).containsKey("CVRContest")) {
      Logger.severe("Current snapshot has no CVRContests.");
      throw new CvrParseException();
    }
    ArrayList cvrContests = (ArrayList) currentSnapshot.get("CVRContest");
    for (Object contestObject : cvrContests) {
      HashMap cvrContest = (HashMap) contestObject;
      // filter by contest ID
      String contestId = (String) cvrContest.get("ContestId");
      if (contestId.equals(contestIdToTabulate)) {
        cvrContestToTabulate = cvrContest;
        break;
      }
    }
    return cvrContestToTabulate;
  }

  void checkForEmptyFields(CastVoteRecordReport cvrReport) throws CvrParseException {
    // Some checks to provide nicer error messages.
    // This is common with Unisyn's CDF CVR, which is not compatible with RCTab.
    if (cvrReport.GpUnit == null) {
      Logger.severe(
          "Field \"GPUnit\" missing from CDF CVR file! "
              + "This is common with older, unsupported formats.");
      throw new CvrParseException();
    }

    // These fields are also required, but we are not aware of any standard format where
    // they would be missing, so we group all these checks together.
    ArrayList<String> missingFields = new ArrayList<>();
    if (cvrReport.CVR == null) {
      missingFields.add("CVR");
    }
    if (cvrReport.Election == null) {
      missingFields.add("Election");
    }
    if (cvrReport.ReportGeneratingDeviceIds == null) {
      missingFields.add("ReportGeneratingDeviceIds");
    }
    if (cvrReport.ReportingDevice == null) {
      missingFields.add("ReportingDevice");
    }
    if (cvrReport.Party == null) {
      missingFields.add("Party");
    }

    if (!missingFields.isEmpty()) {
      Logger.severe("Required fields are missing from CDF CVR file: " + missingFields);
      throw new CvrParseException();
    }
  }

  void parseXml(List<CastVoteRecord> castVoteRecords)
      throws CvrParseException, IOException {
    // load XML
    XmlMapper xmlMapper = new XmlMapper();
    xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    try (FileInputStream inputStream = new FileInputStream(cvrPath)) {
      CastVoteRecordReport cvrReport = xmlMapper.readValue(inputStream, CastVoteRecordReport.class);
      inputStream.close();

      checkForEmptyFields(cvrReport);

      // Parse static election data:

      // Find the Contest we are tabulating:
      // Note: Contest is different from CVRContest objects which appear in CVRSnapshots
      Contest contestToTabulate = null;
      for (Election election : cvrReport.Election) {
        for (Contest contest : election.Contest) {
          if (contest.Name.equals(source.getContestId())) {
            contestToTabulate = contest;
            break;
          }
        }
      }

      if (contestToTabulate == null) {
        Logger.severe("Contest \"%s\" from config file not found!", source.getContestId());
        throw new CvrParseException();
      }

      boolean hasError = false;

      // build a map of Candidates
      HashMap<String, Candidate> candidateById = new HashMap<>();
      for (Election election : cvrReport.Election) {
        for (Candidate candidate : election.Candidate) {
          hasError |= putIfUnique(candidateById, candidate.ObjectId, candidate, "Candidate");
        }
      }

      // ContestSelections
      HashMap<String, ContestSelection> contestSelectionById = new HashMap<>();
      for (ContestSelection contestSelection : contestToTabulate.ContestSelection) {
        hasError |=
            putIfUnique(
                contestSelectionById,
                contestSelection.ObjectId,
                contestSelection,
                "Contest Selection");
      }

      // build a map of GpUnits (aka precinct or district)
      HashMap<String, GpUnit> gpUnitById = new HashMap<>();
      for (GpUnit gpUnit : cvrReport.GpUnit) {
        hasError |= putIfUnique(gpUnitById, gpUnit.ObjectId, gpUnit, "GPUnit");
      }

      if (hasError) {
        Logger.severe("One or more keys were not unique.");
        throw new CvrParseException();
      }

      // process the Cvrs
      int cvrIndex = 0;
      String fileName = new File(cvrPath).getName();
      for (CVR cvr : cvrReport.CVR) {
        CVRContest contest = getCvrContestXml(cvr, contestToTabulate);
        if (contest == null) {
          // the CVR does not contain any votes for this contest
          continue;
        }
        List<Pair<Integer, String>> rankings = new ArrayList<>();
        // parse CVRContestSelections into rankings
        // they will be null for an undervote
        if (contest.CVRContestSelection != null) {
          for (CVRContestSelection cvrContestSelection : contest.CVRContestSelection) {
            if (cvrContestSelection.Status != null
                && cvrContestSelection.Status.equals("needs-adjudication")) {
              Logger.info("Contest Selection needs adjudication. Skipping.");
              continue;
            }
            String contestSelectionId = cvrContestSelection.ContestSelectionId;
            ContestSelection contestSelection = contestSelectionById.get(contestSelectionId);
            if (contestSelection == null) {
              Logger.severe("ContestSelection \"%s\" from CVR not found!", contestSelectionId);
              throw new CvrParseException();
            }
            String candidateName;
            // check for declared write-in:
            if (contestSelection.IsWriteIn != null
                && contestSelection.IsWriteIn.equals(BOOLEAN_TRUE)) {
              candidateName = Tabulator.UNDECLARED_WRITE_IN_OUTPUT_LABEL;
            } else {
              // validate candidate Ids:
              // CDF allows multiple candidate Ids to support party ticket voting options
              // but in practice this is always a single candidate id
              if (contestSelection.CandidateIds == null
                  || contestSelection.CandidateIds.length == 0) {
                Logger.severe(
                    "CandidateSelection \"%s\" has no CandidateIds!", contestSelection.ObjectId);
                throw new CvrParseException();
              }
              if (contestSelection.CandidateIds.length > 1) {
                Logger.warning(
                    "CandidateSelection \"%s\" has multiple CandidateIds. "
                        + "Only the first one will be processed.",
                    contestSelection.ObjectId);
              }

              Candidate candidate = candidateById.get(contestSelection.CandidateIds[0]);
              if (candidate == null) {
                Logger.severe(
                    "CandidateId \"%s\" from ContestSelectionId \"%s\" not found!",
                    contestSelection.CandidateIds[0], contestSelection.ObjectId);
                throw new CvrParseException();
              }
              candidateName = candidate.Name;
              if (candidateName.equals(source.getOvervoteLabel())) {
                candidateName = Tabulator.EXPLICIT_OVERVOTE_LABEL;
              }
            }
            parseRankings(cvr, contest, rankings, cvrContestSelection, candidateName);
          }
        }

        // Extract GPUnit if provided
        String precinctId = null;
        if (cvr.BallotStyleUnitId != null) {
          GpUnit unit = gpUnitById.get(cvr.BallotStyleUnitId);
          if (unit == null) {
            Logger.severe(
                "GpUnit \"%s\" for CVR \"%s\" not found!", cvr.BallotStyleUnitId, cvr.UniqueId);
            throw new CvrParseException();
          }
          precinctId = unit.Name;
        }

        String computedCastVoteRecordId = String.format("%s(%d)", fileName, ++cvrIndex);
        // create the new CastVoteRecord
        CastVoteRecord newRecord =
            new CastVoteRecord(computedCastVoteRecordId, cvr.UniqueId, precinctId, rankings);
        castVoteRecords.add(newRecord);

        // provide some user feedback on the CVR count
        if (castVoteRecords.size() % 50000 == 0) {
          Logger.info("Parsed %d cast vote records.", castVoteRecords.size());
        }
      }
    }
  }

  private void parseRankings(
      CVR cvr,
      CVRContest contest,
      List<Pair<Integer, String>> rankings,
      CVRContestSelection cvrContestSelection,
      String candidateId)
      throws CvrParseException {
    // parse the selected rankings for the specified contest into the provided rankings list
    if (cvrContestSelection.Rank == null) {
      for (SelectionPosition selectionPosition : cvrContestSelection.SelectionPosition) {
        if (selectionPosition.CVRWriteIn != null) {
          candidateId = Tabulator.UNDECLARED_WRITE_IN_OUTPUT_LABEL;
        }
        // ignore if no indication is present (NIST 1500-103 section 3.4.2)
        if (selectionPosition.HasIndication != null
            && selectionPosition.HasIndication.equals(STATUS_NO)) {
          continue;
        }
        // skip if not allocable
        if (selectionPosition.IsAllocable.equals(STATUS_NO)) {
          continue;
        }
        if (selectionPosition.Rank == null) {
          Logger.severe(
              "No Rank found on CVR \"%s\" Contest \"%s\"!", cvr.UniqueId, contest.ContestId);
          throw new CvrParseException();
        }
        Integer rank = Integer.parseInt(selectionPosition.Rank);
        rankings.add(new Pair<>(rank, candidateId));
      }
    } else {
      Integer rank = Integer.parseInt(cvrContestSelection.Rank);
      rankings.add(new Pair<>(rank, candidateId));
    }
  }

  // parse cdf json CastVoteRecordReport into CastVoteRecords and append them to input list
  void parseJson(List<CastVoteRecord> castVoteRecords)
      throws CvrParseException {

    // static election data
    HashMap<Object, Object> candidates = new HashMap<>();
    HashMap<Object, Object> gpUnits = new HashMap<>();
    HashMap<Object, Object> contestSelections = new HashMap<>();
    HashMap contestToTabulate = null;
    HashMap json = JsonParser.readFromFile(cvrPath, HashMap.class);

    boolean hasError = false;

    // GpUnits
    ArrayList gpUnitArray = (ArrayList) json.get("GpUnit");
    for (Object gpUnitObject : gpUnitArray) {
      HashMap gpUnit = (HashMap) gpUnitObject;
      String gpUnitId = (String) gpUnit.get("@id");
      hasError |= putIfUnique(gpUnits, gpUnitId, gpUnit, "GPUnits");
    }

    // Elections
    ArrayList electionArray = (ArrayList) json.get("Election");
    for (Object electionObject : electionArray) {
      HashMap election = (HashMap) electionObject;

      // Candidates
      ArrayList candidatesArray = (ArrayList) election.get("Candidate");
      for (Object candidateObject : candidatesArray) {
        HashMap candidate = (HashMap) candidateObject;
        String candidateId = (String) candidate.get("@id");
        hasError |= putIfUnique(candidates, candidateId, candidate, "Candidate");
      }

      // Find contest to be tabulated
      ArrayList contestArray = (ArrayList) election.get("Contest");
      for (Object contestObject : contestArray) {
        HashMap contest = (HashMap) contestObject;
        String contestName = (String) contest.get("Name");
        if (contestName.equals(source.getContestId())) {
          contestToTabulate = contest;
          break;
        }
      }
    }
    if (contestToTabulate == null) {
      Logger.severe("Contest \"%s\" from config file not found!", source.getContestId());
      throw new CvrParseException();
    }
    String contestToTabulateId = (String) contestToTabulate.get("@id");

    // ContestSelections
    ArrayList contestSelectionArray = (ArrayList) contestToTabulate.get("ContestSelection");
    for (Object contestSelectionObject : contestSelectionArray) {
      HashMap contestSelection = (HashMap) contestSelectionObject;
      String selectionObjectId = (String) contestSelection.get("@id");
      hasError |=
          putIfUnique(contestSelections, selectionObjectId, contestSelection, "Contest Selection");
    }

    if (hasError) {
      Logger.severe("One or more keys were not unique.");
      throw new CvrParseException();
    }

    // process Cvrs
    int cvrIndex = 0;
    String fileName = new File(cvrPath).getName();

    ArrayList cvrs = (ArrayList) json.get("CVR");
    for (Object cvrObject : cvrs) {
      HashMap cvr = (HashMap) cvrObject;
      HashMap cvrContest = getCvrContestJson(cvr, contestToTabulateId);

      // parse selections
      List<Pair<Integer, String>> rankings = new ArrayList<>();
      ArrayList cvrContestSelections = (ArrayList) cvrContest.get("CVRContestSelection");
      for (Object cvrContestSelectionObject : cvrContestSelections) {
        HashMap cvrContestSelection = (HashMap) cvrContestSelectionObject;
        String contestSelectionId = (String) cvrContestSelection.get("ContestSelectionId");
        if (!contestSelections.containsKey(contestSelectionId)) {
          Logger.severe("ContestSelection \"%s\" from CVR not found!", contestSelectionId);
          throw new CvrParseException();
        }
        HashMap contestSelection = (HashMap) contestSelections.get(contestSelectionId);
        String candidateName;
        if (contestSelection.containsKey("IsWriteIn")
            && contestSelection.get("IsWriteIn").equals(BOOLEAN_TRUE)) {
          // this is a write-in
          candidateName = Tabulator.UNDECLARED_WRITE_IN_OUTPUT_LABEL;
        } else {
          // lookup Candidate Name
          ArrayList candidateIds = (ArrayList) contestSelection.get("CandidateIds");
          if (candidateIds == null || candidateIds.size() == 0) {
            Logger.severe("CandidateSelection \"%s\" has no CandidateIds!", contestSelectionId);
            throw new CvrParseException();
          }
          if (candidateIds.size() > 1) {
            Logger.warning(
                "CandidateSelection \"%s\" has multiple CandidateIds. "
                    + "Only the first one will be processed.",
                contestSelectionId);
          }
          String candidateObjectId = (String) candidateIds.get(0);
          HashMap candidate = (HashMap) candidates.get(candidateObjectId);
          if (candidate == null) {
            Logger.severe(
                "Candidate ID \"%s\" in Contest Selection \"%s\" is not in the candidate list.",
                candidateObjectId, contestSelectionId);
            throw new CvrParseException();
          }
          candidateName = (String) candidate.get("Name");
          if (candidateName.equals(source.getOvervoteLabel())) {
            candidateName = Tabulator.EXPLICIT_OVERVOTE_LABEL;
          }
        }

        // parse rankings
        // rank may appear on the CVRContestSelection OR the SelectionPosition
        // this is an ambiguity in the nist spec
        if (cvrContestSelection.containsKey("Rank")) {
          Integer rank = (Integer) (cvrContestSelection.get("Rank"));
          rankings.add(new Pair<>(rank, candidateName));
        } else {
          // extract all the SelectionPositions (ranks) which this selection has been assigned
          ArrayList selectionPositions = (ArrayList) cvrContestSelection.get("SelectionPosition");
          for (Object selectionPositionObject : selectionPositions) {
            HashMap selectionPosition = (HashMap) selectionPositionObject;
            // WriteIn can be linked at the selection position level
            if (selectionPosition.containsKey("CVRWriteIn")) {
              candidateName = Tabulator.UNDECLARED_WRITE_IN_OUTPUT_LABEL;
            }
            // ignore if no indication is present (NIST 1500-103 section 3.4.2)
            if (selectionPosition.containsKey("HasIndication")
                && selectionPosition.get("HasIndication").equals(STATUS_NO)) {
              continue;
            }
            // skip if not allocable
            if (selectionPosition.containsKey("IsAllocable")
                && selectionPosition.get("IsAllocable").equals(STATUS_NO)) {
              continue;
            }
            // and finally the rank
            Integer rank = (Integer) selectionPosition.get("Rank");
            rankings.add(new Pair<>(rank, candidateName));
          }
        }
      } // for (Object cvrContestSelectionObject : cvrContestSelections) {

      // Extract GPUnit if provided
      String precinctId = null;
      if (cvr.containsKey("BallotStyleUnitId")) {
        String unitId = (String) cvr.get("BallotStyleUnitId");
        if (gpUnits.containsKey(unitId)) {
          HashMap unit = (HashMap) gpUnits.get(cvr.get("BallotStyleUnitId"));
          precinctId = (String) unit.get("Name");
        } else {
          Logger.severe("GpUnit \"%s\" not found!", unitId);
        }
      }

      String ballotId = (String) cvr.get("BallotPrePrintedId");
      String computedCastVoteRecordId = String.format("%s(%d)", fileName, ++cvrIndex);
      // create the new CastVoteRecord
      CastVoteRecord newRecord =
          new CastVoteRecord(computedCastVoteRecordId, ballotId, precinctId, rankings);
      castVoteRecords.add(newRecord);
      // provide some user feedback on the CVR count
      if (castVoteRecords.size() % 50000 == 0) {
        Logger.info("Parsed %d cast vote records.", castVoteRecords.size());
      }
    }
  }

  // The following classes are based on the NIST 1500-103 UML structure.
  // Many of the elements represented here will not be present on any particular implementation of
  // a Cdf Cvr report. Many of these elements are also irrelevant for tabulation purposes.
  // However, they are included here for completeness and to aid in interpreting the UML.
  // Note that fields identified as "boolean-like" can be (yes, no, 1, 0, or null)
  static class ContestSelection {

    @JacksonXmlProperty(isAttribute = true)
    String ObjectId;
    // type indicates the ContestSelection subclass
    @JacksonXmlProperty(isAttribute = true)
    String type;

    // CandidateSelection fields
    // CandidateIds is plural to support "party ticket" options which can include multiple
    // candidates. We do not support this yet.
    @JacksonXmlProperty()
    @JacksonXmlElementWrapper(useWrapping = false)
    String[] CandidateIds;
    // boolean-like
    @JacksonXmlProperty() String IsWriteIn;

    // PartySelection fields
    @JacksonXmlProperty() String[] PartyIds;

    // BallotMeasureSelection fields
    @JacksonXmlProperty() String Selection;
  }

  static class CVRWriteIn {

    @JacksonXmlProperty() String Text;
  }

  static class SelectionPosition {

    @JacksonXmlProperty()
    @JacksonXmlElementWrapper(useWrapping = false)
    Code[] Code;

    @JacksonXmlProperty() String FractionalVotes;
    @JacksonXmlProperty() String HasIndication;
    @JacksonXmlProperty() String IsAllocable;
    // boolean-like
    @JacksonXmlProperty() String IsGenerated;
    @JacksonXmlProperty() String MarkMetricValue;
    @JacksonXmlProperty() Integer NumberVotes;
    @JacksonXmlProperty() String Position;
    @JacksonXmlProperty() String Rank;
    @JacksonXmlProperty() String Status;
    @JacksonXmlProperty() String OtherStatus;

    @JacksonXmlProperty()
    @JacksonXmlElementWrapper(useWrapping = false)
    CVRWriteIn[] CVRWriteIn;
  }

  static class CVRContestSelection {

    @JacksonXmlProperty() Integer OptionPosition;
    @JacksonXmlProperty() String Rank;
    @JacksonXmlProperty() String Status;
    @JacksonXmlProperty() String OtherStatus;
    @JacksonXmlProperty() String TotalFractionalVotes;
    @JacksonXmlProperty() Integer TotalNumberVotes;
    @JacksonXmlProperty() String ContestSelectionId;

    @JacksonXmlProperty()
    @JacksonXmlElementWrapper(useWrapping = false)
    SelectionPosition[] SelectionPosition;
  }

  static class CVRContest {

    @JacksonXmlProperty() String ContestId;

    @JacksonXmlProperty()
    @JacksonXmlElementWrapper(useWrapping = false)
    CVRContestSelection[] CVRContestSelection;

    @JacksonXmlProperty() String Overvotes;
    @JacksonXmlProperty() String Selections;
    @JacksonXmlProperty() String Undervotes;
    @JacksonXmlProperty() String WriteIns;
  }

  static class CVRSnapshot {

    @JacksonXmlProperty(isAttribute = true)
    String ObjectId;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty()
    CVRContest[] CVRContest;

    @JacksonXmlProperty() String Status;
    @JacksonXmlProperty() String OtherStatus;
    @JacksonXmlProperty() String Type;
  }

  static class Party {

    @JacksonXmlProperty(isAttribute = true)
    String ObjectId;

    @JacksonXmlProperty() String Abbreviation;
    @JacksonXmlProperty() String Name;
  }

  static class ReportingDevice {

    @JacksonXmlProperty(isAttribute = true)
    String ObjectId;

    @JacksonXmlProperty() String Application;
    @JacksonXmlProperty() Code Code;
    @JacksonXmlProperty() String Manufacturer;
    @JacksonXmlProperty() String MarkMetricType;
    @JacksonXmlProperty() String Model;
    @JacksonXmlProperty() String Notes;
    @JacksonXmlProperty() String SerialNumber;
  }

  static class GpUnit {

    @JacksonXmlProperty(isAttribute = true)
    String ObjectId;

    @JacksonXmlProperty() Code Code;
    @JacksonXmlProperty() String Name;
    @JacksonXmlProperty() String Type;
    @JacksonXmlProperty() String OtherType;
    @JacksonXmlProperty() ReportingDevice[] ReportingDevice;
  }

  static class Code {

    @JacksonXmlProperty() String Type;
    @JacksonXmlProperty() String Value;
    @JacksonXmlProperty() String OtherType;
  }

  static class Candidate {

    @JacksonXmlProperty(isAttribute = true)
    String ObjectId;

    @JacksonXmlProperty() String Name;
    @JacksonXmlProperty() String PartyId;
    @JacksonXmlProperty() Code Code;
  }

  static class Contest {

    @JacksonXmlProperty(isAttribute = true)
    String ObjectId;

    @JacksonXmlProperty(isAttribute = true)
    String type;

    @JacksonXmlProperty() String Abbreviation;
    @JacksonXmlProperty() Code Code;
    @JacksonXmlProperty() String Name;
    @JacksonXmlProperty() String VoteVariation;
    @JacksonXmlProperty() String OtherVoteVariation;

    @JacksonXmlProperty()
    @JacksonXmlElementWrapper(useWrapping = false)
    ContestSelection[] ContestSelection;

    // CandidateContest fields
    @JacksonXmlProperty() Integer NumberElected;
    @JacksonXmlProperty() Integer VotesAllowed;
    @JacksonXmlProperty() String PrimaryPartyId;

    // RetentionContest fields
    @JacksonXmlProperty() String CandidateId;
  }

  static class Election {

    @JacksonXmlProperty(isAttribute = true)
    String ObjectId;

    @JacksonXmlProperty()
    @JacksonXmlElementWrapper(useWrapping = false)
    Candidate[] Candidate;

    @JacksonXmlProperty()
    @JacksonXmlElementWrapper(useWrapping = false)
    Contest[] Contest;

    @JacksonXmlProperty(isAttribute = true)
    String ElectionScopeId;

    @JacksonXmlProperty(isAttribute = true)
    String Name;
  }

  static class CVR {

    @JacksonXmlProperty() String ElectionId;
    // GpUnit
    @JacksonXmlProperty() String BallotStyleUnitId;
    // ReportingDevice
    @JacksonXmlProperty() String CreatingDeviceId;
    @JacksonXmlProperty() String PartyId;
    @JacksonXmlProperty() String CurrentSnapshotId;

    @JacksonXmlProperty()
    @JacksonXmlElementWrapper(useWrapping = false)
    CVRSnapshot[] CVRSnapshot;

    @JacksonXmlProperty() String BallotAuditId;
    @JacksonXmlProperty() String BallotPrePrintedId;
    @JacksonXmlProperty() String BatchSequenceId;
    @JacksonXmlProperty() String BallotSheetId;
    @JacksonXmlProperty() String BallotStyleId;
    @JacksonXmlProperty() String BatchId;
    @JacksonXmlProperty() String UniqueId;
  }

  // top-level cdf structure
  static class CastVoteRecordReport {

    @JacksonXmlProperty() String GeneratedDate;
    @JacksonXmlProperty() String Notes;
    @JacksonXmlProperty() String ReportType;
    @JacksonXmlProperty() String OtherReportType;
    @JacksonXmlProperty() String Version;

    // Cvr records
    @JacksonXmlProperty()
    @JacksonXmlElementWrapper(useWrapping = false)
    CVR[] CVR;

    // Cdf supports multiple Elections however we only tabulate the first one
    @JacksonXmlProperty()
    @JacksonXmlElementWrapper(useWrapping = false)
    Election[] Election;

    // Cdf supports multiple GpUnits at the report level
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty()
    GpUnit[] GpUnit;

    // all Party objects which may appear throughout the report
    @JacksonXmlProperty()
    @JacksonXmlElementWrapper(useWrapping = false)
    CommonDataFormatReader.Party[] Party;

    // report level ReportGeneratingDevices
    @JacksonXmlProperty()
    @JacksonXmlElementWrapper(useWrapping = false)
    String[] ReportGeneratingDeviceIds;

    // All Reporting Devices which may appear in this report
    @JacksonXmlProperty()
    @JacksonXmlElementWrapper(useWrapping = false)
    ReportingDevice[] ReportingDevice;
  }
}
