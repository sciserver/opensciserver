package org.sciserver.testservice;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Bounded HTTP transport. Redirects never forward authentication to another host. */
public class RemoteHttp {
    static final int MAX_BYTES = 10 * 1024 * 1024;
    private final HttpClient client;

    public RemoteHttp(HttpClient client) {
        this.client = client;
    }

    /** Perform a bounded request without following redirects. */
    public Reply call(URI uri, String method, String authHeader, String token, String body)
            throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json");
        if (token != null) {
            request.header(authHeader, token);
        }
        if (body != null) {
            request.header("Content-Type", "PATCH".equals(method)
                    ? "application/merge-patch+json" : "application/json");
        }
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body));
        HttpResponse<InputStream> response = client.send(request.build(), HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream stream = response.body()) {
            byte[] bytes = stream.readNBytes(MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES) {
                throw new IOException("Upstream response exceeds size limit");
            }
            return new Reply(response.statusCode(), new String(bytes, StandardCharsets.UTF_8));
        }
    }

    static class Reply {
        final int status;
        final String body;

        Reply(int status, String body) {
            this.status = status;
            this.body = body;
        }
    }
}
