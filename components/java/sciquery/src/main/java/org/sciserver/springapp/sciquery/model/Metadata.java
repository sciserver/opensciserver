package org.sciserver.springapp.sciquery.model;

import java.security.InvalidParameterException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


/**
 * Utility class for constructing metadata queries.
 */
public class Metadata {

    /**
     * Enum representing the different types of metadata that can be queried.
     */
    public enum MetadataType { TABLES, COLUMNS, ROUTINES, PARAMETERS, VIEWS, CONSTRAINTS, SCHEMAS }
 
    private static final String TAP_TABLE_NAME = "TAP_TABLE_NAME";
    private static final String TAP_OBJECT = "TAP_OBJECT";
    private static final String TAP_WHERE_CLAUSE = "TAP_WHERE_CLAUSE";
    private static final String INF_WHERE_CLAUSE = "INF_WHERE_CLAUSE";
    private static final String INF_OBJECT = "INF_OBJECT";

    private static String hasTapSchemaQuery = "SELECT * FROM INFORMATION_SCHEMA.TABLES "
                                            + "WHERE TABLE_SCHEMA = 'TAP_SCHEMA' "
                                            + "AND TABLE_NAME = '" + TAP_TABLE_NAME + "';";

    private static String tapSchemaQuery = " SELECT * FROM TAP_SCHEMA." + TAP_OBJECT + " " + TAP_WHERE_CLAUSE + ";";
    private static String infSchemaQuery = " SELECT * FROM INFORMATION_SCHEMA." + INF_OBJECT + " " 
                                         + INF_WHERE_CLAUSE + ";";

    public Metadata() {

    }

    /**
     * Gets the appropriate metadata query for the given metadata type and database connection.
     *
     * @param metadataType the type of metadata to query for.
     * @param dbConnection the database connection to use for determining the appropriate query 
     *                     based on the presence of TAP_SCHEMA.
     * @return the metadata query.
     * @throws InvalidParameterException if the provided metadata type is invalid.
     * @throws SQLException if there is an error when executing the query to determine the presence of TAP_SCHEMA.
     */
    public static String getQuery(MetadataType metadataType, Connection dbConnection) 
                                throws InvalidParameterException, SQLException {
        Boolean hasTapSchema;
        String query;
        if (MetadataType.TABLES == metadataType) {
            hasTapSchema = doesQueryReturnRows(dbConnection, hasTapSchemaQuery.replace(TAP_TABLE_NAME, "tables"));
            if (hasTapSchema) {
                query = tapSchemaQuery.replace(TAP_OBJECT, "TABLES").replace(TAP_WHERE_CLAUSE, "");
            } else {
                query = infSchemaQuery.replace(INF_OBJECT, "TABLES").replace(INF_WHERE_CLAUSE, "");
            }
            
        } else if (MetadataType.COLUMNS == metadataType) {
            hasTapSchema = doesQueryReturnRows(dbConnection, hasTapSchemaQuery.replace(TAP_TABLE_NAME, "columns"));
            if (hasTapSchema) {
                query = tapSchemaQuery.replace(TAP_OBJECT, "COLUMNS").replace(TAP_WHERE_CLAUSE, "WHERE TABLE_NAME=?");
            } else {
                query = infSchemaQuery.replace(INF_OBJECT, "COLUMNS").replace(INF_WHERE_CLAUSE, "WHERE TABLE_NAME=?");
            }
            
        } else if (MetadataType.CONSTRAINTS == metadataType) {
            hasTapSchema = doesQueryReturnRows(dbConnection, hasTapSchemaQuery.replace(TAP_TABLE_NAME, "keys"));
            if (hasTapSchema) {
                query = tapSchemaQuery.replace(TAP_OBJECT, "KEYS").replace(TAP_WHERE_CLAUSE, "WHERE TABLE_NAME=?");
            } else {
                query = infSchemaQuery.replace(INF_OBJECT, "TABLE_CONSTRAINTS")
                                      .replace(INF_WHERE_CLAUSE, "WHERE TABLE_NAME=?");
            }
            
        } else if (MetadataType.SCHEMAS == metadataType) {
            hasTapSchema = doesQueryReturnRows(dbConnection, hasTapSchemaQuery.replace(TAP_TABLE_NAME, "schemas"));
            if (hasTapSchema) {
                query = tapSchemaQuery.replace(TAP_OBJECT, "SCHEMAS").replace(TAP_WHERE_CLAUSE, "");
            } else {
                query = infSchemaQuery.replace(INF_OBJECT, "SCHEMATA").replace(INF_WHERE_CLAUSE, "");
            }
        } else if (MetadataType.ROUTINES == metadataType) {
            query = infSchemaQuery.replace(INF_OBJECT, "ROUTINES").replace(INF_WHERE_CLAUSE, "");
            
        } else if (MetadataType.PARAMETERS == metadataType) {
            query = infSchemaQuery.replace(INF_OBJECT, "PARAMETERS").replace(INF_WHERE_CLAUSE, "WHERE SPECIFIC_NAME=?");
            
        } else if (MetadataType.VIEWS == metadataType) {
            query = infSchemaQuery.replace(INF_OBJECT, "VIEWS").replace(INF_WHERE_CLAUSE, "");
            
        } else {
            throw new InvalidParameterException("Invalid metadata type " + metadataType);
        }
        return query;
    }
    
    private static Boolean doesQueryReturnRows(Connection dbConnection, String query) throws SQLException {
        Boolean hasRows = false;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            statement = dbConnection.createStatement();
            resultSet = statement.executeQuery(query);
            if (resultSet.next()) {
                hasRows = true;
            }
        } catch (Exception ex) {
            throw new SQLException("Error when verifying if query return rows: " 
                                  + ex.getMessage(), ex);
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
            } catch (Exception ex) {
                // Ignore exceptions during resultSet cleanup
            }
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (Exception ex) {
                // Ignore exceptions during statement cleanup
            }
        }
        return hasRows;
    }
    

}
