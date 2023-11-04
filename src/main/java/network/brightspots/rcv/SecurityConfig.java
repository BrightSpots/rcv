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

import static network.brightspots.rcv.SecurityXmlParsers.RsaKeyValue;

class SecurityConfig {
  // Only the unit test modules should ever set this to false, if it is initially set as true.
  // Note: On some builds, this will be configured to false by default. We will need some
  // formalized method of toggling this for two versions of builds, which has yet to be determined.
  private static boolean IS_HART_SIGNATURE_VALIDATION_ENABLED = false;

  // Is the user allowed to save output files to their Users directory?
  // Since user accounts retain delete and create permissions to their user account folders,
  // this should be disallowed to truly ensure output files are read-only.
  private static boolean CAN_OUTPUT_FILES_SAVE_TO_USERS_DIRECTORY = true;

  // The base64-encoded RSA public key modulus
  private static final String RSA_MODULUS = "vifu/KSlTnBHOtl0IuHEc1R3A4sH1vKCKU9G/8/LtD6Ih5aWq7Suyu"
        + "GYgIUUzErmFC92kv4chXKBFwti5wSfoHqtTpmlAvlIsLvi4zrllaoewShzUCG/sqAH3Zw4JBOb6wk20064bkiejX"
        + "baxEZticxgs08ZU9bHmpWrlldbIJmgt3gOjhG68+ATfShREpuLeAV9lGU2+Y6OdKtUHVewSeyIfz3+Rpajc/f2UP"
        + "tO6uA09FzmzryWhhtZHiXpev2oVWCpT1MH5JVgrWykX9fWcxJMTHdvZnMxYzJF6ox3vGtx6x8Eib8H4RT4KutWph"
        + "PPT3vLDkhYXP9TAP+B1edxwQ==";

  // The base64-encoded RSA public key exponent
  private static final String RSA_EXPONENT = "AQAB";

  // The RsaKeyValue is lazily-initialized. It is set on the first call to getRsaPublicKey().
  private static RsaKeyValue rsaKeyValue = null;

  public static boolean isIsHartSignatureValidationEnabled() {
    return IS_HART_SIGNATURE_VALIDATION_ENABLED;
  }

  public static boolean canOutputFilesSaveToUsersDirectory() {
    return CAN_OUTPUT_FILES_SAVE_TO_USERS_DIRECTORY;
  }

  // Synchronized to prevent a race condition. SpotBugs will complain otherwise, even though
  // this is not currently called on multiple threads.
  public static synchronized RsaKeyValue getRsaPublicKey() {
    if (rsaKeyValue == null) {
      rsaKeyValue = new RsaKeyValue();
      rsaKeyValue.modulus = RSA_MODULUS;
      rsaKeyValue.exponent = RSA_EXPONENT;
    }

    return rsaKeyValue;
  }

  public static void setEnableValidationForUnitTests(boolean isEnabled) {
    if (isNotCalledByTabulatorTests()) {
      throw new RuntimeException("Only unit tests can edit the security configuration!");
    }

    IS_HART_SIGNATURE_VALIDATION_ENABLED = isEnabled;
  }

  public static void setAllowUsersDirectorySavingForUnitTests(boolean isAllowed) {
    if (isNotCalledByTabulatorTests()) {
      throw new RuntimeException("Only unit tests can edit the security configuration!");
    }

    CAN_OUTPUT_FILES_SAVE_TO_USERS_DIRECTORY = isAllowed;
  }

  private static boolean isNotCalledByTabulatorTests() {
    // Do some basic sanity checking to make sure this is never accidentally called
    // from outside the expected test module.
    StackTraceElement[] currentStack = Thread.currentThread().getStackTrace();
    StackTraceElement lastStackFrame = currentStack[3];
    return !lastStackFrame.getClassName().equals("network.brightspots.rcv.TabulatorTests")
            && !lastStackFrame.getClassName().equals("network.brightspots.rcv.SecurityTests");
  }
}
