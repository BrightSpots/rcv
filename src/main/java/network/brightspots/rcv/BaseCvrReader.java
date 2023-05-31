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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

  // Gather candidate names from the CVR that are not in the config.
  Set<String> gatherUnknownCandidateNames() throws CastVoteRecord.CvrParseException, IOException {
    try {
      readCastVoteRecords(new ArrayList<>(), new HashSet<>());
    } catch (TabulatorSession.UnrecognizedCandidatesException unrecognizedCandidates) {
      return unrecognizedCandidates.candidateCounts.keySet();
    }
    return new HashSet<String>();
  }

  // parse CVR for records matching the specified contestId into CastVoteRecord objects and add
  // them to the input list
  abstract void readCastVoteRecords(List<CastVoteRecord> castVoteRecords, Set<String> precinctIds)
      throws CastVoteRecord.CvrParseException,
          TabulatorSession.UnrecognizedCandidatesException,
          IOException;

  // Human-readable name for output logs
  public abstract String readerName();
}
