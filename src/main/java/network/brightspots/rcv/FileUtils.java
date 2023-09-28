/*
 * RCTab
 * Copyright (c) 2017-2023 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: Utilities for file and folder creation and management.
 * Implements the notion of a user directory to enable relative paths in config files,
 * as well as utilities to secure and hash files.
 * Design: Mostly just a namespace to organize file-related utilities.
 * Conditions: Always.
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import static network.brightspots.rcv.CryptographyXMLParsers.HartSignature;
import static network.brightspots.rcv.CryptographyXMLParsers.RSAKeyValue;
import static network.brightspots.rcv.Utils.isNullOrBlank;

import java.math.BigInteger;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

final class FileUtils {

  // cache location for finding and creating user files and folders
  private static String userDirectory = null;

  private FileUtils() {}

  // return userDirectory if it exists
  // fallback to current working directory
  static String getUserDirectory() {
    return userDirectory == null ? System.getProperty("user.dir") : userDirectory;
  }

  static void setUserDirectory(String userDirectory) {
    FileUtils.userDirectory = userDirectory;
  }

  static void createOutputDirectory(String dir) throws UnableToCreateDirectoryException {
    if (!isNullOrBlank(dir)) {
      File dirFile = new File(dir);
      if (!dirFile.exists() && !dirFile.mkdirs()) {
        Logger.severe(
            "Failed to create output directory: %s\n" + "Check the directory name and permissions.",
            dir);
        throw new UnableToCreateDirectoryException("Unable to create output directory: " + dir);
      }
    }
  }

  static <T> T readFromXML(File xmlFile, Class<T> classType) throws IOException {
    XmlMapper xmlMapper = new XmlMapper();
    xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    try (FileInputStream inputStream = new FileInputStream(xmlFile)) {
      T xmlObject = xmlMapper.readValue(inputStream, classType);
      inputStream.close();
      return xmlObject;
    }
  }

  static boolean verifyPublicKeySignature(File publicKeyFile, File signatureKeyFile, File dataFile)
          throws CouldNotVerifySignatureException {
    // Load the public key and signature from their corresponding files
    HartSignature hartSignature;
    RSAKeyValue rsaKeyValue;
    try {
      hartSignature = readFromXML(signatureKeyFile, HartSignature.class);
      rsaKeyValue = readFromXML(publicKeyFile, RSAKeyValue.class);
    } catch (IOException e) {
      throw new CouldNotVerifySignatureException("Failed to read files: " + e.getMessage());
    }

    // Sanity check: does the signature file match the known public key file?
    // If not, the file may have been signed with a newer or older version than we support.
    RSAKeyValue rsaFromFile = hartSignature.keyInfo.keyValue.rsaKeyValue;
    if (!rsaFromFile.exponent.equals(rsaKeyValue.exponent)
        || !rsaFromFile.modulus.equals(rsaKeyValue.modulus)) {
      throw new CouldNotVerifySignatureException("%s was signed with a different public key than %s"
              .formatted(signatureKeyFile.getAbsolutePath(), publicKeyFile.getAbsolutePath()));
    }

    // Decode Base64
    byte[] modulusBytes = Base64.getDecoder().decode(rsaKeyValue.modulus);
    byte[] exponentBytes = Base64.getDecoder().decode(rsaKeyValue.exponent);
    byte[] signatureBytes = Base64.getDecoder().decode(hartSignature.signatureValue);

    // Convert byte arrays to BigIntegers
    // Use 1 as the signum to treat the bytes as positive
    BigInteger rsaModulus = new BigInteger(1, modulusBytes);
    BigInteger rsaExponent = new BigInteger(1, exponentBytes);

    // Load the public key and signature algorithms
    PublicKey publicKey;
    Signature signature;
    try {
      RSAPublicKeySpec spec = new RSAPublicKeySpec(rsaModulus, rsaExponent);
      publicKey = KeyFactory.getInstance("RSA").generatePublic(spec);
      signature = Signature.getInstance("SHA256withRSA");
    } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
      throw new CouldNotVerifySignatureException("Failed to load signing algorithms: " + e.getMessage());
    }

    try {
      signature.initVerify(publicKey);
    } catch (InvalidKeyException e) {
      throw new CouldNotVerifySignatureException("Invalid Key: %s" + e.getMessage());
    }

    boolean verified;
    byte[] data;
    try {
      // TODO read 1024 bytes at a time
      data = Files.readAllBytes(dataFile.toPath());
      signature.update(data);
      verified = signature.verify(signatureBytes);
    } catch (SignatureException e) {
      throw new CouldNotVerifySignatureException("Signature failure: %s" + e.getMessage());
    } catch (IOException e) {
      throw new CouldNotVerifySignatureException("Failed to read data file: " + e.getMessage());
    }

    return verified;
  }

  static class UnableToCreateDirectoryException extends Exception {

    UnableToCreateDirectoryException(String message) {
      super(message);
    }
  }

  static class CouldNotVerifySignatureException extends Exception {

    CouldNotVerifySignatureException(String message) {
      super(message);
    }
  }
}
