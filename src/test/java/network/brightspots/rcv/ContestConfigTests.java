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

import static network.brightspots.rcv.ContestConfigMigration.isVersionNewer;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ContestConfigTests {
  @BeforeAll
  static void setup() {
    Logger.setup();
  }

  @Test
  @DisplayName("test isVersionNewer")
  void testVersionComparison() {
    // Basic checks: version numbers work fine
    assertFalse(isVersionNewer("1.3.0", "1.3.0"));
    assertFalse(isVersionNewer("1.2.0", "1.3.0"));
    assertFalse(isVersionNewer("1.3.0", "1.3.1"));
    assertTrue(isVersionNewer("1.3.1", "1.3.0"));

    // Version comparison ignores the -alpha specifier when version numbers don't match
    assertFalse(isVersionNewer("1.2.0-alpha", "1.3.0"));
    assertTrue(isVersionNewer("1.4.0-alpha", "1.3.0"));

    // Sane things with false versions
    assertFalse(isVersionNewer("goober", "1.0.0"));
    assertTrue(isVersionNewer("1.0.0", "goober"));

    // Partial versions are parsed correctly and have implicit zeros
    assertTrue(isVersionNewer("1.0.1", "1.0"));
    assertFalse(isVersionNewer("1.0.0", "1.0"));
    assertFalse(isVersionNewer("1.0.0", "1"));
    assertFalse(isVersionNewer("1", "1.0"));

    // Works with snapshot info
    assertTrue(isVersionNewer("1.4.0-beta", "1.4.0-alpha"));
    assertFalse(isVersionNewer("1.4.0-alpha", "1.4.0-beta"));
    assertTrue(isVersionNewer("1.4.0", "1.4.0-alpha"));
    assertFalse(isVersionNewer("1.4.0-alpha", "1.4.0"));
  }
}
