package org.sciserver.testservice;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Administrator API. Browser clients explicitly send X-Auth-Token; cookies are not accepted. */
@RestController
@RequestMapping("/api")
public class TestController {
    private final AdminAccess access;
    private final Runs runs;

    public TestController(AdminAccess access, Runs runs) {
        this.access = access;
        this.runs = runs;
    }

    /** A selection of stable runner test IDs; null selects the full suite. */
    public static class Selection {
        public List<String> tests;
    }

    @GetMapping("/runs")
    public JsonNode list(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        access.require(token);
        return runs.list();
    }

    @PostMapping("/catalog")
    public ResponseEntity<JsonNode> catalog(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        return accepted(runs.create("catalog", null, access.require(token)));
    }

    @PostMapping("/plans")
    public ResponseEntity<JsonNode> plan(@RequestHeader(value = "X-Auth-Token", required = false) String token,
            @RequestBody Selection selection) {
        String user = access.require(token);
        return accepted(runs.create("plan", selection.tests, user));
    }

    @PostMapping("/runs")
    public ResponseEntity<JsonNode> create(@RequestHeader(value = "X-Auth-Token", required = false) String token,
            @RequestBody Selection selection) {
        String user = access.require(token);
        return accepted(runs.create("run", selection.tests, user));
    }

    private ResponseEntity<JsonNode> accepted(JsonNode run) {
        // Relative Location works with any configured ingress prefix.
        return ResponseEntity.accepted().location(URI.create("runs/" + run.path("id").asText())).body(run);
    }

    @GetMapping("/runs/{id}")
    public JsonNode get(@RequestHeader(value = "X-Auth-Token", required = false) String token,
            @PathVariable String id) {
        access.require(token);
        return runs.get(id);
    }

    @GetMapping("/runs/{id}/result")
    public JsonNode result(@RequestHeader(value = "X-Auth-Token", required = false) String token,
            @PathVariable String id) {
        access.require(token);
        return runs.result(id);
    }

    @GetMapping(value = "/runs/{id}/logs", produces = "text/plain")
    public String logs(@RequestHeader(value = "X-Auth-Token", required = false) String token,
            @PathVariable String id) {
        access.require(token);
        return runs.logs(id);
    }

    @DeleteMapping("/runs/{id}")
    public JsonNode cancel(@RequestHeader(value = "X-Auth-Token", required = false) String token,
            @PathVariable String id) {
        access.require(token);
        return runs.cancel(id);
    }
}
