package org.sciserver.springapp.sciquery.controller;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
import java.io.OutputStream;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * Contains an error message written to a json stream.
 */
@Schema(description = "Contains an error message written to a json stream.")
public class StreamingHttpErrorBody implements StreamingResponseBody {

    private static ObjectMapper objectMapper = new ObjectMapper();
    private String error;

    public StreamingHttpErrorBody(String error) {
        this.error = error;
    }

    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
        JsonGenerator jg = objectMapper.getFactory().createGenerator(outputStream).useDefaultPrettyPrinter();

        jg.writeStartObject();
        jg.writeStringField("status", "error");
        jg.writeStringField("error", error);
        jg.writeEndObject();
        jg.close();        
        outputStream.close();
    }
}
