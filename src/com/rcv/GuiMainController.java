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
  private TextArea textStatus;

  private void printToTextStatus(String message) {
    textStatus.appendText("* ");
    textStatus.appendText(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(ZonedDateTime.now()));
    textStatus.appendText(": ");
    textStatus.appendText(message);
    textStatus.appendText("\n");
  }

  public void buttonCreateConfigClicked(ActionEvent event) throws IOException {
    Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
    Parent configParent = FXMLLoader.load(getClass().getResource("GuiConfigLayout.fxml"));
    window.setScene(new Scene(configParent));
  }

  public void buttonLoadConfigClicked() {
    File workingDirectory = new File(System.getProperty("user.dir"));
    FileChooser fc = new FileChooser();
    fc.setInitialDirectory(workingDirectory);
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
