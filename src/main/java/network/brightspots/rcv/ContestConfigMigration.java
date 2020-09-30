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

import static network.brightspots.rcv.Utils.isNullOrBlank;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import network.brightspots.rcv.RawContestConfig.ContestRules;
import network.brightspots.rcv.RawContestConfig.CvrSource;
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
          isNewer = false;
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
    boolean needsMigration = version == null ||
        (!version.equals(Main.APP_VERSION) && !version
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
          case "multiSeatBottomsUp" -> rules.winnerElectionMode =
              config.getNumberOfWinners() == 0
                  || config.getMultiSeatBottomsUpPercentageThreshold() != null
                  ? WinnerElectionMode.MULTI_SEAT_BOTTOMS_UP_USING_PERCENTAGE_THRESHOLD
                  .getInternalLabel()
                  : WinnerElectionMode.MULTI_SEAT_BOTTOMS_UP_UNTIL_N_WINNERS.getInternalLabel();
          default -> {
            Logger.warning(
                "winnerElectionMode \"%s\" is unrecognized! Please supply a valid "
                    + "winnerElectionMode.", oldWinnerElectionMode);
            rules.winnerElectionMode = null;
          }
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
