package org.sciserver.testservice;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Fixed-image harness: Kubernetes owns execution/history, pod logs own the final report. */
@RestController
@RequestMapping("/api/test-runs")
public class TestHarness {
    static final String PREFIX = "SCISERVER_REPORT_V1 ";
    private final Settings settings;
    private final Kubernetes kube;
    private final AccountBroker broker;
    private final AdminAccess access;
    private final ObjectMapper mapper;
    private final JsonSchema schema;

    /** Share authorization and broker ownership, without forwarding administrator sessions to pods. */
    public TestHarness(Settings settings, Kubernetes kube, AccountBroker broker, AdminAccess access,
            ObjectMapper mapper) throws IOException {
        this.settings = settings;
        this.kube = kube;
        this.broker = broker;
        this.access = access;
        this.mapper = mapper;
        try (java.io.InputStream input = getClass().getResourceAsStream("/report.schema.json")) {
            if (input == null) {
                throw new IOException("Runner report schema missing");
            }
            schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
                    .getSchema(mapper.readTree(input));
        }
    }

    private String root() {
        return "/apis/batch/v1/namespaces/" + settings.namespace + "/jobs";
    }

    private String path(String id) {
        try {
            if (!UUID.fromString(id).toString().equals(id)) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid run ID");
        }
        return root() + "/test-run-" + id;
    }

    private JsonNode owned(String id) {
        JsonNode job = kube.json("GET", path(id), null);
        if (!settings.owner.equals(job.at("/metadata/labels/sciserver-test-harness").asText())
                || !id.equals(job.at("/metadata/labels/sciserver-test-run").asText())
                || job.at("/metadata/uid").asText().isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Test run not found");
        }
        return job;
    }

    private List<JsonNode> jobs() {
        List<JsonNode> result = new ArrayList<>();
        kube.json("GET", root() + "?labelSelector=sciserver-test-harness=" + settings.owner, null)
                .path("items").forEach(result::add);
        return result;
    }

    private boolean terminal(JsonNode job) {
        if (job.at("/spec/suspend").asBoolean()) {
            return true;
        }
        for (JsonNode condition : job.path("status").path("conditions")) {
            if ("True".equals(condition.path("status").asText())
                    && List.of("Complete", "Failed").contains(condition.path("type").asText())) {
                return true;
            }
        }
        return false;
    }

    private ObjectNode summary(JsonNode job) {
        return mapper.createObjectNode().put("id", job.at("/metadata/labels/sciserver-test-run").asText())
                .put("createdAt", job.at("/metadata/creationTimestamp").asText())
                .put("suite", job.at("/metadata/annotations/sciserver-test-suite").asText())
                .put("state", job.at("/spec/suspend").asBoolean() ? "cancelling"
                        : terminal(job) ? "finished" : "pending_or_running");
    }

    /** List recent service-created runs, excluding the broker's suspended ownership records. */
    @GetMapping
    public JsonNode list(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        access.require(token);
        List<JsonNode> entries = jobs();
        entries.sort(java.util.Comparator.comparing((JsonNode job) ->
                job.at("/metadata/creationTimestamp").asText()).reversed());
        return mapper.valueToTree(entries.stream().limit(20).map(this::summary).toList());
    }

    /** Launch one of two fixed suites with a deployment-configured image and target. */
    @PostMapping
    public synchronized JsonNode create(@RequestHeader(value = "X-Auth-Token", required = false) String token,
            @RequestBody Map<String, String> request) {
        final String requester = access.require(token);
        String suite = request.getOrDefault("suite", "account-lifecycle");
        if (request.size() > 1 || request.keySet().stream().anyMatch(key -> !"suite".equals(key))
                || suite == null || !List.of("account-lifecycle", "full").contains(suite)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown suite or request field");
        }
        if (settings.harnessImage.isBlank() || ("account-lifecycle".equals(suite) && !settings.racmCleanupEnabled)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Configure the runner image and RACM cleanup first");
        }
        if (jobs().stream().anyMatch(job -> !terminal(job))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A test run is already active");
        }
        JsonNode reservation = broker.create(requester);
        String id = reservation.path("runId").asText();
        List<Map<String, String>> env = new ArrayList<>(List.of(
                Map.of("name", "TEST_RUN_ID", "value", id),
                Map.of("name", "TEST_ACCOUNT_BROKER_URL", "value", settings.harnessBrokerUrl
                        + "/account-broker/" + id),
                Map.of("name", "TEST_ACCOUNT_BROKER_TOKEN", "value", reservation.path("token").asText()),
                Map.of("name", "SCISERVER_BASE_URL", "value", settings.baseUrl),
                Map.of("name", "TEST_REPORT_PATH", "value", "/results/report.json"),
                Map.of("name", "TEST_LOG_REPORT", "value", "true")));
        if ("account-lifecycle".equals(suite)) {
            env.add(Map.of("name", "TEST_IDS", "value", "[\"identity.lifecycle\"]"));
        }
        ObjectNode pod = mapper.valueToTree(Map.of("restartPolicy", "Never", "automountServiceAccountToken", false,
                "securityContext", Map.of("runAsNonRoot", true, "runAsUser", 10001, "runAsGroup", 10001,
                        "fsGroup", 10001, "seccompProfile", Map.of("type", "RuntimeDefault")),
                "containers", List.of(Map.of("name", "tests", "image", settings.harnessImage,
                        "imagePullPolicy", "Always", "env", env,
                        "securityContext", Map.of("allowPrivilegeEscalation", false,
                                "readOnlyRootFilesystem", true, "capabilities", Map.of("drop", List.of("ALL"))),
                        "resources", Map.of("requests", Map.of("cpu", "50m", "memory", "64Mi"),
                                "limits", Map.of("cpu", "500m", "memory", "256Mi")),
                        "volumeMounts", List.of(Map.of("name", "results", "mountPath", "/results"),
                                Map.of("name", "tmp", "mountPath", "/tmp")))),
                "volumes", List.of(Map.of("name", "results", "emptyDir", Map.of("sizeLimit", "32Mi")),
                        Map.of("name", "tmp", "emptyDir", Map.of("sizeLimit", "32Mi")))));
        if (!settings.harnessPullSecret.isBlank()) {
            pod.set("imagePullSecrets", mapper.valueToTree(List.of(Map.of("name", settings.harnessPullSecret))));
        }
        JsonNode job = mapper.valueToTree(Map.of("apiVersion", "batch/v1", "kind", "Job", "metadata", Map.of(
                "name", "test-run-" + id, "labels", Map.of("sciserver-test-harness", settings.owner,
                        "sciserver-test-run", id), "annotations", Map.of("sciserver-test-requester", requester,
                        "sciserver-test-suite", suite)), "spec", Map.of("backoffLimit", 0,
                        "activeDeadlineSeconds", settings.harnessDeadline, "template", Map.of("spec", pod))));
        // Ambiguous create failures leave the independent broker record for expiry/reconciliation.
        kube.json("POST", root(), job);
        return mapper.createObjectNode().put("id", id).put("state", "pending_or_running");
    }

    private List<JsonNode> pods(JsonNode job) {
        String uid = job.at("/metadata/uid").asText();
        String name = job.at("/metadata/name").asText();
        List<JsonNode> result = new ArrayList<>();
        for (JsonNode pod : kube.json("GET", "/api/v1/namespaces/" + settings.namespace
                + "/pods?labelSelector=job-name=" + name, null).path("items")) {
            for (JsonNode owner : pod.path("metadata").path("ownerReferences")) {
                if (uid.equals(owner.path("uid").asText()) && "Job".equals(owner.path("kind").asText())) {
                    result.add(pod);
                    break;
                }
            }
        }
        return result;
    }

    private String cleanup(String id, JsonNode job, List<JsonNode> pods) {
        if ("deleted".equals(job.at("/metadata/annotations/sciserver-test-cleanup").asText())) {
            return "deleted";
        }
        boolean stopped = pods.stream().allMatch(pod ->
                List.of("Succeeded", "Failed").contains(pod.at("/status/phase").asText()));
        if (!terminal(job) || !stopped) {
            return "waiting_for_execution";
        }
        try {
            broker.release(id, "Bearer " + AccountBroker.capability(settings, id));
            kube.json("PATCH", path(id), mapper.valueToTree(Map.of("metadata", Map.of("annotations",
                    Map.of("sciserver-test-cleanup", "deleted")))));
            return "deleted";
        } catch (Exception exception) {
            return "pending";
        }
    }

    JsonNode report(String logs, String id, int exitCode) throws IOException {
        List<String> records = logs.lines().filter(line -> line.startsWith(PREFIX)).toList();
        if (records.size() != 1 || records.get(0).length() > 256 * 1024 + PREFIX.length()) {
            throw new IOException("Missing, truncated or duplicate report");
        }
        JsonNode report = mapper.reader()
                .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .readTree(records.get(0).substring(PREFIX.length()));
        if (!schema.validate(report).isEmpty() || !id.equals(report.path("runId").asText())
                || exitCode != switch (report.path("outcome").asText()) {
                case "passed" -> 0;
                case "failed" -> 1;
                default -> 2;
            }) {
            throw new IOException("Invalid report identity, schema or exit code");
        }
        for (String status : List.of("passed", "failed", "error", "skipped", "blocked")) {
            long count = 0;
            for (JsonNode test : report.path("tests")) {
                if (status.equals(test.path("status").asText())) {
                    count++;
                }
            }
            if (count != report.path("counts").path(status).asLong()) {
                throw new IOException("Invalid report counts");
            }
        }
        if ("passed".equals(report.path("outcome").asText())
                && (report.path("tests").isEmpty() || report.path("counts").path("failed").asInt() > 0
                || report.path("counts").path("error").asInt() > 0
                || report.path("counts").path("blocked").asInt() > 0)) {
            throw new IOException("Inconsistent passing report");
        }
        return report;
    }

    /** Poll state and bounded pod logs; a finished Job alone is never a passing test. */
    @GetMapping("/{id}")
    public JsonNode get(@RequestHeader(value = "X-Auth-Token", required = false) String token,
            @PathVariable String id) {
        access.require(token);
        JsonNode job = owned(id);
        List<JsonNode> pods = pods(job);
        ObjectNode result = summary(job).put("cleanup", cleanup(id, job, pods)).put("logs", "")
                .put("reportStatus", "pending");
        if (job.at("/spec/suspend").asBoolean() && "deleted".equals(result.path("cleanup").asText())) {
            result.put("state", "cancelled");
        }
        if (terminal(job)) {
            result.put("reportStatus", "execution_ended_without_report");
        }
        result.set("conditions", job.path("status").path("conditions"));
        if (pods.size() == 1) {
            JsonNode pod = pods.get(0);
            result.set("podStatus", pod.path("status"));
            for (JsonNode container : pod.path("status").path("containerStatuses")) {
                if (!"tests".equals(container.path("name").asText())) {
                    continue;
                }
                JsonNode terminated = container.path("state").path("terminated");
                if (!terminated.isMissingNode()) {
                    result.put("exitCode", terminated.path("exitCode").asInt());
                    result.put("state", job.at("/spec/suspend").asBoolean() ? "cancelled" : "finished");
                }
                try {
                    String name = pod.at("/metadata/name").asText();
                    if (!name.matches("[a-z0-9-]{1,253}")) {
                        throw new IOException("Invalid pod name");
                    }
                    String logs = kube.text("GET", "/api/v1/namespaces/" + settings.namespace
                            + "/pods/" + name + "/log?container=tests&limitBytes=2097152", null)
                            .replace(AccountBroker.capability(settings, id), "[REDACTED]");
                    result.put("logs", logs);
                    if (!terminated.isMissingNode()) {
                        try {
                            result.set("report", report(logs, id, terminated.path("exitCode").asInt()));
                            result.put("reportStatus", "available");
                        } catch (IOException exception) {
                            result.put("reportStatus", "missing_or_invalid");
                        }
                    }
                } catch (Exception exception) {
                    result.put("reportStatus", "logs_unavailable");
                }
            }
        } else if (terminal(job)) {
            result.put("reportStatus", pods.isEmpty() ? "pod_unavailable" : "ambiguous_pods");
        }
        return result;
    }

    /** Suspend execution; broker cleanup waits until the runner pod has stopped. */
    @DeleteMapping("/{id}")
    public JsonNode cancel(@RequestHeader(value = "X-Auth-Token", required = false) String token,
            @PathVariable String id) {
        access.require(token);
        JsonNode job = owned(id);
        if (terminal(job)) {
            return summary(job);
        }
        kube.json("PATCH", path(id), mapper.valueToTree(Map.of("spec", Map.of("suspend", true))));
        return mapper.createObjectNode().put("id", id).put("state", "cancelling");
    }

    /** Cleanup still progresses when the browser is closed or the service restarts. */
    @Scheduled(initialDelay = 15000, fixedDelay = 15000)
    public void reap() {
        try {
            for (JsonNode candidate : jobs().stream().filter(job ->
                    !"deleted".equals(job.at("/metadata/annotations/sciserver-test-cleanup").asText()))
                    .limit(100).toList()) {
                String id = candidate.at("/metadata/labels/sciserver-test-run").asText();
                JsonNode job = owned(id);
                if (terminal(job)) {
                    cleanup(id, job, pods(job));
                }
            }
        } catch (Exception exception) {
            // Kubernetes/broker outages leave durable Jobs and account ownership records for retry.
        }
    }
}
