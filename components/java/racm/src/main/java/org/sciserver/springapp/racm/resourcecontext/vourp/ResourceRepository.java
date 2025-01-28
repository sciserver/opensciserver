package org.sciserver.springapp.racm.resourcecontext.vourp;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Query;

import org.ivoa.dm.VOURPException;
import org.ivoa.dm.model.InvalidTOMException;
import org.ivoa.dm.model.TransientObjectManager;
import org.sciserver.racm.resourcecontext.model.ActionModel;
import org.sciserver.racm.resourcecontext.model.AssociatedResourceModel;
import org.sciserver.racm.resourcecontext.model.AssociatedSciserverEntityModel;
import org.sciserver.racm.resourcecontext.model.AssociatedSciserverEntityModel.EntityType;
import org.sciserver.racm.resourcecontext.model.ServiceResourceFromUserPerspectiveModel;
import org.sciserver.springapp.racm.login.NotAuthorizedException;
import org.sciserver.springapp.racm.resourcecontext.domain.AssociatedResource;
import org.sciserver.springapp.racm.resourcecontext.domain.AssociatedSciserverEntity;
import org.sciserver.springapp.racm.resourcecontext.domain.Resource;
import org.sciserver.springapp.racm.storem.application.RegistrationInvalidException;
import org.sciserver.springapp.racm.utils.RACMUtil;
import org.sciserver.springapp.racm.utils.VOURPContext;
import org.springframework.stereotype.Component;


import edu.jhu.rac.AssociatedSciEntity;
import edu.jhu.rac.OwnershipCategory;
import edu.jhu.rac.ResourceContext;
import edu.jhu.user.SciserverEntity;
import edu.jhu.user.ServiceAccount;
import edu.jhu.user.User;
import edu.jhu.user.UserGroup;

@Component
public class ResourceRepository {
    private final VOURPContext vourpContext;

    ResourceRepository(VOURPContext vourpContext) {
        this.vourpContext = vourpContext;
    }

    public Resource add(Resource resource, String serviceToken) throws NotAuthorizedException {
        edu.jhu.rac.ResourceContext rc = queryResourceContextCheckToken(resource.resourceContextUUID(), serviceToken);
        
        edu.jhu.rac.ResourceType selectedType = rc.getContextClass().getResourceType().stream()
                .filter(rt -> rt.getName().equals(resource.resourceTypeName())).findAny()
                .orElseThrow(() -> new RegistrationInvalidException("Could not find resource type"));

        edu.jhu.rac.Resource newResource = resource.isTransient() ? RACMUtil.newResource(rc)
                : getFromDatabase(resource.uuid());

        newResource.setName(resource.name());
        newResource.setDescription(resource.description());
        newResource.setPublisherDID(resource.publisherDID());
        newResource.setResourceType(selectedType);
        newResource.setAssociatedResource(resource.associatedResources().stream().map(ar -> {
            edu.jhu.rac.AssociatedResource databaseAr = new edu.jhu.rac.AssociatedResource(newResource);
            databaseAr.setResource(getFromDatabase(ar.resourceUUID()));
            databaseAr.setUsage(ar.usage());
            databaseAr.setOwnership(ownedToOwnershipCategory(ar.isOwned()));
            return databaseAr;
        }).collect(toList()));
        newResource.setAssociatedGroup(resource.associatedSciserverEntities().stream().map(ae -> {
            edu.jhu.rac.AssociatedSciEntity databaseAe = new edu.jhu.rac.AssociatedSciEntity(newResource);
            databaseAe.setUsage(ae.usage());
            databaseAe.setOwnership(ownedToOwnershipCategory(ae.owned()));
            databaseAe.setSciEntity(getSciserverEntityFromDatabase(ae.entityId()));
            return databaseAe;
        }).collect(toList()));
        persistUncheckingException(rc.getTom());
        return convertFromDatabase(newResource);
    }

    /**
     * Delete the specified Resource, together with possible linked groups.<br/>
     * Checks whether Resource is owned, in which case it can only be deleted when
     * the service token for the resource context is provided.<br/>
     * TODO find better solution for allowing an owned UserGroup to be deleted if
     * its TOM is not set. This is the case here because these groups are only
     * retrieved through a reference getter. What we may need to do is to have a
     * JPA @PostLoad (or something like that) callback on such getters that can add
     * the object automatically to the TOM
     * 
     * @param resource
     * @param serviceToken
     */

    public void delete(Resource resource, String serviceToken) throws NotAuthorizedException {
        edu.jhu.rac.ResourceContext rc = queryResourceContextCheckToken(resource.resourceContextUUID(), serviceToken);
        TransientObjectManager tom = rc.getTom();
        edu.jhu.rac.Resource r = getFromDatabase(resource.uuid());
        for (AssociatedSciEntity e : r.getAssociatedGroup()) {
            if (e.getOwnership() == OwnershipCategory.OWNED) {
                if (e.getSciEntity() instanceof UserGroup) {
                    UserGroup ug = (UserGroup) e.getSciEntity();
                    if (ug.getTom() == null)
                        ug.setTom(tom);
                    tom.remove(ug);
                }
            }
        }
        rc.getResource().remove(r);
        // Note, owned resources other than those inside RACM SHOULD (?) also be
        // deleted.
        // This must happen outside of this call though.
        persistUncheckingException(tom);
    }

    private OwnershipCategory ownedToOwnershipCategory(boolean owned) {
        return owned ? OwnershipCategory.OWNED : OwnershipCategory.LINKED;
    }

    private void persistUncheckingException(TransientObjectManager tom) {
        try {
            tom.persist();
        } catch (InvalidTOMException e) {
            throw new IllegalStateException("Invalid state in TOM", e);
        }
    }

    private edu.jhu.rac.ResourceContext getResourceContext(String uuid) {
        TransientObjectManager tom = vourpContext.newTOM();
        return tom.queryOne(
                tom.createQuery("SELECT rc FROM ResourceContext rc WHERE rc.uuid = :uuid").setParameter("uuid", uuid),
                ResourceContext.class);
    }

    public ServiceResourceFromUserPerspectiveModel convertServiceResourceFromDatabase(
            edu.jhu.rac.Resource databaseResource, String username, Set<ActionModel> allowedActions) {
        ServiceResourceFromUserPerspectiveModel model = new ServiceResourceFromUserPerspectiveModel(
                databaseResource.getId().longValue(), databaseResource.getPublisherDID(), databaseResource.getUuid(),
                databaseResource.getName(), databaseResource.getDescription(),
                databaseResource.getResourceType().getName(), allowedActions,
                convertAssociatedResourceAndActionsFromDatabase(databaseResource.getUuid(), username),
                convertAssociatedEntityModelFromDatabase(databaseResource.getAssociatedGroup()));

        return model;
    }

    private HashSet<AssociatedResourceModel> convertAssociatedResourceAndActionsFromDatabase(String uuid,
            String username) {
        TransientObjectManager tom = vourpContext.newTOM();
        String cols = "resourceUUID,resourceid,usage,ownership,resourceType, description, resource,action";
        String s = "SELECT " + cols + "  FROM racm.associatedResourceActions(?,?) order by resourceUUID";
        Query q = tom.createNativeQuery(s);
        q.setParameter(1, uuid);
        q.setParameter(2, username);
        List<?> rows = tom.executeNativeQuery(q);

        HashSet<AssociatedResourceModel> associatedResources = new HashSet<AssociatedResourceModel>();
        String currentUUID = null;
        AssociatedResourceModel current = null;
        for (Object o : rows) {
            Object[] row = (Object[]) o;
            currentUUID = (String) row[0];
            if (current == null || !current.getResourceUUID().contentEquals(currentUUID)) {
                current = new AssociatedResourceModel((String) row[2], (String) row[5], "OWNED".equals((String) row[3]),
                        currentUUID);
                current.setResourceType((String) row[4]);
                associatedResources.add(current);
            }
            current.addAction((String) row[7]);
        }
        return associatedResources;
    }

    private Resource convertFromDatabase(edu.jhu.rac.Resource databaseResource) {
        return Resource.createFromExisting(databaseResource.getId(), databaseResource.getUuid(),
                databaseResource.getContainer().getUuid(), databaseResource.getPublisherDID(),
                databaseResource.getName(), databaseResource.getDescription(),
                databaseResource.getResourceType().getName(),
                convertAssociatedResourceFromDatabase(databaseResource.getAssociatedResource()),
                convertAssociatedEntityFromDatabase(databaseResource.getAssociatedGroup()));
    }

    private Set<AssociatedResource> convertAssociatedResourceFromDatabase(
            Collection<edu.jhu.rac.AssociatedResource> databaseAssociations) {
        return databaseAssociations.stream()
                .map(ar -> new AssociatedResource(ar.getResource().getUuid(), ar.getUsage(),
                        ar.getResource().getDescription(),
                        ar.getOwnership().equals(edu.jhu.rac.OwnershipCategory.OWNED)))
                .collect(toSet());
    }

    private Set<AssociatedSciserverEntity> convertAssociatedEntityFromDatabase(
            Collection<edu.jhu.rac.AssociatedSciEntity> databaseAssociations) {
        return databaseAssociations.stream()
                .map(ae -> new AssociatedSciserverEntity(ae.getSciEntity().getId(),
                        convertEntityTypeToString(ae.getSciEntity()), ae.getUsage(),
                        ae.getOwnership().equals(edu.jhu.rac.OwnershipCategory.OWNED)))
                .collect(toSet());
    }

    private Set<AssociatedSciserverEntityModel> convertAssociatedEntityModelFromDatabase(
            Collection<edu.jhu.rac.AssociatedSciEntity> databaseAssociations) {
        return databaseAssociations.stream()
                .map(ae -> new AssociatedSciserverEntityModel(ae.getUsage(),
                        ae.getOwnership().equals(edu.jhu.rac.OwnershipCategory.OWNED), ae.getSciEntity().getId(),
                        convertEntityType(ae.getSciEntity())))
                .collect(toSet());
    }

    private String convertEntityTypeToString(SciserverEntity entity) {
        if (entity instanceof UserGroup)
            return "GROUP";
        if (entity instanceof User)
            return "USER";
        if (entity instanceof ServiceAccount)
            return "SERVICE";
        throw new IllegalStateException("Only associated with groups is currently supported");
    }

    private EntityType convertEntityType(SciserverEntity entity) {
        if (entity instanceof UserGroup)
            return EntityType.GROUP;
        if (entity instanceof User)
            return EntityType.USER;
        if (entity instanceof ServiceAccount)
            return EntityType.SERVICE;
        throw new IllegalStateException("Only associated with groups is currently supported");
    }

    public Collection<Resource> getByUUIDs(Collection<String> uuids) {
        TransientObjectManager tom = vourpContext.newTOM();
        Query query = tom.createQuery("SELECT r FROM Resource r WHERE r.uuid IN :uuids").setParameter("uuids", uuids);
        try {
            return tom.queryJPA(query, edu.jhu.rac.Resource.class).stream().map(this::convertFromDatabase)
                    .collect(toSet());
        } catch (VOURPException e) {
            throw new IllegalStateException(e);
        }
    }

    public Collection<String> getByPubDID(String serviceToken, String rcUUID, String pubDID) {
        TransientObjectManager tom = vourpContext.newTOM();
        String sql= "SELECT r.uuid "
                + " FROM ResourceContext rc "
                + "  join ServiceAccount sa on sa.id=rc.accountId "
                + "  join Resource r on rc.id=r.containerId and r.publisherDID=? "
                + " WHERE rc.uuid=? and sa.serviceToken=?";
        Query query = tom.createNativeQuery(sql)
                .setParameter(1, pubDID)
                .setParameter(2, rcUUID)
                .setParameter(3, serviceToken);
        
        @SuppressWarnings("unchecked")
        Collection<String> rawResults = (Collection<String>)tom.executeNativeQuery(query);
        return rawResults;
    }

    public Resource get(String resourceUUID) {
        edu.jhu.rac.Resource databaseResource = getFromDatabase(resourceUUID);
        if(databaseResource == null)
            return null;
        return convertFromDatabase(databaseResource);
    }

    public ServiceResourceFromUserPerspectiveModel toServiceResourceFromUserPerspectiveModel(String resourceUUID,
            String username, Set<ActionModel> actions) {
        edu.jhu.rac.Resource databaseResource = getFromDatabase(resourceUUID);
        if(databaseResource == null)
            return null;
        ServiceResourceFromUserPerspectiveModel model = convertServiceResourceFromDatabase(databaseResource, username,
                actions);
        return model;
    }

    private edu.jhu.rac.Resource getFromDatabase(String resourceUUID) {
        TransientObjectManager tom = vourpContext.newTOM();
        return tom.queryOne(
                tom.createQuery("SELECT r FROM Resource r WHERE r.uuid = :uuid").setParameter("uuid", resourceUUID),
                edu.jhu.rac.Resource.class);
    }

    private edu.jhu.user.SciserverEntity getSciserverEntityFromDatabase(long id) {
        TransientObjectManager tom = vourpContext.newTOM();
        return tom.find(SciserverEntity.class, id);
    }

    private ResourceContext queryResourceContextCheckToken(String resourceContextUUID, String serviceToken) {
        edu.jhu.rac.ResourceContext rc = getResourceContext(resourceContextUUID);
        if (rc.getAccount() != null && !rc.getAccount().getServiceToken().equals(serviceToken))
            throw new NotAuthorizedException();
        return rc;

    }

}
