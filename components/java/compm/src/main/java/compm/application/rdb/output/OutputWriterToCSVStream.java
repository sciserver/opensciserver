package compm.application.rdb.output;

import com.fasterxml.jackson.core.JsonGenerator;
import compm.application.rdb.stream.StreamChannel;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Array;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import org.json.JSONArray;


/**
 * Provides methods for writing the resultset(s) of one or several queries in a
 * SQL statement into a CSV stream.
 */
public class OutputWriterToCSVStream extends OutputWriterToStream {

    private static final String RESULTSET_BASE_NAME = "resultset";
    private PrintStream writer;
    JsonGenerator jg;
    JDBCType[] columnTypes;

    public OutputWriterToCSVStream(StreamChannel streamChannel, Long numRowsPerFlush) throws IOException {
        super(streamChannel, numRowsPerFlush);
        // this.streamChannel = streamChannel;
    }

    @Override
    public void open_() throws IOException {
        writer = new PrintStream(getOutputStream());
    }

    @Override
    public void close_() throws IOException {
        writer.close();
    }

    @Override
    public void flush_() throws IOException {
        writer.flush();
    }

    @Override
    public void cleanUp_() {
        try {
            writer.flush();
        } catch (Exception ex) {
        }
        try {
            writer.close();
        } catch (Exception ex) {
        }
    }

    /**
     * Writes the CSV file header, with a first line containing a table name, and a
     * second line with the column names.
     *
     * @param numColumns        number of columns in the resultset.
     * @param resultSetMetaData has information about the columns in a resultset.
     * @param extraMetadata     stores additional metadata information related to
     *                          the resultset, if required also to be written.
     */
    @Override
    public void initializeResultSet(int numColumns, ResultSetMetaData resultSetMetaData,
                                    LinkedHashMap<String, Object> extraMetadata) 
                                    throws IOException, SQLException, Exception {

        writer.print("#" + RESULTSET_BASE_NAME + getCurrentResultSetNumber());
        writer.println();
        // write column names in header
        for (int i = 1; i <= numColumns; i++) {
            String columnName = resultSetMetaData.getColumnName(i);
            if ("?column?".equals(columnName)) {
                columnName = "column" + i;
            }
            writer.print(columnName);
            if (i != numColumns) {
                writer.print(",");
            }
        }
        writer.println();
    }

    @Override
    public void writeResultSetRow_(ResultSet resultSet) throws SQLException, IOException, Exception {

        int n = resultSet.getMetaData().getColumnCount();
        for (int i = 1; i <= n; i++) {
            writer.print(getValueAsString(resultSet, i));
            if (i != n) {
                writer.print(",");
            }
        }
        writer.println();
    }

    public void finalizeResultSet() throws Exception {
        // After the last written row for CSV, there is nothing else to do.
    }

    /**
     * Converts the a column from a resulset row into a string.
     *
     * @param rs     ResultSet object.
     * @param column index of the column in the `ResultSet`.
     * @return a string representation of the column value.
     */
    private static String getValueAsString(ResultSet rs, int column) throws SQLException {
        Object obj = rs.getObject(column);
        JDBCType columnType = JDBCType.valueOf(rs.getMetaData().getColumnType(column));
        if (obj == null) {
            return "NULL";
        }
        if ("String".equals(obj.getClass().getSimpleName())) {

            /*
             * Enclose the string in double quotes. Escape double quotes and back slashes.
             */
            StringBuilder builder = new StringBuilder();
            builder
                    .append("\"")
                    .append(obj.toString().replaceAll("\\\\", "\\\\\\\\").replaceAll("\\\"", "\\\\\""))
                    .append("\"");

            return builder.toString();

        } else if (JDBCType.ARRAY.equals(columnType)) {
            /* switching from comma delimiter to space delimiter. */
            Array array = (Array) obj;
            return new JSONArray(array.getArray()).toString().replaceAll(",", " ");
        } else {
            return obj.toString();
        }
    }
}


