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

import java.util.Map;
import java.util.logging.Level;
import network.brightspots.rcv.Tabulator.OvervoteRule;
import network.brightspots.rcv.Tabulator.TieBreakMode;
import network.brightspots.rcv.Tabulator.WinnerElectionMode;

class ContestConfigMigration {
  static void migrateConfigVersion(ContestConfig config) {
    if (config.rawConfig.tabulatorVersion == null
        || !config.rawConfig.tabulatorVersion.equals(Main.APP_VERSION)) {
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
            Logger.log(Level.WARNING,
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
          Logger.log(Level.WARNING,
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
            Logger.log(Level.WARNING,
                "overvoteRule \"%s\" is unrecognized! Please supply a valid overvoteRule.",
                oldOvervoteRule);
            config.rawConfig.rules.overvoteRule = null;
          }
        }
      }

      Logger.log(
          Level.INFO,
          "Migrated tabulator config version from %s to %s.",
          config.rawConfig.tabulatorVersion != null ? config.rawConfig.tabulatorVersion : "unknown",
          Main.APP_VERSION);
      config.rawConfig.tabulatorVersion = Main.APP_VERSION;
    }
  }
}
