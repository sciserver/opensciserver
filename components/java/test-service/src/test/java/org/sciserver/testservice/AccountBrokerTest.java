package org.sciserver.testservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.server.ResponseStatusException;

/** Ownership remains in Jobs across service restarts; capabilities never grant arbitrary deletion. */
class AccountBrokerTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final Kubernetes kube = mock(Kubernetes.class);
    private final AccountProvider provider = mock(AccountProvider.class);
    private final String id = UUID.randomUUID().toString();
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-05T12:00:00Z"), ZoneOffset.UTC);
    private Settings settings;
    private AccountBroker broker;
    private ObjectNode job;
    private String path;
    private String authorization;

    @BeforeEach
    void setUp() {
        settings = new Settings(new MockEnvironment().withProperty("test.namespace", "sciserver")
                .withProperty("test.image", "registry/tests:example")
                .withProperty("test.base-url", "http://example.test/sciserver")
                .withProperty("test.racm-url", "http://racm/sciserver/racm")
                .withProperty("test.callback-url", "http://test-service/reporting")
                .withProperty("test.report-signing-key", "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=")
                .withProperty("test.account-broker-enabled", "true")
                .withProperty("test.account-admin-user", "cleanup-admin")
                .withProperty("test.account-admin-password", "admin-secret")
                .withProperty("test.account-keystone-url", "http://keystone/v3"));
        broker = new AccountBroker(settings, kube, mapper, provider, clock);
        authorization = "Bearer " + AccountBroker.capability(settings, id);
        path = "/apis/batch/v1/namespaces/sciserver/jobs/test-broker-" + id;
        job = mapper.createObjectNode();
        ObjectNode metadata = job.putObject("metadata").put("uid", "job-uid").put("resourceVersion", "1");
        metadata.put("creationTimestamp", clock.instant().toString());
        metadata.putObject("labels").put("sciserver-test-broker", settings.owner)
                .put("sciserver-test-run", id).put("sciserver-test-kind", "broker");
        metadata.putObject("annotations").put("sciserver-test-broker-state", "open");
        job.putObject("spec");
        job.putObject("status");
        when(kube.json(eq("GET"), eq(path), isNull())).thenAnswer(invocation -> job.deepCopy());
        when(kube.json(eq("PATCH"), eq(path), any())).thenAnswer(invocation -> {
            JsonNode patch = invocation.getArgument(2);
            assertEquals(job.at("/metadata/resourceVersion").asText(),
                    patch.at("/metadata/resourceVersion").asText());
            patch.at("/metadata/annotations").fields().forEachRemaining(entry ->
                    ((ObjectNode) job.at("/metadata/annotations")).set(entry.getKey(), entry.getValue()));
            ((ObjectNode) job.path("metadata")).put("resourceVersion",
                    Integer.toString(job.at("/metadata/resourceVersion").asInt() + 1));
            return job.deepCopy();
        });
        when(kube.json(eq("GET"), eq("/apis/batch/v1/namespaces/sciserver/jobs?labelSelector="
                + "sciserver-test-broker=" + settings.owner), isNull())).thenAnswer(invocation -> {
                    ObjectNode response = mapper.createObjectNode();
                    response.putArray("items").add(job.deepCopy());
                    return response;
                });
    }

    private ObjectNode identity() {
        return mapper.createObjectNode().put("userId", "user-id").put("projectId", "project-id")
                .put("domainId", "default");
    }

    @Test
    void reservationIsDurableAndDoesNotPersistPasswordsOrCapabilities() {
        JsonNode first = broker.reserve(id, "session", authorization);
        AccountBroker restarted = new AccountBroker(settings, kube, mapper, provider, clock);
        JsonNode retry = restarted.reserve(id, "session", authorization);
        assertEquals(first, retry);
        assertTrue(first.path("username").asText().startsWith("test_user_"));
        assertFalse(job.toString().contains(first.path("password").asText()));
        assertFalse(job.toString().contains(AccountBroker.capability(settings, id)));
        verify(provider).assertAbsent(first.path("username").asText());
    }

    @Test
    void rejectsOtherRunTokensReportingTokensAndForeignJobs() {
        String other = "Bearer " + AccountBroker.capability(settings, UUID.randomUUID().toString());
        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(ResponseStatusException.class,
                () -> broker.reserve(id, "session", other)).getStatus());
        ReportInbox inbox = new ReportInbox(settings, new Contract(), mapper, clock);
        assertThrows(ResponseStatusException.class, () -> broker.reserve(id, "session", "Bearer " + inbox.issue(id)));
        assertNotEquals(inbox.issue(id), AccountBroker.capability(settings, id));
        ((ObjectNode) job.at("/metadata/labels")).put("sciserver-test-broker", "someone-else");
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                () -> broker.reserve(id, "session", authorization)).getStatus());
        verify(provider, never()).assertAbsent(any());
    }

    @Test
    void cleanupPersistsExactIdsBeforeDeletionAndSurvivesRestart() {
        JsonNode reservation = broker.reserve(id, "session", authorization);
        String username = reservation.path("username").asText();
        when(provider.identify(eq(username), any())).thenReturn(identity());
        broker.registered(id, "session", authorization);
        doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE)).doNothing()
                .when(provider).delete(eq(username), any());
        assertThrows(ResponseStatusException.class, () -> broker.release(id, authorization));
        assertTrue(job.at("/metadata/annotations/" + AccountBroker.ANNOTATION + "-session")
                .asText().contains("user-id"));
        AccountBroker restarted = new AccountBroker(settings, kube, mapper, provider, clock);
        restarted.sweep();
        assertTrue(job.at("/metadata/annotations/" + AccountBroker.ANNOTATION + "-session")
                .asText().contains("deleted"));
        assertThrows(ResponseStatusException.class, () -> restarted.reserve(id, "session", authorization));
    }

    @Test
    void failedProofCannotTriggerDeletionAndRemainsRetryable() {
        JsonNode reservation = broker.reserve(id, "session", authorization);
        when(provider.identify(eq(reservation.path("username").asText()), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT));
        assertThrows(ResponseStatusException.class, () -> broker.release(id, authorization));
        assertTrue(job.at("/metadata/annotations/" + AccountBroker.ANNOTATION + "-session")
                .asText().contains("deleting"));
        verify(provider, never()).delete(any(), any());
    }

    @Test
    void expiryCleansUnconfirmedRegistrationWithoutRunnerOrAdminSession() {
        JsonNode reservation = broker.reserve(id, "session", authorization);
        when(provider.identify(eq(reservation.path("username").asText()), any())).thenReturn(identity());
        AccountBroker later = new AccountBroker(settings, kube, mapper, provider,
                Clock.offset(clock, java.time.Duration.ofHours(2)));
        later.sweep();
        verify(provider).delete(eq(reservation.path("username").asText()), any());
        assertTrue(job.at("/metadata/annotations/" + AccountBroker.ANNOTATION + "-session")
                .asText().contains("deleted"));
    }

    @Test
    void reservationsDoNotCreateExecutionJobsAndFixtureIdentitiesAreIsolated() {
        when(kube.json(eq("POST"), any(), any())).thenAnswer(invocation -> invocation.getArgument(2));
        JsonNode reservation = broker.create("administrator");
        assertTrue(reservation.has("token"));
        org.mockito.ArgumentCaptor<JsonNode> manifest = org.mockito.ArgumentCaptor.forClass(JsonNode.class);
        verify(kube).json(eq("POST"), any(), manifest.capture());
        assertTrue(manifest.getValue().at("/spec/suspend").asBoolean());
        assertFalse(manifest.getValue().at("/metadata/labels").has("sciserver-test-owner"));
        assertTrue(manifest.getValue().at("/metadata/name").asText().startsWith("test-broker-"));
        JsonNode session = broker.reserve(id, "session", authorization);
        JsonNode logout = broker.reserve(id, "logout", authorization);
        assertNotEquals(session.path("username"), logout.path("username"));
        assertNotEquals(session.path("password"), logout.path("password"));
        assertThrows(ResponseStatusException.class, () -> broker.reserve(id, "arbitrary-scope", authorization));
    }

}
