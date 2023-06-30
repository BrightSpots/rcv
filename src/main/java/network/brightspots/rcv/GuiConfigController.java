/*
 * RCTab
 * Copyright (c) 2017-2023 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: GUI controller class for the JavaFX GUIApplication:
 * Event handlers for the GUI.
 * Logic for loading configs into the GUI, saving, and editing.
 * Launches validate, tabulate, and convert tasks.
 * Design: Layout resources are in GuiConfigLayout.xml.  The event handlers here are called in
 * response to GUI events on the GUI render thread.  Longer actions are done on background threads.
 * Conditions: Runs in GUI mode.
 * Version history: see https://github.com/BrightSpots/rcv.
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
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.Pair;
import javafx.util.StringConverter;
import network.brightspots.rcv.ContestConfig.Provider;
import network.brightspots.rcv.ContestConfig.ValidationError;
import network.brightspots.rcv.ContestConfigMigration.ConfigVersionIsNewerThanAppVersionException;
import network.brightspots.rcv.RawContestConfig.Candidate;
import network.brightspots.rcv.RawContestConfig.ContestRules;
import network.brightspots.rcv.RawContestConfig.CvrSource;
import network.brightspots.rcv.RawContestConfig.OutputSettings;
import network.brightspots.rcv.Tabulator.OvervoteRule;
import network.brightspots.rcv.Tabulator.TiebreakMode;
import network.brightspots.rcv.Tabulator.WinnerElectionMode;

/**
 * View controller for config layout.
 */
@SuppressWarnings({"WeakerAccess"})
public class GuiConfigController implements Initializable {

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final String HINTS_CONTEST_INFO_FILENAME =
      "network/brightspots/rcv/hints_contest_info.txt";
  private static final String HINTS_CVR_FILES_FILENAME =
      "network/brightspots/rcv/hints_cvr_files.txt";
  private static final String HINTS_CANDIDATES_FILENAME =
      "network/brightspots/rcv/hints_candidates.txt";
  private static final String HINTS_WINNING_RULES_FILENAME =
      "network/brightspots/rcv/hints_winning_rules.txt";
  private static final String HINTS_VOTER_ERROR_RULES_FILENAME =
      "network/brightspots/rcv/hints_voter_error_rules.txt";
  private static final String HINTS_OUTPUT_FILENAME = "network/brightspots/rcv/hints_output.txt";
  // It's possible for file paths to legitimately have consecutive semicolons, but unlikely
  private static final String CVR_FILE_PATH_DELIMITER = ";;";

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
  private TableColumn<CvrSource, String> tableColumnCvrOvervoteLabel;
  @FXML
  private TableColumn<CvrSource, String> tableColumnCvrUndervoteLabel;
  @FXML
  private TableColumn<CvrSource, String> tableColumnCvrUndeclaredWriteInLabel;
  @FXML
  private TableColumn<CvrSource, Boolean> tableColumnCvrTreatBlankAsUndeclaredWriteIn;
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
  private TextField textFieldCvrOvervoteLabel;
  @FXML
  private TextField textFieldCvrUndervoteLabel;
  @FXML
  private TextField textFieldCvrUndeclaredWriteInLabel;
  @FXML
  private CheckBox checkBoxCvrTreatBlankAsUndeclaredWriteIn;
  @FXML
  private TableView<Candidate> tableViewCandidates;
  @FXML
  private TableColumn<Candidate, String> tableColumnCandidateName;
  @FXML
  private TableColumn<Candidate, String> tableColumnCandidateAliases;
  @FXML
  private TableColumn<Candidate, Boolean> tableColumnCandidateExcluded;
  @FXML
  private TextField textFieldCandidateName;
  @FXML
  private CheckBox checkBoxCandidateExcluded;
  @FXML
  private TextArea textAreaCandidateAliases;
  @FXML
  private ChoiceBox<TiebreakMode> choiceTiebreakMode;
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
  private CheckBox checkBoxMaxSkippedRanksAllowedUnlimited;
  @FXML
  private TextField textFieldMaxRankingsAllowed;
  @FXML
  private CheckBox checkBoxMaxRankingsAllowedMax;
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
  private CheckBox checkBoxFirstRoundDeterminesThreshold;
  @FXML
  private CheckBox checkBoxPreventOneCandidateInFinalRound;
  @FXML
  private TextField textFieldStopTabulationEarlyAfterRound;
  @FXML
  private CheckBox checkBoxExhaustOnDuplicateCandidate;
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
    return choiceBox.getValue() != null ? Provider
        .getByInternalLabel(choiceBox.getValue().getInternalLabel())
        : Provider.PROVIDER_UNKNOWN;
  }

  private static WinnerElectionMode getWinnerElectionModeChoice(
      ChoiceBox<WinnerElectionMode> choiceBox) {
    return choiceBox.getValue() != null ? WinnerElectionMode
        .getByInternalLabel(choiceBox.getValue().getInternalLabel())
        : WinnerElectionMode.MODE_UNKNOWN;
  }

  private static TiebreakMode getTiebreakModeChoice(ChoiceBox<TiebreakMode> choiceBox) {
    return choiceBox.getValue() != null ? TiebreakMode
        .getByInternalLabel(choiceBox.getValue().getInternalLabel())
        : TiebreakMode.MODE_UNKNOWN;
  }

  private static String getTextOrEmptyString(TextField textField) {
    return textField.getText() != null ? textField.getText().trim() : "";
  }

  private static String loadTxtFileIntoString(String configFileDocumentationFilename) {
    String text;
    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(
                Objects.requireNonNull(
                    ClassLoader.getSystemResourceAsStream(configFileDocumentationFilename)),
                StandardCharsets.UTF_8))) {
      text = reader.lines().collect(Collectors.joining("\n"));
    } catch (Exception exception) {
      Logger.severe("Error loading text file: %s\n%s", configFileDocumentationFilename, exception);
      text = String.format("<Error loading text file: %s>", configFileDocumentationFilename);
    }
    return text;
  }

  private static void addErrorStyling(Control control) {
    if (control instanceof CheckBox) {
      control.getStyleClass().add("check-box-error");
    } else {
      control.getStyleClass().add("error");
    }
  }

  private static void clearErrorStyling(Control control) {
    if (control instanceof CheckBox) {
      control.getStyleClass().removeAll("check-box-error");
    } else {
      control.getStyleClass().removeAll("error");
    }
  }

  private String getOvervoteRuleChoice() {
    OvervoteRule rule = OvervoteRule.RULE_UNKNOWN;
    if (radioOvervoteAlwaysSkip.isSelected()) {
      rule = OvervoteRule.ALWAYS_SKIP_TO_NEXT_RANK;
    } else if (radioOvervoteExhaustImmediately.isSelected()) {
      rule = OvervoteRule.EXHAUST_IMMEDIATELY;
    } else if (radioOvervoteExhaustIfMultiple.isSelected()) {
      rule = OvervoteRule.EXHAUST_IF_MULTIPLE_CONTINUING;
    }
    return rule.getInternalLabel();
  }

  /**
   * Action when help menu item is clicked. Try to open the local help manual.
   */
  public void menuItemOpenHelpClicked() {
    ButtonType saveButton = new ButtonType("Ok", ButtonBar.ButtonData.YES);
    Alert alert =
        new Alert(
            AlertType.INFORMATION,
            "You can find more information in the config_file_documentation.txt file\n"
                + "included in the docs directory of the application folder.",
            saveButton);
    alert.setHeaderText("");
    alert.showAndWait();
  }

  /**
   * Action when new config menu item is clicked.
   */
  public void menuItemNewConfigClicked() {
    if (checkForSaveAndContinue()) {
      Logger.info("Creating new contest config...");
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
      try {
        loadConfig(GuiContext.getInstance().getConfig());
        labelCurrentlyLoaded.setText("Currently loaded: " + fileToLoad.getAbsolutePath());
      } catch (ConfigVersionIsNewerThanAppVersionException exception) {
        // error is logged; nothing else to do here
      }
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
    Pair<String, Boolean> filePathAndTempStatus = commitConfigToFileAndGetFilePath();
    if (filePathAndTempStatus != null) {
      if (GuiContext.getInstance().getConfig() != null) {
        String operatorName = askUserForName();
        setGuiIsBusy(true);
        TabulatorService service = new TabulatorService(
            filePathAndTempStatus.getKey(), operatorName, filePathAndTempStatus.getValue());
        setUpAndStartService(service);
      } else {
        Logger.warning("Please load a contest config file before attempting to tabulate!");
      }
    }
  }

  /**
   * Convert CVRs in current config to CDF. Requires user to save if there are unsaved changes, and
   * create and launches ConvertToCdfService from the saved config path.
   */
  public void menuItemConvertToCdfClicked() {
    Pair<String, Boolean> filePathAndTempStatus = commitConfigToFileAndGetFilePath();
    if (filePathAndTempStatus != null) {
      if (GuiContext.getInstance().getConfig() != null) {
        setGuiIsBusy(true);
        ConvertToCdfService service = new ConvertToCdfService(
            filePathAndTempStatus.getKey(), filePathAndTempStatus.getValue());
        setUpAndStartService(service);
      } else {
        Logger.warning("Please load a contest config file before attempting to convert to CDF!");
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
          Logger.severe("User exited tabulator before it was finished!");
        } else {
          Logger.info("Exiting tabulator GUI...");
        }
        Platform.exit();
      }
    } else if (checkForSaveAndContinue()) {
      Logger.info("Exiting tabulator GUI...");
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

  private List<File> chooseFile(Provider provider, ExtensionFilter filter) {
    FileChooser fc = new FileChooser();
    fc.setInitialDirectory(new File(FileUtils.getUserDirectory()));
    fc.getExtensionFilters().add(filter);
    fc.setTitle("Select " + provider + " Cast Vote Record Files");
    return fc.showOpenMultipleDialog(GuiContext.getInstance().getMainWindow());
  }

  /**
   * Action when CVR file path button is clicked.
   */
  public void buttonCvrFilePathClicked() {
    List<File> selectedFiles = null;
    File selectedDirectory = null;

    Provider provider = getProviderChoice(choiceCvrProvider);
    switch (provider) {
      case CDF -> selectedFiles =
          chooseFile(provider, new ExtensionFilter("JSON and XML files", "*.json", "*.xml"));
      case CLEAR_BALLOT, CSV -> selectedFiles =
          chooseFile(provider, new ExtensionFilter("CSV files", "*.csv"));
      case DOMINION, HART -> {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setInitialDirectory(new File(FileUtils.getUserDirectory()));
        dc.setTitle("Select " + provider + " Cast Vote Record Folder");
        selectedDirectory = dc.showDialog(GuiContext.getInstance().getMainWindow());
      }
      case ESS -> selectedFiles =
          chooseFile(provider, new ExtensionFilter("Excel files", "*.xls", "*.xlsx"));
      default -> {
        // Do nothing for unhandled providers
      }
    }

    if (selectedFiles != null) {
      textFieldCvrFilePath.setText(selectedFiles.stream().map(File::getAbsolutePath).collect(
          Collectors.joining(CVR_FILE_PATH_DELIMITER)));
    } else if (selectedDirectory != null) {
      textFieldCvrFilePath.setText(selectedDirectory.getAbsolutePath());
    }
  }

  /**
   * Action when add CVR file button is clicked.
   */
  public void buttonAddCvrFileClicked() {
    clearBasicCvrValidationHighlighting();
    List<String> cvrPaths = Arrays.stream(getTextOrEmptyString(textFieldCvrFilePath).split(
        CVR_FILE_PATH_DELIMITER)).toList();
    List<String> failedFilePaths = new ArrayList<>();
    String firstVoteColumnIndex = getTextOrEmptyString(textFieldCvrFirstVoteCol);
    String firstVoteRowIndex = getTextOrEmptyString(textFieldCvrFirstVoteRow);
    String idColumnIndex = getTextOrEmptyString(textFieldCvrIdCol);
    String precinctColumnIndex = getTextOrEmptyString(textFieldCvrPrecinctCol);
    String overvoteDelimiter = getTextOrEmptyString(textFieldCvrOvervoteDelimiter);
    String provider = getProviderChoice(choiceCvrProvider).getInternalLabel();
    String contestId = getTextOrEmptyString(textFieldCvrContestId);
    String overvoteLabel = getTextOrEmptyString(textFieldCvrOvervoteLabel);
    String undervoteLabel = getTextOrEmptyString(textFieldCvrUndervoteLabel);
    String undeclaredWriteInLabel = getTextOrEmptyString(textFieldCvrUndeclaredWriteInLabel);
    boolean treatBlankAsUndeclaredWriteIn = checkBoxCvrTreatBlankAsUndeclaredWriteIn.isSelected();

    cvrPaths.forEach(filePath -> {
      CvrSource cvrSource =
          new CvrSource(
              filePath,
              firstVoteColumnIndex,
              firstVoteRowIndex,
              idColumnIndex,
              precinctColumnIndex,
              overvoteDelimiter,
              provider,
              contestId,
              overvoteLabel,
              undervoteLabel,
              undeclaredWriteInLabel,
              treatBlankAsUndeclaredWriteIn
          );
      Set<ValidationError> validationErrors =
          ContestConfig.performBasicCvrSourceValidation(cvrSource);
      if (validationErrors.isEmpty()) {
        tableViewCvrFiles.getItems().add(cvrSource);
      } else {
        highlightInputsFailingBasicCvrValidation(validationErrors);
        failedFilePaths.add(filePath);
      }
    });
    // If any entries failed validation, preserve them in the text box so the user can try again
    textFieldCvrFilePath.setText(String.join(CVR_FILE_PATH_DELIMITER, failedFilePaths));
  }

  private void clearBasicCvrValidationHighlighting() {
    List<Control> controlsToClear = Arrays.asList(
        textFieldCvrFilePath,
        textFieldCvrContestId,
        textFieldCvrFirstVoteCol,
        textFieldCvrFirstVoteRow,
        textFieldCvrIdCol,
        textFieldCvrPrecinctCol,
        textFieldCvrOvervoteDelimiter,
        textFieldCvrOvervoteLabel,
        textFieldCvrUndervoteLabel,
        textFieldCvrUndeclaredWriteInLabel
    );
    controlsToClear.forEach(GuiConfigController::clearErrorStyling);
  }

  private void highlightInputsFailingBasicCvrValidation(Set<ValidationError> validationErrors) {
    // Only highlight fields if it's possible to trigger the validation errors via the GUI (i.e.
    // don't highlight if the elements in question are disabled, and it's not possible to get into
    // the invalid state).

    if (validationErrors.contains(ValidationError.CVR_FILE_PATH_MISSING)
        || validationErrors.contains(ValidationError.CVR_CDF_FILE_PATH_INVALID)) {
      addErrorStyling(textFieldCvrFilePath);
    }

    if (validationErrors.contains(ValidationError.CVR_CONTEST_ID_INVALID)) {
      addErrorStyling(textFieldCvrContestId);
    }

    if (validationErrors.contains(ValidationError.CVR_FIRST_VOTE_COLUMN_INVALID)) {
      addErrorStyling(textFieldCvrFirstVoteCol);
    }

    if (validationErrors.contains(ValidationError.CVR_FIRST_VOTE_ROW_INVALID)) {
      addErrorStyling(textFieldCvrFirstVoteRow);
    }

    if (validationErrors.contains(ValidationError.CVR_ID_COLUMN_INVALID)) {
      addErrorStyling(textFieldCvrIdCol);
    }

    if (validationErrors.contains(ValidationError.CVR_PRECINCT_COLUMN_INVALID)) {
      addErrorStyling(textFieldCvrPrecinctCol);
    }

    if (validationErrors.contains(ValidationError.CVR_OVERVOTE_DELIMITER_INVALID)) {
      addErrorStyling(textFieldCvrOvervoteDelimiter);
    }

    if (validationErrors.contains(ValidationError.CVR_OVERVOTE_LABEL_INVALID)) {
      addErrorStyling(textFieldCvrOvervoteLabel);
    }

    if (validationErrors.contains(ValidationError.CVR_UNDERVOTE_LABEL_INVALID)) {
      addErrorStyling(textFieldCvrUndervoteLabel);
    }

    if (validationErrors.contains(ValidationError.CVR_UWI_LABEL_INVALID)) {
      addErrorStyling(textFieldCvrUndeclaredWriteInLabel);
    }

    if (validationErrors.contains(ValidationError.CVR_OVERVOTE_DELIMITER_AND_LABEL_BOTH_SUPPLIED)) {
      addErrorStyling(textFieldCvrOvervoteDelimiter);
      addErrorStyling(textFieldCvrOvervoteLabel);
    }
  }

  /**
   * Action when clear CVR file button is clicked.
   */
  public void buttonClearCvrFieldsClicked() {
    choiceCvrProvider.setValue(null);
    clearAndDisableCvrFilesTabFields();
  }

  private void clearAndDisableCvrFilesTabFields() {
    clearBasicCvrValidationHighlighting();
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
    textFieldCvrOvervoteLabel.clear();
    textFieldCvrOvervoteLabel.setDisable(true);
    textFieldCvrUndervoteLabel.clear();
    textFieldCvrUndervoteLabel.setDisable(true);
    textFieldCvrUndeclaredWriteInLabel.clear();
    textFieldCvrUndeclaredWriteInLabel.setDisable(true);
    checkBoxCvrTreatBlankAsUndeclaredWriteIn.setSelected(false);
    checkBoxCvrTreatBlankAsUndeclaredWriteIn.setDisable(true);
  }

  /**
   * Action when delete CVR file button is clicked.
   */
  public void buttonDeleteCvrFileClicked() {
    tableViewCvrFiles
        .getItems()
        .removeAll(tableViewCvrFiles.getSelectionModel().getSelectedItems());
  }

  /** Action when "Auto-Load Candidates" button is clicked. */
  public void buttonAutoLoadCandidatesClicked() {
    setGuiIsBusy(true);
    ContestConfig config =
          ContestConfig.loadContestConfig(createRawContestConfig(), FileUtils.getUserDirectory());
    AutoLoadCandidatesService service = new AutoLoadCandidatesService(
          config,
          tableViewCvrFiles.getItems(),
          tableViewCandidates);
    setUpAndStartService(service);
  }

  /**
   * Action when add candidate button is clicked.
   */
  public void buttonAddCandidateClicked() {
    clearErrorStyling(textFieldCandidateName);
    Candidate candidate =
        new Candidate(
            getTextOrEmptyString(textFieldCandidateName),
            textAreaCandidateAliases.getText(),
            checkBoxCandidateExcluded.isSelected());
    Set<ValidationError> validationErrors =
        ContestConfig.performBasicCandidateValidation(candidate);
    if (validationErrors.isEmpty()) {
      tableViewCandidates.getItems().add(candidate);
      buttonClearCandidateClicked();
    } else if (validationErrors.contains(ValidationError.CANDIDATE_NAME_MISSING)) {
      addErrorStyling(textFieldCandidateName);
    }
  }

  /**
   * Action when clear candidate button is clicked.
   */
  public void buttonClearCandidateClicked() {
    clearErrorStyling(textFieldCandidateName);
    textFieldCandidateName.clear();
    textAreaCandidateAliases.clear();
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

  private void clearAndDisableWinningRuleFields() {
    textFieldMaxRankingsAllowed.clear();
    textFieldMaxRankingsAllowed.setDisable(true);
    checkBoxMaxRankingsAllowedMax.setSelected(false);
    checkBoxMaxRankingsAllowedMax.setDisable(true);
    textFieldMinimumVoteThreshold.clear();
    textFieldMinimumVoteThreshold.setDisable(true);
    textFieldStopTabulationEarlyAfterRound.clear();
    textFieldStopTabulationEarlyAfterRound.setDisable(true);
    checkBoxBatchElimination.setSelected(false);
    checkBoxBatchElimination.setDisable(true);
    checkBoxContinueUntilTwoCandidatesRemain.setSelected(false);
    checkBoxContinueUntilTwoCandidatesRemain.setDisable(true);
    checkBoxFirstRoundDeterminesThreshold.setSelected(false);
    checkBoxFirstRoundDeterminesThreshold.setDisable(true);
    checkBoxPreventOneCandidateInFinalRound.setSelected(false);
    checkBoxPreventOneCandidateInFinalRound.setDisable(true);
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
    checkBoxFirstRoundDeterminesThreshold
            .setSelected(ContestConfig.SUGGESTED_FIRST_ROUND_DETERMINES_THRESHOLD);
    checkBoxPreventOneCandidateInFinalRound
        .setSelected(ContestConfig.SUGGESTED_PREVENT_ONE_CANDIDATE_FINAL_ROUND);
    textFieldDecimalPlacesForVoteArithmetic.setText(
        String.valueOf(ContestConfig.SUGGESTED_DECIMAL_PLACES_FOR_VOTE_ARITHMETIC));
    checkBoxMaxRankingsAllowedMax.setSelected(ContestConfig.SUGGESTED_MAX_RANKINGS_ALLOWED_MAXIMUM);
  }

  private void setDefaultValues() {
    labelCurrentlyLoaded.setText("Currently loaded: <New Config>");

    checkBoxCandidateExcluded.setSelected(ContestConfig.SUGGESTED_CANDIDATE_EXCLUDED);

    setWinningRulesDefaultValues();

    textFieldMaxSkippedRanksAllowed.setText(
        String.valueOf(ContestConfig.SUGGESTED_MAX_SKIPPED_RANKS_ALLOWED));
    checkBoxMaxSkippedRanksAllowedUnlimited
        .setSelected(ContestConfig.SUGGESTED_MAX_SKIPPED_RANKS_ALLOWED_UNLIMITED);
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
    textAreaCandidateAliases.clear();
    checkBoxCandidateExcluded.setSelected(false);
    tableViewCandidates.getItems().clear();

    choiceWinnerElectionMode.setValue(null);
    clearAndDisableWinningRuleFields();

    radioOvervoteAlwaysSkip.setSelected(false);
    radioOvervoteExhaustImmediately.setSelected(false);
    radioOvervoteExhaustIfMultiple.setSelected(false);
    textFieldMaxSkippedRanksAllowed.clear();
    textFieldMaxSkippedRanksAllowed.setDisable(false);
    checkBoxMaxSkippedRanksAllowedUnlimited.setSelected(false);
    checkBoxExhaustOnDuplicateCandidate.setSelected(false);

    textFieldOutputDirectory.clear();
    checkBoxTabulateByPrecinct.setSelected(false);
    checkBoxGenerateCdfJson.setSelected(false);

    setDefaultValues();
  }

  /*
   * Compares the GUI configuration with the on-disk configuration.
   * If they differ, also tells you if the on-disk version is "TEST" --
   * in which case, you may be okay with a difference for ease of development.
   */
  private ConfigComparisonResult compareConfigs() {
    ConfigComparisonResult comparisonResult = ConfigComparisonResult.DIFFERENT;
    try {
      String currentConfigString =
          new ObjectMapper()
              .writer()
              .withDefaultPrettyPrinter()
              .writeValueAsString(createRawContestConfig());
      if (selectedFile == null && currentConfigString.equals(emptyConfigString)) {
        // All fields are currently empty / default values so no point in asking to save
        comparisonResult = ConfigComparisonResult.SAME;
      } else if (GuiContext.getInstance().getConfig() != null) {
        // Compare to version currently saved on the hard drive
        RawContestConfig configFromFile =
            JsonParser.readFromFileWithoutLogging(
                selectedFile.getAbsolutePath(), RawContestConfig.class);
        String savedConfigString =
            new ObjectMapper()
                .writer()
                .withDefaultPrettyPrinter()
                .writeValueAsString(configFromFile);
        if (currentConfigString.equals(savedConfigString)) {
          comparisonResult = ConfigComparisonResult.SAME;
        } else if (configFromFile.tabulatorVersion.equals(ContestConfig.AUTOMATED_TEST_VERSION)) {
          comparisonResult = ConfigComparisonResult.DIFFERENT_BUT_VERSION_IS_TEST;
        }
        // Otherwise, comparisonResult should remain ConfigComparisonResult.DIFFERENT
      }
    } catch (JsonProcessingException exception) {
      Logger.warning(
          "Unable tell if saving is necessary, but everything should work fine anyway! Prompting "
              + "for save just in case...\n%s",
          exception);
    }
    return comparisonResult;
  }

  /**
   * Returns whether user entered a name.
   */
  private String askUserForName() {
    TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle("Enter your name");
    dialog.setHeaderText("For auditing purposes, enter the name(s) of everyone currently "
        + "operating this machine.");
    dialog.setContentText("Name:");
    Optional<String> result = dialog.showAndWait();

    return result.isPresent() ? result.get() : null;
  }

  private boolean checkForSaveAndContinue() {
    boolean willContinue = false;
    ConfigComparisonResult comparisonResult = compareConfigs();
    if (comparisonResult != ConfigComparisonResult.SAME) {
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

  /**
   * Takes the configuration specified in the UI and returns a filename.
   * If the UI config is equal to the filename on disk, returns that.
   * Otherwise:
   *   If the on-disk config has version TEST, user has the option to write a temporary file.
   *   Otherwise, user must save a file, and it returns that filename.
   *
   * @return the filename and whether it's a temporary file or not.
   */
  private Pair<String, Boolean> commitConfigToFileAndGetFilePath() {
    Pair<String, Boolean> filePathAndTempStatus = null;
    ConfigComparisonResult comparisonResult = compareConfigs();
    if (comparisonResult != ConfigComparisonResult.SAME) {
      // Three possible buttons, but only Save/Cancel shown unless version is TEST
      ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.YES);
      ButtonType useTempButton = new ButtonType("Use Temporary Config", ButtonBar.ButtonData.NO);
      ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

      // Pop up either a two-button or three-button alert
      Alert alert;
      if (comparisonResult == ConfigComparisonResult.DIFFERENT_BUT_VERSION_IS_TEST) {
        alert =
            new Alert(
                AlertType.WARNING,
                "You are using a test config. You must either save your changes, or use a "
                    + "temporary config file which will be deleted when you exit the program.",
                saveButton,
                useTempButton,
                cancelButton);
      } else {
        alert = new Alert(AlertType.WARNING,
            "You must either save your changes before continuing or load a new contest config!",
            saveButton, cancelButton);
      }
      alert.setHeaderText(null);
      Optional<ButtonType> result = alert.showAndWait();

      // Process the result, handling all three buttons and the "X"
      if (result.isPresent() && result.get() == saveButton) {
        File fileToSave = getSaveFile();
        if (fileToSave != null) {
          saveFile(fileToSave);
          filePathAndTempStatus = new Pair<>(fileToSave.getAbsolutePath(), false);
        }
      } else if (result.isPresent() && result.get() == useTempButton) {
        File tempFile = new File(selectedFile.getAbsolutePath() + ".temp");
        saveFile(tempFile);
        filePathAndTempStatus = new Pair<>(tempFile.getAbsolutePath(), true);
      }
      // The cancel or "X" button shouldn't cause any action to be taken, and return a null filename
    } else {
      filePathAndTempStatus = new Pair<>(selectedFile.getAbsolutePath(), false);
    }
    return filePathAndTempStatus;
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    Logger.addGuiLogging(this.textAreaStatus);
    Logger.info("Opening tabulator GUI...");
    Logger.info("Welcome to %s version %s!", Main.APP_NAME, Main.APP_VERSION);

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
      textFieldCvrUndeclaredWriteInLabel.setDisable(false);
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
          textFieldCvrOvervoteLabel.setDisable(false);
          textFieldCvrOvervoteLabel.setText(ContestConfig.SUGGESTED_OVERVOTE_LABEL);
          textFieldCvrUndervoteLabel.setDisable(false);
          textFieldCvrUndervoteLabel.setText(ContestConfig.SUGGESTED_UNDERVOTE_LABEL);
          checkBoxCvrTreatBlankAsUndeclaredWriteIn.setDisable(false);
          checkBoxCvrTreatBlankAsUndeclaredWriteIn
              .setSelected(ContestConfig.SUGGESTED_TREAT_BLANK_AS_UNDECLARED_WRITE_IN);
        }
        case CSV -> {
          buttonAddCvrFile.setDisable(false);
          textFieldCvrFilePath.setDisable(false);
          buttonCvrFilePath.setDisable(false);
          textFieldCvrFirstVoteCol.setDisable(false);
          textFieldCvrFirstVoteCol
                  .setText(String.valueOf(ContestConfig.SUGGESTED_CVR_FIRST_VOTE_COLUMN));
          textFieldCvrFirstVoteRow.setDisable(false);
          textFieldCvrFirstVoteRow
                  .setText(String.valueOf(ContestConfig.SUGGESTED_CVR_FIRST_VOTE_ROW));
        }
        case CLEAR_BALLOT, DOMINION, HART -> {
          buttonAddCvrFile.setDisable(false);
          textFieldCvrFilePath.setDisable(false);
          buttonCvrFilePath.setDisable(false);
          textFieldCvrContestId.setDisable(false);
        }
        case CDF -> {
          buttonAddCvrFile.setDisable(false);
          textFieldCvrFilePath.setDisable(false);
          buttonCvrFilePath.setDisable(false);
          textFieldCvrContestId.setDisable(false);
          textFieldCvrOvervoteLabel.setDisable(false);
          textFieldCvrOvervoteLabel.setText(ContestConfig.SUGGESTED_OVERVOTE_LABEL);
        }
        case PROVIDER_UNKNOWN -> {
          // Do nothing
        }
        default -> throw new IllegalStateException(
            "Unexpected value: " + getProviderChoice(choiceCvrProvider));
      }
    });
    EditableColumn[] cvrStringColumnsAndProperties = new EditableColumn[]{
        new EditableColumnString(tableColumnCvrFilePath, "filePath"),
        new EditableColumnString(tableColumnCvrFirstVoteCol, "firstVoteColumnIndex"),
        new EditableColumnString(tableColumnCvrFirstVoteRow, "firstVoteRowIndex"),
        new EditableColumnString(tableColumnCvrIdCol, "idColumnIndex"),
        new EditableColumnString(tableColumnCvrPrecinctCol, "precinctColumnIndex"),
        new EditableColumnString(tableColumnCvrOvervoteDelimiter, "overvoteDelimiter"),
        new EditableColumnString(tableColumnCvrContestId, "contestId"),
        new EditableColumnString(tableColumnCvrOvervoteLabel, "overvoteLabel"),
        new EditableColumnString(tableColumnCvrUndervoteLabel, "undervoteLabel"),
        new EditableColumnString(tableColumnCvrUndeclaredWriteInLabel,
           "undeclaredWriteInLabel"),
        new EditableColumnBoolean(tableColumnCvrTreatBlankAsUndeclaredWriteIn,
           "treatBlankAsUndeclaredWriteIn"),
    };
    setUpEditableTableStrings(cvrStringColumnsAndProperties);

    // Don't allow editing of Provider to avoid:
    // (1) the complexity of creating a dropdown
    // (2) the complexity of mapping between the GUI label and the internal label
    tableColumnCvrProvider.setCellValueFactory(
        c -> new SimpleStringProperty(
            Provider.getByInternalLabel(c.getValue().getProvider()).toString())
    );

    tableViewCvrFiles.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    tableViewCvrFiles.setEditable(true);

    EditableColumn[] candidateStringColumnsAndProperties = new EditableColumn[]{
        new EditableColumnString(tableColumnCandidateName, "name"),
        new EditableColumnList(tableColumnCandidateAliases, "aliases"),
        new EditableColumnBoolean(tableColumnCandidateExcluded, "excluded")
    };
    setUpEditableTableStrings(candidateStringColumnsAndProperties);

    EditableTableCellInline.lockWhileEditing(tabContestInfo);
    EditableTableCellInline.lockWhileEditing(tabCvrFiles);
    EditableTableCellInline.lockWhileEditing(tabCandidates);
    EditableTableCellInline.lockWhileEditing(tabWinningRules);
    EditableTableCellInline.lockWhileEditing(tabVoterErrorRules);
    EditableTableCellInline.lockWhileEditing(tabOutput);
    EditableTableCellInline.lockWhileEditing(menuBar);
    // Also disable all visible buttons. This is pretty hacky, but the shortest path to
    // find all visible buttons without traversing the entire scene.
    for (Node child : tabPane.lookupAll(".disableWhileEditingTable")) {
      EditableTableCellInline.lockWhileEditing(child);
    }

    tableViewCandidates.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    tableViewCandidates.setEditable(true);

    // Let's also catch programming errors -- all of these properties must have corresponding
    // functions in order to have the PropertyValueFactory work correctly.
    if (isAnyPropertyValueFunctionMissing(cvrStringColumnsAndProperties, CvrSource.class)) {
      throw new RuntimeException("Not all PropertyValueFactory functions exist for CvrSource");
    }
    if (isAnyPropertyValueFunctionMissing(candidateStringColumnsAndProperties, Candidate.class)) {
      throw new RuntimeException("Not all PropertyValueFactory functions exist for Candidate");
    }

    clearAndDisableWinningRuleFields();
    choiceTiebreakMode.getItems().addAll(TiebreakMode.values());
    choiceTiebreakMode.getItems().remove(TiebreakMode.MODE_UNKNOWN);
    choiceTiebreakMode.setOnAction(event -> {
      clearAndDisableTiebreakFields();
      switch (getTiebreakModeChoice(choiceTiebreakMode)) {
        case RANDOM, PREVIOUS_ROUND_COUNTS_THEN_RANDOM, GENERATE_PERMUTATION -> textFieldRandomSeed
            .setDisable(false);
        case INTERACTIVE, PREVIOUS_ROUND_COUNTS_THEN_INTERACTIVE, USE_PERMUTATION_IN_CONFIG,
            MODE_UNKNOWN -> {
          // Do nothing
        }
        default -> throw new IllegalStateException("Unexpected value: "
            + getTiebreakModeChoice(choiceTiebreakMode));
      }
    });
    choiceWinnerElectionMode.getItems().addAll(WinnerElectionMode.values());
    choiceWinnerElectionMode.getItems().remove(WinnerElectionMode.MODE_UNKNOWN);
    choiceWinnerElectionMode.setOnAction(event -> {
      clearAndDisableWinningRuleFields();
      setWinningRulesDefaultValues();
      checkBoxMaxRankingsAllowedMax.setDisable(false);
      textFieldMinimumVoteThreshold.setDisable(false);
      textFieldStopTabulationEarlyAfterRound.setDisable(false);
      choiceTiebreakMode.setDisable(false);
      switch (getWinnerElectionModeChoice(choiceWinnerElectionMode)) {
        case STANDARD_SINGLE_WINNER -> {
          checkBoxBatchElimination.setDisable(false);
          checkBoxContinueUntilTwoCandidatesRemain.setDisable(false);
          checkBoxFirstRoundDeterminesThreshold.setDisable(false);
          checkBoxPreventOneCandidateInFinalRound.setDisable(false);
          textFieldNumberOfWinners.setText("1");
        }
        case MULTI_SEAT_ALLOW_ONLY_ONE_WINNER_PER_ROUND,
            MULTI_SEAT_ALLOW_MULTIPLE_WINNERS_PER_ROUND -> {
          radioThresholdMostCommon.setDisable(false);
          radioThresholdHbQuota.setDisable(false);
          radioThresholdHareQuota.setDisable(false);
          textFieldDecimalPlacesForVoteArithmetic.setDisable(false);
          textFieldNumberOfWinners.setDisable(false);
        }
        case MULTI_SEAT_BOTTOMS_UP_UNTIL_N_WINNERS -> textFieldNumberOfWinners.setDisable(false);
        case MULTI_SEAT_SEQUENTIAL_WINNER_TAKES_ALL -> {
          textFieldNumberOfWinners.setDisable(false);
          checkBoxBatchElimination.setDisable(false);
          checkBoxContinueUntilTwoCandidatesRemain.setDisable(false);
        }
        case MULTI_SEAT_BOTTOMS_UP_USING_PERCENTAGE_THRESHOLD -> {
          textFieldNumberOfWinners.setText("0");
          textFieldMultiSeatBottomsUpPercentageThreshold.setDisable(false);
        }
        case MODE_UNKNOWN -> {
          // Do nothing
        }
        default -> throw new IllegalStateException("Unexpected value: "
            + getWinnerElectionModeChoice(choiceWinnerElectionMode));
      }
    });
    checkBoxMaxRankingsAllowedMax.setOnAction(event -> {
      textFieldMaxRankingsAllowed.clear();
      textFieldMaxRankingsAllowed.setDisable(
          checkBoxMaxRankingsAllowedMax.isSelected());
    });

    radioOvervoteAlwaysSkip.setText(Tabulator.OVERVOTE_RULE_ALWAYS_SKIP_TEXT);
    radioOvervoteExhaustImmediately.setText(Tabulator.OVERVOTE_RULE_EXHAUST_IMMEDIATELY_TEXT);
    radioOvervoteExhaustIfMultiple.setText(Tabulator.OVERVOTE_RULE_EXHAUST_IF_MULTIPLE_TEXT);
    checkBoxMaxSkippedRanksAllowedUnlimited.setOnAction(event -> {
      textFieldMaxSkippedRanksAllowed.clear();
      textFieldMaxSkippedRanksAllowed.setDisable(
          checkBoxMaxSkippedRanksAllowedUnlimited.isSelected());
    });

    setDefaultValues();

    try {
      emptyConfigString =
          new ObjectMapper()
              .writer()
              .withDefaultPrettyPrinter()
              .writeValueAsString(createRawContestConfig());
    } catch (JsonProcessingException exception) {
      Logger.warning(
          "Unable to set emptyConfigString, but everything should work fine anyway!\n%s",
          exception);
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

  private void setUpEditableTableStrings(EditableColumn[] editableColumns) {
    for (EditableColumn editableColumn : editableColumns) {
      editableColumn.setCellFactoryValue();
      editableColumn.setCellFactory();
    }
  }

  private boolean isAnyPropertyValueFunctionMissing(EditableColumn[] columnsAndProperties,
      Class<?> rowType) {
    boolean doAllExist = true;
    for (EditableColumn editableColumn : columnsAndProperties) {
      String setter = "set"
          + editableColumn.propertyName.substring(0, 1).toUpperCase()
          + editableColumn.propertyName.substring(1);
      String getter = "get"
          + editableColumn.propertyName.substring(0, 1).toUpperCase()
          + editableColumn.propertyName.substring(1);
      String property = editableColumn.propertyName + "Property";

      try {
        rowType.getMethod(getter);
        rowType.getMethod(setter, editableColumn.propertyType());
        rowType.getMethod(property);
      } catch (NoSuchMethodException e) {
        Logger.severe("Could not find required function %s", e.getMessage());
        doAllExist = false;
      }
    }

    return !doAllExist;
  }

  private void loadConfig(ContestConfig config) throws ConfigVersionIsNewerThanAppVersionException {
    clearConfig();
    RawContestConfig rawConfig = config.getRawConfig();
    ContestConfigMigration.migrateConfigVersion(config);
    OutputSettings outputSettings = rawConfig.outputSettings;
    textFieldContestName.setText(outputSettings.contestName);
    textFieldOutputDirectory.setText(config.getOutputDirectoryRaw());
    if (!isNullOrBlank(outputSettings.contestDate)) {
      try {
        datePickerContestDate.setValue(
            LocalDate.parse(outputSettings.contestDate, DATE_TIME_FORMATTER));
      } catch (DateTimeParseException exception) {
        Logger.severe("Invalid contestDate: %s!", outputSettings.contestDate);
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
        config.getTiebreakMode() == TiebreakMode.MODE_UNKNOWN ? null : config.getTiebreakMode());
    setOvervoteRuleRadioButton(config.getOvervoteRule());

    ContestRules rules = rawConfig.rules;
    textFieldRandomSeed.setText(rules.randomSeed);
    textFieldNumberOfWinners.setText(rules.numberOfWinners);
    textFieldMultiSeatBottomsUpPercentageThreshold
        .setText(rules.multiSeatBottomsUpPercentageThreshold);
    textFieldDecimalPlacesForVoteArithmetic.setText(rules.decimalPlacesForVoteArithmetic);
    textFieldMinimumVoteThreshold.setText(rules.minimumVoteThreshold);
    if (rules.maxSkippedRanksAllowed
        .equalsIgnoreCase(ContestConfig.MAX_SKIPPED_RANKS_ALLOWED_UNLIMITED_OPTION)) {
      checkBoxMaxSkippedRanksAllowedUnlimited.setSelected(true);
      textFieldMaxSkippedRanksAllowed.clear();
      textFieldMaxSkippedRanksAllowed.setDisable(true);
    } else {
      checkBoxMaxSkippedRanksAllowedUnlimited.setSelected(false);
      textFieldMaxSkippedRanksAllowed.setText(rules.maxSkippedRanksAllowed);
      textFieldMaxSkippedRanksAllowed.setDisable(false);
    }
    if (rules.maxRankingsAllowed
        .equalsIgnoreCase(ContestConfig.MAX_RANKINGS_ALLOWED_NUM_CANDIDATES_OPTION)) {
      checkBoxMaxRankingsAllowedMax.setSelected(true);
      checkBoxMaxRankingsAllowedMax.setDisable(false);
      textFieldMaxRankingsAllowed.clear();
      textFieldMaxRankingsAllowed.setDisable(true);
    } else {
      checkBoxMaxRankingsAllowedMax.setSelected(false);
      textFieldMaxRankingsAllowed.setText(rules.maxRankingsAllowed);
      textFieldMaxRankingsAllowed.setDisable(false);
    }
    textFieldRulesDescription.setText(rules.rulesDescription);
    setThresholdCalculationMethodRadioButton(rules.nonIntegerWinningThreshold, rules.hareQuota);
    checkBoxBatchElimination.setSelected(rules.batchElimination);
    checkBoxContinueUntilTwoCandidatesRemain.setSelected(rules.continueUntilTwoCandidatesRemain);
    checkBoxFirstRoundDeterminesThreshold.setSelected(rules.doesFirstRoundDetermineThreshold);
    checkBoxPreventOneCandidateInFinalRound.setSelected(rules.preventOneCandidateInFinalRound);
    textFieldStopTabulationEarlyAfterRound.setText(rules.stopTabulationEarlyAfterRound);
    checkBoxExhaustOnDuplicateCandidate.setSelected(rules.exhaustOnDuplicateCandidate);
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
      default -> throw new IllegalStateException("Unexpected value: " + overvoteRule);
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
      source.setOvervoteLabel(
          source.getOvervoteLabel() != null ? source.getOvervoteLabel().trim() : "");
      source.setUndervoteLabel(
          source.getUndervoteLabel() != null ? source.getUndervoteLabel().trim() : "");
      source.setUndeclaredWriteInLabel(
          source.getUndeclaredWriteInLabel() != null ? source.getUndeclaredWriteInLabel().trim()
              : "");
    }
    config.cvrFileSources = cvrSources;

    ArrayList<Candidate> candidates = new ArrayList<>(tableViewCandidates.getItems());
    for (Candidate candidate : candidates) {
      candidate.trimNameAndAllAliases();
    }
    config.candidates = candidates;

    ContestRules rules = new ContestRules();
    rules.tiebreakMode = getTiebreakModeChoice(choiceTiebreakMode).getInternalLabel();
    rules.overvoteRule = getOvervoteRuleChoice();
    rules.winnerElectionMode = getWinnerElectionModeChoice(choiceWinnerElectionMode)
        .getInternalLabel();
    rules.randomSeed = getTextOrEmptyString(textFieldRandomSeed);
    rules.numberOfWinners = getTextOrEmptyString(textFieldNumberOfWinners);
    rules.multiSeatBottomsUpPercentageThreshold = getTextOrEmptyString(
        (textFieldMultiSeatBottomsUpPercentageThreshold));
    rules.decimalPlacesForVoteArithmetic =
        getTextOrEmptyString(textFieldDecimalPlacesForVoteArithmetic);
    rules.minimumVoteThreshold = getTextOrEmptyString(textFieldMinimumVoteThreshold);
    rules.maxSkippedRanksAllowed = checkBoxMaxSkippedRanksAllowedUnlimited.isSelected()
        ? ContestConfig.MAX_SKIPPED_RANKS_ALLOWED_UNLIMITED_OPTION
        : getTextOrEmptyString(textFieldMaxSkippedRanksAllowed);
    rules.maxRankingsAllowed = checkBoxMaxRankingsAllowedMax.isSelected()
        ? ContestConfig.MAX_RANKINGS_ALLOWED_NUM_CANDIDATES_OPTION
        : getTextOrEmptyString(textFieldMaxRankingsAllowed);
    rules.nonIntegerWinningThreshold = radioThresholdHbQuota.isSelected();
    rules.hareQuota = radioThresholdHareQuota.isSelected();
    rules.batchElimination = checkBoxBatchElimination.isSelected();
    rules.continueUntilTwoCandidatesRemain = checkBoxContinueUntilTwoCandidatesRemain.isSelected();
    rules.doesFirstRoundDetermineThreshold = checkBoxFirstRoundDeterminesThreshold.isSelected();
    rules.preventOneCandidateInFinalRound = checkBoxPreventOneCandidateInFinalRound.isSelected();
    rules.stopTabulationEarlyAfterRound =
        getTextOrEmptyString(textFieldStopTabulationEarlyAfterRound);
    rules.exhaustOnDuplicateCandidate = checkBoxExhaustOnDuplicateCandidate.isSelected();
    rules.rulesDescription = getTextOrEmptyString(textFieldRulesDescription);
    config.rules = rules;

    return config;
  }

  private enum ConfigComparisonResult {
    SAME,
    DIFFERENT,
    DIFFERENT_BUT_VERSION_IS_TEST,
  }

  private static class AutoLoadCandidatesService extends Service<Void> {

    private final ContestConfig config;
    private final List<CvrSource> sources;
    private final TableView<Candidate> tableViewCandidates;

    AutoLoadCandidatesService(
        ContestConfig config, List<CvrSource> sources, TableView<Candidate> tableViewCandidates) {
      this.config = config;
      this.sources = sources;
      this.tableViewCandidates = tableViewCandidates;
    }

    @Override
    protected Task<Void> createTask() {
      Task<Void> task =
          new Task<>() {
            @Override
            protected Void call() {
              Logger.info("Auto-loading candidates from CVR files...");
              boolean cvrsSpecified = true;
              if (sources.isEmpty()) {
                Logger.warning("No CVR files specified!");
                cvrsSpecified = false;
              }
              if (cvrsSpecified) {
                // Gather unloaded names from each of the sources and place into the HashSet
                Set<String> unloadedNames = new HashSet<>();
                for (CvrSource source : sources) {
                  Provider provider = ContestConfig.getProvider(source);
                  try {
                    List<CastVoteRecord> castVoteRecords = new ArrayList<>();
                    BaseCvrReader reader = provider.constructReader(config, source);
                    reader.readCastVoteRecords(castVoteRecords);
                    unloadedNames.addAll(reader.gatherUnknownCandidates(castVoteRecords).keySet());
                  } catch (ContestConfig.UnrecognizedProviderException e) {
                    Logger.severe(
                        "Unrecognized provider \"%s\" in source file \"%s\": %s",
                        source.getProvider(), source.getFilePath(), e.getMessage());
                  } catch (CastVoteRecord.CvrParseException | IOException e) {
                    Logger.severe(
                        "Failed to read source file \"%s\": ",
                        source.getFilePath(), e.getMessage());
                  }
                }

                // Validate each name and add to the table of candidates
                int successCount = 0;
                for (String name : unloadedNames) {
                  Candidate candidate = new Candidate(name, null, false);
                  Set<ValidationError> validationErrors =
                      ContestConfig.performBasicCandidateValidation(candidate);
                  if (validationErrors.isEmpty()) {
                    tableViewCandidates.getItems().add(candidate);
                    successCount++;
                  } else {
                    Logger.severe("Failed to load candidate \"%s\"!", name);
                  }
                }

                Logger.info("Auto-loaded %d candidates.", successCount);
              }
              return null;
            }
          };
      task.setOnFailed(
          arg0 -> Logger.severe(
              "Error when trying to auto-load candidates:\n%s\nAuto-load failed!",
              task.getException()));
      return task;
    }
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
              Logger.severe(
                  "Error during validation:\n%s\nValidation failed!",
                  task.getException()));
      return task;
    }
  }

  private abstract static class ConfigReaderService extends Service<Void> {
    protected String configPath;

    private final boolean deleteConfigOnCompletion;

    ConfigReaderService(String configPath, boolean deleteConfigOnCompletion) {
      this.configPath = configPath;
      this.deleteConfigOnCompletion = deleteConfigOnCompletion;
    }

    protected void cleanUp() {
      if (deleteConfigOnCompletion) {
        boolean succeeded = new File(configPath).delete();
        if (!succeeded) {
          Logger.warning("Failed to delete temporary config file: %s", configPath);
        }
      }
    }

    protected void setUpTaskCompletionTriggers(Task<Void> task, String failureMessage) {
      task.setOnFailed(
          arg0 -> {
            Logger.severe(failureMessage, task.getException());
            cleanUp();
          });
      task.setOnCancelled(arg0 -> cleanUp());
      task.setOnSucceeded(arg0 -> cleanUp());
    }
  }

  // TabulatorService runs a tabulation in the background
  private static class TabulatorService extends ConfigReaderService {
    private String operatorName;

    TabulatorService(String configPath, String operatorName, boolean deleteConfigOnCompletion) {
      super(configPath, deleteConfigOnCompletion);
      this.operatorName = operatorName;
    }

    @Override
    protected Task<Void> createTask() {
      Task<Void> task =
          new Task<>() {
            @Override
            protected Void call() {
              TabulatorSession session = new TabulatorSession(configPath);
              session.tabulate(operatorName);
              return null;
            }
          };

      setUpTaskCompletionTriggers(task, "Error during tabulation:\n%s\nTabulation failed!");
      return task;
    }
  }

  // ConvertToCdfService runs a CDF conversion in the background
  private static class ConvertToCdfService extends ConfigReaderService {
    ConvertToCdfService(String configPath, boolean deleteConfigOnCompletion) {
      super(configPath, deleteConfigOnCompletion);
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
      setUpTaskCompletionTriggers(task,
          "Error when attempting to convert to CDF:\n%s\nConversion failed!");
      return task;
    }
  }

  private abstract static class EditableColumn {
    protected final TableColumn column;
    protected final String propertyName;

    public EditableColumn(TableColumn column, String propertyName) {
      this.column = column;
      this.propertyName = propertyName;
    }

    public TableColumn getColumn() {
      return column;
    }

    public String getPropertyName() {
      return propertyName;
    }

    public void setCellFactoryValue() {
      column.setCellValueFactory(new PropertyValueFactory<>(propertyName));
    }

    public abstract void setCellFactory();

    public abstract Class propertyType();
  }

  private static class EditableColumnString extends EditableColumn {
    public EditableColumnString(TableColumn column, String propertyName) {
      super(column, propertyName);
    }

    @Override
    public Class propertyType() {
      return String.class;
    }

    @Override
    public void setCellFactory() {
      column.setCellFactory(EditableTableCellInline.forTableColumn());
    }
  }

  private static class EditableColumnBoolean extends EditableColumn {
    public EditableColumnBoolean(TableColumn column, String propertyName) {
      super(column, propertyName);
    }

    @Override
    public Class propertyType() {
      return Boolean.class;
    }

    @Override
    public void setCellFactory() {
      column.setCellFactory(CheckBoxTableCell.forTableColumn(column));
    }
  }

  private static class EditableColumnList extends EditableColumn {
    public EditableColumnList(TableColumn column, String propertyName) {
      super(column, propertyName);
    }

    @Override
    public Class propertyType() {
      return List.class;
    }

    @Override
    public void setCellFactory() {
      column.setCellFactory(tc -> new EditableTableCellPopup<Candidate>());
    }
  }
}
