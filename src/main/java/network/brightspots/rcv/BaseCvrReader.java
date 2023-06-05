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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
  abstract void readCastVoteRecords(List<CastVoteRecord> castVoteRecords, Set<String> precinctIds)
      throws CastVoteRecord.CvrParseException, IOException;

  // Any reader-specific validations can override this function.
  public void runAdditionalValidations(List<CastVoteRecord> castVoteRecords)
      throws CastVoteRecord.CvrParseException {}

  // Gather candidate names from the CVR that are not in the config.
  Map<String, Integer> gatherUnknownCandidates(List<CastVoteRecord> castVoteRecords) {
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

    return unrecognizedCandidateCounts;
  }

  // Human-readable name for output logs
  public abstract String readerName();
}
