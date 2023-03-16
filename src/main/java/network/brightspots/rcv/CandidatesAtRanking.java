/*
 * RCTab
 * Copyright (c) 2017-2022 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: Low-Memory container for a list of candidates at a single ranking.
 * Design: Assume that there is exactly one candidate per ranking, but supports multiple.
 * Conditions: Always.
 * Version history: see https://github.com/BrightSpots/rcv.
 */


package network.brightspots.rcv;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

class CandidatesAtRanking implements Iterable<String> {
  static class CandidatesAtRankingIterator implements Iterator<String> {
    private int iteratorIndex = 0;

    private CandidatesAtRanking candidates;

    CandidatesAtRankingIterator(CandidatesAtRanking candidates) {
      this.candidates = candidates;
    }

    public boolean hasNext() {
      if (iteratorIndex < candidates.candidateNames.length) {
        return true;
      }
      return false;
    }

    public String next() {
      if (iteratorIndex == candidates.candidateNames.length) {
        throw new NoSuchElementException();
      }

      iteratorIndex++;

      return candidates.candidateNames[iteratorIndex - 1];
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  private String[] candidateNames;

  int count() {
    if (candidateNames == null) {
      return 0;
    }
    return candidateNames.length;
  }

  void addCandidate(String candidateName) {
    int n = count();
    String[] newList = new String[n + 1];
    for (int i = 0; i < n; ++i) {
      newList[i] = this.candidateNames[i];
    }
    newList[n] = candidateName;
    this.candidateNames = newList;
  }

  String get(int i) {
    return candidateNames[i];
  }

  boolean contains(String s) {
    return Arrays.stream(this.candidateNames).anyMatch(s::equals);
  }

  public Iterator<String> iterator() {
    return new CandidatesAtRankingIterator(this);
  }
}
