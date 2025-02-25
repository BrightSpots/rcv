/*
 * RCTab
 * Copyright (c) 2017-2023 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: Read and parse Hart election data for a contest into CastVoteRecord objects.
 * Design: Hart uses an xml file per CVR to store CVR data.  This class uses Jackson
 * XmlMapper to read these files into memory and parse the selections.
 * Conditions: Used when reading Hart election data.
 * Version history: see https://github.com/BrightSpots/rcv.
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.util.Pair;

class HartCvrReader extends BaseCvrReader {
  HartCvrReader(ContestConfig config, RawContestConfig.CvrSource source) {
    super(config, source);
    this.candidateCodesToCandidates = new HashMap<>();
  }

  @Override
  public String readerName() {
    return "Hart";
  }

  private Map<String, Candidate> candidateCodesToCandidates;

  boolean verifyHashIfNeeded(File cvrXml) {
    boolean isHashNeeded = SecurityConfig.isHartSignatureValidationEnabled();
    boolean isHashVerified = false;

    if (SecurityConfig.isHartSignatureValidationEnabled()) {
      File signatureXml = new File(cvrXml.getAbsolutePath() + ".sig.xml");
      if (signatureXml.exists()) {
        try {
          SecuritySignatureValidation.ensureSignatureIsValid(
                  SecurityConfig.getRsaPublicKey(), signatureXml, cvrXml);
          isHashVerified = true;
        } catch (SecuritySignatureValidation.VerificationDidNotRunException e) {
          Logger.severe("Failure while trying to verify hash %s of %s: \n%s",
                  signatureXml.getAbsolutePath(), cvrXml.getAbsolutePath(), e.getMessage());
        } catch (SecuritySignatureValidation.VerificationSignatureDidNotMatchException e) {
          Logger.severe("Incorrect hash %s of %s",
                  signatureXml.getAbsolutePath(), cvrXml.getAbsolutePath());
        }
      } else {
        Logger.severe("A cryptographic signature is required at %s, but it was not found.",
                signatureXml.getAbsolutePath());
      }
    }

    if (isHashNeeded && isHashVerified) {
      Logger.info("Signature validation successful for %s", cvrXml.getName());
    }

    // This function returns true if a hash isn't needed, or if verification is successful
    return !isHashNeeded || isHashVerified;
  }

  // iterate all xml files in the source input folder
  @Override
  void readCastVoteRecords(List<CastVoteRecord> castVoteRecords)
      throws CastVoteRecord.CvrParseException, IOException {
    File cvrRoot = new File(this.cvrPath);
    File[] children = cvrRoot.listFiles();
    if (children != null) {
      for (File child : children) {
        String childNameLower = child.getName().toLowerCase();
        if (childNameLower.endsWith("xml") && !childNameLower.endsWith(".sig.xml")) {
          if (!verifyHashIfNeeded(child)) {
            throw new CastVoteRecord.CvrParseException();
          }
          readCastVoteRecord(castVoteRecords, child.toPath());
        }
      }
    } else {
      Logger.severe("Unable to find any files in directory: %s", cvrRoot.getAbsolutePath());
      throw new CastVoteRecord.CvrParseException();
    }
  }

  // parse Cvr xml file into CastVoteRecord objects and add them to the input List<CastVoteRecord>
  private void readCastVoteRecord(List<CastVoteRecord> castVoteRecords, Path path)
      throws IOException, CastVoteRecord.CvrParseException {
    Logger.info("Reading Hart cast vote record file: %s...", path.getFileName());

    XmlMapper xmlMapper = new XmlMapper();
    xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    try (FileInputStream inputStream = new FileInputStream(path.toFile())) {
      HartCvrXml xmlCvr = xmlMapper.readValue(inputStream, HartCvrXml.class);

      for (Contest contest : xmlCvr.Contests) {
        if (!contest.Id.equals(source.getContestId())) {
          continue;
        }

        ArrayList<Pair<Integer, String>> rankings = new ArrayList<>();
        if (contest.Options != null) {
          for (Option option : contest.Options) {
            Candidate candidate = new Candidate(option.Name, option.Id);
            if (candidate.Code.equals(source.getUndeclaredWriteInLabel())) {
              candidate.Code = Tabulator.UNDECLARED_WRITE_IN_OUTPUT_LABEL;
            } else {
              this.candidateCodesToCandidates.computeIfAbsent(candidate.Code,
                    k -> candidate);

              if (!this.candidateCodesToCandidates.get(candidate.Code).Name
                      .equals(candidate.Name)) {
                String message =
                        "Candidate Code %s associated with more than one candidate name."
                                + "Originally associated with name '%s'."
                                + "In CVR at '%s' it is associated with '%s'."
                                .formatted(candidate.Code,
                                        this.candidateCodesToCandidates.get(candidate.Code).Name,
                                        path.getFileName(), candidate.Name);
                Logger.severe(message);
                throw new CastVoteRecord.CvrParseException();
              }
            }

            // Hart RCV election ranks are indicated by a string read left to right:
            // each digit corresponds to a rank and is set to 1 if that rank was voted:
            // 0100 indicates rank 2 was voted
            // 0000 indicates no rank was voted (undervote)
            // 0101 indicates ranks 2 and 4 are voted (overvote)
            for (int rank = 1; rank < option.Value.length() + 1; rank++) {
              String rankValue = option.Value.substring(rank - 1, rank);
              if (rankValue.equals("1")) {
                rankings.add(new Pair<>(rank, candidate.Code));
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
                usesLastAllowedRanking(rankings, null),
                rankings);
        castVoteRecords.add(cvr);

        // provide some user feedback on the Cvr count
        if (castVoteRecords.size() % 50000 == 0) {
          Logger.info("Parsed %d cast vote records.", castVoteRecords.size());
        }
      }
    }
  }

  @Override
  public Set<RawContestConfig.Candidate> gatherUnknownCandidates(
          List<CastVoteRecord> castVoteRecords) {

    Set<String> knownNames = config.getCandidateNames();
    if (this.candidateCodesToCandidates.entrySet().isEmpty()) {
      try {
        //Reading the CVRs will load this.candidateCodesToCandidates
        readCastVoteRecords(castVoteRecords);
      } catch (CastVoteRecord.CvrParseException | IOException e) {
        Logger.severe("Error gathering Unknown Candidates\n%s", e);
        return new HashSet<>();
      }
    }

    // Return the candidate codes that are not in the knownNames set
    return candidateCodesToCandidates.entrySet().stream()
            .filter(entry -> !knownNames.contains(entry.getValue().Name))
            .map(entry -> new RawContestConfig.Candidate(entry.getValue().Name, entry.getKey()))
            .collect(Collectors.toSet());
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
  static class Candidate {

    public String Name;
    @SuppressWarnings({"unused", "unread"})
    public String Code;

    Candidate(String name, String code) {
      this.Name = name;
      this.Code = code;
    }
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
