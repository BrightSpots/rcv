/*
 * Ranked Choice Voting Universal Tabulator
 * Copyright (c) 2017-2019 Bright Spots Developers.
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
 * TallyTransfers is a container class which stores a map for each round showing how many votes were
 * transferred from each candidate to each candidate.
 * The primary purpose for this is generating Sankey plots that visually show the flow of votes
 * over the course of a tabulation.
 */

package com.rcv;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

// TallyTransfers class stores summary info on vote transfers
// used primary as visualizer input to help build Sankey plots
class TallyTransfers {

  // Map of round number to vote transfers which occurred in that round
  // transfers for a round are a map of SOURCE candidate(s) to one or more TARGET candidates.
  // For each target candidate the map value is total vote values received from that source.
  // For round 1 source candidate is marked "uncounted" since the votes had no prior recipient.
  private final Map<Integer, Map<String, Map<String, BigDecimal>>> tallyTransfers = new HashMap<>();

  // function: getTransfersForRound
  // purpose: getter for tallyTransfers object
  // param: round for which to return transfers
  // returns: tally transfer map for specified round
  Map<String, Map<String, BigDecimal>> getTransfersForRound(int round) {
    return tallyTransfers.get(round);
  }

  // function: addTransfer
  // purpose: add vote transfer value for given round
  // param: round transfers should be added to
  // param: sourceCandidate from which the transfers originate
  // param: targetCandidate to which the transfers go
  // param: value total value of all transfers
  void addTransfer(int round, String sourceCandidate, String targetCandidate, BigDecimal value) {
    // null source means we are transferring the initial count
    if (sourceCandidate == null) {
      sourceCandidate = "uncounted";
    }
    // null target means exhausted
    if (targetCandidate == null) {
      targetCandidate = "exhausted";
    }

    // lookup or create transfer entries for specified round
    Map<String, Map<String, BigDecimal>> roundEntries =
        tallyTransfers.computeIfAbsent(round, k -> new HashMap<>());
    // lookup or create map for the source candidate
    Map<String, BigDecimal> candidateEntries =
        roundEntries.computeIfAbsent(sourceCandidate, k -> new HashMap<>());
    // lookup or create entry for the destination candidate
    BigDecimal currentValue = candidateEntries.getOrDefault(targetCandidate, BigDecimal.ZERO);
    // add transfer value and store the result
    BigDecimal newTally = currentValue.add(value);
    candidateEntries.put(targetCandidate, newTally);
  }
}
