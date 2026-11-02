package org.sciserver.testservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Persist orchestration in Jobs; receive reports independently through callbacks. */
@Component
public class Runs {
    private final Settings settings;
    private final Kubernetes kube;
    private final ObjectMapper mapper;
    private final Contract contract;
    private final ReportInbox inbox;

    /** Connect orchestration, persistence and runner contract validation. */
    public Runs(Settings settings, Kubernetes kube, ObjectMapper mapper, Contract contract, ReportInbox inbox) {
        this.settings = settings;
        this.kube = kube;
        this.mapper = mapper;
        this.contract = contract;
        this.inbox = inbox;
    }

    private String jobs() {
        return "/apis/batch/v1/namespaces/" + settings.namespace + "/jobs";
    }

    private String pods() {
        return "/api/v1/namespaces/" + settings.namespace + "/pods";
    }

    private String name(String id) {
        try {
            if (!UUID.fromString(id).toString().equals(id)) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid run ID");
        }
        return "integration-" + id;
    }

    private String selector() {
        return "sciserver-test-owner=" + settings.owner;
    }

    JsonNode job(String id) {
        JsonNode job = kube.json("GET", jobs() + "/" + name(id), null);
        if (!settings.owner.equals(job.at("/metadata/labels/sciserver-test-owner").asText())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Run not found");
        }
        return job;
    }

    private boolean condition(JsonNode job, String type) {
        for (JsonNode condition : job.path("status").path("conditions")) {
            if (type.equals(condition.path("type").asText()) && "True".equals(condition.path("status").asText())) {
                return true;
            }
        }
        return false;
    }

    private String state(JsonNode job) {
        if (condition(job, "Complete")) {
            return "finished";
        }
        if (condition(job, "Failed")) {
            for (JsonNode condition : job.path("status").path("conditions")) {
                if ("DeadlineExceeded".equals(condition.path("reason").asText())) {
                    return "timed_out";
                }
            }
            return "finished"; // A failed process may still have a valid test-failure report.
        }
        if (job.path("spec").path("suspend").asBoolean(false)) {
            return job.path("status").path("active").asInt() > 0 ? "cancelling" : "cancelled";
        }
        return job.path("status").path("active").asInt() > 0 ? "running" : "queued";
    }

    private boolean terminal(JsonNode job) {
        return List.of("finished", "cancelled", "timed_out").contains(state(job));
    }

    private ObjectNode describe(JsonNode job) {
        ObjectNode response = mapper.createObjectNode();
        response.put("id", job.at("/metadata/labels/sciserver-test-run").asText());
        response.put("kind", job.at("/metadata/labels/sciserver-test-kind").asText());
        response.put("state", state(job));
        String id = job.at("/metadata/labels/sciserver-test-run").asText();
        response.put("reportDelivery", !"run".equals(job.at("/metadata/labels/sciserver-test-kind").asText())
                ? "not_applicable" : inbox.get(id) != null ? "received" : terminal(job) ? "unavailable" : "pending");
        response.put("requestedBy", job.at("/metadata/annotations/sciserver-test-requester").asText());
        response.set("createdAt", job.at("/metadata/creationTimestamp"));
        response.set("conditions", job.path("status").path("conditions"));
        response.put("image", job.at("/spec/template/spec/containers/0/image").asText());
        response.set("arguments", job.at("/spec/template/spec/containers/0/args"));
        return response;
    }

    /** List operations belonging to this service deployment. */
    public ArrayNode list() {
        ArrayNode result = mapper.createArrayNode();
        for (JsonNode job : kube.json("GET", jobs() + "?labelSelector=" + selector(), null).path("items")) {
            result.add(describe(job));
        }
        return result;
    }

    /** Inspect execution and report delivery independently, including retained pod receipts. */
    public JsonNode get(String id) {
        JsonNode existing = job(id);
        ObjectNode response = describe(existing);
        if ("run".equals(response.path("kind").asText()) && inbox.get(id) == null) {
            JsonNode pods = kube.json("GET", pods() + "?labelSelector=job-name=" + name(id), null);
            if (pods != null) {
                for (JsonNode pod : pods.path("items")) {
                    for (JsonNode owner : pod.at("/metadata/ownerReferences")) {
                        if (!existing.at("/metadata/uid").asText().equals(owner.path("uid").asText())) {
                            continue;
                        }
                        for (JsonNode container : pod.at("/status/containerStatuses")) {
                            if (!"tests".equals(container.path("name").asText())) {
                                continue;
                            }
                            JsonNode exit = container.at("/state/terminated");
                            if (!exit.isMissingNode()) {
                                response.set("runnerExitCode", exit.path("exitCode"));
                                response.put("reportDelivery",
                                        exit.path("exitCode").asInt() == 3 ? "failed" : "unavailable");
                                try {
                                    JsonNode receipt = mapper.readTree(exit.path("message").asText());
                                    if (receipt != null && id.equals(receipt.path("runId").asText())
                                            && "received".equals(receipt.path("reportDelivery").asText())) {
                                        response.put("reportDelivery", "lost");
                                    }
                                } catch (IOException ignored) {
                                    // Exit code and Job conditions still explain a runner without a receipt.
                                }
                            }
                        }
                    }
                }
            }
        }
        return response;
    }

    /** Create one fixed-template operation after enforcing admission limits. */
    public synchronized JsonNode create(String kind, List<String> selection, String requester) {
        if (!List.of("catalog", "plan", "run").contains(kind)) {
            throw new IllegalArgumentException("Unknown operation");
        }
        if (selection != null && (selection.isEmpty() || selection.size() > 200
                || selection.stream().anyMatch(id -> id == null || !id.matches("[a-zA-Z0-9][a-zA-Z0-9._-]{0,127}")))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid test selection");
        }
        int active = 0;
        for (JsonNode item : kube.json("GET", jobs() + "?labelSelector=" + selector(), null).path("items")) {
            if (!terminal(item)) {
                active++;
            }
        }
        if (active >= settings.maxActive) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Active run limit reached");
        }
        String id = UUID.randomUUID().toString();
        final Map<String, String> labels = Map.of("sciserver-test-owner", settings.owner,
                "sciserver-test-run", id, "sciserver-test-kind", kind);
        List<String> args = new ArrayList<>();
        if ("catalog".equals(kind)) {
            args.add("--list");
        } else {
            if ("plan".equals(kind)) {
                args.add("--plan");
            }
            if (selection != null) {
                for (String test : selection) {
                    args.add("--test");
                    args.add(test);
                }
            }
        }
        List<Map<String, String>> environment = new ArrayList<>(List.of(
                Map.of("name", "SCISERVER_BASE_URL", "value", settings.baseUrl),
                Map.of("name", "TEST_RUN_ID", "value", id),
                Map.of("name", "TEST_REPORT_PATH", "value", "/results/report.json"),
                Map.of("name", "TEST_REPORT_DELIVERY_PATH", "value", "/results/delivery.json")));
        if ("run".equals(kind)) {
            environment.add(Map.of("name", "TEST_REPORT_CALLBACK_URL", "value", settings.callbackUrl + "/" + id));
            environment.add(Map.of("name", "TEST_REPORT_CALLBACK_TOKEN", "value", inbox.issue(id)));
        }
        Map<String, Object> container = Map.of("name", "tests", "image", settings.image,
                "imagePullPolicy", "Always", "args", args,
                "env", environment,
                "terminationMessagePath", "/results/delivery.json",
                "volumeMounts", List.of(Map.of("name", "results", "mountPath", "/results")),
                "resources", Map.of("requests", Map.of("cpu", "50m", "memory", "64Mi"),
                        "limits", Map.of("cpu", "500m", "memory", "256Mi")),
                "securityContext", Map.of("allowPrivilegeEscalation", false,
                        "capabilities", Map.of("drop", List.of("ALL"))));
        ObjectNode pod = mapper.valueToTree(Map.of("restartPolicy", "Never",
                "automountServiceAccountToken", false, "containers", List.of(container),
                "securityContext", Map.of("runAsNonRoot", true, "runAsUser", 10001,
                        "runAsGroup", 10001, "fsGroup", 10001, "seccompProfile", Map.of("type", "RuntimeDefault")),
                "volumes", List.of(Map.of("name", "results",
                        "emptyDir", Map.of("sizeLimit", "32Mi")))));
        if (!settings.pullSecret.isBlank()) {
            pod.set("imagePullSecrets", mapper.valueToTree(List.of(Map.of("name", settings.pullSecret))));
        }
        JsonNode manifest = mapper.valueToTree(Map.of("apiVersion", "batch/v1", "kind", "Job",
                "metadata", Map.of("name", name(id), "labels", labels,
                        "annotations", Map.of("sciserver-test-requester", requester)),
                "spec", Map.of("backoffLimit", 0, "activeDeadlineSeconds", settings.deadline + 60,
                        "template", Map.of("metadata", Map.of("labels", labels), "spec", pod))));
        return describe(kube.json("POST", jobs(), manifest));
    }

    /** Suspend an active Job while preserving its history. */
    public JsonNode cancel(String id) {
        JsonNode existing = job(id);
        if (!terminal(existing)) {
            // Suspension terminates active pods while retaining Job identity and provenance.
            // resourceVersion makes a concurrent completion/update a conflict instead of a blind overwrite.
            kube.json("PATCH", jobs() + "/" + name(id), mapper.valueToTree(Map.of(
                    "metadata", Map.of("resourceVersion", existing.at("/metadata/resourceVersion").asText(),
                            "annotations", Map.of("sciserver-test-cancelled-at", Instant.now().toString())),
                    "spec", Map.of("suspend", true))));
        }
        return get(id);
    }

    /** Read bounded logs from a pod owned by this Job. */
    public String logs(String id) {
        return readLogs(id, false);
    }

    private String readLogs(String id, boolean result) {
        JsonNode existing = job(id);
        String uid = existing.at("/metadata/uid").asText();
        JsonNode items = kube.json("GET", pods() + "?labelSelector=job-name=" + name(id), null).path("items");
        for (JsonNode pod : items) {
            for (JsonNode owner : pod.path("metadata").path("ownerReferences")) {
                if (uid.equals(owner.path("uid").asText()) && "Job".equals(owner.path("kind").asText())) {
                    return kube.text("GET", pods() + "/" + pod.path("metadata").path("name").asText()
                            + "/log?container=tests&limitBytes=" + (result ? RemoteHttp.MAX_BYTES : 1048576)
                            + (result ? "" : "&tailLines=2000"), null);
                }
            }
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Runner pod is not available yet");
    }

    /** Retrieve and validate the completed operation result. */
    public JsonNode result(String id) {
        JsonNode existing = job(id);
        if (!terminal(existing)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Run has not finished");
        }
        String kind = existing.at("/metadata/labels/sciserver-test-kind").asText();
        try {
            if (!"run".equals(kind)) {
                if (!condition(existing, "Complete")) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Discovery failed; inspect run logs");
                }
                return contract.validate("catalog".equals(kind) ? "catalog" : "plan",
                        mapper.readTree(readLogs(id, true)));
            }
            JsonNode report = inbox.get(id);
            if (report == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "No retained report; inspect delivery and Job state");
            }
            return report;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unreadable runner result");
        }
    }
}
