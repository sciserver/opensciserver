package org.sciserver.springapp.sciquery.model;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.MediaType;

/**
 * Utility class for handling output formats and their corresponding content type headers.
 */
public class Output {

    /**
    * Enum representing the supported output formats for query results.
    */
    public static enum OutputFormat { JSON, CSV, TSV, VOTABLE, XML, FITS }

    public Output() {
    }

    private static final Map<OutputFormat, MediaType> contentTypeHeader = new HashMap<>();
    
    static {
        contentTypeHeader.put(OutputFormat.JSON, MediaType.APPLICATION_JSON);
        contentTypeHeader.put(OutputFormat.CSV, MediaType.TEXT_PLAIN);
        contentTypeHeader.put(OutputFormat.TSV, MediaType.TEXT_PLAIN);
        contentTypeHeader.put(OutputFormat.VOTABLE, MediaType.APPLICATION_XML);
        contentTypeHeader.put(OutputFormat.XML, MediaType.APPLICATION_XML);
        contentTypeHeader.put(OutputFormat.FITS, MediaType.APPLICATION_OCTET_STREAM);
    }

    public static MediaType getContentTypeHeader(OutputFormat outputFormat) {
        return contentTypeHeader.get(outputFormat);
    }
}
