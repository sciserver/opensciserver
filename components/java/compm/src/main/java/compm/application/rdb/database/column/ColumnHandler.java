package compm.application.rdb.database.column;

import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;


/**
 * Contains methods to handle the writing of a SQL query resultset into an
 * output database table.
 */

public abstract class ColumnHandler {

    public ColumnHandler() {
    }

    /**
     * Abstract method for getting the column definition within a CREATE TABLE
     * statement,
     * for a particular column returned in a SQL statement resultset.
     *
     * @param columnName name of column in resultset.
     * @param columnType JDBC column type.
     * @param precision  column precision.
     * @param scale      column scale.
     * @return a String containing the column definition, to be included within a
     *         CREATE TABLE statement.
     */
    public abstract String getColumnDefinition(String columnName, JDBCType columnType,
                                               Integer precision, Integer scale) throws SQLException;

    /**
     * Abstract method for setting the value of a column in a sql statement,
     * which is used for inserting SQL query resultset rows into a database table.
     *
     * @param statement       sql statemment used for inserting a data row into a
     *                        database table.
     * @param resultSet       resultset with a data row needed to be inserted into a
     *                        databse table.
     * @param jdbcColumnTypes JDBC types of the column in the data row.
     * @param sqlType         sql type of the column.
     * @param columnPrecision precision of the column.
     * @param columnIndex     index of a column within the row (starting from 0).
     */
    public abstract void setColumnValue(PreparedStatement statement, ResultSet resultSet,
                                        ArrayList<JDBCType> jdbcColumnTypes,
                                        ArrayList<Integer> sqlType, ArrayList<Integer> columnPrecision,
                                        int columnIndex) throws SQLException;

    /**
     * Abstract method for getting a safe column name, posisble checked against SQL
     * injection.
     *
     * @param unsafeColumnName original column name
     * @return safe column name.
     */
    public abstract String getSafeColumnName(String unsafeColumnName) throws SQLException;
}
