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

package network.brightspots.rcv;

import static javafx.collections.FXCollections.observableArrayList;
import static network.brightspots.rcv.Utils.isNullOrBlank;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.StringConverter;
import network.brightspots.rcv.ContestConfig.Provider;
import network.brightspots.rcv.RawContestConfig.Candidate;
import network.brightspots.rcv.RawContestConfig.ContestRules;
import network.brightspots.rcv.RawContestConfig.CvrSource;
import network.brightspots.rcv.RawContestConfig.OutputSettings;
import network.brightspots.rcv.Tabulator.OvervoteRule;
import network.brightspots.rcv.Tabulator.TieBreakMode;
import network.brightspots.rcv.Tabulator.WinnerElectionMode;

@SuppressWarnings("WeakerAccess")
public class GuiConfigController implements Initializable {

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final String CONFIG_FILE_DOCUMENTATION_FILENAME =
      "network/brightspots/rcv/config_file_documentation.txt";
  private static final List WINNER_ELECTION_MODE_VALUES = List.of(
      "Single-winner majority determines winner",
      "Multi-winner allow only one winner per round",
      "Multi-winner allow multiple winners per round",
      "Bottoms-up",
      "Bottoms-up using percentage threshold",
      "Multi-pass IRV"
  );
  private static final Map<String, String> WINNER_ELECTION_MODE_GUI_TEXT_TO_CONFIG_VALUE_MAP = Map
      .of(
          "Single-winner majority determines winner", WinnerElectionMode.STANDARD.toString(),
          "Multi-winner allow only one winner per round",
          WinnerElectionMode.MULTI_SEAT_ALLOW_ONLY_ONE_WINNER_PER_ROUND.toString(),
          "Multi-winner allow multiple winners per round", WinnerElectionMode.STANDARD.toString(),
          "Bottoms-up", WinnerElectionMode.MULTI_SEAT_BOTTOMS_UP.toString(),
          "Bottoms-up using percentage threshold",
          WinnerElectionMode.MULTI_SEAT_BOTTOMS_UP.toString(),
          "Multi-pass IRV", WinnerElectionMode.MULTI_SEAT_SEQUENTIAL_WINNER_TAKES_ALL.toString()
      );
  private static final List TIEBREAK_MODE_VALUES = List.of(
      "Random",
      "Stop counting and ask",
      "Previous round counts (then random)",
      "Previous round counts (then stop counting and ask)",
      "Use order of candidates list",
      "Generate permutation"
  );
  private static final Map<String, String> TIEBREAK_MODE_GUI_TEXT_TO_CONFIG_VALUE_MAP = Map.of(
      "Random", TieBreakMode.RANDOM.toString(),
      "Stop counting and ask", TieBreakMode.INTERACTIVE.toString(),
      "Previous round counts (then random)",
      TieBreakMode.PREVIOUS_ROUND_COUNTS_THEN_RANDOM.toString(),
      "Previous round counts (then stop counting and ask)",
      TieBreakMode.PREVIOUS_ROUND_COUNTS_THEN_INTERACTIVE.toString(),
      "Use order of candidates list", TieBreakMode.USE_PERMUTATION_IN_CONFIG.toString(),
      "Generate permutation", TieBreakMode.GENERATE_PERMUTATION.toString()
  );
  private static final Map<String, String> TIEBREAK_MODE_CONFIG_VALUE_TO_GUI_TEXT_MAP =
      TIEBREAK_MODE_GUI_TEXT_TO_CONFIG_VALUE_MAP.entrySet()
          .stream()
          .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

  // Used to check if changes have been made to a new config
  private String emptyConfigString;
  // File previously loaded or saved
  private File selectedFile;
  // GUI is currently busy validating or tabulating
  private boolean guiIsBusy;

  @FXML
  private TextArea textAreaStatus;
  @FXML
  private TextArea textAreaHelp;
  @FXML
  private Label labelCurrentlyLoaded;
  @FXML
  private TextField textFieldContestName;
  @FXML
  private TextField textFieldOutputDirectory;
  @FXML
  private DatePicker datePickerContestDate;
  @FXML
  private TextField textFieldContestJurisdiction;
  @FXML
  private TextField textFieldContestOffice;
  @FXML
  private CheckBox checkBoxTabulateByPrecinct;
  @FXML
  private CheckBox checkBoxGenerateCdfJson;
  @FXML
  private TableView<CvrSource> tableViewCvrFiles;
  @FXML
  private TableColumn<CvrSource, String> tableColumnCvrFilePath;
  @FXML
  private TableColumn<CvrSource, String> tableColumnCvrFirstVoteCol;
  @FXML
  private TableColumn<CvrSource, String> tableColumnCvrFirstVoteRow;
  @FXML
  private TableColumn<CvrSource, String> tableColumnCvrIdCol;
  @FXML
  private TableColumn<CvrSource, String> tableColumnCvrPrecinctCol;
  @FXML
  private TableColumn<CvrSource, String> tableColumnCvrProvider;
  @FXML
  private TableColumn<CvrSource, String> tableColumnCvrContestId;
  @FXML
  private ChoiceBox<Provider> choiceCvrProvider;
  @FXML
  private Button buttonAddCvrFile;
  @FXML
  private TextField textFieldCvrFilePath;
  @FXML
  private TextField textFieldCvrContestId;
  @FXML
  private Button buttonCvrFilePath;
  @FXML
  private TextField textFieldCvrFirstVoteCol;
  @FXML
  private TextField textFieldCvrFirstVoteRow;
  @FXML
  private TextField textFieldCvrIdCol;
  @FXML
  private TextField textFieldCvrPrecinctCol;
  @FXML
  private TableView<Candidate> tableViewCandidates;
  @FXML
  private TableColumn<Candidate, String> tableColumnCandidateName;
  @FXML
  private TableColumn<Candidate, String> tableColumnCandidateCode;
  @FXML
  private TableColumn<Candidate, Boolean> tableColumnCandidateExcluded;
  @FXML
  private TextField textFieldCandidateName;
  @FXML
  private TextField textFieldCandidateCode;
  @FXML
  private CheckBox checkBoxCandidateExcluded;
  @FXML
  private ChoiceBox<String> choiceTiebreakMode;
  @FXML
  private ChoiceBox<OvervoteRule> choiceOvervoteRule;
  @FXML
  private ChoiceBox<String> choiceWinnerElectionMode;
  @FXML
  private TextField textFieldRandomSeed;
  @FXML
  private TextField textFieldNumberOfWinners;
  @FXML
  private TextField textFieldMultiSeatBottomsUpPercentageThreshold;
  @FXML
  private TextField textFieldDecimalPlacesForVoteArithmetic;
  @FXML
  private TextField textFieldMinimumVoteThreshold;
  @FXML
  private TextField textFieldMaxSkippedRanksAllowed;
  @FXML
  private TextField textFieldMaxRankingsAllowed;
  @FXML
  private TextField textFieldOvervoteLabel;
  @FXML
  private TextField textFieldUndervoteLabel;
  @FXML
  private TextField textFieldUndeclaredWriteInLabel;
  @FXML
  private TextField textFieldRulesDescription;
  @FXML
  private CheckBox checkBoxNonIntegerWinningThreshold;
  @FXML
  private CheckBox checkBoxHareQuota;
  @FXML
  private CheckBox checkBoxBatchElimination;
  @FXML
  private CheckBox checkBoxExhaustOnDuplicateCandidate;
  @FXML
  private CheckBox checkBoxTreatBlankAsUndeclaredWriteIn;
  @FXML
  private MenuBar menuBar;
  @FXML
  private TabPane tabPane;

  private static String getChoiceElse(ChoiceBox choiceBox, Enum defaultValue) {
    return choiceBox.getValue() != null ? choiceBox.getValue().toString() : defaultValue.toString();
  }

  private static String getTextOrEmptyString(TextField textField) {
    return textField.getText() != null ? textField.getText().trim() : "";
  }

  /**
   * Action when new config menu item is clicked.
   */
  public void menuItemNewConfigClicked() {
    if (checkForSaveAndContinue()) {
      Logger.log(Level.INFO, "Creating new contest config...");
      GuiContext.getInstance().setConfig(null);
      selectedFile = null;
      clearConfig();
    }
  }

  private void loadFile(File fileToLoad, boolean silentMode) {
    // set the user dir for future loads
    FileUtils.setUserDirectory(fileToLoad.getParent());
    // load and cache the config object
    GuiContext.getInstance()
        .setConfig(ContestConfig.loadContestConfig(fileToLoad.getAbsolutePath(), silentMode));
    // if config loaded use it to populate the GUI
    if (GuiContext.getInstance().getConfig() != null) {
      loadConfig(GuiContext.getInstance().getConfig());
      labelCurrentlyLoaded.setText("Currently loaded: " + fileToLoad.getAbsolutePath());
    }
  }

  /**
   * Action when load config menu item is clicked.
   */
  public void menuItemLoadConfigClicked() {
    if (checkForSaveAndContinue()) {
      FileChooser fc = new FileChooser();
      if (selectedFile == null) {
        fc.setInitialDirectory(new File(FileUtils.getUserDirectory()));
      } else {
        fc.setInitialDirectory(new File(selectedFile.getParent()));
      }
      fc.getExtensionFilters().add(new ExtensionFilter("JSON files", "*.json"));
      fc.setTitle("Load Config");
      File fileToLoad = fc.showOpenDialog(GuiContext.getInstance().getMainWindow());
      if (fileToLoad != null) {
        selectedFile = fileToLoad;
        loadFile(fileToLoad, false);
      }
    }
  }

  private File getSaveFile() {
    FileChooser fc = new FileChooser();
    if (selectedFile == null) {
      fc.setInitialDirectory(new File(FileUtils.getUserDirectory()));
    } else {
      fc.setInitialDirectory(new File(selectedFile.getParent()));
      fc.setInitialFileName(selectedFile.getName());
    }
    fc.getExtensionFilters().add(new ExtensionFilter("JSON files", "*.json"));
    fc.setTitle("Save Config");
    File fileToSave = fc.showSaveDialog(GuiContext.getInstance().getMainWindow());
    if (fileToSave != null) {
      selectedFile = fileToSave;
    }
    return fileToSave;
  }

  private void saveFile(File fileToSave) {
    // set save file parent folder as the new default user folder
    FileUtils.setUserDirectory(fileToSave.getParent());
    // create a rawConfig object from GUI content and serialize it as json
    JsonParser.writeToFile(fileToSave, createRawContestConfig());
    // Reload to keep GUI fields updated in case invalid values are replaced during save process
    loadFile(fileToSave, true);
  }

  /**
   * Action when save menu item is clicked.
   */
  public void menuItemSaveClicked() {
    File fileToSave = getSaveFile();
    if (fileToSave != null) {
      saveFile(fileToSave);
    }
  }

  private void setGuiIsBusy(boolean isBusy) {
    guiIsBusy = isBusy;
    menuBar.setDisable(isBusy);
    tabPane.setDisable(isBusy);
  }

  /**
   * Action when validate menu item is clicked. Validates whatever is currently entered into the
   * GUI. Does not save data.
   */
  public void menuItemValidateClicked() {
    setGuiIsBusy(true);
    ContestConfig config =
        ContestConfig.loadContestConfig(createRawContestConfig(), FileUtils.getUserDirectory());
    ValidatorService service = new ValidatorService(config);
    setUpAndStartService(service);
  }

  /**
   * Tabulate whatever is currently entered into the GUI.
   * - Require user to save if there are unsaved changes.
   * - Create and launch TabulatorService from the saved config path.
   */
  public void menuItemTabulateClicked() {
    if (checkForSaveAndExecute()) {
      if (GuiContext.getInstance().getConfig() != null) {
        setGuiIsBusy(true);
        TabulatorService service = new TabulatorService(selectedFile.getAbsolutePath());
        setUpAndStartService(service);
      } else {
        Logger.log(
            Level.WARNING, "Please load a contest config file before attempting to tabulate!");
      }
    }
  }

  /**
   * Convert CVRs in current config to CDF.
   * - Require user to save if there are unsaved changes.
   * - Create and launch ConvertToCdfService from the saved config path.
   */
  public void menuItemConvertToCdfClicked() {
    if (checkForSaveAndExecute()) {
      if (GuiContext.getInstance().getConfig() != null) {
        setGuiIsBusy(true);
        ConvertToCdfService service = new ConvertToCdfService(selectedFile.getAbsolutePath());
        setUpAndStartService(service);
      } else {
        Logger.log(
            Level.WARNING,
            "Please load a contest config file before attempting to convert to CDF!");
      }
    }
  }

  /**
   * Convert Dominion files in specified folder to generic .csv format.
   * - Require user to specify a Dominion data folder path.
   * - Create and launch ConvertDominionService given the provided path.
   */
  public void menuItemConvertDominionClicked() {
    DirectoryChooser dc = new DirectoryChooser();
    dc.setInitialDirectory(new File(FileUtils.getUserDirectory()));
    dc.setTitle("Dominion Data Folder");
    File dominionDataFolderPath = dc.showDialog(GuiContext.getInstance().getMainWindow());
    if (dominionDataFolderPath != null) {
      setGuiIsBusy(true);
      ConvertDominionService service = new ConvertDominionService(
          dominionDataFolderPath.getAbsolutePath());
      setUpAndStartService(service);
    }
  }

  private void setUpAndStartService(Service<Void> service) {
    service.setOnSucceeded(event -> setGuiIsBusy(false));
    service.setOnCancelled(event -> setGuiIsBusy(false));
    service.setOnFailed(event -> setGuiIsBusy(false));
    service.start();
  }

  private void exitGui() {
    if (guiIsBusy) {
      Alert alert =
          new Alert(
              Alert.AlertType.WARNING,
              "The tabulator is currently busy. Are you sure you want to quit?",
              ButtonType.YES,
              ButtonType.NO);
      alert.setHeaderText(null);
      if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
        // In case the alert is still displayed when the GUI is no longer busy
        if (guiIsBusy) {
          Logger.log(Level.SEVERE, "User exited tabulator before it was finished!");
        } else {
          Logger.log(Level.INFO, "Exiting tabulator GUI...");
        }
        Platform.exit();
      }
    } else if (checkForSaveAndContinue()) {
      Logger.log(Level.INFO, "Exiting tabulator GUI...");
      Platform.exit();
    }
  }

  /**
   * Action when exit menu item is clicked.
   */
  public void menuItemExitClicked() {
    exitGui();
  }

  /**
   * Action when output directory button is clicked.
   */
  public void buttonOutputDirectoryClicked() {
    DirectoryChooser dc = new DirectoryChooser();
    dc.setInitialDirectory(new File(FileUtils.getUserDirectory()));
    dc.setTitle("Output Directory");
    File outputDirectory = dc.showDialog(GuiContext.getInstance().getMainWindow());
    if (outputDirectory != null) {
      textFieldOutputDirectory.setText(outputDirectory.getAbsolutePath());
    }
  }

  /** Action when clear button is clicked for contest date. */
  public void buttonClearDatePickerContestDateClicked() {
    datePickerContestDate.setValue(null);
  }

  /** Action when CVR file path button is clicked. */
  public void buttonCvrFilePathClicked() {
    File openFile = null;

    String providerName = getChoiceElse(choiceCvrProvider, Provider.PROVIDER_UNKNOWN);
    switch (providerName) {
      case "CDF" -> {
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File(FileUtils.getUserDirectory()));
        fc.getExtensionFilters().add(new ExtensionFilter("JSON files", "*.json"));
        fc.setTitle("Select CDF Cast Vote Record File");
        openFile = fc.showOpenDialog(GuiContext.getInstance().getMainWindow());
      }
      case "Clear Ballot" -> {
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File(FileUtils.getUserDirectory()));
        fc.getExtensionFilters().add(new ExtensionFilter("CSV files", "*.csv"));
        fc.setTitle("Select Clear Ballot Cast Vote Record File");
        openFile = fc.showOpenDialog(GuiContext.getInstance().getMainWindow());
      }
      case "Dominion", "Hart" -> {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setInitialDirectory(new File(FileUtils.getUserDirectory()));
        dc.setTitle("Select " + providerName + " Cast Vote Record Folder");
        openFile = dc.showDialog(GuiContext.getInstance().getMainWindow());
      }
      case "ES&S" -> {
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File(FileUtils.getUserDirectory()));
        fc.getExtensionFilters()
            .add(new ExtensionFilter("Excel files", "*.xls", "*.xlsx"));
        fc.setTitle("Select ES&S Cast Vote Record File");
        openFile = fc.showOpenDialog(GuiContext.getInstance().getMainWindow());
      }
      default -> {
        // Do nothing for unhandled providers
      }
    }

    if (openFile != null) {
      textFieldCvrFilePath.setText(openFile.getAbsolutePath());
    }
  }

  /** Action when add CVR file button is clicked. */
  public void buttonAddCvrFileClicked() {
    CvrSource cvrSource =
        new CvrSource(
            getTextOrEmptyString(textFieldCvrFilePath),
            getTextOrEmptyString(textFieldCvrFirstVoteCol),
            getTextOrEmptyString(textFieldCvrFirstVoteRow),
            getTextOrEmptyString(textFieldCvrIdCol),
            getTextOrEmptyString(textFieldCvrPrecinctCol),
            getChoiceElse(choiceCvrProvider, Provider.PROVIDER_UNKNOWN),
            getTextOrEmptyString(textFieldCvrContestId));
    if (ContestConfig.passesBasicCvrSourceValidation(cvrSource)) {
      tableViewCvrFiles.getItems().add(cvrSource);
      textFieldCvrFilePath.clear();
    }
  }

  public void buttonClearCvrFieldsClicked() {
    choiceCvrProvider.setValue(null);
    clearAndDisableCvrFilesTabFields();
  }

  private void clearAndDisableCvrFilesTabFields() {
    buttonAddCvrFile.setDisable(true);
    textFieldCvrFilePath.clear();
    textFieldCvrFilePath.setDisable(true);
    buttonCvrFilePath.setDisable(true);
    textFieldCvrFirstVoteCol.clear();
    textFieldCvrFirstVoteCol.setDisable(true);
    textFieldCvrFirstVoteRow.clear();
    textFieldCvrFirstVoteRow.setDisable(true);
    textFieldCvrIdCol.clear();
    textFieldCvrIdCol.setDisable(true);
    textFieldCvrPrecinctCol.clear();
    textFieldCvrPrecinctCol.setDisable(true);
    textFieldCvrContestId.clear();
    textFieldCvrContestId.setDisable(true);
  }

  /**
   * Action when delete CVR file button is clicked.
   */
  public void buttonDeleteCvrFileClicked() {
    tableViewCvrFiles
        .getItems()
        .removeAll(tableViewCvrFiles.getSelectionModel().getSelectedItems());
  }

  /**
   * Action when CVR file path is changed.
   */
  public void changeCvrFilePath(CellEditEvent cellEditEvent) {
    tableViewCvrFiles
        .getSelectionModel()
        .getSelectedItem()
        .setFilePath(cellEditEvent.getNewValue().toString().trim());
    tableViewCvrFiles.refresh();
  }

  /**
   * Action when CVR first vote col is changed.
   */
  public void changeCvrFirstVoteCol(CellEditEvent cellEditEvent) {
    tableViewCvrFiles
        .getSelectionModel()
        .getSelectedItem()
        .setFirstVoteColumnIndex(cellEditEvent.getNewValue().toString().trim());
    tableViewCvrFiles.refresh();
  }

  /**
   * Action when CVR first vote row is changed.
   */
  public void changeCvrFirstVoteRow(CellEditEvent cellEditEvent) {
    tableViewCvrFiles
        .getSelectionModel()
        .getSelectedItem()
        .setFirstVoteRowIndex(cellEditEvent.getNewValue().toString().trim());
    tableViewCvrFiles.refresh();
  }

  /** Action when CVR ID col index is changed. */
  public void changeCvrIdColIndex(CellEditEvent cellEditEvent) {
    tableViewCvrFiles
        .getSelectionModel()
        .getSelectedItem()
        .setIdColumnIndex(cellEditEvent.getNewValue().toString().trim());
    tableViewCvrFiles.refresh();
  }

  /**
   * Action when CVR precinct col index is changed.
   */
  public void changeCvrPrecinctColIndex(CellEditEvent cellEditEvent) {
    tableViewCvrFiles
        .getSelectionModel()
        .getSelectedItem()
        .setPrecinctColumnIndex(cellEditEvent.getNewValue().toString().trim());
    tableViewCvrFiles.refresh();
  }

  /**
   * Action when CVR provider is changed.
   */
  public void changeCvrProvider(CellEditEvent cellEditEvent) {
    tableViewCvrFiles
        .getSelectionModel()
        .getSelectedItem()
        .setProvider(cellEditEvent.getNewValue().toString().trim());
    tableViewCvrFiles.refresh();
  }

  /**
   * Action when CVR contest ID is changed.
   */
  public void changeCvrContestId(CellEditEvent cellEditEvent) {
    tableViewCvrFiles
        .getSelectionModel()
        .getSelectedItem()
        .setContestId(cellEditEvent.getNewValue().toString().trim());
    tableViewCvrFiles.refresh();
  }

  /**
   * Action when add candidate button is clicked.
   */
  public void buttonAddCandidateClicked() {
    Candidate candidate =
        new Candidate(
            getTextOrEmptyString(textFieldCandidateName),
            getTextOrEmptyString(textFieldCandidateCode),
            checkBoxCandidateExcluded.isSelected());
    if (ContestConfig.passesBasicCandidateValidation(candidate)) {
      tableViewCandidates.getItems().add(candidate);
      buttonClearCandidateClicked();
    }
  }

  public void buttonClearCandidateClicked() {
    textFieldCandidateName.clear();
    textFieldCandidateCode.clear();
    checkBoxCandidateExcluded.setSelected(ContestConfig.SUGGESTED_CANDIDATE_EXCLUDED);
  }

  /**
   * Action when delete candidate button is clicked.
   */
  public void buttonDeleteCandidateClicked() {
    tableViewCandidates
        .getItems()
        .removeAll(tableViewCandidates.getSelectionModel().getSelectedItems());
  }

  /**
   * Action when candidate name is changed.
   */
  public void changeCandidateName(CellEditEvent cellEditEvent) {
    tableViewCandidates
        .getSelectionModel()
        .getSelectedItem()
        .setName(cellEditEvent.getNewValue().toString().trim());
    tableViewCandidates.refresh();
  }

  /**
   * Action when candidate code is changed.
   */
  public void changeCandidateCode(CellEditEvent cellEditEvent) {
    tableViewCandidates
        .getSelectionModel()
        .getSelectedItem()
        .setCode(cellEditEvent.getNewValue().toString().trim());
    tableViewCandidates.refresh();
  }

  private void clearAndDisableWinningRuleFields() {
    textFieldMaxRankingsAllowed.clear();
    textFieldMaxRankingsAllowed.setDisable(true);
    textFieldMinimumVoteThreshold.clear();
    textFieldMinimumVoteThreshold.setDisable(true);
    checkBoxBatchElimination.setSelected(false);
    checkBoxBatchElimination.setDisable(true);
    choiceTiebreakMode.setValue(null);
    choiceTiebreakMode.setDisable(true);
    clearAndDisableTiebreakFields();
    textFieldNumberOfWinners.clear();
    textFieldNumberOfWinners.setDisable(true);
    textFieldMultiSeatBottomsUpPercentageThreshold.clear();
    textFieldMultiSeatBottomsUpPercentageThreshold.setDisable(true);
    checkBoxNonIntegerWinningThreshold.setSelected(false);
    checkBoxNonIntegerWinningThreshold.setDisable(true);
    checkBoxHareQuota.setSelected(false);
    checkBoxHareQuota.setDisable(true);
    textFieldDecimalPlacesForVoteArithmetic.clear();
    textFieldDecimalPlacesForVoteArithmetic.setDisable(true);
  }

  private void clearAndDisableTiebreakFields() {
    textFieldRandomSeed.clear();
    textFieldRandomSeed.setDisable(true);
  }

  private void setWinningRulesDefaultValues() {
    checkBoxNonIntegerWinningThreshold.setSelected(
        ContestConfig.SUGGESTED_NON_INTEGER_WINNING_THRESHOLD);
    checkBoxHareQuota.setSelected(ContestConfig.SUGGESTED_HARE_QUOTA);
    checkBoxBatchElimination.setSelected(ContestConfig.SUGGESTED_BATCH_ELIMINATION);
    textFieldNumberOfWinners.setText(String.valueOf(ContestConfig.SUGGESTED_NUMBER_OF_WINNERS));
    textFieldDecimalPlacesForVoteArithmetic.setText(
        String.valueOf(ContestConfig.SUGGESTED_DECIMAL_PLACES_FOR_VOTE_ARITHMETIC));
    textFieldMaxRankingsAllowed.setText(ContestConfig.SUGGESTED_MAX_RANKINGS_ALLOWED);
  }

  private void setDefaultValues() {
    labelCurrentlyLoaded.setText("Currently loaded: <New Config>");

    checkBoxCandidateExcluded.setSelected(ContestConfig.SUGGESTED_CANDIDATE_EXCLUDED);

    checkBoxTreatBlankAsUndeclaredWriteIn.setSelected(
        ContestConfig.SUGGESTED_TREAT_BLANK_AS_UNDECLARED_WRITE_IN);

    setWinningRulesDefaultValues();

    textFieldMaxSkippedRanksAllowed.setText(
        String.valueOf(ContestConfig.SUGGESTED_MAX_SKIPPED_RANKS_ALLOWED));
    checkBoxExhaustOnDuplicateCandidate.setSelected(
        ContestConfig.SUGGESTED_EXHAUST_ON_DUPLICATE_CANDIDATES);

    textFieldOutputDirectory.setText(ContestConfig.SUGGESTED_OUTPUT_DIRECTORY);
    checkBoxTabulateByPrecinct.setSelected(ContestConfig.SUGGESTED_TABULATE_BY_PRECINCT);
    checkBoxGenerateCdfJson.setSelected(ContestConfig.SUGGESTED_GENERATE_CDF_JSON);
  }

  private void clearConfig() {
    textFieldContestName.clear();
    datePickerContestDate.setValue(null);
    textFieldContestJurisdiction.clear();
    textFieldContestOffice.clear();
    textFieldRulesDescription.clear();

    choiceCvrProvider.setValue(null);
    clearAndDisableCvrFilesTabFields();
    tableViewCvrFiles.getItems().clear();

    textFieldCandidateName.clear();
    textFieldCandidateCode.clear();
    checkBoxCandidateExcluded.setSelected(false);
    tableViewCandidates.getItems().clear();

    choiceWinnerElectionMode.setValue(null);
    clearAndDisableWinningRuleFields();

    textFieldOvervoteLabel.clear();
    textFieldUndervoteLabel.clear();
    textFieldUndeclaredWriteInLabel.clear();
    checkBoxTreatBlankAsUndeclaredWriteIn.setSelected(false);

    choiceOvervoteRule.setValue(null);
    textFieldMaxSkippedRanksAllowed.clear();
    checkBoxExhaustOnDuplicateCandidate.setSelected(false);

    textFieldOutputDirectory.clear();
    checkBoxTabulateByPrecinct.setSelected(false);
    checkBoxGenerateCdfJson.setSelected(false);

    setDefaultValues();
  }

  private boolean checkIfNeedsSaving() {
    boolean needsSaving = true;
    try {
      String currentConfigString =
          new ObjectMapper()
              .writer()
              .withDefaultPrettyPrinter()
              .writeValueAsString(createRawContestConfig());
      if (selectedFile == null && currentConfigString.equals(emptyConfigString)) {
        // All fields are currently empty / default values so no point in asking to save
        needsSaving = false;
      } else if (GuiContext.getInstance().getConfig() != null) {
        // Compare to version currently saved on the hard drive
        String savedConfigString =
            new ObjectMapper()
                .writer()
                .withDefaultPrettyPrinter()
                .writeValueAsString(
                    JsonParser.readFromFileWithoutLogging(
                        selectedFile.getAbsolutePath(), RawContestConfig.class));
        needsSaving = !currentConfigString.equals(savedConfigString);
      }
    } catch (JsonProcessingException exception) {
      Logger.log(
          Level.WARNING,
          "Unable tell if saving is necessary, but everything should work fine anyway! Prompting "
              + "for save just in case...\n%s",
          exception.toString());
    }
    return needsSaving;
  }

  private boolean checkForSaveAndContinue() {
    boolean willContinue = false;
    if (checkIfNeedsSaving()) {
      ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.YES);
      ButtonType doNotSaveButton = new ButtonType("Don't Save", ButtonBar.ButtonData.NO);
      Alert alert =
          new Alert(
              AlertType.CONFIRMATION,
              "Do you want to save your changes before continuing?",
              saveButton,
              doNotSaveButton,
              ButtonType.CANCEL);
      alert.setHeaderText(null);
      Optional<ButtonType> result = alert.showAndWait();
      if (result.isPresent() && result.get() == saveButton) {
        File fileToSave = getSaveFile();
        if (fileToSave != null) {
          saveFile(fileToSave);
          willContinue = true;
        }
      } else if (result.isPresent() && result.get() == doNotSaveButton) {
        willContinue = true;
      }
    } else {
      willContinue = true;
    }
    return willContinue;
  }

  private boolean checkForSaveAndExecute() {
    boolean willContinue = false;
    if (checkIfNeedsSaving()) {
      ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.YES);
      Alert alert =
          new Alert(
              AlertType.WARNING,
              "You must either save your changes before continuing or load a new contest config!",
              saveButton,
              ButtonType.CANCEL);
      alert.setHeaderText(null);
      Optional<ButtonType> result = alert.showAndWait();
      if (result.isPresent() && result.get() == saveButton) {
        File fileToSave = getSaveFile();
        if (fileToSave != null) {
          saveFile(fileToSave);
          willContinue = true;
        }
      }
    } else {
      willContinue = true;
    }
    return willContinue;
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    Logger.addGuiLogging(this.textAreaStatus);
    Logger.log(Level.INFO, "Opening tabulator GUI...");
    Logger.log(Level.INFO, "Welcome to the %s version %s!", Main.APP_NAME, Main.APP_VERSION);

    GuiContext.getInstance()
        .getMainWindow()
        .setOnCloseRequest(
            event -> {
              event.consume();
              exitGui();
            });

    String helpText;
    try {
      //noinspection ConstantConditions
      helpText =
          new BufferedReader(
              new InputStreamReader(
                  ClassLoader.getSystemResourceAsStream(CONFIG_FILE_DOCUMENTATION_FILENAME)))
              .lines()
              .collect(Collectors.joining("\n"));
    } catch (Exception exception) {
      Logger.log(
          Level.SEVERE,
          "Error loading config file documentation: %s\n%s",
          CONFIG_FILE_DOCUMENTATION_FILENAME,
          exception.toString());
      helpText =
          String.format(
              "<Error loading config file documentation: %s>", CONFIG_FILE_DOCUMENTATION_FILENAME);
    }
    textAreaHelp.setText(helpText);

    datePickerContestDate.setConverter(
        new StringConverter<>() {
          @Override
          public String toString(LocalDate date) {
            return date != null ? DATE_TIME_FORMATTER.format(date) : "";
          }

          @Override
          public LocalDate fromString(String string) {
            return isNullOrBlank(string) ? null : LocalDate.parse(string, DATE_TIME_FORMATTER);
          }
        });

    clearAndDisableCvrFilesTabFields();
    choiceCvrProvider.getItems().addAll(Provider.values());
    choiceCvrProvider.getItems().remove(Provider.PROVIDER_UNKNOWN);
    choiceCvrProvider.setOnAction(event -> {
      clearAndDisableCvrFilesTabFields();
      String provider = getChoiceElse(choiceCvrProvider, Provider.PROVIDER_UNKNOWN);
      switch (provider) {
        case "ES&S" -> {
          buttonAddCvrFile.setDisable(false);
          textFieldCvrFilePath.setDisable(false);
          buttonCvrFilePath.setDisable(false);
          textFieldCvrFirstVoteCol.setDisable(false);
          textFieldCvrFirstVoteRow.setDisable(false);
          textFieldCvrIdCol.setDisable(false);
          textFieldCvrPrecinctCol.setDisable(false);
        }
        case "CDF" -> {
          buttonAddCvrFile.setDisable(false);
          textFieldCvrFilePath.setDisable(false);
          buttonCvrFilePath.setDisable(false);
        }
        case "Clear Ballot", "Dominion", "Hart" -> {
          buttonAddCvrFile.setDisable(false);
          textFieldCvrFilePath.setDisable(false);
          buttonCvrFilePath.setDisable(false);
          textFieldCvrContestId.setDisable(false);
        }
      }
    });
    tableColumnCvrFilePath.setCellValueFactory(new PropertyValueFactory<>("filePath"));
    tableColumnCvrFilePath.setCellFactory(TextFieldTableCell.forTableColumn());
    tableColumnCvrFirstVoteCol.setCellValueFactory(
        new PropertyValueFactory<>("firstVoteColumnIndex"));
    tableColumnCvrFirstVoteCol.setCellFactory(TextFieldTableCell.forTableColumn());
    tableColumnCvrFirstVoteRow.setCellValueFactory(new PropertyValueFactory<>("firstVoteRowIndex"));
    tableColumnCvrFirstVoteRow.setCellFactory(TextFieldTableCell.forTableColumn());
    tableColumnCvrIdCol.setCellValueFactory(new PropertyValueFactory<>("idColumnIndex"));
    tableColumnCvrIdCol.setCellFactory(TextFieldTableCell.forTableColumn());
    tableColumnCvrPrecinctCol.setCellValueFactory(
        new PropertyValueFactory<>("precinctColumnIndex"));
    tableColumnCvrPrecinctCol.setCellFactory(TextFieldTableCell.forTableColumn());
    tableColumnCvrProvider.setCellValueFactory(new PropertyValueFactory<>("provider"));
    tableColumnCvrProvider.setCellFactory(TextFieldTableCell.forTableColumn());
    tableColumnCvrContestId.setCellValueFactory(new PropertyValueFactory<>("contestId"));
    tableColumnCvrContestId.setCellFactory(TextFieldTableCell.forTableColumn());
    tableViewCvrFiles.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    tableViewCvrFiles.setEditable(true);

    tableColumnCandidateName.setCellValueFactory(new PropertyValueFactory<>("name"));
    tableColumnCandidateName.setCellFactory(TextFieldTableCell.forTableColumn());
    tableColumnCandidateCode.setCellValueFactory(new PropertyValueFactory<>("code"));
    tableColumnCandidateCode.setCellFactory(TextFieldTableCell.forTableColumn());
    tableColumnCandidateExcluded.setCellValueFactory(
        c -> {
          Candidate candidate = c.getValue();
          CheckBox checkBox = new CheckBox();
          checkBox.selectedProperty().setValue(candidate.isExcluded());
          checkBox
              .selectedProperty()
              .addListener((ov, oldVal, newVal) -> candidate.setExcluded(newVal));
          return new SimpleBooleanProperty(checkBox.isSelected());
        });
    tableViewCandidates.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    tableViewCandidates.setEditable(true);

    clearAndDisableWinningRuleFields();
    choiceTiebreakMode.getItems().addAll(TIEBREAK_MODE_VALUES);
    choiceTiebreakMode.setOnAction(event -> {
      clearAndDisableTiebreakFields();
      String mode = getChoiceElse(choiceTiebreakMode, TieBreakMode.MODE_UNKNOWN);
      switch (mode) {
        case "Random", "Previous round counts (then random)", "Generate permutation" -> textFieldRandomSeed
            .setDisable(false);
      }
    });
    choiceOvervoteRule.getItems().addAll(OvervoteRule.values());
    choiceOvervoteRule.getItems().remove(OvervoteRule.RULE_UNKNOWN);
    choiceWinnerElectionMode.getItems().addAll(WINNER_ELECTION_MODE_VALUES);
    choiceWinnerElectionMode.setOnAction(event -> {
      clearAndDisableWinningRuleFields();
      setWinningRulesDefaultValues();
      String mode = getChoiceElse(choiceWinnerElectionMode, WinnerElectionMode.MODE_UNKNOWN);
      switch (mode) {
        case "Single-winner majority determines winner" -> {
          textFieldMaxRankingsAllowed.setDisable(false);
          textFieldMinimumVoteThreshold.setDisable(false);
          choiceTiebreakMode.setDisable(false);
          checkBoxNonIntegerWinningThreshold.setDisable(false);
          checkBoxHareQuota.setDisable(false);
          textFieldDecimalPlacesForVoteArithmetic.setDisable(false);
          checkBoxBatchElimination.setDisable(false);
        }
        case "Multi-winner allow only one winner per round", "Multi-winner allow multiple winners per round", "Bottoms-up", "Multi-pass IRV" -> {
          textFieldMaxRankingsAllowed.setDisable(false);
          textFieldMinimumVoteThreshold.setDisable(false);
          choiceTiebreakMode.setDisable(false);
          checkBoxNonIntegerWinningThreshold.setDisable(false);
          checkBoxHareQuota.setDisable(false);
          textFieldDecimalPlacesForVoteArithmetic.setDisable(false);
          textFieldNumberOfWinners.setDisable(false);
        }
        case "Bottoms-up using percentage threshold" -> {
          textFieldMaxRankingsAllowed.setDisable(false);
          textFieldMinimumVoteThreshold.setDisable(false);
          choiceTiebreakMode.setDisable(false);
          checkBoxNonIntegerWinningThreshold.setDisable(false);
          checkBoxHareQuota.setDisable(false);
          textFieldDecimalPlacesForVoteArithmetic.setDisable(false);
          textFieldNumberOfWinners.setDisable(false);
          textFieldMultiSeatBottomsUpPercentageThreshold.setDisable(false);
        }
      }
    });

    setDefaultValues();

    try {
      emptyConfigString =
          new ObjectMapper()
              .writer()
              .withDefaultPrettyPrinter()
              .writeValueAsString(createRawContestConfig());
    } catch (JsonProcessingException exception) {
      Logger.log(
          Level.WARNING,
          "Unable to set emptyConfigString, but everything should work fine anyway!\n%s",
          exception.toString());
    }
  }

  private void migrateConfigVersion(ContestConfig config) {
    if (config.rawConfig.tabulatorVersion == null
        || !config.rawConfig.tabulatorVersion.equals(Main.APP_VERSION)) {
      // Any necessary future version migration logic goes here
      Logger.log(
          Level.INFO,
          "Migrated tabulator config version from %s to %s.",
          config.rawConfig.tabulatorVersion != null ? config.rawConfig.tabulatorVersion : "unknown",
          Main.APP_VERSION);
      config.rawConfig.tabulatorVersion = Main.APP_VERSION;
    }
  }

  private void loadConfig(ContestConfig config) {
    clearConfig();
    RawContestConfig rawConfig = config.getRawConfig();
    migrateConfigVersion(config);
    OutputSettings outputSettings = rawConfig.outputSettings;
    textFieldContestName.setText(outputSettings.contestName);
    textFieldOutputDirectory.setText(config.getOutputDirectoryRaw());
    if (!isNullOrBlank(outputSettings.contestDate)) {
      try {
        datePickerContestDate.setValue(
            LocalDate.parse(outputSettings.contestDate, DATE_TIME_FORMATTER));
      } catch (DateTimeParseException exception) {
        Logger.log(Level.SEVERE, "Invalid contestDate: %s!", outputSettings.contestDate);
        datePickerContestDate.setValue(null);
      }
    }
    textFieldContestJurisdiction.setText(outputSettings.contestJurisdiction);
    textFieldContestOffice.setText(outputSettings.contestOffice);
    checkBoxTabulateByPrecinct.setSelected(outputSettings.tabulateByPrecinct);
    checkBoxGenerateCdfJson.setSelected(outputSettings.generateCdfJson);

    if (rawConfig.cvrFileSources != null) {
      tableViewCvrFiles.setItems(observableArrayList(rawConfig.cvrFileSources));
    }

    if (rawConfig.candidates != null) {
      tableViewCandidates.setItems(observableArrayList(rawConfig.candidates));
    }

    choiceWinnerElectionMode.setValue(
        config.getWinnerElectionMode() == WinnerElectionMode.MODE_UNKNOWN
            ? null
            : convertConfigWinnerElectionModeToGuiText(config));
    choiceTiebreakMode.setValue(
        config.getTiebreakMode() == TieBreakMode.MODE_UNKNOWN ? null
            : TIEBREAK_MODE_CONFIG_VALUE_TO_GUI_TEXT_MAP
                .getOrDefault(config.getTiebreakMode().toString(), null));
    choiceOvervoteRule.setValue(
        config.getOvervoteRule() == OvervoteRule.RULE_UNKNOWN ? null : config.getOvervoteRule());

    ContestRules rules = rawConfig.rules;
    textFieldRandomSeed.setText(rules.randomSeed);
    textFieldNumberOfWinners.setText(rules.numberOfWinners);
    textFieldMultiSeatBottomsUpPercentageThreshold
        .setText(rules.multiSeatBottomsUpPercentageThreshold);
    textFieldDecimalPlacesForVoteArithmetic.setText(rules.decimalPlacesForVoteArithmetic);
    textFieldMinimumVoteThreshold.setText(rules.minimumVoteThreshold);
    textFieldMaxSkippedRanksAllowed.setText(rules.maxSkippedRanksAllowed);
    textFieldMaxRankingsAllowed.setText(rules.maxRankingsAllowed);
    textFieldOvervoteLabel.setText(rules.overvoteLabel);
    textFieldUndervoteLabel.setText(rules.undervoteLabel);
    textFieldUndeclaredWriteInLabel.setText(rules.undeclaredWriteInLabel);
    textFieldRulesDescription.setText(rules.rulesDescription);
    checkBoxNonIntegerWinningThreshold.setSelected(rules.nonIntegerWinningThreshold);
    checkBoxHareQuota.setSelected(rules.hareQuota);
    checkBoxBatchElimination.setSelected(rules.batchElimination);
    checkBoxExhaustOnDuplicateCandidate.setSelected(rules.exhaustOnDuplicateCandidate);
    checkBoxTreatBlankAsUndeclaredWriteIn.setSelected(rules.treatBlankAsUndeclaredWriteIn);
  }

  private static String convertConfigWinnerElectionModeToGuiText(ContestConfig config) {
    switch (config.getWinnerElectionMode().toString()) {
      case "standard" -> {
        return config.getNumberOfWinners() > 1 ? "Multi-winner allow multiple winners per round"
            : "Single-winner majority determines winner";
      }
      case "singleSeatContinueUntilTwoCandidatesRemain" -> {
        return "Single-winner majority determines winner";
      }
      case "multiSeatAllowOnlyOneWinnerPerRound" -> {
        return "Multi-winner allow only one winner per round";
      }
      case "multiSeatBottomsUp" -> {
        return config.getNumberOfWinners() == 0
            || config.getMultiSeatBottomsUpPercentageThreshold() != null
            ? "Bottoms-up using percentage threshold" : "Bottoms-up";
      }
      case "multiSeatSequentialWinnerTakesAll" -> {
        return "Multi-pass IRV";
      }
    }
    return null;
  }

  private RawContestConfig createRawContestConfig() {
    RawContestConfig config = new RawContestConfig();
    config.tabulatorVersion = Main.APP_VERSION;
    OutputSettings outputSettings = new OutputSettings();
    outputSettings.contestName = getTextOrEmptyString(textFieldContestName);
    outputSettings.outputDirectory = getTextOrEmptyString(textFieldOutputDirectory);
    outputSettings.contestDate =
        datePickerContestDate.getValue() != null ? datePickerContestDate.getValue().toString() : "";
    outputSettings.contestJurisdiction = getTextOrEmptyString(textFieldContestJurisdiction);
    outputSettings.contestOffice = getTextOrEmptyString(textFieldContestOffice);
    outputSettings.tabulateByPrecinct = checkBoxTabulateByPrecinct.isSelected();
    outputSettings.generateCdfJson = checkBoxGenerateCdfJson.isSelected();
    config.outputSettings = outputSettings;

    ArrayList<CvrSource> cvrSources = new ArrayList<>(tableViewCvrFiles.getItems());
    for (CvrSource source : cvrSources) {
      source.setFilePath(source.getFilePath() != null ? source.getFilePath().trim() : "");
      source.setIdColumnIndex(
          source.getIdColumnIndex() != null ? source.getIdColumnIndex().trim() : "");
      source.setPrecinctColumnIndex(
          source.getPrecinctColumnIndex() != null ? source.getPrecinctColumnIndex().trim() : "");
      source.setProvider(source.getProvider() != null ? source.getProvider().trim() : "");
      source.setContestId(source.getContestId() != null ? source.getContestId().trim() : "");
    }
    config.cvrFileSources = cvrSources;

    ArrayList<Candidate> candidates = new ArrayList<>(tableViewCandidates.getItems());
    for (Candidate candidate : candidates) {
      candidate.setName(candidate.getName() != null ? candidate.getName().trim() : "");
      candidate.setCode(candidate.getCode() != null ? candidate.getCode().trim() : "");
    }
    config.candidates = candidates;

    ContestRules rules = new ContestRules();
    rules.tiebreakMode = TIEBREAK_MODE_GUI_TEXT_TO_CONFIG_VALUE_MAP
        .getOrDefault(getChoiceElse(choiceTiebreakMode, TieBreakMode.MODE_UNKNOWN),
            TieBreakMode.MODE_UNKNOWN.toString());
    rules.overvoteRule = getChoiceElse(choiceOvervoteRule, OvervoteRule.RULE_UNKNOWN);
    rules.winnerElectionMode = WINNER_ELECTION_MODE_GUI_TEXT_TO_CONFIG_VALUE_MAP
        .getOrDefault(getChoiceElse(choiceWinnerElectionMode, WinnerElectionMode.MODE_UNKNOWN),
            WinnerElectionMode.MODE_UNKNOWN.toString());
    rules.randomSeed = getTextOrEmptyString(textFieldRandomSeed);
    rules.numberOfWinners = getTextOrEmptyString(textFieldNumberOfWinners);
    rules.multiSeatBottomsUpPercentageThreshold = getTextOrEmptyString(
        (textFieldMultiSeatBottomsUpPercentageThreshold));
    rules.decimalPlacesForVoteArithmetic =
        getTextOrEmptyString(textFieldDecimalPlacesForVoteArithmetic);
    rules.minimumVoteThreshold = getTextOrEmptyString(textFieldMinimumVoteThreshold);
    rules.maxSkippedRanksAllowed = getTextOrEmptyString(textFieldMaxSkippedRanksAllowed);
    rules.maxRankingsAllowed = getTextOrEmptyString(textFieldMaxRankingsAllowed);
    rules.nonIntegerWinningThreshold = checkBoxNonIntegerWinningThreshold.isSelected();
    rules.hareQuota = checkBoxHareQuota.isSelected();
    rules.batchElimination = checkBoxBatchElimination.isSelected();
    rules.exhaustOnDuplicateCandidate = checkBoxExhaustOnDuplicateCandidate.isSelected();
    rules.treatBlankAsUndeclaredWriteIn = checkBoxTreatBlankAsUndeclaredWriteIn.isSelected();
    rules.overvoteLabel = getTextOrEmptyString(textFieldOvervoteLabel);
    rules.undervoteLabel = getTextOrEmptyString(textFieldUndervoteLabel);
    rules.undeclaredWriteInLabel = getTextOrEmptyString(textFieldUndeclaredWriteInLabel);
    rules.rulesDescription = getTextOrEmptyString(textFieldRulesDescription);
    config.rules = rules;

    return config;
  }

  private static class ValidatorService extends Service<Void> {

    private final ContestConfig contestConfig;

    ValidatorService(ContestConfig contestConfig) {
      this.contestConfig = contestConfig;
    }

    @Override
    protected Task<Void> createTask() {
      Task<Void> task =
          new Task<>() {
            @Override
            protected Void call() {
              contestConfig.validate();
              return null;
            }
          };
      task.setOnFailed(
          arg0 ->
              Logger.log(
                  Level.SEVERE,
                  "Error during validation:\n%s\nValidation failed!",
                  task.getException().toString()));
      return task;
    }
  }

  // TabulatorService runs a tabulation in the background
  private static class TabulatorService extends Service<Void> {

    private final String configPath;

    TabulatorService(String configPath) {
      this.configPath = configPath;
    }

    @Override
    protected Task<Void> createTask() {
      Task<Void> task =
          new Task<>() {
            @Override
            protected Void call() {
              TabulatorSession session = new TabulatorSession(configPath);
              session.tabulate();
              return null;
            }
          };
      task.setOnFailed(
          arg0 ->
              Logger.log(
                  Level.SEVERE,
                  "Error during tabulation:\n%s\nTabulation failed!",
                  task.getException().toString()));
      return task;
    }
  }

  // ConvertToCdfService runs a CDF conversion in the background
  private static class ConvertToCdfService extends Service<Void> {

    private final String configPath;

    ConvertToCdfService(String configPath) {
      this.configPath = configPath;
    }

    @Override
    protected Task<Void> createTask() {
      Task<Void> task =
          new Task<>() {
            @Override
            protected Void call() {
              TabulatorSession session = new TabulatorSession(configPath);
              session.convertToCdf();
              return null;
            }
          };
      task.setOnFailed(
          arg0 ->
              Logger.log(
                  Level.SEVERE,
                  "Error when attempting to convert to CDF:\n%s\nConversion failed!",
                  task.getException().toString()));
      return task;
    }
  }

  // ConvertDominionService runs a Dominion conversion in the background
  private static class ConvertDominionService extends Service<Void> {

    private final String dominionDataFolderPath;

    ConvertDominionService(String dominionDataFolderPath) {
      this.dominionDataFolderPath = dominionDataFolderPath;
    }

    @Override
    protected Task<Void> createTask() {
      Task<Void> task =
          new Task<>() {
            @Override
            protected Void call() {
              TabulatorSession session = new TabulatorSession(null);
              session.convertDominionCvrJsonToGenericCsv(dominionDataFolderPath);
              return null;
            }
          };
      task.setOnFailed(
          arg0 ->
              Logger.log(
                  Level.SEVERE,
                  "Error when attempting to convert Dominion files:\n%s\nConversion failed!",
                  task.getException().toString()));
      return task;
    }
  }
}
