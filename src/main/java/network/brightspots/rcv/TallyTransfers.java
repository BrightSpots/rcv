/*
 * RCTab
 * Copyright (c) 2017-2023 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: Export round-by-round vote transfer data for analysis and visualization by external
 * applications.
 * Design: Container class which stores a map for each round showing how many votes were
 * transferred from each candidate to each candidate.
 * Conditions: During tabulation.
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

// TallyTransfers class stores summary info on vote transfers
// used primarily as input for external visualizer software to help build Sankey plots
class TallyTransfers {

  static final String RESIDUAL_TARGET = "residual surplus";
  private static final String EXHAUSTED = "exhausted";
  private static final String UNCOUNTED = "uncounted";
  static final Set<String> RESERVED_STRINGS = Set.of(RESIDUAL_TARGET, EXHAUSTED, UNCOUNTED);

  // Map of round number to vote transfers which occurred in that round
  // transfers for a round are a map of SOURCE candidate(s) to one or more TARGET candidates.
  // For each target candidate the map value is total vote values received from that source.
  // For round 1 source candidate is marked "uncounted" since the votes had no prior recipient.
  private final Map<Integer, Map<String, Map<String, BigDecimal>>> tallyTransfers = new HashMap<>();

  Map<String, Map<String, BigDecimal>> getTransfersForRound(int round) {
    return tallyTransfers.get(round);
  }

  // add vote transfer value for given round
  void addTransfer(int round, String sourceCandidate, String targetCandidate, BigDecimal value) {
    // null source means we are transferring the initial count
    if (sourceCandidate == null) {
      sourceCandidate = UNCOUNTED;
    }
    // null target means exhausted
    if (targetCandidate == null) {
      targetCandidate = EXHAUSTED;
    }

    // lookup or create entries for specified round
    Map<String, Map<String, BigDecimal>> roundEntries =
        tallyTransfers.computeIfAbsent(round, k -> new HashMap<>());
    Map<String, BigDecimal> candidateEntries =
        roundEntries.computeIfAbsent(sourceCandidate, k -> new HashMap<>());
    BigDecimal currentValue = candidateEntries.getOrDefault(targetCandidate, BigDecimal.ZERO);
    BigDecimal newTally = currentValue.add(value);
    candidateEntries.put(targetCandidate, newTally);
  }
}
