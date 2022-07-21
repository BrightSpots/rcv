/*
 * RCTab
 * Copyright (c) 2017-2022 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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
