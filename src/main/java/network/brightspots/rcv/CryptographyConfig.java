/*
 * RCTab
 * Copyright (c) 2017-2023 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: Configuration for the cryptographic signing of a Hart file.
 * Design: A set of read-only properties.
 * Conditions: When verifying the signature of a Hart file.
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import static network.brightspots.rcv.CryptographyXmlParsers.RsaKeyValue;

class CryptographyConfig {
  private static boolean IS_HART_SIGNATURE_VALIDATION_ENABLED = true;

  private static final String RSA_MODULUS = "vifu/KSlTnBHOtl0IuHEc1R3A4sH1vKCKU9G/8/LtD6Ih5aWq7Suyu"
        + "GYgIUUzErmFC92kv4chXKBFwti5wSfoHqtTpmlAvlIsLvi4zrllaoewShzUCG/sqAH3Zw4JBOb6wk20064bkiejX"
        + "baxEZticxgs08ZU9bHmpWrlldbIJmgt3gOjhG68+ATfShREpuLeAV9lGU2+Y6OdKtUHVewSeyIfz3+Rpajc/f2UP"
        + "tO6uA09FzmzryWhhtZHiXpev2oVWCpT1MH5JVgrWykX9fWcxJMTHdvZnMxYzJF6ox3vGtx6x8Eib8H4RT4KutWph"
        + "PPT3vLDkhYXP9TAP+B1edxwQ==";
  private static final String RSA_EXPONENT = "AQAB";

  private static RsaKeyValue rsaKeyValue = null;

  public static boolean isIsHartSignatureValidationEnabled() {
    return IS_HART_SIGNATURE_VALIDATION_ENABLED;
  }

  public static RsaKeyValue getRsaPublicKey() {
    if (rsaKeyValue != null) {
      return rsaKeyValue;
    }
    rsaKeyValue = new RsaKeyValue();
    rsaKeyValue.modulus = RSA_MODULUS;
    rsaKeyValue.exponent = RSA_EXPONENT;
    return rsaKeyValue;
  }

  public static void disableValidationForUnitTests() {
    // Do some basic sanity checking to make sure this is never accidentally called
    // outside of the TabulatorTests.
    StackTraceElement[] currentStack = Thread.currentThread().getStackTrace();
    StackTraceElement lastStackFrame = currentStack[2];
    if (!lastStackFrame.getClassName().equals("network.brightspots.rcv.TabulatorTests")) {
      throw new RuntimeException("Only unit tests can disable validation. Expected to be "
              + "called from TabulatorTests, but instead got " + lastStackFrame.getClassName());
    }

    IS_HART_SIGNATURE_VALIDATION_ENABLED = false;
  }
}
