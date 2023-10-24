/*
 * RCTab
 * Copyright (c) 2017-2023 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: Test the cryptographic signature validation functionality.
 * Design: Passing these tests ensures that cryptographic signature validation checks all
 * the required conditions.
 * Conditions: During automated testing.
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import static network.brightspots.rcv.SecuritySignatureValidation.VerificationDidNotRunException;
import static network.brightspots.rcv.SecuritySignatureValidation.VerificationSignatureDidNotMatchException;
import static network.brightspots.rcv.SecuritySignatureValidation.ensureSignatureIsValid;
import static network.brightspots.rcv.SecurityXmlParsers.RsaKeyValue;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SecurityTests {
  private static final String TEST_ASSET_FOLDER =
          "src/test/resources/network/brightspots/rcv/test_data";
  private static final String TEST_SIG_PREFIX = TEST_ASSET_FOLDER + "/signature_validation/";
  private static final File DEFAULT_PUBLIC_KEY_FILE = new File(TEST_SIG_PREFIX + "public_key.txt");
  private static final File DEFAULT_SIGNATURE_FILE = new File(TEST_SIG_PREFIX + "test.xml.sig.xml");
  private static final File DEFAULT_DATA_FILE = new File(TEST_SIG_PREFIX + "test.xml");
  private static final File TEMP_DIRECTORY = new File(TEST_SIG_PREFIX + "tmp/");

  @BeforeAll
  static void setup() {
    Logger.setup();
    boolean didCreate = TEMP_DIRECTORY.mkdirs();
    if (!didCreate) {
      // Maybe it already existed, maybe it couldn't be created
      Logger.warning("Failed to create directory: %s", TEMP_DIRECTORY.getAbsolutePath());
    }
  }

  @AfterAll
  static void teardown() {
    boolean didDelete = TEMP_DIRECTORY.delete();
    if (!didDelete) {
      Logger.warning("Failed to clean up directory: %s", TEMP_DIRECTORY.getAbsolutePath());
    }
  }

  static RsaKeyValue getPublicKeyFor(File file) {
    XmlMapper xmlMapper = new XmlMapper();
    xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    try (FileInputStream inputStream = new FileInputStream(file)) {
      return xmlMapper.readValue(inputStream, RsaKeyValue.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  static RsaKeyValue defaultPublicKey() {
    return getPublicKeyFor(DEFAULT_PUBLIC_KEY_FILE);
  }


  @Test
  @DisplayName("Succeeds using the default, valid files")
  void testValidationSucceeds() throws
          VerificationDidNotRunException, VerificationSignatureDidNotMatchException {
    ensureSignatureIsValid(defaultPublicKey(), DEFAULT_SIGNATURE_FILE, DEFAULT_DATA_FILE);
  }

  @Test
  @DisplayName("Verification fails when the signature is incorrect")
  void testValidationFailure() {
    File incorrectSignatureFile = new File(TEST_SIG_PREFIX
            + "/incorrect_signature.xml.sig.xml");
    Assertions.assertThrows(VerificationSignatureDidNotMatchException.class, () ->
            ensureSignatureIsValid(
                    defaultPublicKey(), incorrectSignatureFile, DEFAULT_DATA_FILE));
  }

  @Test
  @DisplayName("Exception is thrown when the data file is modified")
  void testModifiedDataFile() throws IOException {
    File modifiedDataFile = File.createTempFile("test", ".xml");
    modifiedDataFile.deleteOnExit();

    Files.writeString(modifiedDataFile.toPath(), "not the original data");

    Assertions.assertThrows(VerificationDidNotRunException.class, () ->
        ensureSignatureIsValid(defaultPublicKey(), DEFAULT_SIGNATURE_FILE, modifiedDataFile)
    );
  }

  @Test
  @DisplayName("Succeeds when data file is in a different folder (but has the right filename)")
  void testSuccessIfFilenameMatches() throws
          VerificationDidNotRunException, VerificationSignatureDidNotMatchException, IOException {
    File modifiedDataFile = new File(TEMP_DIRECTORY.getAbsolutePath() + "/test.xml");
    modifiedDataFile.deleteOnExit();
    Files.copy(DEFAULT_DATA_FILE.toPath(), modifiedDataFile.toPath());

    ensureSignatureIsValid(defaultPublicKey(), DEFAULT_SIGNATURE_FILE, modifiedDataFile);
  }

  @Test
  @DisplayName("Exception is thrown when the filenames differ")
  void testFailsIfFilenameDiffers() throws IOException {
    File modifiedDataFile = new File(TEMP_DIRECTORY.getAbsolutePath() + "/test2.xml");
    modifiedDataFile.deleteOnExit();
    Files.copy(DEFAULT_DATA_FILE.toPath(), modifiedDataFile.toPath());

    Assertions.assertThrows(VerificationDidNotRunException.class, () ->
        ensureSignatureIsValid(defaultPublicKey(), DEFAULT_SIGNATURE_FILE, modifiedDataFile)
    );
  }

  @Test
  @DisplayName("Exception is thrown when the file is signed with an unsupported key")
  void testUnexpectedPublicKey() {
    File incorrectPublicKey = new File(TEST_SIG_PREFIX + "/incorrect_public_key.txt");
    Assertions.assertThrows(VerificationDidNotRunException.class, () -> ensureSignatureIsValid(
            getPublicKeyFor(incorrectPublicKey),
            DEFAULT_SIGNATURE_FILE,
            DEFAULT_DATA_FILE)
    );
  }

  @Test
  @DisplayName("Users may not save output files to their User directory")
  void testFailureOnInvalidOutputDirectory() {
    String configPath = TEST_ASSET_FOLDER + "/invalid_output_directory/config.json";
    ContestConfig config = ContestConfig.loadContestConfig(configPath);
    Set<ContestConfig.ValidationError> errors = config.validate();
    Assertions.assertArrayEquals(new ContestConfig.ValidationError[]{
        ContestConfig.ValidationError.OUTPUT_NOT_ALLOWED_IN_USER_DIRECTORY
    }, errors.toArray());
  }
}
