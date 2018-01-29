/**
 * Created by Jonathan Moldover on 1/28/18
 * Copyright 2018 Bright Spots
 * Purpose:
 * Version: 1.0
 */
package com.rcv;

import java.io.Console;
import java.util.*;

class TieBreak {
  List<String> tiedCandidates;
  Tabulator.TieBreakMode tieBreakMode;
  int round;
  int numVotes;
  Map<Integer, Map<String, Integer>> roundTallies;

  String selection;
  String explanation;

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

  String nonselectedString() {
    ArrayList<String> options = new ArrayList<String>();
    for (String contestOptionId : tiedCandidates) {
      if (!contestOptionId.equals(selection)) {
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

  private String breakTie() {
    String selection;
    switch (tieBreakMode) {
      case INTERACTIVE:
        selection = doInteractive();
        break;
      case RANDOM:
        selection = doRandom();
        break;
      default:
        String loser = doPreviousRounds();
        if (loser != null) {
          selection = loser;
        } else if (tieBreakMode == Tabulator.TieBreakMode.PREVIOUS_ROUND_COUNTS_THEN_INTERACTIVE) {
          selection = doInteractive();
        } else {
          selection = doRandom();
        }
    }
    return selection;
  }

  private String doInteractive() {
    System.out.println("Tie in round " + round + " for these candidateIDs, each of whom has " + numVotes + " votes:");
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

  // purpose: utility function to "invert" the input map of candidateID => tally
  // into a sorted map of tally => List<candidateID>
  // a list is used because multiple candidates may have the same (tying) tally
  // this is used to determine when a final winner is picked, and running tiebreak logic
  // param: roundTally
  //  input map of candidateID to tally for a particular round
  // param candidatesToInclude:
  //  input list of candidateIDs may appear in the output.
  //  This filters out candidates when running a tiebreak tabulation which relies
  //  on the tied candidate's previous round totals to break the tie
  // param: soundLog
  //  if set output the candidate tally to console and audit
  // return: map of tally => List<candidateID> from the input data (excluding candidates
  //  not appearing in candidatesToInclude
  public static SortedMap<Integer, LinkedList<String>> buildTallyToCandidates(
      Map<String, Integer> roundTally,
      Set<String> candidatesToInclude,
      boolean shouldLog
  ) {
    // output map structure containing the map of vote tally to candidate(s)
    SortedMap<Integer, LinkedList<String>> tallyToCandidates = new TreeMap<>();
    // for each candidate record their vote total into the countToCandidates object
    // candidate is the current candidate as we iterate all candidates under consideration
    for (String candidate : candidatesToInclude) {
      // vote count for this candidate
      int votes = roundTally.get(candidate);
      if (shouldLog) {
        Logger.log("Candidate %s got %d votes.", candidate, votes);
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

  private String doRandom() {
    // TODO: use java.security.SecureRandom
    double r = Math.random();
    int index = (int)Math.floor(r * (double)tiedCandidates.size());
    explanation = "The loser was randomly selected.";
    return tiedCandidates.get(index);
  }

  private String doPreviousRounds() {
    Set<String> candidatesInContention = new HashSet<>(tiedCandidates);
    String selected = null;
    for (int roundToCheck = round - 1; roundToCheck > 0; roundToCheck--) {
      SortedMap<Integer, LinkedList<String>> countToCandidates = buildTallyToCandidates(
          roundTallies.get(roundToCheck),
          candidatesInContention,
          false
      );
      int minVotes = countToCandidates.firstKey();
      LinkedList<String> candidatesWithLowestTotal = countToCandidates.get(minVotes);
      if (candidatesWithLowestTotal.size() == 1) {
        explanation = "The loser had the fewest votes (" + minVotes + ") in round " + roundToCheck + ".";
        selected = candidatesWithLowestTotal.getFirst();
        break;
      } else {
        candidatesInContention = new HashSet<>(candidatesWithLowestTotal);
      }
    }
    return selected;
  }


}
