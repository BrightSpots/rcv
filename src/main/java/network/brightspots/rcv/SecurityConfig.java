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

import java.security.NoSuchAlgorithmException;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;

class SecurityConfig {
  // Only the unit test modules should ever set this to false, if it is initially set as true.
  // Note: On some builds, this will be configured to false by default. We will need some
  // formalized method of toggling this for two versions of builds, which has yet to be determined.
  private static boolean IS_HART_SIGNATURE_VALIDATION_ENABLED = true;

  // Is the user allowed to save output files to their Users directory?
  // Since user accounts retain delete and create permissions to their user account folders,
  // this should be disallowed to truly ensure output files are read-only.
  private static boolean CAN_OUTPUT_FILES_SAVE_TO_USERS_DIRECTORY = false;

  // The base64-encoded RSA public key modulus
  private static final String RSA_MODULUS =
      "vifu/KSlTnBHOtl0IuHEc1R3A4sH1vKCKU9G/8/LtD6Ih5aWq7SuyuGYgIUUzErmFC92kv4chXKBFwti5wSfoHqt"
          + "TpmlAvlIsLvi4zrllaoewShzUCG/sqAH3Zw4JBOb6wk20064bkiejXbaxEZticxgs08ZU9bHmpWrlldbIJmgt3"
          + "gOjhG68+ATfShREpuLeAV9lGU2+Y6OdKtUHVewSeyIfz3+Rpajc/f2UPtO6uA09FzmzryWhhtZHiXpev2oVWCp"
          + "T1MH5JVgrWykX9fWcxJMTHdvZnMxYzJF6ox3vGtx6x8Eib8H4RT4KutWphPPT3vLDkhYXP9TAP+B1edxwQ==";

  // The base64-encoded RSA public key exponent
  private static final String RSA_EXPONENT = "AQAB";

  // The RsaKeyValue is lazily-initialized. It is set on the first call to getRsaPublicKey().
  private static RsaKeyValue rsaKeyValue = null;

  private static final String expectedSecureRandomProvider = "SUN";

  private static final String expectedXmlCanonicalizationProvider = "XMLDSig";

  static {
    try {
      setupAndVerifyFipsCompliance();
    } catch (SecurityConfigurationException e) {
      throw new RuntimeException(e);
    }
  }

  public static boolean isHartSignatureValidationEnabled() {
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

  private static void setupAndVerifyFipsCompliance() throws SecurityConfigurationException {
    // First, verify that the SecureRandom provider is what we expect.
    try {
      String randomProviderName =
            java.security.SecureRandom.getInstanceStrong().getProvider().getName();
      if (!randomProviderName.equals(expectedSecureRandomProvider)) {
        Logger.warning("The SecureRandom provider is " + randomProviderName);
      }
    } catch (NoSuchAlgorithmException e) {
      throw new SecurityConfigurationException("No SecureRandom algorithm found.");
    }

    java.security.Security.insertProviderAt(new BouncyCastleFipsProvider(), 1);
    // Remove all providers except for the SecureRandom provider and the XMLDSig provider.
    // XMLDsig is only used for canonicalizaton, not for anything related to cryptography.
    java.security.Provider[] providers = java.security.Security.getProviders();
    int numSecureRandomProviders = 0;
    int numXmlCanonicalizationProviders = 0;
    for (java.security.Provider provider : providers) {
      String name = provider.getName();
      if (name.equals(expectedSecureRandomProvider)) {
        numSecureRandomProviders++;
      } else if (name.equals(expectedXmlCanonicalizationProvider)) {
        numXmlCanonicalizationProviders++;
      } else {
        java.security.Security.removeProvider(name);
      }
    }

    if (numSecureRandomProviders != 1) {
      throw new SecurityConfigurationException(
          "Must have exactly one SecureRandom provider, but found "
          + numSecureRandomProviders);
    }
    if (numXmlCanonicalizationProviders != 1) {
      throw new SecurityConfigurationException(
          "Must have exactly one XML Canonicalization provider, but found "
          + numXmlCanonicalizationProviders);
    }

    // Note: position 1 is "most preferred", not 0
    java.security.Security.insertProviderAt(new BouncyCastleFipsProvider(), 1);

    Logger.info("Security Providers: ");
    for (java.security.Provider provider : java.security.Security.getProviders()) {
      Logger.info(provider.getName() + "\n\t" + provider.getInfo());
    }
  }

  /*
   * Sanity check to ensure setupAndVerifyFipsCompliance has been run.
   */
  public static boolean haveProvidersBeenCulled() {
    return java.security.Security.getProviders().length == 3;
  }

  static class SecurityConfigurationException extends Exception {
    SecurityConfigurationException(String message) {
      super(message);
    }
  }
}
