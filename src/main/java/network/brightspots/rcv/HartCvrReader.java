/*
 * RCTab
 * Copyright (c) 2017-2022 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: Read and parse Hart election data for a contest into CastVoteRecord objects.
 * Design: Hart uses an xml file per cvr to store cvr data.  This class uses Jackson
 * XmlMapper to read these files into memory and parse the selections.
 * Conditions: used when reading Hart election data
 * Version history: version 1.0
 * Complete revision history is available at: https://github.com/BrightSpots/rcv
 */

package network.brightspots.rcv;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.util.Pair;
import network.brightspots.rcv.TabulatorSession.UnrecognizedCandidatesException;

class HartCvrReader {

  private final String cvrPath;
  private final String contestId;
  private final String undeclaredWriteInLabel;
  private final ContestConfig contestConfig;
  // map for tracking unrecognized candidates during parsing
  private final Map<String, Integer> unrecognizedCandidateCounts = new HashMap<>();

  HartCvrReader(
      String cvrPath,
      String contestId,
      ContestConfig contestConfig,
      String undeclaredWriteInLabel) {
    this.cvrPath = cvrPath;
    this.contestId = contestId;
    this.contestConfig = contestConfig;
    this.undeclaredWriteInLabel = undeclaredWriteInLabel;
  }

  // iterate all xml files in the source input folder
  void readCastVoteRecordsFromFolder(List<CastVoteRecord> castVoteRecords)
      throws IOException, UnrecognizedCandidatesException {
    File cvrRoot = new File(this.cvrPath);
    File[] children = cvrRoot.listFiles();
    if (children != null) {
      for (File child : children) {
        if (child.getName().toLowerCase().endsWith("xml")) {
          readCastVoteRecord(castVoteRecords, child.toPath());
        }
      }
    } else {
      Logger.severe("Unable to find any files in directory: %s", cvrRoot.getAbsolutePath());
      throw new IOException();
    }

    if (unrecognizedCandidateCounts.size() > 0) {
      throw new UnrecognizedCandidatesException(unrecognizedCandidateCounts);
    }
  }

  // parse Cvr xml file into CastVoteRecord objects and add them to the input List<CastVoteRecord>
  void readCastVoteRecord(List<CastVoteRecord> castVoteRecords, Path path) throws IOException {
    Logger.info("Reading Hart cast vote record file: %s...", path.getFileName());

    XmlMapper xmlMapper = new XmlMapper();
    xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    try (FileInputStream inputStream = new FileInputStream(path.toFile())) {
      HartCvrXml xmlCvr = xmlMapper.readValue(inputStream, HartCvrXml.class);

      for (Contest contest : xmlCvr.Contests) {
        if (!contest.Id.equals(this.contestId)) {
          continue;
        }

        ArrayList<Pair<Integer, String>> rankings = new ArrayList<>();
        if (contest.Options != null) {
          for (Option option : contest.Options) {
            String candidateId = option.Id;
            if (candidateId.equals(undeclaredWriteInLabel)) {
              candidateId = Tabulator.UNDECLARED_WRITE_IN_OUTPUT_LABEL;
            } else if (!contestConfig.getCandidateCodeList().contains(candidateId)) {
              unrecognizedCandidateCounts.merge(candidateId, 1, Integer::sum);
            }
            // Hart RCV election ranks are indicated by a string read left to right:
            // each digit corresponds to a rank and is set to 1 if that rank was voted:
            // 0100 indicates rank 2 was voted
            // 0000 indicates no rank was voted (undervote)
            // 0101 indicates ranks 2 and 4 are voted (overvote)
            for (int rank = 1; rank < option.Value.length() + 1; rank++) {
              String rankValue = option.Value.substring(rank - 1, rank);
              if (rankValue.equals("1")) {
                rankings.add(new Pair<>(rank, candidateId));
              }
            }
          }
        }

        CastVoteRecord cvr =
            new CastVoteRecord(
                contest.Id,
                null,
                xmlCvr.BatchNumber,
                xmlCvr.CvrGuid,
                xmlCvr.PrecinctSplit.Name,
                xmlCvr.PrecinctSplit.Id,
                null,
                rankings);
        castVoteRecords.add(cvr);

        // provide some user feedback on the Cvr count
        if (castVoteRecords.size() % 50000 == 0) {
          Logger.info("Parsed %d cast vote records.", castVoteRecords.size());
        }
      }
    } catch (IOException exception) {
      Logger.severe("Error parsing cast vote record:\n%s", exception);
      throw exception;
    }
  }

  @SuppressWarnings({"unused", "RedundantSuppression"})
  static class WriteInData {

    public String ImageId;
    public String WriteInDataStatus;
  }

  // a voter selection
  @SuppressWarnings({"unused", "RedundantSuppression"})
  static class Option {

    public String Name;
    public String Id;
    public String Value;
    public WriteInData WriteInData;
  }

  // voter selections for a contest
  @SuppressWarnings({"unused", "RedundantSuppression"})
  static class Contest {

    public String Name;
    public String Id;
    public ArrayList<Option> Options;
  }

  @SuppressWarnings({"unused", "RedundantSuppression"})
  static class PrecinctSplit {

    public String Name;
    public String Id;
  }

  @SuppressWarnings({"unused", "RedundantSuppression"})
  static class Party {

    public String Name;
    public String ID;
  }

  @SuppressWarnings({"unused", "RedundantSuppression"})
  static class HartCvrXml {

    public String BatchSequence;
    public String SheetNumber;
    public String BatchNumber;
    public String CvrGuid;
    public PrecinctSplit PrecinctSplit;
    public Party Party;
    public ArrayList<Contest> Contests;
  }
}
