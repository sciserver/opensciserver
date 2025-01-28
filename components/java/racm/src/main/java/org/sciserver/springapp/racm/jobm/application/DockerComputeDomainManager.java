package org.sciserver.springapp.racm.jobm.application;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.persistence.Query;
import org.eclipse.persistence.config.QueryHints;
import org.ivoa.dm.VOURPException;
import org.ivoa.dm.model.MetadataObject;
import org.ivoa.dm.model.TransientObjectManager;
import org.sciserver.racm.jobm.model.ComputeDomainUserVolumeModel;
import org.sciserver.racm.jobm.model.ComputeResourceModel;
import org.sciserver.racm.jobm.model.DockerComputeDomainModel;
import org.sciserver.racm.jobm.model.DockerImageModel;
import org.sciserver.racm.jobm.model.RootVolumeOnComputeDomainModel;
import org.sciserver.racm.jobm.model.UserDockerComputeDomainModel;
import org.sciserver.racm.jobm.model.VolumeContainerModel;
import org.sciserver.racm.utils.model.NativeQueryResult;
import org.sciserver.springapp.racm.login.InsufficientPermissionsException;
import org.sciserver.springapp.racm.resources.application.ContextClassManager;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.sciserver.springapp.racm.utils.RACMNames;
import org.sciserver.springapp.racm.utils.RACMUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import edu.jhu.job.ComputeResource;
import edu.jhu.job.DockerComputeDomain;
import edu.jhu.job.DockerImage;
import edu.jhu.job.VolumeContainer;
import edu.jhu.rac.ContextClass;
import edu.jhu.rac.Resource;
import edu.jhu.rac.ResourceContext;
import edu.jhu.rac.ResourceType;
import edu.jhu.rac.Role;
import edu.jhu.user.User;
import edu.jhu.user.UserGroup;

@Service
public class DockerComputeDomainManager {
    private final JOBMAccessControl jobmAccessControl;
    private final JOBMModelFactory jobmModelFactory;
    private final ComputeDomainManager computeDomainManager;

    @Autowired
    public DockerComputeDomainManager(JOBMAccessControl jobmAccessControl,
            JOBMModelFactory jobmModelFactory, ComputeDomainManager computeDomainManager) {
        this.jobmAccessControl = jobmAccessControl;
        this.jobmModelFactory = jobmModelFactory;
        this.computeDomainManager = computeDomainManager;
    }

    private DockerComputeDomain createDockerComputeDomain(DockerComputeDomainModel model,
            UserProfile up, UserGroup[] admins) throws VOURPException {
        TransientObjectManager tom = up.getTom();
        if (!jobmAccessControl.canRegisterComputeDomain(up.getUser()))
            throw new VOURPException(VOURPException.UNAUTHORIZED, String.format(
                    "User %s is not authorized to register a ComputeDomain", up.getUsername()));

        DockerComputeDomain dcd = queryDockerComputeDomainForEndpoint(model.getApiEndpoint(), tom);
        if (dcd != null) {
            throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,
                    String.format("A Docker Compute Domain with endpoint %s already exists",
                            model.getApiEndpoint()));
        }
        dcd = new DockerComputeDomain(tom);
        dcd.setApiEndpoint(model.getApiEndpoint());
        dcd.setName(model.getName());
        dcd.setDescription(model.getDescription());
        dcd.setPublisherDID(model.getPublisherDID());
        if (model.getImages() != null) {
            for (DockerImageModel dim : model.getImages())
                jobmModelFactory.newDockerImage(dim, dcd);
        }
        if (model.getVolumes() != null) {
            for (VolumeContainerModel vcm : model.getVolumes())
                jobmModelFactory.newVolumeContainer(vcm, dcd);
        }
        if (model.getRootVolumes() != null) {
            for (RootVolumeOnComputeDomainModel rvm : model.getRootVolumes())
                jobmModelFactory.newRootVolumeOnComputeDomain(rvm, dcd);
        }
        // register with RACM
        defineResourceContext(dcd, up.getUser(), admins);

        return dcd;
    }

    /**
     * Transaction outside the method, i.e. this method does not persist the TOM.
     *
     * @param model
     * @param u
     * @return
     * @throws VOURPException
     */
    public DockerComputeDomain manageDockerComputeDomain(DockerComputeDomainModel model,
            UserProfile up, UserGroup[] admins) throws VOURPException {
        if (model.getId() != null)
            return updateDockerComputeDomain(model, up, admins);
        else
            return createDockerComputeDomain(model, up, admins);
    }

    private DockerComputeDomain queryDockerComputeDomainForEndpoint(String endpoint,
            TransientObjectManager tom) {
        Query q = tom
                .createQuery(
                        "select dcd from DockerComputeDomain dcd where dcd.apiEndpoint=:endpoint")
                .setParameter("endpoint", endpoint);
        return tom.queryOne(q, DockerComputeDomain.class);
    }

    DockerComputeDomain queryDockerComputeDomainForId(long id, TransientObjectManager tom) {
        // TODO should we check that user is allowed to see the compute domain? or
        // can everyone see it?
        Query q = tom.createQuery("select dcd from DockerComputeDomain dcd where dcd.id=:id")
                .setParameter("id", id);
        return tom.queryOne(q, DockerComputeDomain.class);
    }

    private DockerComputeDomain queryDockerComputeDomainForUUID(String uuid,
            TransientObjectManager tom) {
        // TODO should we check that user is allowed to see the compute domain? or
        // can everyone see it?
        Query q = tom.createQuery(
                "select dcd from DockerComputeDomain dcd where dcd.resourceContext.uuid=:uuid")
                .setParameter("uuid", uuid);
        return tom.queryOne(q, DockerComputeDomain.class);
    }

    public List<DockerComputeDomainModel> queryDockerComputeDomains(UserProfile user)
            throws VOURPException {
        // only admins are allowed to see all docker compute domains
        if (!user.isAdmin())
            throw new InsufficientPermissionsException("query docker compute domains");

        TransientObjectManager tom = user.getTom();
        Query q = tom.createQuery("select dcd from DockerComputeDomain dcd ");
        List<DockerComputeDomain> dcds = tom.queryJPA(q, DockerComputeDomain.class);
        List<DockerComputeDomainModel> dcdms = new ArrayList<>();
        if (dcds != null) {
            for (DockerComputeDomain dcd : dcds) {
                DockerComputeDomainModel dcdm =
                        jobmModelFactory.newDockerComputeDomainModel(dcd, true);
                dcdms.add(dcdm);
            }
        }
        return dcdms;
    }

    /**
     * Return visible compute domain and the visible compute resources.<br/>
     *
     * Need a method to create model only for visible compute resources.
     *
     * @param user
     * @return
     * @throws VOURPException
     */
    public Collection<UserDockerComputeDomainModel> queryUserDockerComputeDomains(UserProfile user,
            boolean includeBatch, boolean includeInteractive) {
        TransientObjectManager tom = user.getTom();
        String jpaq = null;
        if (includeBatch) {
            if (includeInteractive)
                jpaq = "select dcd from DockerComputeDomain dcd ";
            else
                /*
                 * This is equivalent to selecting the c.computeDomain from COMPM c, but that
                 * doesn't work with joins in eclipse link. Doing it this way requires extra
                 * matching between compms and compute domains in the database layer, but avoids
                 * having to do n+1 queries due to every resource within a compute domain.
                 */
                jpaq = "select dcd from DockerComputeDomain dcd where exists (select c from COMPM c where c.computeDomain=dcd)";
        } else { // includeInteractive is assumed
            jpaq = "select dcd from DockerComputeDomain dcd where not exists (select c from COMPM c where c.computeDomain=dcd)";
        }
        Query q = tom.createQuery(jpaq);
        q.setHint(QueryHints.LEFT_FETCH, "dcd.resourceContext");
        q.setHint(QueryHints.LEFT_FETCH, "dcd.computeResource.resource");
        List<MetadataObject> os = tom.queryJPA(q, false);

        Map<Long, UserDockerComputeDomainModel> jms = new HashMap<>();
        if (os != null && !os.isEmpty()) {
            NativeQueryResult nqr = queryAccessibleComputeResources(user);

            Map<Long, String> dict = new HashMap<>();
            for (Object[] row : nqr.getRows()) {
                Long id = (Long) row[0];
                String rt = (String) row[1];
                dict.put(id, rt);
            }
            nqr = queryWritableVolumeContainers(user);

            Map<Long, String> dictWritableVCs = new HashMap<>();
            for (Object[] row : nqr.getRows()) {
                Long id = (Long) row[0];
                String name = (String) row[1];
                dictWritableVCs.put(id, name);
            }

            for (MetadataObject o : os) {
                if (o instanceof DockerComputeDomain) {
                    DockerComputeDomain dcd = (DockerComputeDomain) o;
                    // do not recurse, do that here
                    UserDockerComputeDomainModel dcdm =
                            jobmModelFactory.newUserDockerComputeDomainModel(dcd, false);
                    // check whether all compute resources are available
                    for (ComputeResource cr : dcd.getComputeResource()) {
                        if (dict.get(cr.getId()) != null) {
                            if (cr instanceof DockerImage) {
                                DockerImage di = (DockerImage) cr;
                                dcdm.getImages().add(jobmModelFactory.newDockerImageModel(di));
                            } else if (cr instanceof VolumeContainer) {
                                VolumeContainer vc = (VolumeContainer) cr;
                                VolumeContainerModel vcm =
                                        jobmModelFactory.newVolumeContainerModel(vc);
                                if (dictWritableVCs.containsKey(vcm.getId()))
                                    vcm.setWritable(true);

                                dcdm.getVolumes().add(vcm);
                            }
                        }
                    }
                    jms.put(dcdm.getId(), dcdm);
                }
            }
        }
        // returns computeDomainId,userVolumeId,path,displayName,publisherDID,action
        Map<Long, Map<Long, ComputeDomainUserVolumeModel>> cds = queryMountableUserVolumes(user);

        for (Map.Entry<Long, UserDockerComputeDomainModel> entry : jms.entrySet()) {
            Map<Long, ComputeDomainUserVolumeModel> cduvms = cds.get(entry.getKey());
            if (cduvms == null)
                continue;
            UserDockerComputeDomainModel ucdm = entry.getValue();
            for (ComputeDomainUserVolumeModel cduvm : cduvms.values()) {
                ucdm.addUserVolume(cduvm);
            }
        }

        return jms.values();
    }

    private static NativeQueryResult queryWritableVolumeContainers(UserProfile up) {
        String sql =
                "select distinct cr.id as computeResourceId ,cr.name from racm.userActions(?) ua"
                        + ",    ComputeResource cr  where ua.contextclass=? "
                        + " and ua.resourceType =? and cr.resourceId= ua.resourceId  and ua.action =?";
        TransientObjectManager tom = up.getTom();
        Query nq = tom.createNativeQuery(sql);
        nq.setParameter(1, up.getUsername());
        nq.setParameter(2, RACMNames.DOCKER_COMPUTE_DOMAIN_CC_NAME);
        nq.setParameter(3, RACMNames.RT_VOLUME_CONTAINER);
        nq.setParameter(4, RACMNames.A_VOLUME_CONTAINER_WRITE);

        NativeQueryResult r = new NativeQueryResult();
        r.setColumns("computeResourceId,name");
        r.setRows(tom.executeNativeQuery(nq));
        return r;
    }

    // ~~~ methods dealing with RACM resources
    private Resource defineComputeResourceResource(ComputeResource cr, ResourceContext rc,
            ResourceType rt) {
        if (rc.getContextClass() != rt.getContainer())
            return null;
        Resource r = RACMUtil.newResource(rc);
        r.setPublisherDID(cr.getPublisherDID());
        r.setResourceType(rt);
        r.setName(cr.getName());
        r.setDescription(cr.getDescription());
        cr.setResource(r);
        return r;
    }

    private ResourceContext defineResourceContext(DockerComputeDomain dcd, User u,
            UserGroup[] admins) throws VOURPException {
        TransientObjectManager tom = u.getTom();
        ContextClass cc = queryComputeDomainContextClass(tom);

        ResourceContext rc = new ResourceContext(tom);
        rc.setContextClass(cc);
        rc.setUuid(UUID.randomUUID().toString());
        rc.setRacmEndpoint(dcd.getApiEndpoint());
        rc.setDescription("ResourceContext representing the Docker Compute Domain at "
                + dcd.getApiEndpoint());
        rc.setLabel(dcd.getName());

        // create root context so we can assign user to admin role on ComputeDomain
        Resource rootContext = RACMUtil.addRootContext(rc);
        RACMUtil.assignRole(RACMNames.R_COMPUTE_DOMAIN_ROOT_ADMIN, rootContext, u);

        dcd.setResourceContext(rc);

        ResourceType volumeContainerResourceType =
                ContextClassManager.getResourceType(RACMNames.RT_VOLUME_CONTAINER, cc);
        Role adminRoleOnVolumeContainer = ContextClassManager
                .getRole(RACMNames.R_VOLUME_CONTAINER_ADMIN, volumeContainerResourceType);
        ResourceType dockerImageResourceType =
                ContextClassManager.getResourceType(RACMNames.RT_DOCKER_IMAGE, cc);
        Role adminRoleOnDockerImage = ContextClassManager.getRole(RACMNames.R_DOCKER_IMAGE_ADMIN,
                dockerImageResourceType);
        if (dcd.getComputeResource() != null) {
            for (ComputeResource cr : dcd.getComputeResource()) {
                if (cr instanceof VolumeContainer) {
                    Resource r = defineComputeResourceResource(cr, rc, volumeContainerResourceType);
                    RACMUtil.assignRole(adminRoleOnVolumeContainer, r, u);
                    for (UserGroup ug : admins)
                        RACMUtil.assignRole(adminRoleOnVolumeContainer, r, ug);
                } else if (cr instanceof DockerImage) {
                    Resource r = defineComputeResourceResource(cr, rc, dockerImageResourceType);
                    RACMUtil.assignRole(adminRoleOnDockerImage, r, u);
                    for (UserGroup ug : admins)
                        RACMUtil.assignRole(adminRoleOnDockerImage, r, ug);
                }
            }
        }

        return rc;

    }

    private ResourceContext synchronizeResourceContext(DockerComputeDomain dcd, User u,
            UserGroup[] admins) throws VOURPException {
        ResourceContext rc = dcd.getResourceContext();

        if (rc == null) {
            throw new VOURPException(VOURPException.ILLEGAL_STATE,
                    "A DockerComputeDomain is not backed by a ResourceContext");
        } else if (!rc.getRacmEndpoint().equals(dcd.getApiEndpoint())) {
            throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,
                    "A DockerComputeDomain's apiendpint can not be updated.");
        }
        if (dcd.getName() != null && !dcd.getName().equals(rc.getLabel()))
            rc.setLabel(dcd.getName());

        ResourceType volumeContainerResourceType = ContextClassManager
                .getResourceType(RACMNames.RT_VOLUME_CONTAINER, rc.getContextClass());
        ResourceType dockerImageResourceType = ContextClassManager
                .getResourceType(RACMNames.RT_DOCKER_IMAGE, rc.getContextClass());
        Role adminRoleOnVolumeContainer = ContextClassManager
                .getRole(RACMNames.R_VOLUME_CONTAINER_ADMIN, volumeContainerResourceType);
        Role adminRoleOnDockerImage = ContextClassManager.getRole(RACMNames.R_DOCKER_IMAGE_ADMIN,
                dockerImageResourceType);
        for (ComputeResource cr : dcd.getComputeResource()) {
            if (cr.getResource() == null) {
                if (cr instanceof DockerImage) {
                    cr.setResource(defineComputeResourceResource(cr, rc, dockerImageResourceType));
                    RACMUtil.assignRole(adminRoleOnDockerImage, cr.getResource(), u);
                    RACMUtil.assignRole(adminRoleOnDockerImage, cr.getResource(), admins);
                } else if (cr instanceof VolumeContainer) {
                    cr.setResource(
                            defineComputeResourceResource(cr, rc, volumeContainerResourceType));
                    RACMUtil.assignRole(adminRoleOnVolumeContainer, cr.getResource(), u);
                    RACMUtil.assignRole(adminRoleOnVolumeContainer, cr.getResource(), admins);
                }
            } else {
                Resource r = cr.getResource();
                r.setDescription(cr.getDescription());
                r.setName(cr.getName());
            }
        }

        return rc;
    }

    private void updateComputeResource(ComputeResource di, ComputeResourceModel dim) {
        di.setDescription(dim.getDescription());
        di.setName(dim.getName());
        di.setPublisherDID(dim.getPublisherDID());
    }

    private DockerComputeDomain updateDockerComputeDomain(DockerComputeDomainModel dcdm,
            UserProfile up, UserGroup[] admins) throws VOURPException {
        TransientObjectManager tom = up.getTom();
        if (admins == null)
            admins = new UserGroup[] {};

        if (dcdm.getRacmUUID() == null || dcdm.getRacmUUID().trim().length() == 0)
            throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,
                    "When updating a docker compute domain, specifiy both id and racmUUID");
        DockerComputeDomain dcd = queryDockerComputeDomainForUUID(dcdm.getRacmUUID(), tom);
        if (dcd == null) {
            throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,
                    String.format("No docker compute domain with id=%s and racmUUID=%s exists",
                            dcdm.getId().toString(), dcdm.getRacmUUID()));
        }
        if (!jobmAccessControl.canEditComputeDomain(up.getUser(), dcd))
            throw new VOURPException(VOURPException.UNAUTHORIZED, String.format(
                    "User %s is not authorized to update the ComputeDomain with apiEndpoint '%s'",
                    up.getUsername(), dcd.getApiEndpoint()));

        // TBD can a computedomain change its apiendpoint? NO
        // Can only add/remove containers and images and rootvolumes, update
        // description and name.
        dcd.setDescription(dcdm.getDescription());
        if (dcdm.getName() != null)
            dcd.setName(dcdm.getName());

        Map<Long, ComputeResource> crs = new HashMap<>();
        for (ComputeResource cr : dcd.getComputeResource())
            crs.put(cr.getId(), cr);

        for (DockerImageModel dim : dcdm.getImages()) {
            if (dim.getRacmUUID() == null)
                jobmModelFactory.newDockerImage(dim, dcd);
            else {
                ComputeResource cr = crs.get(dim.getId());
                if (cr == null) {
                    throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,
                            String.format("DockerImageModel '%s' does not exist in repository",
                                    dim.getRacmUUID()));
                } else if (cr instanceof DockerImage) {
                    crs.remove(cr.getId()); // remove from hashtable so it won't be
                                            // deleted in the end
                    updateComputeResource(cr, dim);
                } else {
                    throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,
                            String.format(
                                    "DockerImageModel '%s' identifies a '%s', not a DockerImage",
                                    dim.getRacmUUID(), cr.getClass().getName()));
                }
            }
        }
        for (VolumeContainerModel vcm : dcdm.getVolumes()) {
            if (vcm.getRacmUUID() == null)
                jobmModelFactory.newVolumeContainer(vcm, dcd);
            else {
                ComputeResource cr = crs.get(vcm.getId());
                if (cr == null) {
                    throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,
                            String.format("VolumeContainerModel '%s' does not exist in repository",
                                    vcm.getRacmUUID()));
                } else if (cr instanceof VolumeContainer) {
                    crs.remove(cr.getId());
                    updateComputeResource(cr, vcm);
                } else {
                    throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT, String.format(
                            "VolumeContainerModel '%s' identifies a '%s', not a VolumeContainer",
                            vcm.getRacmUUID(), cr.getClass().getName()));
                }
            }
        }
        // delete remaining compute resources, those that were not represented in
        // the DockerComputeDomainModel
        ResourceContext rc = dcd.getResourceContext();
        for (ComputeResource cr : crs.values()) {
            dcd.getComputeResource().remove(cr);
            rc.getResource().remove(cr.getResource());
        }

        computeDomainManager.synchronizeRootVolumes(dcd, dcdm.getRootVolumes());

        synchronizeResourceContext(dcd, up.getUser(), admins);

        return dcd;
    }

    private NativeQueryResult queryAccessibleComputeResources(UserProfile up) {
        // TODO decide whether following should be predefined function
        String sql = "select distinct cr.id as computeResourceId , ua.resourceType "
                + "  from racm.userActions(?) ua " + "  ,    ComputeResource cr "
                + " where ua.contextclass=? " + "   and ((ua.resourceType =? and ua.[action] = ?) "
                + "       or( ua.resourceType=? and ua.[action] =?)) "
                + "   and cr.resourceId= ua.resourceId ";

        TransientObjectManager tom = up.getTom();
        Query nq = tom.createNativeQuery(sql);
        nq.setParameter(1, up.getUsername());
        nq.setParameter(2, RACMNames.DOCKER_COMPUTE_DOMAIN_CC_NAME);
        nq.setParameter(3, RACMNames.RT_DOCKER_IMAGE);
        nq.setParameter(4, RACMNames.A_DOCKER_IMAGE_CREATE_CONTAINER);
        nq.setParameter(5, RACMNames.RT_VOLUME_CONTAINER);
        nq.setParameter(6, RACMNames.A_VOLUME_CONTAINER_READ);

        NativeQueryResult r = new NativeQueryResult();
        r.setColumns("computeResourceId,resourceType");
        r.setRows(tom.executeNativeQuery(nq));
        return r;
    }

    /**
     * Find information for the user volumes a user has access to and which can be mounted on a
     * docker compute domain.<br/>
     *
     * @param up
     * @return
     */
    private Map<Long, Map<Long, ComputeDomainUserVolumeModel>> queryMountableUserVolumes(
            UserProfile up) {
        String columns =
                "computeDomainId,userVolumeId,action,resourceUUID,isShareable,rvPath,uvRelativePath,displayName,publisherDID,owner,ownerId,description,fileServiceAPIEndpoint, rootVolumeName";
        String sql = String.format("select %s from racm.mountableUserVolumes(?) order by 1,2,3",
                columns);

        TransientObjectManager tom = up.getTom();
        Query nq = tom.createNativeQuery(sql);
        nq.setParameter(1, up.getUsername());

        List<?> rows = tom.executeNativeQuery(nq);

        Map<Long, Map<Long, ComputeDomainUserVolumeModel>> cds = new HashMap<>();
        Map<Long, ComputeDomainUserVolumeModel> cduvms = null;
        for (Object o : rows) {
            Object[] row = (Object[]) o;
            int i = 0;
            Long computeDomainId = (Long) row[i++];
            cduvms = cds.computeIfAbsent(computeDomainId, id -> new HashMap<>());

            Long userVolumeId = (Long) row[i++];
            String action = (String) row[i++];
            ComputeDomainUserVolumeModel cduvm = cduvms.get(userVolumeId);
            if (cduvm == null) {
                cduvm = new ComputeDomainUserVolumeModel(userVolumeId);
                cduvms.put(cduvm.getId(), cduvm);
                cduvm.setComputeDomainId(computeDomainId);
                cduvm.setResourceUUID((String) row[i++]);
                cduvm.setIsShareable((Boolean) row[i++]);
                String p1 = ((String) row[i++]).trim();
                String p2 = ((String) row[i++]).trim();
                if (p1.endsWith("/") || p2.startsWith("/"))
                    cduvm.setFullPath(p1 + p2);
                else
                    cduvm.setFullPath(p1 + "/" + p2);
                cduvm.setName((String) row[i++]);
                cduvm.setPublisherDID((String) row[i++]);
                cduvm.setOwner((String) row[i++]);
                cduvm.setOwnerId((String) row[i++]);
                cduvm.setDescription((String) row[i++]);
                cduvm.setFileServiceAPIEndpoint((String) row[i++]);
                cduvm.setRootVolumeName((String) row[i++]);
            }
            cduvm.addAllowedAction(action);

        }

        return cds;
    }

    /**
     * Find mountable compute domains for identified computedomain.<br/>
     *
     * @param up
     * @param computeDomainId
     * @return
     */
    Map<Long, ComputeDomainUserVolumeModel> queryMountableUserVolumes(UserProfile up,
            Long computeDomainId) {
        // TODO be less lazy, now simply using method for all compute domains,
        // picking out the one for the requested computeDomain.
        return queryMountableUserVolumes(up).get(computeDomainId);
    }

    private ContextClass queryComputeDomainContextClass(TransientObjectManager tom)
            throws VOURPException {
        return RACMUtil.queryContextClass(RACMNames.DOCKER_COMPUTE_DOMAIN_CC_NAME, tom);
    }

}
