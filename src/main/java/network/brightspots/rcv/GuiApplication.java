/*
 * Universal RCV Tabulator
 * Copyright (c) 2017-2020 Bright Spots Developers.
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
