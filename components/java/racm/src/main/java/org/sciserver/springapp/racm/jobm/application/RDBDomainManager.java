package org.sciserver.springapp.racm.jobm.application;

import static java.util.stream.Collectors.toMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.eclipse.persistence.config.QueryHints;
import org.ivoa.dm.VOURPException;
import org.ivoa.dm.model.TransientObjectManager;
import org.ivoa.dm.model.TransientObjectManager.ChangeSet;
import org.sciserver.racm.jobm.model.DBCOMPMModel;
import org.sciserver.racm.jobm.model.DatabaseContextModel;
import org.sciserver.racm.jobm.model.RDBComputeDomainModel;
import org.sciserver.racm.utils.model.NativeQueryResult;
import org.sciserver.springapp.racm.resources.application.ContextClassManager;
import org.sciserver.springapp.racm.resources.application.ResourceContextManager;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.sciserver.springapp.racm.utils.RACM;
import org.sciserver.springapp.racm.utils.RACMNames;
import org.sciserver.springapp.racm.utils.RACMUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import edu.jhu.job.COMPM;
import edu.jhu.job.DatabaseContext;
import edu.jhu.job.RDBComputeDomain;
import edu.jhu.job.RDBVendor;
import edu.jhu.rac.ContextClass;
import edu.jhu.rac.Resource;
import edu.jhu.rac.ResourceContext;
import edu.jhu.rac.ResourceType;
import edu.jhu.rac.Role;
import edu.jhu.user.User;
import edu.jhu.user.UserGroup;

@Service
public class RDBDomainManager {
    private final JOBMAccessControl jobmAccessControl;
    private final RACM racm;
    private final JOBMModelFactory jobmModelFactory;
    @PersistenceContext
    private EntityManager em;

    @Autowired
    public RDBDomainManager(JOBMAccessControl jobmAccessControl, RACM racm,
            JOBMModelFactory jobmModelFactory) {
        this.jobmAccessControl = jobmAccessControl;
        this.racm = racm;
        this.jobmModelFactory = jobmModelFactory;
    }

    /**
     * find the RDBComputeDomain for the specified service account token
     * @param serviceAccountToken
     * @return
     */
    public RDBComputeDomain getRDBComputeDomain(String serviceAccountToken) {
        TransientObjectManager tom = racm.newTom();
        Query q = tom.createQuery("select cd from RDBComputeDomain cd where cd.resourceContext.account.serviceToken = :token");
        q.setParameter("token", serviceAccountToken);
        RDBComputeDomain rdbcd = tom.queryOne(q, RDBComputeDomain.class);
        return rdbcd;
    }

    public RDBComputeDomain manageRDBComputeDomain(RDBComputeDomainModel model, UserProfile up,
            UserGroup[] admins) throws VOURPException {
        if (model.getId() != null)
            return updateRDBComputeDomain(model, up, admins);
        else
            return createRDBComputeDomain(model, up, admins);
    }

    public COMPM manageDBCOMPM(String uuid, DBCOMPMModel model, UserProfile up)
            throws VOURPException {

        COMPM compm = null;
        TransientObjectManager tom = up.getTom();
        if (uuid == null) { // create
            if (!jobmAccessControl.canRegisterCOMPM(up))
                throw new VOURPException(VOURPException.UNAUTHORIZED, String
                        .format("User %s is not authorized to register a COMPM", up.getUsername()));
            RDBComputeDomain rdbcd = queryRDBComputeDomainForId(model.getRdbComputeDomainId(), up);
            if (rdbcd == null) // TODO, check that there is not already a COMPM for the domain
                throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT, String.format(
                        "Cannot find RDBComputeDomain for id=%d", model.getRdbComputeDomainId()));

            compm = new COMPM(tom);
            uuid = UUID.randomUUID().toString();
            compm.setUuid(uuid);
            compm.setComputeDomain(rdbcd);
            compm.setCreatorUserid(up.getUserid());
        } else { // retrieve (assumes model==null) and update
            compm = findCOMPM(uuid, tom);
            if (compm == null)
                throw new IllegalArgumentException(
                        "The UUID specified for registering a COMPM is not known");
        }
        compm.setLabel(model.getLabel());
        compm.setDescription(model.getDescription());
        compm.setDefaultJobsPerUser(model.getDefaultJobsPerUser());
        compm.setDefaultJobTimeout(model.getDefaultJobTimeout());

        return compm;
    }

    public DatabaseContext addDbContextToRDBComputeDomain(Long domainId, DatabaseContextModel dbcm, UserProfile up,
            UserGroup[] admins) throws VOURPException {

        RDBComputeDomain rdbcd = queryRDBComputeDomainForId(domainId, up);
        if (rdbcd == null) {
            throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,
                    String.format("No RDB compute domain with id=%s exists", domainId.toString()));
        }
        // Check if user is allowed to update the computedomain, must have admin role on it
        if (!jobmAccessControl.canEditComputeDomain(up.getUser(), rdbcd)) {
            throw new VOURPException(VOURPException.UNAUTHORIZED, String.format(
                    "User %s is not authorized to update the RDBComputeDomain with apiEndpoint '%s'",
                    up.getUsername(), rdbcd.getApiEndpoint()));
        }

        if (dbcm.getId() != null || dbcm.getRacmUUID() != null) {
            throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,
                    String.format("New DatabaseContext '%s' may not have ID or UUID specified", dbcm.getName()));
        }

        DatabaseContext dbc = queryDatabaseContextForName(domainId, dbcm.getName(), up);
        if (dbc != null) {
            throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,
                    String.format("DatabaseContext '%s' already exists in repository", dbcm.getName()));
        }

        dbc = jobmModelFactory.newDatabaseContext(dbcm, rdbcd);
        synchronizeResource(rdbcd, dbc, up.getUser(), admins);

        ChangeSet changeSet = up.getTom().newChangeSet();
        // NB: I've verified that the following tables have associated records persisted w/out explicily adding other
        //   objects to the changeSet: t_Resource, t_DatabaseContext, t_AccessControl, and t_RoleAssignment.  May want
        //   to review this in the future or usage of changeSet in other classes.
        changeSet.add(dbc);
        up.getTom().persistChangeSet(changeSet, true);

        return dbc;
    }

    // ~~~ RDB domain management ~~~
    private RDBComputeDomain createRDBComputeDomain(RDBComputeDomainModel model, UserProfile up,
            UserGroup[] admins) throws VOURPException {
        TransientObjectManager tom = up.getTom();

        if (!jobmAccessControl.canRegisterComputeDomain(up.getUser())) {
            throw new VOURPException(VOURPException.UNAUTHORIZED, String.format(
                    "User %s is not authorized to register a ComputeDomain", up.getUsername()));
        }

        RDBComputeDomain rcd = new RDBComputeDomain(tom);
        rcd.setName(model.getName());
        rcd.setDescription(model.getDescription());
        rcd.setVendor(RDBVendor.fromValue(model.getVendor()));  // TODO do we really care about this?  Should be at DBContext level I think
        rcd.setPublisherDID(model.getPublisherDID());  // TODO check whether publisherDID unique if not null
        rcd.setApiEndpoint(model.getApiEndpoint());

        if (model.getDatabaseContexts() != null) {
            for (DatabaseContextModel dcm : model.getDatabaseContexts()) {
                jobmModelFactory.newDatabaseContext(dcm, rcd);
            }
        }

        // register with RACM
        // TODO: Assignment to unused variable here done in anticipation of modifications required to allow access to
        //   the service token assigned during creation, which is otherwise unavailable.  This may or may not be used
        //   when that work is performed.
        ResourceContext rc = defineResourceContext(rcd, up.getUser(), admins);

        return rcd;
    }

    /**
     * Define a ResourceContext for a RDBComputeDomain.<br/>
     * 
     * @param dcd
     * @param u
     * @param admins
     * @return
     * @throws VOURPException
     */
    private ResourceContext defineResourceContext(RDBComputeDomain dcd, User u, UserGroup[] admins)
            throws VOURPException {
        if (admins == null) {
            admins = new UserGroup[] {};  // to avoid nullpointerexception below
        }

        TransientObjectManager tom = u.getTom();
        ContextClass cc = queryRDBDomainContextClass(tom);

        if (cc == null) {
            throw new VOURPException(VOURPException.ILLEGAL_STATE,
                    "Cannot find ContextClass for RDBComputeDomain. Inform webmaster.");
        }

        ResourceContext rc = new ResourceContext(tom);
        rc.setContextClass(cc);
        rc.setUuid(UUID.randomUUID().toString());
        rc.setRacmEndpoint(dcd.getApiEndpoint());
        rc.setDescription("ResourceContext representing the RDB Compute Domain at " + dcd.getApiEndpoint());
        rc.setLabel(dcd.getName());

        ResourceContextManager.newServiceAccount(rc);
       
        // create root context so we can assign user to admin role on ComputeDomain
        Resource rootContext = RACMUtil.addRootContext(rc);
        RACMUtil.assignRole(RACMNames.R_COMPUTE_DOMAIN_ROOT_ADMIN, rootContext, u);
        for (UserGroup ug : admins) {
            RACMUtil.assignRole(RACMNames.R_COMPUTE_DOMAIN_ROOT_ADMIN, rootContext, ug);
        }

        dcd.setResourceContext(rc);

        ResourceType databaseContextResourceType =
                ContextClassManager.getResourceType(RACMNames.RT_DATABASE_CONTEXT, cc);
        Role adminRoleOnDatabaseContext = ContextClassManager
                .getRole(RACMNames.R_DATABASE_CONTEXT_ADMIN, databaseContextResourceType);

        if (dcd.getDatabaseContext() != null) {
            for (DatabaseContext cr : dcd.getDatabaseContext()) {
                Resource r = defineDatabaseContextResource(cr, rc, databaseContextResourceType);
                RACMUtil.assignRole(adminRoleOnDatabaseContext, r, u);
                for (UserGroup ug : admins) {
                    RACMUtil.assignRole(adminRoleOnDatabaseContext, r, ug);
                }
            }
        }

        return rc;
    }

    private Resource defineDatabaseContextResource(DatabaseContext cr, ResourceContext rc, ResourceType rt) {
        if (rc.getContextClass() != rt.getContainer()) {
            return null;
        }

        Resource r = RACMUtil.newResource(rc);
        r.setPublisherDID(cr.getPublisherDID());
        r.setResourceType(rt);
        r.setName(cr.getName());
        r.setDescription(cr.getDescription());
        cr.setResource(r);

        return r;
    }

    private RDBComputeDomain updateRDBComputeDomain(RDBComputeDomainModel rdbcdm, UserProfile up,
            UserGroup[] admins) throws VOURPException {
        if (admins == null) {
            admins = new UserGroup[] {};
        }

        if (rdbcdm.getRacmUUID() == null || rdbcdm.getRacmUUID().trim().length() == 0)
            throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,
                    "When updating an RDB compute domain, specifiy both id and racmUUID");
        RDBComputeDomain rdbcd = queryRDBComputeDomainForId(rdbcdm.getId(), up);
        if (rdbcd == null) {
            throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,
                    String.format("No RDB compute domain with id=%s and racmUUID=%s exists",
                            rdbcdm.getId().toString(), rdbcdm.getRacmUUID()));
        }
        // Check if user is allowed to update the computedomain, must have admin role on it
        if (!jobmAccessControl.canEditComputeDomain(up.getUser(), rdbcd)) {
            throw new VOURPException(VOURPException.UNAUTHORIZED, String.format(
                    "User %s is not authorized to update the RDBComputeDomain with apiEndpoint '%s'",
                    up.getUsername(), rdbcd.getApiEndpoint()));
        }

        // TBD can a computedomain change its apiendpoint? NO
        // Can only add/remove containers and images, update description and name if not null.
        rdbcd.setDescription(rdbcdm.getDescription());
        if (rdbcdm.getName() != null) {
            rdbcd.setName(rdbcdm.getName());
        }

        Map<Long, DatabaseContext> dbcs = new HashMap<>();
        for (DatabaseContext dbc : rdbcd.getDatabaseContext()) {
            dbcs.put(dbc.getId(), dbc);
        }

        for (DatabaseContextModel dbcm : rdbcdm.getDatabaseContexts()) {
            if (dbcm.getRacmUUID() == null) {
                jobmModelFactory.newDatabaseContext(dbcm, rdbcd);
            } else {
                DatabaseContext dbc = dbcs.get(dbcm.getId());
                if (dbc == null) {
                    throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,
                            String.format("DatabaseContext '%s' does not exist in repository", dbcm.getRacmUUID()));
                } else {
                    dbcs.remove(dbc.getId());  // remove from hashtable so it won't be deleted in the end
                    dbc.setDescription(dbcm.getDescription());
                    dbc.setName(dbcm.getName());
                    dbc.setPublisherDID(dbcm.getPublisherDID());
                }
            }
        }
        // delete remaining compute resources, those that were not represented in the RDBComputeDomainModel
        ResourceContext rc = rdbcd.getResourceContext();
        for (DatabaseContext dbc : dbcs.values()) {
            rdbcd.getDatabaseContext().remove(dbc);
            rc.getResource().remove(dbc.getResource());  // also delete the Resource!
        }
        synchronizeResourceContext(rdbcd, up.getUser(), admins);

        return rdbcd;
    }

    // Perform actions required to create or update a Resource for a DatabaseContext, including specified Roles
    private Resource manageDatabaseContextResource(ResourceContext resourceContext, DatabaseContext dbContext,
            ResourceType databaseContextResourceType, Role adminRoleOnDatabaseContext, User user,
            UserGroup[] admins) throws VOURPException {

        Resource resource = null;
        if (dbContext.getResource() == null) {
            resource = defineDatabaseContextResource(dbContext, resourceContext, databaseContextResourceType);
            dbContext.setResource(resource);
            RACMUtil.assignRole(adminRoleOnDatabaseContext, dbContext.getResource(), user);
            for (UserGroup userGroup : admins) {
                RACMUtil.assignRole(adminRoleOnDatabaseContext, dbContext.getResource(), userGroup);
            }
        } else {
            resource = dbContext.getResource();
            resource.setDescription(dbContext.getDescription());
            resource.setName(dbContext.getName());
        }

        return resource;
    }

    // Synchronize the full ResourceContext encompassing both the RDBComputeDomain and all contained DatabaseContexts
    private ResourceContext synchronizeResourceContext(RDBComputeDomain dcd, User u,
            UserGroup[] admins) throws VOURPException {
        ResourceContext rc = dcd.getResourceContext();

        if (rc == null) {
            throw new VOURPException(VOURPException.ILLEGAL_STATE,
                    "An RDBComputeDomain is not backed by a ResourceContext");
        } else if (!rc.getRacmEndpoint().equals(dcd.getApiEndpoint())) {
            throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,
                    "An RDBComputeDomain's apiEndpoint cannot be updated.");
        }

        if (dcd.getName() != null && !dcd.getName().equals(rc.getLabel())) {
            rc.setLabel(dcd.getName());
        }

        ResourceType databaseContextResourceType = ContextClassManager
                .getResourceType(RACMNames.RT_DATABASE_CONTEXT, rc.getContextClass());
        Role adminRoleOnDatabaseContext = ContextClassManager
                .getRole(RACMNames.R_DATABASE_CONTEXT_ADMIN, databaseContextResourceType);

        for (DatabaseContext cr : dcd.getDatabaseContext()) {
            manageDatabaseContextResource(rc, cr, databaseContextResourceType, adminRoleOnDatabaseContext, u, admins);
        }

        return rc;
    }

    // Synchronize only the specific Resource for the new DatabaseContext
    private Resource synchronizeResource(RDBComputeDomain dcd, DatabaseContext cr, User u,
            UserGroup[] admins) throws VOURPException {
        ResourceContext rc = dcd.getResourceContext();

        if (rc == null) {
            throw new VOURPException(VOURPException.ILLEGAL_STATE,
                    "An RDBComputeDomain is not backed by a ResourceContext");
        }

        ResourceType databaseContextResourceType = ContextClassManager
                .getResourceType(RACMNames.RT_DATABASE_CONTEXT, rc.getContextClass());
        Role adminRoleOnDatabaseContext = ContextClassManager
                .getRole(RACMNames.R_DATABASE_CONTEXT_ADMIN, databaseContextResourceType);

        return manageDatabaseContextResource(rc, cr, databaseContextResourceType, adminRoleOnDatabaseContext, u, admins);
    }

    private ContextClass queryRDBDomainContextClass(TransientObjectManager tom)
            throws VOURPException {
        return RACMUtil.queryContextClass(RACMNames.RDB_COMPUTE_DOMAIN_CC_NAME, tom);
    }

    private RDBComputeDomain queryRDBComputeDomainForId(long id, UserProfile user) {
        TransientObjectManager tom = user.getTom();
        Query q = tom.createQuery("select dcd from RDBComputeDomain dcd where dcd.id=:id")
                .setParameter("id", id);
        return tom.queryOne(q, RDBComputeDomain.class);
    }

    /**
     * Return visible compute domain and the visible compute resources.<br/>
     *
     * TODO this code needs optimization, currently individual queries for all computeresources. TOO
     * expensive. Need a method to create model only for visible compute resources.
     *
     * @param user
     * @return
     * @throws VOURPException
     */
    public List<RDBComputeDomainModel> queryVisibleRDBComputeDomains(UserProfile user)
            throws VOURPException {
        // TODO should find those user is allowed to see
        TransientObjectManager tom = user.getTom();
        String jpaq = "select dcd from RDBComputeDomain dcd ";

        Query q = tom.createQuery(jpaq);
        q.setHint(QueryHints.LEFT_FETCH, "dcd.resourceContext");
        q.setHint(QueryHints.LEFT_FETCH, "dcd.databaseContext.resource");
        List<RDBComputeDomain> os = tom.queryJPA(q, RDBComputeDomain.class);

        List<RDBComputeDomainModel> jms = new ArrayList<>();
        if (os != null && !os.isEmpty()) {
            NativeQueryResult nqr = queryAccessibleDatabaseContexts(user);

            Map<Long, Long> databaseContextToDomainId = nqr.getRows().stream()
                    .collect(toMap(row -> (Long) row[1], row -> (Long) row[0]));

            for (RDBComputeDomain dcd : os) {
                RDBComputeDomainModel dcdm = jobmModelFactory.newRDBComputeDomainModel(dcd, false);
                // check whether all compute resources are available
                int count = 0; // count images
                for (DatabaseContext cr : dcd.getDatabaseContext()) {
                    Long rdId = databaseContextToDomainId.get(cr.getId());
                    if (rdId != null && rdId.equals(dcd.getId())) {
                        count++;
                        dcdm.getDatabaseContexts()
                                .add(jobmModelFactory.newDatabaseContextModel(cr));
                    }
                }
                if (count > 0) {
                    jms.add(dcdm);
                }
            }
        }
        return jms;
    }

    private DatabaseContext queryDatabaseContextForName(Long domainId, String name, UserProfile user) {
        TransientObjectManager tom = user.getTom();
        Query q = tom.createQuery("SELECT dbc FROM DatabaseContext dbc WHERE dbc.container.id=:domainId And dbc.name=:name")
                .setParameter("domainId", domainId).setParameter("name", name);
        return tom.queryOne(q, DatabaseContext.class);
    }

    private NativeQueryResult queryAccessibleDatabaseContexts(UserProfile up) {
        String sql = "select distinct cr.containerId as rdbDomainId, cr.id as databaseContextId "
                + "  from racm.userActions(?) ua " + "  ,    DatabaseContext cr "
                + " where ua.contextclass=? " + "   and ua.resourceType =? and ua.[action] = ? "
                + "   and cr.resourceId= ua.resourceId order by 1,2";

        TransientObjectManager tom = up.getTom();
        Query nq = tom.createNativeQuery(sql);
        nq.setParameter(1, up.getUsername());
        nq.setParameter(2, RACMNames.RDB_COMPUTE_DOMAIN_CC_NAME);
        nq.setParameter(3, RACMNames.RT_DATABASE_CONTEXT);
        nq.setParameter(4, RACMNames.A_DATABASE_CONTEXT_QUERY);

        NativeQueryResult r = new NativeQueryResult();
        r.setColumns("rdbDomainId,databaseContextId");
        r.setRows(tom.executeNativeQuery(nq));
        return r;
    }

    /**
     * Find the COMPM identified by the specified id.<br/>
     * 
     * @param id
     * @param tom
     * @return
     */
    private COMPM findCOMPM(String uuid, TransientObjectManager tom) {
        Query q = tom.createQuery("select c from COMPM c where c.uuid=:uuid").setParameter("uuid",
                uuid);
        return tom.queryOne(q, COMPM.class);
    }

    /**
     * Return true if the specified user is allowed to query the specified database on the specified
     * rdb domain, false otehrwise.<br/>
     * 
     * @param up
     * @param rdbDomainId
     * @param databaseContextName
     * @return
     */
    boolean canUserQueryDatabaseContext(UserProfile up, Long rdbDomainId,
            String databaseContextName) {
        TransientObjectManager tom = up.getTom();
        // first retrieve uuid of resource
        Query q = tom.createQuery(
                "select db.resource.uuid from DatabaseContext db where db.container.id=:id and db.name=:name")
                .setParameter("id", rdbDomainId).setParameter("name", databaseContextName);
        List<?> l = tom.customJPQL(q, false);
        if (l.isEmpty()) {
            return false;
        } else if (l.size() > 1) {
            throw new IllegalStateException(
                    "Retrieved multiple databases with the same name in the same rdb domain");
        } else {
            String uuid = l.get(0).toString();
            return racm.canUserDoActionOnResource(up.getUsername(), uuid,
                    RACMNames.A_DATABASE_CONTEXT_QUERY);
        }
    }
}
