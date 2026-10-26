package org.sciserver.springapp.sciquery.application;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.sciserver.sciquery.metadata.MetadataManager;
import org.sciserver.springapp.sciquery.Config;

/**
 * Manages queries with methods for printing debug information.
 */
public class SciqueryManager {

    private MetadataManager metadataManager;

    public SciqueryManager(MetadataManager metadataManager) {
        this.metadataManager = metadataManager;
    }

    private static ObjectMapper newObjectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.setSerializationInclusion(Include.NON_NULL);
        return om;
    }

    private void printDebug(JsonNode js) {
        if (Config.IS_DEBUG) {
            System.out.println(js);
        }
    }

    private void printDebug(String s) {
        if (Config.IS_DEBUG) {
            System.out.println(s);
        }
    }

}
