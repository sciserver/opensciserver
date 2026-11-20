package org.sciserver.testservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.server.ResponseStatusException;

/** Fake upstream APIs prove that names alone cannot authorize destructive operations. */
class AccountProviderTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final RemoteHttp http = mock(RemoteHttp.class);
    private AccountProvider provider;
    private final RacmProfiles profiles = mock(RacmProfiles.class);
    private boolean userExists = true;
    private boolean projectExists = true;
    private boolean proofFails;
    private boolean malformed;
    private int deletes;

    @BeforeEach
    void setUp() throws Exception {
        Settings settings = new Settings(new MockEnvironment().withProperty("test.namespace", "sciserver")
                .withProperty("test.ledger-image", "registry/tests:example")
                .withProperty("test.base-url", "http://example.test/sciserver")
                .withProperty("test.racm-url", "http://racm/sciserver/racm")
                .withProperty("test.broker-signing-key", "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=")
                .withProperty("test.account-broker-enabled", "true")
                .withProperty("test.account-admin-user", "cleanup-admin")
                .withProperty("test.account-admin-password", "admin-secret")
                .withProperty("test.account-keystone-url", "http://keystone/v3"));
        provider = new AccountProvider(settings, mapper, http, profiles);
        when(http.call(any(), any(), any(), any(), any())).thenAnswer(invocation -> {
            URI uri = invocation.getArgument(0);
            final String method = invocation.getArgument(1);
            String body = invocation.getArgument(4);
            String path = uri.getPath();
            if (path.endsWith("/api/auth")) {
                if (body.contains("cleanup-admin")) {
                    return new RemoteHttp.Reply(200, "admin-token");
                }
                return new RemoteHttp.Reply(proofFails ? 401 : 200, "temporary-token");
            }
            if (path.endsWith("/api/validate/temporary-token")) {
                return new RemoteHttp.Reply(200, "{\"username\":\"test-user\",\"user_id\":\"user-id\"}");
            }
            if (malformed) {
                return new RemoteHttp.Reply(200, "{}");
            }
            String user = "{\"id\":\"user-id\",\"name\":\"test-user\","
                    + "\"default_project_id\":\"project-id\",\"domain_id\":\"default\"}";
            String project = "{\"id\":\"project-id\",\"name\":\"test-user\",\"domain_id\":\"default\"}";
            if (path.equals("/v3/users")) {
                return new RemoteHttp.Reply(200, "{\"users\":[" + (userExists ? user : "") + "]}");
            }
            if (path.equals("/v3/projects")) {
                return new RemoteHttp.Reply(200, "{\"projects\":[" + (projectExists ? project : "") + "]}");
            }
            if ("DELETE".equals(method)) {
                deletes++;
                if (path.equals("/v3/users/user-id")) {
                    userExists = false;
                } else if (path.equals("/v3/projects/project-id")) {
                    projectExists = false;
                } else {
                    throw new AssertionError("Unexpected deletion path");
                }
                return new RemoteHttp.Reply(204, "");
            }
            if (path.equals("/v3/users/user-id")) {
                return new RemoteHttp.Reply(userExists ? 200 : 404, "{\"user\":" + user + "}");
            }
            if (path.equals("/v3/projects/project-id")) {
                return new RemoteHttp.Reply(projectExists ? 200 : 404, "{\"project\":" + project + "}");
            }
            throw new AssertionError("Unexpected account provider request");
        });
    }

    @Test
    void verifiesGeneratedCredentialsThenDeletesExactIdsIdempotently() {
        JsonNode identity = provider.identify("test-user", "generated-password");
        assertEquals("user-id", identity.path("userId").asText());
        provider.delete("test-user", identity);
        assertEquals(2, deletes);
        provider.delete("test-user", identity);
        assertEquals(4, deletes); // Provider DELETE 404 is also success.
    }

    @Test
    void refusesExistingNamesAndWrongPasswordsWithoutDeletion() {
        assertThrows(ResponseStatusException.class, () -> provider.assertAbsent("test-user"));
        proofFails = true;
        assertThrows(ResponseStatusException.class, () -> provider.identify("test-user", "wrong-password"));
        assertEquals(0, deletes);
    }

    @Test
    void malformedResponsesAndUnprovenPartialProjectsAreNeverDeleted() {
        userExists = false;
        assertThrows(ResponseStatusException.class, () -> provider.identify("test-user", "generated-password"));
        malformed = true;
        assertThrows(ResponseStatusException.class, () -> provider.identify("test-user", "generated-password"));
        JsonNode identity = mapper.createObjectNode().put("userId", "user-id").put("projectId", "project-id")
                .put("domainId", "default");
        assertThrows(ResponseStatusException.class, () -> provider.delete("test-user", identity));
        assertEquals(0, deletes);
    }

    @Test
    void racmBlockerPreservesKeystoneIdentity() {
        org.mockito.Mockito.doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT))
                .when(profiles).delete(any(), any());
        JsonNode identity = mapper.createObjectNode().put("userId", "user-id")
                .put("projectId", "project-id").put("domainId", "default");
        assertThrows(ResponseStatusException.class, () -> provider.delete("test-user", identity));
        assertEquals(0, deletes);
    }

}
