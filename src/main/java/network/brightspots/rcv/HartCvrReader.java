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
import java.util.List;
import javafx.util.Pair;

class HartCvrReader extends BaseCvrReader {
  HartCvrReader(ContestConfig config, RawContestConfig.CvrSource source) {
    super(config, source);
  }

  @Override
  public String readerName() {
    return "Hart";
  }

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
      throws IOException {
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
            String candidateName = option.Id;
            if (candidateName.equals(source.getUndeclaredWriteInLabel())) {
              candidateName = Tabulator.UNDECLARED_WRITE_IN_OUTPUT_LABEL;
            }
            // Hart RCV election ranks are indicated by a string read left to right:
            // each digit corresponds to a rank and is set to 1 if that rank was voted:
            // 0100 indicates rank 2 was voted
            // 0000 indicates no rank was voted (undervote)
            // 0101 indicates ranks 2 and 4 are voted (overvote)
            for (int rank = 1; rank < option.Value.length() + 1; rank++) {
              String rankValue = option.Value.substring(rank - 1, rank);
              if (rankValue.equals("1")) {
                rankings.add(new Pair<>(rank, candidateName));
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
