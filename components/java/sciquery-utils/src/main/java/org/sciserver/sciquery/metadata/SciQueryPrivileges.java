package org.sciserver.sciquery.metadata;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sciserver.racm.jobm.model.RDBComputeDomainModel;
import org.sciserver.racm.resources.model.UserResourceModel;

/**
 * Privileges a user has on a given RDBComputeDomain 
 * @author Gerar
 *
 */
public class SciQueryPrivileges extends DBPrivileges{
    private final String domainUUID;
    
// database-level actions
    public static final String cc_RDBDOMAIN="RDBComputeDomain";
    public static final String rt_DATABASE_CONTEXT="DatabaseContext";
    private static final String a_DB_UPDATE = "UPDATE";
    private static final String a_DB_QUERY = "QUERY";
    private static final String a_DB_GRANT = "GRANT";
// table-level actions
    private static final String a_TBL_READ = "READ";
    private static final String a_TBL_WRITE_ = "WRITE";

    public SciQueryPrivileges(String domainUUID, List<UserResourceModel> resources) {
        this.domainUUID = domainUUID;
        initialize(resources);
    }

    private void initialize(List<UserResourceModel> resources) {
        databaseActions = new HashMap<String, Set<String>>();
//        schemaActions = new HashMap<String,String[]>();
        tableActions = new HashMap<String,Set<String>>();
        
        for(UserResourceModel r: resources)
        {
            String rt =r.getResourceType();
            if(rt_DATABASE_CONTEXT.contentEquals(rt))
                databaseActions.put(r.getResourcePubDID(),new HashSet<String>(r.getActions()));
            else 
            {} // ignore for now
        }
    }

    
    public boolean canCreateTable(String db) {
        return hasDatabaseAction(db, a_DB_UPDATE);
    }

    public boolean canReadTable(String db, String table) {
        return hasDatabaseAction(db, a_DB_QUERY) || hasTableAction(table, a_TBL_READ);
    }

    public boolean canWriteTable(String db, String table) {
        return hasDatabaseAction(db, a_DB_UPDATE) || hasTableAction(table, a_TBL_WRITE_);
    }

    @Override
    public boolean canQueryDatabase(String db) {
        return hasDatabaseAction(db, a_DB_QUERY) ;
    }
}
