/*
 * RCTab
 * Copyright (c) 2017-2023 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: Common interface for a CVR Reader.
 * Design: An abstract class.
 * Conditions: When reading CVR election data.
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.util.Pair;
import network.brightspots.rcv.RawContestConfig.CvrSource;

abstract class BaseCvrReader {
  protected final ContestConfig config;
  protected final String cvrPath; // may be a file or directory
  protected final CvrSource source;

  BaseCvrReader(ContestConfig config, CvrSource source) {
    this.config = config;
    this.source = source;
    this.cvrPath = config.resolveConfigPath(source.getFilePath());
  }

  // parse CVR for records matching the specified contestId into CastVoteRecord objects and add
  // them to the input list
  abstract void readCastVoteRecords(List<CastVoteRecord> castVoteRecords)
      throws CastVoteRecord.CvrParseException, IOException;

  // Individual contests may have a different value than what the config allows.
  public Integer getMaxRankingsAllowed(String contestId) {
    return config.getMaxRankingsAllowed();
  }

  // Any reader-specific validations can override this function.
  public void runAdditionalValidations(List<CastVoteRecord> castVoteRecords)
      throws CastVoteRecord.CvrParseException {}

  // Some CVRs have a list of candidates in the file. Read that list and return it.
  // This will be used in tandem with gatherUnknownCandidates, which only looks for candidates
  // that have at least one vote.
  public List<String> readCandidateListFromCvr(List<CastVoteRecord> castVoteRecords)
      throws IOException {
    return new ArrayList<>();
  }

  // Gather candidate names from the CVR that are not in the config.
  Map<String, Integer> gatherUnknownCandidates(
      List<CastVoteRecord> castVoteRecords, boolean includeCandidatesWithZeroVotes) {
    // First pass: gather all unrecognized candidates and their counts
    // All CVR Readers have this implemented
    Map<String, Integer> unrecognizedCandidateCounts = new HashMap<>();
    for (CastVoteRecord cvr : castVoteRecords) {
      for (Pair<Integer, CandidatesAtRanking> ranking : cvr.candidateRankings) {
        for (String candidateName : ranking.getValue()) {
          if (candidateName.equals(source.getUndeclaredWriteInLabel())
              || candidateName.equals(source.getOvervoteLabel())
              || config.getNameForCandidate(candidateName) != null) {
            continue;
          }

          unrecognizedCandidateCounts.merge(candidateName, 1, Integer::sum);
        }
      }
    }

    if (includeCandidatesWithZeroVotes) {
      // Second pass: read the entire candidate list from the CVR,
      // regardless of whether they have any votes.
      // TODO -- once all readers have this implemented, we can skip the first pass entirely
      // during auto-load candidates and just use readCandidateListFromCvr.
      List<String> allCandidates = new ArrayList<>();
      try {
        allCandidates = readCandidateListFromCvr(castVoteRecords);
      } catch (IOException e) {
        // If we can't read the candidate list, we can't check for unrecognized candidates.
        Logger.warning("IOException reading candidate list from CVR: %s", e.getMessage());
      }

      // Remove overvote and write-in label from candidate list, if they exist
      allCandidates.remove(source.getOvervoteLabel());
      allCandidates.remove(source.getUndeclaredWriteInLabel());

      // Combine the lists
      for (String candidateName : allCandidates) {
        if (!unrecognizedCandidateCounts.containsKey(candidateName)
            && config.getNameForCandidate(candidateName) == null) {
          unrecognizedCandidateCounts.put(candidateName, 0);
        }
      }
    }

    return unrecognizedCandidateCounts;
  }

  // Human-readable name for output logs
  public abstract String readerName();
}
