package compm.application.rdb.database;


/**
 * Used for storing information regarding the JDBC connection.
 */
public class JDBCConnectionInfo {
    private String jdbcUrl;
    private String vendor;

    public JDBCConnectionInfo(String jdbcUrl, String vendor) {
        this.jdbcUrl = jdbcUrl;
        this.vendor = vendor;
    }

    /**
     * Gets the JDBC URL.
     *
     * @return the JDBC URL
     */
    public String getJDBCUrl() {
        return jdbcUrl;
    }

    /**
     * Gets the database vendor.
     *
     * @return the database vendor
     */
    public String getVendor() {
        return vendor;
    }

}
