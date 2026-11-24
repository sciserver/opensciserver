package org.sciserver.testservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.server.ResponseStatusException;

/** Jobs, reports and cancellation are checked with fake Kubernetes and broker APIs. */
class TestHarnessTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final Kubernetes kube = mock(Kubernetes.class);
    private final AccountBroker broker = mock(AccountBroker.class);
    private final AdminAccess access = mock(AdminAccess.class);
    private final String id = "12345678-1234-1234-1234-123456789012";
    private final String root = "/apis/batch/v1/namespaces/test/jobs";
    private Settings settings;
    private TestHarness harness;
    private ObjectNode job;
    private ObjectNode pod;

    @BeforeEach
    void prepare() throws Exception {
        settings = new Settings(new MockEnvironment().withProperty("test.namespace", "test")
                .withProperty("test.ledger-image", "service:example").withProperty("test.base-url", "http://example.test")
                .withProperty("test.racm-url", "http://racm")
                .withProperty("test.broker-signing-key", "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=")
                .withProperty("test.harness-image", "registry/tests:fixed")
                .withProperty("test.harness-broker-url", "http://service/test-service")
                .withProperty("test.racm-cleanup-enabled", "true")
                .withProperty("test.racm-jdbc-url", "jdbc:sqlserver://unused")
                .withProperty("test.racm-jdbc-user", "cleanup").withProperty("test.racm-jdbc-password", "secret"));
        harness = new TestHarness(settings, kube, broker, access, mapper);
        when(access.require("admin-session")).thenReturn("administrator");
        when(broker.create("administrator")).thenReturn(mapper.createObjectNode().put("runId", id)
                .put("token", "narrow-capability"));
        job = mapper.valueToTree(Map.of("metadata", Map.of("name", "test-run-" + id, "uid", "job-uid",
                "labels", Map.of("sciserver-test-harness", settings.owner, "sciserver-test-run", id)),
                "spec", Map.of(), "status", Map.of("conditions", List.of(
                        Map.of("type", "Complete", "status", "True")))));
        pod = mapper.valueToTree(Map.of("metadata", Map.of("name", "runner-pod", "ownerReferences",
                List.of(Map.of("kind", "Job", "uid", "job-uid"))), "status", Map.of("phase", "Succeeded",
                "containerStatuses", List.of(Map.of("name", "tests", "state", Map.of("terminated",
                        Map.of("exitCode", 0)))))));
        when(kube.json(eq("GET"), eq(root + "/test-run-" + id), any())).thenReturn(job);
        when(kube.json(eq("GET"), eq(root + "?labelSelector=sciserver-test-harness=" + settings.owner), any()))
                .thenReturn(mapper.createObjectNode().set("items", mapper.createArrayNode()));
        when(kube.json(eq("GET"), eq("/api/v1/namespaces/test/pods?labelSelector=job-name=test-run-" + id), any()))
                .thenReturn(mapper.createObjectNode().set("items", mapper.createArrayNode().add(pod)));
    }

    private ObjectNode report() {
        return mapper.valueToTree(Map.of("contractVersion", "1", "runId", id,
                "startedAt", "2026-11-23T00:00:00Z", "finishedAt", "2026-11-23T00:00:01Z",
                "outcome", "passed", "requested", List.of("identity.lifecycle"),
                "tests", List.of(Map.of("id", "identity.lifecycle", "name", "Account lifecycle", "selected", true,
                        "status", "passed", "durationSeconds", 1, "blockedBy", List.of())),
                "counts", Map.of("passed", 1, "failed", 0, "error", 0, "skipped", 0, "blocked", 0)))
                .deepCopy();
    }

    private String validLog() throws Exception {
        ObjectNode report = report();
        ((ObjectNode) report.path("tests").get(0)).putNull("detail");
        report.putNull("runnerError");
        return "normal output\n" + TestHarness.PREFIX + mapper.writeValueAsString(report) + "\n";
    }

    @Test
    void launchIsFixedAndNeverForwardsAdminCredentials() {
        harness.create("admin-session", Map.of("suite", "account-lifecycle"));
        org.mockito.ArgumentCaptor<JsonNode> manifest = org.mockito.ArgumentCaptor.forClass(JsonNode.class);
        verify(kube).json(eq("POST"), eq(root), manifest.capture());
        JsonNode spec = manifest.getValue().path("spec");
        assertEquals(0, spec.path("backoffLimit").asInt());
        assertFalse(spec.at("/template/spec/automountServiceAccountToken").asBoolean());
        assertEquals("registry/tests:fixed", spec.at("/template/spec/containers/0/image").asText());
        assertTrue(spec.toString().contains("identity.lifecycle"));
        assertTrue(spec.toString().contains("narrow-capability"));
        assertFalse(spec.toString().contains("admin-session"));
        assertThrows(ResponseStatusException.class, () -> harness.create("admin-session", Map.of("image", "evil")));
    }

    @Test
    void reportsAreValidatedAndBoundToExitAndRunIdentity() throws Exception {
        String logs = validLog();
        assertEquals("passed", harness.report(logs, id, 0).path("outcome").asText());
        assertThrows(IOException.class, () -> harness.report(logs + logs, id, 0));
        assertThrows(IOException.class, () -> harness.report(logs, "different-run", 0));
        assertThrows(IOException.class, () -> harness.report(logs, id, 137));
        assertThrows(IOException.class, () -> harness.report(TestHarness.PREFIX + "{", id, 0));
        assertThrows(IOException.class, () -> harness.report(logs.replace("\"passed\":1", "\"passed\":2"), id, 0));
    }

    @Test
    void finishedJobWithoutReportNeverBecomesAPassAndForeignPodsAreIgnored() throws Exception {
        when(kube.text(eq("GET"), any(), any())).thenReturn("no report\n");
        assertEquals("missing_or_invalid", harness.get("admin-session", id).path("reportStatus").asText());
        when(kube.text(eq("GET"), any(), any())).thenReturn(validLog());
        assertEquals("available", harness.get("admin-session", id).path("reportStatus").asText());
        ((ObjectNode) pod.at("/metadata/ownerReferences/0")).put("uid", "someone-else");
        assertEquals("pod_unavailable", harness.get("admin-session", id).path("reportStatus").asText());
        ((ObjectNode) job.at("/metadata/labels")).put("sciserver-test-harness", "someone-else");
        assertThrows(ResponseStatusException.class, () -> harness.get("admin-session", id));
    }

    @Test
    void cancellationWaitsForPodTerminationBeforeReleasingAccounts() {
        ((ObjectNode) job.path("status")).set("conditions", mapper.createArrayNode());
        ((ObjectNode) job.path("spec")).put("suspend", true);
        ((ObjectNode) pod.path("status")).put("phase", "Running");
        harness.get("admin-session", id);
        verify(broker, never()).release(any(), any());
        ((ObjectNode) pod.path("status")).put("phase", "Failed");
        assertEquals("deleted", harness.get("admin-session", id).path("cleanup").asText());
        verify(broker).release(eq(id), any());
    }
}
