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

/*
 * Purpose:
 * Main entry point for the RCV module.
 * Parse command line and launch GUI or create and run a tabulation session.
 */

package network.brightspots.rcv;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import network.brightspots.rcv.CastVoteRecord.CvrParseException;

@SuppressWarnings("WeakerAccess")
public class Main extends GuiApplication {

  public static final String APP_NAME = "Universal RCV Tabulator";
  public static final String APP_VERSION = "1.0.1";

  /**
   * Main entry point to the RCV tabulator program.
   * @param args command-line args
   */
  public static void main(String[] args) {
    System.out.println(String.format("%s version %s", APP_NAME, APP_VERSION));
    Logger.setup();
    logSystemInfo();

    // for testing
    String wyoming = "/Flair/Downloads/Dominion/Wyoming";
    String sf = "/Flair/Downloads/Dominion/SF";
    List<CastVoteRecord> castVoteRecords = new ArrayList<>();
    DominionCvrReader test = new DominionCvrReader(wyoming);
    try {
      test.readCastVoteRecords(castVoteRecords);
    } catch (CvrParseException e) {
      e.printStackTrace();
    }

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
      Logger.log(Level.INFO, "Tabulator is being used via the CLI.");
      // check for unexpected input
      if (argsCli.size() == 0) {
        Logger.log(Level.SEVERE, "No config file path provided on command line!\n"
            + "Please provide a path to the config file!\n"
            + "See README.md for more details.");
        System.exit(1);
      } else if (argsCli.size() > 2) {
        Logger.log(Level.SEVERE, "Too many arguments! Max is 2 but got: %d\n"
                + "See README.md for more details.",
            argsCli.size());
        System.exit(2);
      }
      // config file for configuring the tabulator
      String configPath = argsCli.get(0);
      boolean convertToCdf = argsCli.size() == 2 && argsCli.get(1).equals("convert-to-cdf");
      // session object will manage the tabulation process
      TabulatorSession session = new TabulatorSession(configPath);
      if (convertToCdf) {
        session.convertToCdf();
      } else {
        session.tabulate();
      }
    }

    System.exit(0);
  }

  private static void logSystemInfo() {
    Logger.log(Level.INFO, "Launching %s version %s...", APP_NAME, APP_VERSION);
    Logger.log(Level.INFO, "Host system: %s version %s", System.getProperty("os.name"),
        System.getProperty("os.version"));
  }

}
