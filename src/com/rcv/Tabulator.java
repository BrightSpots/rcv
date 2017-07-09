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

    RCVLogger.log("Beginning tabulation for contest:%d", this.contestId);
    RCVLogger.log("there are %d candidates for this contest:", this.contestOptions.size());
    for (Integer option : this.contestOptions) {
      RCVLogger.log("%d", option);
    }
    RCVLogger.log("there are %d cast vote records for this contest", this.castVoteRecords.size());

    // map of round to a map of candidate ID -> vote totals for that round
    Map<Integer, Map<Integer, Integer>> roundTallies = new HashMap<Integer, Map<Integer, Integer>>();
    // map of candidate IDs to the round they were eliminated in
    Map<Integer, Integer> eliminatedRound = new HashMap<Integer, Integer>();
    int round = 1;
    Integer winner;

    List<SortedMap<Integer, Set<Integer>>> sortedRankings = sortCastVoteRecords(castVoteRecords);

    // loop until we achieve a majority winner:
    // at each iteration we will eliminate the lowest-total candidate OR multiple losing candidates if using batch
    // elimination logic (Maine rules)
    while (true) {
      RCVLogger.log("Round: %d", round);

      // map of candidate ID to vote tallies
      // tallies are generated based on previously eliminated candidates (contained in eliminatedRound object)
      Map<Integer, Integer> roundTally = getRoundTally(sortedRankings, eliminatedRound);
      roundTallies.put(round, roundTally);

      // We fully sort the list in case we want to run batch elimination.
      int totalVotes = 0;
      // map of vote tally to candidate(s).  A list is used to handle ties.
      SortedMap<Integer, LinkedList<Integer>> countToCandidates = new TreeMap<Integer, LinkedList<Integer>>();
      // for each candidate record their vote total into the countTOCandidates object
      for (int contestOptionId : roundTally.keySet()) {
        int votes = roundTally.get(contestOptionId);

        RCVLogger.log("candidate %d got %d votes", contestOptionId, votes);
        
        // count the total votes cast in this round
        totalVotes += votes;
        LinkedList<Integer> candidates = countToCandidates.get(votes);
        if (candidates == null) {
          candidates = new LinkedList<Integer>();
          countToCandidates.put(votes, candidates);
        }
        candidates.add(contestOptionId);
      }

      RCVLogger.log("Total votes in round %d:%d", round , totalVotes);

      int maxVotes = countToCandidates.lastKey();
      // Does the leader have a majority of non-exhausted ballots?
      if (maxVotes > (float)totalVotes / 2.0) {
        winner = countToCandidates.get(maxVotes).getFirst();
        RCVLogger.log(
          winner + " won in round %d with %d votes",round , maxVotes);
        break;
      }

      // container for eliminated candidate(s)
      List<Integer> eliminated = new LinkedList<Integer>();
      if (batchElimination) {
        eliminated.addAll(runBatchElimination(round, countToCandidates));
      }

      // If batch elimination caught anyone, don't apply regular elimination logic on this iteration.
      // Otherwise, eliminate last place, breaking tie if necessary.
      if (eliminated.isEmpty()) {
        int loser;
        int minVotes = countToCandidates.firstKey();
        LinkedList<Integer> lastPlace = countToCandidates.get(minVotes);
        if (lastPlace.size() > 1) {
          TieBreak tieBreak = new TieBreak(lastPlace);
          loser = tieBreak.getSelection();
          tieBreaks.put(round, tieBreak);
          RCVLogger.log(
            loser + " lost a tie-breaker in round " + round + " against " + tieBreak.nonSelectedString() +
              ". Each candidate had " + minVotes + " vote(s)."
          );
        } else {
          loser = lastPlace.getFirst();
          RCVLogger.log(loser + " was eliminated in round " + round + " with " + minVotes + " vote(s).");
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
    RCVLogger.log(s);
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

  private Map<Integer, Integer> getRoundTally(
    List<SortedMap<Integer, Set<Integer>>> allSortedRankings,
    Map<Integer, Integer> eliminatedRound
  ) {
    Map<Integer, Integer> roundTally = new HashMap<Integer, Integer>();

    // if a candidate has already been eliminated they get 0 votes
    for (int contestOptionId : contestOptions) {
      if (eliminatedRound.get(contestOptionId) == null) {
        roundTally.put(contestOptionId, 0);
      }
    }

    // count first-place votes, considering only non-eliminated options
    for (SortedMap<Integer, Set<Integer>> rankings : allSortedRankings) {
      for (int rank : rankings.keySet()) {
        Set<Integer> contestOptionIds = rankings.get(rank);
        if (contestOptionIds.size() > 1) {
          System.out.println("Overvote!");
          continue;
        }
        int contestOptionId = contestOptionIds.iterator().next();
        if (eliminatedRound.get(contestOptionId) == null) {
          // found a continuing candidate so increase their tally by 1
          roundTally.put(contestOptionId, roundTally.get(contestOptionId) + 1);
          break;
        }
      }
    }

    return roundTally;
  }

  private List<SortedMap<Integer, Set<Integer>>> sortCastVoteRecords(List<CastVoteRecord> castVoteRecords) {
    List<SortedMap<Integer, Set<Integer>>> allSortedRankings = new LinkedList<SortedMap<Integer, Set<Integer>>>();
    for (CastVoteRecord cvr : castVoteRecords) {
      SortedMap<Integer, Set<Integer>> sortedCVR = new TreeMap<Integer, Set<Integer>>();
      for (ContestRanking ranking : cvr.getRankingsForContest(contestId)) {
        Set<Integer> optionsAtRank = sortedCVR.get(ranking.getRank());
        if (optionsAtRank == null) {
          optionsAtRank = new HashSet<Integer>();
          sortedCVR.put(ranking.getRank(), optionsAtRank);
        }
        optionsAtRank.add(ranking.getOptionId());
      }
      allSortedRankings.add(sortedCVR);
    }

    return allSortedRankings;
  }
}
