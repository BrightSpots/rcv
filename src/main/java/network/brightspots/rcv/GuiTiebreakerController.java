/*
 * RCTab
 * Copyright (c) 2017-2022 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: GUI Controller for tiebreak selection during a GUI tabulation.
 * Design: GuiTiebreakerLayout.fxml
 * Conditions: in GUI tabulation when a tiebreak occurs
 * Version history: version 1.0
 * Complete revision history is available at: https://github.com/BrightSpots/rcv
 */

package network.brightspots.rcv;

import java.util.List;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

/**
 * View controller for tiebreaker layout.
 */
@SuppressWarnings("WeakerAccess")
public class GuiTiebreakerController {

  private String selectedCandidate;
  private boolean tabulationCancelled = false;

  @FXML
  private ListView<String> listViewCandidates;
  @FXML
  private Label labelSelectionPrompt;
  @FXML
  private Button buttonSelect;

  String getSelectedCandidate() {
    return selectedCandidate;
  }

  private void setSelectedCandidate(String selectedCandidate) {
    this.selectedCandidate = selectedCandidate;
  }

  boolean getTabulationCancelled() {
    return tabulationCancelled;
  }

  private void setTabulationCancelled(boolean tabulationCancelled) {
    this.tabulationCancelled = tabulationCancelled;
  }

  void populateTiedCandidates(List<String> tiedCandidates) {
    listViewCandidates.setItems(FXCollections.observableArrayList(tiedCandidates));
  }

  void populateLabelAndButtonText(boolean isSelectingWinner) {
    labelSelectionPrompt.setText(
        "Please select a candidate to " + (isSelectingWinner ? "elect" : "eliminate") + ":");
    buttonSelect.setText((isSelectingWinner ? "Elect" : "Eliminate") + " Candidate");
  }

  /**
   * Action when cancel button is clicked.
   *
   * @param actionEvent ignored
   */
  public void buttonCancelClicked(ActionEvent actionEvent) {
    setTabulationCancelled(true);
    ((Stage) ((Node) actionEvent.getSource()).getScene().getWindow()).close();
  }

  /**
   * Action when select button is clicked.
   *
   * @param actionEvent ignored
   */
  public void buttonSelectClicked(ActionEvent actionEvent) {
    if (listViewCandidates.getSelectionModel().getSelectedItems().size() == 1) {
      setSelectedCandidate(listViewCandidates.getSelectionModel().getSelectedItems().get(0));
      ((Stage) ((Node) actionEvent.getSource()).getScene().getWindow()).close();
    }
  }
}
