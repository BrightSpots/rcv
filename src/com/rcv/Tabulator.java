package com.rcv;

import java.io.Console;
import java.security.SecureRandom;
import java.util.*;

public class Tabulator {

  // When the CVR communicates an overvote with an explicit flag, we translate it to a vote for this dummy candidate.
  static String explicitOvervoteLabel = "overvote";

  enum OvervoteRule {
    EXHAUST_IMMEDIATELY,
    ALWAYS_SKIP_TO_NEXT_RANK,
    EXHAUST_IF_ANY_CONTINUING,
    IGNORE_IF_ANY_CONTINUING,
    EXHAUST_IF_MULTIPLE_CONTINUING,
    IGNORE_IF_MULTIPLE_CONTINUING,
    RULE_UNKNOWN
  }

  enum OvervoteDecision {
    NONE,
    EXHAUST,
    IGNORE,
    SKIP_TO_NEXT_RANK,
  }

  enum TieBreakMode {
    RANDOM,
    INTERACTIVE,
    PREVIOUS_ROUND_COUNTS_THEN_RANDOM,
    PREVIOUS_ROUND_COUNTS_THEN_INTERACTIVE,
    MODE_UNKNOWN
  }

  static OvervoteRule overvoteRuleForConfigSetting(String setting) {
    switch (setting) {
      case "always_skip_to_next_rank":
        return OvervoteRule.ALWAYS_SKIP_TO_NEXT_RANK;
      case "exhaust_immediately":
        return OvervoteRule.EXHAUST_IMMEDIATELY;
      default:
        log("Unrecognized overvote rule setting:%s", setting);
        System.exit(1);
    }
    return OvervoteRule.RULE_UNKNOWN;
  }

  static TieBreakMode tieBreakModeForConfigSetting(String setting) {
    switch (setting) {
      case "random":
        return TieBreakMode.RANDOM;
      case "interactive":
        return TieBreakMode.INTERACTIVE;
      case "previous_round_counts_then_random":
        return TieBreakMode.PREVIOUS_ROUND_COUNTS_THEN_RANDOM;
      case "previous_round_counts_then_interactive":
        return TieBreakMode.PREVIOUS_ROUND_COUNTS_THEN_INTERACTIVE;
      default:
        log("Unrecognized tiebreaker mode rule setting: %s", setting);
        System.exit(1);
    }
    return TieBreakMode.MODE_UNKNOWN;
  }

  private List<CastVoteRecord> castVoteRecords;
  private int contestId;
  private List<String> contestOptions;
  private ElectionConfig config;

//  private String contestName;
//  private String jurisdiction;
//  private String office;
//  private String electionDate;

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
    List<String> tiedCandidates;
    TieBreakMode tieBreakMode;
    int round;
    int numVotes;
    Map<Integer, Map<String, Integer>> roundTallies;

    String selection;
    String explanation;

    TieBreak(
      List<String> tiedCandidates,
      TieBreakMode tieBreakMode,
      int round,
      int numVotes,
      Map<Integer, Map<String, Integer>> roundTallies
    ) {
      this.tiedCandidates = tiedCandidates;
      this.tieBreakMode = tieBreakMode;
      this.round = round;
      this.numVotes = numVotes;
      this.roundTallies = roundTallies;
    }

    String getSelection() {
      if (selection == null) {
        selection = breakTie();
      }
      return selection;
    }

    String getExplanation() {
      if (explanation == null) {
        getSelection();
      }
      return explanation;
    }

    String nonSelectedString() {
      ArrayList<String> options = new ArrayList<String>();
      for (String contestOptionId : tiedCandidates) {
        if (!contestOptionId.equals(selection)) {
          options.add(contestOptionId);
        }
      }
      if (options.size() == 1) {
        return options.get(0);
      } else if (options.size() == 2) {
        return options.get(0) + " and " + options.get(1);
      } else {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < options.size() - 1; i++) {
          sb.append(options.get(i)).append(", ");
        }
        sb.append("and ").append(options.get(options.size() - 1));
        return sb.toString();
      }
    }

    private String breakTie() {
      switch (tieBreakMode) {
        case INTERACTIVE:
          return doInteractive();
        case RANDOM:
          return doRandom();
        default:
          String loser = doPreviousRounds();
          if (loser != null) {
            return loser;
          } else if (tieBreakMode == TieBreakMode.PREVIOUS_ROUND_COUNTS_THEN_INTERACTIVE) {
            return doInteractive();
          } else {
            return doRandom();
          }
      }
    }

    private String doInteractive() {
      System.out.println("Tie in round " + round + " for these candidates, each of whom has " + numVotes + " votes:");
      for (int i = 0; i < tiedCandidates.size(); i++) {
        System.out.println((i+1) + ". " + tiedCandidates.get(i));
      }
      Console c = System.console();
      System.out.println("Enter the number corresponding to the candidate who should lose this tiebreaker.");
      while (true) {
        String line = c.readLine();
        try {
          int choice = Integer.parseInt(line);
          if (choice >= 1 && choice <= tiedCandidates.size()) {
            explanation = "The loser was supplied by the operator.";
            return tiedCandidates.get(choice - 1);
          }
        } catch (NumberFormatException e) {
        }
        System.out.println("Invalid selection. Please try again.");
      }
    }

    private String doRandom() {
      // TODO: use java.security.SecureRandom
      double r = Math.random();
      int index = (int)Math.floor(r * (double)tiedCandidates.size());
      explanation = "The loser was randomly selected.";
      return tiedCandidates.get(index);
    }

    private String doPreviousRounds() {
      Set<String> candidatesInContention = new HashSet<>(tiedCandidates);
      for (int roundToCheck = round - 1; roundToCheck > 0; roundToCheck--) {
        SortedMap<Integer, LinkedList<String>> countToCandidates = buildCountToCandidates(
          roundTallies.get(roundToCheck),
          candidatesInContention,
          false
        );
        int minVotes = countToCandidates.firstKey();
        LinkedList<String> candidatesWithLowestTotal = countToCandidates.get(minVotes);
        if (candidatesWithLowestTotal.size() == 1) {
          explanation = "The loser had the fewest votes (" + minVotes + ") in round " + roundToCheck + ".";
          return candidatesWithLowestTotal.getFirst();
        } else {
          candidatesInContention = new HashSet<>(candidatesWithLowestTotal);
        }
      }
      return null;
    }
  }

  private Map<Integer, TieBreak> tieBreaks = new HashMap<>();

  Tabulator(
    List<CastVoteRecord> castVoteRecords,
    int contestId,
    List<String> contestOptions,
    ElectionConfig config
  ) {
    this.castVoteRecords = castVoteRecords;
    this.contestId = contestId;
    this.contestOptions = contestOptions;
    this.config = config;
  }

  public void tabulate() throws Exception {

    log("Beginning tabulation for contest: %d", contestId);
    log("There are %d candidates for this contest:", numCandidates());
    for (String option : contestOptions) {
      log("%s", option);
    }
    log("There are %d cast vote records for this contest.", castVoteRecords.size());

    // add UWI string to contest options so it will be tallied similarly to other candidates
    if (config.undeclaredWriteInLabel() != null) {
      this.contestOptions.add(config.undeclaredWriteInLabel());
    }

    // exhaustedBallots is a map of ballot indexes to the round in which they were exhausted
    Map<Integer, Integer> exhaustedBallots = new HashMap<Integer, Integer>();

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
      Map<String, Integer> roundTally = getRoundTally(castVoteRecords, eliminatedRound, exhaustedBallots, finalRound);
      roundTallies.put(finalRound, roundTally);

      // map of vote tally to candidate(s).  A list is used to handle ties.
      SortedMap<Integer, LinkedList<String>> countToCandidates = buildCountToCandidates(
        roundTally,
        roundTally.keySet(),
        true
      );

      int totalVotes = 0;
      for (int numVotes : roundTally.values()) {
        totalVotes += numVotes;
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
      if (
        finalRound == 1 &&
        config.undeclaredWriteInLabel() != null &&
        contestOptions.contains(config.undeclaredWriteInLabel()) &&
        roundTally.get(config.undeclaredWriteInLabel()) > 0
      ) {
        eliminated.add(config.undeclaredWriteInLabel());
        log(
          "Eliminated %s in round %d because it represents undeclared write-ins. It had %d votes.",
          config.undeclaredWriteInLabel(),
          finalRound,
          roundTally.get(config.undeclaredWriteInLabel())
        );
      }

      // 2. If there's a minimum vote threshold, drop all candidates failing to meet that threshold.
      if (eliminated.isEmpty() && config.minimumVoteThreshold() != null &&
        countToCandidates.firstKey() < config.minimumVoteThreshold()) {
        for (int count : countToCandidates.keySet()) {
          if (count < config.minimumVoteThreshold()) {
            for (String candidate : countToCandidates.get(count)) {
              eliminated.add(candidate);
              log(
                "Eliminated %s in round %d because they only had %d votes, below the minimum threshold of %d.",
                candidate,
                finalRound,
                count,
                config.minimumVoteThreshold()
              );
            }
          }
        }
      }

      // 3. Otherwise, try batch elimination.
      if (eliminated.isEmpty() && config.batchElimination()) {
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
          TieBreak tieBreak = new TieBreak(lastPlace, config.tiebreakMode(), finalRound, minVotes, roundTallies);
          loser = tieBreak.getSelection();
          tieBreaks.put(finalRound, tieBreak);
          log(
            "%s lost a tie-breaker in round %d against %s. Each candidate had %d vote(s). %s",
            loser,
            finalRound,
            tieBreak.nonSelectedString(),
            minVotes,
            tieBreak.getExplanation()
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

  private static SortedMap<Integer, LinkedList<String>> buildCountToCandidates(
    Map<String, Integer> roundTally,
    Set<String> candidatesToInclude,
    boolean shouldLog
  ) {
    SortedMap<Integer, LinkedList<String>> countToCandidates = new TreeMap<>();
    // for each candidate record their vote total into the countToCandidates object
    for (String candidate : candidatesToInclude) {
      int votes = roundTally.get(candidate);

      if (shouldLog) {
        log("Candidate %s got %d votes.", candidate, votes);
      }

      LinkedList<String> candidates = countToCandidates.get(votes);
      if (candidates == null) {
        candidates = new LinkedList<>();
        countToCandidates.put(votes, candidates);
      }
      candidates.add(candidate);
    }

    return countToCandidates;
  }

  public void generateSummarySpreadsheet(String outputFile) {
    ResultsWriter writer = new ResultsWriter().
      setNumRounds(finalRound).
      setRoundTallies(roundTallies).
      setCandidatesToRoundEliminated(eliminatedRound).
      setOutputFilePath(outputFile).
      setContestName(config.contestName()).
      setJurisdiction(config.jurisdiction()).
      setOffice(config.office()).
      setElectionDate(config.electionDate()).
      setNumCandidates(numCandidates()).
      setUndeclaredWriteInString(config.undeclaredWriteInLabel()).
      setWinner(winner);

    writer.generateSummarySpreadsheet();
  }

  static void log(String s, Object... var1) {
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
    String reason,
    CastVoteRecord cvr
  ) {
    String description = String.format("Round %d exhausted ballot #%d due to %s. ", round,ballotIndex, reason);
    cvr.addRoundDescription(description, round);
    exhaustedBallots.put(ballotIndex, round);
    return true;
  }

  private void ignoreBallot(
    int ballotIndex,
    int round,
    String reason,
    CastVoteRecord cvr
  ) {
    String description = String.format("Round %d ignored ballot #%d due to %s. ", round,ballotIndex, reason);
    cvr.addRoundDescription(description, round);
  }


  private OvervoteDecision getOvervoteDecision(
    Set<String> contestOptionIds,
    Map<String, Integer> eliminatedRound
  ) {
    if (contestOptionIds.size() == 0 ||
        (contestOptionIds.size() == 1 && contestOptionIds.toArray()[0] != explicitOvervoteLabel)) {
      return OvervoteDecision.NONE;
    }

    if (config.overvoteRule() == OvervoteRule.EXHAUST_IMMEDIATELY) {
      return OvervoteDecision.EXHAUST;
    } else if (config.overvoteRule() == OvervoteRule.ALWAYS_SKIP_TO_NEXT_RANK) {
      return OvervoteDecision.SKIP_TO_NEXT_RANK;
    }

    List<String> continuingAtThisRank = new LinkedList<String>();
    for (String optionId : contestOptionIds) {
      if (eliminatedRound.get(optionId) == null) {
        continuingAtThisRank.add(optionId);
      }
    }

    if (continuingAtThisRank.size() > 0) {
      if (config.overvoteRule() == OvervoteRule.EXHAUST_IF_ANY_CONTINUING) {
        return OvervoteDecision.EXHAUST;
      } else if (config.overvoteRule() == OvervoteRule.IGNORE_IF_ANY_CONTINUING) {
        return OvervoteDecision.IGNORE;
      } else if (continuingAtThisRank.size() > 1) {
        if (config.overvoteRule() == OvervoteRule.EXHAUST_IF_MULTIPLE_CONTINUING) {
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
    List<CastVoteRecord> castVoteRecords,
    Map<String, Integer> eliminatedRound,
    Map<Integer, Integer> exhaustedBallots,
    int round
  ) throws Exception {
    Map<String, Integer> roundTally = new HashMap<>();

    // initialize round tallies for non-eliminated candidates
    for (String contestOptionId : contestOptions) {
      if (eliminatedRound.get(contestOptionId) == null) {
        roundTally.put(contestOptionId, 0);
      }
    }

    // loop over the ballots and count votes for continuing candidates
    for (int i = 0; i < castVoteRecords.size(); i++) {
      if (exhaustedBallots.get(i) != null) {
        continue;
      }
      CastVoteRecord cvr = castVoteRecords.get(i);
      SortedMap<Integer, Set<String>> rankings = cvr.sortedRankings();

      if (!hasContinuingCandidates(rankings, eliminatedRound)) {
        exhaustBallot(i, round, exhaustedBallots, "no continuing candidates", cvr);
        continue;
      }

      Integer lastRank = null;

      for (int rank : rankings.keySet()) { // loop over the rankings within one ballot
        Set<String> contestOptionIds = rankings.get(rank);

        // handle possible overvote
        OvervoteDecision overvoteDecision = getOvervoteDecision(contestOptionIds, eliminatedRound);
        if (overvoteDecision == OvervoteDecision.EXHAUST) {
          exhaustBallot(i, round, exhaustedBallots, "overvote", cvr);
          break;
        } else if (overvoteDecision == OvervoteDecision.IGNORE) {
          ignoreBallot(i, round, "overvote", cvr);
          break;
        } else if (overvoteDecision == OvervoteDecision.SKIP_TO_NEXT_RANK) {
          continue;
        }

        // and possible undervote
        if (config.maxSkippedRanksAllowed() != null &&
            lastRank != null && rank - lastRank > config.maxSkippedRanksAllowed() + 1) {
          exhaustBallot(i, round, exhaustedBallots, "undervote", cvr);
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
              // TODO: could put this into a helper fxn like exhaust ballot
              selectedOptionId = optionId;
              String description = String.format("Round %d voted for %s ", round, selectedOptionId);
              cvr.addRoundDescription(description, round);
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


  private int numCandidates() {
    int num = contestOptions.size();
    if (config.undeclaredWriteInLabel()!= null &&
      contestOptions.contains(config.undeclaredWriteInLabel())) {
      num--;
    }
    return num;
  }

  public void doAudit(List<CastVoteRecord> castVoteRecords) {
    for(CastVoteRecord cvr : castVoteRecords) {
      log(cvr.getAuditString());
    }
  }


}
