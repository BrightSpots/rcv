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

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import network.brightspots.rcv.Tabulator.OvervoteRule;
import network.brightspots.rcv.Tabulator.TieBreakMode;
import network.brightspots.rcv.Tabulator.WinnerElectionMode;

class ContestConfigMigration {

  private static final Pattern versionNumPattern = Pattern.compile("(\\d+).*");

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
    if (version1.equals(version2)) {
      return false;
    }

    ArrayList<Integer> version1Parsed = parseVersionString(version1);
    ArrayList<Integer> version2Parsed = parseVersionString(version2);

    for (int i = 0; i < version1Parsed.size(); i++) {
      if (version2Parsed.size() <= i) {
        return true;
      }
      int version1Num = version1Parsed.get(i);
      int version2Num = version2Parsed.get(i);
      if (version1Num > version2Num) {
        return true;
      } else if (version2Num > version1Num) {
        return false;
      }
    }

    return false;
  }

  static boolean isConfigVersionOlderThanAppVersion(String configVersion) {
    return configVersion == null || isVersionNewer(Main.APP_VERSION, configVersion);
  }

  static boolean isConfigVersionNewerThanAppVersion(String configVersion) {
    if (configVersion == null) {
      return false;
    }

    if (isVersionNewer(configVersion, Main.APP_VERSION)) {
      Logger.severe(
          "Unable to process a config file with version %s using older version %s of the app!",
          configVersion, Main.APP_VERSION);
      return true;
    }

    return false;
  }

  static void migrateConfigVersion(ContestConfig config)
      throws ConfigVersionIsNewerThanAppVersionException {
    String version = config.rawConfig.tabulatorVersion;
    if (version != null &&
        (version.equals(Main.APP_VERSION) || version
            .equals(ContestConfig.AUTOMATED_TEST_VERSION))) {
      return;
    }

    if (isConfigVersionNewerThanAppVersion(version)) {
      throw new ConfigVersionIsNewerThanAppVersionException();
    }

    // Any necessary future version migration logic goes here

    if (config.getWinnerElectionMode() == WinnerElectionMode.MODE_UNKNOWN) {
      String oldWinnerElectionMode = config.rawConfig.rules.winnerElectionMode;
      switch (oldWinnerElectionMode) {
        case "standard" -> config.rawConfig.rules.winnerElectionMode =
            config.getNumberOfWinners() > 1
                ? WinnerElectionMode.MULTI_SEAT_ALLOW_MULTIPLE_WINNERS_PER_ROUND.toString()
                : WinnerElectionMode.STANDARD_SINGLE_WINNER.toString();
        case "singleSeatContinueUntilTwoCandidatesRemain" -> {
          config.rawConfig.rules.winnerElectionMode = WinnerElectionMode.STANDARD_SINGLE_WINNER
              .toString();
          config.rawConfig.rules.continueUntilTwoCandidatesRemain = true;
        }
        case "multiSeatAllowOnlyOneWinnerPerRound" -> config.rawConfig.rules.winnerElectionMode =
            WinnerElectionMode.MULTI_SEAT_ALLOW_ONLY_ONE_WINNER_PER_ROUND.toString();
        case "multiSeatBottomsUp" -> config.rawConfig.rules.winnerElectionMode =
            config.getNumberOfWinners() == 0
                || config.getMultiSeatBottomsUpPercentageThreshold() != null
                ? WinnerElectionMode.MULTI_SEAT_BOTTOMS_UP_USING_PERCENTAGE_THRESHOLD.toString()
                : WinnerElectionMode.MULTI_SEAT_BOTTOMS_UP_UNTIL_N_WINNERS.toString();
        case "multiSeatSequentialWinnerTakesAll" -> config.rawConfig.rules.winnerElectionMode =
            WinnerElectionMode.MULTI_SEAT_SEQUENTIAL_WINNER_TAKES_ALL.toString();
        default -> {
          Logger.warning(
              "winnerElectionMode \"%s\" is unrecognized! Please supply a valid "
                  + "winnerElectionMode.", oldWinnerElectionMode);
          config.rawConfig.rules.winnerElectionMode = null;
        }
      }
    }

    if (config.getTiebreakMode() == TieBreakMode.MODE_UNKNOWN) {
      Map<String, String> tiebreakModeMigrationMap = Map.of(
          "random", TieBreakMode.RANDOM.toString(),
          "interactive", TieBreakMode.INTERACTIVE.toString(),
          "previousRoundCountsThenRandom",
          TieBreakMode.PREVIOUS_ROUND_COUNTS_THEN_RANDOM.toString(),
          "previousRoundCountsThenInteractive",
          TieBreakMode.PREVIOUS_ROUND_COUNTS_THEN_INTERACTIVE.toString(),
          "usePermutationInConfig", TieBreakMode.USE_PERMUTATION_IN_CONFIG.toString(),
          "generatePermutation", TieBreakMode.GENERATE_PERMUTATION.toString()
      );
      String oldTiebreakMode = config.rawConfig.rules.tiebreakMode;
      if (tiebreakModeMigrationMap.containsKey(oldTiebreakMode)) {
        config.rawConfig.rules.tiebreakMode = tiebreakModeMigrationMap.get(oldTiebreakMode);
      } else {
        Logger.warning(
            "tiebreakMode \"%s\" is unrecognized! Please supply a valid tiebreakMode.",
            oldTiebreakMode);
        config.rawConfig.rules.tiebreakMode = null;
      }
    }

    if (config.getOvervoteRule() == OvervoteRule.RULE_UNKNOWN) {
      String oldOvervoteRule = config.rawConfig.rules.overvoteRule;
      switch (oldOvervoteRule) {
        case "alwaysSkipToNextRank" -> config.rawConfig.rules.overvoteRule = OvervoteRule.ALWAYS_SKIP_TO_NEXT_RANK
            .toString();
        case "exhaustImmediately" -> config.rawConfig.rules.overvoteRule = OvervoteRule.EXHAUST_IMMEDIATELY
            .toString();
        case "exhaustIfMultipleContinuing" -> config.rawConfig.rules.overvoteRule = OvervoteRule.EXHAUST_IF_MULTIPLE_CONTINUING
            .toString();
        default -> {
          Logger.warning(
              "overvoteRule \"%s\" is unrecognized! Please supply a valid overvoteRule.",
              oldOvervoteRule);
          config.rawConfig.rules.overvoteRule = null;
        }
      }
    }

    Logger.info(
        "Migrated tabulator config version from %s to %s.",
        config.rawConfig.tabulatorVersion != null ? config.rawConfig.tabulatorVersion : "unknown",
        Main.APP_VERSION);

    config.rawConfig.tabulatorVersion = Main.APP_VERSION;
  }

  static class ConfigVersionIsNewerThanAppVersionException extends Exception {

  }
}
