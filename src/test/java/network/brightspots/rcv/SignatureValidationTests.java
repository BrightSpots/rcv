/*
 * RCTab
 * Copyright (c) 2017-2023 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: These regression tests run various tabulations and compare the generated results to
 * expected results.
 * Design: Passing these tests ensures that changes to code have not altered the results of the
 * tabulation.
 * Conditions: During automated testing.
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import static network.brightspots.rcv.CryptographySignatureValidation.CouldNotVerifySignatureException;
import static network.brightspots.rcv.CryptographySignatureValidation.verifyPublicKeySignature;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SignatureValidationTests {

  // folder where we store test inputs
  private static final String TEST_FOLDER =
      "src/test/resources/network/brightspots/rcv/test_data/signature_validation";
  private static final File DEFAULT_PUBLIC_KEY_FILE = new File(TEST_FOLDER + "/public_key.txt");
  private static final File DEFAULT_SIGNATURE_FILE = new File(TEST_FOLDER + "/test.xml.sig.xml");
  private static final File DEFAULT_DATA_FILE = new File(TEST_FOLDER + "/test.xml");
  private static final File TEMP_DIRECTORY = new File(TEST_FOLDER + "/tmp/");

  @BeforeAll
  static void setup() {
    Logger.setup();
    TEMP_DIRECTORY.mkdirs();
  }

  @AfterAll
  static void teardown() {
    TEMP_DIRECTORY.delete();
  }

  @Test
  @DisplayName("Test RSA signature validation passes")
  void testValidationSucceeds() throws CouldNotVerifySignatureException {
    assertTrue(verifyPublicKeySignature(
            DEFAULT_PUBLIC_KEY_FILE, DEFAULT_SIGNATURE_FILE, DEFAULT_DATA_FILE));
  }

  @Test
  @DisplayName("Failure: incorrect signature")
  void testValidationFailure() throws CouldNotVerifySignatureException {
    File incorrectSignatureFile = new File(TEST_FOLDER + "/incorrect_signature.xml.sig.xml");
    assertFalse(verifyPublicKeySignature(
            DEFAULT_PUBLIC_KEY_FILE, incorrectSignatureFile, DEFAULT_DATA_FILE));
  }

  @Test
  @DisplayName("Exception: modified file")
  void testModifiedDataFile() throws IOException {
    File modifiedDataFile = File.createTempFile("test", ".xml");
    modifiedDataFile.deleteOnExit();
    // Open modified file and write a byte to modify it
    Files.write(modifiedDataFile.toPath(), "not the original data".getBytes());

    Assertions.assertThrows(CouldNotVerifySignatureException.class, () ->
      verifyPublicKeySignature(DEFAULT_PUBLIC_KEY_FILE, DEFAULT_SIGNATURE_FILE, modifiedDataFile)
    );
  }

  @Test
  @DisplayName("Different folder, same filename passes")
  void testSuccessIfFilenameMatches() throws CouldNotVerifySignatureException, IOException {
    File modifiedDataFile = new File(TEMP_DIRECTORY.getAbsolutePath() + "/test.xml");
    modifiedDataFile.deleteOnExit();
    Files.copy(DEFAULT_DATA_FILE.toPath(), modifiedDataFile.toPath());

    assertTrue(verifyPublicKeySignature(
            DEFAULT_PUBLIC_KEY_FILE, DEFAULT_SIGNATURE_FILE, modifiedDataFile));
  }

  @Test
  @DisplayName("Different filename throws an exception")
  void testFailsIfFilenameDiffers() throws IOException {
    File modifiedDataFile = new File(TEMP_DIRECTORY.getAbsolutePath() + "/test2.xml");
    modifiedDataFile.deleteOnExit();
    Files.copy(DEFAULT_DATA_FILE.toPath(), modifiedDataFile.toPath());

    Assertions.assertThrows(CouldNotVerifySignatureException.class, () ->
      verifyPublicKeySignature(DEFAULT_PUBLIC_KEY_FILE, DEFAULT_SIGNATURE_FILE, modifiedDataFile)
    );
  }

  @Test
  @DisplayName("Correct signature but unexpected public key throws an exception")
  void testUnexpectedPublicKey() throws IOException {
    File incorrectPublicKey = new File(TEST_FOLDER + "/incorrect_public_key.txt");
    Assertions.assertThrows(CouldNotVerifySignatureException.class, () ->
            verifyPublicKeySignature(incorrectPublicKey, DEFAULT_SIGNATURE_FILE, DEFAULT_DATA_FILE)
    );
  }
}
