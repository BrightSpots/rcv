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
import static network.brightspots.rcv.CryptographyXmlParsers.RsaKeyValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    assertTrue(TEMP_DIRECTORY.mkdirs());
  }

  @AfterAll
  static void teardown() {
    boolean didDelete = TEMP_DIRECTORY.delete();
    if (!didDelete) {
      Logger.warning("Failed to clean up directory: %s", TEMP_DIRECTORY.getAbsolutePath());
    }
  }

  static RsaKeyValue getPublicKeyFor(File file) {
    try {
      return CryptographySignatureValidation.readFromXml(file, RsaKeyValue.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  static RsaKeyValue defaultPublicKey() {
    return getPublicKeyFor(DEFAULT_PUBLIC_KEY_FILE);
  }


  @Test
  @DisplayName("Success: default files")
  void testValidationSucceeds() throws CouldNotVerifySignatureException {
    assertTrue(verifyPublicKeySignature(
            defaultPublicKey(), DEFAULT_SIGNATURE_FILE, DEFAULT_DATA_FILE));
  }

  @Test
  @DisplayName("Failure: incorrect signature")
  void testValidationFailure() throws CouldNotVerifySignatureException {
    File incorrectSignatureFile = new File(TEST_FOLDER + "/incorrect_signature.xml.sig.xml");
    assertFalse(verifyPublicKeySignature(
            defaultPublicKey(), incorrectSignatureFile, DEFAULT_DATA_FILE));
  }

  @Test
  @DisplayName("Exception: modified file")
  void testModifiedDataFile() throws IOException {
    File modifiedDataFile = File.createTempFile("test", ".xml");
    modifiedDataFile.deleteOnExit();

    Files.writeString(modifiedDataFile.toPath(), "not the original data");

    Assertions.assertThrows(CouldNotVerifySignatureException.class, () ->
        verifyPublicKeySignature(defaultPublicKey(), DEFAULT_SIGNATURE_FILE, modifiedDataFile)
    );
  }

  @Test
  @DisplayName("Success: Different folder, same filename")
  void testSuccessIfFilenameMatches() throws CouldNotVerifySignatureException, IOException {
    File modifiedDataFile = new File(TEMP_DIRECTORY.getAbsolutePath() + "/test.xml");
    modifiedDataFile.deleteOnExit();
    Files.copy(DEFAULT_DATA_FILE.toPath(), modifiedDataFile.toPath());

    assertTrue(verifyPublicKeySignature(
            defaultPublicKey(), DEFAULT_SIGNATURE_FILE, modifiedDataFile));
  }

  @Test
  @DisplayName("Exception: Different filename")
  void testFailsIfFilenameDiffers() throws IOException {
    File modifiedDataFile = new File(TEMP_DIRECTORY.getAbsolutePath() + "/test2.xml");
    modifiedDataFile.deleteOnExit();
    Files.copy(DEFAULT_DATA_FILE.toPath(), modifiedDataFile.toPath());

    Assertions.assertThrows(CouldNotVerifySignatureException.class, () ->
        verifyPublicKeySignature(defaultPublicKey(), DEFAULT_SIGNATURE_FILE, modifiedDataFile)
    );
  }

  @Test
  @DisplayName("Exception: Correct signature but unexpected public key")
  void testUnexpectedPublicKey() {
    File incorrectPublicKey = new File(TEST_FOLDER + "/incorrect_public_key.txt");
    Assertions.assertThrows(CouldNotVerifySignatureException.class, () -> verifyPublicKeySignature(
            getPublicKeyFor(incorrectPublicKey),
            DEFAULT_SIGNATURE_FILE,
            DEFAULT_DATA_FILE)
    );
  }
}
