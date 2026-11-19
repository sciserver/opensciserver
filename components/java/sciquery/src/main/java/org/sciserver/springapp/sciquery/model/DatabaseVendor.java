package org.sciserver.springapp.sciquery.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is used to represent a Database Vendor and contains helper methods for its use with JDBC.
 */
@Schema(description = "This class is used to represent a Database Vendor "
                    + "and contains helper methods for its use with JDBC.")
public class DatabaseVendor {

    /**
     * Enum representing the supported database vendors types.
     */
    public static enum DatabaseVendorType { POSTGRES, MYSQL, SQLSERVER }

    private static final Map<DatabaseVendorType, String> vendors = new HashMap<>();

    static {
        vendors.put(DatabaseVendorType.POSTGRES, "postgresql");
        vendors.put(DatabaseVendorType.SQLSERVER, "sqlserver");
        vendors.put(DatabaseVendorType.MYSQL, "mysql");
    }

    /**
     * Gets the formal database vendor name that forms part of a JDBC URL, 
     * based on a string vendor name (independent of its letter case).
     *
     * @param vendor database vendor name, does not matter the letter case.
     * @return the formal database vendor name that forms part of a JDBC URL.
     * @throws InvalidParameterException if the provided vendor string cannot be matched
     *                                   to any of the known formal names.
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
     * Gets the formal database vendor name that forms part of a JDBC URL, 
     * based on a vendor type enum value.
     *
     * @param vendor the vendor type enum value.
     * @return the formal database vendor name that forms part of a JDBC URL.
     * @throws InvalidParameterException if the provided vendor type does not match any of the known types.
     */
    public static String getJDBCVendorName(DatabaseVendorType vendor) throws InvalidParameterException {
        if (!vendors.containsKey(vendor)) {
            throw new InvalidParameterException("Invalid vendor of type " + vendor.toString());
        }
        return vendors.get(vendor);
    }

    /**
     * Gets the db vendor type enum, based on the vendor string.
     *
     * @param vendor database vendor string.
     * @return DatabaseVendorType` enum value.
     * @throws InvalidParameterException if the provided vendor string cannot be matched to.
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
     * Get a JDBC URL based on the provided database vendor, server,
     * database name, credentials, and other parameters.
     *
     * @param dbVendor database vendor.
     * @param dbServer  server where the database is hosted.
     * @param database name od database.
     * @param userName database user name.
     * @param userPassword database user password.
     * @param databaseSchema database schema.
     * @param doUseSSL whether to use SSL for the JDBC connection.
     * @return a JDBC URL based on the provided parameters.
     * @throws InvalidParameterException if the provided database vendor is not recognized or supported.
     */
    public static String getJDBCUrl(String dbVendor, String dbServer, String database, String userName, 
                                    String userPassword, String databaseSchema, Boolean doUseSSL) 
                                    throws InvalidParameterException {
        
        String jdbcUrl = "";
        DatabaseVendorType vendorType = getJDBCVendorType(dbVendor);
        String vendorName = getJDBCVendorName(dbVendor);
        switch (vendorType) {
            case MYSQL:
            case POSTGRES:
                jdbcUrl = "jdbc:%s://%s/%s?user=%s&password=%s&rewriteBatchedStatements=true\"";
                break;
            case SQLSERVER:
                jdbcUrl = "jdbc:%s://;servername=%s;database=%s;user=%s;password=%s;rewriteBatchedStatements=true";
                break;
            default:
                throw new InvalidParameterException("Invalid vendor"); 
        }
        jdbcUrl = String.format(jdbcUrl, vendorName, dbServer, database, userName, userPassword);
        return jdbcUrl; 
    }

    public static String getVendorFromJDBCUrl(String jdbcUrl) {
        String vendorName = jdbcUrl.split(":")[1];
        return getJDBCVendorType(vendorName).toString();
    }
}