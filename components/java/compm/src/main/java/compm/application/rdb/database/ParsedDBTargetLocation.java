package compm.application.rdb.database;

import org.sciserver.racm.jobm.model.RDBTargetModel;


/**
 * Used for parsing a target location string into domain, database, schema,
 * table, owner, and a flag indicating if the target is a MyDB.
 */
public class ParsedDBTargetLocation {

    private String domain;
    private String database;
    private String schema;
    private String table;
    private String owner = null;
    private boolean isMyDB = false;

    /**
     * ParsedDBTargetLocation Constructor.
     *
     * @param targetModel the database target model that needs to be parsed.
     */
    public ParsedDBTargetLocation(RDBTargetModel targetModel) {

        try {
            // remove square brackets and split by "dot" character
            String[] targetLocationElements = targetModel.getLocation().replaceAll("\\[|\\]", "").split("\\.");
            if (targetLocationElements.length != 4) {
                throw new IllegalArgumentException("Wrong number of elements in target location (needed 4).");
            }
            this.domain = targetLocationElements[0];
            this.database = targetLocationElements[1];
            this.schema = targetLocationElements[2];
            this.table = targetLocationElements[3];
            String[] databaseElements = this.database.split(":");
            if (databaseElements.length > 1) {
                owner = databaseElements[1];
                isMyDB = true; // for now
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException(
                    "Unable to parse target location " + targetModel.getLocation() + " . " + ex.toString());
        }
    }

    /**
     * Gets the database domain.
     *
     * @return the database domain.
     */
    public String getDomain() {
        return domain;
    }

    /**
     * Gets the database.
     *
     * @return the database.
     */
    public String getDatabase() {
        return database;
    }

    /**
     * Gets the database schema.
     *
     * @return the database schema.
     */
    public String getSchema() {
        return schema;
    }

    /**
     * Gets the database table.
     *
     * @return the database table.
     */
    public String getTable() {
        return table;
    }

    /**
     * Gets the database owner.
     *
     * @return the database owner.
     */
    public String getOwner() {
        return owner;
    }

    /**
     * Whther the database if a MyDB or not..
     *
     * @return `true` if database is MyDB, `false` if not.
     */
    public boolean isMyDB() {
        return isMyDB;
    }

}
