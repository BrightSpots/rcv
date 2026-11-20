/*
 * RCTab
 * Copyright (c) 2017-2023 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: Main entry point for JavaFX GUI application startup.  Loads layout resources.
 * Design: This class uses JavaFX and implements the Application start method.
 * Conditions: Used whenever using the GUI application mode.
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;

@SuppressWarnings("WeakerAccess")
class GuiApplication extends Application {

  private static final int STAGE_HEIGHT = 1000;
  private static final int STAGE_WIDTH = 1200;

  private void loadBrandFonts() {
    String base = "/network/brightspots/rcv/fonts/";
    String[] fonts = {
        "PublicSans-Regular.ttf", "PublicSans-Medium.ttf",
        "PublicSans-SemiBold.ttf", "PublicSans-Bold.ttf",
        "Montserrat-Bold.ttf", "Montserrat-ExtraBold.ttf",
        "IBMPlexMono-Regular.ttf", "IBMPlexMono-Medium.ttf"
    };
    for (String f : fonts) {
      try (var in = getClass().getResourceAsStream(base + f)) {
        if (in != null) Font.loadFont(in, 12);
      } catch (Exception e) { /* falls back to system sans */ }
    }
  }

  @Override
  public void start(Stage window) {
    loadBrandFonts();

    GuiContext context = GuiContext.getInstance();
    context.setMainWindow(window);

    String resourcePath = "/network/brightspots/rcv/GuiConfigLayout.fxml";
    String iconPath = "/network/brightspots/rcv/launcher.png";
    try {
      Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(resourcePath)));
      window.setTitle(Main.APP_NAME);
      Scene scene = new Scene(root);
      scene.getStylesheets().add(
          Objects.requireNonNull(
              getClass().getResource("/network/brightspots/rcv/RCTabTheme.css")).toExternalForm());
      window.setScene(scene);

      Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream(iconPath)));
      window.getIcons().add(icon);
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
