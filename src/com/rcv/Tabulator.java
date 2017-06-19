package com.rcv;

import java.util.*;

public class Tabulator {

  private List<CastVoteRecord> castVoteRecords;
  private int contestId;
  private List<Integer> contestOptions;

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

  public Tabulator(List<CastVoteRecord> castVoteRecords, int contestId, List<Integer> contestOptions) {
    this.castVoteRecords = castVoteRecords;
    this.contestId = contestId;
    this.contestOptions = contestOptions;
  }

  public void tabulate() {
    Map<Integer, Map<Integer, Integer>> roundTallies = new HashMap<Integer, Map<Integer, Integer>>();
    Map<Integer, Integer> eliminatedRound = new HashMap<Integer, Integer>();
    int round = 1;
    Integer winner = null;

    while (true) {
      Map<Integer, Integer> roundTally = new HashMap<Integer, Integer>();
      roundTallies.put(round, roundTally);
      for (int contestOptionId : contestOptions) {
        if (eliminatedRound.get(contestOptionId) == null) {
          roundTally.put(contestOptionId, 0);
        }
      }

      // count first-place votes considering only non-eliminated options
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

      int totalVotes = 0;
      LinkedList<Integer> firstPlace = new LinkedList<Integer>();
      int maxVotes = 0;
      LinkedList<Integer> lastPlace = new LinkedList<Integer>();
      Integer minVotes = Integer.MAX_VALUE;
      for (int contestOptionId : roundTally.keySet()) {
        int votes = roundTally.get(contestOptionId);
        totalVotes += votes;
        if (votes < minVotes) {
          minVotes = votes;
          lastPlace.clear();
          lastPlace.add(contestOptionId);
        } else if (votes == minVotes) {
          lastPlace.add(contestOptionId);
        }
        if (votes > maxVotes) {
          maxVotes = votes;
          firstPlace.clear();
          firstPlace.add(contestOptionId);
        } else if (votes == maxVotes) {
          firstPlace.add(contestOptionId);
        }
      }

      // Does the leader have a majority of non-exhausted ballots?
      if (maxVotes >= ((float)totalVotes + 1.0) / 2.0) {
        winner = firstPlace.getFirst();
        System.out.println(
          winner + " won in round " + round + " with " + maxVotes + " out of " + totalVotes + " votes"
        );
        break;
      }

      // eliminate last place (breaking tie if necessary)
      int loser;
      if (lastPlace.size() > 1) {
        TieBreak tieBreak = new TieBreak(lastPlace);
        loser = tieBreak.getSelection();
        tieBreaks.put(round, tieBreak);
        System.out.println(
          loser + " lost a tie-breaker in round " + round + " against " + tieBreak.nonSelectedString()
        );
      } else {
        loser = lastPlace.getFirst();
      }
      eliminatedRound.put(loser, round);
      System.out.println(
        loser + " was eliminated in round " + round + " with " + minVotes + " out of " + totalVotes + " votes"
      );
      round++;
    }
  }
}
