package compm.application.rdb.output;

import compm.application.rdb.database.DBConnectionBuilder;
import compm.application.rdb.database.DatabaseVendor;
import compm.application.rdb.database.column.ColumnHandler;
import java.io.IOException;
import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;


/**
 * Writes the resultset of a SQL query into database table.
 */
public class OutputWriterToDatabase extends OutputWriter {

    private Connection connection;
    private PreparedStatement statement;
    private String tableName;
    private String schema;
    private int timeout;
    private String jdbcUrl;

    private ArrayList<JDBCType> jdbcColumnTypes = new ArrayList<JDBCType>();
    private ArrayList<Integer> sqlTypes = new ArrayList<Integer>();
    private ArrayList<Integer> columnPrecisions = new ArrayList<Integer>();
    private ColumnHandler columnHandler;

    /**
     * Constructor for `OutputWriterToDatabase`.
     */
    public OutputWriterToDatabase(String jdbcUrl, String dbVendor, String tableName, String schema, int timeout,
            Long numRowsPerFlush) throws IOException {
        super(numRowsPerFlush);
        this.jdbcUrl = jdbcUrl;
        this.tableName = tableName;
        this.schema = schema;
        this.timeout = timeout;
        this.columnHandler = DatabaseVendor.getDBColumnHandler(dbVendor);
    }

    /**
     * Opens a new connection to the database.
     */
    @Override
    public void open() throws SQLException, ClassNotFoundException {
        connection = DBConnectionBuilder.getNewConnection(jdbcUrl);
        // connection = dataSource.getConnection();
        // connection.setSchema(schema);
        connection.setAutoCommit(false);
    }

    /**
     * Closes the SQL statement used for writing the rows to the table, and
     * commits/closes the connection to the database.
     */
    @Override
    public void close() throws SQLException {
        try {
            statement.close();
        } catch (Exception e) {
        }
        try {
            connection.commit();
        } catch (Exception e) {
        }
        ;
        try {
            connection.close();
        } catch (Exception e) {
        }
    }

    /**
     * Writes the resultset rows into the database table in batches.
     */
    @Override
    public void flush() throws SQLException, IOException {
        statement.executeBatch();
    }

    /**
     * Closes the SQL statement used for writing the rows to the table, and
     * rollsback/closes the connection to the database. Used in case of an error.
     */
    @Override
    public void cleanUp() {
        try {
            statement.close();
        } catch (Exception e) {
        }
        try {
            connection.rollback();
        } catch (Exception e) {
        }
        ;
        try {
            connection.close();
        } catch (Exception e) {
        }
    }

    /**
     * Creates the output database table using column information from the resultset
     * metadata.
     *
     * @param numColumns        number of columns in the resultset.
     * @param resultSetMetaData has information about the columns in a resultset.
     * @param extraMetadata     stores additional metadata information related to
     *                          the resultset, if needed.
     */
    @Override
    public void initializeResultSet(int numColumns, ResultSetMetaData resultSetMetaData,
                                    LinkedHashMap<String, Object> extraMetadata) 
                                    throws IOException, SQLException, Exception {
        try {
            if (resultSetMetaData != null && resultSetMetaData.getColumnCount() > 0) {
                String columnName;
                JDBCType columnType;
                Integer type;
                Integer precision;
                Integer scale;
                String tableNameSafe = getSafeTableName(tableName);
                String query = "CREATE TABLE " + tableNameSafe + "(";
                for (int i = 1; i <= numColumns; i++) {
                    if (i > 1) {
                        query += ", ";
                    }
                    columnName = resultSetMetaData.getColumnLabel(i);
                    if ("".equals(columnName) || "?column?".equals(columnName)) {
                        columnName = "column" + i;
                    }

                    columnType = JDBCType.valueOf(resultSetMetaData.getColumnType(i));
                    precision = resultSetMetaData.getPrecision(i);
                    scale = resultSetMetaData.getScale(i);
                    type = resultSetMetaData.getColumnType(i);
                    jdbcColumnTypes.add(columnType);
                    columnPrecisions.add(precision);
                    sqlTypes.add(type);

                    query += columnHandler.getColumnDefinition(columnName, columnType, precision, scale);
                }
                query += ")";
                // System.out.println(query);
                statement = connection.prepareStatement(query);
                statement.setQueryTimeout(timeout);
                statement.execute();
                // statement.close() is called in
                // statement.close();
            } else {
                throw new SQLException(
                        "Output database table cannot be created since query does not return a resultset.");
            }
        } catch (SQLException ex) {
            cleanUp();
            throw (ex);
        }
    }

    /**
     * Used for specifically writing a resultset row into this database table.
     *
     * @param resultSet ResultSet object resulting from executing a SQL query (it
     *                  needs its next() method to be already run in order to expose
     *                  the next available row).
     */
    @Override
    public void writeResultSetRow_(ResultSet resultSet) throws SQLException, IOException, Exception {

        ResultSetMetaData metadata = resultSet.getMetaData();
        if (getNumRowsWritten() == 0) {
            String insertQuery = "INSERT INTO " + tableName + " VALUES (";
            for (int i = 1; i <= metadata.getColumnCount(); i++) {
                if (i > 1) {
                    insertQuery += ", ?";
                } else {
                    insertQuery += " ?";
                }
            }
            insertQuery += " )";

            statement = connection.prepareStatement(insertQuery);
            statement.setQueryTimeout(timeout);
        }

        // injecting the row values:
        for (int i = 1; i <= metadata.getColumnCount(); i++) {
            try {
                columnHandler.setColumnValue(statement, resultSet, jdbcColumnTypes, sqlTypes, columnPrecisions, i);
            } catch (Exception e) {
                String columnName = metadata.getColumnName(i);
                throw new SQLException("Unable to write value into column " + columnName, e);
            }
        }
        statement.addBatch();
    }

    /**
     * Finalizes the writing of this resultset with a flush.
     */
    public void finalizeResultSet() throws IOException, Exception {
        flush();
    }

    /**
     * Gets the database table name, checking for sql injection.
     *
     * @param tableName the database table name, as defined by the user.
     * @return a safe databse table name.
     */
    private String getSafeTableName(String tableName) {
        // TODO: should prevent SQL injection
        return tableName;
    }

}


