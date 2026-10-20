package compm.application.rdb.database;

import compm.application.rdb.database.column.ColumnHandler;
import compm.application.rdb.database.column.ColumnHandlerSQLSERVER;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;


/**
 * Contains methods for handling the JDBC URLs and drivers for several database
 * vendors.
 */
public class DatabaseVendor {

    /**
     * Enum listing database vendor types.
     */
    public static enum DatabaseVendorType {
        POSTGRES, MYSQL, SQLSERVER
    } // TODO: complete list and add respective JDBC drivers

    private static final Map<DatabaseVendorType, String> vendors = new HashMap<>();

    static {
        vendors.put(DatabaseVendorType.POSTGRES, "postgresql");
        vendors.put(DatabaseVendorType.SQLSERVER, "sqlserver");
        vendors.put(DatabaseVendorType.MYSQL, "mysql");
    }

    /**
     * Checks if a case-insensitive JDBC vendor name is valid/supported, and
     * restuens the corresponding lower-case JDBC vendor name, as written in JDBC
     * URLs.
     *
     * @param vendor case-insensitive JDBC vendor name.
     * @return valid/supported JDBC vendor name.
     */
    public static String getJDBCVendorName(String vendor) throws InvalidParameterException {
        for (DatabaseVendorType vendorType : DatabaseVendorType.values()) {
            if (vendor.equalsIgnoreCase(vendorType.toString())) {
                return getJDBCVendorName(vendorType);
            }
        }
        throw new InvalidParameterException("Invalid vendor " + vendor);
    }

    /**
     * Gets the JDBC vendor name, as written in JDBC URLs, based on a provided
     * DatabaseVendorType enum value.
     *
     * @param vendor a particular DatabaseVendorType enum value.
     * @return a JDBC vendor name.
     */
    public static String getJDBCVendorName(DatabaseVendorType vendor) throws InvalidParameterException {
        if (!vendors.containsKey(vendor)) {
            throw new InvalidParameterException("Invalid vendor of type " + vendor.toString());
        }
        return vendors.get(vendor);
    }

    /**
     * Gets the DatabaseVendorType enum value, given a JDBC vendor name, as written
     * in JDBC URLs.
     *
     * @param vendor a JDBC vendor name, as written in JDBC URLs.
     * @return a DatabaseVendorType enum value.
     */
    public static DatabaseVendorType getJDBCVendorType(String vendor) throws InvalidParameterException {
        for (DatabaseVendorType vendorType : DatabaseVendorType.values()) {
            if (vendor.equalsIgnoreCase(vendorType.toString())) {
                return vendorType;
            }
        }
        throw new InvalidParameterException("Invalid vendor " + vendor);
    }

    /**
     * Gets a JDBC URL for connecting to a database in a db server, based on several
     * parameters needed to build it.
     *
     * @param dbVendor       database vendor name.
     * @param dbServer       name of database server.
     * @param database       name of database.
     * @param userName       username used to authenticate and connect to the
     *                       database server.
     * @param userPassword   password of the user trying to establish a connection
     *                       to the database.
     * @param databaseSchema database schema.
     * @param doUseSSL       boolean value that indicates whether to use SSL for the
     *                       database connection.
     * @return String JDBC URL.
     */
    public static String getJDBCUrl(String dbVendor, String dbServer, String database, String userName,
                                    String userPassword, String databaseSchema, Boolean doUseSSL) 
                                    throws ClassNotFoundException {

        String jdbcUrl = "";
        DatabaseVendorType vendorType = getJDBCVendorType(dbVendor);
        String vendorName = getJDBCVendorName(dbVendor);
        // load driver
        Class.forName(getJDBCDriverClassName(dbVendor));
        // see https://www.tutorialspoint.com/jdbc/jdbc-db-connections.htm
        switch (vendorType) {
            case MYSQL:
            case POSTGRES:
                jdbcUrl = "jdbc:%s://%s/%s?user=%s&password=%s&rewriteBatchedStatements=true\"";
                break;
            // see
            // https://stackoverflow.com/questions/26307760/mysql-and-jdbc-with-rewritebatchedstatements-true
            case SQLSERVER:
                jdbcUrl = "jdbc:%s://;servername=%s;database=%s;user=%s;password=%s;rewriteBatchedStatements=true";
                break;
            default:
                break;
        }
        jdbcUrl = String.format(jdbcUrl, vendorName, dbServer, database, userName, userPassword);
        return jdbcUrl;
    }

    /**
     * Gets the JDBC driver class name needed for a particualr JDBC URL string.
     *
     * @param jdbcUrl JDBC URL.
     * @return the JDBC driver class name.
     */
    public static String getJDBCDriverClassNameFromUrl(String jdbcUrl) throws ClassNotFoundException {
        return getJDBCDriverClassName(getVendorFromJDBCUrl(jdbcUrl));
    }

    /**
     * Gets the JDBC driver class name, given the vendor name.
     *
     * @param dbVendor vendor name, as written in JDBC URLs.
     * @return the JDBC driver class name.
     */
    public static String getJDBCDriverClassName(String dbVendor) throws ClassNotFoundException {
        DatabaseVendorType vendorType = getJDBCVendorType(dbVendor);
        switch (vendorType) {
            case POSTGRES:
                return "org.postgresql.Driver";
            case SQLSERVER:
                return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            case MYSQL:
                return "com.mysql.jdbc.Driver";
            default:
                throw new ClassNotFoundException("Unknown class for database vendor " + dbVendor);
        }
    }

    /**
     * Gets a new ColumnHandler instance, based om the database vendor name.
     *
     * @param dbVendor database vendor name.
     * @return new ColumnHandler instance.
     */
    public static ColumnHandler getDBColumnHandler(String dbVendor) {
        DatabaseVendorType vendorType = getJDBCVendorType(dbVendor);
        if (vendorType == DatabaseVendorType.SQLSERVER) {
            return new ColumnHandlerSQLSERVER();
        } else {
            throw new InvalidParameterException("Invalid vendor " + dbVendor);
        }
    }

    /**
     * Gets the DatabaseVendorType enum value corresponding to the vendor name in a
     * JDBC URL string.
     *
     * @param jdbcUrl JDBC URL.
     * @return DatabaseVendorType enum value.
     */
    public static String getVendorFromJDBCUrl(String jdbcUrl) {
        String vendorName = jdbcUrl.split(":")[1];
        return getJDBCVendorType(vendorName).toString();
    }

}