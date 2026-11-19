package org.sciserver.sciquery.metadata;

import java.util.Map;
import java.util.Set;

public abstract class DBPrivileges implements IDBPrivileges {
    protected Map<String,Set<String>> databaseActions;
//  private Map<String,Set<String>> schemaActions;
    protected Map<String,Set<String>> tableActions;

    /**
     * Database actions are global, pertain to all objects in the database.
     * 
     * @param db     Used in case-insensitive manner
     * @param action
     * @return
     */
    protected boolean hasDatabaseAction(String db, String action) {
        Set<String> actions = databaseActions.get(db);
        return actions == null ? false : actions.contains(action);
    }

    protected boolean hasTableAction(String table, String action) {
        Set<String> actions = tableActions.get(table);
        return actions == null ? false : actions.contains(action);
    }

}
