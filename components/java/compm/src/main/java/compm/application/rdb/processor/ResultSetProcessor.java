package compm.application.rdb.processor;

import compm.application.rdb.output.OutputWriter;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;


/**
 * Used for Processing a SQL query `ResultSet` by initializing its output
 * writers, writing the rows,
 * and finalizing the writing process.
 */
public class ResultSetProcessor {

    ArrayList<OutputWriter> outputsWriters = new ArrayList<OutputWriter>();

    public ResultSetProcessor() {
    }

    /**
     * Adds an `OutputWriter` object to the local list of output writers.
     *
     * @param outputWriter `OutputWriter` object.
     */
    public void addOutputWriter(OutputWriter outputWriter) {
        outputsWriters.add(outputWriter);
    }

    /**
     * Used for initializing, writing the rows, and finalizing the local output
     * writers, which are used for writing the rows coming from a SQL Query
     * resultset.
     *
     * @param resultSet The `resultSet` object to be processed.
     * @param metadata  object with query metadata to be written in the output.
     */
    public void processResultSet(ResultSet resultSet, LinkedHashMap<String, Object> metadata)
                                 throws Exception, SQLException {

        ResultSetMetaData resultSetMetaData = resultSet == null ? null : resultSet.getMetaData();
        for (OutputWriter outputWriter : outputsWriters) {
            outputWriter.initialize(resultSetMetaData, metadata);
        }
        if (resultSet != null) {
            while (resultSet.next()) {
                for (OutputWriter writer : outputsWriters) {
                    writer.writeResultSetRow(resultSet);
                }
            }
        }
        for (OutputWriter writer : outputsWriters) {
            writer.finalize();
        }
    }

}
