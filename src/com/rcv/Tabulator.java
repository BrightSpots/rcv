package com.rcv;

import java.util.*;

public class Tabulator {

  enum OvervoteRule {
    IMMEDIATE_EXHAUSTION,
    EXHAUST_IF_ANY_CONTINUING,
    IGNORE_IF_ANY_CONTINUING,
    EXHAUST_IF_MULTIPLE_CONTINUING,
    IGNORE_IF_MULTIPLE_CONTINUING,
  }

  enum OvervoteDecision {
    NONE,
    EXHAUST,
    IGNORE,
  }

  private List<CastVoteRecord> castVoteRecords;
  private int contestId;
  private List<String> contestOptions;
  private boolean useBatchElimination = true;
  private Integer maxNumberOfSkippedRanks = 1;
  private OvervoteRule overvoteRule = OvervoteRule.EXHAUST_IF_ANY_CONTINUING;
  private Integer minVoteThreshold;
  private String undeclaredWriteInString;

  private String contestName;
  private String jurisdiction;
  private String office;
  private String electionDate;

  // roundTallies is a map of round # --> a map of candidate ID -> vote totals for that round
  private Map<Integer, Map<String, Integer>> roundTallies = new HashMap<Integer, Map<String, Integer>>();

  // eliminatedRound is a map of candidate IDs to the round in which they were eliminated
  private Map<String, Integer> eliminatedRound = new HashMap<String, Integer>();

  // the winner
  private String winner;

  // when tabulation is complete this will be how many rounds did it take to determine a winner
  private int finalRound = 1;

  static class BatchElimination {
    String optionId;
    int runningTotal;
    int nextHighestCount;

    BatchElimination(String optionId, int runningTotal, int nextHighestCount) {
      this.optionId = optionId;
      this.runningTotal = runningTotal;
      this.nextHighestCount = nextHighestCount;
    }
  }

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
        if (!contestOptionId.equals(selection)) {
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

  Tabulator(
    List<CastVoteRecord> castVoteRecords,
    int contestId,
    List<String> contestOptions,
    Boolean useBatchElimination,
    Integer maxNumberOfSkippedRanks,
    OvervoteRule overvoteRule,
    Integer minVoteThreshold,
    String undeclaredWriteInString
  ) {
    this.castVoteRecords = castVoteRecords;
    this.contestId = contestId;
    this.contestOptions = contestOptions;
    this.useBatchElimination = useBatchElimination;
    this.maxNumberOfSkippedRanks = maxNumberOfSkippedRanks;
    this.overvoteRule = overvoteRule;
    this.minVoteThreshold = minVoteThreshold;
    this.undeclaredWriteInString = undeclaredWriteInString;
  }

  public void tabulate() throws Exception {

    log("Beginning tabulation for contest: %d", contestId);
    log("There are %d candidates for this contest:", numCandidates());
    for (String option : contestOptions) {
      log("%s", option);
    }
    log("There are %d cast vote records for this contest.", castVoteRecords.size());


    // exhaustedBallots is a map of ballot indexes to the round in which they were exhausted
    Map<Integer, Integer> exhaustedBallots = new HashMap<Integer, Integer>();

    ArrayList<SortedMap<Integer, Set<String>>> sortedRankings = sortCastVoteRecords(castVoteRecords);

    // loop until we achieve a majority winner:
    // at each iteration we will eliminate the lowest-total candidate OR multiple losing candidates if using batch
    // elimination logic (Maine rules)
    while (true) {
      log("Round: %d", finalRound);

      // roundTally is map of candidate ID to vote tallies
      // generated based on previously eliminated candidates contained in eliminatedRound object
      // at each iteration of this loop, the eliminatedRound object will get more entries as candidates are eliminated
      // conversely the roundTally object returned here will contain fewer entries each of which will have more votes
      // eventually a winner will be chosen
      Map<String, Integer> roundTally = getRoundTally(sortedRankings, eliminatedRound, exhaustedBallots, finalRound);
      roundTallies.put(finalRound, roundTally);

      // We fully sort the list in case we want to run batch elimination.
      int totalVotes = 0;
      // map of vote tally to candidate(s).  A list is used to handle ties.
      SortedMap<Integer, LinkedList<String>> countToCandidates = new TreeMap<Integer, LinkedList<String>>();
      // for each candidate record their vote total into the countToCandidates object
      for (String contestOptionId : roundTally.keySet()) {
        int votes = roundTally.get(contestOptionId);

        log("Candidate %s got %d votes.", contestOptionId, votes);

        // count the total votes cast in this round
        totalVotes += votes;
        LinkedList<String> candidates = countToCandidates.get(votes);
        if (candidates == null) {
          candidates = new LinkedList<String>();
          countToCandidates.put(votes, candidates);
        }
        candidates.add(contestOptionId);
      }

      log("Total votes in round %d: %d.", finalRound, totalVotes);

      int maxVotes = countToCandidates.lastKey();
      // Does the leader have a majority of non-exhausted ballots?
      if (maxVotes > (float)totalVotes / 2.0) {
        // we have a winner
        for (Integer votes : countToCandidates.keySet()) {
          // record winner and loser(s)
          if (votes == maxVotes) {
            winner = countToCandidates.get(votes).getFirst();
          } else {
            // Once we have a winner, there's no need to eliminate the runners-up.
            /*
            String loser = countToCandidates.get(votes).getFirst();
            eliminatedRound.put(loser, finalRound);
            log("%s was eliminated in round %d with %d vote(s).", loser, finalRound, votes);
            */
          }
        }
        log("%s won in round %d with %d votes.", winner, finalRound, maxVotes);
        break;
      }

      // container for eliminated candidate(s)
      List<String> eliminated = new LinkedList<String>();

      // Four mutually exclusive ways to eliminate candidates.

      // 1. Some races contain undeclared write-ins that should be dropped immediately.
      if (finalRound == 1 && undeclaredWriteInString != null && contestOptions.contains(undeclaredWriteInString)) {
        eliminated.add(undeclaredWriteInString);
        log(
          "Eliminated %s in round %d because it represents undeclared write-ins. It had %d votes.",
          undeclaredWriteInString,
          finalRound,
          roundTally.get(undeclaredWriteInString)
        );
      }

      // 2. If there's a minimum vote threshold, drop all candidates failing to meet that threshold.
      if (eliminated.isEmpty() && minVoteThreshold != null && countToCandidates.firstKey() < minVoteThreshold) {
        for (int count : countToCandidates.keySet()) {
          if (count < minVoteThreshold) {
            for (String candidate : countToCandidates.get(count)) {
              eliminated.add(candidate);
              log(
                "Eliminated %s in round %d because they only had %d votes, below the minimum threshold of %d.",
                candidate,
                finalRound,
                count,
                minVoteThreshold
              );
            }
          }
        }
      }

      // 3. Otherwise, try batch elimination.
      if (eliminated.isEmpty() && useBatchElimination) {
        List<BatchElimination> batchEliminations = runBatchElimination(countToCandidates);
        // If batch elimination caught multiple candidates, don't apply regular elimination logic on
        // this iteration.
        if (batchEliminations.size() > 1) {
          for (BatchElimination elimination : batchEliminations) {
            eliminated.add(elimination.optionId);
            log(
              "Batch-eliminated %s in round %d. The running total was %d vote(s) and the " +
                "next-highest count was %d vote(s).",
              elimination.optionId,
                finalRound,
              elimination.runningTotal,
              elimination.nextHighestCount
            );
          }
        }
      }

      // 4. And if we didn't do batch elimination, eliminate last place now, breaking a tie if necessary.
      if (eliminated.isEmpty()) {
        String loser;
        int minVotes = countToCandidates.firstKey();
        LinkedList<String> lastPlace = countToCandidates.get(minVotes);
        if (lastPlace.size() > 1) {
          TieBreak tieBreak = new TieBreak(lastPlace);
          loser = tieBreak.getSelection();
          tieBreaks.put(finalRound, tieBreak);
          log(
            "%s lost a tie-breaker in round %d against %s. Each candidate had %d vote(s).",
            loser,
              finalRound,
            tieBreak.nonSelectedString(),
            minVotes
          );
        } else {
          loser = lastPlace.getFirst();
          log(
            "%s was eliminated in round %d with %d vote(s).",
            loser,
              finalRound,
            minVotes
          );
        }
        eliminated.add(loser);
      }

      for (String loser : eliminated) {
        eliminatedRound.put(loser, finalRound);
      }

      finalRound++;
    }

  }

  public void generateSummarySpreadsheet(String outputFile) {
    ResultsWriter writer = new ResultsWriter().
      setNumRounds(finalRound).
      setRoundTallies(roundTallies).
      setCandidatesToRoundEliminated(eliminatedRound).
      setOutputFilePath(outputFile).
      setContestName(contestName).
      setJurisdiction(jurisdiction).
      setOffice(office).
      setElectionDate(electionDate).
      setNumCandidates(numCandidates()).
      setUndeclaredWriteInString(undeclaredWriteInString).
      setWinner(winner);

    writer.generateSummarySpreadsheet();
  }

  public Tabulator setContestName(String contestName) {
    this.contestName = contestName;
    return this;
  }

  public Tabulator setJurisdiction(String jurisdiction) {
    this.jurisdiction = jurisdiction;
    return this;
  }

  public Tabulator setOffice(String office) {
    this.office = office;
    return this;
  }

  public Tabulator setElectionDate(String electionDate) {
    this.electionDate = electionDate;
    return this;
  }

  private void log(String s, Object... var1) {
    RCVLogger.log(s, var1);
  }

  private List<BatchElimination> runBatchElimination(
    SortedMap<Integer, LinkedList<String>> countToCandidates
  ) {
    int runningTotal = 0;
    List<String> candidatesSeen = new LinkedList<String>();
    Set<String> candidatesEliminated = new HashSet<String>();
    List<BatchElimination> eliminations = new LinkedList<BatchElimination>();
    for (int currentVoteCount : countToCandidates.keySet()) {
      if (runningTotal < currentVoteCount) {
        for (String candidate : candidatesSeen) {
          if (!candidatesEliminated.contains(candidate)) {
            candidatesEliminated.add(candidate);
            eliminations.add(
              new BatchElimination(candidate, runningTotal, currentVoteCount)
            );
          }
        }
      }
      List<String> currentCandidates = countToCandidates.get(currentVoteCount);
      runningTotal += currentVoteCount * currentCandidates.size();
      candidatesSeen.addAll(currentCandidates);
    }

    return eliminations;
  }

  private boolean exhaustBallot(
    int ballotIndex,
    int round,
    Map<Integer, Integer> exhaustedBallots,
    String reason
  ) {
    log("Exhausted ballot #%d in round %d due to %s.", ballotIndex, round, reason);
    exhaustedBallots.put(ballotIndex, round);
    return true;
  }

  private void ignoreBallot(
    int ballotIndex,
    int round,
    String reason
  ) {
    log("Ignored ballot #%d in round %d due to %s.", ballotIndex, round, reason);
  }

  private OvervoteDecision getOvervoteDecision(
    Set<String> contestOptionIds,
    Map<String, Integer> eliminatedRound
  ) {
    if (contestOptionIds.size() <= 1) {
      return OvervoteDecision.NONE;
    }

    if (overvoteRule == OvervoteRule.IMMEDIATE_EXHAUSTION) {
      return OvervoteDecision.EXHAUST;
    }

    List<String> continuingAtThisRank = new LinkedList<String>();
    for (String optionId : contestOptionIds) {
      if (eliminatedRound.get(optionId) == null) {
        continuingAtThisRank.add(optionId);
      }
    }

    if (continuingAtThisRank.size() > 0) {
      if (overvoteRule == OvervoteRule.EXHAUST_IF_ANY_CONTINUING) {
        return OvervoteDecision.EXHAUST;
      } else if (overvoteRule == OvervoteRule.IGNORE_IF_ANY_CONTINUING) {
        return OvervoteDecision.IGNORE;
      } else if (continuingAtThisRank.size() > 1) {
        if (overvoteRule == OvervoteRule.EXHAUST_IF_MULTIPLE_CONTINUING) {
          return OvervoteDecision.EXHAUST;
        } else { // if there's > 1 continuing, OvervoteDecision.NONE is not a valid option
          return OvervoteDecision.IGNORE;
        }
      }
    }

    return OvervoteDecision.NONE;
  }

  private boolean hasContinuingCandidates(
    SortedMap<Integer, Set<String>> rankings,
    Map<String, Integer> eliminatedRound
  ) {
    for (Set<String> optionIds : rankings.values()) {
      for (String optionId : optionIds) {
        if (eliminatedRound.get(optionId) == null) {
          return true;
        }
      }
    }

    return false;
  }

  // roundTally returns a map of candidate ID to vote tallies
  // generated based on previously eliminated candidates contained in eliminatedRound object
  // at each iteration of this loop, the eliminatedRound object will get more entries as candidates are eliminated
  // conversely the roundTally object returned here will contain fewer entries each of which will have more votes
  // eventually a winner will be chosen
  private Map<String, Integer> getRoundTally(
    ArrayList<SortedMap<Integer, Set<String>>> allSortedRankings,
    Map<String, Integer> eliminatedRound,
    Map<Integer, Integer> exhaustedBallots,
    int round
  ) throws Exception {
    Map<String, Integer> roundTally = new HashMap<String, Integer>();

    // initialize round tallies for non-eliminated candidates
    for (String contestOptionId : contestOptions) {
      if (eliminatedRound.get(contestOptionId) == null) {
        roundTally.put(contestOptionId, 0);
      }
    }

    // loop over the ballots and count first-place votes, considering only continuing candidates
    for (int i = 0; i < allSortedRankings.size(); i++) {
      if (exhaustedBallots.get(i) != null) {
        continue;
      }

      SortedMap<Integer, Set<String>> rankings = allSortedRankings.get(i);

      if (!hasContinuingCandidates(rankings, eliminatedRound)) {
        exhaustBallot(i, round, exhaustedBallots, "no continuing candidates");
        continue;
      }

      Integer lastRank = null;

      for (int rank : rankings.keySet()) { // loop over the rankings within one ballot
        Set<String> contestOptionIds = rankings.get(rank);

        // handle possible overvote
        OvervoteDecision overvoteDecision = getOvervoteDecision(contestOptionIds, eliminatedRound);
        if (overvoteDecision == OvervoteDecision.EXHAUST) {
          exhaustBallot(i, round, exhaustedBallots, "overvote");
          break;
        } else if (overvoteDecision == OvervoteDecision.IGNORE) {
          ignoreBallot(i, round, "overvote");
          break;
        }

        // and possible undervote
        if (maxNumberOfSkippedRanks != null &&
            lastRank != null && rank - lastRank > maxNumberOfSkippedRanks + 1) {
          exhaustBallot(i, round, exhaustedBallots, "undervote");
          break;
        }

        String selectedOptionId = null;
        for (String optionId : contestOptionIds) {
          if (eliminatedRound.get(optionId) == null) {
            if (selectedOptionId != null) {
              throw new Exception(
                "Our code failed to handle an overvote with multiple continuing candidates properly."
              );
            } else {
              // found a continuing candidate, so increase their tally by 1
              selectedOptionId = optionId;
              roundTally.put(optionId, roundTally.get(optionId) + 1);
            }
          }
        }

        if (selectedOptionId != null) {
          break; // we've found our candidate already
        }
        lastRank = rank;
      } // end looping over the rankings within one ballot
    } // end looping over all ballots

    return roundTally;
  }

  // input is a list of CastVoteRecords
  // output is that same list, but the rankings are sorted from low to high
  private ArrayList<SortedMap<Integer, Set<String>>> sortCastVoteRecords(List<CastVoteRecord> castVoteRecords) {
    // returns a list of "sortedCVRs"
    ArrayList<SortedMap<Integer, Set<String>>> allSortedRankings = new ArrayList<SortedMap<Integer, Set<String>>>();

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

  private int numCandidates() {
    int num = contestOptions.size();
    if (undeclaredWriteInString != null && contestOptions.contains(undeclaredWriteInString)) {
      num--;
    }
    return num;
  }
}
