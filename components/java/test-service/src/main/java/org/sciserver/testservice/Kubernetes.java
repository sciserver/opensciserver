package org.sciserver.testservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.time.Duration;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Small in-cluster API adapter. Uses the mounted CA and re-reads rotating tokens. */
@Component
public class Kubernetes {
    private final Settings settings;
    private final ObjectMapper mapper;
    private final RemoteHttp http;

    /** Create a TLS-verified, in-cluster API client. */
    public Kubernetes(Settings settings, ObjectMapper mapper) throws Exception {
        this.settings = settings;
        this.mapper = mapper;
        KeyStore trust = KeyStore.getInstance(KeyStore.getDefaultType());
        trust.load(null);
        try (InputStream ca = Files.newInputStream(settings.caFile)) {
            int index = 0;
            for (java.security.cert.Certificate cert : CertificateFactory.getInstance("X.509")
                    .generateCertificates(ca)) {
                trust.setCertificateEntry("cluster-" + index++, cert);
            }
        }
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(trust);
        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(null, factory.getTrustManagers(), null);
        http = new RemoteHttp(HttpClient.newBuilder().sslContext(ssl)
                .connectTimeout(Duration.ofSeconds(5)).build());
    }

    /** Perform an API call and decode its JSON response. */
    public JsonNode json(String method, String path, JsonNode body) {
        try {
            return mapper.readTree(text(method, path, body));
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid Kubernetes response");
        }
    }

    /** Perform an API call using the current projected token. */
    public String text(String method, String path, JsonNode body) {
        try {
            String token = Files.readString(settings.tokenFile).trim();
            RemoteHttp.Reply response = http.call(URI.create(settings.kubernetes + path), method,
                    "Authorization", "Bearer " + token, body == null ? null : mapper.writeValueAsString(body));
            if (response.status == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Kubernetes resource not found");
            }
            if (response.status == 409) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Kubernetes resource changed; retry request");
            }
            if (response.status < 200 || response.status >= 300) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Kubernetes rejected the request");
            }
            return response.body;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Kubernetes unavailable");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Kubernetes request interrupted");
        }
    }
}
