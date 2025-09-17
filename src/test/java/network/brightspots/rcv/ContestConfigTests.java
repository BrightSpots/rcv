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

import static network.brightspots.rcv.ContestConfig.getPercentageFromStringWithAccurateSigFigs;
import static network.brightspots.rcv.ContestConfig.numDecimalsInString;
import static network.brightspots.rcv.ContestConfigMigration.isVersionNewer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
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

  void assertNumDecimalsTestsFails(String input) {
    try {
      numDecimalsInString(input);
      throw new AssertionError("Expected IllegalArgumentException for '" + input + "'");
    } catch (IllegalArgumentException e) {
      // Expected exception
    }
  }

  @Test
  @DisplayName("test numDecimalsInString")
  void testNumDecimalsInString() {
    // This test ensures that the automatic precision that occurs when setting
    // the bottoms-up threshold is accurate
    assertEquals(1, numDecimalsInString("0.1"));
    assertEquals(2, numDecimalsInString("0.15"));
    assertEquals(2, numDecimalsInString("0.10"));
    assertEquals(3, numDecimalsInString("0.100"));
    assertEquals(4, numDecimalsInString("0.0100"));
    assertEquals(0, numDecimalsInString("1"));
    assertEquals(0, numDecimalsInString("10"));
    assertEquals(0, numDecimalsInString("100"));
    assertEquals(1, numDecimalsInString("1.0"));
    assertEquals(2, numDecimalsInString("1.01"));
    assertEquals(2, numDecimalsInString("50.50"));
    assertEquals(0, numDecimalsInString("100"));
    assertEquals(0, numDecimalsInString("100."));
    assertEquals(1, numDecimalsInString("100.0"));
    assertNumDecimalsTestsFails("");
    assertNumDecimalsTestsFails("100..");
    assertNumDecimalsTestsFails("0.00100.0");
    assertNumDecimalsTestsFails(null);
    assertNumDecimalsTestsFails("g.g");
    assertEquals(new BigDecimal("0.010"),
          getPercentageFromStringWithAccurateSigFigs("1.0"));
    assertEquals(new BigDecimal("0.10"),
            getPercentageFromStringWithAccurateSigFigs("10"));
    assertEquals(new BigDecimal("0.15"),
            getPercentageFromStringWithAccurateSigFigs("15"));
    assertEquals(new BigDecimal("1.00"),
            getPercentageFromStringWithAccurateSigFigs("100"));
    assertEquals(new BigDecimal("0.9999"),
            getPercentageFromStringWithAccurateSigFigs("99.99"));
    assertEquals(new BigDecimal("0.009999"),
            getPercentageFromStringWithAccurateSigFigs(".9999"));

  }
}
