/*
 * RCTab
 * Copyright (c) 2017-2023 Bright Spots Developers.
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
import java.io.IOException;
import java.nio.file.Files;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Service;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Pair;
import network.brightspots.rcv.TabulatorSession.LoadedCvrData;

/** View controller for tabulator layout. */
@SuppressWarnings("WeakerAccess")
public class GuiTabulateController {
  /**
   * Once the file is saved, cache it here. It cannot be changed while this modal is open. Do not
   * rely on this variable alone -- it should be used in conjunction with
   * useTemporaryConfigBeforeTabulation. It will be null if the Save Temp File button was used.
   */
  private String savedConfigFilePath = null;

  /**
   * If true, savedConfigFilePath will be null and we will load and save a temporary config file on
   * each tabulation.
   */
  private boolean useTemporaryConfigBeforeTabulation = false;

  /**
   * The output folder of the tabulated config. It's important to cache this, since Temp Configs
   * will be deleted after tabulation and we won't know this value without re-reading the GUI state.
   */
  private String configOutputPath;

  /**
   * This modal builds upon the GuiConfigController, and the two share some functionality (e.g.
   * saving a config file). The shared functionality lives in GuiConfigController.
   */
  private GuiConfigController guiConfigController;

  /** The style applied when a required field is correctly filled. */
  private String filledFieldStyle;

  /** The style applied when a required field is unfilled and requires user input. */
  private String unfilledFieldStyle;

  /** If the tabulation task succeeded. */
  private boolean tabulationSucceeded = false;

  /** Last-loaded CVR metadata, with the CVRs themselves discarded from memory. */
  private LoadedCvrData lastLoadedCvrData;

  @FXML private TextArea filepath;
  @FXML private Button saveButton;
  @FXML private Button tempSaveButton;
  @FXML private Label numberOfCandidates;
  @FXML private Label numberOfCvrFiles;
  @FXML private TableView<Pair<String, String>> perSourceCvrCountTable;
  @FXML private TableColumn<Pair<String, String>, String> perSourceCvrColumnFilepath;
  @FXML private TableColumn<Pair<String, String>, String> perSourceCvrColumnCount;
  @FXML private TextField userNameField;
  @FXML private ProgressBar progressBar;
  @FXML private Button loadCvrButton;
  @FXML private Button tabulateButton;
  @FXML private Button openResultsButton;
  @FXML private Text progressText;

  /** Initialize the GUI with all information required for tabulation. */
  public void initialize(GuiConfigController controller, int numCandidates, int numCvrs) {
    guiConfigController = controller;
    numberOfCandidates.setText("Number of Candidates: " + numCandidates);
    numberOfCvrFiles.setText("Number of CVR Files: " + numCvrs);
    filledFieldStyle = "";
    unfilledFieldStyle = "-fx-border-color: red;";

    // Allow tempSaveButton to take up no space when hidden
    tempSaveButton.managedProperty().bind(tempSaveButton.visibleProperty());

    perSourceCvrColumnFilepath.setCellValueFactory(
            c -> new SimpleStringProperty(c.getValue().getKey()));
    perSourceCvrColumnCount.setCellValueFactory(
            c -> new SimpleStringProperty(c.getValue().getValue()));
    perSourceCvrCountTable.getColumns().add(0,
            GuiConfigController.NumberTableCellFactory.createNumberColumn("#", 1));
    perSourceCvrCountTable.setVisible(false);

    initializeSaveButtonStatuses();
    setTabulationButtonStatus();
    updateProgressText();
  }

  /**
   * Call this before closing the window to delete any lingering temp files.
   */
  public void cleanUp() {
    if (useTemporaryConfigBeforeTabulation) {
      try {
        File tempFile = guiConfigController.getTemporaryFile();
        Files.deleteIfExists(tempFile.toPath());
      } catch (IOException e) {
        Logger.severe("Could not delete temporary config file: " + e.getMessage());
        throw new RuntimeException(e);
      }
    }
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
   * Action when the Load CVRs button is clicked.
   *
   * @param actionEvent ignored
   */
  public void buttonLoadCvrsClicked(ActionEvent actionEvent) {
    String configPath = getConfigPathOrCreateTempFile();

    enableButtonsUpTo(null);
    tabulateButton.setText("Tabulate");
    Service<LoadedCvrData> service = guiConfigController.parseAndCountCastVoteRecords(configPath);

    watchParseCvrServiceProgress(service);
  }

  /**
   * Action when the tabulate button is clicked.
   *
   * @param actionEvent ignored
   */
  public void buttonTabulateClicked(ActionEvent actionEvent) {
    String configPath = getConfigPathOrCreateTempFile();

    Service<Boolean> service =
        guiConfigController.startTabulation(
            configPath,
            userNameField.getText(),
            useTemporaryConfigBeforeTabulation,
            lastLoadedCvrData);

    watchTabulatorServiceProgress(service);
  }

  /**
   * Action when Open Results button is clicked.
   *
   * @param actionEvent ignored
   */
  public void buttonOpenResultsClicked(ActionEvent actionEvent) {
    if (tabulationSucceeded) {
      openOutputDirectoryInFileExplorer();
    } else {
      // Close the window
      Window window = tabulateButton.getScene().getWindow();
      ((Stage) window).close();
    }
  }

  private void watchTabulatorServiceProgress(Service<Boolean> service) {
    // Measure time it takes for the function to complete
    long startTime = System.currentTimeMillis();

    // Prevent the user from closing the window while tabulation is in progress
    Window window = tabulateButton.getScene().getWindow();
    Stage stage = ((Stage) window);
    stage.setOnCloseRequest(event -> event.consume());

    EventHandler<WorkerStateEvent> onSucceededEvent =
        workerStateEvent -> {
          tabulationSucceeded = service.getValue();
          if (tabulationSucceeded) {
            openResultsButton.setText("Open Results Folder");
          } else {
            openResultsButton.setText("Close and View Errors");
          }
          enableButtonsUpTo(openResultsButton);
          stage.setOnCloseRequest(null);
          long endTime = System.currentTimeMillis();
          Logger.info("Tabulation took " + (endTime - startTime) / 1000.0 + " seconds.");
        };
    watchGenericService(service, onSucceededEvent);
  }

  private void watchParseCvrServiceProgress(Service<LoadedCvrData> service) {
    EventHandler<WorkerStateEvent> onSucceededEvent =
        workerStateEvent -> {
          enableButtonsUpTo(tabulateButton);
          LoadedCvrData data = service.getValue();
          tabulateButton.setText("Tabulate " + String.format("%,d", data.numCvrs()) + " ballots");

          // Populate the per-source CVR count table, and calculate the width of the filename column
          perSourceCvrCountTable.getItems().clear();
          int maxFilenameLength = 0;
          for (ResultsWriter.CvrSourceData sourceData : data.getCvrSourcesData()) {
            String countString = String.format("%,d", sourceData.getNumCvrs());
            String fileString = new File(sourceData.source.getFilePath()).getName();
            perSourceCvrCountTable.getItems().add(new Pair<>(fileString, countString));
            maxFilenameLength = Math.max(maxFilenameLength, fileString.length());
          }
          perSourceCvrColumnFilepath.setPrefWidth(getEstWidthForStringInPixels(maxFilenameLength));
          perSourceCvrCountTable.setVisible(true);

          data.discard();
          lastLoadedCvrData = data;
        };

    watchGenericService(service, onSucceededEvent);
  }

  private float getEstWidthForStringInPixels(int stringLength) {
    return stringLength * 7.5f;
  }

  private <T> void watchGenericService(
      Service<T> service, EventHandler<WorkerStateEvent> onSuccessCallback) {
    progressBar.progressProperty().bind(service.progressProperty());
    enableButtonsUpTo(null);

    // Don't let user close the modal while the service is running
    Window window = tabulateButton.getScene().getWindow();
    Stage stage = ((Stage) window);
    stage.setOnCloseRequest(event -> event.consume());

    // This is a little hacky -- we want two listeners on setOnSucceded,
    // so we enforce that this callback is added second.
    EventHandler<WorkerStateEvent> originalCallback = service.getOnSucceeded();
    if (originalCallback == null) {
      throw new RuntimeException(
          "Programming Error: Java does not allow multiple listeners, "
              + "so this listener must be called after the other listener is added.");
    }

    service.setOnSucceeded(
        workerStateEvent -> {
          progressBar.progressProperty().unbind();
          progressBar.setProgress(1);
          stage.setOnCloseRequest(null);
          originalCallback.handle(workerStateEvent);
          onSuccessCallback.handle(workerStateEvent);
        });
    service.setOnFailed(
        workerStateEvent -> {
          stage.setOnCloseRequest(null);
        });
  }

  /**
   * In the list of three buttons, Read > Tabulate > Open, sets the buttons up until this button as
   * enabled.
   *
   * @param button Last button to be enabled, or null to disable all buttons
   */
  private void enableButtonsUpTo(Button button) {
    loadCvrButton.setDisable(true);
    tabulateButton.setDisable(true);
    openResultsButton.setDisable(true);

    if (button == loadCvrButton) {
      loadCvrButton.setDisable(false);
    } else if (button == tabulateButton) {
      loadCvrButton.setDisable(false);
      tabulateButton.setDisable(false);
    } else if (button == openResultsButton) {
      loadCvrButton.setDisable(false);
      tabulateButton.setDisable(false);
      openResultsButton.setDisable(false);
    } else if (button != null) {
      throw new IllegalArgumentException("Invalid button");
    }
  }

  /**
   * Action when the save button is clicked.
   *
   * @param actionEvent ignored
   */
  public void buttonSaveClicked(ActionEvent actionEvent) {
    savedConfigFilePath = guiConfigController.saveFile(saveButton, false);
    if (isConfigSavedOrTempFileReady()) {
      saveButton.setText("Saved!");
      tempSaveButton.setVisible(false);
      filepath.setText(savedConfigFilePath);
      updateGuiNotifyConfigSaved();
    }
    setTabulationButtonStatus();
  }

  /**
   * Action when the save button is clicked.
   *
   * @param actionEvent ignored
   */
  public void buttonTempSaveClicked(ActionEvent actionEvent) {
    useTemporaryConfigBeforeTabulation = true;
    saveButton.setText("Saved!");
    tempSaveButton.setVisible(false);
    updateGuiNotifyConfigSaved();
    setTabulationButtonStatus();
  }

  private String getConfigPathOrCreateTempFile() {
    String path =
        useTemporaryConfigBeforeTabulation
            ? guiConfigController.saveFile(tempSaveButton, true)
            : savedConfigFilePath;

    ContestConfig config = ContestConfig.loadContestConfig(path);
    configOutputPath = config.getOutputDirectory();
    String os = System.getProperty("os.name").toLowerCase();
    if (!os.contains("win") && !configOutputPath.endsWith("/")) {
      configOutputPath += "/";
    }

    return path;
  }

  private boolean isConfigSavedOrTempFileReady() {
    return savedConfigFilePath != null || useTemporaryConfigBeforeTabulation;
  }

  private void setTabulationButtonStatus() {
    if (isConfigSavedOrTempFileReady()) {
      // Don't override the progress text unless we're past the Save stage
      updateGuiWithNameEnteredStatus();
    }

    if (isConfigSavedOrTempFileReady() && !userNameField.getText().isBlank()) {
      enableButtonsUpTo(loadCvrButton);
    } else {
      enableButtonsUpTo(null);
    }
  }

  private void updateGuiNotifyConfigSaved() {
    filepath.setStyle(filledFieldStyle);
    tempSaveButton.setStyle(filledFieldStyle);
    saveButton.setStyle(filledFieldStyle);
    tempSaveButton.setDisable(true);
    saveButton.setDisable(true);
    updateProgressText();
  }

  private void updateGuiWithNameEnteredStatus() {
    if (userNameField.getText().isBlank()) {
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
      case GUI_IS_EMPTY:
        saveButton.setText("Save*");
        tempSaveButton.setVisible(false);
        break;
      case DIFFERENT_BUT_VERSION_IS_TEST:
        saveButton.setText("Save*");
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
    if (!isConfigSavedOrTempFileReady()) {
      progressText.setText("Save the config file to continue.");
    } else if (userNameField.getText().isBlank()) {
      progressText.setText("Enter your name to continue.");
    } else {
      progressText.setText("");
    }
  }

  private void openOutputDirectoryInFileExplorer() {
    try {
      String os = System.getProperty("os.name").toLowerCase();
      if (os.contains("win")) {
        // Windows
        Runtime.getRuntime().exec(new String[]{"explorer.exe", "/select,", configOutputPath});
      } else if (os.contains("mac")) {
        // MacOS
        Runtime.getRuntime().exec(new String[]{"open", "-R", configOutputPath});
      } else if (os.contains("nix") || os.contains("nux")) {
        // Linux
        Runtime.getRuntime().exec(new String[]{"xdg-open", configOutputPath});
      } else {
        // Fallback to console output
        System.out.println("Unsupported operating system for this operation: " + os);
      }
    } catch (IOException e) {
      Logger.warning("Failed to open file explorer: " + e.getMessage());
    }
  }
}
