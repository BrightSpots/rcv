/*
 * RCTab
 * Copyright (c) 2017-2023 Bright Spots Developers.
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
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;
import java.util.stream.Stream;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/** Main entry point to RCTab. */
@SuppressWarnings("WeakerAccess")
public class Main extends GuiApplication {

  public static final String APP_NAME = "RCTab";

  // TODO Sync version number with release.yml and build.gradle:
  // github.com/BrightSpots/rcv/issues/662
  public static final String APP_VERSION = "1.4.0-alpha";

  /**
   * Main entry point to RCTab.
   *
   * @param args command-line args
   */
  public static void main(String[] args) {
    System.out.printf("%s version %s%n", APP_NAME, APP_VERSION);
    Logger.setup();
    logSystemInfo();

    // Check if args contains string "--cli"
    if (Arrays.stream(args).filter(arg -> arg.equals("--cli")).findAny().isEmpty()) {
      // --cli not found. Launch the GUI
      launch(args);
    } else {
      Logger.info("Tabulator is being used via the CLI");

      CommandLine cmd = parseArgsForCli(args);
      String path = cmd.getOptionValue("cli");
      String name = cmd.getOptionValue("name");
      boolean convertToCdf = cmd.hasOption("convert-to-cdf");

      boolean validNameProvided = false;
      if (name != null) {
        // Name was provided via CLI arg
        name = name.trim();
        if (!name.isEmpty()) {
          validNameProvided = true;
          Logger.info("Operator name(s), as entered via command-line argument: " + name);
        }
      } else {
        // Name wasn't provided via CLI arg, so prompt user to enter
        Logger.info("Enter operator name(s), for auditing purposes:");

        Scanner sc = new Scanner(System.in, StandardCharsets.UTF_8);
        if (sc.hasNextLine()) {
          name = sc.nextLine();
        }

        if (name != null) {
          name = name.trim();
          if (!name.isEmpty()) {
            validNameProvided = true;
            Logger.info("Operator name(s), as entered interactively: " + name);
          }
        }
      }

      if (!validNameProvided) {
        Logger.severe(
            "Must supply --name as a CLI argument, or run via an interactive shell and actually provide"
                + " operator name(s)!");
        System.exit(1);
      }

      TabulatorSession session = new TabulatorSession(path);
      if (convertToCdf) {
        session.convertToCdf();
      } else {
        session.tabulate();
      }
    }

    System.exit(0);
  }

  // Call this function if using the command line interface. Do not call if --cli
  // has not been passed as an argument; it will fail.
  private static CommandLine parseArgsForCli(String[] args) {
    // Remove all args that start with "-D" -- these are added automatically when running via
    // IntelliJ
    Stream<String> filteredArgs = Arrays.stream(args).filter(arg -> !arg.startsWith("-D"));
    args = filteredArgs.toArray(String[]::new);

    Options options = new Options();

    Option inputPath =
        new Option("c", "cli", true, "launch command-line version, providing path to config file");
    inputPath.setRequired(true);
    options.addOption(inputPath);

    Option doConvert =
        new Option("x", "convert-to-cdf", false, "convert CVR(s) to CDF (instead of tabulating)");
    doConvert.setRequired(false);
    options.addOption(doConvert);

    Option name = new Option("n", "name", true, "current operator name(s), for auditing purposes");
    name.setRequired(false);
    options.addOption(name);

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = null;

    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      Logger.severe(e.getMessage());
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("RCTab", options);

      System.exit(1);
    }

    return cmd;
  }

  private static void logSystemInfo() {
    Logger.info("Launching %s version %s...", APP_NAME, APP_VERSION);
    Logger.info(
        "Host system: %s version %s",
        System.getProperty("os.name"), System.getProperty("os.version"));
  }
}
