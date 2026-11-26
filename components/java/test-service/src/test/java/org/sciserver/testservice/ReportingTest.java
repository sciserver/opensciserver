package org.sciserver.testservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

/** Reporting authentication, idempotency and intentionally volatile retention. */
class ReportingTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final String id = UUID.randomUUID().toString();

    private Settings settings() {
        return new Settings(new MockEnvironment().withProperty("test.namespace", "sciserver")
                .withProperty("test.image", "tests:example").withProperty("test.base-url", "http://example.test")
                .withProperty("test.racm-url", "http://racm").withProperty("test.callback-url", "http://receiver/reporting")
                .withProperty("test.report-signing-key", "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=")
                .withProperty("test.retained-reports", "1").withProperty("test.retention-seconds", "10"));
    }

    private ObjectNode report(String runId) throws Exception {
        ObjectNode report = (ObjectNode) mapper.readTree(getClass().getResourceAsStream(
                "/contract-fixtures/examples/passed.json"));
        report.put("runId", runId);
        return report;
    }

    @Test
    void tokensAreScopedAndExpireButSurviveRestart() {
        Settings settings = settings();
        Clock start = Clock.fixed(Instant.ofEpochSecond(1000), ZoneOffset.UTC);
        ReportInbox first = new ReportInbox(settings, new Contract(), mapper, start);
        String token = "Bearer " + first.issue(id);
        ReportInbox restarted = new ReportInbox(settings, new Contract(), mapper, start);
        restarted.authorize(id, token);
        assertEquals(401, assertThrows(ResponseStatusException.class,
                () -> restarted.authorize(UUID.randomUUID().toString(), token)).getStatus().value());
        assertEquals(401, assertThrows(ResponseStatusException.class,
                () -> restarted.authorize(id, token + "x")).getStatus().value());
        ReportInbox later = new ReportInbox(settings, new Contract(), mapper,
                Clock.offset(start, java.time.Duration.ofSeconds(settings.deadline + settings.callbackWindow)));
        assertEquals(401, assertThrows(ResponseStatusException.class,
                () -> later.authorize(id, token)).getStatus().value());
    }

    @Test
    void reportsAreIdempotentImmutableBoundedAndExpire() throws Exception {
        MutableClock clock = new MutableClock();
        ReportInbox inbox = new ReportInbox(settings(), new Contract(), mapper, clock);
        ObjectNode report = report(id);
        assertTrue(inbox.accept(id, report));
        assertFalse(inbox.accept(id, report.deepCopy()));
        report.put("runnerError", "different report");
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> inbox.accept(id, report)).getStatus().value());
        assertNull(inbox.get(id).get("runnerError").textValue());
        String next = UUID.randomUUID().toString();
        inbox.accept(next, report(next));
        assertNull(inbox.get(id));
        assertNotNull(inbox.get(next));
        clock.now = 11;
        assertNull(inbox.get(next));
    }

    @Test
    void callbackHasSeparateAuthAndLimitsAndAcknowledgesRetries() throws Exception {
        Settings settings = settings();
        ReportInbox inbox = new ReportInbox(settings, new Contract(), mapper);
        Runs runs = mock(Runs.class);
        ObjectNode job = mapper.createObjectNode();
        job.putObject("metadata").putObject("labels").put("sciserver-test-kind", "run");
        when(runs.job(any())).thenReturn(job);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ReportingController(inbox, runs, settings, mapper)).build();
        String url = "/reporting/" + id;
        String auth = "Bearer " + inbox.issue(id);
        String body = report(id).toString();
        mvc.perform(post(url).contentType("application/json").header("X-Auth-Token", "admin-session")
                .content(body)).andExpect(status().isUnauthorized());
        mvc.perform(post(url).contentType("application/json").header("Authorization", auth)
                .content(body)).andExpect(status().isCreated());
        mvc.perform(post(url).contentType("application/json").header("Authorization", auth)
                .content(body)).andExpect(status().isNoContent());
        mvc.perform(post(url).contentType("application/json").header("Authorization", auth)
                .content("{}")).andExpect(status().isBadRequest());
        mvc.perform(post(url).contentType("application/json").header("Authorization", auth)
                .content(new byte[settings.reportLimit + 1])).andExpect(status().isPayloadTooLarge());
        mvc.perform(post(url).contentType("application/json").header("Authorization", auth)
                .content(report(UUID.randomUUID().toString()).toString())).andExpect(status().isBadRequest());
    }

    private static class MutableClock extends Clock {
        private long now;

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochSecond(now);
        }
    }
}
