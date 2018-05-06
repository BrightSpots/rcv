/**
 * Created by Jonathan Moldover and Louis Eisenberg
 * Copyright 2018 Bright Spots
 * Purpose: perform ranked choice tabulation calculations
 * round-by-round tabulation of votes to each candidate
 * handles overvote / undervote decisions batch elimination and tiebreaks
 * results are logged to console and audit file
 * Version: 1.0
 */

package com.rcv;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class Tabulator {

  // When the CVR contains an overvote we "normalize" it to use this string
  static String explicitOvervoteLabel = "overvote";

  // vote transfer rule to use in multi-seat elections
  enum MultiSeatTransferRule {
    TRANSFER_FRACTIONAL_SURPLUS,
    TRANSFER_WHOLE_SURPLUS,
    TRANSFER_RULE_UNKNOWN
  }

  // OvervoteRule determines how overvotes are handled
  enum OvervoteRule {
    EXHAUST_IMMEDIATELY,
    ALWAYS_SKIP_TO_NEXT_RANK,
    EXHAUST_IF_ANY_CONTINUING,
    IGNORE_IF_ANY_CONTINUING,
    EXHAUST_IF_MULTIPLE_CONTINUING,
    IGNORE_IF_MULTIPLE_CONTINUING,
    RULE_UNKNOWN
  }

  // OvervoteDecision is the result of applying an OvervoteRule to a CVR in a particular round
  enum OvervoteDecision {
    NONE,
    EXHAUST,
    IGNORE,
    SKIP_TO_NEXT_RANK,
  }

  // TieBreakMode determines how ties will be handled
  enum TieBreakMode {
    RANDOM,
    INTERACTIVE,
    PREVIOUS_ROUND_COUNTS_THEN_RANDOM,
    PREVIOUS_ROUND_COUNTS_THEN_INTERACTIVE,
    MODE_UNKNOWN
  }

  enum CandidateStatus {
    CONTINUING,
    WINNER,
    ELIMINATED,
  }

  // cast vote records parsed from CVR input files
  private List<CastVoteRecord> castVoteRecords;
  // all candidateIDs for this election parsed from the election config (does not include UWIs)
  private List<String> candidateIDs;
  // election config contains specific rules and file paths to be used during tabulation
  private ElectionConfig config;

  // roundTallies is a map from round number to a map from candidate ID to vote total for the round
  // e.g. roundTallies[1] contains a map of all candidate ID -> votes for each candidate in round 1
  // this structure is computed over the course of tabulation
  private Map<Integer, Map<String, BigDecimal>> roundTallies = new HashMap<>();

  // candidateToRoundEliminated is a map from candidate ID to round in which they were eliminated
  private Map<String, Integer> candidateToRoundEliminated = new HashMap<>();

  // map from candidate ID to the round in which they won
  private Map<String, Integer> winnerToRound = new HashMap<>();

  // when tabulation is complete, this will be how many rounds it took to determine the winner(s)
  private int currentRound = 0;

  // simple container class used during batch elimination process to store the results
  // for later logging output
  static class BatchElimination {
    // the candidate eliminated
    String candidateID;
    // how many total votes were totaled when this candidate was eliminated
    BigDecimal runningTotal;
    // next highest count total (validates that we were correctly batch eliminated)
    BigDecimal nextHighestTally;

    // function: BatchElimination constructor
    // purpose: create a new BatchElimination object simple container
    // param: candidateID the candidate eliminated
    // param: runningTotal how many total votes were totaled when this candidate was eliminated
    // param: nextHighestTally next highest count total
    // returns: the new object
    BatchElimination(String candidateID, BigDecimal runningTotal, BigDecimal nextHighestTally) {
      this.candidateID = candidateID;
      this.runningTotal = runningTotal;
      this.nextHighestTally = nextHighestTally;
    }
  }

  // map of round to TieBreak objects to record how tiebreaks were decided
  private Map<Integer, TieBreak> roundToTieBreak = new HashMap<>();

  // function: Tabulator constructor
  // purpose: assigns input params to member variables and caches the candidateIDlist
  // which will be used when reading input cast vote records
  // param: castVoteRecords list of all cast vote records to be tabulated for this contest
  // param: config describes various tabulation rules to be used for tabulation
  // returns: the new object
  Tabulator(
      List<CastVoteRecord> castVoteRecords,
      ElectionConfig config
  ) {
    this.castVoteRecords = castVoteRecords;
    this.candidateIDs = config.getCandidateCodeList();
    this.config = config;
  }

  // purpose: run the main tabulation routine to determine election results
  //  this is the high-level control of the tabulation algorithm
  public void tabulate() {

    log("Beginning tabulation for contest.");
    log("There are %d declared candidates for this contest:", config.numDeclaredCandidates());
    // string indexes over all candidate IDs to log them
    for (String candidateID : candidateIDs) {
      log("%s", candidateID);
    }
    log("There are %d cast vote records for this contest.", castVoteRecords.size());

    // add UWI string to candidateIDs so it will be tallied similarly to other candidates
    if (config.undeclaredWriteInLabel() != null) {
      this.candidateIDs.add(config.undeclaredWriteInLabel());
    }

    // Loop until we've found our winner(s) unless using
    // continueTabulationUntilTwoCandidatesRemain in which case loop until only two candidates remain
    // At each iteration, we'll either a) identify one or more
    // winners and transfer their votes to the remaining candidates (if we still need to find more
    // winners), or b) eliminate one or more candidates and gradually transfer votes to the
    // remaining candidates.
    while (shouldContinueTabulating()) {
      currentRound++;
      log("Round: %d", currentRound);

      // currentRoundCandidateToTally is a map from candidateID to vote tally for the current round.
      // At each iteration of this loop that involves eliminating candidates, the eliminatedRound
      // object will gain entries.
      // Conversely, the currentRoundCandidateToTally object returned here will contain fewer
      // entries, each of which will have as many or more votes than they did in prior rounds.
      // Eventually the winner(s) will be chosen.
      Map<String, BigDecimal> currentRoundCandidateToTally = getTallyForRound(currentRound);
      roundTallies.put(currentRound, currentRoundCandidateToTally);

      // cache this as it will change after adding winners to winnerToRound
      // TODO: better encapsulation
      BigDecimal winningThresholdThisRound = getThreshold(currentRoundCandidateToTally);

      // currentRoundTallyToCandidates is a sorted map from tally to candidate(s) with that tally.
      SortedMap<BigDecimal, LinkedList<String>> currentRoundTallyToCandidates = buildTallyToCandidates(
        currentRoundCandidateToTally,
        currentRoundCandidateToTally.keySet(),
        true
      );
      // see if a winner is determined in this iteration
      List<String> winners = identifyWinners(currentRoundCandidateToTally, currentRoundTallyToCandidates);

      if (winners.size() > 0) {
        for (String winner : winners) {
          winnerToRound.put(winner, currentRound);
        }
        for (String winner : winners) {
          // are there still more winners to select in future rounds?
          if (winnerToRound.size() < config.numberOfWinners()) {
            // fractional transfer based on surplus votes
            BigDecimal candidateVotes = currentRoundCandidateToTally.get(winner);
            BigDecimal extraVotes =
                candidateVotes.subtract(winningThresholdThisRound, config.mathContext());
            BigDecimal surplusFraction = extraVotes.divide(candidateVotes, config.mathContext());
            for (CastVoteRecord cvr : castVoteRecords) {
              if (winner.equals(cvr.getCurrentRecipientOfVote())) {
                cvr.setFractionalTransferValue(
                    cvr.getFractionalTransferValue().multiply(surplusFraction,
                        config.mathContext()));
              }
            }
          }
        }
      } else {  // if no winners in this round, determine who will be eliminated
        // container for eliminated candidate(s)
        List<String> eliminated = new LinkedList<>();

        // Four mutually exclusive ways to eliminate candidates.
        // 1. Some races contain undeclared write-ins that should be dropped immediately.
        boolean foundCandidateToEliminate = dropUWI(eliminated, currentRoundCandidateToTally);
        // 2. If there's a minimum vote threshold, drop all candidates below that threshold.
        if (!foundCandidateToEliminate) {
          foundCandidateToEliminate =
            dropCandidatesBelowThreshold(eliminated, currentRoundTallyToCandidates);
        }
        // 3. Otherwise, try batch elimination.
        if (!foundCandidateToEliminate) {
          foundCandidateToEliminate = doBatchElimination(eliminated, currentRoundTallyToCandidates);
        }
        // 4. If we didn't do batch elimination, eliminate the remaining candidate with the lowest
        //    tally, breaking a tie if needed.
        if (!foundCandidateToEliminate) {
          foundCandidateToEliminate =
            doRegularElimination(eliminated, currentRoundTallyToCandidates);
        }

        // If we failed to eliminate anyone, there's a bug in the code.
        assert foundCandidateToEliminate;

        // store the losers
        for (String loser : eliminated) {
          candidateToRoundEliminated.put(loser, currentRound);
        }
      }
    }
  }

  // function: getThreshold
  // purpose: determine the threshold to win for the given round
  // param: currentRoundCandidateToTally map of candidateID to their tally for a particular round
  // param: winnerToRound map of candidateID to round in which they won
  // return: threshold to determine a winner
  private BigDecimal getThreshold(Map<String, BigDecimal> currentRoundCandidateToTally) {
    // currentRoundTotalVotes holds total active votes in this round
    BigDecimal currentRoundTotalVotes = BigDecimal.ZERO;
    // numVotes indexes over all vote tallies in this round
    for (BigDecimal numVotes : currentRoundCandidateToTally.values()) {
      currentRoundTotalVotes = currentRoundTotalVotes.add(numVotes, config.mathContext());
    }
    // how many seats have been filled
    int numPreviousWinners = winnerToRound.size();
    // how many seats remain to be filled
    int seatsRemaining = config.numberOfWinners() - numPreviousWinners;

    // divisor for threshold is seats remaining to fill + 1
    BigDecimal divisor = new BigDecimal(seatsRemaining + 1);
    // return value
    BigDecimal threshold = currentRoundTotalVotes.divide(divisor, config.mathContext());
    return threshold;
  }

  // purpose: determine if we should continue tabulating based on how many winners have been
  // selected and if continueTabulationUntilTwoCandidatesRemain flag is in use.
  // return: true if we should continue tabulating
  private boolean shouldContinueTabulating() {
    // how many candidates have already been eliminated
    int eliminatedCandidates = candidateToRoundEliminated.keySet().size();
    // how many winners have been selected
    int winners = winnerToRound.size();
    
    if(config.continueUntilTwoCandidatesRemain()) {
      return (eliminatedCandidates + winners) < config.numCandidates();
    } else {
      return winners < config.numberOfWinners();
    }
  }
  
  // function: isCandidateContinuing
  // purpose: returns true if candidate is continuing with respect to tabulation
  // this handles continued tabulation after a winner has been chosen for the
  // continueTabulationUntilTwoCandidatesRemain setting
  // returns: true if we should continue tabulating
  private Boolean isCandidateContinuing(String candidate) {
    CandidateStatus status = getCandidateStatus(candidate);
    return(status == CandidateStatus.CONTINUING ||
        (status == CandidateStatus.WINNER && config.continueUntilTwoCandidatesRemain()));
  }

  // function: getCandidateStatus
  // purpose: returns candidate status (continuing, eliminated or winner)
  // returns: candidate status
  private CandidateStatus getCandidateStatus(String candidate) {
    CandidateStatus status = CandidateStatus.CONTINUING;
    if (winnerToRound.containsKey(candidate)) {
      status = CandidateStatus.WINNER;
    } else if (candidateToRoundEliminated.containsKey(candidate)) {
      status = CandidateStatus.ELIMINATED;
    }
    return status;
  }

  // function: identifyWinners
  // purpose: determine if one or more winners have been identified in this round
  // param: currentRoundCandidateToTally map of candidateID to their tally in a particular round
  // param: currentRoundTallyToCandidates map of tally to candidate ID(s) for a particular round
  // return: list of winning candidates in this round (if any)
  private List<String> identifyWinners(
    Map<String, BigDecimal> currentRoundCandidateToTally,
    SortedMap<BigDecimal, LinkedList<String>> currentRoundTallyToCandidates
  ) {
    // store result here
    List<String> selectedWinners = new LinkedList<>();
    // winning threshold this round
    BigDecimal thresholdToWin = getThreshold(currentRoundCandidateToTally);
    // tally indexes over all tallies to find any winners
    for (BigDecimal tally : currentRoundTallyToCandidates.keySet()) {
      // TODO: some rules require >= than here
      if (tally.compareTo(thresholdToWin) == 1) {
        // we have winner(s)
        List<String> winningCandidates = currentRoundTallyToCandidates.get(tally);
        for(String winningCandidate : winningCandidates) {
          selectedWinners.add(winningCandidate);
          log("%s won in round %d with %s votes.", winningCandidate, currentRound,
              tally.toString());
          break;
        }
      }
    }
    return selectedWinners;
  }

  // function: dropUWI
  // purpose: eliminate all undeclared write in candidates
  // param: on exit eliminated will contain newly eliminated undeclared write in candidate IDs
  // param: currentRoundCandidateToTally map of candidate IDs to their tally for a given round
  // returns: true if any candidates were eliminated
  private boolean dropUWI(
    List<String> eliminated,
    Map<String, BigDecimal> currentRoundCandidateToTally
  ) {
    if (
      currentRound == 1 &&
      config.undeclaredWriteInLabel() != null &&
      candidateIDs.contains(config.undeclaredWriteInLabel()) &&
      currentRoundCandidateToTally.get(config.undeclaredWriteInLabel()).signum() == 1
    ) {
      eliminated.add(config.undeclaredWriteInLabel());
      log(
        "Eliminated %s in round %d because it represents undeclared write-ins. It had %s votes.",
        config.undeclaredWriteInLabel(),
        currentRound,
        currentRoundCandidateToTally.get(config.undeclaredWriteInLabel()).toString()
      );
    }
    return !eliminated.isEmpty();
  }

  // function: dropCandidatesBelowThreshold
  // purpose: eliminate all candidates below a certain tally threshold
  // param: eliminated will contain any newly eliminated candidate IDs
  // param: currentRoundTallyToCandidates map of tally to candidate IDs for a given round
  // returns: true if any candidates were eliminated
  private boolean dropCandidatesBelowThreshold(
    List<String> eliminated,
    SortedMap<BigDecimal, LinkedList<String>> currentRoundTallyToCandidates
  ) {
    if (
      eliminated.isEmpty() && // <-- TODO: Louis should this condition be here?
      config.minimumVoteThreshold().signum() == 1 &&
      currentRoundTallyToCandidates.firstKey().compareTo(config.minimumVoteThreshold()) == -1
    ) {
      // tally indexes over all tallies in the current round
      for (BigDecimal tally : currentRoundTallyToCandidates.keySet()) {
        if (tally.compareTo(config.minimumVoteThreshold()) == -1) {
          // candidate indexes over all candidates who received this tally
          for (String candidate : currentRoundTallyToCandidates.get(tally)) {
            eliminated.add(candidate);
            log(
              "Eliminated %s in round %d because they only had %s vote(s), below the minimum " +
                "threshold of %s.",
              candidate,
              currentRound,
              tally.toString(),
              config.minimumVoteThreshold().toString()
            );
          }
        } else {
          break;
        }
      }
    }
    return !eliminated.isEmpty();
  }

  // function: doBatchElimination
  // purpose: eliminate all candidates who are mathematically unable to win
  // param: eliminated will contain any newly eliminated candidate IDs
  // param: currentRoundTallyToCandidates map of tally to candidate IDs for a given round
  // returns: true if any candidates were eliminated
  private boolean doBatchElimination(
    List<String> eliminated,
    SortedMap<BigDecimal, LinkedList<String>> currentRoundTallyToCandidates
  ) {
    if (eliminated.isEmpty() && config.batchElimination()) {
      // container for results
      List<BatchElimination> batchEliminations = runBatchElimination(currentRoundTallyToCandidates);
      if (batchEliminations.size() > 1) {
        // elimination iterates over all BatchElimination objects describing the eliminations
        for (BatchElimination elimination : batchEliminations) {
          eliminated.add(elimination.candidateID);
          log(
            "Batch-eliminated %s in round %d. The running total was %s vote(s) and the " +
              "next-highest count was %s vote(s).",
            elimination.candidateID,
            currentRound,
            elimination.runningTotal.toString(),
            elimination.nextHighestTally.toString()
          );
        }
      }
    }
    return !eliminated.isEmpty();
  }

  // function: doRegularElimination
  // purpose: eliminate candidate with the lowest tally using tiebreak if necessary
  // param: eliminated will contain newly eliminated candidate ID
  // param: currentRoundTallyToCandidates map of tally to candidate IDs for a given round
  // returns: true if any candidates were eliminated
  private boolean doRegularElimination(
    List<String> eliminated,
    SortedMap<BigDecimal, LinkedList<String>> currentRoundTallyToCandidates
  ) {
    if (eliminated.isEmpty()) {
      // eliminated candidate
      String eliminatedCandidate;
      // lowest tally in this round
      BigDecimal minVotes = currentRoundTallyToCandidates.firstKey();
      // list of candidates receiving the lowest tally
      LinkedList<String> lastPlaceCandidates = currentRoundTallyToCandidates.get(minVotes);
      if (lastPlaceCandidates.size() > 1) {
        // there was a tie for last place
        // create new tieBreak object to pick a loser
        TieBreak tieBreak =
          new TieBreak(lastPlaceCandidates,
          config.tiebreakMode(),
          currentRound,
          minVotes,
          roundTallies);

        // results of tiebreak stored here
        eliminatedCandidate = tieBreak.loser();
        roundToTieBreak.put(currentRound, tieBreak);
        log(
          "%s lost a tie-breaker in round %d against %s. Each candidate had %s vote(s). %s",
          eliminatedCandidate,
          currentRound,
          tieBreak.nonLosingCandidateDescription(),
          minVotes.toString(),
          tieBreak.explanation()
        );
      } else {
        // last place candidate will be eliminated
        eliminatedCandidate = lastPlaceCandidates.getFirst();
        log("%s was eliminated in round %d with %s vote(s).",
          eliminatedCandidate,
          currentRound,
          minVotes.toString()
        );
      }
      eliminated.add(eliminatedCandidate);
    }
    return !eliminated.isEmpty();
  }

  // function: generateSummarySpreadsheet
  // purpose: create a ResultsWriter object with the tabulation results data and use it
  //  to generate the results spreadsheet
  // param: outputFile path to write the output file to
  public void generateSummarySpreadsheet() {
    // writer object will create the output xls
    ResultsWriter writer = new ResultsWriter().
      setNumRounds(currentRound).
      setRoundTallies(roundTallies).
      setCandidatesToRoundEliminated(candidateToRoundEliminated).
      setWinnerToRound(winnerToRound).
      setElectionConfig(config);

    writer.generateSummarySpreadsheet();
  }

  // function: log
  // purpose: helper function for logging to console and audit
  // param: s input s to be logged
  // param: var1 ... objects to be formatted into the log output
  static void log(String s, Object... var1) {
    Logger.log(s, var1);
  }

  // Function: runBatchElimination
  // Purpose: applies batch elimination logic to the input vote counts to remove multiple candidates
  //   in a single round if their vote counts are so low that they could not possibly end up winning
  //   Consider, after each round of voting a candidate not eliminated could potentially receive ALL
  //   the votes from candidates who ARE eliminated, keeping them in the race and "leapfrogging"
  //   ahead of candidates who were leading them.
  //   In this algorithm we sum candidate vote totals (low to high) and find where this leapfrogging
  //   is impossible: that is, when the sum of all batch-eliminated candidates' votes fails to equal
  //   or exceed the next-lowest candidate vote total.
  //
  // param: currentRoundTallyToCandidates map from vote tally to candidates with that tally
  // returns: list of BatchElimination objects, one for each batch-eliminated candidate
  private List<BatchElimination> runBatchElimination(
    SortedMap<BigDecimal, LinkedList<String>> currentRoundTallyToCandidates
  ) {
    // The sum total of all vote counts examined. This must equal or exceed the next-lowest
    // candidate tally to prevent batch elimination.
    BigDecimal runningTotal = BigDecimal.ZERO;
    // Tracks candidates whose totals have been included in the runningTotal and thus are being
    // considered for batch elimination.
    List<String> candidatesSeen = new LinkedList<>();
    // Tracks candidates who have been batch-eliminated (to prevent duplicate eliminations).
    Set<String> candidatesEliminated = new HashSet<>();
    // BatchElimination objects contain contextual data that will be used by the tabulation to log
    // the batch elimination results.
    List<BatchElimination> eliminations = new LinkedList<>();
    // At each iteration, currentVoteTally is the next-lowest vote count received by one or more
    // candidate(s) in the current round.
    for (BigDecimal currentVoteTally : currentRoundTallyToCandidates.keySet()) {
      // Test whether leapfrogging is possible.
      if (runningTotal.compareTo(currentVoteTally) == -1) {
        // Not possible, so eliminate everyone who has been seen and not eliminated yet.
        // candidate indexes over all seen candidates
        for (String candidate : candidatesSeen) {
          if (!candidatesEliminated.contains(candidate)) {
            candidatesEliminated.add(candidate);
            eliminations.add(new BatchElimination(candidate, runningTotal, currentVoteTally));
          }
        }
      }
      // Add the candidates for the currentVoteTally to the seen list and accumulate their votes.
      // currentCandidates is all candidates receiving the current vote tally
      List<String> currentCandidates = currentRoundTallyToCandidates.get(currentVoteTally);
      BigDecimal totalForThisRound = currentVoteTally.multiply(new BigDecimal(currentCandidates.size()));
      runningTotal = runningTotal.add(totalForThisRound,config.mathContext());
      candidatesSeen.addAll(currentCandidates);
    }
    return eliminations;
  }

  // purpose: determine if any overvote has occurred for this ranking set (from a cvr)
  // and if so return how to handle it based on the rules configuration in use
  // param: candidateIDSet all candidates this cvr contains at a particular rank
  // return: an OvervoteDecision enum to be applied to the cvr under consideration
  private OvervoteDecision getOvervoteDecision(Set<String> candidateIDSet) {
    // the resulting decision
    OvervoteDecision decision;
    // the rule we're using
    OvervoteRule rule = config.overvoteRule();

    // does this set include the explicit overvote label?
    boolean explicitOvervote = candidateIDSet.contains(explicitOvervoteLabel);
    if (explicitOvervote) {
      // we should never have the explicit overvote flag AND other candidates for a given ranking
      assert candidateIDSet.size() == 1;

      // if we have an explicit overvote, the only valid rules are exhaust immediately or
      // always skip. (this is enforced when we load the config also)
      assert rule == OvervoteRule.EXHAUST_IMMEDIATELY ||
        rule == OvervoteRule.ALWAYS_SKIP_TO_NEXT_RANK;

      if (rule == OvervoteRule.EXHAUST_IMMEDIATELY) {
        decision = OvervoteDecision.EXHAUST;
      } else {
        decision = OvervoteDecision.SKIP_TO_NEXT_RANK;
      }
    } else if (candidateIDSet.size() <= 1) {
      // if undervote or one vote which is not the overvote label, then there is no overvote
      decision = OvervoteDecision.NONE;
    } else if (rule == OvervoteRule.EXHAUST_IMMEDIATELY) {
      decision = OvervoteDecision.EXHAUST;
    } else if (rule == OvervoteRule.ALWAYS_SKIP_TO_NEXT_RANK) {
      decision = OvervoteDecision.SKIP_TO_NEXT_RANK;
    } else {
      // if we got here, there are multiple candidates, and the decision depends on how the rule
      // handles continuing candidates

      // build a list of all continuing candidates from the input set
      List<String> continuingAtThisRank = new LinkedList<>();
      // candidateID indexes over all candidate IDs in this ranking set
      for (String candidateID : candidateIDSet) {
        if (isCandidateContinuing(candidateID)) {
          continuingAtThisRank.add(candidateID);
        }
      }

      if (continuingAtThisRank.size() > 0) {
        // at least 1 continuing candidate
        if (rule == OvervoteRule.EXHAUST_IF_ANY_CONTINUING) {
          decision = OvervoteDecision.EXHAUST;
        } else if (rule == OvervoteRule.IGNORE_IF_ANY_CONTINUING) {
          decision = OvervoteDecision.IGNORE;
        } else if (continuingAtThisRank.size() > 1) {
          // multiple continuing candidates at this rank
          if (rule == OvervoteRule.EXHAUST_IF_MULTIPLE_CONTINUING) {
            decision = OvervoteDecision.EXHAUST;
          } else {
            // if there's > 1 continuing, OvervoteDecision.NONE is not a valid option
            decision = OvervoteDecision.IGNORE;
          }
        } else {
          // exactly 1 continuing candidate at this rank
          decision = OvervoteDecision.NONE;
        }
      } else {
        // no continuing candidates at this rank
        decision = OvervoteDecision.NONE;
      }
    }

    return decision;
  }

  // function: hasContinuingCnadidates
  // purpose: determine if the input rankings specify a candidate who has not been eliminated
  //   i.e. a continuing candidate
  // param: rankToCandidateIDs ordered map of rankings (most preferred to least) to candidateIDs
  // a set is used to accommodate overvotes
  // return: true if there is a continuing candidate otherwise false
  private boolean hasContinuingCandidates(SortedMap<Integer, Set<String>> rankToCandidateIDs) {
    // the result of this function
    boolean foundContinuingCandidate = false;
    // iterate through all candidateID sets
    for (Set<String> candidateIDSet : rankToCandidateIDs.values()) {
      // iterate through all candidateIDs in the set
      for (String candidateID : candidateIDSet) {
        // see if the candidate has been eliminated
        if (isCandidateContinuing(candidateID)) {
          foundContinuingCandidate = true;
          break;
        }
      }
      if (foundContinuingCandidate) {
        break;
      }
    }
    return foundContinuingCandidate;
  }

  // function: getTallyForRound
  // purpose: return a map of candidate ID to vote tallies for this round
  //   generated based on previously eliminated candidateIDs contained in
  //   candidateToRoundEliminated object.
  //   After each call the candidateToRoundEliminated object will get
  //   more entries as candidateIDs are eliminated.
  //   Conversely the roundTally object returned here will contain fewer entries each
  //   of which will have more votes.
  // param: the current round
  // return: map of candidateID to vote tallies for this round
  private Map<String, BigDecimal> getTallyForRound(int currentRound) {
    // map of candidateID to vote tally to store the results
    Map<String, BigDecimal> roundTally = new HashMap<>();

    // initialize round tallies to 0 for all continuing candidates
    // candidateID indexes through all candidateIDs
    for (String candidateID : candidateIDs) {
      if (isCandidateContinuing(candidateID)) {
        roundTally.put(candidateID, BigDecimal.ZERO);
      }
    }

    // cvr indexes over the cast vote records to count votes for continuing candidateIDs
    for (CastVoteRecord cvr : castVoteRecords) {
      cvr.setCurrentRecipientOfVote(null);
      if (cvr.isExhausted()) {
        continue;
      }
      // if this cvr has no continuing candidate exhaust it
      if (!hasContinuingCandidates(cvr.rankToCandidateIDs)) {
        cvr.exhaust(currentRound, "no continuing candidates");
        continue;
      }

      // lastRank tracks the last rank in this rankings set as we iterate through it
      // this is used to determine how many skipped rankings occurred in the case of
      // undervotes
      int lastRank = 0;
      // loop over all rankings in this cvr from most preferred to least and see how they will
      // be rendered
      // rank iterates through all ranks in this cvr ranking set
      for (int rank : cvr.rankToCandidateIDs.keySet()) {
        // candidateIDSet is all candidates selected at the current rank
        Set<String> candidateIDSet = cvr.rankToCandidateIDs.get(rank);

        // check for an overvote
        // overvoteDecision is the overvote decision for this ranking
        OvervoteDecision overvoteDecision = getOvervoteDecision(candidateIDSet);
        if (overvoteDecision == OvervoteDecision.EXHAUST) {
          cvr.exhaust(currentRound, "overvote");
          break;
        } else if (overvoteDecision == OvervoteDecision.IGNORE) {
          // description of the overvote decision
          String description = String.format("%d|ignored:%s|", currentRound, "overvote");
          cvr.addRoundDescription(description, currentRound);
          break;
        } else if (overvoteDecision == OvervoteDecision.SKIP_TO_NEXT_RANK) {
          continue;
        }

        // check for an undervote
        if ((config.maxSkippedRanksAllowed() != null) &&
            (rank - lastRank > config.maxSkippedRanksAllowed() + 1))
        {
          cvr.exhaust(currentRound, "undervote");
          break;
        }

        // selectedCandidateID for this rank
        String selectedCandidateID = null;
        // candidateID indexes through all candidates selected at this rank
        for (String candidateID : candidateIDSet) {
          // skip non-continuing candidates
          if (isCandidateContinuing(candidateID)) {
            // If this fails, it means the code failed to handle an overvote with multiple
            // continuing candidates.
            assert selectedCandidateID == null;
            // we found a continuing candidate, so increase their tally by 1
            selectedCandidateID = candidateID;
            // text description of the vote
            String description = String.format("%d|%s|", currentRound, selectedCandidateID);
            cvr.addRoundDescription(description, currentRound);
            // Increment the tally for this candidate by the fractional transfer value of the CVR.
            // (By default the FTV is exactly one vote, but it could be less than one in a multi-
            // winner election if this CVR already helped elect a winner.)

            // current tally for this candidate
            BigDecimal currentTally = roundTally.get(selectedCandidateID);
            // new tally after adding this vote
            BigDecimal newTally = currentTally.add(cvr.getFractionalTransferValue(),
                config.mathContext());
            roundTally.put(selectedCandidateID, newTally);
            cvr.setCurrentRecipientOfVote(selectedCandidateID);
          }
        }

        if (selectedCandidateID != null) {
          // we've found our candidate
          break;
        }
        lastRank = rank;
      } // end looping over the rankings within one ballot
    } // end looping over all ballots

    return roundTally;
  }

  // function: doAudit
  // purpose: log the audit info to console and audit file
  // param: castVoteRecords list of all cvrs which have been tabulated
  public void doAudit(List<CastVoteRecord> castVoteRecords) {
    for (CastVoteRecord cvr : castVoteRecords) {
      log(cvr.getAuditString());
    }
  }

  // function: buildTallyToCandidates
  // purpose: utility function to "invert" the input map of candidateID to tally
  //   into a sorted map of tally to List of candidateIDs.
  //   A list is used because multiple candidates may have the same tally.
  //   This is used to determine when winners are selected and for running tiebreak logic.
  // param: roundTally input map of candidateID to tally for a particular round
  // param candidatesToInclude: list of candidateIDs which may be included in the output.
  //   This filters out candidates when running a tiebreak tabulation which relies
  //   on the tied candidate's previous round totals to break the tie.
  // param: shouldLog is set to log to console and log file
  // return: sorted map of tally to List of candidateIDs drawn from the input data and excluding
  //   candidates not appearing in candidatesToInclude)
  public static SortedMap<BigDecimal, LinkedList<String>> buildTallyToCandidates(
    Map<String, BigDecimal> roundTally,
    Set<String> candidatesToInclude,
    boolean shouldLog
  ) {
    // output map structure containing the map of vote tally to candidate(s)
    SortedMap<BigDecimal, LinkedList<String>> tallyToCandidates = new TreeMap<>();
    // for each candidate record their vote total into the countToCandidates object
    // candidate is the current candidate as we iterate all candidates under consideration
    for (String candidate : candidatesToInclude) {
      // vote count for this candidate
      BigDecimal votes = roundTally.get(candidate);
      if (shouldLog) {
        Logger.log("Candidate %s got %s votes.", candidate, votes.toString());
      }
      // all candidates in the existing output structure (if any) who received the same vote tally
      LinkedList<String> candidates = tallyToCandidates.get(votes);
      if (candidates == null) {
        // new container list for candidates who recieved this vote tally
        candidates = new LinkedList<>();
        tallyToCandidates.put(votes, candidates);
      }
      candidates.add(candidate);
    }
    return tallyToCandidates;
  }
}
