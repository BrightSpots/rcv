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

import java.io.IOException;
import javafx.concurrent.Service;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.Window;

/** View controller for tabulator layout. */
@SuppressWarnings("WeakerAccess")
public class GuiTabulateController {
  /**
   * The button text to display before tabulation begins.
   */
  private static final String buttonTabulateText = "Tabulate";

  /**
   * The button text to display while tabulation is in progress.
   */
  private static final String buttonTabulateInProgressText = "Tabulating...";

  /**
   * The button text to display after tabulation successfully completes.
   */
  private static final String buttonOpenResultsText = "Open Results Folder";

  /**
   * The button text to display after tabulation fails.
   */
  private static final String buttonViewErrorLogsText = "View Error Logs";

  /**
   * Once the file is saved, cache it here. It cannot be changed while this modal is open.
   */
  private String savedConfigFilePath = null;

  /**
   * Cache whether the user is using a temporary config.
   */
  private boolean isSavedConfigFileTemporary = false;

  /**
   * The output folder of the tabulated config.
   * It's important to cache this, since Temp Configs will be deleted after tabulation
   * and we won't know this value without re-reading the GUI state.
   */
  private String configOutputPath;

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
   * Action when a letter is typed in the name field.
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
    switch (tabulateButton.getText()) {
      case buttonTabulateText:
        ContestConfig config = ContestConfig.loadContestConfig(savedConfigFilePath);
        configOutputPath = config.getOutputDirectory();
        if (!configOutputPath.endsWith("/")) {
          configOutputPath += "/";
        }

        Service<Boolean> service = guiConfigController.startTabulation(
                savedConfigFilePath, userNameField.getText(), isSavedConfigFileTemporary);
        // Dispatch a function that watches the service and updates the progress bar
        watchServiceProgress(service);
        break;
      case buttonOpenResultsText:
        openOutputDirectoryInFileExplorer();
        break;
      case buttonViewErrorLogsText:
        // Close the window
        Window window = tabulateButton.getScene().getWindow();
        ((Stage) window).close();
        break;
      default:
        throw new IllegalStateException("Unexpected value: " + tabulateButton.getText());
    }
  }

  private void watchServiceProgress(Service<Boolean> service) {
    progressBar.progressProperty().bind(service.progressProperty());
    tabulateButton.setText(buttonTabulateInProgressText);
    tabulateButton.setDisable(true);
    userNameField.setDisable(true);

    // This is a litle hacky -- we want two listeners on setOnSucceded,
    // so we enforce that this callback is added second.
    EventHandler<WorkerStateEvent> originalCallback = service.getOnSucceeded();
    if (originalCallback == null) {
      throw new RuntimeException("Programming Error: Java does not allow multiple listeners, "
              + "so this listener must be called after the other listener is added.");
    }

    service.setOnSucceeded(workerStateEvent -> {
      originalCallback.handle(workerStateEvent);
      progressBar.progressProperty().unbind();
      tabulateButton.setDisable(false);

      boolean succeeded = service.getValue();
      if (succeeded) {
        progressBar.setProgress(1);
        tabulateButton.setText(buttonOpenResultsText);
      } else {
        progressBar.setProgress(0);
        tabulateButton.setText(buttonViewErrorLogsText);
      }
    });
  }

  /**
   * Action when the save button is clicked.
   *
   * @param actionEvent ignored
   */
  public void buttonSaveClicked(ActionEvent actionEvent) {
    savedConfigFilePath = guiConfigController.saveFile(saveButton, false);
    if (savedConfigFilePath != null) {
      saveButton.setText("Save");
      tempSaveButton.setText("Temp File Saved!");
      filepath.setText(savedConfigFilePath);
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
    savedConfigFilePath = guiConfigController.saveFile(tempSaveButton, true);
    isSavedConfigFileTemporary = true;
    tempSaveButton.setText("Saved!");
    updateGuiNotifyConfigSaved();
    setTabulationButtonStatus();
  }

  private void setTabulationButtonStatus() {
    if (savedConfigFilePath != null) {
      // Don't override the progress text unless we're past the Save stage
      updateGuiWithNameEnteredStatus();
    }

    if (savedConfigFilePath != null && !userNameField.getText().isEmpty()) {
      tabulateButton.setDisable(false);
    } else {
      tabulateButton.setDisable(true);
    }
  }

  private void updateGuiNotifyConfigSaved() {
    if (savedConfigFilePath == null) {
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
    filepath.setText(guiConfigController.getSelectedFilePath());
    filepath.positionCaret(filepath.getLength() - 1); // scroll to end
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
        savedConfigFilePath = guiConfigController.getSelectedFilePath();
        tempSaveButton.setVisible(false);
        updateGuiNotifyConfigSaved();
        break;
      default:
        throw new IllegalStateException("Unexpected value: " + result);
    }
  }

  private void updateProgressText() {
    if (savedConfigFilePath == null) {
      progressText.setText("Save the config file to continue.");
    } else if (userNameField.getText().isEmpty()) {
      progressText.setText("Please enter your name to continue.");
    } else {
      progressText.setText("");
    }
  }

  private void openOutputDirectoryInFileExplorer() {
    String[] cmd = new String[]{"open", "-R", configOutputPath};

    try {
      Runtime.getRuntime().exec(cmd, null);
    } catch (IOException e) {
      Logger.warning("Failed to open file explorer: " + e.getMessage());
    }
  }
}
