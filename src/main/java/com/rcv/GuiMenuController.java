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
    GuiContext.getInstance().setConfig(null);
    GuiContext.getInstance().setSelectedFile(null);
    openConfigCreator();
  }

  public void buttonModifyConfigClicked() {
    buttonLoadConfigClicked();
    if (GuiContext.getInstance().getConfig() != null) {
      openConfigCreator();
    }
  }

  public void buttonLoadConfigClicked() {
    FileChooser fc = new FileChooser();
    fc.setInitialDirectory(new File(System.getProperty("user.dir")));
    fc.getExtensionFilters().add(new ExtensionFilter("JSON files", "*.json"));
    fc.setTitle("Load Config");

    GuiContext.getInstance().setSelectedFile(fc.showOpenDialog(null));
    if (GuiContext.getInstance().getSelectedFile() != null) {
      GuiContext.getInstance()
          .setConfig(
              Main.loadElectionConfig(
                  GuiContext.getInstance().getSelectedFile().getAbsolutePath()));
    }
  }

  public void buttonTabulateClicked() {
    if (GuiContext.getInstance().getConfig() != null) {
      new Thread(() -> Main.executeTabulation(GuiContext.getInstance().getConfig())).start();
    } else {
      Logger.guiLog(Level.WARNING, "Please load a config file before attempting to tabulate!");
    }
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    Logger.executionLog(Level.FINE, "Opening main menu GUI...");
  }
}
