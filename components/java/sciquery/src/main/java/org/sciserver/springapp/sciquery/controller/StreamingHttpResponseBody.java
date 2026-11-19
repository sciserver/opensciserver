package org.sciserver.springapp.sciquery.controller;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.InvalidParameterException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HexFormat; 
import org.sciserver.springapp.sciquery.model.Output.OutputFormat;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * Writes query results into streaming HTTP response body in several formats.
 */
@Schema(description = "Writes query results into streaming HTTP response body in several formats.")
public class StreamingHttpResponseBody implements StreamingResponseBody {

    private static ObjectMapper objectMapper = new ObjectMapper();

    private static final String RESULTSET_BASE_NAME = "resultset";
    private OutputFormat outputFormat;
    private PreparedStatement sqlStatement;
    private String query;
    private boolean isResultSet;
    private long numRowsPerFlush;

    /**
     * Constructor for StreamingHttpResponseBody.
     *
     * @param outputFormat The format of the output.
     * @param sqlStatement Prepared statement from which the results will be fetched.
     * @param query The query string.
     * @param isResultSet Indicates if the result is a ResultSet.
     * @param numRowsPerFlush The number of rows to flush per write operation.
    */
    public StreamingHttpResponseBody(OutputFormat outputFormat, PreparedStatement sqlStatement, 
                                     String query, boolean isResultSet, long numRowsPerFlush) {
        this.outputFormat = outputFormat;
        this.sqlStatement = sqlStatement;
        this.query = query; //sqlStatement.toString();
        this.numRowsPerFlush = numRowsPerFlush;
        this.isResultSet = isResultSet;
    }

    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
        if (this.outputFormat == OutputFormat.JSON) {
            writeJSONArray(outputStream);
        } else if (this.outputFormat == OutputFormat.CSV) {
            writeCSV(outputStream);
        } else {
            throw new InvalidParameterException("Invalid output format " + outputFormat.toString()); 
        }
    }

    /**
     * Writes the data into an output stream in JSON format.
     *
     * @param outputStream  The `outputStream` where the data is written to.
     * @exception IOException If an I/O error occurs.
     */
    public void writeJSONArray(OutputStream outputStream) throws IOException {
        /*        
        JsonGenerator jg = objectMapper.getFactory().createGenerator(outputStream).useDefaultPrettyPrinter();
        jg.writeStartArray();
        for(int i=0; i < 100; i++) {
            jg.writeStartObject();

            jg.writeStringField("Response_State", "Response State - " + i);
            jg.writeStringField("Response_Report", "Response Report - " + i);
            jg.writeStringField("Error_details", "Error Details - " + i);

            jg.writeEndObject();;
            jg.flush();

            try {
                TimeUnit.MILLISECONDS.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        jg.writeEndArray();

        jg.close();
*/            
        //http://www.javased.com/index.php?api=org.codehaus.jackson.JsonGenerator
        //https://www.codota.com/code/java/methods/javax.json.stream.JsonGenerator/write

        JsonGenerator jg = objectMapper.getFactory().createGenerator(outputStream).useDefaultPrettyPrinter();        
        ResultSet resultSet = null;

        try {
            //execute query.
            //boolean isResultSet = this.sqlStatement.execute();

            //start json
            jg.writeStartObject();

            //create "Query" object
            //jg.writeStringField("Query", query);

            //create "Result" array
            jg.writeFieldName("Result");
            jg.writeStartArray();

            //start adding results to "Result" array
            Short resultsetIndex = 0;
            while (true) {     // loop over result sets
                if (isResultSet) {
                    try {
                        resultSet = sqlStatement.getResultSet();
                        resultsetIndex++;        
                        // Start new Results object as element of results array 
                        jg.writeStartObject();

                        jg.writeStringField("TableName", RESULTSET_BASE_NAME + resultsetIndex);
                        jg.writeStringField("Query", query);
                        jg.writeStringField("DatabaseProductName", 
                                            sqlStatement.getConnection().getMetaData().getDatabaseProductName());

                        // starting columns array
                        jg.writeFieldName("ColumnNames");
                        jg.writeStartArray();

                        ResultSetMetaData metadata = resultSet.getMetaData();
                        int n = metadata.getColumnCount();
                        Integer [] columnTypes = new Integer[n];
                        //String [] columnSizes = new String[n];
                        for (Integer i = 1; i <= n; i++) {
                            String columnName = metadata.getColumnName(i);
                            if ("?column?".equals(columnName)) {
                                columnName = "column" + i;
                            }
                            jg.writeString(columnName);
                            columnTypes[i - 1] = metadata.getColumnType(i);
                            //Object obj = resultSet.getObject(i);
                            //System.out.println(obj.toString());
                            //columnSizes[i-1] = metadata.getColumnClassName(i) + "_" + metadata.getC.getColumnLabel(i);
                        }
                        // ending columns array:
                        jg.writeEndArray();

                        jg.writeFieldName("ColumnTypes");
                        jg.writeStartArray();

                        for (Integer i = 1; i <= n; i++) {
                            String typeName = metadata.getColumnTypeName(i);
                            jg.writeString(typeName);
                        }
                        // ending ColumnTypes array:
                        jg.writeEndArray();

                        /*
                        jg.writeFieldName("ColumnSizes");
                        jg.writeStartArray();

                        for (int i=1; i<=n; i++) {
                            jg.writeString(columnSizes[i-1]);
                        }
                        // ending ColumnSizes array:
                        jg.writeEndArray();
                         */

                        jg.flush();
                        outputStream.flush();

                        // starting data array:
                        jg.writeFieldName("Data");
                        jg.writeStartArray();

                        long rowCounter = 0;
                        while (resultSet.next()) {
                            //TimeUnit.MILLISECONDS.sleep(100);
                            jg.writeStartArray();    
                            for (Integer i = 1; i <= n; i++) {
                                //jg.writeString(getValueAsString(resultSet, i));
                                writeJsonField(jg, resultSet, i, columnTypes);
                            }
                            jg.writeEndArray();
                            rowCounter++;

                            //flush rows to stream every certain amount of rows
                            if (rowCounter % this.numRowsPerFlush == 0) {
                                //System.out.println("Flushing in row " + rowCounter);
                                jg.flush();
                                outputStream.flush();
                            }
                        }
                        // ending data array:                        
                        jg.writeEndArray();

                        // Ending new object as element of results array 
                        jg.writeEndObject();

                        //final flush to the stream
                        jg.flush();
                        outputStream.flush();
                    } finally {
                        try {
                            if (resultSet != null) {
                                resultSet.close();
                            }
                        } catch (Exception e) {
                            // ignore
                        }
                    }      

                } else {
                    Integer updateCount = sqlStatement.getUpdateCount();
                    if (updateCount == -1) {
                        break;
                    }
                }
                isResultSet = sqlStatement.getMoreResults();
            }
            //ending the "Results" object:
            jg.writeEndArray();
            
            //ending the response object
            jg.writeEndObject();
            
        } catch (SQLException ex) {
            throw new IOException("Oops, a SQL exception occurred. ", ex);
        } catch (Exception ex) {
            throw new IOException("Oops, an error occurred. ", ex);
        } finally {
            try {
                jg.close();
            } catch (Exception e) {
                // ignore
            }
            try {
                sqlStatement.close();
            } catch (Exception e) {
                // ignore
            }
            try {
                sqlStatement.getConnection().close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    /**
     * Writes a JSON field value based on the column type from the ResultSet.
     *
     * @param jg The JsonGenerator to write to.
     * @param rs The ResultSet containing the data.
     * @param columnIndex The index of the column to read from.
     * @param columnTypes Array of column types for type-specific handling.
     * @throws SQLException If a database access error occurs.
     * @throws IOException If an I/O error occurs.
     */
    private static void writeJsonField(JsonGenerator jg, ResultSet rs, 
                                       Integer columnIndex, Integer [] columnTypes) throws SQLException, IOException {
        
        Integer columnType = columnTypes[columnIndex - 1];

        try {

            Object obj = rs.getObject(columnIndex);
            if (obj == null) {
                jg.writeNull();
                return;
            }
        
            switch (columnType) {
                // TODO: test types
                case Types.BOOLEAN:
                    jg.writeBoolean(rs.getBoolean(columnIndex)); 
                    break;
                case Types.BIT:
                    jg.writeNumber(rs.getInt(columnIndex)); 
                    break;
                case Types.TINYINT:
                case Types.SMALLINT:
                case Types.INTEGER:
                case Types.BIGINT:
                case Types.DOUBLE:
                case Types.REAL:
                case Types.DECIMAL:
                case Types.NUMERIC:
                    jg.writeNumber(obj.toString()); 
                    break;
                case Types.DATE:
                    jg.writeString(rs.getDate(columnIndex).toString()); 
                    break;
                case Types.TIME:
                    jg.writeString(rs.getTime(columnIndex).toString()); 
                    break;
                case Types.TIMESTAMP:
                    jg.writeString(rs.getTimestamp(columnIndex).toString()); 
                    break;
                case Types.BINARY:
                case Types.VARBINARY:
                    //jg.writeString(encodeBinary(rs.getBytes(columnIndex)));
                    jg.writeString(HexFormat.of().withPrefix("0x").withUpperCase().formatHex(rs.getBytes(columnIndex)));
                    break;
                case Types.BLOB:
                    java.sql.Blob blob = rs.getBlob(columnIndex);
                    long len = blob.length();
                    jg.writeString(HexFormat.of().withPrefix("0x").withUpperCase()
                                                                          .formatHex(blob.getBytes(1, (int) len)));
                    break;
                case Types.CLOB:
                    jg.writeString(rs.getClob(columnIndex).toString());
                    break;
                case Types.NCLOB:
                    jg.writeString(rs.getNClob(columnIndex).toString());
                    break;
                case Types.SQLXML:
                    jg.writeString(rs.getSQLXML(columnIndex).toString());
                    break;
                case Types.REF:
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

    /**
     * Writes the data into an output stream in CSV format.
     *
     * @param outputStream The `outputStream` where the data is written to.
     */
    public void writeCSV(OutputStream outputStream) throws IOException {

        PrintStream writer = new PrintStream(outputStream);
        
        ResultSet resultSet = null;
        
        try {
            //execute query and loop over result sets:
            //boolean isResultSet = this.sqlStatement.execute();
            Short resultsetIndex = 0;
            while (true) {
                if (isResultSet) {
                    
                    try {

                        resultSet = sqlStatement.getResultSet();
                        resultsetIndex++;         
                        // write resultset name
                        writer.print("#" + RESULTSET_BASE_NAME + resultsetIndex);
                        writer.println();

                        int n = resultSet.getMetaData().getColumnCount();
                        
                        //write column names in header
                        for (int i = 1; i <= n; i++) {
                            String columnName = resultSet.getMetaData().getColumnName(i);
                            if ("?column?".equals(columnName)) {
                                columnName = "column" + i;
                            }
                            //String typeName = resultSet.getMetaData().getColumnTypeName(i);
                            //writer.print(columnName + "["+ typeName + "]");
                            writer.print(columnName);
                            if (i != n) {
                                writer.print(",");
                            }
                        }
                        writer.println();
                        writer.flush();
                        outputStream.flush();

                        // write data rows:
                        long rowCounter = 0;
                        while (resultSet.next()) {
                            //TimeUnit.MILLISECONDS.sleep(100);
                            for (int i = 1; i <= n; i++) {
                                writer.print(getValueAsString(resultSet, i));
                                if (i != n) {
                                    writer.print(",");
                                }
                            }
                            writer.println();
                            rowCounter++;
                            
                            //flush rows to stream every certain amount of rows
                            if (rowCounter % this.numRowsPerFlush == 0) {
                                //System.out.println("Flushing in row " + rowCounter);
                                writer.flush();
                                outputStream.flush();
                            }
                        }
                        //final flush to the stream
                        writer.flush();
                        outputStream.flush();
                    } finally {
                        try { 
                            if (resultSet != null) {
                                resultSet.close();
                            }
                        } catch (Exception e) { 
                            // ignore
                        }
                    }      
                    
                    
                } else {
                    Integer updateCount = sqlStatement.getUpdateCount();
                    if (updateCount == -1) {
                        break;
                    }
                }
                isResultSet = sqlStatement.getMoreResults();
            }
        } catch (SQLException ex) {
            throw new IOException("Oops, a SQL exception occurrred!. ", ex);
        } finally {
            try { 
                writer.close();
            } catch (Exception e) { 
                // ignore
            }
            try { 
                sqlStatement.close();
            } catch (Exception e) { 
                // ignore
            }
            try { 
                sqlStatement.getConnection().close();
            } catch (Exception e) {
                // ignore
            }
            
        }
    }

    private static String getValueAsString(ResultSet rs, int column) throws SQLException {
        Object obj = rs.getObject(column);
        if (obj == null) {
            return "NULL";
        }

        if ("String".equals(obj.getClass().getSimpleName())) {
            /* Enclose the string in double quotes. Escape double quotes and back slashes. */ 
            StringBuilder builder = new StringBuilder();
            builder.append("\"")
                   .append(obj.toString().replaceAll("\\\\", "\\\\\\\\").replaceAll("\\\"", "\\\\\""))
                   .append("\"");

            return builder.toString();

        } else if ("PgArray".equals(obj.getClass().getSimpleName())) {
            /* switching from comma delimiter to space delimiter. */
            return obj.toString().replace(",", " ");
        } else { 
            return obj.toString();
        }
    }

    private static String encodeBinary(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }



}
