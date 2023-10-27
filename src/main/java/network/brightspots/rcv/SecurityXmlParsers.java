/*
 * RCTab
 * Copyright (c) 2017-2023 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: In-memory representation of .sig.xml signature files.
 * Design: A series of Java objects that can be serialized to and from XML.
 * Conditions: When verifying the signature of a Hart file.
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

class SecurityXmlParsers {
  @JacksonXmlRootElement(localName = "Signature", namespace = "http://www.w3.org/2000/09/xmldsig#")
  public static class HartSignature {
    @JacksonXmlProperty(localName = "SignedInfo")
    SignedInfo signedInfo;

    @JacksonXmlProperty(localName = "SignatureValue", isAttribute = true)
    String signatureValue;

    @JacksonXmlProperty(localName = "KeyInfo")
    KeyInfo keyInfo;
  }

  static class SignedInfo {
    @JacksonXmlProperty(localName = "CanonicalizationMethod")
    CanonicalizationMethod canonicalizationMethod;

    @JacksonXmlProperty(localName = "SignatureMethod")
    SignatureMethod signatureMethod;

    @JacksonXmlProperty(localName = "Reference")
    Reference reference;

    // Required to match Hart's implementation of canonicalization
    @JacksonXmlProperty(localName = "xmlns", isAttribute = true)
    String xmlns = "http://www.w3.org/2000/09/xmldsig#";
  }

  static class CanonicalizationMethod {
    @JacksonXmlProperty(localName = "Algorithm", isAttribute = true)
    String algorithm;
  }

  static class SignatureMethod {
    @JacksonXmlProperty(localName = "Algorithm", isAttribute = true)
    String algorithm;
  }

  static class Reference {
    @JacksonXmlProperty(localName = "URI", isAttribute = true)
    String uri;

    @JacksonXmlProperty(localName = "DigestMethod")
    DigestMethod digestMethod;

    @JacksonXmlProperty(localName = "DigestValue")
    String digestValue;

    // Getters and setters
  }

  static class DigestMethod {
    @JacksonXmlProperty(localName = "Algorithm", isAttribute = true)
    String algorithm;
  }

  static class KeyInfo {
    @JacksonXmlProperty(localName = "KeyValue")
    KeyValue keyValue;
  }

  static class KeyValue {
    @JacksonXmlProperty(localName = "RSAKeyValue")
    RsaKeyValue rsaKeyValue;
  }

  static class RsaKeyValue {
    @JacksonXmlProperty(localName = "Modulus", isAttribute = true)
    String modulus;

    @JacksonXmlProperty(localName = "Exponent", isAttribute = true)
    String exponent;
  }
}
