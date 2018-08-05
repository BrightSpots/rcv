package com.rcv;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

public class GuiConfigController implements Initializable {

  @FXML
  private TextField textContestName;
  @FXML
  private ChoiceBox<Tabulator.OvervoteRule> choiceOvervoteRule;

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
    choiceOvervoteRule.getSelectionModel().select(0);
  }

  private void saveElectionConfig(File saveFile) {
    RawElectionConfig config = new RawElectionConfig();
    RawElectionConfig.ElectionRules rules = new RawElectionConfig.ElectionRules();

    config.contestName = textContestName.getText();

    rules.overvoteRule = choiceOvervoteRule.getValue().toString();
    config.rules = rules;

    String response = JsonParser.createFileFromRawElectionConfig(saveFile, config);
    if (response.equals("SUCCESS")) {
      Logger.info("Saved config via the GUI to: %s", saveFile.getAbsolutePath());
    }
  }
}
