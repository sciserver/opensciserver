package org.sciserver.sciquery.metadata;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sciserver.racm.collaboration.model.ResourceWithAnyType.ResourceContextInfo;
import org.sciserver.racm.jobm.model.RDBComputeDomainModel;
import org.sciserver.racm.resources.model.UserResourceModel;
import org.sciserver.racm.resources.v2.model.GenericResource.ResourceContext;

/**
 * Privileges a user has on a given RDBComputeDomain 
 * @author Gerar
 *
 */
public class CasJobsPrivileges extends DBPrivileges {
    
// database-level actions
    public static final String cc_CASJOBS="CasJobs";
    public static final String rt_DATABASE_CONTEXT="Casjobs.DatabaseContext";
//    private static final String a_DB_UPDATE = "update";
    private static final String a_DB_VIEWSCHEMA = "viewSchema";
    private static final String a_DB_QUERY = "submitQuery";
    private static final String a_DB_GRANT = "grant";
// table-level actions
//    private static final String a_TBL_READ = "READ";
//    private static final String a_TBL_WRITE_ = "WRITE";

    public CasJobsPrivileges(List<UserResourceModel> resources) {
        initialize(resources);
    }

    private void initialize(List<UserResourceModel> resources) {
        databaseActions = new HashMap<String, Set<String>>();
//        tableActions = new HashMap<String,Set<String>>();
        
        for(UserResourceModel r: resources)
        {
            String rt =r.getResourceType();
            if(rt_DATABASE_CONTEXT.contentEquals(rt))
                databaseActions.put(r.getResourcePubDID(),new HashSet<String>(r.getActions()));
            else 
            {} // ignore for now
        }
    }

    
    @Override
    public boolean canCreateTable(String db) {
//        return hasDatabaseAction(db, a_DB_UPDATE);
        return false;
    }

    @Override
    public boolean canReadTable(String db, String table) {
//        return hasDatabaseAction(db, a_DB_QUERY) || hasTableAction(table, a_TBL_READ);
        return hasDatabaseAction(db, a_DB_QUERY);
    }

    @Override
    public boolean canWriteTable(String db, String table) {
//        return hasDatabaseAction(db, a_DB_UPDATE) || hasTableAction(table, a_TBL_WRITE_);
        return false;
    }

    @Override
    public boolean canQueryDatabase(String db) {
        Set<String> actions = databaseActions.get(db);
        return actions != null && actions.contains(a_DB_QUERY);
    }
}
