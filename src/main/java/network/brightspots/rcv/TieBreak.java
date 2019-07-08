/*
 * Universal RCV Tabulator
 * Copyright (c) 2017-2019 Bright Spots Developers.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See
 * the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this
 * program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Purpose:
 * Handle tiebreak scenarios based on rules configuration.
 */

package network.brightspots.rcv;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import network.brightspots.rcv.Tabulator.TabulationCancelledException;

class TieBreak {

  private static Random random;

  // list of candidates who are tied
  private final List<String> tiedCandidates;
  // mode we're using for breaking ties
  private final Tabulator.TieBreakMode tieBreakMode;
  // ordering to use if we're doing permutation-based tie-breaking
  private final ArrayList<String> candidatePermutation;
  // round in which this tiebreak occurred
  private final int round;
  // number of votes the tying candidates received
  private final BigDecimal numVotes;
  // roundTallies: map from round number to map of candidate ID to vote total (for that round)
  // e.g. roundTallies[1] contains a map of candidate IDs to tallies for each candidate in round 1
  private final Map<Integer, Map<String, BigDecimal>> roundTallies;
  private boolean selectingAWinner;
  // candidate ID selected to lose/win the tiebreak
  private String selectedCandidate;
  // reason for the selection
  private String explanation;

  // function: TieBreak
  // purpose: TieBreak constructor stores member data
  // param: selectingAWinner whether we're determining a winner (as opposed to a loser)
  // param: tiedCandidates list of all candidate IDs tied at this vote total
  // param: tieBreakMode rule to use for selecting the loser/winner
  // param: round in which this tie occurs
  // param: numVotes tally of votes for tying candidates
  // param: roundTallies map from round number to map of candidate ID to vote total (for that round)
  // return newly constructed TieBreak object
  TieBreak(
      boolean selectingAWinner,
      List<String> tiedCandidates,
      Tabulator.TieBreakMode tieBreakMode,
      int round,
      BigDecimal numVotes,
      Map<Integer, Map<String, BigDecimal>> roundTallies,
      ArrayList<String> candidatePermutation) {
    this.selectingAWinner = selectingAWinner;
    this.tiedCandidates = tiedCandidates;
    this.tieBreakMode = tieBreakMode;
    this.round = round;
    this.numVotes = numVotes;
    this.roundTallies = roundTallies;
    this.candidatePermutation = candidatePermutation;
    // sort tied candidates for reproducibility
    Collections.sort(this.tiedCandidates);
  }

  static void setRandomSeed(int randomSeed) {
    random = new Random(randomSeed);
  }

  // function: nonSelectedCandidateDescription
  // purpose: generate a string listing candidate(s) not selected by this tiebreak
  // return: string listing candidate(s) not selected
  String nonSelectedCandidateDescription() {
    // options: container for non selected candidate IDs
    ArrayList<String> options = new ArrayList<>();
    // contestOptionId indexes over tied candidates
    for (String contestOptionId : tiedCandidates) {
      if (!contestOptionId.equals(selectedCandidate)) {
        options.add(contestOptionId);
      }
    }
    // container for results
    String nonselected;
    if (options.size() == 1) {
      nonselected = options.get(0);
    } else if (options.size() == 2) {
      // if there are only 2 candidates don't use a comma
      nonselected = options.get(0) + " and " + options.get(1);
    } else {
      // StringBuilder for faster string construction
      StringBuilder stringBuilder = new StringBuilder();
      // i indexes over all candidates
      for (int i = 0; i < options.size() - 1; i++) {
        stringBuilder.append(options.get(i)).append(", ");
      }
      stringBuilder.append("and ").append(options.get(options.size() - 1));
      nonselected = stringBuilder.toString();
    }
    return nonselected;
  }

  String getExplanation() {
    return explanation;
  }

  // function: breakTie
  // purpose: execute the tiebreak logic given the tiebreak rule in use
  // returns: selected candidate
  String selectCandidate() throws TabulationCancelledException {
    switch (tieBreakMode) {
      case INTERACTIVE:
        selectedCandidate = doInteractive();
        break;
      case RANDOM:
        selectedCandidate = doRandom();
        break;
      case GENERATE_PERMUTATION:
      case USE_PERMUTATION_IN_CONFIG:
        selectedCandidate = doPermutationSelection();
        break;
      default:
        // handle tiebreaks that involve previous round tallies
        // selectedCandidate will be set if there is a previous round count winner/loser
        // it will be null if candidates are still tied all the way back to the first round
        selectedCandidate = doPreviousRounds();
        if (selectedCandidate == null) {
          if (tieBreakMode == Tabulator.TieBreakMode.PREVIOUS_ROUND_COUNTS_THEN_INTERACTIVE) {
            selectedCandidate = doInteractive();
          } else {
            // PREVIOUS_ROUND_COUNTS_THEN_RANDOM is handled here
            selectedCandidate = doRandom();
          }
        }
    }
    return selectedCandidate;
  }

  // function: doPermutationSelection
  // purpose: select the candidate based on the provided permutation order
  // returns: selected candidate
  private String doPermutationSelection() {
    String selection = null;
    // create a set to simplify matching logic
    Set<String> tiedCandidatesSet = new HashSet<>(tiedCandidates);

    List<String> permutationToSearch = candidatePermutation;
    if (!selectingAWinner) {
      // If we're selecting a loser, we should search the list in reverse.
      permutationToSearch = new ArrayList<>(candidatePermutation);
      Collections.reverse(permutationToSearch);
    }
    // start from beginning of list and look for a matching candidate
    for (int i = 0; i < permutationToSearch.size(); i++) {
      String candidate = permutationToSearch.get(i);
      if (tiedCandidatesSet.contains(candidate)) {
        selection = candidate;
        break;
      }
    }
    explanation =
        "The selected candidate appeared "
            + (selectingAWinner ? "earliest" : "latest")
            + " in the tie-breaking permutation list.";
    return selection;
  }

  // function doInteractiveCli
  // purpose: interactively select the winner/loser of this tiebreak via the command-line interface
  // returns: selected candidate
  private String doInteractiveCli() throws TabulationCancelledException {
    System.out.println(
        String.format(
            "Tie in round %d for the following candidates, each of whom has %d vote(s): ",
            round, numVotes.intValue()));
    // i: index over tied candidates
    for (int i = 0; i < tiedCandidates.size(); i++) {
      System.out.println((i + 1) + ". " + tiedCandidates.get(i));
    }
    final String CANCEL_COMMAND = "x";
    final String TIEBREAKER_PROMPT =
        "Enter the number corresponding to the candidate who should "
            + (selectingAWinner ? "win" : "lose")
            + " this tiebreaker (or "
            + CANCEL_COMMAND
            + " to cancel): ";
    System.out.println(TIEBREAKER_PROMPT);

    String selection = null;

    while (selection == null) {
      Scanner sc = new Scanner(System.in);
      String userInput = sc.nextLine();
      if (userInput.equals(CANCEL_COMMAND)) {
        System.out.println("Cancelling tabulation...");
        throw new TabulationCancelledException();
      }
      try {
        // user selected candidate parsed to int
        int choice = Integer.parseInt(userInput);
        if (choice >= 1 && choice <= tiedCandidates.size()) {
          // Convert from 1-indexed list back to 0-indexed list.
          selection = tiedCandidates.get(choice - 1);
        }
      } catch (NumberFormatException exception) {
        // if parseInt failed selectedCandidate will be null and we will retry
      }
      if (selection == null) {
        System.out.println("Invalid selection. Please try again.");
        System.out.println(TIEBREAKER_PROMPT);
      }
    }

    return selection;
  }

  // function doInteractiveGui
  // purpose: interactively select the loser of this tiebreak via the graphical user interface
  // returns: selected candidate
  private String doInteractiveGui() throws TabulationCancelledException {
    Logger.log(
        Level.INFO,
        "Tie in round %d for the following candidates, each of whom has %d votes: %s",
        round,
        numVotes.intValue(),
        String.join(", ", tiedCandidates));
    Logger.log(
        Level.INFO,
        "Please use the pop-up window to select the candidate who should "
            + (selectingAWinner ? "win" : "lose")
            + " this tiebreaker.");

    String selection = null;

    while (selection == null) {
      try {
        FutureTask<GuiTiebreakerPromptResponse> futureTask =
            new FutureTask<>(new GuiTiebreakerPrompt());
        Platform.runLater(futureTask);
        GuiTiebreakerPromptResponse guiTiebreakerPromptResponse = futureTask.get();
        if (guiTiebreakerPromptResponse.tabulationCancelled) {
          throw new TabulationCancelledException();
        } else {
          selection = guiTiebreakerPromptResponse.selectedCandidate;
        }
      } catch (InterruptedException | ExecutionException exception) {
        Logger.log(Level.SEVERE, "Failed to get tiebreaker!\n%s", exception.toString());
      }
      if (selection == null) {
        Logger.log(Level.WARNING, "Invalid selection! Please try again.");
      }
    }

    return selection;
  }

  // function doInteractive
  // purpose: interactively select the winner/loser of this tiebreak
  // returns: selected candidate
  private String doInteractive() throws TabulationCancelledException {
    String selection;
    if (GuiContext.getInstance().getConfig() != null) {
      selection = doInteractiveGui();
    } else {
      selection = doInteractiveCli();
    }
    explanation = "The selected candidate was supplied by the operator.";
    return selection;
  }

  // function doRandom
  // purpose: randomly select the winner/loser for this tiebreak
  // returns: selected candidate
  private String doRandom() {
    // random number used for random candidate selection
    double randomDouble = random.nextDouble();
    // index of randomly selected candidate
    int randomCandidateIndex = (int) Math.floor(randomDouble * (double) tiedCandidates.size());
    explanation = "The candidate was randomly selected.";
    return tiedCandidates.get(randomCandidateIndex);
  }

  // function doPreviousRounds
  // purpose: select candidate based on previous round tallies
  // returns: selected candidate
  private String doPreviousRounds() {
    String selection = null;
    // stores candidates still in contention while we iterate through previous rounds to determine
    // the selection
    Set<String> candidatesInContention = new HashSet<>(tiedCandidates);

    for (int round = this.round - 1; round > 0; round--) {
      // map of tally to candidate IDs for round under consideration
      SortedMap<BigDecimal, LinkedList<String>> tallyToCandidates =
          Tabulator.buildTallyToCandidates(roundTallies.get(round), candidatesInContention, false);
      BigDecimal voteTotalForSelection =
          selectingAWinner ? tallyToCandidates.lastKey() : tallyToCandidates.firstKey();
      LinkedList<String> candidatesWithVoteTotal = tallyToCandidates.get(voteTotalForSelection);
      if (candidatesWithVoteTotal.size() == 1) {
        selection = candidatesWithVoteTotal.getFirst();
        explanation =
            String.format(
                "%s had the %s votes (%s) in round %d.",
                selectedCandidate,
                selectingAWinner ? "most" : "fewest",
                voteTotalForSelection.toString(),
                round);
        break;
      } else {
        // update candidatesInContention and check the previous round
        candidatesInContention = new HashSet<>(candidatesWithVoteTotal);
      }
    }

    return selection;
  }

  class GuiTiebreakerPrompt implements Callable<GuiTiebreakerPromptResponse> {

    @Override
    public GuiTiebreakerPromptResponse call() {
      GuiTiebreakerPromptResponse guiTiebreakerPromptResponse = null;
      final Stage window = new Stage();
      window.initModality(Modality.APPLICATION_MODAL);
      window.setTitle("RCV Tiebreaker");
      String resourcePath = "/network/brightspots/rcv/GuiTiebreakerLayout.fxml";
      try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(resourcePath));
        Parent root = loader.load();
        GuiTiebreakerController controller = loader.getController();
        controller.populateTiedCandidates(tiedCandidates);
        controller.populateLabelAndButtonText(selectingAWinner);
        window.setScene(new Scene(root));
        window.showAndWait();
        guiTiebreakerPromptResponse =
            new GuiTiebreakerPromptResponse(
                controller.getTabulationCancelled(), controller.getSelectedCandidate());
      } catch (IOException exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        Logger.log(Level.SEVERE, "Failed to open: %s:\n%s. ", resourcePath, sw.toString());
      }
      return guiTiebreakerPromptResponse;
    }
  }

  private class GuiTiebreakerPromptResponse {

    final boolean tabulationCancelled;
    final String selectedCandidate;

    GuiTiebreakerPromptResponse(boolean tabulationCancelled, String selectedCandidate) {
      this.tabulationCancelled = tabulationCancelled;
      this.selectedCandidate = selectedCandidate;
    }
  }
}
