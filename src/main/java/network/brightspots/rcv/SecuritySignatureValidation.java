/*
 * RCTab
 * Copyright (c) 2017-2023 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: A set of tools to verify signatures of Hart CVRs.
 * Design: Reproduces the signature verification algorithm used by Hart, which
 * is the .NET Framework 4.8.1 implementation of a detached XML Digital Signature.
 * Conditions: When verifying the signature of a Hart file.
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import static network.brightspots.rcv.SecurityXmlParsers.HartSignature;
import static network.brightspots.rcv.SecurityXmlParsers.RsaKeyValue;
import static network.brightspots.rcv.SecurityXmlParsers.SignedInfo;

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
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
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

class SecuritySignatureValidation {
  /**
   * Throws an exception if the signature is invalid.
   * Throws CouldNotVerifySignature if it is unable to run the validation, for reasons including:
   * 1. The public key in the signed file does not match the expected key
   * 2. The canonicalization or hashing algorithm is not supported
   * 3. The data file has been edited and its hash does not match what's found in signatureKeyFile
   * Throws VerificationFailedException if the signature verification successfully ran, but
   * the corresponding signature was invalid.
   */
  public static void ensureSignatureIsValid(
          RsaKeyValue publicKey,
          File signatureKeyFile,
          File dataFile) throws CouldNotVerifySignatureException, VerificationFailedException {
    // Load the .sig.xml file into a HartSignature object
    HartSignature hartSignature;
    try {
      hartSignature = readFromXml(signatureKeyFile, HartSignature.class);
    } catch (IOException e) {
      throw new CouldNotVerifySignatureException("Failed to read files: " + e.getMessage());
    }

    ensurePublicKeyMatchesExpectedValue(hartSignature, publicKey, signatureKeyFile);
    ensureDigestMatchesExpectedValue(dataFile, hartSignature);
    ensureSignatureMatches(hartSignature, publicKey);
  }

  /**
   * It is possible for the signature to be valid, but for the underlying file to have been changed,
   * meaning the hash no longer matches what was signed. This function mandates that the data file
   * was not modified. If it was, it is evidence of tampering and we throw an error.
   */
  private static void ensureDigestMatchesExpectedValue(File dataFile, HartSignature hartSignature)
          throws CouldNotVerifySignatureException {
    // Check if the filenames match
    String actualFilename = dataFile.getName();
    String expectedFilename = new File(hartSignature.signedInfo.reference.uri).getName();
    if (!actualFilename.equals(expectedFilename)) {
      throw new CouldNotVerifySignatureException(
              "Signed file was %s but you provided %s".formatted(actualFilename, expectedFilename));
    }

    String algorithm = javaAlgorithmForW3AlgorithmUrl(
            hartSignature.signedInfo.reference.digestMethod.algorithm);
    byte[] actualDigestBytes = FileUtils.getHashBytes(dataFile, algorithm);
    String actualDigest = Base64.getEncoder().encodeToString(actualDigestBytes);
    String expectedDigest = hartSignature.signedInfo.reference.digestValue;

    if (!actualDigest.equals(expectedDigest)) {
      throw new CouldNotVerifySignatureException(
              "Signed file had digest %s but the file you provided had %s"
              .formatted(expectedDigest, actualDigest));
    }
  }

  /**
   * The .sig.xml files use a w3.org URL to reference an algorithm, whereas Java
   * uses just SHA-X. Convert the w3.org URL to the Java algorithm name .
   */
  private static String javaAlgorithmForW3AlgorithmUrl(String w3AlgorithmUrl)
          throws CouldNotVerifySignatureException {
    return switch (w3AlgorithmUrl) {
      case "http://www.w3.org/2001/04/xmlenc#sha512" -> "SHA-512";
      case "http://www.w3.org/2001/04/xmlenc#sha256" -> "SHA-256";
      case "http://www.w3.org/2000/09/xmldsig#sha1" -> "SHA-1";
      default -> throw new CouldNotVerifySignatureException(
              "Unsupported digest algorithm: %s".formatted(w3AlgorithmUrl));
    };
  }


  /**
   * Does the signature file match the expected public key, which is encoded in the
   * source of the Java application? If not, the file was signed with a different public
   * key, and we cannot trust it.
   */
  private static void ensurePublicKeyMatchesExpectedValue(
          HartSignature hartSignature,
          RsaKeyValue expectedPublicKey,
          File signatureKeyFile) throws CouldNotVerifySignatureException {
    RsaKeyValue actualPublicKey = hartSignature.keyInfo.keyValue.rsaKeyValue;
    actualPublicKey.exponent = actualPublicKey.exponent.trim();
    actualPublicKey.modulus = actualPublicKey.modulus.trim();
    if (!actualPublicKey.exponent.equals(expectedPublicKey.exponent)
        || !actualPublicKey.modulus.equals(expectedPublicKey.modulus)) {
      throw new CouldNotVerifySignatureException(
              "%s was signed with unexpected public key.\nActual modulus: %s\nExpected: %s"
              .formatted(signatureKeyFile.getAbsolutePath(),
                      actualPublicKey.modulus, expectedPublicKey.modulus)
      );
    }
  }

  /**
   * Once all checks of tampering have been performed, we can check the signature itself.
   * If this returns false, it is still a severe, blocking error.
   */
  private static void ensureSignatureMatches(HartSignature hartSignature, RsaKeyValue rsaKeyValue)
          throws CouldNotVerifySignatureException, VerificationFailedException {
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
      if (!signature.verify(signatureBytes)) {
        throw new VerificationFailedException("Signature did not match.");
      }
    } catch (SignatureException e) {
      throw new CouldNotVerifySignatureException("Signature failure: %s" + e.getMessage());
    }
  }

  /**
   * The XML namespace is propagated in .NET Framework 4.8.1's library, but not in Java's security
   * libraries. Since Hart uses an outdated canonicalization algorithm (1.0) that leaves
   * ambiguity about namespace propagation, this function resolves that ambiguity by replicating
   * what .NET Framework 4.8.1 does.
   * Note that more modern canonicalization algorithms do not have this ambiguity.
   * See also:
   * Canonical XML Version 1.0: https://www.w3.org/TR/2001/REC-xml-c14n-20010315
   * Canonical XML Version 1.1: https://www.w3.org/TR/xml-c14n11/
   */
  private static byte[] canonicalizeXml(SignedInfo signedInfo)
          throws CouldNotVerifySignatureException {
    // Convert the SignedInfo object to XML
    XmlMapper xmlMapper = new XmlMapper();
    String xmlSignedInfo;
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

  /**
   * Reads an XML file into a Java object.
   */
  private static <T> T readFromXml(File xmlFile, Class<T> classType) throws IOException {
    XmlMapper xmlMapper = new XmlMapper();
    xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    try (FileInputStream inputStream = new FileInputStream(xmlFile)) {
      T xmlObject = xmlMapper.readValue(inputStream, classType);
      inputStream.close();
      return xmlObject;
    }
  }

  // Used when something prevents the signature verification from running,
  // either because of an error or because the verification result would be invalid even if
  // it succeeded (e.g. the signature was valid, but the hash did not match).
  static class CouldNotVerifySignatureException extends Exception {
    CouldNotVerifySignatureException(String message) {
      super(message);
    }
  }

  // Used when the signature verification successfully ran, but the signatures did not match.
  static class VerificationFailedException extends Exception {
    VerificationFailedException(String message) {
      super(message);
    }
  }
}
