/*
 * RCTab
 * Copyright (c) 2017-2022 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: Main entry point for the RCV module.
 * Parse command line, configure logging and launch GUI or create and run a tabulation session.
 * Design: NA.
 * Conditions: Always.
 * Version history: Version 1.0.
 * Complete revision history is available at: https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import java.util.ArrayList;
import java.util.List;

/**
 * Main entry point to RCTab.
 */
@SuppressWarnings("WeakerAccess")
public class Main extends GuiApplication {

  public static final String APP_NAME = "RCTab";
  public static final String APP_VERSION = "1.2.0";

  /**
   * Main entry point to RCTab.
   *
   * @param args command-line args
   */
  public static void main(String[] args) {
    System.out.printf("%s version %s%n", APP_NAME, APP_VERSION);
    Logger.setup();
    logSystemInfo();

    // Determine if user intends to use the command-line interface, and gather args if so
    boolean useCli = false;
    List<String> argsCli = new ArrayList<>();
    for (String arg : args) {
      if (!useCli && arg.equals("-cli")) {
        useCli = true;
      } else if (useCli) {
        argsCli.add(arg);
      }
    }

    if (!useCli) {
      // Launch the GUI
      launch(args);
    } else {
      Logger.info("Tabulator is being used via the CLI.");
      // Check for unexpected input
      if (argsCli.size() == 0) {
        Logger.severe(
            "No config file path provided on command line!\n"
                + "Please provide a path to the config file!\n"
                + "See README.md for more details.");
        System.exit(1);
      } else if (argsCli.size() > 2) {
        Logger.severe(
            "Too many arguments! Max is 2 but got: %d\n" + "See README.md for more details.",
            argsCli.size());
        System.exit(2);
      }
      // Path to either: config file for configuring the tabulator, or Dominion JSONs
      String providedPath = argsCli.get(0);
      // Session object will manage the tabulation process
      TabulatorSession session = new TabulatorSession(providedPath);
      if (argsCli.size() == 2 && argsCli.get(1).equals("convert-to-cdf")) {
        session.convertToCdf();
      } else {
        session.tabulate();
      }
    }

    System.exit(0);
  }

  private static void logSystemInfo() {
    Logger.info("Launching %s version %s...", APP_NAME, APP_VERSION);
    Logger.info(
        "Host system: %s version %s",
        System.getProperty("os.name"), System.getProperty("os.version"));
  }
}
