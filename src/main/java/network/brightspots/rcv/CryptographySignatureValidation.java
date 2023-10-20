/*
 * RCTab
 * Copyright (c) 2017-2023 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: Java static class representing the crpytographic signing of a Hart file.
 * Design: a Jackson XML object that can be serialized to and from XML.
 * Conditions: When verifying the signature of a Hart file.
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import static network.brightspots.rcv.CryptographyXmlParsers.HartSignature;
import static network.brightspots.rcv.CryptographyXmlParsers.RsaKeyValue;
import static network.brightspots.rcv.CryptographyXmlParsers.SignedInfo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import javax.xml.crypto.OctetStreamData;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.TransformException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;

public class CryptographySignatureValidation {
  /**
   * This function returns whether the signature matches validation.
   * It throws an error if it is unable to perform this check, for reasons including:
   * 1. The public key in the signed file does not match the expected key
   * 2. The canonicalization or hashing algorithm is not supported
   * 3. The corresponding file has been edited and the hash no longer matches
   */
  public static boolean verifyPublicKeySignature(
          File publicKeyFile,
          File signatureKeyFile,
          File dataFile) throws CouldNotVerifySignatureException {
    // Load the public key and signature from their corresponding files
    HartSignature hartSignature;
    CryptographyXmlParsers.RsaKeyValue rsaKeyValue;
    try {
      hartSignature = readFromXml(signatureKeyFile, HartSignature.class);
      rsaKeyValue = readFromXml(publicKeyFile, RsaKeyValue.class);
    } catch (IOException e) {
      throw new CouldNotVerifySignatureException("Failed to read files: " + e.getMessage());
    }

    ensurePublicKeyMatchesExpectedValue(hartSignature, rsaKeyValue,
            signatureKeyFile, publicKeyFile);
    ensureDigestMatchesExpectedValue(dataFile, hartSignature);

    return checkSignature(hartSignature, rsaKeyValue);
  }

  private static void ensureDigestMatchesExpectedValue(File dataFile, HartSignature hartSignature)
          throws CouldNotVerifySignatureException {
    // Check if the filenames match
    String actualFilename = dataFile.getName();
    String expectedFilename = new File(hartSignature.signedInfo.reference.uri).getName();
    if (!actualFilename.equals(expectedFilename)) {
      throw new CouldNotVerifySignatureException(
              "Signed file was %s but you provided %s".formatted(actualFilename, expectedFilename));
    }

    String actualDigest = tempMergeConflictGetHash(dataFile, "SHA-256");
    String expectedDigest = hartSignature.signedInfo.reference.digestValue;

    if (!actualDigest.equals(expectedDigest)) {
      throw new CouldNotVerifySignatureException(
              "Signed file had digest %s but the file you provided had %s"
              .formatted(expectedDigest, actualDigest));
    }
  }

  private static String tempMergeConflictGetHash(File file, String algorithm) {
    // TODO delete this function once develop is merged in
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance(algorithm);
    } catch (NoSuchAlgorithmException e) {
      Logger.severe("Failed to get algorithm " + algorithm);
      return "[hash not available]";
    }

    try (InputStream is = Files.newInputStream(file.toPath())) {
      try (DigestInputStream hashingStream = new DigestInputStream(is, digest)) {
        while (hashingStream.readNBytes(1024).length > 0) {
          // Read in 1kb chunks -- don't need to do anything in the body here
        }
      }
    } catch (IOException e) {
      Logger.severe("Failed to read file: %s", file.getAbsolutePath());
      return "[hash not available]";
    }

    return Base64.getEncoder().encodeToString(digest.digest());
  }

  private static void ensurePublicKeyMatchesExpectedValue(
          HartSignature hartSignature,
          RsaKeyValue rsaKeyValue,
          File signatureKeyFile,
          File publicKeyFile) throws CouldNotVerifySignatureException {
    // Sanity check: does the signature file match the known public key file?
    // If not, the file may have been signed with a newer or older version than we support.
    RsaKeyValue rsaFromFile = hartSignature.keyInfo.keyValue.rsaKeyValue;
    rsaFromFile.exponent = rsaFromFile.exponent.trim();
    rsaFromFile.modulus = rsaFromFile.modulus.trim();
    if (!rsaFromFile.exponent.equals(rsaKeyValue.exponent)
      || !rsaFromFile.modulus.equals(rsaKeyValue.modulus)) {
      throw new CouldNotVerifySignatureException(
              "%s was signed with a different public key than %s.\nActual modulus: %s\nExpected: %s"
              .formatted(signatureKeyFile.getAbsolutePath(), publicKeyFile.getAbsolutePath(),
                      rsaFromFile.modulus, rsaKeyValue.modulus)
      );
    }
  }

  private static boolean checkSignature(HartSignature hartSignature, RsaKeyValue rsaKeyValue)
          throws CouldNotVerifySignatureException {
    // Decode Base64
    byte[] modulusBytes = Base64.getDecoder().decode(rsaKeyValue.modulus);
    byte[] exponentBytes = Base64.getDecoder().decode(rsaKeyValue.exponent);
    byte[] signatureBytes = Base64.getDecoder().decode(hartSignature.signatureValue.trim());

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
      throw new CouldNotVerifySignatureException("Failed to load signing algorithms: "
              + e.getMessage());
    }

    // Initialize the signature with the public key
    try {
      signature.initVerify(publicKey);
    } catch (InvalidKeyException e) {
      throw new CouldNotVerifySignatureException("Invalid Key: " + e.getMessage());
    }

    // Canonicalize the XML
    byte[] canonicalizedBytes = canonicalizeXml(hartSignature.signedInfo);

    // Verify the signature
    try {
      signature.update(canonicalizedBytes);
      return signature.verify(signatureBytes);
    } catch (SignatureException e) {
      throw new CouldNotVerifySignatureException("Signature failure: %s" + e.getMessage());
    }
  }

  private static byte[] canonicalizeXml(SignedInfo signedInfo)
          throws CouldNotVerifySignatureException {
    // Canonicalize -- sort of. We need one change in addition to canonicalization to mirror what
    // .NET Framework 4.8.1 does, which is what Hart uses.

    // Build the XML mapper to serialize from the SignedInfo object
    XmlMapper xmlMapper = new XmlMapper();
    String xmlSignedInfo = null;
    try {
      xmlSignedInfo = xmlMapper.writeValueAsString(signedInfo);
    } catch (JsonProcessingException e) {
      throw new CouldNotVerifySignatureException("Failed to parse the signature XML file");
    }

    // Set up the canonicalization transform
    XMLSignatureFactory sigFactory = XMLSignatureFactory.getInstance("DOM");
    CanonicalizationMethod c14n;
    try {
      c14n = sigFactory.newCanonicalizationMethod(signedInfo.canonicalizationMethod.algorithm,
              (C14NMethodParameterSpec) null);
    } catch (NoSuchAlgorithmException e) {
      throw new CouldNotVerifySignatureException(
              ".sig.xml file uses an unsupported canonicalization algorithm");
    } catch (InvalidAlgorithmParameterException e) {
      throw new CouldNotVerifySignatureException(
              ".sig.xml file uses an invalid canonicalization algorithm parameter");
    }

    // Read the serialized data into a stream and canonicalize it
    InputStream xmlSignedInfoStream = new ByteArrayInputStream(
            xmlSignedInfo.getBytes(StandardCharsets.UTF_8));
    OctetStreamData uncanonicalizedData = new OctetStreamData(xmlSignedInfoStream);
    OctetStreamData canonicalizedData;
    try {
      canonicalizedData = (OctetStreamData) c14n.transform(uncanonicalizedData, null);
    } catch (TransformException e) {
      throw new CouldNotVerifySignatureException("Canonicalization failed: " + e.getMessage());
    }

    try {
      return canonicalizedData.getOctetStream().readAllBytes();
    } catch (IOException e) {
      throw new CouldNotVerifySignatureException("Canonicalization returned an invalid result");
    }
  }

  private static <T> T readFromXml(File xmlFile, Class<T> classType) throws IOException {
    XmlMapper xmlMapper = new XmlMapper();
    xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    try (FileInputStream inputStream = new FileInputStream(xmlFile)) {
      T xmlObject = xmlMapper.readValue(inputStream, classType);
      inputStream.close();
      return xmlObject;
    }
  }

  public static class CouldNotVerifySignatureException extends Exception {
    CouldNotVerifySignatureException(String message) {
      super(message);
    }
  }
}
