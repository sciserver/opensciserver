package org.sciserver.testservice;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sciserver.clientutils.SciServerClientException;
import org.sciserver.racm.client.RACMClient;
import org.sciserver.racm.ugm.model.PersonalUserInfo;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

/** Exercise authorization, fixed Job construction, report consumption and lifecycle. */
class ServiceTest {
    private Settings settings;
    private final ObjectMapper mapper = new ObjectMapper();
    private Kubernetes kube;
    private Runs runs;
    private ReportInbox inbox;
    private RACMClient client;
    private AdminAccess access;
    private final String id = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        settings = new Settings(new MockEnvironment().withProperty("test.namespace", "sciserver")
                .withProperty("test.image", "registry/tests@sha256:fixed")
                .withProperty("test.base-url", "http://example.test/sciserver")
                .withProperty("test.racm-url", "http://racm/sciserver/racm")
                .withProperty("test.callback-url", "http://test-service/reporting")
                .withProperty("test.report-signing-key", "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=")
                );
        kube = mock(Kubernetes.class);
        inbox = new ReportInbox(settings, new Contract(), mapper);
        runs = new Runs(settings, kube, mapper, new Contract(), inbox);
        client = mock(RACMClient.class);
        access = new AdminAccess(client);
    }

    @Test
    void allRoutesDenyMissingTokensBeforeOrchestration() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new TestController(access, runs)).build();
        for (String path : List.of("/api/runs", "/api/runs/" + id,
                "/api/runs/" + id + "/result", "/api/runs/" + id + "/logs")) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
        for (String path : List.of("/api/catalog", "/api/plans", "/api/runs")) {
            mvc.perform(post(path).contentType("application/json").content("{}"))
                    .andExpect(status().isUnauthorized());
        }
        verify(kube, never()).json(any(), any(), any());
    }

    @Test
    void racmErrorsPreserveDenialAndHideUpstreamDetails() throws Exception {
        for (int code : new int[] {401, 403, 302, 500, 502}) {
            doThrow(new SciServerClientException("sensitive upstream detail", code))
                    .when(client).getGroupsUserIsMemberOf("token");
            ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> access.require("token"));
            assertEquals(code == 401 || code == 403 ? code : 503, error.getStatus().value());
            assertFalse(error.getReason().contains("sensitive"));
        }
        doThrow(new IllegalArgumentException("malformed payload")).when(client).getGroupsUserIsMemberOf("token");
        assertEquals(503, assertThrows(ResponseStatusException.class,
                () -> access.require("token")).getStatus().value());
    }

    @Test
    void validAdminGetsTrustedIdentity() throws Exception {
        when(client.getGroupsUserIsMemberOf("token")).thenReturn(List.of("public", "admin"));
        when(client.getUser("token")).thenReturn(new PersonalUserInfo(
                1L, "administrator", null, null, null, null, "PUBLIC"));
        assertEquals("administrator", access.require("token"));
    }

    @Test
    void ordinaryMembershipCannotAuthorizeAndMissingIdentityFailsClosed() throws Exception {
        when(client.getGroupsUserIsMemberOf("token")).thenReturn(List.of("public", "administrator"));
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> access.require("token")).getStatus().value());
        verify(client, never()).getUser(any());
        when(client.getGroupsUserIsMemberOf("token")).thenReturn(List.of("admin"));
        assertEquals(503, assertThrows(ResponseStatusException.class,
                () -> access.require("token")).getStatus().value());
    }

    @Test
    void jobsHaveFixedImageTargetAndNoCallerToken() {
        when(kube.json(eq("GET"), any(), isNull()))
                .thenReturn(mapper.createObjectNode().set("items", mapper.createArrayNode()));
        when(kube.json(eq("POST"), any(), any())).thenAnswer(invocation -> invocation.getArgument(2));
        JsonNode run = runs.create("run", List.of("fileservice.upload"), "admin-user");
        assertEquals("run", run.path("kind").asText());
        org.mockito.ArgumentCaptor<JsonNode> manifest = org.mockito.ArgumentCaptor.forClass(JsonNode.class);
        verify(kube).json(eq("POST"), any(), manifest.capture());
        JsonNode spec = manifest.getValue().path("spec");
        assertEquals(0, spec.path("backoffLimit").asInt());
        assertEquals(1860, spec.path("activeDeadlineSeconds").asInt());
        JsonNode pod = spec.path("template").path("spec");
        assertFalse(pod.path("automountServiceAccountToken").asBoolean());
        assertEquals(10001, pod.path("securityContext").path("runAsUser").asInt());
        assertEquals(settings.image, pod.path("containers").get(0).path("image").asText());
        assertEquals("--test", pod.at("/containers/0/args/0").asText());
        assertEquals("fileservice.upload", pod.at("/containers/0/args/1").asText());
        assertTrue(pod.at("/volumes/0/emptyDir").isObject());
        assertFalse(pod.toString().contains("persistentVolumeClaim"));
        assertFalse(pod.toString().contains("X-Auth-Token"));
    }

    private ObjectNode job(String kind) {
        ObjectNode job = mapper.createObjectNode();
        job.putObject("metadata").put("uid", "job-uid").put("resourceVersion", "1").putObject("labels")
                .put("sciserver-test-owner", settings.owner).put("sciserver-test-run", id)
                .put("sciserver-test-kind", kind);
        job.putObject("spec");
        job.putObject("status");
        when(kube.json(eq("GET"), eq("/apis/batch/v1/namespaces/sciserver/jobs/integration-" + id), isNull()))
                .thenReturn(job);
        return job;
    }

    @Test
    void selectionValidationAndActiveLimit() {
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> runs.create("run", List.of(), "admin")).getStatus().value());
        ObjectNode list = mapper.createObjectNode();
        list.putArray("items").add(job("run")).add(job("run"));
        when(kube.json(eq("GET"), eq("/apis/batch/v1/namespaces/sciserver/jobs?labelSelector=sciserver-test-owner="
                + settings.owner), isNull())).thenReturn(list);
        assertEquals(429, assertThrows(ResponseStatusException.class,
                () -> runs.create("run", null, "admin")).getStatus().value());
    }

    @Test
    void unknownOwnerAndMalformedIdsCannotReadOtherJobs() {
        ObjectNode other = job("run");
        ((ObjectNode) other.at("/metadata/labels")).put("sciserver-test-owner", "someone-else");
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> runs.get(id)).getStatus().value());
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> runs.get("../other")).getStatus().value());
    }

    @Test
    void failedJobCanHaveValidAssertionFailureReportAfterRestart() throws Exception {
        ObjectNode job = job("run");
        ((ObjectNode) job.path("status")).putArray("conditions")
                .addObject().put("type", "Failed").put("status", "True").put("reason", "BackoffLimitExceeded");
        ObjectNode report = (ObjectNode) mapper.readTree(getClass().getResourceAsStream(
                "/contract-fixtures/examples/failed.json"));
        report.put("runId", id);
        String token = inbox.issue(id);
        inbox.accept(id, report);
        ReportInbox freshInbox = new ReportInbox(settings, new Contract(), mapper);
        Runs restarted = new Runs(settings, kube, mapper, new Contract(), freshInbox);
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> restarted.result(id)).getStatus().value());
        freshInbox.authorize(id, "Bearer " + token);
        freshInbox.accept(id, report);
        assertEquals("finished", restarted.get(id).path("state").asText());
        assertEquals("failed", restarted.result(id).path("outcome").asText());
        report.put("contractVersion", "999");
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> freshInbox.accept(id, report)).getStatus().value());
    }

    @Test
    void missingReportsAreNotSuccessAndTimeoutIsSeparate() {
        ObjectNode job = job("run");
        ((ObjectNode) job.path("status")).putArray("conditions")
                .addObject().put("type", "Failed").put("status", "True").put("reason", "DeadlineExceeded");
        assertEquals("timed_out", runs.get(id).path("state").asText());
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> runs.result(id)).getStatus().value());
        runs.cancel(id);
        verify(kube, never()).json(eq("PATCH"), any(), any());
    }

    @Test
    void cancellationSuspendsInsteadOfDeletingHistory() {
        final ObjectNode job = job("run");
        runs.cancel(id);
        org.mockito.ArgumentCaptor<JsonNode> patch = org.mockito.ArgumentCaptor.forClass(JsonNode.class);
        verify(kube).json(eq("PATCH"), any(), patch.capture());
        assertTrue(patch.getValue().path("spec").path("suspend").asBoolean());
        ((ObjectNode) job.path("spec")).put("suspend", true);
        assertEquals("cancelled", runs.get(id).path("state").asText());
    }

    @Test
    void discoveryResultsComeFromOwnedRunnerPodAndUseContractSchema() throws Exception {
        ObjectNode job = job("catalog");
        ((ObjectNode) job.path("status")).putArray("conditions")
                .addObject().put("type", "Complete").put("status", "True");
        ObjectNode pods = mapper.createObjectNode();
        ObjectNode metadata = pods.putArray("items").addObject().putObject("metadata");
        metadata.put("name", "runner-pod").putArray("ownerReferences")
                .addObject().put("kind", "Job").put("uid", "job-uid");
        when(kube.json(eq("GET"), eq("/api/v1/namespaces/sciserver/pods?labelSelector=job-name=integration-" + id),
                isNull())).thenReturn(pods);
        JsonNode catalog = mapper.readTree(getClass().getResourceAsStream("/contract-fixtures/catalog.json"));
        when(kube.text(eq("GET"), any(), isNull())).thenReturn(catalog.toString());
        assertEquals(catalog, runs.result(id));
        ((ObjectNode) catalog).put("contractVersion", "unsupported");
        when(kube.text(eq("GET"), any(), isNull())).thenReturn(catalog.toString());
        assertEquals(502, assertThrows(ResponseStatusException.class, () -> runs.result(id)).getStatus().value());
        ((ObjectNode) metadata.path("ownerReferences").get(0)).put("uid", "unrelated-job");
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> runs.logs(id)).getStatus().value());
    }


    @Test
    void deliveryFailureAndRestartLossAreDistinctFromTestOutcome() throws Exception {
        ObjectNode job = job("run");
        ((ObjectNode) job.path("status")).putArray("conditions")
                .addObject().put("type", "Failed").put("status", "True");
        ObjectNode pods = mapper.createObjectNode();
        ObjectNode pod = pods.putArray("items").addObject();
        pod.putObject("metadata").putArray("ownerReferences").addObject().put("uid", "job-uid");
        ObjectNode exit = pod.putObject("status").putArray("containerStatuses").addObject()
                .put("name", "tests").putObject("state").putObject("terminated");
        exit.put("exitCode", 3);
        when(kube.json(eq("GET"), eq("/api/v1/namespaces/sciserver/pods?labelSelector=job-name=integration-" + id),
                isNull())).thenReturn(pods);
        assertEquals("failed", runs.get(id).path("reportDelivery").asText());
        exit.put("exitCode", 0).put("message", mapper.createObjectNode()
                .put("runId", id).put("reportDelivery", "received").toString());
        assertEquals("lost", runs.get(id).path("reportDelivery").asText());
        ObjectNode report = (ObjectNode) mapper.readTree(getClass().getResourceAsStream(
                "/contract-fixtures/examples/passed.json"));
        report.put("runId", id);
        inbox.accept(id, report);
        assertEquals("received", runs.get(id).path("reportDelivery").asText());
        assertEquals("passed", runs.result(id).path("outcome").asText());
    }

}
