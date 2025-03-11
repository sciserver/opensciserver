package org.sciserver.springapp.racm.resources.application;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.ivoa.dm.VOURPException;
import org.ivoa.dm.model.MetadataObject;
import org.ivoa.dm.model.TransientObjectManager;
import org.ivoa.dm.model.TransientObjectManager.ChangeSet;
import org.sciserver.racm.cctree.model.ActionModel;
import org.sciserver.racm.cctree.model.ContextClassModel;
import org.sciserver.racm.cctree.model.ResourceTypeModel;
import org.sciserver.racm.cctree.model.RoleModel;
import org.sciserver.racm.rctree.model.AccessControlModel;
import org.sciserver.racm.rctree.model.PrivilegeModel;
import org.sciserver.racm.rctree.model.ResourceContextMVCModel;
import org.sciserver.racm.rctree.model.ResourceGrants;
import org.sciserver.racm.rctree.model.ResourceMVCModel;
import org.sciserver.racm.rctree.model.RoleAssignmentModel;
import org.sciserver.springapp.racm.login.InsufficientPermissionsException;
import org.sciserver.springapp.racm.storem.application.RegistrationInvalidException;
import org.sciserver.springapp.racm.ugm.application.UsersAndGroupsManager;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.sciserver.springapp.racm.utils.RACMNames;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
import edu.jhu.user.GroupAccessibility;
import edu.jhu.user.GroupRole;
import edu.jhu.user.Member;
import edu.jhu.user.SciserverEntity;
import edu.jhu.user.ServiceAccount;
import edu.jhu.user.User;
import edu.jhu.user.UserGroup;

//In order to distribute racm model package to others without including vo-urp generated classes(domain model class),
//use factory method instead of each racm model class having constructor with vo-urp generated classes as arguments.
@Component
public class RACMModelFactory {

    // identifiers for different sciserverentity types.
    // using X for serviceaccount as care should be taken using it...
    public static final String USERTYPE = "U";
    public static final String GROUPTYPE = "G";
    public static final String SERVICETYPE = "X";

    private final UsersAndGroupsManager usersAndGroupsManager;

    @Autowired
    public RACMModelFactory(UsersAndGroupsManager usersAndGroupsManager) {
        this.usersAndGroupsManager = usersAndGroupsManager;
    }

    static ContextClassModel newContextClassModel(ContextClass contextClass) {
        if (contextClass == null)
            return null;
        ContextClassModel ccm = new ContextClassModel();
        ccm.setId((contextClass.getId() == null ? null : contextClass.getId()));
        ccm.setName(contextClass.getName());
        ccm.setDescription(contextClass.getDescription());
        ccm.setRelease(contextClass.getRelease());

        if (contextClass.getResourceType() != null)
            fillResourceTypeCollections(ccm, contextClass);
        return ccm;
    }

    /**
     * Build a ResourceTypeModelCollection rts from a given ContextClass's
     * ResourceTypeCollection. rts is populated in ViewContextClass.jsp
     * 
     * @param ccm
     * @param contextClass
     */
    private static void fillResourceTypeCollections(ContextClassModel ccm, ContextClass contextClass) {
        List<ResourceTypeModel> rts = new ArrayList<>();
        ccm.setResourceTypes(rts);
        for (ResourceType rt : contextClass.getResourceType()) {
            rts.add(newResourceTypeModel(rt));
        }

    }

    static ResourceTypeModel newResourceTypeModel(ResourceType resourceType) {
        if (resourceType == null)
            return null;
        ResourceTypeModel rtm = new ResourceTypeModel();
        rtm.setId(resourceType.getId());
        rtm.setName(resourceType.getName());
        rtm.setDescription(resourceType.getDescription());
        rtm.setContextClassId(resourceType.getContainer().getId());

        if (resourceType.getAction() != null)
            fillActionCollections(rtm, resourceType);
        if (resourceType.getRole() != null)
            fillRoleCollections(rtm, resourceType);
        return rtm;
    }

    private static void fillActionCollections(ResourceTypeModel rtm, ResourceType resourceType) {
        List<ActionModel> actions = new ArrayList<>();
        for (Action a : resourceType.getAction()) {
            actions.add(newActionModel(a));
        }
        rtm.setActions(actions);
    }

    private static void fillRoleCollections(ResourceTypeModel rtm, ResourceType resourceType) {
        List<RoleModel> rms = new ArrayList<>();
        for (Role r : resourceType.getRole()) {
            rms.add(newRoleModel(r));
        }
        rtm.setRoles(rms);
    }

    public static ActionModel newActionModel(Action action) {
        ActionModel am = new ActionModel();
        am.setId(action.getId());
        am.setName(action.getName());
        am.setDescription(action.getDescription());
        am.setResourceTypeId(action.getContainer().getId());
        if (action.getCategory() != null)
            am.setCategory(action.getCategory().value());
        else
            am.setCategory("Z");
        return am;
    }

    static RoleModel newRoleModel(Role role) {
        if (role == null)
            return null;
        RoleModel rm = new RoleModel();
        rm.setId(role.getId());
        rm.setName(role.getName());
        rm.setDescription(role.getDescription());
        rm.setResourceTypeId(role.getContainer().getId());

        fillActionCollections(rm, role);
        return rm;
    }

    /**
     * Used -if there is an exception creating a new role Once exception is thrown
     * display ViewRole.jsp For creating role. -from getter roleCreateForm from
     * controller.
     *
     *
     * @param rt
     * @return
     */
    public static RoleModel newRoleModel(ResourceType rt) {
        if (rt == null)
            return null;
        RoleModel rm = new RoleModel();
        rm.setResourceTypeId(rt.getId());
        fillAvailableActions(rm, rt, null);
        return rm;
    }

    /**
     * Used in initializing new RoleModel for creating or updating role. It is
     * necessary to fill AvailableAction collection of new RoleModel object.
     * 
     * @param rt
     * @return
     */
    private static void fillActionCollections(RoleModel rm, Role role) {
        List<ActionModel> ams = new ArrayList<>();
        rm.setAssignedActions(ams);
        Map<Long, Action> ht = new HashMap<>();
        if (role.getAction() != null) {
            for (RoleAction ra : role.getAction()) {
                ams.add(newActionModel(ra.getAction()));
                ht.put(ra.getAction().getId(), ra.getAction());
            }
        }
        fillAvailableActions(rm, role.getContainer(), ht);
    }

    /**
     * From ResourceType's action collection, remove all actions which are contained
     * in RoleAction collection of Role.
     * 
     * @param rm
     * @param rt
     * @param ht
     */
    private static void fillAvailableActions(RoleModel rm, ResourceType rt, Map<Long, Action> ht) {
        List<ActionModel> ams = new ArrayList<>();
        rm.setAvailableActions(ams);
        for (Action a : rt.getAction()) {
            if (ht == null || ht.get(a.getId()) == null)
                ams.add(newActionModel(a));
        }
    }

    public static ResourceContextMVCModel newResourceContextModel(UserProfile up) {
        List<ContextClass> ccs = ContextClassManager.queryContextClasses(up);
        ResourceContextMVCModel rcm = new ResourceContextMVCModel();
        fillAvailableContextClassModelCollection(rcm, ccs);
        return rcm;
    }

    static ResourceContextMVCModel newResourceContextModel(ResourceContext resourceContext, List<ContextClass> ccs,
            TransientObjectManager tom) {
        if (resourceContext == null)
            return null;
        ResourceContextMVCModel rcm = new ResourceContextMVCModel();
        rcm.setId((resourceContext.getId() == null ? null : resourceContext.getId()));
        rcm.setLabel(resourceContext.getLabel());
        rcm.setDescription(resourceContext.getDescription());
        rcm.setEndpoint(resourceContext.getRacmEndpoint());
        rcm.setUuid(resourceContext.getUuid());
        if (resourceContext.getAccount() != null)
            rcm.setSecretToken(resourceContext.getAccount().getServiceToken());
        rcm.setContextClassModel(newContextClassModel(resourceContext.getContextClass()));

        if (resourceContext.getResource() != null)
            fillResourceCollections(tom, rcm, resourceContext);

        // To populate the Context Class list in the UI
        fillAvailableContextClassModelCollection(rcm, ccs);

        return rcm;
    }

    /**
     * Build a ResourceModelCollection rs from a given ResourceContext's
     * ResourceCollection. rs is populated in ViewResourceContext.jsp
     * 
     * @param rcm
     * @param resourceContext
     * @throws Exception
     */
    private static void fillResourceCollections(TransientObjectManager tom, ResourceContextMVCModel rcm,
            ResourceContext resourceContext) {
        List<ResourceMVCModel> rs = new ArrayList<>();
        rcm.setResources(rs);
        for (Resource r : resourceContext.getResource()) {
            rs.add(newResourceModel(r, tom));
        }

    }

    private static void fillAvailableContextClassModelCollection(ResourceContextMVCModel rcm, List<ContextClass> ccs) {
        List<ContextClassModel> ccms = new ArrayList<>();
        rcm.setAvailableContextClasses(ccms);

        for (ContextClass contextClass : ccs) {
            ccms.add(newContextClassModel(contextClass));
        }

    }

    static ResourceMVCModel newResourceModel(Resource resource, TransientObjectManager tom) {
        ResourceMVCModel rm = new ResourceMVCModel(resource.getId());
        rm.setContextUUID(resource.getContainer().getUuid());
        rm.setPublisherDID(resource.getPublisherDID());
        rm.setUuid(resource.getUuid());
        rm.setResourceTypeModel(RACMModelFactory.newResourceTypeModel(resource.getResourceType()));
        rm.setContainerId(resource.getContainer().getId());
        rm.setName(resource.getName());
        rm.setDescription(resource.getDescription());

        List<ResourceType> rts = queryResourceTypes(rm, tom);
        List<ResourceTypeModel> rtms = new ArrayList<>();
        for (ResourceType rt : rts)
            rtms.add(newResourceTypeModel(rt));
        rm.setAvailableResourceTypeModels(rtms);
        return rm;
    }

    static ResourceMVCModel newResourceModel(Resource resource, List<ResourceType> rts) {
        if (resource == null)
            return null;
        ResourceMVCModel rm = new ResourceMVCModel();
        rm.setId(resource.getId());
        rm.setName(resource.getName());
        rm.setPublisherDID(resource.getPublisherDID());
        rm.setUuid(resource.getUuid());
        rm.setResourceTypeModel(newResourceTypeModel(resource.getResourceType()));
        rm.setDescription(resource.getDescription());
        rm.setContainerId(resource.getContainer().getId());

        fillAvailableResourceTypeModelCollection(rm, rts);
        return rm;
    }

    /**
     * Call from controller's createResource GET method.
     */
    public static ResourceMVCModel newResourceModel(TransientObjectManager tom, long resourceContextId) {
        ResourceMVCModel rm = new ResourceMVCModel();
        rm.setContainerId(resourceContextId);

        List<ResourceType> rts = queryResourceTypes(rm, tom);
        fillAvailableResourceTypeModelCollection(rm, rts);
        return rm;
    }

    static List<ResourceType> queryResourceTypes(ResourceMVCModel resourceModel, TransientObjectManager tom) {
        ResourceContext rc = ResourceContextManager.queryResourceContext(resourceModel.getContainerId(), tom);
        return rc.getContextClass().getResourceType();
    }

    private static void fillAvailableResourceTypeModelCollection(ResourceMVCModel rm, List<ResourceType> rts) {
        List<ResourceTypeModel> rtms = new ArrayList<>();
        rm.setAvailableResourceTypeModels(rtms);

        for (ResourceType resourceType : rts) {
            rtms.add(newResourceTypeModel(resourceType));
        }

    }

    // ~~~
    private static PrivilegeModel newPrivilegeModel(Privilege p) {
        PrivilegeModel pm = new PrivilegeModel(p.getId());
        pm.setActionId(p.getAction().getId());
        pm.setActionName(p.getAction().getName());
        fillAccessControlModel(pm, p);
        return pm;
    }

    private static RoleAssignmentModel newRoleAssignmentModel(RoleAssignment ra) {
        RoleAssignmentModel ram = new RoleAssignmentModel(ra.getId());
        ram.setRoleId(ra.getRole().getId());
        ram.setRoleName(ra.getRole().getName());
        fillAccessControlModel(ram, ra);
        return ram;
    }

    private static void fillAccessControlModel(AccessControlModel acm, AccessControl ac) {
        acm.setResourceId(ac.getContainer().getId());
        acm.setScisId(ac.getScisEntity().getId());
        if (ac.getScisEntity() instanceof User) {
            User u = (User) ac.getScisEntity();
            acm.setScisType(USERTYPE);
            acm.setScisName(u.getUsername());
        } else if (ac.getScisEntity() instanceof UserGroup) {
            edu.jhu.user.UserGroup ug = (UserGroup) ac.getScisEntity();
            acm.setScisType(GROUPTYPE);
            acm.setScisName(ug.getName());
        } else { // if ac.getScisEntity() instanceof ServiceAccount
            edu.jhu.user.ServiceAccount sa = (ServiceAccount) ac.getScisEntity();
            acm.setScisType(SERVICETYPE);
            acm.setScisName(String.valueOf(sa.getPublisherDID()));
        }
    }

    public static ResourceGrants newResourceGrants(Resource resource, UserProfile up) {
        ResourceGrants rg = new ResourceGrants(resource.getId());
        rg.setResourceName(resource.getName());
        rg.setResourceId(resource.getPublisherDID());
        rg.setResourceUUID(resource.getUuid());
        rg.setResourceDescription(resource.getDescription());
        rg.setContextuuid(resource.getContainer().getUuid());
        rg.setContextClass(resource.getResourceType().getContainer().getName());
        rg.setRtm(RACMModelFactory.newResourceTypeModel(resource.getResourceType()));
        for (AccessControl ac : resource.getAccesControl()) {
            if (ac.getScisEntity() instanceof ServiceAccount && !up.isAdmin()) // admins are allowed to see
                                                                               // ServiceAccounts
                continue;
            if (ac instanceof Privilege) {
                rg.getPrivileges().add(RACMModelFactory.newPrivilegeModel((Privilege) ac));
            } else { // if ac instanceof RoleAssignment
                rg.getRoles().add(RACMModelFactory.newRoleAssignmentModel((RoleAssignment) ac));
            }
        }
        return rg;
    }

    /**
     * Find the action with the specified name on the specified RsourceType.<br/>
     * 
     * @param name
     * @param rt
     * @return
     */
    private static Optional<Action> findAction(String name, ResourceType rt) {
        return rt.getAction().stream().filter(a -> a.getName().equals(name)).findAny();
    }

    /**
     * Find the role with the specified name on the specified RsourceType.<br/>
     * 
     * @param name
     * @param rt
     * @return
     */
    private static Optional<Role> findRole(String name, ResourceType rt) {
        return rt.getRole().stream().filter(role -> role.getName().equals(name)).findAny();
    }

    /**
     * Find the SciserverEntity with specified name and of specified type (U(ser) or
     * (User)G(group).<br/>
     * 
     * @param name
     * @param type
     * @param tom
     * @return
     * @throws VOURPException
     */
    private SciserverEntity findSciserverEntity(String name, String type, TransientObjectManager tom)
            throws VOURPException {
        if (USERTYPE.equals(type)) {
            return Optional.ofNullable(usersAndGroupsManager.queryUserByName(name, tom))
                    .orElseThrow(() -> new RegistrationInvalidException("User not Found"));
        } else if (GROUPTYPE.equals(type)) {
            return usersAndGroupsManager.queryGroup(name, tom)
                    .orElseThrow(() -> new RegistrationInvalidException("Group not Found"));
        } else if (SERVICETYPE.equals(type)) {
            return usersAndGroupsManager.queryServiceAccount(name, tom)
                    .orElseThrow(() -> new RegistrationInvalidException("ServiceAccount not Found"));
        } else {
            throw new RegistrationInvalidException("Tried to create a privilege or role on an invalid type");
        }
    }

    /**
     * Uudate the state of the specified Resource with the state of this
     * ReourceGrants.<br/>
     * 
     * Some rules: - only admins are allowed to add privileges to the 'public' group
     * - Only admin/owners of PUBLIC groups are allowed to assign resources to their
     * group
     * 
     * 
     * @param r
     * @throws VOURPException
     */
    public ChangeSet updateResource(Resource r, ResourceGrants rg, UserProfile up) throws VOURPException {
        ChangeSet changeSet = up.getTom().newChangeSet();

        ResourceType rt = r.getResourceType();
        // copy this list to obtain the original access controls
        // before we add more later in this method
        List<AccessControl> originalAccessControls = new ArrayList<>(r.getAccesControl());

        boolean canEditPublic = up.isAdmin();
        boolean canShareWithService = up.isAdmin();

        Map<Long, Long> existingPrivs = new HashMap<>();
        for (PrivilegeModel pm : rg.getPrivileges()) {
            if (pm.getId() == null) {
                SciserverEntity e = findSciserverEntity(pm.getScisName(), pm.getScisType(), r.getTom());

                // If entity is a PUBLIC group, then ONLY owners or admins are allowed to assign
                // privileges
                if (e instanceof UserGroup && entityIsAPublicGroup(e)
                        && !canUserEditGroup((UserGroup) e, up.getUser())) {
                    throw new InsufficientPermissionsException("share to public group");
                }

                if (entityIsThePublicGroup(e) && !canEditPublic) {
                    throw new InsufficientPermissionsException("assign privilege to 'public' group");
                }

                if (e instanceof ServiceAccount && !canShareWithService) {
                    throw new InsufficientPermissionsException("share with service");
                }

                Action a = findAction(pm.getActionName(), rt)
                        .orElseThrow(() -> new RegistrationInvalidException("Unknown action: " + pm.getActionName()));
                Privilege p = new Privilege(r);
                p.setAction(a);
                p.setScisEntity(e);
                changeSet.add(p);
            } else {
                existingPrivs.put(pm.getId(), pm.getId());
            }
        }
        for (RoleAssignmentModel ram : rg.getRoles()) {
            if (ram.getId() == null) {
                SciserverEntity e = findSciserverEntity(ram.getScisName(), ram.getScisType(), r.getTom());

                if (entityIsThePublicGroup(e) && !canEditPublic) {
                    throw new InsufficientPermissionsException("assign role to public group");
                }
                // If entity is a PUBLIC group, then ONLY owners or admins are allowed to assign
                // privileges
                if (e instanceof UserGroup && entityIsAPublicGroup(e)
                        && !canUserEditGroup((UserGroup) e, up.getUser())) {
                    throw new InsufficientPermissionsException("share to public group");
                }

                Role role = findRole(ram.getRoleName(), rt)
                        .orElseThrow(() -> new RegistrationInvalidException("Unknown role: " + ram.getRoleName()));

                RoleAssignment p = new RoleAssignment(r);
                p.setRole(role);
                p.setScisEntity(e);
                changeSet.add(p);
            } else {
                existingPrivs.put(ram.getId(), ram.getId());
            }
        }

        // remove access controls that are not in rg
        ArrayList<MetadataObject> removed = removeAccessControlsFromResource(r, originalAccessControls,
                existingPrivs.keySet(), up);
        for (MetadataObject o : removed)
            changeSet.delete(o);

        return changeSet;
    }

    private ArrayList<MetadataObject> removeAccessControlsFromResource(Resource r,
            List<AccessControl> originalAccessControls, Collection<Long> accessControlsToKeep, UserProfile up) {
        boolean canEditPublic = up.isAdmin();
        ArrayList<MetadataObject> removed = new ArrayList<>();
        originalAccessControls.stream().filter(ac -> !accessControlsToKeep.contains(ac.getId())).forEach(ac -> {
            SciserverEntity e = ac.getScisEntity();
            if (entityIsThePublicGroup(e) && !canEditPublic) {
                throw new InsufficientPermissionsException("remove privilege/role from he 'public' group");
            }
            if (entityIsAPublicGroup(e) && !canUserEditGroup((UserGroup) e, up.getUser())) {
                throw new InsufficientPermissionsException("remove privilege/role from a public group");
            }
            r.getAccesControl().remove(ac);
            removed.add(ac);
        });
        return removed;
    }

    private boolean entityIsThePublicGroup(SciserverEntity e) {
        return e instanceof UserGroup && ((UserGroup) e).getName().equals(RACMNames.USERGROUP_PUBLIC);
    }

    private boolean entityIsAPublicGroup(SciserverEntity e) {
        return e instanceof UserGroup && ((UserGroup) e).getAccessibility() == GroupAccessibility.PUBLIC;
    }

    /**
     * Return true if the specified user has OWNER or ADMIN role on the specified
     * group, false otherwise.<br/>
     * 
     * @param ug
     * @param u
     * @return
     */
    private boolean canUserEditGroup(UserGroup ug, User u) {
        for (Member m : ug.getMember()) {
            if (m.getScisEntity() == u
                    && (m.getMemberRole() == GroupRole.ADMIN || m.getMemberRole() == GroupRole.OWNER))
                return true;
        }
        return false;
    }
}
