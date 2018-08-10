/*
 * Ranked Choice Voting Universal Tabulator
 * Copyright (C) 2018 Jonathan Moldover, Louis Eisenberg, and Hylton Edingfield
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

package com.rcv;

import com.rcv.RawElectionConfig.Candidate;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class GuiConfigController implements Initializable {

  @FXML
  private TextArea textAreaHelp;
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
  private ToggleGroup toggleTabulateByPrecinct;
  @FXML
  private TableView<Candidate> tableViewCandidates;
  @FXML
  private TableColumn<Candidate, String> tableColumnCandidateName;
  @FXML
  private TableColumn<Candidate, String> tableColumnCandidateCode;
  @FXML
  private TextField textFieldCandidateName;
  @FXML
  private TextField textFieldCandidateCode;
  @FXML
  private ChoiceBox<Tabulator.TieBreakMode> choiceTiebreakMode;
  @FXML
  private ChoiceBox<Tabulator.OvervoteRule> choiceOvervoteRule;
  @FXML
  private ChoiceBox<Tabulator.MultiSeatTransferRule> choiceMultiSeatTransferRule;
  @FXML
  private TextField textFieldMaxRankingsAllowed;
  @FXML
  private TextField textFieldMaxSkippedRanksAllowed;
  @FXML
  private TextField textFieldNumberOfWinners;
  @FXML
  private TextField textFieldDecimalPlacesForVoteArithmetic;
  @FXML
  private TextField textFieldMinimumVoteThreshold;
  @FXML
  private ToggleGroup toggleBatchElimination;
  @FXML
  private ToggleGroup toggleContinueUntilTwoCandidatesRemain;
  @FXML
  private ToggleGroup toggleExhaustOnDuplicateCandidate;
  @FXML
  private ToggleGroup toggleTreatBlankAsUndeclaredWriteIn;

  public void buttonClearDatePickerContestDateClicked() {
    datePickerContestDate.getEditor().clear();
    datePickerContestDate.setValue(null);
  }

  public void buttonOutputDirectoryClicked() {
    DirectoryChooser dc = new DirectoryChooser();
    dc.setInitialDirectory(new File(System.getProperty("user.dir")));
    dc.setTitle("Output Directory");

    File outputDirectory = dc.showDialog(null);
    if (outputDirectory != null) {
      textFieldOutputDirectory.setText(outputDirectory.getAbsolutePath());
    }
  }

  public void buttonMenuClicked(ActionEvent event) throws IOException {
    Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
    Parent menuParent = FXMLLoader.load(getClass().getResource("/GuiMainLayout.fxml"));
    window.setScene(new Scene(menuParent));
  }

  public void buttonSaveClicked() {
    FileChooser fc = new FileChooser();
    fc.setInitialDirectory(new File(System.getProperty("user.dir")));
    fc.getExtensionFilters().add(new ExtensionFilter("JSON files", "*.json"));
    fc.setTitle("Save Config");

    File saveFile = fc.showSaveDialog(null);
    if (saveFile != null) {
      saveElectionConfig(saveFile);
    }
  }

  public void buttonAddCandidateClicked() {
    Candidate candidate = new Candidate();
    // TODO: check if candidate is already in list?
    if (!textFieldCandidateName.getText().isEmpty()) {
      candidate.setName(textFieldCandidateName.getText());
      candidate.setCode(textFieldCandidateCode.getText());
      tableViewCandidates.getItems().add(candidate);
      textFieldCandidateName.clear();
      textFieldCandidateCode.clear();
    } else {
      // TODO: Need to convey this in the UI; also consider moving validation to setter
      Logger.warn("Candidate name field is required!");
    }
  }

  public void buttonDeleteCandidateClicked() {
    tableViewCandidates
        .getItems()
        .removeAll(tableViewCandidates.getSelectionModel().getSelectedItems());
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    Logger.info("Opening config creator GUI...");

    String helpText;
    try {
      helpText =
          new BufferedReader(
              new InputStreamReader(
                  ClassLoader.getSystemResourceAsStream("config_file_documentation.txt")))
              .lines()
              .collect(Collectors.joining("\n"));
    } catch (Exception e) {
      Logger.severe("Error loading config_file_documentation.txt: %s", e.toString());
      helpText = "<Error loading config_file_documentation.txt>";
    }
    textAreaHelp.setText(helpText);

    datePickerContestDate.setConverter(
        new StringConverter<>() {
          final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

          @Override
          public String toString(LocalDate date) {
            return date != null ? dateFormatter.format(date) : "";
          }

          @Override
          public LocalDate fromString(String string) {
            return string != null && !string.isEmpty()
                ? LocalDate.parse(string, dateFormatter)
                : null;
          }
        });

    tableColumnCandidateName.setCellValueFactory(new PropertyValueFactory<>("name"));
    tableColumnCandidateCode.setCellValueFactory(new PropertyValueFactory<>("code"));
    tableViewCandidates.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

    choiceTiebreakMode.getItems().addAll(Tabulator.TieBreakMode.values());
    choiceTiebreakMode.getItems().remove(Tabulator.TieBreakMode.MODE_UNKNOWN);
    choiceOvervoteRule.getItems().addAll(Tabulator.OvervoteRule.values());
    choiceOvervoteRule.getItems().remove(Tabulator.OvervoteRule.RULE_UNKNOWN);
    choiceMultiSeatTransferRule.getItems().addAll(Tabulator.MultiSeatTransferRule.values());
    choiceMultiSeatTransferRule
        .getItems()
        .remove(Tabulator.MultiSeatTransferRule.TRANSFER_RULE_UNKNOWN);

    // Restrict text fields to non-negative integers and set default values where applicable
    textFieldMaxRankingsAllowed
        .textProperty()
        .addListener(
            (observable, oldValue, newValue) -> {
              if (!newValue.matches("\\d*")) {
                textFieldMaxRankingsAllowed.setText(oldValue);
              }
            });
    textFieldMaxSkippedRanksAllowed
        .textProperty()
        .addListener(
            (observable, oldValue, newValue) -> {
              if (!newValue.matches("\\d*")) {
                textFieldMaxSkippedRanksAllowed.setText(oldValue);
              }
            });
    textFieldNumberOfWinners
        .textProperty()
        .addListener(
            (observable, oldValue, newValue) -> {
              if (!newValue.matches("\\d*")) {
                textFieldNumberOfWinners.setText(oldValue);
              }
            });
    textFieldNumberOfWinners.setText(String.valueOf(ElectionConfig.DEFAULT_NUMBER_OF_WINNERS));
    textFieldDecimalPlacesForVoteArithmetic
        .textProperty()
        .addListener(
            (observable, oldValue, newValue) -> {
              if (!newValue.matches("\\d*")) {
                textFieldDecimalPlacesForVoteArithmetic.setText(oldValue);
              }
            });
    textFieldDecimalPlacesForVoteArithmetic.setText(
        String.valueOf(ElectionConfig.DEFAULT_DECIMAL_PLACES_FOR_VOTE_ARITHMETIC));
    textFieldMinimumVoteThreshold
        .textProperty()
        .addListener(
            (observable, oldValue, newValue) -> {
              if (!newValue.matches("\\d*")) {
                textFieldMinimumVoteThreshold.setText(oldValue);
              }
            });
    textFieldMinimumVoteThreshold.setText(
        String.valueOf(ElectionConfig.DEFAULT_MINIMUM_VOTE_THRESHOLD));
  }

  private void saveElectionConfig(File saveFile) {
    RawElectionConfig config = new RawElectionConfig();
    RawElectionConfig.ElectionRules rules = new RawElectionConfig.ElectionRules();

    config.contestName = textFieldContestName.getText();
    config.outputDirectory = textFieldOutputDirectory.getText();
    config.contestDate =
        datePickerContestDate.getValue() != null ? datePickerContestDate.getValue().toString() : "";
    config.contestJurisdiction = textFieldContestJurisdiction.getText();
    config.contestOffice = textFieldContestOffice.getText();
    config.tabulateByPrecinct =
        ((RadioButton) toggleTabulateByPrecinct.getSelectedToggle()).getText().equals("True");

    config.candidates = new ArrayList<>(tableViewCandidates.getItems());

    rules.tiebreakMode =
        choiceTiebreakMode.getValue() != null
            ? choiceTiebreakMode.getValue().toString()
            : Tabulator.TieBreakMode.MODE_UNKNOWN.toString();
    rules.overvoteRule =
        choiceOvervoteRule.getValue() != null
            ? choiceOvervoteRule.getValue().toString()
            : Tabulator.OvervoteRule.RULE_UNKNOWN.toString();
    rules.multiSeatTransferRule =
        choiceMultiSeatTransferRule.getValue() != null
            ? choiceMultiSeatTransferRule.getValue().toString()
            : Tabulator.MultiSeatTransferRule.TRANSFER_RULE_UNKNOWN.toString();
    rules.maxRankingsAllowed =
        !textFieldMaxRankingsAllowed.getText().isEmpty()
            ? Integer.parseInt(textFieldMaxRankingsAllowed.getText())
            : null;
    rules.maxSkippedRanksAllowed =
        !textFieldMaxSkippedRanksAllowed.getText().isEmpty()
            ? Integer.parseInt(textFieldMaxSkippedRanksAllowed.getText())
            : null;
    rules.numberOfWinners =
        !textFieldNumberOfWinners.getText().isEmpty()
            ? Integer.parseInt(textFieldNumberOfWinners.getText())
            : ElectionConfig.DEFAULT_NUMBER_OF_WINNERS;
    rules.decimalPlacesForVoteArithmetic =
        !textFieldDecimalPlacesForVoteArithmetic.getText().isEmpty()
            ? Integer.parseInt(textFieldDecimalPlacesForVoteArithmetic.getText())
            : ElectionConfig.DEFAULT_DECIMAL_PLACES_FOR_VOTE_ARITHMETIC;
    rules.minimumVoteThreshold =
        !textFieldMinimumVoteThreshold.getText().isEmpty()
            ? Integer.parseInt(textFieldMinimumVoteThreshold.getText())
            : ElectionConfig.DEFAULT_MINIMUM_VOTE_THRESHOLD.intValue();
    rules.batchElimination =
        ((RadioButton) toggleBatchElimination.getSelectedToggle()).getText().equals("True");
    rules.continueUntilTwoCandidatesRemain =
        ((RadioButton) toggleContinueUntilTwoCandidatesRemain.getSelectedToggle())
            .getText()
            .equals("True");
    rules.exhaustOnDuplicateCandidate =
        ((RadioButton) toggleExhaustOnDuplicateCandidate.getSelectedToggle())
            .getText()
            .equals("True");
    rules.treatBlankAsUndeclaredWriteIn =
        ((RadioButton) toggleTreatBlankAsUndeclaredWriteIn.getSelectedToggle())
            .getText()
            .equals("True");
    config.rules = rules;

    String response = JsonParser.createFileFromRawElectionConfig(saveFile, config);
    if (response.equals("SUCCESS")) {
      Logger.info("Saved config via the GUI to: %s", saveFile.getAbsolutePath());
    }
  }
}
