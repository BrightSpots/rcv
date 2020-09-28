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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
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
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
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

@SuppressWarnings({"WeakerAccess", "rawtypes"})
public class GuiConfigController implements Initializable {

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final String CONFIG_FILE_DOCUMENTATION_FILENAME =
      "network/brightspots/rcv/config_file_documentation.txt";
  private static final String HINTS_CONTEST_INFO_FILENAME = "network/brightspots/rcv/hints_contest_info.txt";
  private static final String HINTS_CVR_FILES_FILENAME = "network/brightspots/rcv/hints_cvr_files.txt";
  private static final String HINTS_CANDIDATES_FILENAME = "network/brightspots/rcv/hints_candidates.txt";
  private static final String HINTS_WINNING_RULES_FILENAME = "network/brightspots/rcv/hints_winning_rules.txt";
  private static final String HINTS_VOTER_ERROR_RULES_FILENAME = "network/brightspots/rcv/hints_voter_error_rules.txt";
  private static final String HINTS_OUTPUT_FILENAME = "network/brightspots/rcv/hints_output.txt";

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
  private TableColumn<CvrSource, String> tableColumnCvrOvervoteDelimiter;
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
  private TextField textFieldCvrOvervoteDelimiter;
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
  private ChoiceBox<TieBreakMode> choiceTiebreakMode;
  @FXML
  private RadioButton radioOvervoteAlwaysSkip;
  @FXML
  private RadioButton radioOvervoteExhaustImmediately;
  @FXML
  private RadioButton radioOvervoteExhaustIfMultiple;
  @FXML
  private ChoiceBox<WinnerElectionMode> choiceWinnerElectionMode;
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
  private RadioButton radioThresholdMostCommon;
  @FXML
  private RadioButton radioThresholdHbQuota;
  @FXML
  private RadioButton radioThresholdHareQuota;
  @FXML
  private CheckBox checkBoxBatchElimination;
  @FXML
  private CheckBox checkBoxContinueUntilTwoCandidatesRemain;
  @FXML
  private CheckBox checkBoxExhaustOnDuplicateCandidate;
  @FXML
  private CheckBox checkBoxTreatBlankAsUndeclaredWriteIn;
  @FXML
  private MenuBar menuBar;
  @FXML
  private TabPane tabPane;
  @FXML
  private Tab tabContestInfo;
  @FXML
  private Tab tabCvrFiles;
  @FXML
  private Tab tabCandidates;
  @FXML
  private Tab tabWinningRules;
  @FXML
  private Tab tabVoterErrorRules;
  @FXML
  private Tab tabOutput;

  private static Provider getProviderChoice(ChoiceBox<Provider> choiceBox) {
    return choiceBox.getValue() != null ? Provider.getByLabel(choiceBox.getValue().toString())
        : Provider.PROVIDER_UNKNOWN;
  }

  private static WinnerElectionMode getWinnerElectionModeChoice(
      ChoiceBox<WinnerElectionMode> choiceBox) {
    return choiceBox.getValue() != null ? WinnerElectionMode
        .getByLabel(choiceBox.getValue().toString()) : WinnerElectionMode.MODE_UNKNOWN;
  }

  private static TieBreakMode getTiebreakModeChoice(ChoiceBox<TieBreakMode> choiceBox) {
    return choiceBox.getValue() != null ? TieBreakMode.getByLabel(choiceBox.getValue().toString())
        : TieBreakMode.MODE_UNKNOWN;
  }

  private static String getTextOrEmptyString(TextField textField) {
    return textField.getText() != null ? textField.getText().trim() : "";
  }

  private String getOvervoteRuleChoice() {
    String overvoteRuleString = OvervoteRule.RULE_UNKNOWN.toString();
    if (radioOvervoteAlwaysSkip.isSelected()) {
      overvoteRuleString = OvervoteRule.ALWAYS_SKIP_TO_NEXT_RANK.toString();
    } else if (radioOvervoteExhaustImmediately.isSelected()) {
      overvoteRuleString = OvervoteRule.EXHAUST_IMMEDIATELY.toString();
    } else if (radioOvervoteExhaustIfMultiple.isSelected()) {
      overvoteRuleString = OvervoteRule.EXHAUST_IF_MULTIPLE_CONTINUING.toString();
    }
    return overvoteRuleString;
  }

  /**
   * Action when help menu item is clicked. Try to open the local help manual.
   */
  public void menuItemOpenHelpClicked() {
    URL helpFileUrl = ClassLoader.getSystemResource(CONFIG_FILE_DOCUMENTATION_FILENAME);
    String command = null;
    if (Utils.IS_OS_WINDOWS) {
      command = String.format("cmd /c start \"Help\" \"%s\"", helpFileUrl);
    } else if (Utils.IS_OS_MAC) {
      command = String.format("open %s", helpFileUrl);
    } else if (Utils.IS_OS_LINUX) {
      command = String.format("xdg-open \"%s\"", helpFileUrl);
    } else {
      Logger.info("Unable to determine operating system. Try opening the documentation "
          + "manually at: %s", helpFileUrl);
    }
    if (command != null) {
      try {
        Process process = Runtime.getRuntime().exec(command);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
          Logger.info(line);
        }
        reader.close();
      } catch (IOException e) {
        Logger.severe("Error opening help file: %s", e.toString());
        Logger.info("Try opening the documentation manually at: %s", helpFileUrl);
      }
    }
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
   * Tabulate whatever is currently entered into the GUI. Requires user to save if there are unsaved
   * changes, and creates and launches TabulatorService from the saved config path.
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
   * Convert CVRs in current config to CDF. Requires user to save if there are unsaved changes, and
   * create and launches ConvertToCdfService from the saved config path.
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

  /**
   * Action when clear button is clicked for contest date.
   */
  public void buttonClearDatePickerContestDateClicked() {
    datePickerContestDate.setValue(null);
  }

  /**
   * Action when CVR file path button is clicked.
   */
  public void buttonCvrFilePathClicked() {
    File openFile = null;

    Provider provider = getProviderChoice(choiceCvrProvider);
    switch (provider) {
      case CDF -> {
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File(FileUtils.getUserDirectory()));
        fc.getExtensionFilters().add(new ExtensionFilter("JSON and XML files", "*.json", "*.xml"));
        fc.setTitle("Select " + provider + " Cast Vote Record File");
        openFile = fc.showOpenDialog(GuiContext.getInstance().getMainWindow());
      }
      case CLEAR_BALLOT -> {
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File(FileUtils.getUserDirectory()));
        fc.getExtensionFilters().add(new ExtensionFilter("CSV files", "*.csv"));
        fc.setTitle("Select " + provider + " Cast Vote Record File");
        openFile = fc.showOpenDialog(GuiContext.getInstance().getMainWindow());
      }
      case DOMINION, HART -> {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setInitialDirectory(new File(FileUtils.getUserDirectory()));
        dc.setTitle("Select " + provider + " Cast Vote Record Folder");
        openFile = dc.showDialog(GuiContext.getInstance().getMainWindow());
      }
      case ESS -> {
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File(FileUtils.getUserDirectory()));
        fc.getExtensionFilters()
            .add(new ExtensionFilter("Excel files", "*.xls", "*.xlsx"));
        fc.setTitle("Select " + provider + " Cast Vote Record File");
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

  /**
   * Action when add CVR file button is clicked.
   */
  public void buttonAddCvrFileClicked() {
    CvrSource cvrSource =
        new CvrSource(
            getTextOrEmptyString(textFieldCvrFilePath),
            getTextOrEmptyString(textFieldCvrFirstVoteCol),
            getTextOrEmptyString(textFieldCvrFirstVoteRow),
            getTextOrEmptyString(textFieldCvrIdCol),
            getTextOrEmptyString(textFieldCvrPrecinctCol),
            getTextOrEmptyString(textFieldCvrOvervoteDelimiter),
            getProviderChoice(choiceCvrProvider).toString(),
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
    textFieldCvrOvervoteDelimiter.clear();
    textFieldCvrOvervoteDelimiter.setDisable(true);
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

  /**
   * Action when CVR ID col index is changed.
   */
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
   * Action when CVR overvote delimiter is changed.
   */
  public void changeCvrOvervoteDelimiter(CellEditEvent cellEditEvent) {
    tableViewCvrFiles
        .getSelectionModel()
        .getSelectedItem()
        .setOvervoteDelimiter(cellEditEvent.getNewValue().toString().trim());
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
    checkBoxContinueUntilTwoCandidatesRemain.setSelected(false);
    checkBoxContinueUntilTwoCandidatesRemain.setDisable(true);
    choiceTiebreakMode.setValue(null);
    choiceTiebreakMode.setDisable(true);
    clearAndDisableTiebreakFields();
    textFieldNumberOfWinners.clear();
    textFieldNumberOfWinners.setDisable(true);
    textFieldMultiSeatBottomsUpPercentageThreshold.clear();
    textFieldMultiSeatBottomsUpPercentageThreshold.setDisable(true);
    radioThresholdMostCommon.setSelected(false);
    radioThresholdMostCommon.setDisable(true);
    radioThresholdHbQuota.setSelected(false);
    radioThresholdHbQuota.setDisable(true);
    radioThresholdHareQuota.setSelected(false);
    radioThresholdHareQuota.setDisable(true);
    textFieldDecimalPlacesForVoteArithmetic.clear();
    textFieldDecimalPlacesForVoteArithmetic.setDisable(true);
  }

  private void clearAndDisableTiebreakFields() {
    textFieldRandomSeed.clear();
    textFieldRandomSeed.setDisable(true);
  }

  private void setWinningRulesDefaultValues() {
    setThresholdCalculationMethodRadioButton(ContestConfig.SUGGESTED_NON_INTEGER_WINNING_THRESHOLD,
        ContestConfig.SUGGESTED_HARE_QUOTA);
    checkBoxBatchElimination.setSelected(ContestConfig.SUGGESTED_BATCH_ELIMINATION);
    checkBoxContinueUntilTwoCandidatesRemain
        .setSelected(ContestConfig.SUGGESTED_CONTINUE_UNTIL_TWO_CANDIDATES_REMAIN);
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
    textFieldOvervoteLabel.setText(ContestConfig.SUGGESTED_OVERVOTE_LABEL);
    textFieldUndervoteLabel.setText(ContestConfig.SUGGESTED_UNDERVOTE_LABEL);

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

    radioOvervoteAlwaysSkip.setSelected(false);
    radioOvervoteExhaustImmediately.setSelected(false);
    radioOvervoteExhaustIfMultiple.setSelected(false);
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

  private static String loadTxtFileIntoString(String configFileDocumentationFilename) {
    String text;
    try {
      text =
          new BufferedReader(
              new InputStreamReader(
                  Objects.requireNonNull(
                      ClassLoader.getSystemResourceAsStream(configFileDocumentationFilename))))
              .lines()
              .collect(Collectors.joining("\n"));
    } catch (Exception exception) {
      Logger.log(
          Level.SEVERE,
          "Error loading text file: %s\n%s",
          configFileDocumentationFilename,
          exception.toString());
      text =
          String.format(
              "<Error loading text file: %s>", configFileDocumentationFilename);
    }
    return text;
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

    setUpHintsForTab(tabContestInfo, HINTS_CONTEST_INFO_FILENAME);
    setUpHintsForTab(tabCvrFiles, HINTS_CVR_FILES_FILENAME);
    setUpHintsForTab(tabCandidates, HINTS_CANDIDATES_FILENAME);
    setUpHintsForTab(tabWinningRules, HINTS_WINNING_RULES_FILENAME);
    setUpHintsForTab(tabVoterErrorRules, HINTS_VOTER_ERROR_RULES_FILENAME);
    setUpHintsForTab(tabOutput, HINTS_OUTPUT_FILENAME);
    // Necessary because the initial load doesn't count as a "selectionChanged" event
    textAreaHelp.setText(loadTxtFileIntoString(HINTS_CONTEST_INFO_FILENAME));

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
      switch (getProviderChoice(choiceCvrProvider)) {
        case ESS -> {
          buttonAddCvrFile.setDisable(false);
          textFieldCvrFilePath.setDisable(false);
          buttonCvrFilePath.setDisable(false);
          textFieldCvrFirstVoteCol.setDisable(false);
          textFieldCvrFirstVoteCol
              .setText(String.valueOf(ContestConfig.SUGGESTED_CVR_FIRST_VOTE_COLUMN));
          textFieldCvrFirstVoteRow.setDisable(false);
          textFieldCvrFirstVoteRow
              .setText(String.valueOf(ContestConfig.SUGGESTED_CVR_FIRST_VOTE_ROW));
          textFieldCvrIdCol.setDisable(false);
          textFieldCvrIdCol.setText(String.valueOf(ContestConfig.SUGGESTED_CVR_ID_COLUMN));
          textFieldCvrPrecinctCol.setDisable(false);
          textFieldCvrPrecinctCol
              .setText(String.valueOf(ContestConfig.SUGGESTED_CVR_PRECINCT_COLUMN));
          textFieldCvrOvervoteDelimiter.setDisable(false);
        }
        case CLEAR_BALLOT, DOMINION, HART, CDF -> {
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
    tableColumnCvrOvervoteDelimiter.setCellValueFactory(
        new PropertyValueFactory<>("overvoteDelimiter"));
    tableColumnCvrOvervoteDelimiter.setCellFactory(TextFieldTableCell.forTableColumn());
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
          //noinspection unchecked
          return new SimpleObjectProperty(checkBox);
        });
    tableViewCandidates.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    tableViewCandidates.setEditable(true);

    clearAndDisableWinningRuleFields();
    choiceTiebreakMode.getItems().addAll(TieBreakMode.values());
    choiceTiebreakMode.getItems().remove(TieBreakMode.MODE_UNKNOWN);
    choiceTiebreakMode.setOnAction(event -> {
      clearAndDisableTiebreakFields();
      switch (getTiebreakModeChoice(choiceTiebreakMode)) {
        case RANDOM, PREVIOUS_ROUND_COUNTS_THEN_RANDOM, GENERATE_PERMUTATION -> textFieldRandomSeed
            .setDisable(false);
      }
    });
    choiceWinnerElectionMode.getItems().addAll(WinnerElectionMode.values());
    choiceWinnerElectionMode.getItems().remove(WinnerElectionMode.MODE_UNKNOWN);
    choiceWinnerElectionMode.setOnAction(event -> {
      clearAndDisableWinningRuleFields();
      setWinningRulesDefaultValues();
      switch (getWinnerElectionModeChoice(choiceWinnerElectionMode)) {
        case STANDARD_SINGLE_WINNER -> {
          textFieldMaxRankingsAllowed.setDisable(false);
          textFieldMinimumVoteThreshold.setDisable(false);
          checkBoxBatchElimination.setDisable(false);
          checkBoxContinueUntilTwoCandidatesRemain.setDisable(false);
          choiceTiebreakMode.setDisable(false);
          textFieldNumberOfWinners.setText("1");
        }
        case MULTI_SEAT_ALLOW_ONLY_ONE_WINNER_PER_ROUND, MULTI_SEAT_ALLOW_MULTIPLE_WINNERS_PER_ROUND -> {
          textFieldMaxRankingsAllowed.setDisable(false);
          textFieldMinimumVoteThreshold.setDisable(false);
          choiceTiebreakMode.setDisable(false);
          radioThresholdMostCommon.setDisable(false);
          radioThresholdHbQuota.setDisable(false);
          radioThresholdHareQuota.setDisable(false);
          textFieldDecimalPlacesForVoteArithmetic.setDisable(false);
          textFieldNumberOfWinners.setDisable(false);
        }
        case MULTI_SEAT_BOTTOMS_UP_UNTIL_N_WINNERS, MULTI_SEAT_SEQUENTIAL_WINNER_TAKES_ALL -> {
          textFieldMaxRankingsAllowed.setDisable(false);
          textFieldMinimumVoteThreshold.setDisable(false);
          choiceTiebreakMode.setDisable(false);
          textFieldNumberOfWinners.setDisable(false);
        }
        case MULTI_SEAT_BOTTOMS_UP_USING_PERCENTAGE_THRESHOLD -> {
          textFieldMaxRankingsAllowed.setDisable(false);
          textFieldMinimumVoteThreshold.setDisable(false);
          choiceTiebreakMode.setDisable(false);
          textFieldNumberOfWinners.setText("0");
          textFieldMultiSeatBottomsUpPercentageThreshold.setDisable(false);
        }
      }
    });

    radioOvervoteAlwaysSkip.setText(Tabulator.OVERVOTE_RULE_ALWAYS_SKIP_TEXT);
    radioOvervoteExhaustImmediately.setText(Tabulator.OVERVOTE_RULE_EXHAUST_IMMEDIATELY_TEXT);
    radioOvervoteExhaustIfMultiple.setText(Tabulator.OVERVOTE_RULE_EXHAUST_IF_MULTIPLE_TEXT);

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

  private void setUpHintsForTab(Tab tab, String hintsFilename) {
    String hints = loadTxtFileIntoString(hintsFilename);
    tab.setOnSelectionChanged(e -> {
      if (tab.isSelected()) {
        textAreaHelp.setText(hints);
      }
    });
  }

  private void migrateConfigVersion(ContestConfig config) {
    if (config.rawConfig.tabulatorVersion == null
        || !config.rawConfig.tabulatorVersion.equals(Main.APP_VERSION)) {
      // Any necessary future version migration logic goes here

      if (config.getWinnerElectionMode() == WinnerElectionMode.MODE_UNKNOWN) {
        String oldWinnerElectionMode = config.rawConfig.rules.winnerElectionMode;
        switch (oldWinnerElectionMode) {
          case "standard" -> config.rawConfig.rules.winnerElectionMode =
              config.getNumberOfWinners() > 1
                  ? WinnerElectionMode.MULTI_SEAT_ALLOW_MULTIPLE_WINNERS_PER_ROUND.toString()
                  : WinnerElectionMode.STANDARD_SINGLE_WINNER.toString();
          case "singleSeatContinueUntilTwoCandidatesRemain" -> {
            config.rawConfig.rules.winnerElectionMode = WinnerElectionMode.STANDARD_SINGLE_WINNER
                .toString();
            config.rawConfig.rules.continueUntilTwoCandidatesRemain = true;
          }
          case "multiSeatAllowOnlyOneWinnerPerRound" -> config.rawConfig.rules.winnerElectionMode =
              WinnerElectionMode.MULTI_SEAT_ALLOW_ONLY_ONE_WINNER_PER_ROUND.toString();
          case "multiSeatBottomsUp" -> config.rawConfig.rules.winnerElectionMode =
              config.getNumberOfWinners() == 0
                  || config.getMultiSeatBottomsUpPercentageThreshold() != null
                  ? WinnerElectionMode.MULTI_SEAT_BOTTOMS_UP_USING_PERCENTAGE_THRESHOLD.toString()
                  : WinnerElectionMode.MULTI_SEAT_BOTTOMS_UP_UNTIL_N_WINNERS.toString();
          case "multiSeatSequentialWinnerTakesAll" -> config.rawConfig.rules.winnerElectionMode =
              WinnerElectionMode.MULTI_SEAT_SEQUENTIAL_WINNER_TAKES_ALL.toString();
          default -> {
            Logger.log(Level.WARNING,
                "winnerElectionMode \"%s\" is unrecognized! Please supply a valid "
                    + "winnerElectionMode.", oldWinnerElectionMode);
            config.rawConfig.rules.winnerElectionMode = null;
          }
        }
      }

      if (config.getTiebreakMode() == TieBreakMode.MODE_UNKNOWN) {
        Map<String, String> tiebreakModeMigrationMap = Map.of(
            "random", TieBreakMode.RANDOM.toString(),
            "interactive", TieBreakMode.INTERACTIVE.toString(),
            "previousRoundCountsThenRandom",
            TieBreakMode.PREVIOUS_ROUND_COUNTS_THEN_RANDOM.toString(),
            "previousRoundCountsThenInteractive",
            TieBreakMode.PREVIOUS_ROUND_COUNTS_THEN_INTERACTIVE.toString(),
            "usePermutationInConfig", TieBreakMode.USE_PERMUTATION_IN_CONFIG.toString(),
            "generatePermutation", TieBreakMode.GENERATE_PERMUTATION.toString()
        );
        String oldTiebreakMode = config.rawConfig.rules.tiebreakMode;
        if (tiebreakModeMigrationMap.containsKey(oldTiebreakMode)) {
          config.rawConfig.rules.tiebreakMode = tiebreakModeMigrationMap.get(oldTiebreakMode);
        } else {
          Logger.log(Level.WARNING,
              "tiebreakMode \"%s\" is unrecognized! Please supply a valid tiebreakMode.",
              oldTiebreakMode);
          config.rawConfig.rules.tiebreakMode = null;
        }
      }

      if (config.getOvervoteRule() == OvervoteRule.RULE_UNKNOWN) {
        String oldOvervoteRule = config.rawConfig.rules.overvoteRule;
        switch (oldOvervoteRule) {
          case "alwaysSkipToNextRank" -> config.rawConfig.rules.overvoteRule = OvervoteRule.ALWAYS_SKIP_TO_NEXT_RANK
              .toString();
          case "exhaustImmediately" -> config.rawConfig.rules.overvoteRule = OvervoteRule.EXHAUST_IMMEDIATELY
              .toString();
          case "exhaustIfMultipleContinuing" -> config.rawConfig.rules.overvoteRule = OvervoteRule.EXHAUST_IF_MULTIPLE_CONTINUING
              .toString();
          default -> {
            Logger.log(Level.WARNING,
                "overvoteRule \"%s\" is unrecognized! Please supply a valid overvoteRule.",
                oldOvervoteRule);
            config.rawConfig.rules.overvoteRule = null;
          }
        }
      }

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
            : config.getWinnerElectionMode());
    choiceTiebreakMode.setValue(
        config.getTiebreakMode() == TieBreakMode.MODE_UNKNOWN ? null : config.getTiebreakMode());
    setOvervoteRuleRadioButton(config.getOvervoteRule());

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
    setThresholdCalculationMethodRadioButton(rules.nonIntegerWinningThreshold, rules.hareQuota);
    checkBoxBatchElimination.setSelected(rules.batchElimination);
    checkBoxContinueUntilTwoCandidatesRemain.setSelected(rules.continueUntilTwoCandidatesRemain);
    checkBoxExhaustOnDuplicateCandidate.setSelected(rules.exhaustOnDuplicateCandidate);
    checkBoxTreatBlankAsUndeclaredWriteIn.setSelected(rules.treatBlankAsUndeclaredWriteIn);
  }

  private void setThresholdCalculationMethodRadioButton(boolean nonIntegerWinningThreshold,
      boolean hareQuota) {
    if (!nonIntegerWinningThreshold && !hareQuota) {
      radioThresholdMostCommon.setSelected(true);
    } else if (nonIntegerWinningThreshold && !hareQuota) {
      radioThresholdHbQuota.setSelected(true);
    } else if (!nonIntegerWinningThreshold) {
      radioThresholdHareQuota.setSelected(true);
    }  // If both are true, don't select any option since this should no longer be valid
  }

  private void setOvervoteRuleRadioButton(OvervoteRule overvoteRule) {
    switch (overvoteRule) {
      case ALWAYS_SKIP_TO_NEXT_RANK -> radioOvervoteAlwaysSkip.setSelected(true);
      case EXHAUST_IMMEDIATELY -> radioOvervoteExhaustImmediately.setSelected(true);
      case EXHAUST_IF_MULTIPLE_CONTINUING -> radioOvervoteExhaustIfMultiple.setSelected(true);
      case RULE_UNKNOWN -> {
        // Do nothing for unknown overvote rules
      }
    }
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
      source.setOvervoteDelimiter(
          source.getOvervoteDelimiter() != null ? source.getOvervoteDelimiter().trim() : "");
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
    rules.tiebreakMode = getTiebreakModeChoice(choiceTiebreakMode).toString();
    rules.overvoteRule = getOvervoteRuleChoice();
    rules.winnerElectionMode = getWinnerElectionModeChoice(choiceWinnerElectionMode).toString();
    rules.randomSeed = getTextOrEmptyString(textFieldRandomSeed);
    rules.numberOfWinners = getTextOrEmptyString(textFieldNumberOfWinners);
    rules.multiSeatBottomsUpPercentageThreshold = getTextOrEmptyString(
        (textFieldMultiSeatBottomsUpPercentageThreshold));
    rules.decimalPlacesForVoteArithmetic =
        getTextOrEmptyString(textFieldDecimalPlacesForVoteArithmetic);
    rules.minimumVoteThreshold = getTextOrEmptyString(textFieldMinimumVoteThreshold);
    rules.maxSkippedRanksAllowed = getTextOrEmptyString(textFieldMaxSkippedRanksAllowed);
    rules.maxRankingsAllowed = getTextOrEmptyString(textFieldMaxRankingsAllowed);
    rules.nonIntegerWinningThreshold = radioThresholdHbQuota.isSelected();
    rules.hareQuota = radioThresholdHareQuota.isSelected();
    rules.batchElimination = checkBoxBatchElimination.isSelected();
    rules.continueUntilTwoCandidatesRemain = checkBoxContinueUntilTwoCandidatesRemain.isSelected();
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
}
