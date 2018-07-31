package com.rcv;

import java.io.File;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class GuiController implements Initializable {

  // currently-loaded tabulator config
  private static ElectionConfig config;

  // text area which communicates the status of the tabulator's operations
  @FXML
  private TextArea textStatus;

  // function: printToTextStatus
  // purpose: prints a message to the textStatus box, with timestamp and line break
  // param: the message to print
  // returns: N/A
  private void printToTextStatus(String message) {
    textStatus.appendText("* ");
    textStatus.appendText(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(ZonedDateTime.now()));
    textStatus.appendText(": ");
    textStatus.appendText(message);
    textStatus.appendText("\n");
  }

  // function: buttonCreateConfigClicked
  // purpose: performs an action when buttonCreateConfig is clicked
  // param: N/A
  // returns: N/A
  public void buttonCreateConfigClicked() {
    printToTextStatus("Opening config creator...");
    // TODO: add code to actually swap the scene
  }

  // function: buttonLoadConfigClicked
  // purpose: performs an action when buttonLoadConfig is clicked
  // param: N/A
  // returns: N/A
  public void buttonLoadConfigClicked() {
    // Current working directory
    File workingDirectory = new File(System.getProperty("user.dir"));
    // FileChooser used as a dialog box for loading a config
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

  // function: buttonTabulateClicked
  // purpose: performs an action when buttonTabulate is clicked
  // param: N/A
  // returns: N/A
  public void buttonTabulateClicked() {
    printToTextStatus("Starting tabulation...");
    // String indicating whether or not execution was successful
    String response = Main.executeTabulation(config);
    printToTextStatus(response);
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    Logger.info("No arguments provided; starting GUI...");
  }
}
