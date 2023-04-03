/*
 * RCTab
 * Copyright (c) 2017-2022 Bright Spots Developers.
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
  class CandidateRankingsListIterator implements Iterator<Pair<Integer, CandidatesAtRanking>> {
    private int iteratorIndex = 0;

    private CandidateRankingsList list;

    CandidateRankingsListIterator(CandidateRankingsList list) {
      this.list = list;
    }

    public boolean hasNext() {
      return iteratorIndex < list.rankings.length;
    }

    public Pair<Integer, CandidatesAtRanking> next() {
      if (iteratorIndex == list.rankings.length) {
        throw new NoSuchElementException();
      }

      do {
        iteratorIndex++;
        assert iteratorIndex <= list.rankings.length;
      } while (!hasRankingAt(iteratorIndex));

      // Note: round numbers are 1-indexed externally, 0-indexed internally,
      // thus why we return a different value than what we index into here
      return new Pair<>(iteratorIndex, list.rankings[iteratorIndex - 1]);
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  private CandidatesAtRanking[] rankings;
  private int numRankings;
  private int iteratorIndex = 0;

  CandidateRankingsList(List<Pair<Integer, String>> rawRankings) {
    if (rawRankings.isEmpty()) {
      this.rankings = new CandidatesAtRanking[0];
      numRankings = 0;
    } else {
      rawRankings.sort(Comparator.comparingInt(Pair::getKey));

      // Initialize up to maxRankings, leaving empty arrays for any undervotes
      int minRanking = rawRankings.get(0).getKey();
      if (minRanking <= 0) {
        throw new RuntimeException("Rankings must start at 1, but you have a ranking at %d"
            .formatted(minRanking));
      }

      int maxRanking = rawRankings.get(rawRankings.size() - 1).getKey();
      this.rankings = new CandidatesAtRanking[maxRanking];
      for (int i = 0; i < maxRanking; ++i) {
        this.rankings[i] = new CandidatesAtRanking();
      }

      // Populate, which is most efficient in the common case of no overvotes
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

  CandidatesAtRanking get(int i) {
    return rankings[i - 1];
  }

  boolean hasRankingAt(int num) {
    assert num >= 1;
    return num <= rankings.length && rankings[num - 1].count() != 0;
  }

  int maxRankingNumber() {
    return this.rankings.length;
  }

  int numRankings() {
    return numRankings;
  }

  public Iterator<Pair<Integer, CandidatesAtRanking>> iterator() {
    return new CandidateRankingsListIterator(this);
  }
}
