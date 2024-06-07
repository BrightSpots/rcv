/*
 * RCTab
 * Copyright (c) 2017-2023 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: Low-Memory container for a single cast vote record
 * Design: Assumes valid ballots but supports overvotes and skipped rankings.
 * Conditions: Always.
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import javafx.util.Pair;

class CandidateRankingsList implements Iterable<Pair<Integer, CandidatesAtRanking>> {
  private final CandidatesAtRanking[] rankings;
  private int numRankings;

  CandidateRankingsList(List<Pair<Integer, String>> rawRankings) {
    if (rawRankings.isEmpty()) {
      this.rankings = new CandidatesAtRanking[0];
      numRankings = 0;
    } else {
      rawRankings.sort(Comparator.comparingInt(Pair::getKey));

      // Initialize up to maxRankings, leaving empty arrays for any skipped rankings
      int minRanking = rawRankings.get(0).getKey();
      if (minRanking <= 0) {
        throw new RuntimeException(
            "Invalid ranking %d. All rankings must be positive integers".formatted(minRanking));
      }

      int maxRanking = rawRankings.get(rawRankings.size() - 1).getKey();
      this.rankings = new CandidatesAtRanking[maxRanking];
      for (int i = 0; i < maxRanking; i++) {
        this.rankings[i] = new CandidatesAtRanking();
      }

      // Populate, which is most efficient in the common case of no overvotes.
      // For every overvote, it will do a full copy of all previous candidates at
      // this ranking, for O(numCandidatesAtThisRanking^2) copies.
      for (Pair<Integer, String> ranking : rawRankings) {
        this.rankings[ranking.getKey() - 1].addCandidate(ranking.getValue());
      }

      for (CandidatesAtRanking c : this.rankings) {
        if (c.count() != 0) {
          ++numRankings;
        }
      }
    }
  }

  /**
   * Precondition: must be called with a 1-indexed ranking number. Note: just because this returns
   * false at ranking N doesn't mean it will also return false at N+1 -- specifically, that will not
   * be true if there are skipped rankings.
   *
   * @param num A value >= 1.
   * @return Whether the candidate has a ranking at the given value.
   */
  boolean hasRankingAt(int num) {
    if (num < 1) {
      throw new IllegalArgumentException();
    }
    return num <= rankings.length && rankings[num - 1].count() != 0;
  }

  CandidatesAtRanking get(int i) {
    return rankings[i - 1];
  }

  int maxRankingNumber() {
    if (numRankings == 0) {
      throw new IllegalArgumentException("Max ranking may only be called on non-empty rankings!");
    }
    return this.rankings.length;
  }

  int numRankings() {
    return numRankings;
  }

  public Iterator<Pair<Integer, CandidatesAtRanking>> iterator() {
    return new CandidateRankingsListIterator();
  }

  class CandidateRankingsListIterator implements Iterator<Pair<Integer, CandidatesAtRanking>> {
    private int iteratorIndex = 0;

    public boolean hasNext() {
      return iteratorIndex < rankings.length;
    }

    public Pair<Integer, CandidatesAtRanking> next() {
      if (iteratorIndex >= rankings.length) {
        throw new NoSuchElementException();
      }

      do {
        iteratorIndex++;
        if (iteratorIndex > rankings.length) {
          throw new NoSuchElementException();
        }
      } while (!hasRankingAt(iteratorIndex));

      // Note: round numbers are 1-indexed externally, 0-indexed internally,
      // thus why we return a different value than what we index into here
      return new Pair<>(iteratorIndex, rankings[iteratorIndex - 1]);
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
}
