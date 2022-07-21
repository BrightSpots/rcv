/*
 * RCTab
 * Copyright (c) 2017-2022 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose:
 * Helper class to read and parse a Common Data Format file into cast vote record objects
 * and candidate names.
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
import java.util.Map;
import java.util.Objects;
import javafx.util.Pair;
import network.brightspots.rcv.CastVoteRecord.CvrParseException;
import network.brightspots.rcv.TabulatorSession.UnrecognizedCandidatesException;

@SuppressWarnings({"rawtypes", "unused", "RedundantSuppression"})
class CommonDataFormatReader {

  private static final String STATUS_NO = "no";
  private static final String BOOLEAN_TRUE = "true";

  private final String filePath;
  private final ContestConfig config;
  private final String contestId;
  private final String overvoteLabel;
  private final Map<String, Integer> unrecognizedCandidateCounts = new HashMap<>();

  CommonDataFormatReader(
      String filePath, ContestConfig config, String contestId, String overvoteLabel) {
    this.filePath = filePath;
    this.config = config;
    this.contestId = contestId;
    this.overvoteLabel = overvoteLabel;
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

  void parseCvrFile(List<CastVoteRecord> castVoteRecords)
      throws UnrecognizedCandidatesException, IOException, CvrParseException {
    if (filePath.endsWith(".xml")) {
      parseXml(castVoteRecords);
    } else if (filePath.endsWith(".json")) {
      parseJson(castVoteRecords);
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

  void parseXml(List<CastVoteRecord> castVoteRecords)
      throws CvrParseException, IOException, UnrecognizedCandidatesException {
    // load XML
    XmlMapper xmlMapper = new XmlMapper();
    xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    try (FileInputStream inputStream = new FileInputStream(filePath)) {
      CastVoteRecordReport cvrReport = xmlMapper.readValue(inputStream, CastVoteRecordReport.class);
      inputStream.close();

      // Parse static election data:

      // Find the Contest we are tabulating:
      // Note: Contest is different from CVRContest objects which appear in CVRSnapshots
      Contest contestToTabulate = null;
      for (Election election : cvrReport.Election) {
        for (Contest contest : election.Contest) {
          if (contest.Name.equals(this.contestId)) {
            contestToTabulate = contest;
            break;
          }
        }
      }

      if (contestToTabulate == null) {
        Logger.severe("Contest \"%s\" from config file not found!", this.contestId);
        throw new CvrParseException();
      }

      // build a map of Candidates
      HashMap<String, Candidate> candidateById = new HashMap<>();
      for (Election election : cvrReport.Election) {
        for (Candidate candidate : election.Candidate) {
          candidateById.put(candidate.ObjectId, candidate);
        }
      }

      // ContestSelections
      HashMap<String, ContestSelection> contestSelectionById = new HashMap<>();
      for (ContestSelection contestSelection : contestToTabulate.ContestSelection) {
        contestSelectionById.put(contestSelection.ObjectId, contestSelection);
      }

      // build a map of GpUnits (aka precinct or district)
      HashMap<String, GpUnit> gpUnitById = new HashMap<>();
      for (GpUnit gpUnit : cvrReport.GpUnit) {
        gpUnitById.put(gpUnit.ObjectId, gpUnit);
      }

      // process the Cvrs
      int cvrIndex = 0;
      String fileName = new File(filePath).getName();
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
            String candidateId;
            // check for declared write-in:
            if (contestSelection.IsWriteIn != null
                && contestSelection.IsWriteIn.equals(BOOLEAN_TRUE)) {
              candidateId = Tabulator.UNDECLARED_WRITE_IN_OUTPUT_LABEL;
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
              candidateId = candidate.Name;
              if (candidateId.equals(overvoteLabel)) {
                candidateId = Tabulator.EXPLICIT_OVERVOTE_LABEL;
              } else if (!config.getCandidateCodeList().contains(candidateId)) {
                Logger.severe("Unrecognized candidate found in CVR: %s", candidateId);
                unrecognizedCandidateCounts.merge(candidateId, 1, Integer::sum);
              }
            }
            parseRankings(cvr, contest, rankings, cvrContestSelection, candidateId);
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
            new CastVoteRecord(computedCastVoteRecordId, cvr.UniqueId, precinctId, null, rankings);
        castVoteRecords.add(newRecord);

        // provide some user feedback on the CVR count
        if (castVoteRecords.size() % 50000 == 0) {
          Logger.info("Parsed %d cast vote records.", castVoteRecords.size());
        }
      }
      if (unrecognizedCandidateCounts.size() > 0) {
        throw new UnrecognizedCandidatesException(unrecognizedCandidateCounts);
      }
    }
  }

  private void parseRankings(CVR cvr, CVRContest contest, List<Pair<Integer, String>> rankings,
      CVRContestSelection cvrContestSelection, String candidateId) throws CvrParseException {
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
              "No Rank found on CVR \"%s\" Contest \"%s\"!", cvr.UniqueId,
              contest.ContestId);
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
      throws CvrParseException, UnrecognizedCandidatesException {

    // static election data
    HashMap<Object, Object> candidates = new HashMap<>();
    HashMap<Object, Object> gpuUnits = new HashMap<>();
    HashMap<Object, Object> contestSelections = new HashMap<>();
    HashMap contestToTabulate = null;
    HashMap json = JsonParser.readFromFile(filePath, HashMap.class);

    // GpUnits
    ArrayList gpUnitArray = (ArrayList) json.get("GpUnit");
    for (Object gpUnitObject : gpUnitArray) {
      HashMap gpUnit = (HashMap) gpUnitObject;
      String gpUnitId = (String) gpUnit.get("@id");
      gpuUnits.put(gpUnitId, gpUnit);
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
        candidates.put(candidateId, candidate);
      }

      // Find contest to be tabulated
      ArrayList contestArray = (ArrayList) election.get("Contest");
      for (Object contestObject : contestArray) {
        HashMap contest = (HashMap) contestObject;
        String contestName = (String) contest.get("Name");
        if (contestName.equals(this.contestId)) {
          contestToTabulate = contest;
          break;
        }
      }
    }
    if (contestToTabulate == null) {
      Logger.severe("Contest \"%s\" from config file not found!", this.contestId);
      throw new CvrParseException();
    }
    String contestToTabulateId = (String) contestToTabulate.get("@id");

    // ContestSelections
    ArrayList contestSelectionArray = (ArrayList) contestToTabulate.get("ContestSelection");
    for (Object contestSelectionObject : contestSelectionArray) {
      HashMap contestSelection = (HashMap) contestSelectionObject;
      String selectionObjectId = (String) contestSelection.get("@id");
      contestSelections.put(selectionObjectId, contestSelection);
    }

    // process Cvrs
    int cvrIndex = 0;
    String fileName = new File(filePath).getName();

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
        String candidateId;
        if (contestSelection.containsKey("IsWriteIn")
            && contestSelection.get("IsWriteIn").equals(BOOLEAN_TRUE)) {
          // this is a write-in
          candidateId = Tabulator.UNDECLARED_WRITE_IN_OUTPUT_LABEL;
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
          candidateId = (String) candidate.get("Name");
          if (candidateId.equals(overvoteLabel)) {
            candidateId = Tabulator.EXPLICIT_OVERVOTE_LABEL;
          } else if (!this.config.getCandidateCodeList().contains(candidateId)) {
            Logger.severe("Unrecognized candidate found in CVR: %s", candidateId);
            unrecognizedCandidateCounts.merge(candidateId, 1, Integer::sum);
          }
        }

        // parse rankings
        // rank may appear on the CVRContestSelection OR the SelectionPosition
        // this is an ambiguity in the nist spec
        if (cvrContestSelection.containsKey("Rank")) {
          Integer rank = (Integer) (cvrContestSelection.get("Rank"));
          rankings.add(new Pair<>(rank, candidateId));
        } else {
          // extract all the SelectionPositions (ranks) which this selection has been assigned
          ArrayList selectionPositions = (ArrayList) cvrContestSelection.get("SelectionPosition");
          for (Object selectionPositionObject : selectionPositions) {
            HashMap selectionPosition = (HashMap) selectionPositionObject;
            // WriteIn can be linked at the selection position level
            if (selectionPosition.containsKey("CVRWriteIn")) {
              candidateId = Tabulator.UNDECLARED_WRITE_IN_OUTPUT_LABEL;
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
            rankings.add(new Pair<>(rank, candidateId));
          }
        }
      } // for (Object cvrContestSelectionObject : cvrContestSelections) {

      // Extract GPUnit if provided
      String precinctId = null;
      if (cvr.containsKey("BallotStyleUnitId")) {
        String unitId = (String) cvr.get("BallotStyleUnitId");
        if (gpuUnits.containsKey(unitId)) {
          HashMap unit = (HashMap) gpuUnits.get(cvr.get("BallotStyleUnitId"));
          precinctId = (String) unit.get("Name");
        } else {
          Logger.severe("GpUnit \"%s\" not found!", unitId);
        }
      }

      String ballotId = (String) cvr.get("BallotPrePrintedId");
      String computedCastVoteRecordId = String.format("%s(%d)", fileName, ++cvrIndex);
      // create the new CastVoteRecord
      CastVoteRecord newRecord =
          new CastVoteRecord(computedCastVoteRecordId, ballotId, precinctId, null, rankings);
      castVoteRecords.add(newRecord);
      // provide some user feedback on the CVR count
      if (castVoteRecords.size() % 50000 == 0) {
        Logger.info("Parsed %d cast vote records.", castVoteRecords.size());
      }
    } // for (Object cvr : cvrs) {

    if (unrecognizedCandidateCounts.size() > 0) {
      throw new UnrecognizedCandidatesException(unrecognizedCandidateCounts);
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
    @JacksonXmlProperty()
    String IsWriteIn;

    // PartySelection fields
    @JacksonXmlProperty()
    String[] PartyIds;

    // BallotMeasureSelection fields
    @JacksonXmlProperty()
    String Selection;
  }

  static class CVRWriteIn {

    @JacksonXmlProperty()
    String Text;
  }

  static class SelectionPosition {

    @JacksonXmlProperty()
    @JacksonXmlElementWrapper(useWrapping = false)
    Code[] Code;

    @JacksonXmlProperty()
    String FractionalVotes;
    @JacksonXmlProperty()
    String HasIndication;
    @JacksonXmlProperty()
    String IsAllocable;
    // boolean-like
    @JacksonXmlProperty()
    String IsGenerated;
    @JacksonXmlProperty()
    String MarkMetricValue;
    @JacksonXmlProperty()
    Integer NumberVotes;
    @JacksonXmlProperty()
    String Position;
    @JacksonXmlProperty()
    String Rank;
    @JacksonXmlProperty()
    String Status;
    @JacksonXmlProperty()
    String OtherStatus;

    @JacksonXmlProperty()
    @JacksonXmlElementWrapper(useWrapping = false)
    CVRWriteIn[] CVRWriteIn;
  }

  static class CVRContestSelection {

    @JacksonXmlProperty()
    Integer OptionPosition;
    @JacksonXmlProperty()
    String Rank;
    @JacksonXmlProperty()
    String Status;
    @JacksonXmlProperty()
    String OtherStatus;
    @JacksonXmlProperty()
    String TotalFractionalVotes;
    @JacksonXmlProperty()
    Integer TotalNumberVotes;
    @JacksonXmlProperty()
    String ContestSelectionId;

    @JacksonXmlProperty()
    @JacksonXmlElementWrapper(useWrapping = false)
    SelectionPosition[] SelectionPosition;
  }

  static class CVRContest {

    @JacksonXmlProperty()
    String ContestId;

    @JacksonXmlProperty()
    @JacksonXmlElementWrapper(useWrapping = false)
    CVRContestSelection[] CVRContestSelection;

    @JacksonXmlProperty()
    String Overvotes;
    @JacksonXmlProperty()
    String Selections;
    @JacksonXmlProperty()
    String Undervotes;
    @JacksonXmlProperty()
    String WriteIns;
  }

  static class CVRSnapshot {

    @JacksonXmlProperty(isAttribute = true)
    String ObjectId;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty()
    CVRContest[] CVRContest;

    @JacksonXmlProperty()
    String Status;
    @JacksonXmlProperty()
    String OtherStatus;
    @JacksonXmlProperty()
    String Type;
  }

  static class Party {

    @JacksonXmlProperty(isAttribute = true)
    String ObjectId;

    @JacksonXmlProperty()
    String Abbreviation;
    @JacksonXmlProperty()
    String Name;
  }

  static class ReportingDevice {

    @JacksonXmlProperty(isAttribute = true)
    String ObjectId;

    @JacksonXmlProperty()
    String Application;
    @JacksonXmlProperty()
    Code Code;
    @JacksonXmlProperty()
    String Manufacturer;
    @JacksonXmlProperty()
    String MarkMetricType;
    @JacksonXmlProperty()
    String Model;
    @JacksonXmlProperty()
    String Notes;
    @JacksonXmlProperty()
    String SerialNumber;
  }

  static class GpUnit {

    @JacksonXmlProperty(isAttribute = true)
    String ObjectId;

    @JacksonXmlProperty()
    Code Code;
    @JacksonXmlProperty()
    String Name;
    @JacksonXmlProperty()
    String Type;
    @JacksonXmlProperty()
    String OtherType;
    @JacksonXmlProperty()
    ReportingDevice[] ReportingDevice;
  }

  static class Code {

    @JacksonXmlProperty()
    String Type;
    @JacksonXmlProperty()
    String Value;
    @JacksonXmlProperty()
    String OtherType;
  }

  static class Candidate {

    @JacksonXmlProperty(isAttribute = true)
    String ObjectId;

    @JacksonXmlProperty()
    String Name;
    @JacksonXmlProperty()
    String PartyId;
    @JacksonXmlProperty()
    Code Code;
  }

  static class Contest {

    @JacksonXmlProperty(isAttribute = true)
    String ObjectId;

    @JacksonXmlProperty(isAttribute = true)
    String type;

    @JacksonXmlProperty()
    String Abbreviation;
    @JacksonXmlProperty()
    Code Code;
    @JacksonXmlProperty()
    String Name;
    @JacksonXmlProperty()
    String VoteVariation;
    @JacksonXmlProperty()
    String OtherVoteVariation;

    @JacksonXmlProperty()
    @JacksonXmlElementWrapper(useWrapping = false)
    ContestSelection[] ContestSelection;

    // CandidateContest fields
    @JacksonXmlProperty()
    Integer NumberElected;
    @JacksonXmlProperty()
    Integer VotesAllowed;
    @JacksonXmlProperty()
    String PrimaryPartyId;

    // RetentionContest fields
    @JacksonXmlProperty()
    String CandidateId;
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

    @JacksonXmlProperty()
    String ElectionId;
    // GpUnit
    @JacksonXmlProperty()
    String BallotStyleUnitId;
    // ReportingDevice
    @JacksonXmlProperty()
    String CreatingDeviceId;
    @JacksonXmlProperty()
    String PartyId;
    @JacksonXmlProperty()
    String CurrentSnapshotId;

    @JacksonXmlProperty()
    @JacksonXmlElementWrapper(useWrapping = false)
    CVRSnapshot[] CVRSnapshot;

    @JacksonXmlProperty()
    String BallotAuditId;
    @JacksonXmlProperty()
    String BallotPrePrintedId;
    @JacksonXmlProperty()
    String BatchSequenceId;
    @JacksonXmlProperty()
    String BallotSheetId;
    @JacksonXmlProperty()
    String BallotStyleId;
    @JacksonXmlProperty()
    String BatchId;
    @JacksonXmlProperty()
    String UniqueId;
  }

  // top-level cdf structure
  static class CastVoteRecordReport {

    @JacksonXmlProperty()
    String GeneratedDate;
    @JacksonXmlProperty()
    String Notes;
    @JacksonXmlProperty()
    String ReportType;
    @JacksonXmlProperty()
    String OtherReportType;
    @JacksonXmlProperty()
    String Version;

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
