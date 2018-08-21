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
import java.io.IOException;
import java.util.logging.Level;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;

class GuiContext {

  // context instance
  private static final GuiContext instance = new GuiContext();
  // currently-loaded tabulator config
  private static ElectionConfig config;
  // file selected for loading
  private static File selectedFile;
  // VBox for displaying main content
  private VBox contentVBox;

  static GuiContext getInstance() {
    return instance;
  }

  static ElectionConfig getConfig() {
    return config;
  }

  static void setConfig(ElectionConfig config) {
    GuiContext.config = config;
  }

  static File getSelectedFile() {
    return selectedFile;
  }

  static void setSelectedFile(File selectedFile) {
    GuiContext.selectedFile = selectedFile;
  }

  private VBox getContentVBox() {
    return contentVBox;
  }

  void setContentVBox(VBox contentVBox) {
    this.contentVBox = contentVBox;
  }

  void showContent(String resourcePath) {
    getContentVBox().getChildren().clear();
    try {
      getContentVBox().getChildren().add(FXMLLoader.load(getClass().getResource(resourcePath)));
    } catch (IOException e) {
      Logger.executionLog(
          Level.SEVERE, "Failed to open '%s': %s", resourcePath, e.getCause().toString());
    }
  }
}
