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
    System.out.println(
      "Tie in round " + round + " for these candidateIDs, each of whom has " + numVotes + " votes:"
    );
    for (int i = 0; i < tiedCandidates.size(); i++) {
      System.out.println((i+1) + ". " + tiedCandidates.get(i));
    }
    System.out.println(
      "Enter the number corresponding to the candidate who should lose this tiebreaker."
    );
    Console c = System.console();
    String selectedCandidate = null;
    while (selectedCandidate == null) {
      String line = c.readLine();
      try {
        int choice = Integer.parseInt(line);
        if (choice >= 1 && choice <= tiedCandidates.size()) {
          explanation = "The loser was supplied by the operator.";
          // Convert from 1-indexed list back to 0-indexed list.
          selectedCandidate = tiedCandidates.get(choice - 1);
        }
      } catch (NumberFormatException e) {
      }
      if (selectedCandidate == null) {
        System.out.println("Invalid selection. Please try again.");
      }
    }
    return selectedCandidate;
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
      SortedMap<Integer, LinkedList<String>> countToCandidates = Tabulator.buildTallyToCandidates(
        roundTallies.get(roundToCheck),
        candidatesInContention,
        false
      );
      int minVotes = countToCandidates.firstKey();
      LinkedList<String> candidatesWithLowestTotal = countToCandidates.get(minVotes);
      if (candidatesWithLowestTotal.size() == 1) {
        explanation =
          "The loser had the fewest votes (" + minVotes + ") in round " + roundToCheck + ".";
        selected = candidatesWithLowestTotal.getFirst();
        break;
      } else {
        candidatesInContention = new HashSet<>(candidatesWithLowestTotal);
      }
    }
    return selected;
  }
}
