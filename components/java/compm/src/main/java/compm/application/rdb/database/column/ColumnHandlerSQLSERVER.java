package compm.application.rdb.database.column;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONArray;


/**
 * Contains methods to handle the writing of a SQL query resultset into an
 * output database table, specific to SQL Server databases.
 */
public class ColumnHandlerSQLSERVER extends ColumnHandler {
    private static final Integer VARCHAR_MAX_LENGH = 8000;
    private static final Integer BINARY_MAX_LENGH = 8000;

    public ColumnHandlerSQLSERVER() {
        super();
    }

    @Override
    public String getColumnDefinition(String columnName, JDBCType columnType,
                                      Integer precision, Integer scale) throws SQLException {
        String columnNameSafe = getSafeColumnName(columnName);
        switch (columnType) {
            case CHAR:
            case NCHAR:
            case BINARY:
                return columnNameSafe + " " + getMSSQLColumnType(columnType) + "(" + precision + ")";
            case VARCHAR:
            case NVARCHAR:
                if (precision <= VARCHAR_MAX_LENGH) {
                    return columnNameSafe + " " + getMSSQLColumnType(columnType) + "(" + precision + ")";
                } else {
                    return columnNameSafe + " " + getMSSQLColumnType(columnType) + "(MAX)";
                }
            case VARBINARY:
                if (precision <= BINARY_MAX_LENGH) {
                    return columnNameSafe + " " + getMSSQLColumnType(columnType) + "(" + precision + ")";
                } else {
                    return columnNameSafe + " " + getMSSQLColumnType(columnType) + "(MAX)";
                }
            case NUMERIC:
            case DECIMAL:
                return columnNameSafe + " " + getMSSQLColumnType(columnType) + "(" + precision + ", " + scale + ")";
            default:
                return columnNameSafe + " " + getMSSQLColumnType(columnType);
        }
    }

    @Override
    public void setColumnValue(PreparedStatement statement, ResultSet resultSet, 
                               ArrayList<JDBCType> jdbcColumnTypes, ArrayList<Integer> sqlTypes, 
                               ArrayList<Integer> columnPrecisions, int columnIndex) throws SQLException {
        Object columnValue = resultSet.getObject(columnIndex);
        if (columnValue == null) {
            statement.setNull(columnIndex, sqlTypes.get(columnIndex - 1));
            return;
        }

        JDBCType jdbcColumnType = jdbcColumnTypes.get(columnIndex - 1);
        if (JDBCType.ARRAY == jdbcColumnType) {
            Array array = resultSet.getArray(columnIndex);
            String value = new JSONArray(array.getArray()).toString();
            statement.setString(columnIndex, value);
        } else if (JDBCType.DECIMAL == jdbcColumnType || JDBCType.NUMERIC == jdbcColumnType) {
            statement.setBigDecimal(columnIndex, (BigDecimal) columnValue);
        } else {
            int sqlType = sqlTypes.get(columnIndex - 1);
            statement.setObject(columnIndex, columnValue, sqlType, columnPrecisions.get(columnIndex - 1));
        }
    }

    @Override
    public String getSafeColumnName(String unsafeColumnName) throws SQLException {
        // TODO: prevent sql injection
        return unsafeColumnName;
    }

    // see
    // https://www.tutorialspoint.com/java-resultsetmetadata-getcolumntype-method-with-example
    // https://alvinalexander.com/java/edu/pj/jdbc/recipes/ResultSet-ColumnType.shtml
    // https://www.cis.upenn.edu/~bcpierce/courses/629/jdkdocs/guide/jdbc/getstart/mapping.doc.html

    private static final HashMap<JDBCType, String> SQLSERVERColType = new HashMap<>();

    // https://docs.oracle.com/javase/8/docs/api/java/sql/Types.html
    // https://docs.microsoft.com/en-us/sql/connect/jdbc/understanding-the-jdbc-driver-data-types?view=sql-server-ver15
    // https://docs.microsoft.com/en-us/sql/connect/jdbc/understanding-data-type-conversions?view=sql-server-ver15
    
    static {
        SQLSERVERColType.put(JDBCType.ARRAY, "NVARCHAR(MAX)"); // use string representation
        SQLSERVERColType.put(JDBCType.BIGINT, "BIGINT");
        SQLSERVERColType.put(JDBCType.BINARY, "BINARY");
        SQLSERVERColType.put(JDBCType.BIT, "BIT");
        SQLSERVERColType.put(JDBCType.BLOB, "VARBINARY(MAX)");
        SQLSERVERColType.put(JDBCType.BOOLEAN, "");
        SQLSERVERColType.put(JDBCType.CHAR, "CHAR");
        SQLSERVERColType.put(JDBCType.CLOB, "NVARCHAR(MAX)");
        SQLSERVERColType.put(JDBCType.DATALINK, "");
        SQLSERVERColType.put(JDBCType.DATE, "DATE");
        SQLSERVERColType.put(JDBCType.DECIMAL, "DECIMAL");
        SQLSERVERColType.put(JDBCType.DISTINCT, "");
        SQLSERVERColType.put(JDBCType.DOUBLE, "FLOAT");
        SQLSERVERColType.put(JDBCType.FLOAT, "REAL");
        SQLSERVERColType.put(JDBCType.INTEGER, "INT");
        SQLSERVERColType.put(JDBCType.JAVA_OBJECT, "SQL_VARIANT");
        SQLSERVERColType.put(JDBCType.LONGNVARCHAR, "NVARCHAR(MAX)");
        SQLSERVERColType.put(JDBCType.LONGVARBINARY, "IMAGE");
        SQLSERVERColType.put(JDBCType.LONGVARCHAR, "VARCHAR(MAX)");
        SQLSERVERColType.put(JDBCType.NCHAR, "NCHAR");
        SQLSERVERColType.put(JDBCType.NCLOB, "NVARCHAR(MAX)");
        SQLSERVERColType.put(JDBCType.NULL, "");
        SQLSERVERColType.put(JDBCType.NUMERIC, "NUMERIC");
        SQLSERVERColType.put(JDBCType.NVARCHAR, "NVARCHAR");
        SQLSERVERColType.put(JDBCType.OTHER, "");
        SQLSERVERColType.put(JDBCType.REAL, "REAL");
        SQLSERVERColType.put(JDBCType.REF, "");
        SQLSERVERColType.put(JDBCType.REF_CURSOR, "");
        SQLSERVERColType.put(JDBCType.ROWID, "");
        SQLSERVERColType.put(JDBCType.SMALLINT, "SMALLINT");
        SQLSERVERColType.put(JDBCType.SQLXML, "");
        SQLSERVERColType.put(JDBCType.STRUCT, "");
        SQLSERVERColType.put(JDBCType.TIME, "TIME");
        SQLSERVERColType.put(JDBCType.TIME_WITH_TIMEZONE, "");
        SQLSERVERColType.put(JDBCType.TIMESTAMP, "DATETIME2");
        SQLSERVERColType.put(JDBCType.TIMESTAMP_WITH_TIMEZONE, "DATETIMEOFFSET");
        SQLSERVERColType.put(JDBCType.TINYINT, "TINYINT");
        SQLSERVERColType.put(JDBCType.VARBINARY, "VARBINARY");
        SQLSERVERColType.put(JDBCType.VARCHAR, "VARCHAR");
    }

    /**
     * Gets the MS SQL Server column type from the JDBC column type.
     *
     * @param columnType JDBC column type.
     * @return the equivalent MS SQL Server column type.
     */
    public static String getMSSQLColumnType(JDBCType columnType) throws IllegalArgumentException {
        if (!SQLSERVERColType.containsKey(columnType) || SQLSERVERColType.get(columnType).equals("")) {
            throw (new IllegalArgumentException(
                    "Unknown conversion from a column type " + columnType.toString() + " to a SQLSERVER column type"));
        }
        return SQLSERVERColType.get(columnType);
    }

}
