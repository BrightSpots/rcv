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

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

public class CryptographyXMLParsers {
    @JacksonXmlRootElement(localName = "Signature", namespace = "http://www.w3.org/2000/09/xmldsig#")
    public static class HartSignature {
        @JacksonXmlProperty(localName = "SignedInfo")
        SignedInfo signedInfo;

        @JacksonXmlProperty(localName = "SignatureValue")
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
    }

    static class CanonicalizationMethod {
        @JacksonXmlProperty(isAttribute = true)
        String algorithm;
    }

    static class SignatureMethod {
        @JacksonXmlProperty(isAttribute = true)
        String algorithm;
    }

    static class Reference {
        @JacksonXmlProperty(isAttribute = true)
        String URI;

        @JacksonXmlProperty(localName = "DigestMethod")
        DigestMethod digestMethod;

        @JacksonXmlProperty(localName = "DigestValue")
        String digestValue;

        // Getters and setters
    }

    static class DigestMethod {
        @JacksonXmlProperty(isAttribute = true)
        String algorithm;
    }

    static class KeyInfo {
        @JacksonXmlProperty(localName = "KeyValue")
        KeyValue keyValue;
    }

    static class KeyValue {
        @JacksonXmlProperty(localName = "RSAKeyValue")
        RSAKeyValue rsaKeyValue;
    }

    static class RSAKeyValue {
        @JacksonXmlProperty(localName = "Modulus")
        String modulus;

        @JacksonXmlProperty(localName = "Exponent")
        String exponent;
    }
}