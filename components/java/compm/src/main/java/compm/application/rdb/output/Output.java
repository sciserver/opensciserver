package compm.application.rdb.output;

import java.util.HashMap;
import java.util.Map;
import org.sciserver.racm.jobm.model.RDBTargetModel;
import org.springframework.http.MediaType;


/**
 * Defines the possible output formats of SQL query resultsets, as well as their
 * corresponding media types (to be included as HTTP request content headers).
 */
public class Output {

    /**
     * Lists the possible outout formats.
     */
    public static enum OutputFormat {
        JSON, CSV, TSV, VOTABLE, XML, FITS, DATABASETABLE
    }

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

    /**
     * Gets the media type to be passed as the HTTP request content header, given
     * the output format of that content.
     *
     * @param outputFormat the output format of a SQL query result set.
     * @return the `MediaType` corresponding to the output format.
     */
    public static MediaType getContentTypeHeader(OutputFormat outputFormat) {
        return contentTypeHeader.get(outputFormat);
    }

    private static final Map<String, MediaType> contentType = new HashMap<>();

    static {
        contentType.put(RDBTargetModel.FILE_JSON, MediaType.APPLICATION_JSON);
        contentType.put(RDBTargetModel.FILE_CSV, MediaType.TEXT_PLAIN);
        contentType.put(RDBTargetModel.FILE_TSV, MediaType.TEXT_PLAIN);
        contentType.put(RDBTargetModel.FILE_BSV, MediaType.TEXT_PLAIN);
        contentType.put(RDBTargetModel.FILE_VOTABLE, MediaType.APPLICATION_XML);
        contentType.put(RDBTargetModel.FILE_FITS, MediaType.APPLICATION_OCTET_STREAM);
    }

    /**
     * Gets the media type to be passed as the HTTP request content header, given
     * the target type (defined in RDBTargetModel) of a SQL query resultset.
     *
     * @param targetType output target type of a SQL query result set.
     * @return the `MediaType` corresponding to the target type.
     */
    public static String getContentType(String targetType) {
        return contentType.get(targetType).toString();
    }
}


