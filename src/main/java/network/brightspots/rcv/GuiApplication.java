/*
 * RCTab
 * Copyright (c) 2017-2022 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: Main entry point for JavaFX GUI application startup.  Loads layout resources.
 * Design: This class uses JavaFX and implements the Application start method.
 * Conditions: Used whenever using the GUI application mode.
 * Version history: Version 1.0.
 * Complete revision history is available at: https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

@SuppressWarnings("WeakerAccess")
class GuiApplication extends Application {

  private static final int STAGE_HEIGHT = 1000;
  private static final int STAGE_WIDTH = 1200;

  @Override
  public void start(Stage window) {
    GuiContext context = GuiContext.getInstance();
    context.setMainWindow(window);

    String resourcePath = "/network/brightspots/rcv/GuiConfigLayout.fxml";
    try {
      Parent root = FXMLLoader.load(getClass().getResource(resourcePath));
      window.setTitle(Main.APP_NAME);
      window.setScene(new Scene(root));
    } catch (IOException exception) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      exception.printStackTrace(pw);
      Logger.severe("Failed to open: %s:\n%s. ", resourcePath, sw);
    }

    // Avoid cutting off the top bar for low resolution displays
    window.setHeight(Math.min(STAGE_HEIGHT, Screen.getPrimary().getVisualBounds().getHeight()));
    window.setWidth(Math.min(STAGE_WIDTH, Screen.getPrimary().getVisualBounds().getWidth()));
    window.show();
  }
}
