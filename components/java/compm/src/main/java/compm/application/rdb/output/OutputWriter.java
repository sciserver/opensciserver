package compm.application.rdb.output;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.LinkedHashMap;


/**
 * Provides methods for writing the resultset(s) of one or several queries in a
 * SQL statement into a user-predefined output.
 */
public abstract class OutputWriter {

    private int numberOfResultSetsToWrite = 0;
    private int currentResultSetNumber = 0;
    private Long numRowsWritten = 0L;
    private Long numRowsPerFlush = Long.MAX_VALUE;

    public OutputWriter(Long numRowsPerFlush) {
        this.numRowsPerFlush = numRowsPerFlush == null ? Long.MAX_VALUE : numRowsPerFlush;
    }

    /**
     * Abstract method for opening this output for writing.
     */
    public abstract void open() throws Exception;

    /**
     * Abstract method for closing this output for writing.
     */
    public abstract void close() throws Exception;

    /**
     * Abstract method for flushing the query resultset rows into this output.
     */
    public abstract void flush() throws Exception;

    /**
     * Abstract method for cleaning up the writer state, after an exception occurs.
     */
    public abstract void cleanUp();

    /**
     * Abstract method for initializing the writing of a resultset and possibly
     * prepending informational metadata to this output.
     *
     * @param numColumns        number of columns in the resultset.
     * @param resultSetMetaData has information about the columns in a resultset.
     * @param extraMetadata     stores additional metadata information related to
     *                          the resultset, if required also to be written.
     */
    public abstract void initializeResultSet(int numColumns, ResultSetMetaData resultSetMetaData,
            LinkedHashMap<String, Object> extraMetadata) throws Exception;

    /**
     * Abstract method for finalizing the writing of a resultset.
     */
    public abstract void finalizeResultSet() throws Exception;

    /**
     * Abstract method for specifically writing one resultset row to this output, 
     * without inlcuding flushing of rows.
     *
     * @param resultSet ResultSet object resulting from executing a SQL query (it
     *                  needs its next() method to be already run in order to expose
     *                  the next available row).
     */
    public abstract void writeResultSetRow_(ResultSet resultSet) throws Exception;

    /**
     * Opens this outout for writing a SQL query resultset (if not already opened),
     * and writes resultset informational metadata in it.
     *
     * @param resultSetMetaData has information about the columns in a resultset.
     * @param extraMetadata     stores additional metadata information related to
     *                          the resultset, if required also to be written.
     */
    public final void initialize(ResultSetMetaData resultSetMetaData, LinkedHashMap<String, Object> extraMetadata)
            throws Exception {
        currentResultSetNumber++;
        if (currentResultSetNumber == 1) {
            open();
        }
        int numColumns = resultSetMetaData == null ? 0 : resultSetMetaData.getColumnCount();
        initializeResultSet(numColumns, resultSetMetaData, extraMetadata);
        flush();
    }

    /**
     * Writes the next row from a `ResultSet` into this output, and assures row
     * flushing every `numRowsPerFlush` amount of rows.
     *
     * @param resultSet ResultSet object resulting from executing a SQL query (it
     *                  needs its next() method to be already run in order to expose
     *                  the next available row).
     */
    public final void writeResultSetRow(ResultSet resultSet) throws Exception {
        writeResultSetRow_(resultSet);
        numRowsWritten++;
        if (numRowsWritten % numRowsPerFlush == 0) {
            flush();
        }
    }

    /**
     * Finalizes and flushes a resultset in this output, and closes the writer if
     * the last resultset has already been written to this output.
     */
    public final void finalize() throws Exception {
        finalizeResultSet();
        flush();
        if (this.currentResultSetNumber == this.numberOfResultSetsToWrite) {
            close();
        }
    }

    /**
     * Gets the total number of resultsets from the SQL query statement to be
     * written in this output.
     *
     * @return the value of the variable `numberOfResultSetsToWrite`, which is the
     *         total number of resultsets from the SQL query statement to be written
     *         to this output.
     */
    public int getNumberOfResultSetsToWrite() {
        return numberOfResultSetsToWrite;
    }

    /**
     * Sets the total number of resultsets to be written in this output.
     *
     * @param numberOfResultSetsToWrite value of the total number of resultsets to
     *                                  be written in this output.
     */
    public void setNumberOfResultSetsToWrite(int numberOfResultSetsToWrite) {
        this.numberOfResultSetsToWrite = numberOfResultSetsToWrite;
    }

    /**
     * Increments the value of the variable `numberOfResultSetsToWrite` by one.
     */
    public void incrementNumberOfResultSetsToWrite() {
        this.numberOfResultSetsToWrite++;
    }

    /**
     * Gets the resultset number index (starting from zero) that is currently being
     * written to th output.
     *
     * @return the value of the `currentResultSetNumber`, which is the resultset
     *         number index (starting from zero) that is currently being written to
     *         th output.
     *         field.
     */
    public int getCurrentResultSetNumber() {
        return this.currentResultSetNumber;
    }

    /**
     * Gets the number of dataset rows already written.
     *
     * @return the value of the variable `numRowsWritten`, which is the number of
     *         dataset rows already written.
     */
    public Long getNumRowsWritten() {
        return numRowsWritten;
    }

}
