package org.sciserver.testservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Stateless run-scoped credentials and bounded, intentionally volatile report retention. */
@Component
public class ReportInbox {
    private final Settings settings;
    private final Contract contract;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final Map<String, Entry> reports = new LinkedHashMap<>();

    /** Initialize the inbox using the deployment's stable signing key. */
    @Autowired
    public ReportInbox(Settings settings, Contract contract, ObjectMapper mapper) {
        this(settings, contract, mapper, Clock.systemUTC());
    }

    ReportInbox(Settings settings, Contract contract, ObjectMapper mapper, Clock clock) {
        this.settings = settings;
        this.contract = contract;
        this.mapper = mapper;
        this.clock = clock;
    }

    private byte[] sign(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(settings.signingKey, "HmacSHA256"));
        return mac.doFinal(payload.getBytes(StandardCharsets.US_ASCII));
    }

    /** Issue an opaque credential authorizing only this deployment and run. */
    public String issue(String id) {
        try {
            byte[] bytes = mapper.writeValueAsBytes(Map.of("version", 1, "owner", settings.owner, "runId", id,
                    "expires", clock.instant().getEpochSecond() + settings.deadline + settings.callbackWindow));
            String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            return payload + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(sign(payload));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to issue reporting credential");
        }
    }

    /** Validate without depending on in-memory state, so in-flight tokens survive restarts. */
    public void authorize(String id, String authorization) {
        try {
            if (authorization == null || !authorization.startsWith("Bearer ") || authorization.length() > 8192) {
                throw new IllegalArgumentException();
            }
            String[] parts = authorization.substring(7).split("\\.", -1);
            if (parts.length != 2 || !MessageDigest.isEqual(sign(parts[0]), Base64.getUrlDecoder().decode(parts[1]))) {
                throw new IllegalArgumentException();
            }
            JsonNode payload = mapper.readTree(Base64.getUrlDecoder().decode(parts[0]));
            if (payload.path("version").asInt() != 1 || !id.equals(payload.path("runId").asText())
                    || !settings.owner.equals(payload.path("owner").asText())
                    || !payload.path("expires").isIntegralNumber()
                    || payload.path("expires").asLong() <= clock.instant().getEpochSecond()) {
                throw new IllegalArgumentException();
            }
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired reporting credential");
        }
    }

    private void purge() {
        long now = clock.instant().getEpochSecond();
        reports.values().removeIf(entry -> entry.expires <= now);
    }

    /** Accept a validated immutable report, allowing identical retries but no replacement. */
    public synchronized boolean accept(String id, JsonNode report) {
        try {
            contract.validate("report", report);
        } catch (ResponseStatusException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid report contract");
        }
        if (!id.equals(report.path("runId").asText())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Report run ID mismatch");
        }
        purge();
        try {
            Entry previous = reports.get(id);
            if (previous != null) {
                if (!mapper.readTree(previous.bytes).equals(report)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "A different report was already accepted");
                }
                return false;
            }
            byte[] bytes = mapper.writeValueAsBytes(report);
            if (bytes.length > settings.reportLimit) {
                throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Report exceeds size limit");
            }
            while (reports.size() >= settings.retainedReports) {
                Iterator<String> keys = reports.keySet().iterator();
                keys.next();
                keys.remove();
            }
            reports.put(id, new Entry(bytes, clock.instant().getEpochSecond() + settings.retentionSeconds));
            return true;
        } catch (java.io.IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid report JSON");
        }
    }

    /** Read a retained result, or null after loss, expiry or eviction. */
    public synchronized JsonNode get(String id) {
        purge();
        Entry entry = reports.get(id);
        try {
            return entry == null ? null : mapper.readTree(entry.bytes);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Unreadable retained report");
        }
    }

    private static class Entry {
        private final byte[] bytes;
        private final long expires;

        Entry(byte[] bytes, long expires) {
            this.bytes = bytes;
            this.expires = expires;
        }
    }
}
