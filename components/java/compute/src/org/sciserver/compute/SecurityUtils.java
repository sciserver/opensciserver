/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/
package org.sciserver.compute;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Calendar;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class SecurityUtils {
	private static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
	private static final String END_CERT = "-----END CERTIFICATE-----";
	private static final String BEGIN_KEY = "-----BEGIN RSA PRIVATE KEY-----";
	private static final String END_KEY = "-----END RSA PRIVATE KEY-----";
	private static final Encoder base64 = Base64.getEncoder();
	
	public static void main(String[] args) throws Exception {
		// Generate CA
		KeyPair caKeyPair = createKeyPair();
		X509Certificate ca = createRootCA(caKeyPair, "CN=Default CA");
		
		// Generate server certificate
		KeyPair serverKeyPair = createKeyPair();
		X509Certificate server = createSingedCert(serverKeyPair, ca, caKeyPair.getPrivate(), "CN=Server");
		
		// Generate client certificate
		KeyPair clientKeyPair = createKeyPair();
		X509Certificate client = createSingedCert(clientKeyPair, ca, caKeyPair.getPrivate(), "CN=Client");
		
		System.out.println("# CA");
		System.out.println(new String(getCertPem(ca)) + "\n");
		
		System.out.println("# Server certificate");
		System.out.println(new String(getCertPem(server)) + "\n");
		
		System.out.println("# Server private key");
		System.out.println(new String(getPrivateKeyPem(serverKeyPair.getPrivate())) + "\n");
		
		System.out.println("# Client certificate");
		System.out.println(new String(getCertPem(client)) + "\n");
		
		System.out.println("# Client private key");
		System.out.println(new String(getPrivateKeyPem(clientKeyPair.getPrivate())) + "\n");
	}
	
	public static KeyPair createKeyPair() throws Exception {
		KeyPairGenerator rsa = KeyPairGenerator.getInstance("RSA");
		rsa.initialize(4096);
		return rsa.generateKeyPair();
	}
	
	public static X509Certificate createRootCA(KeyPair keyPair, String subject) throws Exception {
		String issuer = subject;
		
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, 1);
		
		JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
				new X500Name(issuer),
				BigInteger.valueOf(1),
				new Date(),
				cal.getTime(),
				new X500Name(subject),
				keyPair.getPublic());
		
		builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign));
		builder.addExtension(Extension.basicConstraints, false, new BasicConstraints(true));
		
		X509CertificateHolder certHolder = builder
				.build(new JcaContentSignerBuilder("SHA1withRSA").build(keyPair.getPrivate()));
		
		return new JcaX509CertificateConverter().getCertificate(certHolder);
	}
	
	public static X509Certificate createSingedCert(KeyPair keyPair, X509Certificate ca, PrivateKey caPrivateKey, String subject) throws Exception {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, 1);
		
		JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
				ca,
				BigInteger.valueOf(1),
				new Date(),
				cal.getTime(),
				new X500Name(subject),
				keyPair.getPublic());
		
		builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));
		builder.addExtension(Extension.basicConstraints, false, new BasicConstraints(true));
		
		X509CertificateHolder certHolder = 
				builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(caPrivateKey));
		
		return new JcaX509CertificateConverter().getCertificate(certHolder);
	}
	
	public static byte[] getCertPem(X509Certificate cert) throws Exception {
		return (BEGIN_CERT + "\n" + new String(base64.encode(cert.getEncoded())) + "\n" + END_CERT).getBytes();
	}
	
	public static byte[] getPrivateKeyPem(PrivateKey cert) throws Exception {
		return (BEGIN_KEY + "\n" + new String(base64.encode(cert.getEncoded())) + "\n" + END_KEY).getBytes();
	}
}
