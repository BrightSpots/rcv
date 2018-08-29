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
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class GuiMenuController implements Initializable {

  @FXML
  private VBox vboxMenu;

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
              Main.loadContestConfig(
                  GuiContext.getInstance().getSelectedFile().getAbsolutePath()));
    }
  }

  private void setButtonsDisable(boolean disable) {
    for (Node node : vboxMenu.getChildren()) {
      if (node instanceof Button) {
        node.setDisable(disable);
      }
    }
  }

  public void buttonTabulateClicked() {
    if (GuiContext.getInstance().getConfig() != null) {
      setButtonsDisable(true);
      TabulatorService service = new TabulatorService();
      service.setOnSucceeded(event -> setButtonsDisable(false));
      service.setOnCancelled(event -> setButtonsDisable(false));
      service.setOnFailed(event -> setButtonsDisable(false));
      service.start();
    } else {
      Logger.guiLog(Level.WARNING, "Please load a config file before attempting to tabulate!");
    }
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    Logger.executionLog(Level.FINE, "Opening main menu GUI...");
  }

  private static class TabulatorService extends Service<Void> {

    @Override
    protected Task<Void> createTask() {
      return new Task<>() {
        @Override
        protected Void call() {
          Main.executeTabulation(GuiContext.getInstance().getConfig());
          return null;
        }
      };
    }
  }
}
