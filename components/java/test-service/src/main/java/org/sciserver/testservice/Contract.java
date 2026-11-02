package org.sciserver.testservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Consume schemas owned by tests/integration; do not reinterpret runner results. */
@Component
public class Contract {
    private final Map<String, JsonSchema> schemas = new HashMap<>();

    /** Load the runner-owned validation schemas. */
    public Contract() {
        for (String type : new String[] {"catalog", "plan", "report"}) {
            schemas.put(type, JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
                    .getSchema(getClass().getResourceAsStream("/contract/" + type + ".schema.json")));
        }
    }

    /** Validate a runner payload against its versioned schema. */
    public JsonNode validate(String type, JsonNode value) {
        if (value == null || !schemas.get(type).validate(value).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Runner returned an invalid " + type);
        }
        return value;
    }
}
