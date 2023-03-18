/*
 * RCTab
 * Copyright (c) 2017-2022 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: When loading contest config files into the GUI, this class will migrate older config
 * data to the latest version.
 * Design: Static methods which operate on contest config data.
 * Conditions: When using the GUI.
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import static network.brightspots.rcv.Utils.isNullOrBlank;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import network.brightspots.rcv.RawContestConfig.ContestRules;
import network.brightspots.rcv.RawContestConfig.CvrSource;
import network.brightspots.rcv.Tabulator.TiebreakMode;
import network.brightspots.rcv.Tabulator.WinnerElectionMode;

final class ContestConfigMigration {

  private static final Pattern versionNumPattern = Pattern.compile("(\\d+).*");

  private ContestConfigMigration() {
  }

  private static ArrayList<Integer> parseVersionString(String version) {
    ArrayList<Integer> parsed = new ArrayList<>();
    String[] arr = version.split("\\.");
    for (String numString : arr) {
      Matcher m = versionNumPattern.matcher(numString);
      if (m.matches()) {
        parsed.add(Integer.parseInt(m.group(1)));
      }
    }

    return parsed;
  }

  // not intended to be used if either version is null
  private static boolean isVersionNewer(String version1, String version2) {
    boolean isNewer = false;
    if (!version1.equals(version2)) {
      ArrayList<Integer> version1Parsed = parseVersionString(version1);
      ArrayList<Integer> version2Parsed = parseVersionString(version2);

      for (int i = 0; i < version1Parsed.size(); i++) {
        if (version2Parsed.size() <= i) {
          isNewer = true;
          break;
        }
        int version1Num = version1Parsed.get(i);
        int version2Num = version2Parsed.get(i);
        if (version1Num > version2Num) {
          isNewer = true;
          break;
        } else if (version2Num > version1Num) {
          break;
        }
      }
    }

    return isNewer;
  }

  static boolean isConfigVersionOlderThanAppVersion(String configVersion) {
    return configVersion == null || isVersionNewer(Main.APP_VERSION, configVersion);
  }

  static boolean isConfigVersionNewerThanAppVersion(String configVersion) {
    boolean isNewer = false;
    if (configVersion != null) {
      if (isVersionNewer(configVersion, Main.APP_VERSION)) {
        Logger.severe(
            "Unable to process a config file with version %s using older version %s of the app!",
            configVersion, Main.APP_VERSION);
        isNewer = true;
      }
    }

    return isNewer;
  }

  static void migrateConfigVersion(ContestConfig config)
      throws ConfigVersionIsNewerThanAppVersionException {
    String version = config.rawConfig.tabulatorVersion;
    boolean needsMigration = version == null
        || (!version.equals(Main.APP_VERSION) && !version
        .equals(ContestConfig.AUTOMATED_TEST_VERSION));
    if (needsMigration) {
      if (isConfigVersionNewerThanAppVersion(version)) {
        throw new ConfigVersionIsNewerThanAppVersionException();
      }

      // Any necessary future version migration logic goes here
      RawContestConfig rawConfig = config.getRawConfig();
      ContestRules rules = rawConfig.rules;

      if (config.getWinnerElectionMode() == WinnerElectionMode.MODE_UNKNOWN) {
        String oldWinnerElectionMode = rules.winnerElectionMode;
        switch (oldWinnerElectionMode) {
          case "standard" -> rules.winnerElectionMode =
              config.getNumberOfWinners() > 1
                  ? WinnerElectionMode.MULTI_SEAT_ALLOW_MULTIPLE_WINNERS_PER_ROUND
                  .getInternalLabel()
                  : WinnerElectionMode.STANDARD_SINGLE_WINNER.getInternalLabel();
          case "singleSeatContinueUntilTwoCandidatesRemain" -> {
            rules.winnerElectionMode = WinnerElectionMode.STANDARD_SINGLE_WINNER
                .getInternalLabel();
            rules.continueUntilTwoCandidatesRemain = true;
          }
          case "multiSeatAllowOnlyOneWinnerPerRound" -> rules.winnerElectionMode =
              WinnerElectionMode.MULTI_SEAT_ALLOW_ONLY_ONE_WINNER_PER_ROUND.getInternalLabel();
          case "multiSeatBottomsUp" -> rules.winnerElectionMode =
              config.getNumberOfWinners() == 0
                  || config.getMultiSeatBottomsUpPercentageThreshold() != null
                  ? WinnerElectionMode.MULTI_SEAT_BOTTOMS_UP_USING_PERCENTAGE_THRESHOLD
                  .getInternalLabel()
                  : WinnerElectionMode.MULTI_SEAT_BOTTOMS_UP_UNTIL_N_WINNERS.getInternalLabel();
          case "multiSeatSequentialWinnerTakesAll" -> rules.winnerElectionMode =
              WinnerElectionMode.MULTI_SEAT_SEQUENTIAL_WINNER_TAKES_ALL.getInternalLabel();
          default -> {
            Logger.warning(
                "winnerElectionMode \"%s\" is unrecognized! Please supply a valid "
                    + "winnerElectionMode.", oldWinnerElectionMode);
            rules.winnerElectionMode = null;
          }
        }
      }

      if (config.getTiebreakMode() == TiebreakMode.MODE_UNKNOWN) {
        Map<String, TiebreakMode> tiebreakModeMigrationMap = Map.of(
            "random", TiebreakMode.RANDOM,
            "interactive", TiebreakMode.INTERACTIVE,
            "previousRoundCountsThenRandom",
            TiebreakMode.PREVIOUS_ROUND_COUNTS_THEN_RANDOM,
            "previousRoundCountsThenInteractive",
            TiebreakMode.PREVIOUS_ROUND_COUNTS_THEN_INTERACTIVE,
            "usePermutationInConfig", TiebreakMode.USE_PERMUTATION_IN_CONFIG,
            "generatePermutation", TiebreakMode.GENERATE_PERMUTATION
        );
        String oldTiebreakMode = rules.tiebreakMode;
        if (tiebreakModeMigrationMap.containsKey(oldTiebreakMode)) {
          rules.tiebreakMode = tiebreakModeMigrationMap.get(oldTiebreakMode).getInternalLabel();
        } else {
          Logger.warning(
              "tiebreakMode \"%s\" is unrecognized! Please supply a valid tiebreakMode.",
              oldTiebreakMode);
          rules.tiebreakMode = null;
        }
      }

      // These four fields were previously at the config level, but are now set on a per-source
      // basis.

      if (!isNullOrBlank(rules.overvoteLabel)) {
        for (CvrSource source : rawConfig.cvrFileSources) {
          source.setOvervoteLabel(rules.overvoteLabel);
        }
      }

      if (!isNullOrBlank(rules.undervoteLabel)) {
        for (CvrSource source : rawConfig.cvrFileSources) {
          source.setUndervoteLabel(rules.undervoteLabel);
        }
      }

      if (!isNullOrBlank(rules.undeclaredWriteInLabel)) {
        for (CvrSource source : rawConfig.cvrFileSources) {
          source.setUndeclaredWriteInLabel(rules.undeclaredWriteInLabel);
        }
      }

      if (rules.treatBlankAsUndeclaredWriteIn) {
        for (CvrSource source : rawConfig.cvrFileSources) {
          source.setTreatBlankAsUndeclaredWriteIn(rules.treatBlankAsUndeclaredWriteIn);
        }
      }

      // Migrations from 1.3.0 to 1.4.0
      if (rules.stopTabulationEarlyOnRound == null) {
        rules.stopTabulationEarlyOnRound = "";
      }

      Logger.info(
          "Migrated tabulator config version from %s to %s.",
          config.rawConfig.tabulatorVersion != null ? config.rawConfig.tabulatorVersion : "unknown",
          Main.APP_VERSION);

      config.rawConfig.tabulatorVersion = Main.APP_VERSION;
    }
  }

  static class ConfigVersionIsNewerThanAppVersionException extends Exception {

  }
}
