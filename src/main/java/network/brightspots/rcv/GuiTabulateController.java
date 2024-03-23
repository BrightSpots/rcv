/*
 * RCTab
 * Copyright (c) 2017-2024 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: GUI Controller for tabulate confirmation popup.
 * Design: NA.
 * Conditions: Before GUI tabulation.
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import java.io.File;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Text;

/** View controller for tabulator layout. */
@SuppressWarnings("WeakerAccess")
public class GuiTabulateController {
  /**
   * Once the file is saved, cache it here. It cannot be changed while this modal is open.
   */
  private File savedConfigFile = null;

  /**
   * Cache whether the user is using a temporary config.
   */
  private boolean isSavedConfigFileTemporary = false;

  /**
   * This modal builds upon the GuiConfigController, and therefore only interacts with it.
   */
  private GuiConfigController guiConfigController;

  /**
   * The style applied when a field is filled.
   */
  private String filledFieldStyle;

  /**
   * The style applied when a field is unfilled.
   */
  private String unfilledFieldStyle;

  @FXML private TextArea filepath;
  @FXML private Button saveButton;
  @FXML private Button tempSaveButton;
  @FXML private Label numberOfCandidates;
  @FXML private Label numberOfCvrs;
  @FXML private TextField userNameField;
  @FXML private ProgressBar progressBar;
  @FXML private Button tabulateButton;
  @FXML private Text progressText;

  /**
   * Initialize the GUI with all information required for tabulation.
   */
  public void initialize(GuiConfigController controller, int numCandidates, int numCvrs) {
    guiConfigController = controller;
    numberOfCandidates.setText("Number of candidates: " + numCandidates);
    numberOfCvrs.setText("Number of CVRs: " + numCvrs);
    filledFieldStyle = "";
    unfilledFieldStyle = "-fx-border-color: red;";

    // Allow tempSaveButton to take up no space when hidden
    tempSaveButton.managedProperty().bind(tempSaveButton.visibleProperty());

    initializeSaveButtonStatuses();
    setTabulationButtonStatus();
    updateProgressText();
  }

  /**
   * Action when a letter is typed in the name field
   *
   * @param keyEvent ignored
   */
  public void nameUpdated(KeyEvent keyEvent) {
    updateGuiWithNameEnteredStatus();
    setTabulationButtonStatus();
    updateProgressText();
  }

  /**
   * Action when the tabulate button is clicked.
   *
   * @param actionEvent ignored
   */
  public void buttonTabulateClicked(ActionEvent actionEvent) {
    guiConfigController.startTabulation(
        savedConfigFile.getAbsolutePath(), userNameField.getText(), isSavedConfigFileTemporary);
  }

  /**
   * Action when the save button is clicked.
   *
   * @param actionEvent ignored
   */
  public void buttonSaveClicked(ActionEvent actionEvent) {
    savedConfigFile = guiConfigController.saveFile(false);
    if (savedConfigFile != null) {
      saveButton.setText("Save");
      tempSaveButton.setText("Temp File Saved!");
    }
    updateGuiNotifyConfigSaved();
    setTabulationButtonStatus();
  }

  /**
   * Action when the save button is clicked.
   *
   * @param actionEvent ignored
   */
  public void buttonTempSaveClicked(ActionEvent actionEvent) {
    savedConfigFile = guiConfigController.saveFile(true);
    isSavedConfigFileTemporary = true;
    tempSaveButton.setText("Saved!");
    updateGuiNotifyConfigSaved();
    setTabulationButtonStatus();
  }

  private void setTabulationButtonStatus() {
    if (savedConfigFile != null) {
      // Don't override the progress text unless we're past the Save stage
      updateGuiWithNameEnteredStatus();
    }

    if (savedConfigFile != null && !userNameField.getText().isEmpty()) {
      tabulateButton.setDisable(false);
    } else {
      tabulateButton.setDisable(true);
    }
  }

  private void updateGuiNotifyConfigSaved() {
    if (savedConfigFile == null) {
      throw new RuntimeException("There must be a saved file before calling this function.");
    }

    filepath.setStyle(filledFieldStyle);
    tempSaveButton.setStyle(filledFieldStyle);
    saveButton.setStyle(filledFieldStyle);
    tempSaveButton.setDisable(true);
    saveButton.setDisable(true);
    updateProgressText();
  }

  private void updateGuiWithNameEnteredStatus() {
    if (userNameField.getText().isEmpty()) {
      userNameField.setStyle(unfilledFieldStyle);
    } else {
      userNameField.setStyle(filledFieldStyle);
    }
  }

  private void initializeSaveButtonStatuses() {
    filepath.setText(guiConfigController.getSelectedFile().getAbsolutePath());
    filepath.setScrollLeft(1);
    filepath.setStyle(unfilledFieldStyle);
    saveButton.setStyle(unfilledFieldStyle);
    tempSaveButton.setStyle(unfilledFieldStyle);

    GuiConfigController.ConfigComparisonResult result = guiConfigController.compareConfigs();
    switch (result) {
      case DIFFERENT:
        tempSaveButton.setVisible(false);
        break;
      case DIFFERENT_BUT_VERSION_IS_TEST:
        tempSaveButton.setVisible(true);
        break;
      case SAME:
        saveButton.setText("Saved!");
        savedConfigFile = guiConfigController.getSelectedFile();
        tempSaveButton.setVisible(false);
        updateGuiNotifyConfigSaved();
    }
  }

  private void updateProgressText() {
    if (savedConfigFile == null) {
      progressText.setText("Save the config file to continue.");
    } else if (userNameField.getText().isEmpty()) {
      progressText.setText("Please enter your name to continue.");
    } else {
      progressText.setText("");
    }
  }
}
