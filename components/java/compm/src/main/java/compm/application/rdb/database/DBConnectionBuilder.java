package compm.application.rdb.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import compm.application.Config;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


/**
 * Used for creating database connections and retrieving JDBC connection
 * information.
 */
public class DBConnectionBuilder {

    /**
     * Gets prepared statement from a SQL query to be executed in a specific
     * database.
     *
     * @param query           the SQL query.
     * @param database        the name of the database.
     * @param domainName      the relational database compute domain that contains
     *                        the database.
     * @param databaseSchema  the database schema.
     * @param isQueryReadOnly whether the query is executed with a read-only or
     *                        read-write user.
     * @param queryTimeout    the SQL query execution timeout (in seconds).
     * @param fetchSize       number of rows fetched at a time and made available in
     *                        the ResultSet object.
     * @return a `PreparedStatement` object.
     */
    public static PreparedStatement getNewStatement(String query, String database, String domainName,
                                                    String databaseSchema, Boolean isQueryReadOnly, 
                                                    Integer queryTimeout, Integer fetchSize)
                                                    throws SQLException, ClassNotFoundException {
        // https://www.cs.mun.ca/java-api-1.5/guide/jdbc/getstart/resultset.html
        // https://knes1.github.io/blog/2015/2015-10-19-streaming-mysql-results-using-java8-streams-and-spring-data.html
        Connection connection = getNewConnection(database, domainName, databaseSchema, isQueryReadOnly);
        PreparedStatement sqlStatement = connection.prepareStatement(query, ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_READ_ONLY);
        sqlStatement.setFetchSize(fetchSize);
        sqlStatement.setQueryTimeout(queryTimeout);
        return sqlStatement;
    }

    /**
     * Gets prepared statement from a SQL query to be executed on a specific data
     * source.
     *
     * @param dataSource   datasource object.
     * @param query        the SQL query.
     * @param queryTimeout the SQL query execution timeout (in seconds).
     * @param fetchSize    number of rows fetched at a time and made available in
     *                     the ResultSet object.
     * @return a `PreparedStatement` object.
     */
    public static PreparedStatement getNewStatement(HikariDataSource dataSource, String query, Integer queryTimeout,
                                                    Integer fetchSize) throws SQLException {
        // https://www.cs.mun.ca/java-api-1.5/guide/jdbc/getstart/resultset.html
        // https://knes1.github.io/blog/2015/2015-10-19-streaming-mysql-results-using-java8-streams-and-spring-data.html
        Connection connection = dataSource.getConnection();
        PreparedStatement sqlStatement = connection.prepareStatement(query, ResultSet.TYPE_FORWARD_ONLY,
                                                                     ResultSet.CONCUR_READ_ONLY);
        sqlStatement.setFetchSize(fetchSize);
        sqlStatement.setQueryTimeout(queryTimeout);
        return sqlStatement;
    }

    /**
     * Gets the JDBC connection information from examining the information in the
     * SciQuery database, based on the database name and domain name.
     *
     * @param databaseName    the name of the database context.
     * @param domainName      the name of the domain in which the database context
     *                        is located.
     * @param databaseSchema  the database schema
     * @param isQueryReadOnly whether the query is executed with a read-only or
     *                        read-write user.
     * @return An instance of `JDBCConnectionInfo` containing the JDBC URL and the
     *         database vendor.
     */
    public static JDBCConnectionInfo getJDBCConnectionInfo(String databaseName, String domainName,
                                                           String databaseSchema, Boolean isQueryReadOnly) 
                                                           throws SQLException, ClassNotFoundException {

        String dbVendor = "";
        String dbServer = "";
        String dbName = "";
        String userName = "";
        String userPassword = "";
        Boolean doUseSSL = false;
        //int maxPoolSize = 1;
        Connection conn = null;
        ResultSet rs = null;
        PreparedStatement pst = null;
        try {
            conn = Config.getInstance().getSciQueryDBDataSource().getConnection();
            String query = "SELECT c.*, s.vendor, s.host " 
                         + "FROM DBcontext c join DBDomain d on c.domain_id = d.id " 
                         + "JOIN DBServer s on s.id = c.server_id " 
                         + "WHERE c.context_name = ? and d.displayName = ?;";

            pst = conn.prepareStatement(query);
            pst.setString(1, databaseName);
            pst.setString(2, domainName);
            rs = pst.executeQuery();
            Boolean hasResult = rs.next();
            if (!hasResult) {
                throw new SQLException(
                        "Unable to find database context '" + databaseName + "' in domain '" + domainName + "'");
            } else {
                dbServer = rs.getString("host");
                dbVendor = rs.getString("vendor");
                dbName = rs.getString("db_name");
                // should get different credentials depending on read-only or read/write
                // permissions

                userName = isQueryReadOnly ? rs.getString("readonly_user_name") : rs.getString("readwrite_user_name");
                userPassword = isQueryReadOnly ? rs.getString("readonly_user_password")
                        : rs.getString("readwrite_user_password");
                if (userName == null || userPassword == null) {
                    throw new SQLException("Wrong database credentials");
                }
            }
        } catch (SQLException ex) {
            throw new SQLException("Error when getting db connection information. " + ex.getMessage());
        } finally {
            try {
                rs.close();
            } catch (Exception ex) {
            }
            try {
                pst.close();
            } catch (Exception ex) {
            }
            try {
                conn.close();
            } catch (Exception ex) {
            }
        }
        String jdbcUrl = DatabaseVendor.getJDBCUrl(dbVendor, dbServer, dbName, userName, userPassword, databaseSchema,
                                                   doUseSSL);
        JDBCConnectionInfo info = new JDBCConnectionInfo(jdbcUrl, dbVendor);
        return info;
    }

    /**
     * Gets a database connection based on the database and domain names.
     *
     * @param databaseName    the database name.
     * @param domainName      the domain or server where the database is hosted.
     * @param databaseSchema  the database schema.
     * @param isQueryReadOnly whether the query is executed with a read-only or
     *                        read-write user.
     * @return A new database connection.
     */
    private static Connection getNewConnection(String databaseName, String domainName, String databaseSchema,
                                               Boolean isQueryReadOnly) throws SQLException, ClassNotFoundException {
        JDBCConnectionInfo info = getJDBCConnectionInfo(databaseName, domainName, databaseSchema, isQueryReadOnly);
        return getNewConnection(info.getJDBCUrl());
    }

    /**
     * Gets a database connection based on the parameters needed to build a JDBC
     * URL.
     *
     * @param dbVendor       the database vendor being used.
     * @param dbServer       the server where the database is hosted.
     * @param database       the name of the database.
     * @param userName       the username used to authenticate and connect to the
     *                       database server.
     * @param userPassword   the password of the user trying to establish a
     *                       connection to the database.
     * @param databaseSchema the database schema.
     * @param doUseSSL       boolean value that indicates whether to use SSL for the
     *                       database connection.
     * @param maxPoolSize    the maximum number of connections that can be held in
     *                       the connection pool.
     * @return A new database connection.
     */
    public static Connection getNewConnection(String dbVendor, String dbServer, String database, String userName,
                                              String userPassword, String databaseSchema, Boolean doUseSSL, 
                                              int maxPoolSize) throws SQLException, ClassNotFoundException {
        String jdbcUrl = DatabaseVendor.getJDBCUrl(dbVendor, dbServer, database, userName, userPassword, databaseSchema,
                                                   doUseSSL);
        return getNewConnection(jdbcUrl);
    }

    /**
     * Gets a database connection based on a given JDBC URL.
     *
     * @param jdbcUrl JDBC url.
     * @return A new database connection.
     */
    public static Connection getNewConnection(String jdbcUrl) throws SQLException, ClassNotFoundException {
        Connection connection = DriverManager.getConnection(jdbcUrl);
        connection.setAutoCommit(false);
        return connection;
    }

    /**
     * Gets a datasource based on a given JDBC URL and pool size.
     *
     * @param jdbcUrl     JDBC url.
     * @param maxPoolSize the maximum number of connections that can be held in the
     *                    connection pool.
     * @return A new database connection.
     */
    public static HikariDataSource getNewDataSource(String jdbcUrl, Integer maxPoolSize) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setRegisterMbeans(true);
        hikariConfig.setMaximumPoolSize(maxPoolSize);
        hikariConfig.setAutoCommit(false);
        // for more config see
        // https://github.com/brettwooldridge/HikariCP/wiki/MySQL-Configuration

        hikariConfig.setJdbcUrl(jdbcUrl);

        HikariDataSource dataSource = new HikariDataSource(hikariConfig);

        return dataSource;
    }

}
