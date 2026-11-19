package org.sciserver.testservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Run-owned account reservations persisted on existing Kubernetes Jobs, not in memory. */
@Component
public class AccountBroker {
    static final String ANNOTATION = "sciserver-test-account";
    static final java.util.List<String> SCOPES = java.util.List.of("session", "registration", "logout");

    private String annotation(String scope) {
        if (!SCOPES.contains(scope)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown account fixture");
        }
        return ANNOTATION + "-" + scope;
    }

    private static final Logger LOG = LoggerFactory.getLogger(AccountBroker.class);
    private final Settings settings;
    private final Kubernetes kube;
    private final ObjectMapper mapper;
    private final AccountProvider provider;
    private final Clock clock;

    @Autowired
    public AccountBroker(Settings settings, Kubernetes kube, ObjectMapper mapper, AccountProvider provider) {
        this(settings, kube, mapper, provider, Clock.systemUTC());
    }

    AccountBroker(Settings settings, Kubernetes kube, ObjectMapper mapper, AccountProvider provider, Clock clock) {
        this.settings = settings;
        this.kube = kube;
        this.mapper = mapper;
        this.provider = provider;
        this.clock = clock;
    }

    private static String sign(Settings settings, String purpose, String id) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(settings.signingKey, "HmacSHA256"));
            byte[] bytes = mac.doFinal((purpose + ":" + settings.owner + ":" + id)
                    .getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign account capability");
        }
    }

    /** Keep the established signing purpose stable for existing broker capabilities. */
    static String capability(Settings settings, String id) {
        return sign(settings, "account-broker-v1", id);
    }

    private String password(String id, JsonNode lease) {
        // Separators prevent random four-character alphabetic/numeric/keyboard sequences
        // from occasionally violating the unchanged login portal's password rules.
        return "_#Aa1." + sign(settings, "account-password-v1", id + ":" + lease.path("nonce").asText())
                .replaceAll("(.{3})", "$1.");
    }

    private String path(String id) {
        try {
            if (!UUID.fromString(id).toString().equals(id)) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid run ID");
        }
        return "/apis/batch/v1/namespaces/" + settings.namespace + "/jobs/test-broker-" + id;
    }

    private JsonNode job(String id) {
        if (!settings.accountBrokerEnabled) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account broker is disabled");
        }
        JsonNode job = kube.json("GET", path(id), null);
        if (!settings.owner.equals(job.at("/metadata/labels/sciserver-test-broker").asText())
                || !id.equals(job.at("/metadata/labels/sciserver-test-run").asText())
                || !"broker".equals(job.at("/metadata/labels/sciserver-test-kind").asText())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Broker run not found");
        }
        return job;
    }

    private JsonNode authorize(String id, String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.length() > 256
                || !MessageDigest.isEqual(capability(settings, id).getBytes(StandardCharsets.US_ASCII),
                authorization.substring(7).getBytes(StandardCharsets.US_ASCII))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid account broker capability");
        }
        return job(id);
    }

    private ObjectNode read(JsonNode job, String id, String scope) {
        String raw = job.at("/metadata/annotations/" + annotation(scope)).asText();
        if (raw.isBlank()) {
            return null;
        }
        try {
            JsonNode lease = mapper.readTree(raw);
            if (job.at("/metadata/uid").asText().isBlank() || !lease.isObject()
                    || lease.path("contractVersion").asInt() != 1
                    || !id.equals(lease.path("runId").asText())
                    || !job.at("/metadata/uid").asText().equals(lease.path("jobUid").asText())
                    || !lease.path("username").asText().matches("test_user_[a-f0-9]{32}")
                    || !lease.path("nonce").asText().matches("[a-f0-9-]{36}")
                    || !lease.path("expiresAt").isIntegralNumber()) {
                throw new IllegalArgumentException();
            }
            return (ObjectNode) lease;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account ownership ledger is invalid");
        }
    }

    private void save(String id, JsonNode job, ObjectNode lease, String scope) {
        kube.json("PATCH", path(id), mapper.valueToTree(Map.of("metadata", Map.of(
                "resourceVersion", job.at("/metadata/resourceVersion").asText(),
                "annotations", Map.of(annotation(scope), lease.toString())))));
    }

    /** Create a suspended ownership record independently of any execution Job. */
    static JsonNode createRun(Settings settings, Kubernetes kube, ObjectMapper mapper, String id, String requester) {
        if (!settings.accountBrokerEnabled) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Account broker must be configured");
        }
        return kube.json("POST", "/apis/batch/v1/namespaces/" + settings.namespace + "/jobs",
                mapper.valueToTree(Map.of("apiVersion", "batch/v1", "kind", "Job",
                "metadata", Map.of("name", "test-broker-" + id, "labels", Map.of(
                        "sciserver-test-broker", settings.owner, "sciserver-test-run", id,
                        "sciserver-test-kind", "broker"), "annotations", Map.of(
                        "sciserver-test-requester", requester, "sciserver-test-broker-state", "open")),
                "spec", Map.of("suspend", true, "backoffLimit", 0, "template", Map.of("spec", Map.of(
                        "restartPolicy", "Never", "automountServiceAccountToken", false,
                        "containers", java.util.List.of(Map.of("name", "ledger", "image", settings.ledgerImage,
                                "command", java.util.List.of("/bin/false")))))))));
    }

    /** CLI and web orchestration use the same durable run reservation. */
    public JsonNode create(String requester) {
        String id = UUID.randomUUID().toString();
        createRun(settings, kube, mapper, id, requester);
        return mapper.createObjectNode().put("contractVersion", "1").put("runId", id)
                .put("brokerPath", "account-broker/" + id).put("token", capability(settings, id))
                .put("baseUrl", settings.baseUrl);
    }

    private boolean closed(JsonNode job) {
        return !"open".equals(job.at("/metadata/annotations/sciserver-test-broker-state").asText());
    }

    /** Validate the common execution context without provisioning a fixture. */
    public JsonNode context(String id, String authorization) {
        JsonNode record = authorize(id, authorization);
        long expires = java.time.Instant.parse(record.at("/metadata/creationTimestamp").asText()).toEpochMilli()
                + settings.leaseSeconds * 1000L;
        if (closed(record) || expires <= clock.millis()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Broker run is closed or expired");
        }
        return mapper.createObjectNode().put("contractVersion", "1").put("runId", id)
                .put("baseUrl", settings.baseUrl).put("expiresAt", expires);
    }

    /** Persist a reservation before returning credentials for normal registration. */
    public synchronized JsonNode reserve(String id, String scope, String authorization) {
        JsonNode job = authorize(id, authorization);
        annotation(scope);
        ObjectNode lease = read(job, id, scope);
        if (closed(job)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Run no longer accepts account reservations");
        }
        if (lease == null) {
            long expires;
            try {
                expires = java.time.Instant.parse(job.at("/metadata/creationTimestamp").asText()).toEpochMilli()
                        + settings.leaseSeconds * 1000L;
            } catch (Exception exception) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Run creation time is unavailable");
            }
            if (expires <= clock.millis()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Run account reservation has expired");
            }
            String username = "test_user_" + UUID.randomUUID().toString().replace("-", "");
            provider.assertAbsent(username);
            lease = mapper.createObjectNode().put("contractVersion", "1").put("runId", id)
                    .put("jobUid", job.at("/metadata/uid").asText()).put("username", username)
                    .put("nonce", UUID.randomUUID().toString()).put("state", "reserved")
                    .put("expiresAt", expires);
            save(id, job, lease, scope);
        }
        if (!java.util.List.of("reserved", "active").contains(lease.path("state").asText())
                || lease.path("expiresAt").asLong() <= clock.millis()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account reservation is closed");
        }
        ObjectNode response = lease.deepCopy();
        response.remove(java.util.List.of("nonce", "jobUid"));
        response.put("password", password(id, lease));
        response.put("email", lease.path("username").asText() + "@example.com");
        return response;
    }

    /** Record exact IDs after proving ownership with the broker-issued registration password. */
    public synchronized JsonNode registered(String id, String scope, String authorization) {
        JsonNode job = authorize(id, authorization);
        annotation(scope);
        ObjectNode lease = read(job, id, scope);
        if (closed(job) || lease == null
                || !java.util.List.of("reserved", "active").contains(lease.path("state").asText())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account reservation is not open");
        }
        ObjectNode identity = provider.identify(lease.path("username").asText(), password(id, lease));
        if (identity == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Registered account not found");
        }
        lease.set("identity", identity);
        lease.put("state", "active");
        save(id, job, lease, scope);
        return mapper.createObjectNode().put("runId", id).put("state", "active");
    }

    /** Cleanup remains authorized after logout and expiry, but only for this owned run. */
    public synchronized JsonNode release(String id, String authorization) {
        return releaseRun(id, authorize(id, authorization));
    }

    private JsonNode clean(String id, JsonNode job, String scope) {
        annotation(scope);
        ObjectNode lease = read(job, id, scope);
        if (lease == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account reservation not found");
        }
        String username = lease.path("username").asText();
        lease.put("state", "deleting");
        lease.put("lastAttemptAt", clock.millis());
        save(id, job, lease, scope);
        if (!lease.path("identity").isObject()) {
            ObjectNode identity = provider.identify(username, password(id, lease));
            if (identity != null) {
                lease.set("identity", identity);
            }
        }
        // The provider never deletes until exact IDs and proof have been persisted.
        save(id, job(id), lease, scope);
        if (lease.path("identity").isObject()) {
            provider.delete(username, lease.path("identity"));
        }
        JsonNode latest = job(id);
        lease.put("state", "deleted");
        lease.put("nextAuditAt", clock.millis() + 3600000L);
        save(id, latest, lease, scope);
        return mapper.createObjectNode().put("contractVersion", "1").put("runId", id).put("state", "deleted");
    }

    private JsonNode releaseRun(String id, JsonNode record) {
        kube.json("PATCH", path(id), mapper.valueToTree(Map.of("metadata", Map.of(
                "resourceVersion", record.at("/metadata/resourceVersion").asText(), "annotations",
                Map.of("sciserver-test-broker-state", "closing", "sciserver-test-broker-attempt",
                        Long.toString(clock.millis()))))));
        boolean failed = false;
        for (String scope : SCOPES) {
            try {
                JsonNode current = job(id);
                if (read(current, id, scope) != null) {
                    clean(id, current, scope);
                }
            } catch (Exception exception) {
                failed = true;
            }
        }
        if (failed) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Account cleanup pending; broker will retry");
        }
        JsonNode latest = job(id);
        kube.json("PATCH", path(id), mapper.valueToTree(Map.of("metadata", Map.of(
                "resourceVersion", latest.at("/metadata/resourceVersion").asText(), "annotations", Map.of(
                "sciserver-test-broker-state", "deleted", "sciserver-test-broker-audit",
                Long.toString(clock.millis() + 3600000L))))));
        return mapper.createObjectNode().put("contractVersion", "1").put("runId", id).put("state", "deleted");
    }

    /** Retry expired/closed reservations independently of execution location or process lifetime. */
    @Scheduled(initialDelay = 60000, fixedDelay = 60000)
    public synchronized void sweep() {
        if (!settings.accountBrokerEnabled) {
            return;
        }
        try {
            JsonNode jobs = kube.json("GET", "/apis/batch/v1/namespaces/" + settings.namespace
                    + "/jobs?labelSelector=sciserver-test-broker=" + settings.owner, null);
            int attempted = 0;
            java.util.List<JsonNode> candidates = new java.util.ArrayList<>();
            jobs.path("items").forEach(candidates::add);
            candidates.sort(java.util.Comparator.comparingLong(candidate ->
                    candidate.at("/metadata/annotations/sciserver-test-broker-attempt").asLong()));
            for (JsonNode candidate : candidates) {
                String id = candidate.at("/metadata/labels/sciserver-test-run").asText();
                try {
                    long expires = java.time.Instant.parse(candidate.at("/metadata/creationTimestamp").asText())
                            .toEpochMilli() + settings.leaseSeconds * 1000L;
                    if ("deleted".equals(candidate.at("/metadata/annotations/sciserver-test-broker-state").asText())
                            && candidate.at("/metadata/annotations/sciserver-test-broker-audit").asLong()
                            > clock.millis()) {
                        continue;
                    }
                    if (closed(candidate) || expires <= clock.millis()) {
                        if (++attempted > 100) {
                            break;
                        }
                        releaseRun(id, job(id));
                    }
                } catch (Exception exception) {
                    LOG.warn("Account cleanup pending for broker run {}", id);
                }
            }
        } catch (Exception exception) {
            LOG.warn("Account ownership scan failed; cleanup will be retried");
        }
    }
}
