package org.sciserver.springapp.racm.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.Query;

import org.ivoa.dm.VOURPException;
import org.ivoa.dm.model.MetadataObject;
import org.ivoa.dm.model.TransientObjectManager;
import org.sciserver.racm.resources.model.NewSharedWithEntity;
import org.sciserver.racm.resources.model.SciServerEntityType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.jhu.rac.AccessControl;
import edu.jhu.rac.Action;
import edu.jhu.rac.ContextClass;
import edu.jhu.rac.Privilege;
import edu.jhu.rac.Resource;
import edu.jhu.rac.ResourceContext;
import edu.jhu.rac.ResourceType;
import edu.jhu.rac.Role;
import edu.jhu.rac.RoleAction;
import edu.jhu.rac.RoleAssignment;
import edu.jhu.user.SciserverEntity;
import edu.jhu.user.ServiceAccount;

/* Don't add to this class unless you can give it a clear purpose
 * (and then rename the class to reflect that purpose).
 * See https://sourcemaking.com/antipatterns/the-blob
 *
 * To use methods from this class, consider injecting RACMUtilWrapper
 */
@Component
public class RACMUtil {
    private RACMUtil() {
    }

    public static ObjectMapper newObjectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.setSerializationInclusion(Include.NON_NULL);
        return om;
    }

    /**
     * Find action with given name on resourcetype for specified resource.
     * 
     * @param r
     * @param a
     * @return
     */
    public static Action findAction(Resource r, String a) {
        ResourceType rt = r.getResourceType();
        for (Action action : rt.getAction())
            if (action.getName().equals(a))
                return action;
        return null;
    }

    public static RoleAction addAction(Role r, Action a) {
        RoleAction ra = new RoleAction(r);
        ra.setAction(a);
        return ra;
    }

    public static void assignRole(Role role, Resource r, SciserverEntity... assignees) {
        for (SciserverEntity assignee : assignees) {
            RoleAssignment ra = new RoleAssignment(r);
            ra.setScisEntity(assignee);
            ra.setRole(role);
        }
    }

    public static void assignRole(String rolename, Resource r, SciserverEntity... assignees) {
        Role role = getRole(rolename, r.getResourceType());
        assignRole(role, r, assignees);
    }

    public static Privilege assignPrivilege(Action action, Resource r, SciserverEntity e) {
        Privilege p = new Privilege(r);
        p.setScisEntity(e);
        p.setAction(action);
        return p;
    }

    public List<Privilege> assignPrivileges(SciserverEntity e, Resource r, String... actions) {
        ArrayList<Privilege> privs = new ArrayList<Privilege>();
        for (String action : actions) {
            privs.add(assignPrivilege(findAction(r, action), r, e));
        }
        return privs;
    }

    public static void assignAllPrivileges(Resource r, SciserverEntity e) {
        if (r.getResourceType() == null)
            return;
        if (r.getResourceType().getAction() == null)
            return;
        for (Action a : r.getResourceType().getAction())
            assignPrivilege(a, r, e);
    }

    public void deleteAllPrivileges(TransientObjectManager tom, long entityId, Resource resource) {
        List<Privilege> privilegesToDelete = new ArrayList<>();
        for (AccessControl ac : resource.getAccesControl()) {
            if (ac instanceof Privilege && ac.getScisEntity().getId() == entityId) {
                privilegesToDelete.add((Privilege) ac);
            }
        }
        for (Privilege p : privilegesToDelete) {
            resource.getAccesControl().remove(p);
        }
    }

    public static ContextClass queryContextClass(String name, TransientObjectManager tom) throws VOURPException {
        Query q = tom.createQuery("select cc from ContextClass cc where cc.name=:name").setParameter("name", name);
        List<MetadataObject> l = tom.queryJPA(q, false);
        if (l.size() == 1)
            return (ContextClass) l.get(0);
        else if (l.size() > 1)
            throw new VOURPException(String.format("Multiple ContextClass-es found with same name: '%s'", name));
        return null;
    }

    public static ResourceType queryResourceType(String contextClass, String resourceTypeName,
            TransientObjectManager tom) {
        Query q = tom.createQuery("select rt from ResourceType rt where rt.container.name=:ccname and rt.name=:rtname")
                .setParameter("ccname", contextClass).setParameter("rtname", resourceTypeName);

        return tom.queryOne(q, ResourceType.class);
    }

    public Resource queryServiceResource(ServiceAccount serviceAccount, String resourceUUID) {
        TransientObjectManager tom = serviceAccount.getTom();
        Query q = tom.createQuery(
                "select r from Resource r where r.container.account.serviceToken=:serviceToken and r.uuid=:uuid")
                .setParameter("serviceToken", serviceAccount.getServiceToken()).setParameter("uuid", resourceUUID);

        return tom.queryOne(q, Resource.class);
    }

    /**
     * Return SciServerEntity of given typoe and name.<br/>
     * 'name' is interpreted depending on the type: userName for USER name for GROUP
     * serviceToken for SERVICE
     * 
     * @param tom
     * @param name
     * @param type
     * @return
     */
    public static SciserverEntity querySciServerEntity(TransientObjectManager tom, String name,
            SciServerEntityType type) {
        String where = null;
        switch (type) {
        case USER:
            where = "username";
            break;
        case GROUP:
            where = "name";
            break;
        default: // case SERVICE:
            where = "serviceToken";
            break;
        }
        Query q = tom.createQuery(String.format("select s from %s s where s.%s=:name", type.className, where));
        q.setParameter("name", name);
        SciserverEntity e = tom.queryOne(q, SciserverEntity.class);
        return e;
    }

    /**
     * Add a resource type to the specified ContextClass representing the root of
     * that CC.<br/>
     * Allows one to define kind-a "static", or context(class)-level actions.
     * 
     * @param cc
     * @return
     */
    public static ResourceType addRootContextRT(ContextClass cc) {
        ResourceType rt = new ResourceType(cc);
        rt.setName(RACMNames.CONTEXT_ROOTRESOURCE_PUBDID); // protected name
        rt.setDescription("The root context resource type that every contextclass must have.");
        return rt;
    }

    /**
     * Add a resource to the specified ResourceContext representing the root of that
     * RC.<br/>
     *
     * @param rc
     * @return
     */
    public static Resource addRootContext(ResourceContext rc) {
        ContextClass cc = rc.getContextClass();
        if (cc == null)
            return null; //
        ResourceType rootResourceType = getRootResourceType(cc);
        Resource r = getRootResource(rc);
        if (r != null)
            return r;
        r = newResource(rc);
        r.setName(RACMNames.CONTEXT_ROOTRESOURCE_PUBDID); // protected name
        r.setDescription(
                "The root Resource for its container ResourceContext, corresponding to root ResourceType of its ContainerClass. \"\r\n"
                        + "					+ \"Used for associating privileges having to do with ResourceContext-level actions such as those to create other Resources");
        r.setResourceType(rootResourceType);
        r.setPublisherDID(RACMNames.CONTEXT_ROOTRESOURCE_PUBDID);
        return r;
    }

    // ~~~ utility methods

    /**
     * Find and return the root context resource for the specified context.<br/>
     * 
     * @param c
     * @return
     */
    public static final Resource getRootResource(ResourceContext c) {
        if (c.getResource() != null) {
            for (Resource r : c.getResource())
                if (RACMNames.CONTEXT_ROOTRESOURCE_PUBDID.equals(r.getPublisherDID()))
                    return r;
        }
        return null;
    }

    public static final Resource getRootResource(TransientObjectManager tom, String resourceContextUUID) {
        Query q = tom
                .createQuery("SELECT r FROM Resource r " + "WHERE r.container.uuid = :uuid "
                        + "AND r.identity.publisherDID = :pubdid")
                .setParameter("uuid", resourceContextUUID)
                .setParameter("pubdid", RACMNames.CONTEXT_ROOTRESOURCE_PUBDID);
        return tom.queryOne(q, Resource.class);
    }

    /**
     * Find and return the context resourcetype for the specified contextclass.<br/>
     * 
     * @param c
     * @return
     */
    private static final ResourceType getRootResourceType(ContextClass c) {
        for (ResourceType r : c.getResourceType())
            if (RACMNames.CONTEXT_ROOTRESOURCE_PUBDID.equals(r.getName()))
                return r;

        throw new IllegalStateException(
                String.format("ContextClass '%s' has no context Resource detected", c.getName()));
    }

    /**
     * Find and return the specified role for the specified resourcetype.<br/>
     * 
     * @param c
     * @return
     */
    private static final Role getRole(String role, ResourceType rt) {
        for (Role r : rt.getRole())
            if (r.getName().equals(role))
                return r;

        throw new IllegalStateException(
                String.format("ResourceType '%s' has no Role with name '%s'", rt.getName(), role));
    }

    /**
     * This method should be used when creating a Resource, as it also generates a
     * new UUID for the resource.<br/>
     * 
     * @param rc
     * @return
     */
    public static Resource newResource(ResourceContext rc) {
        Resource r = new Resource(rc);
        r.setUuid(UUID.randomUUID().toString());
        return r;
    }

    public static ServiceAccount findServiceAccount(String serviceToken, TransientObjectManager tom) {
        Query q = tom.createQuery("select sa from ServiceAccount sa where sa.serviceToken=:serviceToken");
        q.setParameter("serviceToken", serviceToken);
        return tom.queryOne(q, ServiceAccount.class);
    }
}
