/*
 * Ranked Choice Voting Universal Tabulator
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
import network.brightspots.rcv.Tabulator.TabulationCancelledException;


@SuppressWarnings("WeakerAccess")
public class GuiConfigController implements Initializable {

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final String CONFIG_FILE_DOCUMENTATION_FILENAME = "network/brightspots/rcv/config_file_documentation.txt";

  // Used to check if changes have been made to a new config
  private String emptyConfigString;
  // File previously loaded or saved
  private File selectedFile;

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
  private TableColumn<CVRSource, Integer> tableColumnCvrIdCol;
  @FXML
  private TableColumn<CVRSource, Integer> tableColumnCvrPrecinctCol;
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
  private ChoiceBox<Tabulator.TieBreakMode> choiceTiebreakMode;
  @FXML
  private ChoiceBox<Tabulator.OvervoteRule> choiceOvervoteRule;
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
  private CheckBox checkBoxSequentialMultiSeat;
  @FXML
  private CheckBox checkBoxBottomsUpMultiSeat;
  @FXML
  private CheckBox checkBoxNonIntegerWinningThreshold;
  @FXML
  private CheckBox checkBoxHareQuota;
  @FXML
  private CheckBox checkBoxBatchElimination;
  @FXML
  private CheckBox checkBoxContinueUntilTwoCandidatesRemain;
  @FXML
  private CheckBox checkBoxExhaustOnDuplicateCandidate;
  @FXML
  private CheckBox checkBoxTreatBlankAsUndeclaredWriteIn;
  @FXML
  private ButtonBar buttonBar;

  public void buttonNewConfigClicked() {
    if (checkForSaveAndContinue()) {
      Logger.log(Level.INFO, "Creating new contest config...");
      GuiContext.getInstance().setConfig(null);
      selectedFile = null;
      clearConfig();
    }
  }

  private void loadFile(File fileToLoad) {
    // set the user dir for future loads
    FileUtils.setUserDirectory(fileToLoad.getParent());
    // load and cache the config object
    GuiContext.getInstance()
        .setConfig(ContestConfig.loadContestConfig(fileToLoad.getAbsolutePath()));
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
        loadFile(fileToLoad);
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
    loadFile(fileToSave);
  }

  public void buttonSaveClicked() {
    File fileToSave = getSaveFile();
    if (fileToSave != null) {
      saveFile(fileToSave);
    }
  }

  // validate whatever is currently entered into the GUI - does not save data
  public void buttonValidateClicked() {
    buttonBar.setDisable(true);
    ContestConfig config =
        ContestConfig.loadContestConfig(createRawContestConfig(), FileUtils.getUserDirectory());
    ValidatorService service = new ValidatorService(config);
    service.setOnSucceeded(event -> buttonBar.setDisable(false));
    service.setOnCancelled(event -> buttonBar.setDisable(false));
    service.setOnFailed(event -> buttonBar.setDisable(false));
    service.start();
  }

  // tabulate whatever is currently entered into the GUI:
  // - require user to save if there are un-saved changes
  // - create and launch TabulatorService from the saved config path
  public void buttonTabulateClicked() {
    if (checkForSaveAndTabulate()) {
      if (GuiContext.getInstance().getConfig() != null) {
        buttonBar.setDisable(true);
        TabulatorService service = new TabulatorService(selectedFile.getAbsolutePath());
        service.setOnSucceeded(event -> buttonBar.setDisable(false));
        service.setOnCancelled(event -> buttonBar.setDisable(false));
        service.setOnFailed(event -> buttonBar.setDisable(false));
        service.start();
      } else {
        Logger.log(
            Level.WARNING, "Please load a contest config file before attempting to tabulate!");
      }
    }
  }

  public void buttonExitClicked() {
    if (checkForSaveAndContinue()) {
      Logger.log(Level.INFO, "Exiting tabulator GUI...");
      Platform.exit();
    }
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
    fc.setTitle("Select CVR File");

    File openFile = fc.showOpenDialog(GuiContext.getInstance().getMainWindow());
    if (openFile != null) {
      textFieldCvrFilePath.setText(openFile.getAbsolutePath());
    }
  }

  public void buttonAddCvrFileClicked() {
    CVRSource cvrSource = new CVRSource();
    String cvrFilePath = textFieldCvrFilePath.getText().trim();
    String cvrFirstVoteCol = textFieldCvrFirstVoteCol.getText().trim();
    String cvrFirstVoteRow = textFieldCvrFirstVoteRow.getText().trim();
    boolean fileIsJson = cvrFilePath.toLowerCase().endsWith(".json");
    if (cvrFilePath.isEmpty()) {
      Logger.log(Level.WARNING, "CVR file path is required!");
    } else if (cvrFirstVoteCol.isEmpty() && !fileIsJson) {
      Logger.log(Level.WARNING, "CVR first vote column index is required!");
    } else if (cvrFirstVoteRow.isEmpty() && !fileIsJson) {
      Logger.log(Level.WARNING, "CVR first vote row index is required!");
    } else {
      cvrSource.setFilePath(cvrFilePath);
      cvrSource.setFirstVoteColumnIndex(getIntValueOrNull(cvrFirstVoteCol));
      cvrSource.setFirstVoteRowIndex(getIntValueOrNull(cvrFirstVoteRow));
      cvrSource.setIdColumnIndex(getIntValueOrNull(textFieldCvrIdCol));
      cvrSource.setPrecinctColumnIndex(getIntValueOrNull(textFieldCvrPrecinctCol));
      cvrSource.setProvider(textFieldCvrProvider.getText().trim());
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
    if (cvrFilePath.isEmpty()) {
      Logger.log(Level.WARNING, "CVR file path is required!");
    } else {
      cvrSelected.setFilePath(cvrFilePath);
    }
    tableViewCvrFiles.refresh();
  }

  public void changeCvrFirstVoteCol(CellEditEvent cellEditEvent) {
    CVRSource cvrSelected = tableViewCvrFiles.getSelectionModel().getSelectedItem();
    if (cellEditEvent.getNewValue() == null) {
      Logger.log(Level.WARNING, "CVR first vote column is required!");
    } else {
      cvrSelected.setFirstVoteColumnIndex((Integer) cellEditEvent.getNewValue());
    }
    tableViewCvrFiles.refresh();
  }

  public void changeCvrFirstVoteRow(CellEditEvent cellEditEvent) {
    CVRSource cvrSelected = tableViewCvrFiles.getSelectionModel().getSelectedItem();
    if (cellEditEvent.getNewValue() == null) {
      Logger.log(Level.WARNING, "CVR first vote row index is required!");
    } else {
      cvrSelected.setFirstVoteRowIndex((Integer) cellEditEvent.getNewValue());
    }
    tableViewCvrFiles.refresh();
  }

  public void changeCvrIdColIndex(CellEditEvent cellEditEvent) {
    CVRSource cvrSelected = tableViewCvrFiles.getSelectionModel().getSelectedItem();
    // FIXME: If user enters bad data, value is nulled out instead of reverting to previous value
    cvrSelected.setIdColumnIndex(
        cellEditEvent.getNewValue() == null ? null : (Integer) cellEditEvent.getNewValue());
    tableViewCvrFiles.refresh();
  }

  public void changeCvrPrecinctCol(CellEditEvent cellEditEvent) {
    CVRSource cvrSelected = tableViewCvrFiles.getSelectionModel().getSelectedItem();
    // FIXME: If user enters bad data, value is nulled out instead of reverting to previous value
    cvrSelected.setPrecinctColumnIndex(
        cellEditEvent.getNewValue() == null ? null : (Integer) cellEditEvent.getNewValue());
    tableViewCvrFiles.refresh();
  }

  public void changeCvrProvider(CellEditEvent cellEditEvent) {
    CVRSource cvrSelected = tableViewCvrFiles.getSelectionModel().getSelectedItem();
    cvrSelected.setProvider(cellEditEvent.getNewValue().toString().trim());
    tableViewCvrFiles.refresh();
  }

  public void buttonAddCandidateClicked() {
    Candidate candidate = new Candidate();
    String candidateName = textFieldCandidateName.getText().trim();
    if (candidateName.isEmpty()) {
      Logger.log(Level.WARNING, "Candidate name is required!");
    } else {
      candidate.setName(candidateName);
      candidate.setCode(textFieldCandidateCode.getText().trim());
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
    Candidate candidateSelected = tableViewCandidates.getSelectionModel().getSelectedItem();
    String candidateName = cellEditEvent.getNewValue().toString().trim();
    if (candidateName.isEmpty()) {
      Logger.log(Level.WARNING, "Candidate name is required!");
    } else {
      candidateSelected.setName(candidateName);
    }
    tableViewCandidates.refresh();
  }

  public void changeCandidateCode(CellEditEvent cellEditEvent) {
    Candidate candidateSelected = tableViewCandidates.getSelectionModel().getSelectedItem();
    candidateSelected.setCode(cellEditEvent.getNewValue().toString().trim());
    tableViewCandidates.refresh();
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    Logger.addGuiLogging(this.textAreaStatus);
    Logger.log(Level.INFO, String.format("Opening tabulator GUI...\n"
        + "Welcome to the %s!", Main.APP_NAME));

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
            return string != null && !string.isEmpty()
                ? LocalDate.parse(string, DATE_TIME_FORMATTER)
                : null;
          }
        });

    textFieldCvrFirstVoteCol
        .textProperty()
        .addListener(new TextFieldListenerNonNegInt(textFieldCvrFirstVoteCol));
    textFieldCvrFirstVoteRow
        .textProperty()
        .addListener(new TextFieldListenerNonNegInt(textFieldCvrFirstVoteRow));
    textFieldCvrIdCol.textProperty().addListener(new TextFieldListenerNonNegInt(textFieldCvrIdCol));
    textFieldCvrPrecinctCol
        .textProperty()
        .addListener(new TextFieldListenerNonNegInt(textFieldCvrPrecinctCol));
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
    tableColumnCvrIdCol.setCellFactory(
        TextFieldTableCell.forTableColumn(new SimpleIntegerStringConverter()));
    tableColumnCvrPrecinctCol.setCellValueFactory(
        new PropertyValueFactory<>("precinctColumnIndex"));
    tableColumnCvrPrecinctCol.setCellFactory(
        TextFieldTableCell.forTableColumn(new SimpleIntegerStringConverter()));
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

    choiceTiebreakMode.getItems().addAll(Tabulator.TieBreakMode.values());
    choiceTiebreakMode.getItems().remove(Tabulator.TieBreakMode.MODE_UNKNOWN);
    choiceOvervoteRule.getItems().addAll(Tabulator.OvervoteRule.values());
    choiceOvervoteRule.getItems().remove(Tabulator.OvervoteRule.RULE_UNKNOWN);

    textFieldNumberOfWinners
        .textProperty()
        .addListener(new TextFieldListenerNonNegInt(textFieldNumberOfWinners));
    textFieldDecimalPlacesForVoteArithmetic
        .textProperty()
        .addListener(new TextFieldListenerNonNegInt(textFieldDecimalPlacesForVoteArithmetic));
    textFieldMinimumVoteThreshold
        .textProperty()
        .addListener(new TextFieldListenerNonNegInt(textFieldMinimumVoteThreshold));
    textFieldMaxSkippedRanksAllowed
        .textProperty()
        .addListener(new TextFieldListenerNonNegInt(textFieldMaxSkippedRanksAllowed));
    textFieldMaxRankingsAllowed
        .textProperty()
        .addListener(new TextFieldListenerNonNegInt(textFieldMaxRankingsAllowed));

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

  private void setTextFieldToInteger(TextField textField, Integer value) {
    textField.setText(value != null ? Integer.toString(value) : "");
  }

  private void setDefaultValues() {
    labelCurrentlyLoaded.setText("Currently loaded: <New Config>");

    checkBoxTabulateByPrecinct.setSelected(ContestConfig.SUGGESTED_TABULATE_BY_PRECINCT);
    checkBoxGenerateCdfJson.setSelected(ContestConfig.SUGGESTED_GENERATE_CDF_JSON);
    checkBoxSequentialMultiSeat.setSelected(ContestConfig.SUGGESTED_SEQUENTIAL_MULTI_SEAT);
    checkBoxBottomsUpMultiSeat.setSelected(ContestConfig.SUGGESTED_BOTTOMS_UP_MULTI_SEAT);
    checkBoxNonIntegerWinningThreshold.setSelected(
        ContestConfig.SUGGESTED_NON_INTEGER_WINNING_THRESHOLD);
    checkBoxHareQuota.setSelected(ContestConfig.SUGGESTED_HARE_QUOTA);
    checkBoxBatchElimination.setSelected(ContestConfig.SUGGESTED_BATCH_ELIMINATION);
    checkBoxContinueUntilTwoCandidatesRemain.setSelected(
        ContestConfig.SUGGESTED_CONTINUE_UNTIL_TWO_CANDIDATES_REMAIN);
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
    textFieldNumberOfWinners.clear();
    textFieldDecimalPlacesForVoteArithmetic.clear();
    textFieldMinimumVoteThreshold.clear();
    textFieldMaxSkippedRanksAllowed.clear();
    textFieldMaxRankingsAllowed.clear();
    textFieldOvervoteLabel.clear();
    textFieldUndervoteLabel.clear();
    textFieldUndeclaredWriteInLabel.clear();
    textFieldRulesDescription.clear();
    checkBoxSequentialMultiSeat.setSelected(false);
    checkBoxBottomsUpMultiSeat.setSelected(false);
    checkBoxNonIntegerWinningThreshold.setSelected(false);
    checkBoxHareQuota.setSelected(false);
    checkBoxBatchElimination.setSelected(false);
    checkBoxContinueUntilTwoCandidatesRemain.setSelected(false);
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

  private void loadConfig(ContestConfig config) {
    clearConfig();
    RawContestConfig rawConfig = config.getRawConfig();

    OutputSettings outputSettings = rawConfig.outputSettings;
    textFieldContestName.setText(outputSettings.contestName);
    textFieldOutputDirectory.setText(config.getOutputDirectoryRaw());
    if (outputSettings.contestDate != null && !outputSettings.contestDate.isEmpty()) {
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
    setTextFieldToInteger(textFieldNumberOfWinners, rules.numberOfWinners);
    setTextFieldToInteger(
        textFieldDecimalPlacesForVoteArithmetic, rules.decimalPlacesForVoteArithmetic);
    setTextFieldToInteger(textFieldMinimumVoteThreshold, rules.minimumVoteThreshold);
    setTextFieldToInteger(textFieldMaxSkippedRanksAllowed, rules.maxSkippedRanksAllowed);
    setTextFieldToInteger(textFieldMaxRankingsAllowed, rules.maxRankingsAllowed);
    textFieldOvervoteLabel.setText(rules.overvoteLabel);
    textFieldUndervoteLabel.setText(rules.undervoteLabel);
    textFieldUndeclaredWriteInLabel.setText(rules.undeclaredWriteInLabel);
    textFieldRulesDescription.setText(rules.rulesDescription);
    checkBoxSequentialMultiSeat.setSelected(rules.sequentialMultiSeat);
    checkBoxBottomsUpMultiSeat.setSelected(rules.bottomsUpMultiSeat);
    checkBoxNonIntegerWinningThreshold.setSelected(rules.nonIntegerWinningThreshold);
    checkBoxHareQuota.setSelected(rules.hareQuota);
    checkBoxBatchElimination.setSelected(rules.batchElimination);
    checkBoxContinueUntilTwoCandidatesRemain.setSelected(rules.continueUntilTwoCandidatesRemain);
    checkBoxExhaustOnDuplicateCandidate.setSelected(rules.exhaustOnDuplicateCandidate);
    checkBoxTreatBlankAsUndeclaredWriteIn.setSelected(rules.treatBlankAsUndeclaredWriteIn);
  }

  private Integer getIntValueOrNull(TextField textField) {
    return getIntValueOrNull(textField.getText());
  }

  private Integer getIntValueOrNull(String str) {
    Integer returnValue = null;
    try {
      if (str != null) {
        str = str.trim();
        if (!str.isEmpty()) {
          returnValue = Integer.valueOf(str);
        }
      }
    } catch (Exception exception) {
      Logger.log(Level.WARNING, "Integer required! Illegal value \"%s\" found.", str);
    }
    return returnValue;
  }

  private String getChoiceElse(ChoiceBox choiceBox, Enum defaultValue) {
    return choiceBox.getValue() != null ? choiceBox.getValue().toString() : defaultValue.toString();
  }

  private RawContestConfig createRawContestConfig() {
    RawContestConfig config = new RawContestConfig();

    OutputSettings outputSettings = new OutputSettings();
    outputSettings.contestName = textFieldContestName.getText();
    outputSettings.outputDirectory = textFieldOutputDirectory.getText();
    outputSettings.contestDate =
        datePickerContestDate.getValue() != null ? datePickerContestDate.getValue().toString() : "";
    outputSettings.contestJurisdiction = textFieldContestJurisdiction.getText();
    outputSettings.contestOffice = textFieldContestOffice.getText();
    outputSettings.tabulateByPrecinct = checkBoxTabulateByPrecinct.isSelected();
    outputSettings.generateCdfJson = checkBoxGenerateCdfJson.isSelected();
    config.outputSettings = outputSettings;

    config.cvrFileSources = new ArrayList<>(tableViewCvrFiles.getItems());

    config.candidates = new ArrayList<>(tableViewCandidates.getItems());

    ContestRules rules = new ContestRules();
    rules.tiebreakMode = getChoiceElse(choiceTiebreakMode, Tabulator.TieBreakMode.MODE_UNKNOWN);
    rules.overvoteRule = getChoiceElse(choiceOvervoteRule, Tabulator.OvervoteRule.RULE_UNKNOWN);
    rules.numberOfWinners = getIntValueOrNull(textFieldNumberOfWinners);
    rules.decimalPlacesForVoteArithmetic =
        getIntValueOrNull(textFieldDecimalPlacesForVoteArithmetic);
    rules.minimumVoteThreshold = getIntValueOrNull(textFieldMinimumVoteThreshold);
    rules.maxSkippedRanksAllowed = getIntValueOrNull(textFieldMaxSkippedRanksAllowed);
    rules.maxRankingsAllowed = getIntValueOrNull(textFieldMaxRankingsAllowed);
    rules.sequentialMultiSeat = checkBoxSequentialMultiSeat.isSelected();
    rules.bottomsUpMultiSeat = checkBoxBottomsUpMultiSeat.isSelected();
    rules.nonIntegerWinningThreshold = checkBoxNonIntegerWinningThreshold.isSelected();
    rules.hareQuota = checkBoxHareQuota.isSelected();
    rules.batchElimination = checkBoxBatchElimination.isSelected();
    rules.continueUntilTwoCandidatesRemain = checkBoxContinueUntilTwoCandidatesRemain.isSelected();
    rules.exhaustOnDuplicateCandidate = checkBoxExhaustOnDuplicateCandidate.isSelected();
    rules.treatBlankAsUndeclaredWriteIn = checkBoxTreatBlankAsUndeclaredWriteIn.isSelected();
    rules.overvoteLabel = textFieldOvervoteLabel.getText();
    rules.undervoteLabel = textFieldUndervoteLabel.getText();
    rules.undeclaredWriteInLabel = textFieldUndeclaredWriteInLabel.getText();
    rules.rulesDescription = textFieldRulesDescription.getText();
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
      Task<Void> task = new Task<>() {
        @Override
        protected Void call() {
          contestConfig.validate();
          return null;
        }
      };
      task.setOnFailed(arg0 -> Logger.log(Level.SEVERE, "Error during validation:\n%s\n"
          + "Validation failed!", task.getException().toString()));
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
      Task<Void> task = new Task<>() {
        @Override
        protected Void call() {
          // create session object used for tabulation
          TabulatorSession session = new TabulatorSession(configPath);
          try {
            session.tabulate();
          } catch (TabulationCancelledException e) {
            Logger.log(Level.SEVERE, "Tabulation was cancelled!");
          }
          return null;
        }
      };
      task.setOnFailed(arg0 -> Logger.log(Level.SEVERE, "Error during tabulation:\n%s\n"
          + "Tabulation failed!", task.getException().toString()));
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
