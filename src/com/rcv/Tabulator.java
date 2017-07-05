package com.rcv;

import java.util.*;

public class Tabulator {

  private List<CastVoteRecord> castVoteRecords;
  private int contestId;
  private List<Integer> contestOptions;
  private boolean batchElimination = false;

  static class TieBreak {
    List<Integer> contestOptionIds;
    Integer selection;

    TieBreak(List<Integer> contestOptionIds) {
      this.contestOptionIds = contestOptionIds;
    }

    int getSelection() {
      if (selection == null) {
        selection = breakTie();
      }
      return selection;
    }

    String nonSelectedString() {
      ArrayList<Integer> options = new ArrayList<Integer>();
      for (int contestOptionId : contestOptionIds) {
        if (contestOptionId != selection) {
          options.add(contestOptionId);
        }
      }
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < options.size(); i++) {
        sb.append(options.get(i));
        if (i <= options.size() - 2 && options.size() > 2) {
          sb.append(", ");
        }
        if (i == options.size() - 2) {
          sb.append(" and ");
        }
      }
      return sb.toString();
    }

    private int breakTie() {
      // TODO: use java.security.SecureRandom
      double r = Math.random();
      int index = (int)Math.floor(r * (double)contestOptionIds.size());
      return contestOptionIds.get(index);
    }
  }

  private Map<Integer, TieBreak> tieBreaks = new HashMap<Integer, TieBreak>();

  public Tabulator(
    List<CastVoteRecord> castVoteRecords,
    int contestId,
    List<Integer> contestOptions
  ) {
    this.castVoteRecords = castVoteRecords;
    this.contestId = contestId;
    this.contestOptions = contestOptions;
  }

  public void setBatchElimination(boolean value) {
    this.batchElimination = value;
  }

  public void tabulate() {
    Map<Integer, Map<Integer, Integer>> roundTallies = new HashMap<Integer, Map<Integer, Integer>>();
    Map<Integer, Integer> eliminatedRound = new HashMap<Integer, Integer>();
    int round = 1;
    Integer winner;

    while (true) {
      Map<Integer, Integer> roundTally = getRoundTally(eliminatedRound);
      roundTallies.put(round, roundTally);

      // We fully sort the list in case we want to run batch elimination.
      int totalVotes = 0;
      SortedMap<Integer, LinkedList<Integer>> countToCandidates = new TreeMap<Integer, LinkedList<Integer>>();
      for (int contestOptionId : roundTally.keySet()) {
        int votes = roundTally.get(contestOptionId);
        totalVotes += votes;
        LinkedList<Integer> candidates = countToCandidates.get(votes);
        if (candidates == null) {
          candidates = new LinkedList<Integer>();
          countToCandidates.put(votes, candidates);
        }
        candidates.add(contestOptionId);
      }

      log("Total votes in round " + round + ": " + totalVotes + ".");

      int maxVotes = countToCandidates.lastKey();
      // Does the leader have a majority of non-exhausted ballots?
      if (maxVotes >= ((float)totalVotes + 1.0) / 2.0) {
        winner = countToCandidates.get(maxVotes).getFirst();
        log(
          winner + " won in round " + round + " with " + maxVotes + " vote(s)."
        );
        break;
      }

      List<Integer> eliminated = new LinkedList<Integer>();

      if (batchElimination) {
        eliminated.addAll(runBatchElimination(round, countToCandidates));
      }

      // If batch is enabled and it caught anyone, don't apply regular elimination logic on this iteration.
      // Otherwise, eliminate last place (breaking tie if necessary).
      if (eliminated.isEmpty()) {
        int loser;
        int minVotes = countToCandidates.firstKey();
        LinkedList<Integer> lastPlace = countToCandidates.get(minVotes);
        if (lastPlace.size() > 1) {
          TieBreak tieBreak = new TieBreak(lastPlace);
          loser = tieBreak.getSelection();
          tieBreaks.put(round, tieBreak);
          log(
            loser + " lost a tie-breaker in round " + round + " against " + tieBreak.nonSelectedString() +
              ". Each candidate had " + minVotes + " vote(s)."
          );
        } else {
          loser = lastPlace.getFirst();
          log(loser + " was eliminated in round " + round + " with " + minVotes + " vote(s).");
        }
        eliminated.add(loser);
      }

      for (int loser : eliminated) {
        eliminatedRound.put(loser, round);
      }

      round++;
    }
  }

  private void log(String s) {
    System.out.println(s);
  }

  private List<Integer> runBatchElimination(int round, SortedMap<Integer, LinkedList<Integer>> countToCandidates) {
    int runningTotal = 0;
    List<Integer> candidatesSeen = new LinkedList<Integer>();
    List<Integer> eliminated = new LinkedList<Integer>();
    for (int currentVoteCount : countToCandidates.keySet()) {
      if (runningTotal < currentVoteCount) {
        eliminated.addAll(candidatesSeen);
        for (int candidate : candidatesSeen) {
          log(
            "Batch-eliminated " + candidate + " in round " + round + ". The running total was " + runningTotal +
              " vote(s) and the next-highest count was " + currentVoteCount + " vote(s)."
          );
        }
      }
      List<Integer> currentCandidates = countToCandidates.get(currentVoteCount);
      runningTotal += currentVoteCount * currentCandidates.size();
      candidatesSeen.addAll(currentCandidates);
    }

    return eliminated;
  }

  private Map<Integer, Integer> getRoundTally(Map<Integer, Integer> eliminatedRound) {
    Map<Integer, Integer> roundTally = new HashMap<Integer, Integer>();

    for (int contestOptionId : contestOptions) {
      if (eliminatedRound.get(contestOptionId) == null) {
        roundTally.put(contestOptionId, 0);
      }
    }

    // count first-place votes, considering only non-eliminated options
    for (CastVoteRecord cvr : castVoteRecords) {
      SortedMap<Integer, Integer> rankings = cvr.getRankingsForContest(contestId);
      if (rankings == null) {
        continue;
      }
      for (int rank : rankings.keySet()) {
        int contestOptionId = rankings.get(rank);
        if (eliminatedRound.get(contestOptionId) == null) {
          roundTally.put(contestOptionId, roundTally.get(contestOptionId) + 1);
          break;
        }
      }
    }

    return roundTally;
  }
}
