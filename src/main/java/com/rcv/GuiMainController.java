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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

public class GuiMainController implements Initializable {

  // currently-loaded tabulator config
  private static ElectionConfig config;

  // text area which communicates the status of the tabulator's operations
  @FXML
  private TextArea textAreaStatus;

  private void printToTextStatus(String message) {
    textAreaStatus.appendText("* ");
    textAreaStatus.appendText(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(ZonedDateTime.now()));
    textAreaStatus.appendText(": ");
    textAreaStatus.appendText(message);
    textAreaStatus.appendText("\n");
  }

  public void buttonCreateConfigClicked(ActionEvent event) throws IOException {
    Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
    Parent configParent = FXMLLoader.load(getClass().getResource("/GuiConfigLayout.fxml"));
    window.setScene(new Scene(configParent));
  }

  public void buttonLoadConfigClicked() {
    FileChooser fc = new FileChooser();
    fc.setInitialDirectory(new File(System.getProperty("user.dir")));
    fc.getExtensionFilters().add(new ExtensionFilter("JSON files", "*.json"));
    fc.setTitle("Load Config");

    File selectedFile = fc.showOpenDialog(null);
    if (selectedFile != null) {
      String configPath = selectedFile.getAbsolutePath();
      config = Main.loadElectionConfig(configPath);
      if (config == null) {
        printToTextStatus(String.format("ERROR: Unable to load config file: %s", configPath));
      } else {
        printToTextStatus(String.format("Successfully loaded config file: %s", configPath));
      }
    }
  }

  public void buttonTabulateClicked() {
    if (config != null) {
      printToTextStatus("Starting tabulation...");
      String response = Main.executeTabulation(config);
      printToTextStatus(response);
      printToTextStatus(String.format("Output available here: %s", config.getOutputDirectory()));
    } else {
      printToTextStatus("Please load a config before attempting to tabulate!");
    }
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    Logger.info("Opening main menu GUI...");
  }
}
