package compm.application.rdb.output;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import compm.application.rdb.stream.StreamChannel;
import java.io.IOException;
import java.sql.Array;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;


/**
 * Provides methods for writing the resultset(s) of one or several queries in a
 * SQL statement into a JSON stream.
 */
public class OutputWriterToJSONStream extends OutputWriterToStream {

    private static ObjectMapper objectMapper = new ObjectMapper();
    private static final String RESULTSET_BASE_NAME = "resultset";
    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    JsonGenerator jg;
    String[] columnNames;
    String[] columnLabels;
    JDBCType[] columnTypes;
    Integer[] columnPrecisions;
    Integer[] columnScales;
    LinkedHashMap<String, Object> metadata;

    /**
     * `OutputWriterToJSONStream` constructor.
     *
     * @param streamChannel   stream channel where the output is written as JSON.
     * @param numRowsPerFlush number of rows written at a time before flushing takes place.
     * @param metadata        metadata to be written at the beginning of the JSON.
     */
    public OutputWriterToJSONStream(StreamChannel streamChannel, Long numRowsPerFlush,
            LinkedHashMap<String, Object> metadata) throws IOException {
        super(streamChannel, numRowsPerFlush);
        // this.streamChannel = streamChannel;
        this.metadata = metadata;
    }

    @Override
    public void open_() throws IOException {
        jg = objectMapper.getFactory().createGenerator(getOutputStream()).useDefaultPrettyPrinter();
        jg.writeStartObject();
        for (String key : metadata.keySet()) {
            jg.writeStringField(key, metadata.get(key).toString());
        }
        jg.writeFieldName("Result");
        jg.writeStartArray();
    }

    @Override
    public void close_() throws IOException {
        jg.writeEndArray();
        jg.writeEndObject();
        jg.close();
    }

    @Override
    public void flush_() throws IOException {
        jg.flush();
    }

    @Override
    public void cleanUp_() {
        try {
            jg.flush();
        } catch (Exception ex) {
        }
        try {
            jg.close();
        } catch (Exception ex) {
        }
    }

    /**
     * Starts json object corersponding to the resultset, writes the TableName,
     * ResultSetNumber fields, and extrMetadata fields, and adds arrays containing
     * the column names and their associated metadata.
     *
     * @param numColumns        number of columns in the resultset.
     * @param resultSetMetaData has information about the columns in a resultset.
     * @param extraMetadata     stores additional metadata information related to
     *                          the resultset, if required also to be written.
     */
    @Override
    public void initializeResultSet(int numColumns, ResultSetMetaData resultSetMetaData,
            LinkedHashMap<String, Object> extraMetadata) throws IOException, SQLException, Exception {

        // start the resultset object inside the result array
        jg.writeStartObject();

        jg.writeStringField("TableName", RESULTSET_BASE_NAME + this.getCurrentResultSetNumber());
        jg.writeNumberField("ResultSetNumber", this.getCurrentResultSetNumber());
        // jg.writeStringField("WriteStartTime", dateFormat.format(new Date()));
        for (String key : extraMetadata.keySet()) {
            jg.writeStringField(key, extraMetadata.get(key).toString());
        }

        // Filling up metadata arrays

        columnTypes = new JDBCType[numColumns];
        columnPrecisions = new Integer[numColumns];
        columnScales = new Integer[numColumns];
        columnNames = new String[numColumns];
        columnLabels = new String[numColumns];
        String columnName;
        String columnLabel;

        for (Integer i = 1; i <= numColumns; i++) {
            columnTypes[i - 1] = JDBCType.valueOf(resultSetMetaData.getColumnType(i));
            columnPrecisions[i - 1] = resultSetMetaData.getPrecision(i);
            columnScales[i - 1] = resultSetMetaData.getScale(i);
            columnName = resultSetMetaData.getColumnName(i);
            columnLabel = resultSetMetaData.getColumnLabel(i);
            if ("?column?".equals(columnName) || "".equals(columnName)) {
                columnName = "column" + i;
                columnLabel = columnName;
            }
            columnNames[i - 1] = columnName;
            columnLabels[i - 1] = columnLabel;
        }

        // write metadata arrays
        writeMetadataArray("ColumnNames", columnNames);
        writeMetadataArray("ColumnLabels", columnLabels);
        writeMetadataArray("ColumnTypes", columnTypes);
        writeMetadataArray("ColumnPrecisions", columnPrecisions);
        writeMetadataArray("ColumnScales", columnScales);

        // starting data array:
        jg.writeFieldName("Data");
        jg.writeStartArray();
    }

    /**
     * Writes a json array containing resultset metadata.
     *
     * @param name  name of the json array to be written.
     * @param array array of objects to be written in the JSON output.
     */
    private void writeMetadataArray(String name, Object[] array) throws IOException {
        jg.writeFieldName(name);
        jg.writeStartArray();
        for (Integer i = 0; i < array.length; i++) {
            if (array[i] == null) {
                jg.writeNull();
            } else if (array[i] instanceof Number) {
                jg.writeNumber(array[i].toString());
            } else if (array[i] instanceof Boolean) {
                jg.writeBoolean((Boolean) array[i]);
            } else {
                jg.writeString(array[i].toString());
            }
        }
        jg.writeEndArray();
    }

    /**
     * Writes the next row from a `ResultSet` into this output, and assures row
     * flushing every `numRowsPerFlush` amount of rows.
     *
     * @param resultSet ResultSet object resulting from executing a SQL query (it
     *                  needs its next() method to be already run in order to expose
     *                  the next available row).
     */
    public void writeResultSetRow_(ResultSet resultSet) throws SQLException, IOException, Exception {
        int n = resultSet.getMetaData().getColumnCount();
        jg.writeStartArray();
        for (Integer i = 1; i <= n; i++) {
            // jg.writeString(getValueAsString(resultSet, i));
            writeJsonField(jg, resultSet, i, columnTypes);
        }
        jg.writeEndArray();
    }

    /**
     * Finalizes the writing of a resultset output by closing the JSON array and object.
     */
    public void finalizeResultSet() throws IOException, Exception {
        // ending the data array of the current resultset object:
        jg.writeEndArray();
        // ending the current resultset object:
        jg.writeEndObject();
    }

    /**
     * Writes a column in a resultset into the JSON output.
     *
     * @param jg          JsonGenerator instance used to write the JSON output.
     * @param rs          a `ResultSet` object.
     * @param columnIndex the index of the column in the `ResultSet` that need to be
     *                    written.
     * @param columnTypes The types of the resultSet columns.
     */
    private static void writeJsonField(JsonGenerator jg, ResultSet rs, Integer columnIndex, JDBCType[] columnTypes)
            throws SQLException, IOException {

        JDBCType columnType = columnTypes[columnIndex - 1];

        try {

            Object obj = rs.getObject(columnIndex);
            if (obj == null) {
                jg.writeNull();
                return;
            }

            switch (columnType) {
                // TODO: test types
                case ARRAY:
                    Array array = rs.getArray(columnIndex);
                    jg.writeObject((array.getArray()));
                    break;
                case BOOLEAN:
                    jg.writeBoolean(rs.getBoolean(columnIndex));
                    break;
                case BIT:
                    jg.writeNumber(rs.getInt(columnIndex));
                    break;
                case TINYINT:
                case SMALLINT:
                case INTEGER:
                case BIGINT:
                case DOUBLE:
                case REAL:
                case DECIMAL:
                case NUMERIC:
                    jg.writeNumber(obj.toString());
                    break;
                case DATE:
                    jg.writeString(rs.getDate(columnIndex).toString());
                    break;
                case TIME:
                    jg.writeString(rs.getTime(columnIndex).toString());
                    break;
                case TIMESTAMP:
                    jg.writeString(rs.getTimestamp(columnIndex).toString());
                    break;
                case BINARY:
                case VARBINARY:
                    jg.writeString(rs.getBytes(columnIndex).toString());
                    break;
                case BLOB:
                    jg.writeString(rs.getBlob(columnIndex).toString());
                    break;
                case CLOB:
                    jg.writeString(rs.getClob(columnIndex).toString());
                    break;
                case NCLOB:
                    jg.writeString(rs.getNClob(columnIndex).toString());
                    break;
                case SQLXML:
                    jg.writeString(rs.getSQLXML(columnIndex).toString());
                    break;
                case REF:
                    jg.writeString(rs.getRef(columnIndex).toString());
                    break;
                default:
                    jg.writeString(obj.toString());
                    break;
            }
        } catch (Exception e) {
            jg.writeString("UNKNOWN_VALUE");
        }
    }
}


