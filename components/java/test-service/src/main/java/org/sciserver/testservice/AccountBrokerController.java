package org.sciserver.testservice;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** Narrow runner capabilities, independent of administrator sessions. */
@RestController

public class AccountBrokerController {
    private final AccountBroker broker;
    private final AdminAccess access;

    public AccountBrokerController(AccountBroker broker, AdminAccess access) {
        this.broker = broker;
        this.access = access;
    }

    @PostMapping("/account-broker/{id}/accounts/{scope}")
    public ResponseEntity<JsonNode> reserve(@PathVariable String id,
            @PathVariable String scope,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.status(201).header("Cache-Control", "no-store")
                .body(broker.reserve(id, scope, authorization));
    }

    @PostMapping("/account-broker/{id}/accounts/{scope}/registered")
    public ResponseEntity<JsonNode> registered(@PathVariable String id,
            @PathVariable String scope,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok().header("Cache-Control", "no-store")
                .body(broker.registered(id, scope, authorization));
    }

    @DeleteMapping("/account-broker/{id}")
    public ResponseEntity<JsonNode> release(@PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(broker.release(id, authorization));
    }

    @PostMapping("/api/broker/runs")
    public ResponseEntity<JsonNode> create(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        return ResponseEntity.status(201).header("Cache-Control", "no-store")
                .body(broker.create(access.require(token)));
    }

    @org.springframework.web.bind.annotation.GetMapping("/api/broker/runs/{id}/accounts/{scope}/racm-deletion")
    public ResponseEntity<JsonNode> inspectRacm(@PathVariable String id, @PathVariable String scope,
            @RequestHeader(value = "X-Auth-Token", required = false) String token) {
        access.require(token);
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(broker.inspectRacm(id, scope));
    }

    @org.springframework.web.bind.annotation.GetMapping("/account-broker/{id}")
    public ResponseEntity<JsonNode> context(@PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(broker.context(id, authorization));
    }

}
