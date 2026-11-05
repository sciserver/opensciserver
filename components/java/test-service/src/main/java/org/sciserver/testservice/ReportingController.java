package org.sciserver.testservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Reporting-only authentication, independent of administrator sessions. */
@RestController
public class ReportingController {
    private final ReportInbox inbox;
    private final Runs runs;
    private final Settings settings;
    private final ObjectMapper mapper;

    /** Connect reporting-only authentication to validation and run ownership. */
    public ReportingController(ReportInbox inbox, Runs runs, Settings settings, ObjectMapper mapper) {
        this.inbox = inbox;
        this.runs = runs;
        this.settings = settings;
        this.mapper = mapper;
    }

    /** Accept a bounded report using only the temporary run-scoped credential. */
    @PostMapping(value = "/reporting/{id}", consumes = "application/json")
    public ResponseEntity<Void> report(@PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            HttpServletRequest request) throws IOException {
        inbox.authorize(id, authorization);
        JsonNode job = runs.job(id);
        if (!"run".equals(job.at("/metadata/labels/sciserver-test-kind").asText())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This operation cannot submit a test report");
        }
        byte[] bytes = request.getInputStream().readNBytes(settings.reportLimit + 1);
        if (bytes.length > settings.reportLimit) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Report exceeds size limit");
        }
        JsonNode report;
        try {
            report = mapper.readTree(bytes);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON report");
        }
        return ResponseEntity.status(inbox.accept(id, report) ? HttpStatus.CREATED : HttpStatus.NO_CONTENT).build();
    }
}
