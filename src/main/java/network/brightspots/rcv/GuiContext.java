/*
 * Universal RCV Tabulator
 * Copyright (c) 2017-2019 Bright Spots Developers.
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

import javafx.stage.Stage;

class GuiContext {

  // context instance
  private static final GuiContext INSTANCE = new GuiContext();
  // currently-loaded tabulator config
  private ContestConfig config;
  // cache for main window so we can parent file choosers to it
  private Stage mainWindow;

  private GuiContext() {
  }

  static GuiContext getInstance() {
    return INSTANCE;
  }

  ContestConfig getConfig() {
    return config;
  }

  void setConfig(ContestConfig config) {
    this.config = config;
  }

  Stage getMainWindow() {
    return mainWindow;
  }

  void setMainWindow(Stage mainWindow) {
    this.mainWindow = mainWindow;
  }
}
