/*
 * RCTab
 * Copyright (c) 2017-2023 Bright Spots Developers.
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

import static com.fasterxml.jackson.core.util.VersionUtil.parseVersion;
import static network.brightspots.rcv.Utils.isNullOrBlank;

import com.fasterxml.jackson.core.Version;
import java.util.Map;
import network.brightspots.rcv.RawContestConfig.ContestRules;
import network.brightspots.rcv.RawContestConfig.CvrSource;
import network.brightspots.rcv.Tabulator.TiebreakMode;
import network.brightspots.rcv.Tabulator.WinnerElectionMode;
import org.apache.poi.util.StringUtil;

final class ContestConfigMigration {
  private ContestConfigMigration() {}

  // not intended to be used if either version is null
  public static boolean isVersionNewer(String version1, String version2) {
    Version version1Parsed = parseVersion(version1, null, null);
    Version version2Parsed = parseVersion(version2, null, null);
    return version1Parsed.compareTo(version2Parsed) > 0;
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
    String originalVersion = config.rawConfig.tabulatorVersion;
    boolean needsMigration =
            originalVersion == null
                    || (!originalVersion.equals(Main.APP_VERSION));
    if (!needsMigration) {
      return;
    }

    if (isConfigVersionNewerThanAppVersion(originalVersion)) {
      throw new ConfigVersionIsNewerThanAppVersionException();
    }

    if (originalVersion == null || isVersionNewer("2.0.2", originalVersion)) {
      migrateUnversionedTo202(config);
    }

    if (isVersionNewer("2.1.0", originalVersion)) {
      migrate201To210(config);
    }

    // Always bump to the current app version, even if there is no change in config fields.
    if (!Main.APP_VERSION.equals(config.rawConfig.tabulatorVersion)) {
      config.rawConfig.tabulatorVersion = Main.APP_VERSION;
    }

    Logger.info(
            "Migrated tabulator config version from %s to %s.",
            originalVersion != null ? originalVersion : "unknown",
            config.rawConfig.tabulatorVersion);
  }

  private static void migrate201To210(ContestConfig config) {
    RawContestConfig rawConfig = config.getRawConfig();

    for (CvrSource source : rawConfig.cvrFileSources) {
      if (StringUtil.isNotBlank(source.getSkippedRankLabel()) && ContestConfig.Provider.ESS
              == ContestConfig.Provider.getByInternalLabel(source.getProvider())) {
        Logger.warning("ES&S no longer supports custom undervote labels. Ignoring.");
        source.setSkippedRankLabel(null);
      }
    }

    config.rawConfig.tabulatorVersion = "2.1.0";
  }

  private static void migrateUnversionedTo202(ContestConfig config) {
    RawContestConfig rawConfig = config.getRawConfig();
    ContestRules rules = rawConfig.rules;

    if (config.getWinnerElectionMode() == WinnerElectionMode.MODE_UNKNOWN) {
      String oldWinnerElectionMode = rules.winnerElectionMode;
      if (oldWinnerElectionMode == null) {
        Logger.severe("winnerElectionMode is required but was not found in the config! "
                + "Supply a valid winnerElectionMode.");
        return;
      }
      switch (oldWinnerElectionMode) {
        case "standard" -> rules.winnerElectionMode =
                config.getNumberOfWinners() > 1
                        ? WinnerElectionMode.MULTI_SEAT_ALLOW_MULTIPLE_WINNERS_PER_ROUND
                        .getInternalLabel()
                        : WinnerElectionMode.STANDARD_SINGLE_WINNER.getInternalLabel();
        case "singleSeatContinueUntilTwoCandidatesRemain" -> {
          rules.winnerElectionMode = WinnerElectionMode.STANDARD_SINGLE_WINNER.getInternalLabel();
          rules.continueUntilTwoCandidatesRemain = true;
        }
        case "multiSeatAllowOnlyOneWinnerPerRound" -> rules.winnerElectionMode =
                WinnerElectionMode.MULTI_SEAT_ALLOW_ONLY_ONE_WINNER_PER_ROUND.getInternalLabel();
        case "multiSeatBottomsUp" -> rules.winnerElectionMode =
                config.getNumberOfWinners() == 0
                        || config.getMultiSeatBottomsUpPercentageThreshold() != null
                        ? WinnerElectionMode.MULTI_SEAT_BOTTOMS_UP_USING_PERCENTAGE_THRESHOLD
                        .getInternalLabel()
                        : WinnerElectionMode.MULTI_SEAT_BOTTOMS_UP_UNTIL_N_WINNERS
                        .getInternalLabel();
        case "multiSeatSequentialWinnerTakesAll" -> rules.winnerElectionMode =
                WinnerElectionMode.MULTI_SEAT_SEQUENTIAL_WINNER_TAKES_ALL.getInternalLabel();
        default -> {
          Logger.warning(
                  "winnerElectionMode \"%s\" is unrecognized! Supply a valid "
                          + "winnerElectionMode.",
                  oldWinnerElectionMode);
          rules.winnerElectionMode = null;
        }
      }
    }

    if (config.getTiebreakMode() == TiebreakMode.MODE_UNKNOWN) {
      Map<String, TiebreakMode> tiebreakModeMigrationMap =
              Map.of(
                      "random",
                      TiebreakMode.RANDOM,
                      "interactive",
                      TiebreakMode.INTERACTIVE,
                      "previousRoundCountsThenRandom",
                      TiebreakMode.PREVIOUS_ROUND_COUNTS_THEN_RANDOM,
                      "previousRoundCountsThenInteractive",
                      TiebreakMode.PREVIOUS_ROUND_COUNTS_THEN_INTERACTIVE,
                      "usePermutationInConfig",
                      TiebreakMode.USE_PERMUTATION_IN_CONFIG,
                      "generatePermutation",
                      TiebreakMode.GENERATE_PERMUTATION);
      String oldTiebreakMode = rules.tiebreakMode;
      if (tiebreakModeMigrationMap.containsKey(oldTiebreakMode)) {
        rules.tiebreakMode = tiebreakModeMigrationMap.get(oldTiebreakMode).getInternalLabel();
      } else {
        Logger.warning("tiebreakMode \"%s\" is unrecognized! Supply a valid tiebreakMode.",
            oldTiebreakMode);
        rules.tiebreakMode = null;
      }
    }

    // These fields were previously at the config level, but are now set on a per-source basis.
    // They used to include undervoteLabel and treatBlankAsUndeclaredWriteIn, both of which were
    // removed in 2.1.0, so by ignoring them we effectively delete them.
    // That means the output of this function is not exactly compatible with 2.0.2, but rather,
    // a correct stepping stone while migrating up to the current app version.

    if (!isNullOrBlank(rules.overvoteLabel)) {
      for (CvrSource source : rawConfig.cvrFileSources) {
        source.setOvervoteLabel(rules.overvoteLabel);
      }
    }

    if (!isNullOrBlank(rules.undeclaredWriteInLabel)) {
      for (CvrSource source : rawConfig.cvrFileSources) {
        source.setUndeclaredWriteInLabel(rules.undeclaredWriteInLabel);
      }
    }

    // Migrations from 1.3.0 to 1.4.0
    if (rules.stopTabulationEarlyAfterRound == null) {
      rules.stopTabulationEarlyAfterRound = "";
    }

    config.rawConfig.tabulatorVersion = "2.0.2";
  }

  static class ConfigVersionIsNewerThanAppVersionException extends Exception {}
}
