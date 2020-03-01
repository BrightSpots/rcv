/*
 * Universal RCV Tabulator
 * Copyright (c) 2017-2020 Bright Spots Developers.
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
import network.brightspots.rcv.Tabulator.TieBreakMode;

class TieBreak {

  private static final String CLI_CANCEL_COMMAND = "x";
  private static Random random;
  private final List<String> allTiedCandidates;
  private final Tabulator.TieBreakMode tieBreakMode;
  // ordering to use if we're doing permutation-based tie-breaking
  private final ArrayList<String> candidatePermutation;
  private final int round;
  // number of votes the tying candidates received
  private final BigDecimal numVotes;
  // roundTallies: map from round number to map of candidate ID to vote total (for that round)
  // e.g. roundTallies[1] contains a map of candidate IDs to tallies for each candidate in round 1
  private final Map<Integer, Map<String, BigDecimal>> roundTallies;
  private final boolean selectingAWinner;
  private String selectedCandidate;
  private String explanation;

  // TieBreak constructor
  // param: selectingAWinner are we determining a winner or loser
  // param: allTiedCandidates list of all candidate IDs tied at this vote total
  // param: tieBreakMode rule to use for selecting the loser/winner
  // param: round in which this tie occurs
  // param: numVotes tally of votes for tying candidates
  // param: roundTallies map from round number to map of candidate ID to vote total (for that round)
  TieBreak(
      boolean selectingAWinner,
      List<String> allTiedCandidates,
      Tabulator.TieBreakMode tieBreakMode,
      int round,
      BigDecimal numVotes,
      Map<Integer, Map<String, BigDecimal>> roundTallies,
      ArrayList<String> candidatePermutation) {
    this.selectingAWinner = selectingAWinner;
    this.allTiedCandidates = allTiedCandidates;
    this.tieBreakMode = tieBreakMode;
    this.round = round;
    this.numVotes = numVotes;
    this.roundTallies = roundTallies;
    this.candidatePermutation = candidatePermutation;

    // sort tied candidates for reproducibility
    Collections.sort(this.allTiedCandidates);
  }

  static void setRandom(Random r) {
    random = r;
  }

  // generate a string listing all tying candidates not selected by this tiebreak
  String nonSelectedCandidateDescription() {
    ArrayList<String> options = new ArrayList<>();
    for (String contestOptionId : allTiedCandidates) {
      if (!contestOptionId.equals(selectedCandidate)) {
        options.add(contestOptionId);
      }
    }
    return Utils.listToSentenceWithQuotes(options);
  }

  String getExplanation() {
    return explanation;
  }

  // execute the tiebreak logic given the tiebreak rule in use
  String selectCandidate() throws TabulationCancelledException {
    switch (tieBreakMode) {
      case INTERACTIVE:
        selectedCandidate = doInteractive(allTiedCandidates);
        break;
      case RANDOM:
        selectedCandidate = doRandom(allTiedCandidates);
        break;
      case GENERATE_PERMUTATION:
      case USE_PERMUTATION_IN_CONFIG:
        selectedCandidate = doPermutationSelection(allTiedCandidates);
        break;
      default:
        selectedCandidate = doPreviousRounds(allTiedCandidates);
    }
    return selectedCandidate;
  }

  // select the candidate based on the provided permutation order
  private String doPermutationSelection(List<String> tiedCandidates) {
    String selection = null;
    Set<String> tiedCandidatesSet = new HashSet<>(tiedCandidates);
    List<String> permutationToSearch = candidatePermutation;
    if (!selectingAWinner) {
      // If we're selecting a loser, we should search the list in reverse.
      permutationToSearch = new ArrayList<>(candidatePermutation);
      Collections.reverse(permutationToSearch);
    }
    // start from beginning of list and look for a matching candidate
    for (String candidate : permutationToSearch) {
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

  // interactively select the winner/loser of this tiebreak via the command-line interface
  private String doInteractiveCli(List<String> tiedCandidates) throws TabulationCancelledException {
    System.out.println(
        String.format(
            "Tie in round %d for the following candidates, each of whom has %d vote(s): ",
            round, numVotes.intValue()));
    for (int i = 0; i < tiedCandidates.size(); i++) {
      System.out.println((i + 1) + ". " + tiedCandidates.get(i));
    }
    final String prompt =
        "Enter the number corresponding to the candidate who should "
            + (selectingAWinner ? "win" : "lose")
            + " this tiebreaker (or "
            + CLI_CANCEL_COMMAND
            + " to cancel): ";
    System.out.println(prompt);

    String selection = null;

    while (selection == null) {
      Scanner sc = new Scanner(System.in);
      String userInput = sc.nextLine();
      if (userInput.equals(CLI_CANCEL_COMMAND)) {
        System.out.println("Cancelling tabulation...");
        throw new TabulationCancelledException();
      }
      try {
        int choice = Integer.parseInt(userInput);
        if (choice >= 1 && choice <= tiedCandidates.size()) {
          // Convert from 1-indexed list back to 0-indexed list.
          selection = tiedCandidates.get(choice - 1);
        }
      } catch (NumberFormatException exception) {
        // if parseInt failed selection will be null and we will retry
      }
      if (selection == null) {
        System.out.println("Invalid selection. Please try again.");
        System.out.println(prompt);
      }
    }

    return selection;
  }

  // interactively select the loser of this tiebreak via the graphical user interface
  private String doInteractiveGui(List<String> tiedCandidates) throws TabulationCancelledException {
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
        GuiTiebreakerPrompt prompt = new GuiTiebreakerPrompt();
        prompt.setTiedCandidates(tiedCandidates);
        FutureTask<GuiTiebreakerPromptResponse> futureTask = new FutureTask<>(prompt);
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

  // interactively select the winner/loser of this tiebreak
  private String doInteractive(List<String> tiedCandidates) throws TabulationCancelledException {
    String selection;
    if (GuiContext.getInstance().getConfig() != null) {
      selection = doInteractiveGui(tiedCandidates);
    } else {
      selection = doInteractiveCli(tiedCandidates);
    }
    explanation = "The selected candidate was supplied by the operator.";
    return selection;
  }

  // randomly select the winner/loser for this tiebreak
  private String doRandom(List<String> tiedCandidates) {
    double randomDouble = random.nextDouble();
    int randomCandidateIndex = (int) Math.floor(randomDouble * (double) tiedCandidates.size());
    explanation = "The candidate was randomly selected.";
    return tiedCandidates.get(randomCandidateIndex);
  }

  // select candidate based on previous round tallies (fallback to interactive or random)
  private String doPreviousRounds(List<String> tiedCandidates) throws TabulationCancelledException {
    String selection = null;
    List<String> candidatesInContention = tiedCandidates;

    for (int roundToCompare = this.round - 1; roundToCompare > 0; roundToCompare--) {
      // map of tally to candidate IDs for round under consideration
      SortedMap<BigDecimal, LinkedList<String>> tallyToCandidates =
          Tabulator.buildTallyToCandidates(
              roundTallies.get(roundToCompare), new HashSet<>(candidatesInContention), false);
      BigDecimal voteTotalForSelection =
          selectingAWinner ? tallyToCandidates.lastKey() : tallyToCandidates.firstKey();
      candidatesInContention = tallyToCandidates.get(voteTotalForSelection);
      if (candidatesInContention.size() == 1) {
        selection = candidatesInContention.get(0);
        explanation =
            String.format(
                "%s had the %s votes (%s) in round %d.",
                selection,
                selectingAWinner ? "most" : "fewest",
                voteTotalForSelection.toString(),
                roundToCompare);
        break;
      } // else keep looping
    }

    // if 2 or more candidates are still tied, we fall back to interactive or random with the
    // remaining candidates
    if (candidatesInContention.size() > 1) {
      String explanationPrefix =
          String.format(
              "Comparing previous round counts still resulted in a tie %s %s, so we fell back to "
                  + "%s.",
              candidatesInContention.size() > 2 ? "among" : "between",
              Utils.listToSentenceWithQuotes(candidatesInContention),
              tieBreakMode == TieBreakMode.PREVIOUS_ROUND_COUNTS_THEN_RANDOM
                  ? "random"
                  : "interactive");
      if (tieBreakMode == TieBreakMode.PREVIOUS_ROUND_COUNTS_THEN_INTERACTIVE) {
        selection = doInteractive(candidatesInContention);
      } else { // PREVIOUS_ROUND_COUNTS_THEN_RANDOM
        selection = doRandom(candidatesInContention);
      }
      explanation = String.format("%s %s", explanationPrefix, explanation);
    }

    return selection;
  }

  class GuiTiebreakerPrompt implements Callable<GuiTiebreakerPromptResponse> {

    private List<String> tiedCandidates;

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

    void setTiedCandidates(List<String> tiedCandidates) {
      this.tiedCandidates = tiedCandidates;
    }
  }

  private static class GuiTiebreakerPromptResponse {

    final boolean tabulationCancelled;
    final String selectedCandidate;

    GuiTiebreakerPromptResponse(boolean tabulationCancelled, String selectedCandidate) {
      this.tabulationCancelled = tabulationCancelled;
      this.selectedCandidate = selectedCandidate;
    }
  }
}
