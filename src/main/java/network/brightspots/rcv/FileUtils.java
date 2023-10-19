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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.crypto.Data;
import javax.xml.crypto.NodeSetData;
import javax.xml.crypto.OctetStreamData;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.dom.DOMCryptoContext;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.TransformException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import static network.brightspots.rcv.CryptographyXMLParsers.HartSignature;
import static network.brightspots.rcv.CryptographyXMLParsers.RSAKeyValue;
import static network.brightspots.rcv.Utils.isNullOrBlank;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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

  static String tempMergeConflictGetHash(File file, String algorithm) {
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

  @SuppressWarnings("checkstyle:LeftCurly")
  static boolean verifyPublicKeySignature(File publicKeyFile, File signatureKeyFile, File dataFile)
          throws CouldNotVerifySignatureException {
    // try {
    //   return XmlSignature.validate(signatureKeyFile);
    // } catch (Exception e) {
    //   return false;
    // }
    // Load the public key and signature from their corresponding files
    HartSignature hartSignature;
    RSAKeyValue rsaKeyValue;
    try {
      hartSignature = readFromXML(signatureKeyFile, HartSignature.class);
      rsaKeyValue = readFromXML(publicKeyFile, RSAKeyValue.class);
    } catch (IOException e) {
      throw new CouldNotVerifySignatureException("Failed to read files: " + e.getMessage());
    }

    EnsurePublicKeyMatchesExpectedValue(hartSignature, rsaKeyValue, signatureKeyFile, publicKeyFile);
    EnsureDigestMatchesExpectedValue(dataFile, hartSignature);
    EnsureSignatureMatchesExpectedValue(hartSignature, rsaKeyValue, dataFile);

    return true;
  }

  static void EnsureDigestMatchesExpectedValue(File dataFile, HartSignature hartSignature) throws CouldNotVerifySignatureException
  {
    // Check if the filenames match
    String actualFilename = dataFile.getName();
    String expectedFilename = new File(hartSignature.signedInfo.reference.URI).getName();
    if (!actualFilename.equals(expectedFilename))
    {
      throw new CouldNotVerifySignatureException(
              "Signed file was %s but you provided %s".formatted(actualFilename, expectedFilename));
    }

    String actualDigest = FileUtils.tempMergeConflictGetHash(dataFile, "SHA-256");
    String expectedDigest = hartSignature.signedInfo.reference.digestValue;

    if (!actualDigest.equals(expectedDigest))
    {
      throw new CouldNotVerifySignatureException(
              "Signed file had digest %s but the file you provided had %s".formatted(expectedDigest, actualDigest));
    }
  }

  static void EnsurePublicKeyMatchesExpectedValue(HartSignature hartSignature, RSAKeyValue rsaKeyValue, File signatureKeyFile, File publicKeyFile) throws CouldNotVerifySignatureException {
    // Sanity check: does the signature file match the known public key file?
    // If not, the file may have been signed with a newer or older version than we support.
    RSAKeyValue rsaFromFile = hartSignature.keyInfo.keyValue.rsaKeyValue;
    rsaFromFile.exponent = rsaFromFile.exponent.trim();
    rsaFromFile.modulus = rsaFromFile.modulus.trim();
    if (!rsaFromFile.exponent.equals(rsaKeyValue.exponent) || !rsaFromFile.modulus.equals(rsaKeyValue.modulus))
    {
      throw new CouldNotVerifySignatureException("%s was signed with a different public key than %s"
              .formatted(signatureKeyFile.getAbsolutePath(), publicKeyFile.getAbsolutePath()));
    }
  }

  static void EnsureSignatureMatchesExpectedValue(HartSignature hartSignature, RSAKeyValue rsaKeyValue, File dataFile) throws CouldNotVerifySignatureException
  {
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
      throw new CouldNotVerifySignatureException("Failed to load signing algorithms: " + e.getMessage());
    }

    // Initialize the signature with the public key
    try {
      signature.initVerify(publicKey);
    } catch (InvalidKeyException e) {
      throw new CouldNotVerifySignatureException("Invalid Key: %s" + e.getMessage());
    }

    // Canonicalize the XML
    byte[] canonicalizedBytes = CanonicalizeXml(hartSignature.signedInfo);

    // Verify the signature
    boolean verified;
    try {
      signature.update(canonicalizedBytes);
      verified = signature.verify(signatureBytes);
    } catch (SignatureException e) {
      throw new CouldNotVerifySignatureException("Signature failure: %s" + e.getMessage());
    }

    if (!verified) {
      throw new CouldNotVerifySignatureException("Signature verification failed");
    }
  }

  static byte[] CanonicalizeXml(CryptographyXMLParsers.SignedInfo signedInfo) throws CouldNotVerifySignatureException {
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
      throw new CouldNotVerifySignatureException(".sig.xml file uses an unsupported canonicalization algorithm");
    } catch (InvalidAlgorithmParameterException e) {
      throw new CouldNotVerifySignatureException(".sig.xml file uses an invalid canonicalization algorithm parameter");
    }

    // Read the serialized data into a stream and canonicalize it
    InputStream xmlSignedInfoStream = new ByteArrayInputStream(xmlSignedInfo.getBytes(StandardCharsets.UTF_8));
    OctetStreamData canonicalizedData;
    try {
      canonicalizedData = (OctetStreamData)c14n.transform(new OctetStreamData(xmlSignedInfoStream), null);
    } catch (TransformException e) {
      throw new CouldNotVerifySignatureException("Canonicalization failed: " + e.getMessage());
    }

    try {
      return canonicalizedData.getOctetStream().readAllBytes();
    } catch (IOException e) {
      throw new CouldNotVerifySignatureException("Canonicalization returned an invalid result");
    }
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
