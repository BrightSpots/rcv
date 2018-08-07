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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
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
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class GuiConfigController implements Initializable {

  @FXML
  private TextField textContestName;
  @FXML
  private TextField textOutputDirectory;
  @FXML
  private ChoiceBox<Tabulator.OvervoteRule> choiceOvervoteRule;
  @FXML
  private ToggleGroup toggleTabulateByPrecinct;
  @FXML
  private DatePicker datePickerContestDate;

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
      textOutputDirectory.setText(outputDirectory.getAbsolutePath());
    }
  }

  public void buttonMenuClicked(ActionEvent event) throws IOException {
    Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
    Parent menuParent = FXMLLoader.load(getClass().getResource("GuiMainLayout.fxml"));
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

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    Logger.info("Opening config creator GUI...");

    choiceOvervoteRule.getItems().addAll(Tabulator.OvervoteRule.values());
    choiceOvervoteRule.getItems().remove(Tabulator.OvervoteRule.RULE_UNKNOWN);

    datePickerContestDate.setConverter(
        new StringConverter<>() {
          final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

          @Override
          public String toString(LocalDate date) {
            return (date != null) ? dateFormatter.format(date) : "";
          }

          @Override
          public LocalDate fromString(String string) {
            return (string != null && !string.isEmpty())
                ? LocalDate.parse(string, dateFormatter)
                : null;
          }
        });
  }

  private void saveElectionConfig(File saveFile) {
    RawElectionConfig config = new RawElectionConfig();
    RawElectionConfig.ElectionRules rules = new RawElectionConfig.ElectionRules();

    config.contestName = textContestName.getText();
    config.outputDirectory = textOutputDirectory.getText();
    config.tabulateByPrecinct =
        ((RadioButton) toggleTabulateByPrecinct.getSelectedToggle()).getText().equals("True");
    config.contestDate =
        (datePickerContestDate.getValue() != null)
            ? datePickerContestDate.getValue().toString()
            : "";
    rules.overvoteRule =
        (choiceOvervoteRule.getValue() != null)
            ? choiceOvervoteRule.getValue().toString()
            : Tabulator.OvervoteRule.RULE_UNKNOWN.toString();
    config.rules = rules;

    String response = JsonParser.createFileFromRawElectionConfig(saveFile, config);
    if (response.equals("SUCCESS")) {
      Logger.info("Saved config via the GUI to: %s", saveFile.getAbsolutePath());
    }
  }
}
