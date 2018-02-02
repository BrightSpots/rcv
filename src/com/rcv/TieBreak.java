/**
 * Created by Jonathan Moldover on 1/28/18
 * Copyright 2018 Bright Spots
 * Purpose: Handle tie break scenarios based on rules configuration
 * Version: 1.0
 */
package com.rcv;

import java.util.*;

class TieBreak {
  List<String> tiedCandidates;
  Tabulator.TieBreakMode tieBreakMode;
  // round in which this tiebreak occurred
  int round;
  // number of votes tying candidates received
  int numVotes;
  // roundTallies is a map from round number to a map from candidate ID to vote total for the round
  // e.g. roundTallies[1] contains a map of candidate IDs to votes for each candidate in round 1
  Map<Integer, Map<String, Integer>> roundTallies;
  // candidate ID selected to lose
  String loser;
  // reason for the loser
  String explanation;

  // function: TieBreak
  // purpose: TieBreak constructor will store data for the tiebreak and select a loser
  // param: tiedCandidates list of all candidate IDs tied at this vote total
  // param: tieBreakMode rule to use for selecting the loser
  // param: round in which this tie occurs
  // param: numVotes tally of votes for tying candidates
  // param: roundTallies map from round number to a map from candidate ID to vote
  //  total for the round
  // return newly constructed TieBreak object
  TieBreak(
    List<String> tiedCandidates,
    Tabulator.TieBreakMode tieBreakMode,
    int round,
    int numVotes,
    Map<Integer, Map<String, Integer>> roundTallies
  ) {
    this.tiedCandidates = tiedCandidates;
    this.tieBreakMode = tieBreakMode;
    this.round = round;
    this.numVotes = numVotes;
    this.roundTallies = roundTallies;
    this.loser = breakTie();
  }
  
  String nonLosingCandidateDescription() {
    ArrayList<String> options = new ArrayList<>();
    for (String contestOptionId : tiedCandidates) {
      if (!contestOptionId.equals(loser)) {
        options.add(contestOptionId);
      }
    }
    String nonselected;
    if (options.size() == 1) {
      nonselected = options.get(0);
    } else if (options.size() == 2) {
      nonselected = options.get(0) + " and " + options.get(1);
    } else {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < options.size() - 1; i++) {
        sb.append(options.get(i)).append(", ");
      }
      sb.append("and ").append(options.get(options.size() - 1));
      nonselected = sb.toString();
    }
    return nonselected;
  }

  // function: breakTie
  // purpose: execute the tiebreak logic given the tiebreak rule in use
  // returns: losing candidate
  private String breakTie() {
    // return value
    String losingCandidate;
    switch (tieBreakMode) {
      case INTERACTIVE:
        losingCandidate = doInteractive();
        break;
      case RANDOM:
        losingCandidate = doRandom();
        break;
      default:
        // TODO: rewrite this 
        // handle tiebreaks which involve previous round tallies

        // loser will be set if there is a previous round count loser
        // it will be null if candidates were still tied at first round
        String loser = doPreviousRounds();
        if (loser != null) {
          losingCandidate = loser;
        } else if (tieBreakMode == Tabulator.TieBreakMode.PREVIOUS_ROUND_COUNTS_THEN_INTERACTIVE) {
          losingCandidate = doInteractive();
        } else {
          losingCandidate = doRandom();
        }
    }
    return losingCandidate;
  }

  // function doInteraction
  // purpose: interactively select the loser of this tiebreak
  // return: candidateID of the selected loser
  private String doInteractive() {
    System.out.println(
      "Tie in round " + round + " for the following candidateIDs each of whom has " + numVotes + " votes:"
    );
    for (int i = 0; i < tiedCandidates.size(); i++) {
      System.out.println((i+1) + ". " + tiedCandidates.get(i));
    }
    System.out.println(
      "Enter the number corresponding to the candidate who should lose this tiebreaker."
    );
    // the candidate selected to lose
    String selectedCandidate = null;
    while (selectedCandidate == null) {
      // whatever the user entered in console
      String userInput = System.console().readLine();
      try {
        // user loser parsed to int
        int choice = Integer.parseInt(userInput);
        if (choice >= 1 && choice <= tiedCandidates.size()) {
          explanation = "The losing candidate was supplied by the operator.";
          // Convert from 1-indexed list back to 0-indexed list.
          selectedCandidate = tiedCandidates.get(choice - 1);
        }
      } catch (NumberFormatException e) {
        // if parseInt failed selectedCandidate will be null and we will retry
      }
      if (selectedCandidate == null) {
        System.out.println("Invalid selection. Please try again.");
      }
    }
    return selectedCandidate;
  }

  // function doRandom
  // purpose: randomly select the loser for this tiebreak
  // return: candidateID of the selected loser
  private String doRandom() {
    // TODO: use java.security.SecureRandom
    // random number used for random candidate ID loser
    double randomNormalFloat = Math.random();
    // index of randomly selected candidate
    int randomCandidateIndex = (int)Math.floor(randomNormalFloat * (double)tiedCandidates.size());
    explanation = "The loser was randomly selected.";
    return tiedCandidates.get(randomCandidateIndex);
  }

  // function doPreviousRounds
  // purpose: select loser based on previous round tallies
  // return: candidateID of the selected loser
  private String doPreviousRounds() {
    // stores candidates still in contention while we iterate through previous rounds to determine
    // the loser
    Set<String> candidatesInContention = new HashSet<>(tiedCandidates);
    // the candidate selected to lose
    String loser = null;
    // round indexes from the previous round back to round 1
    for (int round = this.round - 1; round > 0; round--) {
      // map of tally to candidate(s) for the round under consideration
      SortedMap<Integer, LinkedList<String>> tallyToCandidates = Tabulator.buildTallyToCandidates(
        roundTallies.get(round),
        candidatesInContention,
        false
      );
      // lowest tally for this round
      int minVotes = tallyToCandidates.firstKey();
      // candidates receiving the lowest tally
      LinkedList<String> candidatesWithLowestTotal = tallyToCandidates.get(minVotes);
      if (candidatesWithLowestTotal.size() == 1) {
        loser = candidatesWithLowestTotal.getFirst();
        explanation = "%s had the fewest votes (%d) in round %d.".format(loser, minVotes, round);
        break;
      } else {
        // update candidatesInContention and check the previous round
        candidatesInContention = new HashSet<>(candidatesWithLowestTotal);
      }
    }
    return loser;
  }
}
