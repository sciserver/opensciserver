/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0. 
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.compute.core.client;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;


public class HttpClientFactory {
    private static final Logger logger = LogManager.getLogger(HttpClientFactory.class);
    
    private boolean isSecure;
    private byte[] certData;
    private byte[] keyData;

    public HttpClientFactory(byte[] certData, byte[] keyData) {
        this.isSecure = !(certData == null && keyData == null);
        this.certData = certData;
        this.keyData = keyData;
    }

    public CloseableHttpClient createHttpClient() throws Exception {
        if (isSecure) {
            if (Security.getProvider(BouncyCastleJsseProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleJsseProvider());
            }
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
            return HttpClients.custom().setSSLSocketFactory(getSSLSocketFactory()).build();
        } else {
            return HttpClients.createDefault();
        }
    }

    private SSLConnectionSocketFactory getSSLSocketFactory() throws Exception {
        X509Certificate cert = getX509Certificate(certData);
        PrivateKey key = getPrivateKey(keyData);
        
        KeyStore keystore = KeyStore.getInstance("JKS");
        keystore.load(null);
        keystore.setKeyEntry("foo", key, new char[] {}, new Certificate[] { cert });
        
        SSLContext sslContext = SSLContexts.custom()
                .loadKeyMaterial(keystore, new char[] {})
                .loadTrustMaterial(new TrustStrategy() {
                    @Override
                    public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                        return true;
                    }
                })
                .setProvider(BouncyCastleJsseProvider.PROVIDER_NAME)
                .build();
        
        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                sslContext,
                new String[] {"TLSv1", "TLSv1.1", "TLSv1.2"},
                null,
                NoopHostnameVerifier.INSTANCE);
        
        return sslSocketFactory;
    }

    private X509Certificate getX509Certificate(byte[] data) throws Exception {
        PEMParser pemParser = new PEMParser(new InputStreamReader(new ByteArrayInputStream(data)));
        try {
            X509CertificateHolder certHolder = (X509CertificateHolder) pemParser.readObject();
            JcaX509CertificateConverter converter = new JcaX509CertificateConverter()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME);
            return converter.getCertificate(certHolder);
        } finally {
            pemParser.close();
        }
    }

    private PrivateKey getPrivateKey(byte[] data) throws Exception {
        PEMParser pemParser = new PEMParser(new InputStreamReader(new ByteArrayInputStream(data)));
        try {
            PEMKeyPair keyPair = (PEMKeyPair) pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);
            return converter.getPrivateKey(keyPair.getPrivateKeyInfo());
        } finally {
            pemParser.close();
        }
    }
}
