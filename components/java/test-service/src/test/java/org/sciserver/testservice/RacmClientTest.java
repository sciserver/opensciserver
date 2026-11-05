package org.sciserver.testservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.server.ResponseStatusException;

/** Verify real RACM client decoding, token headers and no-redirect transport against a local server. */
class RacmClientTest {
    private HttpServer server;
    private AdminAccess access;
    private volatile String groups;
    private volatile String profile;
    private volatile int responseCode;
    private final AtomicReference<String> receivedToken = new AtomicReference<>();
    private final AtomicInteger redirects = new AtomicInteger();

    @BeforeEach
    void start() throws IOException {
        groups = "{\"columns\":[\"name\",\"status\"],\"rows\":[[\"admin\",\"ACCEPTED\"]]}";
        profile = "{\"id\":1,\"username\":\"administrator\",\"visibility\":\"PUBLIC\"}";
        responseCode = 200;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/racm/ugm/rest/groups", exchange -> {
            receivedToken.set(exchange.getRequestHeaders().getFirst("X-Auth-Token"));
            if (responseCode == 302) {
                exchange.getResponseHeaders().set("Location", "/trap");
            }
            respond(exchange, responseCode, groups);
        });
        server.createContext("/racm/ugm/rest/user", exchange -> respond(exchange, 200, profile));
        server.createContext("/trap", exchange -> {
            redirects.incrementAndGet();
            respond(exchange, 200, "{}");
        });
        server.start();
        Settings settings = new Settings(new MockEnvironment().withProperty("test.namespace", "test")
                .withProperty("test.image", "tests:example").withProperty("test.base-url", "http://example.test")
                .withProperty("test.racm-url", "http://127.0.0.1:" + server.getAddress().getPort() + "/racm")
                .withProperty("test.callback-url", "http://test-service/reporting")
                .withProperty("test.report-signing-key", "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE="));
        access = new AdminAccess(settings);
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private void respond(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    @Test
    void acceptedAndOwnedAdminMembershipAuthorizeWithTrustedIdentity() {
        for (String status : new String[] {"ACCEPTED", "OWNER"}) {
            groups = "{\"columns\":[\"name\",\"status\"],\"rows\":[[\"admin\",\"" + status + "\"]]}";
            assertEquals("administrator", access.require("session-token"));
            assertEquals("session-token", receivedToken.get());
        }
    }

    @Test
    void pendingDeclinedAndUnrelatedMembershipCannotAuthorize() {
        for (String status : new String[] {"INVITED", "DECLINED", "WITHDRAWN"}) {
            groups = "{\"columns\":[\"name\",\"status\"],\"rows\":[[\"admin\",\"" + status + "\"]]}";
            assertEquals(403, assertThrows(ResponseStatusException.class,
                    () -> access.require("session-token")).getStatus().value());
        }
    }

    @Test
    void redirectDoesNotForwardTokenAndMalformedResponsesFailClosed() {
        responseCode = 302;
        assertEquals(503, assertThrows(ResponseStatusException.class,
                () -> access.require("session-token")).getStatus().value());
        assertEquals(0, redirects.get());
        responseCode = 200;
        groups = "<html>login</html>";
        assertEquals(503, assertThrows(ResponseStatusException.class,
                () -> access.require("session-token")).getStatus().value());
    }
}
