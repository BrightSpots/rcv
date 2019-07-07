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

package network.brightspots.rcv;

import static network.brightspots.rcv.Utils.isNullOrBlank;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
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
import javafx.util.converter.IntegerStringConverter;
import network.brightspots.rcv.RawContestConfig.CVRSource;
import network.brightspots.rcv.RawContestConfig.Candidate;
import network.brightspots.rcv.RawContestConfig.ContestRules;
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
  private TableView<CVRSource> tableViewCvrFiles;
  @FXML
  private TableColumn<CVRSource, String> tableColumnCvrFilePath;
  @FXML
  private TableColumn<CVRSource, Integer> tableColumnCvrFirstVoteCol;
  @FXML
  private TableColumn<CVRSource, Integer> tableColumnCvrFirstVoteRow;
  @FXML
  private TableColumn<CVRSource, String> tableColumnCvrIdCol;
  @FXML
  private TableColumn<CVRSource, String> tableColumnCvrPrecinctCol;
  @FXML
  private TableColumn<CVRSource, String> tableColumnCvrProvider;
  @FXML
  private TextField textFieldCvrFilePath;
  @FXML
  private TextField textFieldCvrFirstVoteCol;
  @FXML
  private TextField textFieldCvrFirstVoteRow;
  @FXML
  private TextField textFieldCvrIdCol;
  @FXML
  private TextField textFieldCvrPrecinctCol;
  @FXML
  private TextField textFieldCvrProvider;
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
  private ChoiceBox<TieBreakMode> choiceTiebreakMode;
  @FXML
  private ChoiceBox<OvervoteRule> choiceOvervoteRule;
  @FXML
  private ChoiceBox<WinnerElectionMode> choiceWinnerElectionMode;
  @FXML
  private TextField textFieldRandomSeed;
  @FXML
  private TextField textFieldNumberOfWinners;
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
  private ButtonBar buttonBar;
  @FXML
  private TabPane tabPane;

  public void buttonNewConfigClicked() {
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

  public void buttonLoadConfigClicked() {
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

  public void buttonSaveClicked() {
    File fileToSave = getSaveFile();
    if (fileToSave != null) {
      saveFile(fileToSave);
    }
  }

  private void setGuiIsBusy(boolean isBusy) {
    guiIsBusy = isBusy;
    buttonBar.setDisable(isBusy);
    tabPane.setDisable(isBusy);
  }

  // validate whatever is currently entered into the GUI - does not save data
  public void buttonValidateClicked() {
    setGuiIsBusy(true);
    ContestConfig config =
        ContestConfig.loadContestConfig(createRawContestConfig(), FileUtils.getUserDirectory());
    ValidatorService service = new ValidatorService(config);
    service.setOnSucceeded(event -> setGuiIsBusy(false));
    service.setOnCancelled(event -> setGuiIsBusy(false));
    service.setOnFailed(event -> setGuiIsBusy(false));
    service.start();
  }

  // tabulate whatever is currently entered into the GUI:
  // - require user to save if there are un-saved changes
  // - create and launch TabulatorService from the saved config path
  public void buttonTabulateClicked() {
    if (checkForSaveAndTabulate()) {
      if (GuiContext.getInstance().getConfig() != null) {
        setGuiIsBusy(true);
        TabulatorService service = new TabulatorService(selectedFile.getAbsolutePath());
        service.setOnSucceeded(event -> setGuiIsBusy(false));
        service.setOnCancelled(event -> setGuiIsBusy(false));
        service.setOnFailed(event -> setGuiIsBusy(false));
        service.start();
      } else {
        Logger.log(
            Level.WARNING, "Please load a contest config file before attempting to tabulate!");
      }
    }
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

  public void buttonExitClicked() {
    exitGui();
  }

  public void buttonOutputDirectoryClicked() {
    DirectoryChooser dc = new DirectoryChooser();
    dc.setInitialDirectory(new File(FileUtils.getUserDirectory()));
    dc.setTitle("Output Directory");
    File outputDirectory = dc.showDialog(GuiContext.getInstance().getMainWindow());
    if (outputDirectory != null) {
      textFieldOutputDirectory.setText(outputDirectory.getAbsolutePath());
    }
  }

  public void buttonClearDatePickerContestDateClicked() {
    datePickerContestDate.setValue(null);
  }

  public void buttonCvrFilePathClicked() {
    FileChooser fc = new FileChooser();
    fc.setInitialDirectory(new File(FileUtils.getUserDirectory()));
    fc.getExtensionFilters()
        .add(new ExtensionFilter("Excel and JSON files", "*.xls", "*.xlsx", "*.json"));
    fc.setTitle("Select cast vote record file");

    File openFile = fc.showOpenDialog(GuiContext.getInstance().getMainWindow());
    if (openFile != null) {
      textFieldCvrFilePath.setText(openFile.getAbsolutePath());
    }
  }

  public void buttonAddCvrFileClicked() {
    CVRSource cvrSource = new CVRSource();
    String cvrFilePath = getTextOrEmptyString(textFieldCvrFilePath);
    String cvrFirstVoteCol = getTextOrEmptyString(textFieldCvrFirstVoteCol);
    String cvrFirstVoteRow = getTextOrEmptyString(textFieldCvrFirstVoteRow);
    boolean fileIsJson = cvrFilePath.toLowerCase().endsWith(".json");
    if (cvrFilePath.isBlank()) {
      Logger.log(Level.WARNING, "Cast vote record file path is required!");
    } else if (cvrFirstVoteCol.isBlank() && !fileIsJson) {
      Logger.log(Level.WARNING, "Cast vote record first vote column index is required!");
    } else if (cvrFirstVoteRow.isBlank() && !fileIsJson) {
      Logger.log(Level.WARNING, "Cast vote record first vote row index is required!");
    } else {
      cvrSource.setFilePath(cvrFilePath);
      cvrSource.setFirstVoteColumnIndex(getIntValueOrNull(cvrFirstVoteCol));
      cvrSource.setFirstVoteRowIndex(getIntValueOrNull(cvrFirstVoteRow));
      cvrSource.setIdColumnIndex(getTextOrEmptyString(textFieldCvrIdCol));
      cvrSource.setPrecinctColumnIndex(getTextOrEmptyString(textFieldCvrPrecinctCol));
      cvrSource.setProvider(getTextOrEmptyString(textFieldCvrProvider));
      tableViewCvrFiles.getItems().add(cvrSource);
      textFieldCvrFilePath.clear();
      textFieldCvrFirstVoteCol.clear();
      textFieldCvrFirstVoteRow.clear();
      textFieldCvrIdCol.clear();
      textFieldCvrPrecinctCol.clear();
      textFieldCvrProvider.clear();
    }
  }

  public void buttonDeleteCvrFileClicked() {
    tableViewCvrFiles
        .getItems()
        .removeAll(tableViewCvrFiles.getSelectionModel().getSelectedItems());
  }

  public void changeCvrFilePath(CellEditEvent cellEditEvent) {
    CVRSource cvrSelected = tableViewCvrFiles.getSelectionModel().getSelectedItem();
    String cvrFilePath = cellEditEvent.getNewValue().toString().trim();
    if (cvrFilePath.isBlank()) {
      Logger.log(Level.WARNING, "Cast vote record file path is required!");
    } else {
      cvrSelected.setFilePath(cvrFilePath);
    }
    tableViewCvrFiles.refresh();
  }

  public void changeCvrFirstVoteCol(CellEditEvent cellEditEvent) {
    CVRSource cvrSelected = tableViewCvrFiles.getSelectionModel().getSelectedItem();
    if (cellEditEvent.getNewValue() == null) {
      Logger.log(Level.WARNING, "Cast vote record first vote column is required!");
    } else {
      cvrSelected.setFirstVoteColumnIndex((Integer) cellEditEvent.getNewValue());
    }
    tableViewCvrFiles.refresh();
  }

  public void changeCvrFirstVoteRow(CellEditEvent cellEditEvent) {
    CVRSource cvrSelected = tableViewCvrFiles.getSelectionModel().getSelectedItem();
    if (cellEditEvent.getNewValue() == null) {
      Logger.log(Level.WARNING, "Cast vote record first vote row index is required!");
    } else {
      cvrSelected.setFirstVoteRowIndex((Integer) cellEditEvent.getNewValue());
    }
    tableViewCvrFiles.refresh();
  }

  public void changeCvrIdColIndex(CellEditEvent cellEditEvent) {
    tableViewCvrFiles.getSelectionModel().getSelectedItem()
        .setIdColumnIndex(cellEditEvent.getNewValue().toString().trim());
    tableViewCvrFiles.refresh();
  }

  public void changeCvrPrecinctColIndex(CellEditEvent cellEditEvent) {
    tableViewCvrFiles.getSelectionModel().getSelectedItem()
        .setPrecinctColumnIndex(cellEditEvent.getNewValue().toString().trim());
    tableViewCvrFiles.refresh();
  }

  public void changeCvrProvider(CellEditEvent cellEditEvent) {
    tableViewCvrFiles.getSelectionModel().getSelectedItem()
        .setProvider(cellEditEvent.getNewValue().toString().trim());
    tableViewCvrFiles.refresh();
  }

  public void buttonAddCandidateClicked() {
    Candidate candidate = new Candidate();
    String candidateName = getTextOrEmptyString(textFieldCandidateName);
    if (candidateName.isBlank()) {
      Logger.log(Level.WARNING, "Candidate name is required!");
    } else {
      candidate.setName(candidateName);
      candidate.setCode(getTextOrEmptyString(textFieldCandidateCode));
      candidate.setExcluded(ContestConfig.SUGGESTED_CANDIDATE_EXCLUDED);
      tableViewCandidates.getItems().add(candidate);
      textFieldCandidateName.clear();
      textFieldCandidateCode.clear();
    }
  }

  public void buttonDeleteCandidateClicked() {
    tableViewCandidates
        .getItems()
        .removeAll(tableViewCandidates.getSelectionModel().getSelectedItems());
  }

  public void changeCandidateName(CellEditEvent cellEditEvent) {
    String candidateName = cellEditEvent.getNewValue().toString().trim();
    if (candidateName.isBlank()) {
      Logger.log(Level.WARNING, "Candidate name is required!");
    } else {
      tableViewCandidates.getSelectionModel().getSelectedItem().setName(candidateName);
    }
    tableViewCandidates.refresh();
  }

  public void changeCandidateCode(CellEditEvent cellEditEvent) {
    tableViewCandidates.getSelectionModel().getSelectedItem()
        .setCode(cellEditEvent.getNewValue().toString().trim());
    tableViewCandidates.refresh();
  }

  private static Integer getIntValueOrNull(String str) {
    Integer returnValue = null;
    try {
      if (!isNullOrBlank(str)) {
        returnValue = Integer.valueOf(str);
      }
    } catch (Exception exception) {
      Logger.log(Level.WARNING, "Integer required! Illegal value \"%s\" found.", str);
    }
    return returnValue;
  }

  private static void setTextFieldToInteger(TextField textField, Integer value) {
    textField.setText(value != null ? Integer.toString(value) : "");
  }

  private void setDefaultValues() {
    labelCurrentlyLoaded.setText("Currently loaded: <New Config>");

    checkBoxTabulateByPrecinct.setSelected(ContestConfig.SUGGESTED_TABULATE_BY_PRECINCT);
    checkBoxGenerateCdfJson.setSelected(ContestConfig.SUGGESTED_GENERATE_CDF_JSON);
    checkBoxNonIntegerWinningThreshold.setSelected(
        ContestConfig.SUGGESTED_NON_INTEGER_WINNING_THRESHOLD);
    checkBoxHareQuota.setSelected(ContestConfig.SUGGESTED_HARE_QUOTA);
    checkBoxBatchElimination.setSelected(ContestConfig.SUGGESTED_BATCH_ELIMINATION);
    checkBoxExhaustOnDuplicateCandidate.setSelected(
        ContestConfig.SUGGESTED_EXHAUST_ON_DUPLICATE_CANDIDATES);
    checkBoxTreatBlankAsUndeclaredWriteIn.setSelected(
        ContestConfig.SUGGESTED_TREAT_BLANK_AS_UNDECLARED_WRITE_IN);

    textFieldNumberOfWinners.setText(String.valueOf(ContestConfig.SUGGESTED_NUMBER_OF_WINNERS));
    textFieldDecimalPlacesForVoteArithmetic.setText(
        String.valueOf(ContestConfig.SUGGESTED_DECIMAL_PLACES_FOR_VOTE_ARITHMETIC));
    textFieldMaxSkippedRanksAllowed.setText(
        String.valueOf(ContestConfig.SUGGESTED_MAX_SKIPPED_RANKS_ALLOWED));
    textFieldMinimumVoteThreshold.setText(
        String.valueOf(ContestConfig.SUGGESTED_MINIMUM_VOTE_THRESHOLD));
    textFieldMaxRankingsAllowed.setText(ContestConfig.SUGGESTED_MAX_RANKINGS_ALLOWED);
  }

  private void clearConfig() {
    textFieldContestName.clear();
    textFieldOutputDirectory.clear();
    datePickerContestDate.setValue(null);
    textFieldContestJurisdiction.clear();
    textFieldContestOffice.clear();
    checkBoxTabulateByPrecinct.setSelected(false);
    checkBoxGenerateCdfJson.setSelected(false);

    textFieldCvrFilePath.clear();
    textFieldCvrFirstVoteCol.clear();
    textFieldCvrFirstVoteRow.clear();
    textFieldCvrIdCol.clear();
    textFieldCvrPrecinctCol.clear();
    textFieldCvrProvider.clear();
    tableViewCvrFiles.getItems().clear();

    textFieldCandidateName.clear();
    textFieldCandidateCode.clear();
    tableViewCandidates.getItems().clear();

    choiceTiebreakMode.setValue(null);
    choiceOvervoteRule.setValue(null);
    choiceWinnerElectionMode.setValue(null);
    textFieldRandomSeed.clear();
    textFieldNumberOfWinners.clear();
    textFieldDecimalPlacesForVoteArithmetic.clear();
    textFieldMinimumVoteThreshold.clear();
    textFieldMaxSkippedRanksAllowed.clear();
    textFieldMaxRankingsAllowed.clear();
    textFieldOvervoteLabel.clear();
    textFieldUndervoteLabel.clear();
    textFieldUndeclaredWriteInLabel.clear();
    textFieldRulesDescription.clear();
    checkBoxNonIntegerWinningThreshold.setSelected(false);
    checkBoxHareQuota.setSelected(false);
    checkBoxBatchElimination.setSelected(false);
    checkBoxExhaustOnDuplicateCandidate.setSelected(false);
    checkBoxTreatBlankAsUndeclaredWriteIn.setSelected(false);

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
          "Unable tell if saving is necessary, but everything should work fine anyway! Prompting for save just in case...\n%s",
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

  private boolean checkForSaveAndTabulate() {
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

  private static Integer getIntValueOrNull(TextField textField) {
    return getIntValueOrNull(textField.getText().trim());
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
            return !isNullOrBlank(string) ? LocalDate.parse(string, DATE_TIME_FORMATTER) : null;
          }
        });

    textFieldCvrFirstVoteCol
        .textProperty()
        .addListener(new TextFieldListenerNonNegInt(textFieldCvrFirstVoteCol));
    textFieldCvrFirstVoteRow
        .textProperty()
        .addListener(new TextFieldListenerNonNegInt(textFieldCvrFirstVoteRow));
    tableColumnCvrFilePath.setCellValueFactory(new PropertyValueFactory<>("filePath"));
    tableColumnCvrFilePath.setCellFactory(TextFieldTableCell.forTableColumn());
    tableColumnCvrFirstVoteCol.setCellValueFactory(
        new PropertyValueFactory<>("firstVoteColumnIndex"));
    tableColumnCvrFirstVoteCol.setCellFactory(
        TextFieldTableCell.forTableColumn(new SimpleIntegerStringConverter()));
    tableColumnCvrFirstVoteRow.setCellValueFactory(new PropertyValueFactory<>("firstVoteRowIndex"));
    tableColumnCvrFirstVoteRow.setCellFactory(
        TextFieldTableCell.forTableColumn(new SimpleIntegerStringConverter()));
    tableColumnCvrIdCol.setCellValueFactory(new PropertyValueFactory<>("idColumnIndex"));
    tableColumnCvrIdCol.setCellFactory(TextFieldTableCell.forTableColumn());
    tableColumnCvrPrecinctCol.setCellValueFactory(
        new PropertyValueFactory<>("precinctColumnIndex"));
    tableColumnCvrPrecinctCol.setCellFactory(TextFieldTableCell.forTableColumn());
    tableColumnCvrProvider.setCellValueFactory(new PropertyValueFactory<>("provider"));
    tableColumnCvrProvider.setCellFactory(TextFieldTableCell.forTableColumn());
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
              .addListener((ov, old_val, new_val) -> candidate.setExcluded(new_val));
          //noinspection unchecked
          return new SimpleObjectProperty(checkBox);
        });
    tableViewCandidates.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    tableViewCandidates.setEditable(true);

    choiceTiebreakMode.getItems().addAll(TieBreakMode.values());
    choiceTiebreakMode.getItems().remove(TieBreakMode.MODE_UNKNOWN);
    choiceOvervoteRule.getItems().addAll(OvervoteRule.values());
    choiceOvervoteRule.getItems().remove(OvervoteRule.RULE_UNKNOWN);
    choiceWinnerElectionMode.getItems().addAll(WinnerElectionMode.values());
    choiceWinnerElectionMode.getItems().remove(WinnerElectionMode.MODE_UNKNOWN);

    textFieldRandomSeed
        .textProperty()
        .addListener(new TextFieldListenerNonNegInt(textFieldRandomSeed));
    textFieldNumberOfWinners
        .textProperty()
        .addListener(new TextFieldListenerNonNegInt(textFieldNumberOfWinners));
    textFieldDecimalPlacesForVoteArithmetic
        .textProperty()
        .addListener(new TextFieldListenerNonNegInt(textFieldDecimalPlacesForVoteArithmetic));
    textFieldMinimumVoteThreshold
        .textProperty()
        .addListener(new TextFieldListenerNonNegInt(textFieldMinimumVoteThreshold));

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

  // version migration logic goes here
  private void migrateConfigVersion(ContestConfig config) {
    if (config.rawConfig.tabulatorVersion == null || !config.rawConfig.tabulatorVersion
        .equals(Main.APP_VERSION)) {
      config.rawConfig.tabulatorVersion = Main.APP_VERSION;
      Logger.log(Level.INFO, "Migrated tabulator version from %s to %s.",
          config.rawConfig.tabulatorVersion, Main.APP_VERSION);
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
      datePickerContestDate.setValue(
          LocalDate.parse(outputSettings.contestDate, DATE_TIME_FORMATTER));
    }
    textFieldContestJurisdiction.setText(outputSettings.contestJurisdiction);
    textFieldContestOffice.setText(outputSettings.contestOffice);
    checkBoxTabulateByPrecinct.setSelected(outputSettings.tabulateByPrecinct);
    checkBoxGenerateCdfJson.setSelected(outputSettings.generateCdfJson);

    if (rawConfig.cvrFileSources != null) {
      tableViewCvrFiles.setItems(FXCollections.observableArrayList(rawConfig.cvrFileSources));
    }

    if (rawConfig.candidates != null) {
      tableViewCandidates.setItems(FXCollections.observableArrayList(rawConfig.candidates));
    }

    ContestRules rules = rawConfig.rules;
    choiceTiebreakMode.setValue(config.getTiebreakMode());
    choiceOvervoteRule.setValue(config.getOvervoteRule());
    choiceWinnerElectionMode.setValue(config.getWinnerElectionMode());
    setTextFieldToInteger(textFieldRandomSeed, rules.randomSeed);
    setTextFieldToInteger(textFieldNumberOfWinners, rules.numberOfWinners);
    setTextFieldToInteger(
        textFieldDecimalPlacesForVoteArithmetic, rules.decimalPlacesForVoteArithmetic);
    setTextFieldToInteger(textFieldMinimumVoteThreshold, rules.minimumVoteThreshold);
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

  private static String getChoiceElse(ChoiceBox choiceBox, Enum defaultValue) {
    return choiceBox.getValue() != null ? choiceBox.getValue().toString() : defaultValue.toString();
  }

  private static String getTextOrEmptyString(TextField textField) {
    return textField.getText() != null ? textField.getText().trim() : "";
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

    ArrayList<CVRSource> cvrSources = new ArrayList<>(tableViewCvrFiles.getItems());
    for (CVRSource source : cvrSources) {
      source.setFilePath(source.getFilePath() != null ? source.getFilePath().trim() : "");
      source.setIdColumnIndex(
          source.getIdColumnIndex() != null ? source.getIdColumnIndex().trim() : "");
      source.setPrecinctColumnIndex(
          source.getPrecinctColumnIndex() != null ? source.getPrecinctColumnIndex().trim() : "");
      source.setProvider(source.getProvider() != null ? source.getProvider().trim() : "");
    }
    config.cvrFileSources = cvrSources;

    ArrayList<Candidate> candidates = new ArrayList<>(tableViewCandidates.getItems());
    for (Candidate candidate : candidates) {
      candidate.setName(candidate.getName() != null ? candidate.getName().trim() : "");
      candidate.setCode(candidate.getCode() != null ? candidate.getCode().trim() : "");
    }
    config.candidates = candidates;

    ContestRules rules = new ContestRules();
    rules.tiebreakMode = getChoiceElse(choiceTiebreakMode, TieBreakMode.MODE_UNKNOWN);
    rules.overvoteRule = getChoiceElse(choiceOvervoteRule, OvervoteRule.RULE_UNKNOWN);
    rules.winnerElectionMode = getChoiceElse(choiceWinnerElectionMode,
        WinnerElectionMode.MODE_UNKNOWN);
    rules.randomSeed = getIntValueOrNull(textFieldRandomSeed);
    rules.numberOfWinners = getIntValueOrNull(textFieldNumberOfWinners);
    rules.decimalPlacesForVoteArithmetic =
        getIntValueOrNull(textFieldDecimalPlacesForVoteArithmetic);
    rules.minimumVoteThreshold = getIntValueOrNull(textFieldMinimumVoteThreshold);
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

  private static class TabulatorService extends Service<Void> {

    // path to config file we will use for tabulation
    private final String configPath;

    // function: TabulatorService
    // purpose: constructor for Service object which runs a tabulation
    // param: configPath path to config file to be tabulated
    TabulatorService(String configPath) {
      this.configPath = configPath;
    }

    @Override
    protected Task<Void> createTask() {
      Task<Void> task =
          new Task<>() {
            @Override
            protected Void call() {
              // create session object used for tabulation
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

  private class TextFieldListenerNonNegInt implements ChangeListener<String> {
    // Restricts text fields to non-negative integers

    private final TextField textField;

    TextFieldListenerNonNegInt(TextField textField) {
      this.textField = textField;
    }

    @Override
    public void changed(
        ObservableValue<? extends String> observable, String oldValue, String newValue) {
      if (!newValue.matches("\\d*")) {
        textField.setText(oldValue);
      }
    }
  }

  private class SimpleIntegerStringConverter extends IntegerStringConverter {

    @Override
    public Integer fromString(String value) {
      return getIntValueOrNull(value);
    }
  }
}
