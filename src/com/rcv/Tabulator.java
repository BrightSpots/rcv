package com.rcv;

import java.util.*;

public class Tabulator {

  private List<CastVoteRecord> castVoteRecords;
  private int contestId;
  private List<String> contestOptions;
  private boolean batchElimination = false;

  static class TieBreak {
    List<String> contestOptionIds;
    String selection;

    TieBreak(List<String> contestOptionIds) {
      this.contestOptionIds = contestOptionIds;
    }

    String getSelection() {
      if (selection == null) {
        selection = breakTie();
      }
      return selection;
    }

    String nonSelectedString() {
      ArrayList<String> options = new ArrayList<String>();
      for (String contestOptionId : contestOptionIds) {
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

    private String breakTie() {
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
    List<String> contestOptions,
    Boolean batchElimination
  ) {
    this.castVoteRecords = castVoteRecords;
    this.contestId = contestId;
    this.contestOptions = contestOptions;
    this.batchElimination = batchElimination;
  }

  public void tabulate() {

    RCVLogger.log("Beginning tabulation for contest:%d", this.contestId);
    RCVLogger.log("there are %d candidates for this contest:", this.contestOptions.size());
    for (String option : this.contestOptions) {
      RCVLogger.log("%s", option);
    }
    RCVLogger.log("there are %d cast vote records for this contest", this.castVoteRecords.size());

    // roundTallies is a map of round # --> a map of candidate ID -> vote totals for that round
    Map<Integer, Map<String, Integer>> roundTallies = new HashMap<Integer, Map<String, Integer>>();

    // eliminatedRound object is a map of candidate IDs to the round in which they were eliminated
    Map<String, Integer> eliminatedRound = new HashMap<String, Integer>();
    int round = 1;
    String winner;

    List<SortedMap<Integer, Set<String>>> sortedRankings = sortCastVoteRecords(castVoteRecords);

    // loop until we achieve a majority winner:
    // at each iteration we will eliminate the lowest-total candidate OR multiple losing candidates if using batch
    // elimination logic (Maine rules)
    while (true) {
      RCVLogger.log("Round: %d", round);

      // roundTally is map of candidate ID to vote tallies
      // generated based on previously eliminated candidates contained in eliminatedRound object
      // at each iteration of this loop, the eliminatedRound object will get more entries as candidates are eliminated
      // conversely the roundTally object returned here will contain fewer entries each of which will have more votes
      // eventually a winner will be chosen
      Map<String, Integer> roundTally = getRoundTally(sortedRankings, eliminatedRound);
      roundTallies.put(round, roundTally);

      // We fully sort the list in case we want to run batch elimination.
      int totalVotes = 0;
      // map of vote tally to candidate(s).  A list is used to handle ties.
      SortedMap<Integer, LinkedList<String>> countToCandidates = new TreeMap<Integer, LinkedList<String>>();
      // for each candidate record their vote total into the countTOCandidates object
      for (String contestOptionId : roundTally.keySet()) {
        int votes = roundTally.get(contestOptionId);

        RCVLogger.log("candidate %s got %d votes", contestOptionId, votes);
        
        // count the total votes cast in this round
        totalVotes += votes;
        LinkedList<String> candidates = countToCandidates.get(votes);
        if (candidates == null) {
          candidates = new LinkedList<String>();
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
      List<String> eliminated = new LinkedList<String>();
      if (batchElimination) {
        eliminated.addAll(runBatchElimination(round, countToCandidates));
      }

      // If batch elimination caught anyone, don't apply regular elimination logic on this iteration.
      // Otherwise, eliminate last place, breaking tie if necessary.
      if (eliminated.isEmpty()) {
        String loser;
        int minVotes = countToCandidates.firstKey();
        LinkedList<String> lastPlace = countToCandidates.get(minVotes);
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

      for (String loser : eliminated) {
        eliminatedRound.put(loser, round);
      }

      round++;
    }
  }

  private void log(String s) {
    RCVLogger.log(s);
  }

  private List<String> runBatchElimination(int round, SortedMap<Integer, LinkedList<String>> countToCandidates) {
    int runningTotal = 0;
    List<String> candidatesSeen = new LinkedList<String>();
    List<String> eliminated = new LinkedList<String>();
    for (int currentVoteCount : countToCandidates.keySet()) {
      if (runningTotal < currentVoteCount) {
        eliminated.addAll(candidatesSeen);
        for (String candidate : candidatesSeen) {
          log(
            "Batch-eliminated " + candidate + " in round " + round + ". The running total was " + runningTotal +
              " vote(s) and the next-highest count was " + currentVoteCount + " vote(s)."
          );
        }
      }
      List<String> currentCandidates = countToCandidates.get(currentVoteCount);
      runningTotal += currentVoteCount * currentCandidates.size();
      candidatesSeen.addAll(currentCandidates);
    }

    return eliminated;
  }

  // roundTally returns a map of candidate ID to vote tallies
  // generated based on previously eliminated candidates contained in eliminatedRound object
  // at each iteration of this loop, the eliminatedRound object will get more entries as candidates are eliminated
  // conversely the roundTally object returned here will contain fewer entries each of which will have more votes
  // eventually a winner will be chosen
  private Map<String, Integer> getRoundTally(
    List<SortedMap<Integer, Set<String>>> allSortedRankings,
    Map<String, Integer> eliminatedRound
  ) {
    Map<String, Integer> roundTally = new HashMap<String, Integer>();

    // if a candidate has already been eliminated they get 0 votes
    for (String contestOptionId : contestOptions) {
      if (eliminatedRound.get(contestOptionId) == null) {
        roundTally.put(contestOptionId, 0);
      }
    }

    // count first-place votes, considering only non-eliminated options
    for (SortedMap<Integer, Set<String>> rankings : allSortedRankings) {
      for (int rank : rankings.keySet()) {
        Set<String> contestOptionIds = rankings.get(rank);
        if (contestOptionIds.size() > 1) {
          System.out.println("Overvote!");
          continue;
        }
        String contestOptionId = contestOptionIds.iterator().next();
        if (eliminatedRound.get(contestOptionId) == null) {
          // found a continuing candidate so increase their tally by 1
          roundTally.put(contestOptionId, roundTally.get(contestOptionId) + 1);
          break;
        }
      }
    }

    return roundTally;
  }

  // input is a list of CastVoteRecords
  // 
  private List<SortedMap<Integer, Set<String>>> sortCastVoteRecords(List<CastVoteRecord> castVoteRecords) {
    // returns a list of "sortedCVRs"
    List<SortedMap<Integer, Set<String>>> allSortedRankings = new LinkedList<SortedMap<Integer, Set<String>>>();

    // for each input CVR see what rankings were given for the contest of interest
    for (CastVoteRecord cvr : castVoteRecords) {
      // sortedCVR will contain the rankings for the contest of interest in order from low to high
      // note: we use a set<Integer> here because there may be overvotes with different candidates getting
      // the same ranking, for example ranking 3 could map to candidates 1 and 2
      SortedMap<Integer, Set<String>> sortedCVR = new TreeMap<Integer, Set<String>>();
      for (ContestRanking ranking : cvr.getRankingsForContest(contestId)) {
        // set of candidates given this rank
        Set<String> optionsAtRank = sortedCVR.get(ranking.getRank());
        if (optionsAtRank == null) {
          // create the new optionsAtRank and add to the sorted cvr
          optionsAtRank = new HashSet<String>();
          sortedCVR.put(ranking.getRank(), optionsAtRank);
        }
        // add this option into the map
        optionsAtRank.add(ranking.getOptionId());
      }
      allSortedRankings.add(sortedCVR);
    }

    return allSortedRankings;
  }
}
