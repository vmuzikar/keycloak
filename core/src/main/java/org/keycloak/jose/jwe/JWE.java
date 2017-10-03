/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.jose.jwe;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.BouncyIntegration;
import org.keycloak.jose.jwe.alg.JWEAlgorithmProvider;
import org.keycloak.jose.jwe.enc.JWEEncryptionProvider;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JWE {

    static {
        BouncyIntegration.init();
    }

    private JWEHeader header;
    private String base64Header;

    private JWEKeyStorage keyStorage = new JWEKeyStorage();
    private String base64Cek;

    private byte[] initializationVector;

    private byte[] content;
    private byte[] encryptedContent;

    private byte[] authenticationTag;

    public JWE header(JWEHeader header) {
        this.header = header;
        this.base64Header = null;
        return this;
    }

    JWEHeader getHeader() {
        if (header == null && base64Header != null) {
            try {
                byte[] decodedHeader = Base64Url.decode(base64Header);
                header = JsonSerialization.readValue(decodedHeader, JWEHeader.class);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }
        return header;
    }

    public String getBase64Header() throws IOException {
        if (base64Header == null && header != null) {
            byte[] contentBytes = JsonSerialization.writeValueAsBytes(header);
            base64Header = Base64Url.encode(contentBytes);
        }
        return base64Header;
    }


    public JWEKeyStorage getKeyStorage() {
        return keyStorage;
    }


    public byte[] getInitializationVector() {
        return initializationVector;
    }


    public JWE content(byte[] content) {
        this.content = content;
        return this;
    }

    public byte[] getContent() {
        return content;
    }

    public byte[] getEncryptedContent() {
        return encryptedContent;
    }


    public byte[] getAuthenticationTag() {
        return authenticationTag;
    }


    public void setEncryptedContentInfo(byte[] initializationVector, byte[] encryptedContent, byte[] authenticationTag) {
        this.initializationVector = initializationVector;
        this.encryptedContent = encryptedContent;
        this.authenticationTag = authenticationTag;
    }


    public String encodeJwe() throws JWEException {
        try {
            if (header == null) {
                throw new IllegalStateException("Header must be set");
            }
            if (content == null) {
                throw new IllegalStateException("Content must be set");
            }

            JWEAlgorithmProvider algorithmProvider = JWERegistry.getAlgProvider(header.getAlgorithm());
            if (algorithmProvider == null) {
                throw new IllegalArgumentException("No provider for alg '" + header.getAlgorithm() + "'");
            }

            JWEEncryptionProvider encryptionProvider = JWERegistry.getEncProvider(header.getEncryptionAlgorithm());
            if (encryptionProvider == null) {
                throw new IllegalArgumentException("No provider for enc '" + header.getAlgorithm() + "'");
            }

            keyStorage.setEncryptionProvider(encryptionProvider);
            keyStorage.getCEKKey(JWEKeyStorage.KeyUse.ENCRYPTION, true); // Will generate CEK if it's not already present

            byte[] encodedCEK = algorithmProvider.encodeCek(encryptionProvider, keyStorage, keyStorage.getEncryptionKey());
            base64Cek = Base64Url.encode(encodedCEK);

            encryptionProvider.encodeJwe(this);

            return getEncodedJweString();
        } catch (Exception e) {
            throw new JWEException(e);
        }
    }


    private String getEncodedJweString() {
        StringBuilder builder = new StringBuilder();
        builder.append(base64Header).append(".")
                .append(base64Cek).append(".")
                .append(Base64Url.encode(initializationVector)).append(".")
                .append(Base64Url.encode(encryptedContent)).append(".")
                .append(Base64Url.encode(authenticationTag));

        return builder.toString();
    }


    public JWE verifyAndDecodeJwe(String jweStr) throws JWEException {
        try {
            String[] parts = jweStr.split("\\.");
            if (parts.length != 5) {
                throw new IllegalStateException("Not a JWE String");
            }

            this.base64Header = parts[0];
            this.base64Cek = parts[1];
            this.initializationVector = Base64Url.decode(parts[2]);
            this.encryptedContent = Base64Url.decode(parts[3]);
            this.authenticationTag = Base64Url.decode(parts[4]);

            this.header = getHeader();
            JWEAlgorithmProvider algorithmProvider = JWERegistry.getAlgProvider(header.getAlgorithm());
            if (algorithmProvider == null) {
                throw new IllegalArgumentException("No provider for alg '" + header.getAlgorithm() + "'");
            }

            JWEEncryptionProvider encryptionProvider = JWERegistry.getEncProvider(header.getEncryptionAlgorithm());
            if (encryptionProvider == null) {
                throw new IllegalArgumentException("No provider for enc '" + header.getAlgorithm() + "'");
            }

            keyStorage.setEncryptionProvider(encryptionProvider);

            byte[] decodedCek = algorithmProvider.decodeCek(Base64Url.decode(base64Cek), keyStorage.getEncryptionKey());
            keyStorage.setCEKBytes(decodedCek);

            encryptionProvider.verifyAndDecodeJwe(this);

            return this;
        } catch (Exception e) {
            throw new JWEException(e);
        }
    }

}
