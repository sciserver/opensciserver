package org.sciserver.testservice;

import java.net.URI;
import java.nio.file.Path;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Deployment-controlled settings; API callers cannot change these. */
@Component
public class Settings {
    final String namespace;
    final String image;
    final String baseUrl;
    final String racmUrl;
    final String owner;
    final String pullSecret;
    final String callbackUrl;
    final byte[] signingKey;
    final int callbackWindow;
    final int reportLimit;
    final int retainedReports;
    final int retentionSeconds;
    final int deadline;
    final int maxActive;
    final URI kubernetes;
    final Path tokenFile;
    final Path caFile;

    /** Read and validate deployment-controlled configuration. */
    public Settings(Environment env) {
        namespace = env.getRequiredProperty("test.namespace");
        image = env.getRequiredProperty("test.image");
        baseUrl = env.getRequiredProperty("test.base-url");
        racmUrl = env.getRequiredProperty("test.racm-url").replaceAll("/+$", "");
        callbackUrl = env.getRequiredProperty("test.callback-url").replaceAll("/+$", "");
        signingKey = java.util.Base64.getDecoder().decode(env.getRequiredProperty("test.report-signing-key"));
        callbackWindow = env.getProperty("test.callback-window-seconds", Integer.class, 120);
        reportLimit = 1024 * 1024;
        retainedReports = env.getProperty("test.retained-reports", Integer.class, 50);
        retentionSeconds = env.getProperty("test.retention-seconds", Integer.class, 3600);
        owner = env.getProperty("test.owner", "sciserver-tests");
        pullSecret = env.getProperty("test.pull-secret", "");
        deadline = env.getProperty("test.deadline-seconds", Integer.class, 1800);
        maxActive = env.getProperty("test.max-active", Integer.class, 2);
        kubernetes = URI.create(env.getProperty("test.kubernetes-url", "https://kubernetes.default.svc"));
        tokenFile = Path.of(env.getProperty("test.token-file",
                "/var/run/secrets/kubernetes.io/serviceaccount/token"));
        caFile = Path.of(env.getProperty("test.ca-file",
                "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt"));
        if (!namespace.matches("[a-z0-9][a-z0-9-]{0,62}") || !owner.matches("[a-z0-9][a-z0-9-]{0,40}")
                || image.isBlank() || deadline < 1 || maxActive < 1 || signingKey.length < 32
                || callbackWindow < 60 || callbackWindow > 300 || retainedReports < 1 || retainedReports > 100
                || retentionSeconds < 1 || retentionSeconds > 86400) {
            throw new IllegalArgumentException("Invalid test-service configuration");
        }
        for (String url : new String[] {baseUrl, racmUrl, callbackUrl}) {
            URI uri = URI.create(url);
            if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getQuery() != null || uri.getFragment() != null) {
                throw new IllegalArgumentException("Invalid configured service URL");
            }
        }
    }
}
