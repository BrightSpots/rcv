/*
 * RCTab
 * Copyright (c) 2017-2026 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: Shared utilities for tabulator test classes.
 * Design: Provides helpers for generating and managing test config files.
 * Conditions: During automated testing.
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.UnaryOperator;

class Common {

  static final String TEST_ASSET_FOLDER =
      "src/test/resources/network/brightspots/rcv/test_data";

  @FunctionalInterface
  interface ConfigConsumer {
    void accept(Path configPath) throws Exception;
  }

  /**
   * Calls the consumer on a config file for each supported provider type.
   * Each returned config file uses a different provider, but the data in each CVR is the same,
   * and the results of tabulation (and nearly every other operation) should be equivalent.
   * Under the hood, this generates temporary config files and manages their lifecycle.
   */
  static void runForEachProvider(ConfigConsumer consumer) throws Exception {
    runForEachProvider(consumer, UnaryOperator.identity());
  }

  /**
   * Like {@link #runForEachProvider(ConfigConsumer)}, but applies {@code configModifier} to the
   * merged config JSON before writing it to disk. Use this to adjust the config for tests that
   * need a non-default starting state (e.g. removing all candidates to test autoload).
   */
  static void runForEachProvider(
      ConfigConsumer consumer, UnaryOperator<ObjectNode> configModifier) throws Exception {
    final String[] stems = {"ess", "cdf"};

    Path sharedDir = Paths.get(System.getProperty("user.dir"),
        TEST_ASSET_FOLDER, "_shared", "same_data_different_formats");

    ObjectMapper mapper = new ObjectMapper();
    JsonNode baseConfig = mapper.readTree(sharedDir.resolve("base_config.json").toFile());

    for (String stem : stems) {
      File stemConfigFile = sharedDir.resolve(stem + "_config.json").toFile();
      JsonNode stemSources = mapper.readTree(stemConfigFile);

      ObjectNode mergedConfig = baseConfig.deepCopy();
      mergedConfig.set("cvrFileSources", stemSources);
      mergedConfig = configModifier.apply(mergedConfig);

      Path tempConfig = sharedDir.resolve(stem + "_combined_config.json");
      mapper.writeValue(tempConfig.toFile(), mergedConfig);
      try {
        consumer.accept(tempConfig);
      } finally {
        Files.deleteIfExists(tempConfig);
      }
    }
  }
}
