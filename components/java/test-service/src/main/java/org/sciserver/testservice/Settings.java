package org.sciserver.testservice;

import java.net.URI;
import java.nio.file.Path;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Deployment-controlled settings; API callers cannot change these. */
@Component
public class Settings {
    final String ledgerImage;
    final int leaseSeconds;
    final String namespace;
    final boolean accountBrokerEnabled;
    final String accountAdminUser;
    final String accountAdminPassword;
    final String accountKeystoneUrl;
    final String baseUrl;
    final String racmUrl;
    final String owner;
    final byte[] signingKey;
    final URI kubernetes;
    final Path tokenFile;
    final Path caFile;

    /** Read and validate deployment-controlled configuration. */
    public Settings(Environment env) {
        accountBrokerEnabled = env.getProperty("test.account-broker-enabled", Boolean.class, false);
        accountAdminUser = env.getProperty("test.account-admin-user", "");
        accountAdminPassword = env.getProperty("test.account-admin-password", "");
        accountKeystoneUrl = env.getProperty("test.account-keystone-url", "").replaceAll("/+$", "");
        ledgerImage = env.getRequiredProperty("test.ledger-image");
        leaseSeconds = env.getProperty("test.account-lease-seconds", Integer.class, 1980);
        namespace = env.getRequiredProperty("test.namespace");
        baseUrl = env.getRequiredProperty("test.base-url");
        racmUrl = env.getRequiredProperty("test.racm-url").replaceAll("/+$", "");
        signingKey = java.util.Base64.getDecoder().decode(env.getRequiredProperty("test.broker-signing-key"));
        owner = env.getProperty("test.owner", "sciserver-tests");
        kubernetes = URI.create(env.getProperty("test.kubernetes-url", "https://kubernetes.default.svc"));
        tokenFile = Path.of(env.getProperty("test.token-file",
                "/var/run/secrets/kubernetes.io/serviceaccount/token"));
        caFile = Path.of(env.getProperty("test.ca-file",
                "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt"));
        if (!namespace.matches("[a-z0-9][a-z0-9-]{0,62}") || !owner.matches("[a-z0-9][a-z0-9-]{0,40}")
                || ledgerImage.isBlank() || leaseSeconds < 1 || signingKey.length < 32) {
            throw new IllegalArgumentException("Invalid test-service configuration");
        }
        if (accountBrokerEnabled && (accountAdminUser.isBlank() || accountAdminPassword.isBlank())) {
            throw new IllegalArgumentException("Account broker requires dedicated cleanup credentials");
        }
        if (accountBrokerEnabled) {
            URI uri = URI.create(accountKeystoneUrl);
            if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getQuery() != null || uri.getFragment() != null || !uri.getPath().endsWith("/v3")) {
                throw new IllegalArgumentException("Account broker requires a Keystone v3 URL");
            }
        }
        for (String url : new String[] {baseUrl, racmUrl}) {
            URI uri = URI.create(url);
            if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getQuery() != null || uri.getFragment() != null) {
                throw new IllegalArgumentException("Invalid configured service URL");
            }
        }
    }
}
