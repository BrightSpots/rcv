/*
 * RCTab
 * Copyright (c) 2017-2023 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: These tests check configuration files.
 * Design: Unit tests, and other tests that don't run a tabulation.
 * Conditions: During automated testing.
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GuiFunctionalityTests {
  @BeforeAll
  static void setup() {
    Logger.setup();
  }

  @Test
  @DisplayName("test autoload when all candidates are present in config")
  void testAutoloadWithCompleteConfig() throws Exception {
    Common.runForEachProvider(configPath -> {
      String configPathStr = configPath.toString();
      ContestConfig config = ContestConfig.loadContestConfig(configPathStr);
      assertTrue(
          GuiConfigController.gatherAutoloadedCandidates(
              config, config.getRawConfig().cvrFileSources).isEmpty(),
          "Expected no unknown candidates in " + configPath.getFileName());
    });
  }

  @Test
  @DisplayName("test autoload when one candidate is missing")
  void testAutoloadWithOneMissingCandidate() throws Exception {
    Common.runForEachProvider(
        configPath -> {
          String configPathStr = configPath.toString();
          ContestConfig config = ContestConfig.loadContestConfig(configPathStr);
          assertSame(1,
              GuiConfigController.gatherAutoloadedCandidates(
                  config, config.getRawConfig().cvrFileSources).size(),
              "Expected one missing candidate to be autoloaded in " + configPath.getFileName());
        },
        config -> {
          config.withArray("candidates").remove(0);
          return config;
        });
  }

  @Test
  @DisplayName("test autoload when candidates list is empty")
  void testAutoloadWithEmptyConfig() throws Exception {
    Common.runForEachProvider(
        configPath -> {
          String configPathStr = configPath.toString();
          ContestConfig config = ContestConfig.loadContestConfig(configPathStr);
          assertSame(4,
              GuiConfigController.gatherAutoloadedCandidates(
                  config, config.getRawConfig().cvrFileSources).size(),
              "Expected candidates to be autoloaded in " + configPath.getFileName());
        },
        config -> {
          config.set("candidates", config.arrayNode());
          return config;
        });
  }
}
