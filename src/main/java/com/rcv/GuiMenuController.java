/*
 * Ranked Choice Voting Universal Tabulator
 * Copyright (c) 2018 Jonathan Moldover, Louis Eisenberg, and Hylton Edingfield
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
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import javafx.fxml.Initializable;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class GuiMenuController implements Initializable {

  private void openConfigCreator() {
    GuiContext.getInstance().showContent("/GuiConfigLayout.fxml");
  }

  public void buttonCreateConfigClicked() {
    GuiContext.setConfig(null);
    GuiContext.setSelectedFile(null);
    openConfigCreator();
  }

  public void buttonModifyConfigClicked() {
    buttonLoadConfigClicked();
    if (GuiContext.getConfig() != null) {
      openConfigCreator();
    }
  }

  public void buttonLoadConfigClicked() {
    FileChooser fc = new FileChooser();
    fc.setInitialDirectory(new File(System.getProperty("user.dir")));
    fc.getExtensionFilters().add(new ExtensionFilter("JSON files", "*.json"));
    fc.setTitle("Load Config");

    GuiContext.setSelectedFile(fc.showOpenDialog(null));
    if (GuiContext.getSelectedFile() != null) {
      GuiContext.setConfig(Main.loadElectionConfig(GuiContext.getSelectedFile().getAbsolutePath()));
    }
  }

  public void buttonTabulateClicked() {
    if (GuiContext.getConfig() != null) {
      Main.executeTabulation(GuiContext.getConfig());
    } else {
      Logger.executionLog(Level.WARNING, "Please load a config before attempting to tabulate!");
    }
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    Logger.executionLog(Level.INFO, "Opening main menu GUI...");
  }
}
