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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javafx.util.Pair;
import network.brightspots.rcv.CastVoteRecord.CvrParseException;
import network.brightspots.rcv.RawContestConfig.CvrSource;
import network.brightspots.rcv.TabulatorSession.UnrecognizedCandidatesException;

class CommonDataFormatReader {

  private final String STATUS_NEEDS_ADJUDICATION = "needs-adjudication";
  private final String STATUS_YES = "yes";
  private final String STATUS_NO = "no";
  private final String STATUS_UNKNOWN = "unknown";
  private final String BOOLEAN_TRUE = "true";
  private final String BOOLEAN_FALSE = "false";

  private final String filePath;
  private final ContestConfig config;
  private final CvrSource source;
  private Map<String, Integer> unrecognizedCandidateCounts = new HashMap<>();

  CommonDataFormatReader(String filePath, ContestConfig config, CvrSource source) {
    this.filePath = filePath;
    this.config = config;
    this.source = source;
  }
  
  // This method will extract candidate data from a CDF file for the contestID specified in
  // our CvrSource.  It is currently un-used but will be handy for automating
  // config file creation when we are ready to implement that.
  Map<String, String> getCandidates() throws CvrParseException {
    Map<String, String> candidates;
    if (filePath.endsWith(".xml")) {
      candidates = getCandidatesXml();
    } else if (filePath.endsWith(".json")) {
      candidates = getCandidatesJson();
    } else {
      Logger.log(Level.SEVERE,
          "Unexpected file extension: %s.  CDF source files must be .xml or .json", this.filePath);
      throw new CvrParseException();
    }
    return candidates;
  }

  Map<String, String> getCandidatesXml() throws CvrParseException {
    Map<String, String> candidates = new HashMap<>();
    try {
      XmlMapper xmlMapper = new XmlMapper();
      xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      FileInputStream inputStream = new FileInputStream(new File(filePath));
      CastVoteRecordReport cvrReport = xmlMapper.readValue(inputStream, CastVoteRecordReport.class);
      for (Contest contest : cvrReport.Election[0].Contest) {
        if (!contest.ObjectId.equals(source.getContestId())) {
          continue;
        }
        for (ContestSelection contestSelection : contest.ContestSelection) {
          String candidateId = contestSelection.ObjectId;
          // lookup the Candidate name
          String candidateName = null;
          for (Candidate candidate : cvrReport.Election[0].Candidate) {
            if (candidate.ObjectId.equals(candidateId)) {
              candidateName = candidate.Name;
              // fallback to Code.Value
              if (candidateName == null && candidate.Code != null && candidate.Code.Value != null) {
                candidateName = candidate.Code.Value;
              }
              break;
            }
          }
          if (candidateName == null) {
            Logger.log(Level.WARNING, "No name found for CandidateId: %s", candidateId);
            candidateName = candidateId;
          }
          candidates.put(candidateId, candidateName);
        }
      }
    } catch (Exception e) {
      Logger.log(Level.SEVERE, "Error parsing CDF data:\n%s", e.toString());
      throw new CvrParseException();
    }
    return candidates;
  }

  // returns map from candidate ID to name parsed from CDF election json
  Map<String, String> getCandidatesJson() {
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
          // filter by contest ID
          if (!contest.get("@id").equals(source.getContestId())) {
            continue;
          }
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
      Logger.log(Level.SEVERE, "Error parsing candidate data:\n%s", e.toString());
    }
    return candidates;
  }

  // parse a list of contest selection rankings from a NIST "Snapshot" HashMap
  private List<Pair<Integer, String>> parseRankingsFromSnapshot(HashMap snapshot,
      Map<String, String> candidates) {
    List<Pair<Integer, String>> rankings = new ArrayList<>();
    // at the top level is a list of contests each of which contains selections
    ArrayList cvrContests = (ArrayList) snapshot.get("CVRContest");
    for (Object contestObject : cvrContests) {
      HashMap cvrContest = (HashMap) contestObject;
      // filter by contest ID
      if (!cvrContest.get("ContestId").equals(source.getContestId())) {
        continue;
      }
      // each contest contains contestSelections
      ArrayList contestSelections = (ArrayList) cvrContest.get("CVRContestSelection");
      for (Object contestSelectionObject : contestSelections) {
        HashMap contestSelection = (HashMap) contestSelectionObject;
        // selectionId is the candidate/contest ID for this selection position
        String selectionId = (String) contestSelection.get("ContestSelectionId");
        if (selectionId.equals(config.getOvervoteLabel())) {
          selectionId = Tabulator.EXPLICIT_OVERVOTE_LABEL;
        } else if (!candidates.containsKey(selectionId)) {
          unrecognizedCandidateCounts.merge(selectionId, 1, Integer::sum);
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

  // helper to extract the required CVRContest from the input CVR
  CVRContest getCvrContest(CVR cvr, Contest contestToTabulate) {
    CVRContest cvrContestToTabulate = null;
    // find current snapshot
    CVRSnapshot currentSnapshot = null;
    for (CVRSnapshot cvrSnapshot : cvr.CVRSnapshot) {
      if (cvrSnapshot.ObjectId.equals(cvr.CurrentSnapshotId)) {
        currentSnapshot = cvrSnapshot;
        break;
      }
    }
    // find CVRContest which matches the Contest we are tabulating
    for (CVRContest cvrContest : currentSnapshot.CVRContest) {
      if (cvrContest.ContestId.equals(contestToTabulate.ObjectId)) {
        cvrContestToTabulate = cvrContest;
        break;
      }
    }
    return cvrContestToTabulate;
  }

  void parseXML(List<CastVoteRecord> castVoteRecords) {
    try {
      // parse XML into memory
      XmlMapper xmlMapper = new XmlMapper();
      xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      FileInputStream inputStream = new FileInputStream(new File(filePath));
      CastVoteRecordReport cvrReport = xmlMapper.readValue(inputStream, CastVoteRecordReport.class);
      if (cvrReport.Election.length > 1) {
        Logger.warning("Multiple Election objects found.  Only the first one will be processed.");
      }
      // build a map from CandidateId to Candidate object
      HashMap<String, Candidate> candidateById = new HashMap<>();
      for (Candidate candidate : cvrReport.Election[0].Candidate) {
        candidateById.put(candidate.ObjectId, candidate);
      }
      // extract the contest for tabulation
      Contest contestToTabulate = null;
      for (Contest contest : cvrReport.Election[0].Contest) {
        if (contest.Name.equals(source.getContestId())) {
          contestToTabulate = contest;
          break;
        }
      }
      if (contestToTabulate == null) {
        Logger.severe("Contest \"%s\" from config file not found!", source.getContestId());
        throw new CvrParseException();
      }

      // build a map from contestSelectionId to ContestSelection
      HashMap<String, ContestSelection> contestSelectionById = new HashMap<>();
      for (ContestSelection contestSelection : contestToTabulate.ContestSelection) {
        if (contestSelection.CandidateIds.length > 1) {
          Logger.warning(
              "Multiple CandidateIds found in ContestSelection \"%s\". "
                  + " Only the first one will be processed.",
              contestSelection.ObjectId);
        }
        contestSelectionById.put(contestSelection.ObjectId, contestSelection);
      }

      // process the Cvrs
      for (CVR cvr : cvrReport.CVR) {
        CVRContest contest = getCvrContest(cvr, contestToTabulate);
        if (contest == null) {
          // the CVR does not contain any votes for this contest
          continue;
        }
        List<Pair<Integer, String>> rankings = new ArrayList<>();
        // parse CVRContestSelections into rankings
        // they will be null for an undervote
        if (contest.CVRContestSelection != null) {
          for (CVRContestSelection cvrContestSelection : contest.CVRContestSelection) {
            if (cvrContestSelection.Status != null && cvrContestSelection.Status
                .equals(STATUS_NEEDS_ADJUDICATION)) {
              Logger.info("Contest Selection needs adjudication.  Skipping.");
              continue;
            }
            String contestSelectionId = cvrContestSelection.ContestSelectionId;
            ContestSelection contestSelection = contestSelectionById
                .get(contestSelectionId);
            if (contestSelection == null) {
              Logger.severe("ContestSelection \"%s\" from CVR not found!", contestSelectionId);
              throw new CvrParseException();
            }
            String candidateId;
            // check for declared write-in:
            if (contestSelection.IsWriteIn != null && contestSelection.IsWriteIn
                .equals(BOOLEAN_TRUE)) {
              candidateId = this.config.getUndeclaredWriteInLabel();
            } else {
              Candidate candidate = candidateById
                  .get(contestSelection.CandidateIds[0]);
              if (candidate == null) {
                Logger.severe("CandidateId \"%s\" from ContestSelectionId \"%s\" not found!",
                    contestSelection.CandidateIds[0], contestSelection.ObjectId);
                throw new CvrParseException();
              }
              candidateId = candidate.Name;
              if (candidateId.equals(config.getOvervoteLabel())) {
                candidateId = Tabulator.EXPLICIT_OVERVOTE_LABEL;
              }
              if (!config.getCandidateCodeList().contains(candidateId)) {
                Logger
                    .severe(
                        "Contest Selection CandidateId: \"%s\" from CVR is not in the config file!",
                        candidateId);
                throw new CvrParseException();
              }
            }

            if (cvrContestSelection.Rank == null) {
              for (SelectionPosition selectionPosition : cvrContestSelection.SelectionPosition) {
                if (selectionPosition.CVRWriteIn != null) {
                  candidateId = this.config.getUndeclaredWriteInLabel();
                }
                // ignore if no indication is present (NIST 1500-103 section 3.4.2)
                if (selectionPosition.HasIndication != null && selectionPosition.HasIndication
                    .equals(STATUS_NO)) {
                  continue;
                }
                // skip if not allocable
                if (selectionPosition.IsAllocable.equals(STATUS_NO)) {
                  continue;
                }
                if (selectionPosition.Rank == null) {
                  Logger
                      .severe("No Rank found on CVR %s Contest %s!", cvr.UniqueId,
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
        }

        // create the new CastVoteRecord
        CastVoteRecord newRecord =
            new CastVoteRecord(null, /* computed Id */
                cvr.UniqueId, /* supplied Id */
                null, /* precinct */
                null, /* full Cvr data */
                rankings);
        castVoteRecords.add(newRecord);

        // provide some user feedback on the CVR count
        if (castVoteRecords.size() % 50000 == 0) {
          Logger.log(Level.INFO, "Parsed %d cast vote records.", castVoteRecords.size());
        }
      }
    } catch (Exception e) {
      Logger.log(Level.SEVERE, "Error parsing CDF data:\n%s", e.toString());
    }
  }

  void parseCvrFile(List<CastVoteRecord> castVoteRecords)
      throws CvrParseException, UnrecognizedCandidatesException {
    if (filePath.endsWith(".xml")) {
      parseXML(castVoteRecords);
    } else if (filePath.endsWith(".json")) {
      parseJson(castVoteRecords);
    } else {
      Logger.log(Level.SEVERE,
          "Unexpected file extension: %s.  CDF source files must be .xml or .json", this.filePath);
      throw new CvrParseException();
    }
  }

  void parseJson(List<CastVoteRecord> castVoteRecords) throws UnrecognizedCandidatesException {
    // cvrIndex and fileName are used to generate IDs for cvrs
    int cvrIndex = 0;
    String fileName = new File(filePath).getName();
    try {
      HashMap json = JsonParser.readFromFile(filePath, HashMap.class);
      Map<String, String> candidates = getCandidates();

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
          List<Pair<Integer, String>> rankings = parseRankingsFromSnapshot(snapshot, candidates);
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
      Logger.log(Level.SEVERE, "Error parsing CDF data:\n%s", e.toString());
    }

    if (unrecognizedCandidateCounts.size() > 0) {
      throw new UnrecognizedCandidatesException(unrecognizedCandidateCounts);
    }
  }

  // The following classes are based on the NIST 1500-103 UML structure.
  // Many of the elements represented here will not be present on any particular implementation of
  // a Cdf Cvr report.  Many of these elements are also irrelevant for tabulation purposes.
  // However they are included here for completeness and to aid in interpreting the UML.
  // Note that fields identified as "boolean-like" can be (yes, no, 1, 0, or null)
  static class ContestSelection {

    @JacksonXmlProperty(isAttribute = true)
    String ObjectId;
    // type indicates the ContestSelection subclass
    @JacksonXmlProperty(isAttribute = true)
    String type;

    // CandidateSelection fields
    // CandidateIds is plural to support "party ticket" options which can include multiple
    // candidates.  We do not support this yet.
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
