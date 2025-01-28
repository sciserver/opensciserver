package org.sciserver.springapp.racm.storem.application;

import static java.util.Collections.emptyList;
import static org.sciserver.springapp.racm.storem.application.STOREMConstants.A_FILESERVICE_DATAVOLUME_DELETE;
import static org.sciserver.springapp.racm.storem.application.STOREMConstants.A_FILESERVICE_DATAVOLUME_EDIT;
import static org.sciserver.springapp.racm.storem.application.STOREMConstants.A_FILESERVICE_DATAVOLUME_GRANT;
import static org.sciserver.springapp.racm.storem.application.STOREMConstants.A_FILESERVICE_DATAVOLUME_READ;
import static org.sciserver.springapp.racm.storem.application.STOREMConstants.A_FILESERVICE_DATAVOLUME_WRITE;
import static org.sciserver.springapp.racm.storem.application.STOREMConstants.A_FILESERVICE_ROOTVOLUME_CREATE;
import static org.sciserver.springapp.racm.storem.application.STOREMConstants.A_FILESERVICE_ROOTVOLUME_GRANT;
import static org.sciserver.springapp.racm.storem.application.STOREMConstants.A_FILESERVICE_USERVOLUME_DELETE;
import static org.sciserver.springapp.racm.storem.application.STOREMConstants.A_FILESERVICE_USERVOLUME_GRANT;
import static org.sciserver.springapp.racm.storem.application.STOREMConstants.A_FILESERVICE_USERVOLUME_READ;
import static org.sciserver.springapp.racm.storem.application.STOREMConstants.A_FILESERVICE_USERVOLUME_WRITE;
import static org.sciserver.springapp.racm.storem.application.STOREMConstants.R_FILESERVICE_ADMIN;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.SharedCacheMode;
import javax.persistence.TypedQuery;

import org.eclipse.persistence.config.QueryHints;
import org.ivoa.dm.VOURPException;
import org.ivoa.dm.model.InvalidTOMException;
import org.ivoa.dm.model.MetadataObject;
import org.ivoa.dm.model.TransientObjectManager;
import org.ivoa.dm.model.TransientObjectManager.ChangeSet;
import org.sciserver.racm.resources.model.NewSharedWithEntity;
import org.sciserver.racm.resources.model.SciServerEntityType;
import org.sciserver.racm.resources.model.SharedWithEntity;
import org.sciserver.racm.storem.model.DataVolumeModel;
import org.sciserver.racm.storem.model.FileServiceModel;
import org.sciserver.racm.storem.model.FileServiceResourceModel;
import org.sciserver.racm.storem.model.MinimalFileServiceModel;
import org.sciserver.racm.storem.model.RegisterNewDataVolumeModel;
import org.sciserver.racm.storem.model.RegisterNewFileServiceModel;
import org.sciserver.racm.storem.model.RegisterNewRootVolumeModel;
import org.sciserver.racm.storem.model.RegisterNewServiceVolumeModel;
import org.sciserver.racm.storem.model.RegisterNewUserVolumeModel;
import org.sciserver.racm.storem.model.RegisterNewUserVolumeWithOwnerModel;
import org.sciserver.racm.storem.model.RegisteredDataVolumeModel;
import org.sciserver.racm.storem.model.RegisteredFileServiceModel;
import org.sciserver.racm.storem.model.RegisteredRootVolumeModel;
import org.sciserver.racm.storem.model.RegisteredServiceVolumeModel;
import org.sciserver.racm.storem.model.RegisteredUserVolumeModel;
import org.sciserver.racm.storem.model.RootVolumeModel;
import org.sciserver.racm.storem.model.UpdateSharedWithEntry;
import org.sciserver.racm.storem.model.UpdatedDataVolumeInfo;
import org.sciserver.racm.storem.model.UpdatedFileServiceInfo;
import org.sciserver.racm.storem.model.UpdatedRootVolumeInfo;
import org.sciserver.racm.storem.model.UpdatedUserVolumeInfo;
import org.sciserver.racm.storem.model.UserVolumeModel;
import org.sciserver.racm.utils.model.NativeQueryResult;
import org.sciserver.springapp.racm.login.InsufficientPermissionsException;
import org.sciserver.springapp.racm.login.NotAuthorizedException;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.sciserver.springapp.racm.utils.RACMAccessControl;
import org.sciserver.springapp.racm.utils.RACMNames;
import org.sciserver.springapp.racm.utils.RACMUtil;
import org.sciserver.springapp.racm.utils.VOURPContext;
import org.sciserver.springapp.racm.utils.controller.ResourceNotFoundException;
import org.sciserver.springapp.racm.utils.logging.LogUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import edu.jhu.file.DataVolume;
import edu.jhu.file.FileService;
import edu.jhu.file.RootVolume;
import edu.jhu.file.UserVolume;
import edu.jhu.rac.AssociatedResource;
import edu.jhu.rac.OwnershipCategory;
import edu.jhu.rac.Privilege;
import edu.jhu.rac.Resource;
import edu.jhu.user.SciserverEntity;
import edu.jhu.user.ServiceAccount;
import edu.jhu.user.User;
import edu.jhu.user.UserGroup;

/*
 * Repository to obtain or register file service-related objects from a database
 * using vo-urp.
 */
@Component
class FileServiceRepository {
    private static final String UNKNOWN_USER_VOLUME_MESSAGE = "Could not locate user volume";
    private static final String QUERY_PATH_FS_RESOURCE_CONTEXT = "f.resourceContext";

    private final StoremMapper mapper;
    private final STOREMAccessControl accessControl;
    private final RACMUtil racmUtil;
    private final VOURPContext vourpContext;

    @PersistenceContext
    private EntityManager em;

    @Autowired
    FileServiceRepository(StoremMapper mapper, STOREMAccessControl accessControl, RACMUtil racmUtil,
            VOURPContext vourpContext) {
        this.mapper = mapper;
        this.accessControl = accessControl;
        this.racmUtil = racmUtil;
        this.vourpContext = vourpContext;
    }

    public boolean existsByName(UserProfile up, String fileServiceName) {
        return !up.getTom().createQuery("SELECT f.name from FileService f WHERE f.name = :name").setMaxResults(1)
                .setParameter("name", fileServiceName).getResultList().isEmpty();
    }

    public boolean existsByIdentifier(UserProfile up, String fileServiceIdentifer) {
        return !up.getTom()
                .createQuery(
                        "SELECT f.resourceContext.uuid from FileService f WHERE f.resourceContext.uuid = :uuid")
                .setMaxResults(1).setParameter("uuid", fileServiceIdentifer).getResultList().isEmpty();
    }

    public List<String> getFileServiceEndpoints(UserProfile up) {
        List<MinimalFileServiceModel> fs = this.getMinimalFileServices(up);
        return fs.stream().map(MinimalFileServiceModel::getApiEndpoint).collect(Collectors.toList());
    }

    public List<MinimalFileServiceModel> getMinimalFileServices(UserProfile up) {
        TransientObjectManager tom = up.getTom();
        String sql = "  select rc.uuid, f.name,f.description,f.apiendpoint " + 
                "  from t_fileservice f " + 
                "    join t_resourcecontext rc on rc.id=f.resourceContextId " + 
                " where exists (select ua.resourceId from racm.userResourceActionsOnContext(rc.uuid,?) ua)" + 
                "";
        Query q = tom.createNativeQuery(sql).setParameter(1, up.getUsername());
        List<MinimalFileServiceModel> fms = new ArrayList<MinimalFileServiceModel>();
        List<?> rows = tom.executeNativeQuery(q);
        for(Object r : rows) {
            Object[] row = (Object[]) r;
            MinimalFileServiceModel fm = new MinimalFileServiceModel((String)row[0], (String)row[1],(String)row[2],(String)row[3]);
            fms.add(fm);
        }
        return fms;
    }

    public List<FileServiceModel> getFileServices(UserProfile up) {
        TypedQuery<FileService> q = em.createQuery("SELECT f from FileService f", FileService.class);
        q.setHint(QueryHints.LEFT_FETCH, "f.rootVolume.resource");
        q.setHint(QueryHints.LEFT_FETCH, "f.userVolumes.resource");
        q.setHint(QueryHints.LEFT_FETCH, "f.userVolumes.rootVolume");
        q.setHint(QueryHints.LEFT_FETCH, QUERY_PATH_FS_RESOURCE_CONTEXT);

        return q.getResultList().stream().map(fs -> maybeGetFileService(up, fs)).filter(Optional::isPresent)
                .map(Optional::get).collect(Collectors.toList());
    }

    public RegisteredFileServiceModel getRegisteredFileServiceFromToken(String token) {
        TransientObjectManager tom = vourpContext.newTOM();
        Query q = tom.createQuery("SELECT f FROM FileService f WHERE f.serviceToken = :token").setParameter("token",
                token);
        q.setHint(QueryHints.LEFT_FETCH, "f.rootVolume");
        q.setHint(QueryHints.LEFT_FETCH, QUERY_PATH_FS_RESOURCE_CONTEXT);
        FileService fs = tom.queryOne(q, FileService.class);
        if (fs == null)
            throw new ResourceNotFoundException();
        return mapper.getRegisteredFileServiceView(fs, getDataVolumes(fs));
    }

    /**
     * Alternative, hopefully faster method to retrieve sufficient details about a file service's conmtents to display in the SciServer dashboard.<br/> 
     * @param up
     * @param fileServiceResourceContextUUID
     * @return
     * TODO move this code to FileServiceModel??
     */
    public FileServiceModel getFileServiceFAST(UserProfile up, String fileServiceResourceContextUUID) {
        TransientObjectManager tom = up.getTom();
        Query q = tom.createQuery("select fs from FileService fs where fs.resourceContext.uuid=:uuid").setParameter("uuid", fileServiceResourceContextUUID);
        FileService fs = tom.queryOne(q, FileService.class); 
        
        String columns = "type,id,resourceUUID,name,description,path,displayName,owner,url,"
                + "containsSharedVolumes,actions,resourceId,rootVolumeId,owningResourceId";
        // important to order by type first, so R comes before U and RZ in between them!
        String sql = String.format("SELECT %s FROM racm.userFileserviceResources(?,?) order by type,resourceId", columns);
        Query nativeQuery = tom.createNativeQuery(sql).setParameter(1, fileServiceResourceContextUUID).setParameter(2,up.getUsername());

        List<?> rawResults = tom.executeNativeQuery(nativeQuery);
        HashMap<Long,RootVolumeModel> rvs = new HashMap<Long,RootVolumeModel>();
        List<DataVolumeModel> dvs = new ArrayList<DataVolumeModel>();
        HashMap<Long,FileServiceResourceModel> resources = new HashMap<Long, FileServiceResourceModel>();
        for(Object o: rawResults) {
            Object[] row = (Object[])o;
            int ix = 0;
            String type = (String) row[ix++];
            Long id = (Long) row[ix++];
            String resourceUUID=(String)row[ix++];
            String name=(String)row[ix++];
            String description=(String)row[ix++];
            String path=(String)row[ix++];
            String displayName=(String)row[ix++];
            String owner=(String)row[ix++];
            String url=(String)row[ix++];
            Integer bit = (Integer)row[ix++];
            Boolean containsSharedVolumes = (bit == null || bit.equals(0))?false:true;
            List<String> actions = csv2list((String)row[ix++]);
            Long resourceId=(Long)row[ix++];
            Long rootVolumeId=(Long)row[ix++];
            Long owningResourceId=(Long)row[ix++];
            FileServiceResourceModel r = null;
            if("R".equals(type)) {
                r = new RootVolumeModel(id,resourceUUID,name,description,path,containsSharedVolumes,actions);
                rvs.put(id,(RootVolumeModel)r);
            }
            else if("RZ".equals(type)) {  // in case there are RootVOlumes the user has no actions on.
                RootVolumeModel rv = rvs.get(id);
                if(rv != null)
                    continue;
                r = new RootVolumeModel(id,resourceUUID,name,description,path,containsSharedVolumes,actions);
                rvs.put(id,(RootVolumeModel)r);
            }
            else if("D".equals(type)) {
                r = new DataVolumeModel(id,resourceUUID,name,description,displayName,path,url,actions);
                dvs.add((DataVolumeModel)r);
            }
            else if("U".equals(type)) {
                UserVolumeModel uv = new UserVolumeModel(id,resourceUUID,name,description,path,owner,actions);
                r=uv;
                RootVolumeModel rv = rvs.get(rootVolumeId);
                if(rv != null)   
                    rv.addUserVolume(uv);
                else
                    // TODO do real logging iso system.out.print
                    System.out.printf("UserVolume [%s,%s,%s,%s] cannot find RootVolume [%d]\n",uv.getResourceUUID(), uv.getName(), uv.getRelativePath(), uv.getOwner(), rootVolumeId);
            }
            if(r != null) {
                if(owningResourceId != null)
                    r.setOwningResourceId(owningResourceId);
                resources.put(resourceId, r);
            }
        }
        
        columns = "resourceid,resourceUUID,scientitytype,scientityid,scientityname,action";
        sql = String.format("SELECT %s FROM racm.userContextResourceShares(?,?) order by resourceId,scientityid", columns);
        nativeQuery = tom.createNativeQuery(sql).setParameter(1, fileServiceResourceContextUUID).setParameter(2,up.getUsername());
        
        rawResults = tom.executeNativeQuery(nativeQuery);
        SharedWithEntity s = null;
        Long currentResourceId = null;
        Long currentscientityid = null;
        FileServiceResourceModel currentResource = null;
        for(Object o: rawResults) {
            Object[] row = (Object[])o;
            int ix = 0;
            Long resourceId = (Long)row[ix++];
            if(!resourceId.equals(currentResourceId)) {
                currentResourceId = resourceId;
                currentResource = resources.get(resourceId);
                currentscientityid = null;  // needs resetting as well
            }
            if(currentResource != null) {
                String resourceUUID = (String)row[ix++];
                String scientitytype = (String)row[ix++];
                Long scientityid = (Long)row[ix++];
                String scientityname = (String)row[ix++];
                String action = (String)row[ix++];
                if(!scientityid.equals(currentscientityid)) {
                    SciServerEntityType t = SciServerEntityType.valueOf(scientitytype);
                    s = new SharedWithEntity(scientityid, scientityname, t, null);
                    if(!(t == SciServerEntityType.USER && currentResource instanceof UserVolumeModel
                            && scientityname.equals(((UserVolumeModel)currentResource).getOwner())))
                            if(!(currentResource instanceof RootVolumeModel))
                                currentResource.addSharedWith(s); 
                    currentscientityid = scientityid;
                }
                s.addAllowedAction(action);
            } else { // this would be a server error. log here iso returning anything to user
                // TODO do real logging iso system.out.print
                System.out.printf("currentResource == null for currentResourceId=%d: can happen e.g. if a __rootcontext__ has a privilege\n",
                        currentResourceId);
            }
        } 
        List<RootVolumeModel> lrvs = new ArrayList<RootVolumeModel>(rvs.values());
        FileServiceModel fsm = new FileServiceModel(fileServiceResourceContextUUID,fs.getName(),fs.getDescription(),fs.getApiEndpoint(),lrvs, dvs);
        
        return fsm;
    }

    private List<String> csv2list(String actionsCSV) {
        return actionsCSV == null ? new ArrayList<String>():Arrays.asList(actionsCSV.split(","));
    }
    
    public FileServiceModel getFileService(UserProfile up, String fileServiceIdentifer) {
        return maybeGetFileService(up, fileServiceIdentifer)
                .orElseThrow(() -> new ResourceNotFoundException("Cannot locate file service"));
    }

    private Optional<FileServiceModel> maybeGetFileService(UserProfile up, String fileServiceIdentifer) {
        return maybeGetFileService(up, getFileserviceObject(up.getTom(), fileServiceIdentifer));
    }

    private Optional<FileServiceModel> maybeGetFileService(UserProfile up, FileService fs) {
        Map<Long, List<String>> allowedActions = accessControl.getAllowedActions(up, fs.getResourceContext());
        if (allowedActions.entrySet().stream().allMatch(e -> e.getValue().isEmpty()))
            return Optional.empty();

        List<DataVolume> dataVolumes = getDataVolumes(fs);
        List<VOURPEntityWithResource> userVolumesToSearch = Stream.concat(
                fs.getUserVolumes().stream().filter(uv -> allowedActions.containsKey(uv.getResource().getId()))
                        .map(VOURPEntityWithResource::wrap),

                dataVolumes.stream().filter(uv -> allowedActions.containsKey(uv.getResource().getId()))
                        .map(VOURPEntityWithResource::wrap))
                .collect(Collectors.toList());

        Map<VOURPEntityWithResource, Map<SciserverEntity, List<Privilege>>> sharedWith = accessControl
                .getSharedPrivileges(userVolumesToSearch, up.getTom());

        return mapper.getListingDTO(fs, dataVolumes, allowedActions, sharedWith);
    }

    public RegisteredFileServiceModel registerFileService(UserProfile up, RegisterNewFileServiceModel fileService) {
        if (!accessControl.canRegisterFileService(up))
            throw new InsufficientPermissionsException("register a file service");

        FileService newFileService = mapper.createFileService(fileService, up.getTom());
        RegisteredFileServiceModel createdFileService = mapper.getRegisteredFileServiceView(newFileService,
                getDataVolumes(newFileService));

        racmUtil.assignRole(R_FILESERVICE_ADMIN, racmUtil.addRootContext(newFileService.getResourceContext()),
                up.getUser());

        if (!newFileService.getRootVolume().isEmpty()) {
            for (RootVolume rv : newFileService.getRootVolume()) {
                racmUtil.assignPrivileges(up.getUser(), rv.getResource(), A_FILESERVICE_ROOTVOLUME_CREATE,
                        A_FILESERVICE_ROOTVOLUME_GRANT);
            }
        }
        persistUncheckingException(up.getTom());

        return createdFileService;
    }

    public RegisteredRootVolumeModel registerRootVolume(UserProfile up, String fileServiceIdentifer,
            RegisterNewRootVolumeModel rootVolume) {
        return registerRootVolume(up, getFileserviceObject(up.getTom(), fileServiceIdentifer), rootVolume);
    }

    RegisteredRootVolumeModel registerRootVolume(UserProfile up, FileService fileService,
            RegisterNewRootVolumeModel rootVolume) {
        if (!accessControl.canEditFileService(up, fileService))
            throw new InsufficientPermissionsException("create a root volume");

        RootVolume rv = mapper.createRootVolume(rootVolume, fileService);

        racmUtil.assignPrivileges(up.getUser(), rv.getResource(), A_FILESERVICE_ROOTVOLUME_CREATE,
                A_FILESERVICE_ROOTVOLUME_GRANT);

        persistUncheckingException(up.getTom());
        return mapper.getRegisteredRootVolume(rv);
    }
    /* package-private visible for testing purposes */
     RegisteredUserVolumeModel registerUserVolume(UserProfile up, FileService fs, RootVolume rv,
            RegisterNewUserVolumeModel userVolume) throws InvalidTOMException{
         User user = up.getUser();

         if (!accessControl.canCreateUserVolume(up, rv))
             throw new InsufficientPermissionsException("create a user volume");

         if (getUserVolume(up.getTom(), userVolume.getName(), fs.getResourceContext().getUuid(), rv.getName(),
                 userVolume.getOwner().orElse(up.getUsername())).isPresent()) {
             throw new RegistrationInvalidException("User volume name must be unique for each user");
         }

         userVolume.getOwner().ifPresent(id -> {
             if (!id.equals(up.getUsername()))
                 throw new InsufficientPermissionsException("register a user volume as another user or group."
                         + " This functionality may be implemented later");
         });

         RegisterNewUserVolumeWithOwnerModel newUserVolume = new RegisterNewUserVolumeWithOwnerModel(
                 userVolume.getName(), userVolume.getDescription(), userVolume.getRelativePath(), user);

         ChangeSet changeSet = up.getTom().newChangeSet();
         
         UserVolume uv = mapper.createUserVolume(newUserVolume, rv, fs);
         changeSet.add(uv);
         changeSet.add(uv.getResource());
         
         List<Privilege> privs = racmUtil.assignPrivileges(user, uv.getResource(), A_FILESERVICE_USERVOLUME_READ, A_FILESERVICE_USERVOLUME_WRITE,
                 A_FILESERVICE_USERVOLUME_DELETE);
         for(Privilege priv : privs)
             changeSet.add(priv);
         
         if (rv.getContainsSharedVolumes()) {
             privs = racmUtil.assignPrivileges(user, uv.getResource(), A_FILESERVICE_USERVOLUME_GRANT);
             for(Privilege priv : privs)
                 changeSet.add(priv);
         }

         up.getTom().persistChangeSet(changeSet,  true);
         return mapper.getRegisteredUserVolume(uv);
    }

    public RegisteredUserVolumeModel registerUserVolume(UserProfile up, String fileServiceIdentifer,
            String rootVolumeName, RegisterNewUserVolumeModel userVolume) throws InvalidTOMException {
        FileService fs = getMinimalFileserviceObject(up.getTom(), fileServiceIdentifer);
        RootVolume rv = getRootVolume(up.getTom(), rootVolumeName, fs);
        return registerUserVolume(up, fs, rv, userVolume);
    }

    public RegisteredDataVolumeModel registerDataVolume(UserProfile up, String fileServiceIdentifer,
            RegisterNewDataVolumeModel dataVolume) {
        FileService fs = getFileserviceObject(up.getTom(), fileServiceIdentifer);

        if (!accessControl.canCreateDataVolume(up, fs))
            throw new InsufficientPermissionsException("create a data volume");

        if (getDataVolume(up.getTom(), fs.getResourceContext().getUuid(), dataVolume.getName()).isPresent()) {
            throw new RegistrationInvalidException("Data volume name must be unique for each file service");
        }
        DataVolume dv = mapper.createDataVolume(dataVolume, fs);

        racmUtil.assignPrivileges(up.getUser(), dv.getResource(), A_FILESERVICE_DATAVOLUME_READ,
                A_FILESERVICE_DATAVOLUME_WRITE, A_FILESERVICE_DATAVOLUME_DELETE, A_FILESERVICE_DATAVOLUME_GRANT,
                A_FILESERVICE_DATAVOLUME_EDIT);

        persistUncheckingException(up.getTom());
        return mapper.getRegisteredDataVolume(dv);
    }

    public Set<String> getRootVolumeActions(UserProfile up, String fileServiceIdentifer, String rootVolumeName) {
        return accessControl.getAllowedActions(up, fileServiceIdentifer, rootVolumeName);
    }

    public NativeQueryResult getDataVolumeActions(UserProfile up, String fileServiceIdentifer, String dataVolumeName) {
        return accessControl.getAllowedDataVolumeActions(up, fileServiceIdentifer, dataVolumeName);
    }

    public NativeQueryResult getUserVolumeActions(UserProfile up, String fileServiceIdentifer, String rootVolumeName,
            String userVolumeOwnerName, String userVolumeName) {
        return accessControl.getAllowedUserVolumeActions(up, fileServiceIdentifer,
                UriComponentsBuilder.fromPath(rootVolumeName).pathSegment(userVolumeOwnerName)
                        .pathSegment(userVolumeName).build().toUriString());
    }
    public void removeMyShares(UserProfile up, String fileServiceIdentifer, String rootVolumeName,
            String userVolumeOwnerName, String userVolumeName) {
        UserVolume uv = getUserVolume(up.getTom(),
                userVolumeName, fileServiceIdentifer, rootVolumeName, userVolumeOwnerName)
                .orElseThrow(() ->  new ResourceNotFoundException(UNKNOWN_USER_VOLUME_MESSAGE));

        if (accessControl.isUserVolumeOwned(uv))
            throw new InsufficientPermissionsException(
                    "user volume owned by resource, can only remove access through owning service");

        if (up.getUsername().equals(userVolumeOwnerName))
            throw new InsufficientPermissionsException("cannot unshare owned user volume");

        accessControl.deleteAllPrivileges(up.getTom(), up.getId(), uv.getResource());
        accessControl.deleteAllRoles(up.getTom(), up.getId(), uv.getResource());

        persistUncheckingException(up.getTom());
    }

    public NativeQueryResult getServiceVolumeActions(UserProfile up, String fileServiceIdentifer, String serviceToken,
            String rootVolumeName, String userVolumeOwnerName, String userVolumeName) {
        return accessControl.getAllowedServiceVolumeActions(serviceToken, up, fileServiceIdentifer,
                UriComponentsBuilder.fromPath(rootVolumeName).pathSegment(userVolumeOwnerName)
                        .pathSegment(userVolumeName).build().toUriString());
    }

    public void deleteUserVolume(UserProfile up, String fileServiceIdentifer, String rootVolumeName,
            String userVolumeOwnerName, String userVolumeName) {
        UserVolume uv = getUserVolume(up.getTom(), userVolumeName, fileServiceIdentifer, rootVolumeName,
                userVolumeOwnerName).orElseThrow(() -> new ResourceNotFoundException(UNKNOWN_USER_VOLUME_MESSAGE));

        // TBD whether this check should be done at the RACM level already
        if (accessControl.isUserVolumeOwned(uv))
            throw new InsufficientPermissionsException(
                    "user volume owned by resource, can only be deleted as a service volume");

        if (!accessControl.canUserDeleteUserVolume(up, uv))
            throw new InsufficientPermissionsException("delete this user volume");

        ArrayList<MetadataObject> os = new ArrayList<>();
        os.add(uv);
        os.add(uv.getResource());
        up.getTom().delete(os);
    }

    public void deleteFileService(UserProfile up, String fileServiceIdentifer) {
        FileService fs = getFileserviceObject(up.getTom(), fileServiceIdentifer);
        if (!accessControl.canEditFileService(up, fs))
            throw new InsufficientPermissionsException("delete this file service");

        for (DataVolume dv : getDataVolumes(fs)) {
            deleteDataVolume(up.getTom(), fs, dv);
        }
        up.getTom().remove(fs);
        fs.getResourceContext().setTom(up.getTom());
        up.getTom().remove(fs.getResourceContext());
        persistUncheckingException(up.getTom());
    }

    public void deleteRootVolume(UserProfile up, String fileServiceIdentifer, String rootVolumeName) {
        FileService fs = getFileserviceObject(up.getTom(), fileServiceIdentifer);
        if (!accessControl.canEditFileService(up, fs))
            throw new InsufficientPermissionsException("delete this root volume");

        RootVolume rv = getRootVolume(up.getTom(), rootVolumeName, fs);

        deleteRootVolume(fs, rv);
        persistUncheckingException(up.getTom());
    }

    private void deleteRootVolume(FileService fs, RootVolume rv) {
        fs.getRootVolume().remove(rv);
        for (UserVolume uv : fs.getUserVolumes()) {
            if (uv.getRootVolume().equals(rv)) {
                deleteUserVolume(fs, uv);
            }
        }
        fs.getResourceContext().setTom(fs.getTom());
        fs.getResourceContext().getResource().remove(rv.getResource());
    }

    private void deleteUserVolume(FileService fs, UserVolume uv) {
        fs.getUserVolumes().remove(uv);
        fs.getResourceContext().setTom(fs.getTom());
        fs.getResourceContext().getResource().remove(uv.getResource());
    }

    public void deleteDataVolume(UserProfile up, String fileServiceIdentifer, String dataVolumeName) {
        DataVolume dv = getDataVolume(up.getTom(), fileServiceIdentifer, dataVolumeName)
                .orElseThrow(() -> new ResourceNotFoundException("Could not locate data volume: " + dataVolumeName));
        if (!accessControl.canUserDeleteDataVolume(up, dv))
            throw new InsufficientPermissionsException("delete this data volume");

        deleteDataVolume(up.getTom(), dv.getFileService(), dv);
        persistUncheckingException(up.getTom());
    }

    private void deleteDataVolume(TransientObjectManager tom, FileService fs, DataVolume dv) {
        fs.getResourceContext().setTom(tom);
        fs.getResourceContext().getResource().remove(dv.getResource());

        dv.setTom(tom);
        tom.remove(dv);
    }

    public void updateSharing(UserProfile up, String fileServiceIdentifer, String rootVolumeName,
            String userVolumeOwnerName, String userVolumeName, UpdateSharedWithEntry updatedEntry) {
        UserVolume uv = getUserVolume(up.getTom(), userVolumeName, fileServiceIdentifer, rootVolumeName,
                userVolumeOwnerName).orElseThrow(() -> new ResourceNotFoundException(UNKNOWN_USER_VOLUME_MESSAGE));

        if (!accessControl.canUserShareUserVolume(up, uv))
            throw new InsufficientPermissionsException("share this user volume");

        SciserverEntity entity = getEntityForSharing(up, updatedEntry);

        accessControl.deleteAllPrivileges(up.getTom(), entity.getId(), uv.getResource());
        accessControl.assignPrivilages(entity, uv.getResource(),
                updatedEntry.getAllowedActions().toArray(new String[] {}));

        persistUncheckingException(up.getTom());
    }

    public void updateSharingOfDataVolumes(UserProfile up, String fileServiceIdentifer, String dataVolumeName,
            UpdateSharedWithEntry updatedEntry) {
        DataVolume dv = getDataVolume(up.getTom(), fileServiceIdentifer, dataVolumeName)
                .orElseThrow(() -> new ResourceNotFoundException("Could not locate data volume"));

        if (!accessControl.canUserShareDataVolume(up, dv))
            throw new InsufficientPermissionsException("share this data volume");

        SciserverEntity entity = getEntityForSharing(up, updatedEntry);

        accessControl.deleteAllPrivileges(up.getTom(), entity.getId(), dv.getResource());
        accessControl.assignPrivilages(entity, dv.getResource(),
                updatedEntry.getAllowedActions().toArray(new String[] {}));

        persistUncheckingException(up.getTom());
    }

    /**
     * ONLY allow users or groups to share through this method.<br/>
     * 
     * @param up
     * @param updatedEntry
     * @return
     */
    private SciserverEntity getEntityForSharing(UserProfile up, UpdateSharedWithEntry updatedEntry) {
        SciserverEntity entity = null;
        Optional<Long> entityId = updatedEntry.getId();
        Optional<String> name = updatedEntry.getName(); // for ServiceAccount the publisherDID

        if (updatedEntry.getType() == SciServerEntityType.USER) {
            if (entityId.isPresent())
                entity = up.getTom().find(User.class, entityId.get());
            else {
                Query q = up.getTom().createQuery("SELECT u from User u WHERE u.username = :name");
                q.setParameter("name", name.orElseThrow(
                        () -> new RegistrationInvalidException("Name or id must be set when sharing volumes")));
                entity = up.getTom().queryOne(q, User.class);
            }
        } else if (updatedEntry.getType() == SciServerEntityType.GROUP) {
            if (entityId.isPresent())
                entity = up.getTom().find(UserGroup.class, entityId.get());
            else {
                Query q = up.getTom().createQuery("SELECT g from UserGroup g WHERE g.name = :name");
                q.setParameter("name", name.orElseThrow(
                        () -> new RegistrationInvalidException("Name or id must be set when sharing volumes")));
                entity = up.getTom().queryOne(q, UserGroup.class);
            }
            if (((UserGroup) entity).getName().equals(RACMNames.USERGROUP_PUBLIC) && !up.isAdmin()) {
                throw new InsufficientPermissionsException("share this user volume with the public group");
            }
        } else if (updatedEntry.getType() == SciServerEntityType.SERVICE) {
            if (entityId.isPresent())
                entity = up.getTom().find(ServiceAccount.class, entityId.get());
            else {
                Query q = up.getTom().createQuery("SELECT a from ServiceAccount a WHERE a.publisherDID = :pubDID");
                q.setParameter("pubDID", name.orElseThrow(
                        () -> new RegistrationInvalidException("Name or id must be set when sharing volumes")));
                entity = up.getTom().queryOne(q, ServiceAccount.class);
            }
            if (!up.isAdmin()) {
                throw new InsufficientPermissionsException("share this user volume with a service account");
            }
        }
        return entity;
    }

    public void updateUserVolume(UserProfile up, String fileServiceIdentifer, String rootVolumeName,
            String userVolumeOwnerName, String userVolumeName, UpdatedUserVolumeInfo updatedUserVolumeInfo) {
        UserVolume uv = getUserVolume(up.getTom(), userVolumeName, fileServiceIdentifer, rootVolumeName,
                userVolumeOwnerName).orElseThrow(() -> new ResourceNotFoundException(UNKNOWN_USER_VOLUME_MESSAGE));

        if (!uv.getOwner().equals(up.getUser()))
            throw new InsufficientPermissionsException("update this user volume");

        updatedUserVolumeInfo.getName().ifPresent(name -> {
            if (!name.equals(userVolumeName)
                    && getUserVolume(up.getTom(), name, fileServiceIdentifer, rootVolumeName, userVolumeOwnerName)
                            .isPresent()) {
                throw new RegistrationInvalidException("User volume name must be unique for each user");
            }

            uv.setName(name);
            uv.getResource().setPublisherDID(UriComponentsBuilder.fromPath(uv.getRootVolume().getName())
                    .pathSegment(up.getUsername()).pathSegment(uv.getName()).build().toUriString());
        });
        updatedUserVolumeInfo.getDescription().ifPresent(uv::setDescription);

        persistUncheckingException(up.getTom());
    }

    public void updateRootVolume(UserProfile up, String fileServiceIdentifier, String rootVolumeName,
            UpdatedRootVolumeInfo updatedRootVolumeInfo) {
        FileService fs = getFileserviceObject(up.getTom(), fileServiceIdentifier);
        if (!accessControl.canEditFileService(up, fs))
            throw new InsufficientPermissionsException("update this root volume");

        RootVolume rv = getRootVolume(up.getTom(), rootVolumeName, fs);

        updatedRootVolumeInfo.getName().ifPresent(rv::setName);
        updatedRootVolumeInfo.getName().ifPresent(rv.getResource()::setPublisherDID);
        updatedRootVolumeInfo.getDescription().ifPresent(rv::setDescription);
        updatedRootVolumeInfo.getPathOnFileSystem().ifPresent(rv::setPathOnFileSystem);
        updatedRootVolumeInfo.getContainsSharedVolumes().ifPresent(rv::setContainsSharedVolumes);

        for (UserVolume uv : fs.getUserVolumes()) {
            if (uv.getRootVolume().getId().equals(rv.getId())) {
                uv.getResource().setPublisherDID(UriComponentsBuilder.fromPath(rv.getName())
                        .pathSegment(uv.getOwner().getUsername()).pathSegment(uv.getName()).build().toUriString());
            }
        }

        persistUncheckingException(up.getTom());
    }

    public void updateDataVolume(UserProfile up, String fileServiceIdentifier, String dataVolumeName,
            UpdatedDataVolumeInfo updatedDataVolumeInfo) {
        DataVolume dv = getDataVolume(up.getTom(), fileServiceIdentifier, dataVolumeName)
                .orElseThrow(() -> new ResourceNotFoundException("Could not locate data volume"));
        if (!accessControl.canUserEditDataVolume(up, dv))
            throw new InsufficientPermissionsException("update this data volume");

        updatedDataVolumeInfo.getName().ifPresent(newDataVolumeName -> {
            if (!dv.getName().equals(newDataVolumeName)
                    && getDataVolume(up.getTom(), fileServiceIdentifier, newDataVolumeName).isPresent()) {
                throw new RegistrationInvalidException("Data volume name must be unique for each file service");
            }
            dv.setName(newDataVolumeName);
            dv.getResource().setPublisherDID(newDataVolumeName);
        });
        if (updatedDataVolumeInfo.getDescription().isPresent()) {
            dv.setDescription(updatedDataVolumeInfo.getDescription().get());
            dv.getResource().setDescription(updatedDataVolumeInfo.getDescription().get());
        }
        if (updatedDataVolumeInfo.getDisplayName().isPresent()) {
            dv.setDisplayName(updatedDataVolumeInfo.getDisplayName().get());
            dv.getResource().setName(updatedDataVolumeInfo.getDisplayName().get());
        }
        updatedDataVolumeInfo.getPathOnFileSystem().ifPresent(dv::setPathOnFileSystem);
        updatedDataVolumeInfo.getUrl().ifPresent(dv::setUrl);

        persistUncheckingException(up.getTom());
    }

    public void updateFileService(UserProfile up, String fileServiceIdentifier,
            UpdatedFileServiceInfo updatedFileServiceInfo) {
        FileService fs = getFileserviceObject(up.getTom(), fileServiceIdentifier);
        if (!accessControl.canEditFileService(up, fs))
            throw new InsufficientPermissionsException("update this file service");

        updatedFileServiceInfo.getName().ifPresent(fs::setName);
        updatedFileServiceInfo.getDescription().ifPresent(fs::setDescription);
        updatedFileServiceInfo.getApiEndpoint().ifPresent(apiEndpoint -> {
            fs.setApiEndpoint(apiEndpoint);
            fs.getResourceContext().setRacmEndpoint(apiEndpoint);
        });

        persistUncheckingException(up.getTom());
    }

    private FileService getFileserviceObject(TransientObjectManager tom, String fileServiceIdentifer) {
        Query q = tom.createQuery("SELECT f FROM FileService f WHERE f.resourceContext.uuid = :uuid")
                .setParameter("uuid", fileServiceIdentifer);
        q.setHint(QueryHints.LEFT_FETCH, "f.rootVolume.resource");
        q.setHint(QueryHints.LEFT_FETCH, "f.userVolumes.resource");
        q.setHint(QueryHints.LEFT_FETCH, "f.userVolumes.rootVolume");
        q.setHint(QueryHints.LEFT_FETCH, QUERY_PATH_FS_RESOURCE_CONTEXT);
        return tom.queryOne(q, FileService.class);
    }
    /**
     * Return only a FileService, its rootvolumes and its resourcecontext.<br/>
     * Useful for registering new uservolumes. 
     * @param tom
     * @param fileServiceIdentifer
     * @return
     */
    private FileService getMinimalFileserviceObject(TransientObjectManager tom, String fileServiceIdentifer) {
        Query q = tom.createQuery("SELECT f FROM FileService f WHERE f.resourceContext.uuid = :uuid")
                .setParameter("uuid", fileServiceIdentifer);
        q.setHint(QueryHints.LEFT_FETCH, QUERY_PATH_FS_RESOURCE_CONTEXT);
        q.setHint(QueryHints.LEFT_FETCH, "f.rootVolume");
        return tom.queryOne(q, FileService.class);
    }
    /**
     * TODO why not just ask the fileservice for its root volumes and select the one
     * with the right name?<br/>
     * Maybe inefficient, might bring too much data for all these other rootvolumes
     * if they are requested one at a time.
     * 
     * @param tom
     * @param name
     * @param fileService
     * @return
     */
    private RootVolume getRootVolume(TransientObjectManager tom, String name, FileService fileService) {
        Query q = tom.createQuery("SELECT rv from RootVolume rv where rv.name = :name and rv.container = :fs")
                .setParameter("name", name).setParameter("fs", fileService);
        RootVolume rv = tom.queryOne(q, RootVolume.class);
        if (rv == null) {
            throw new ResourceNotFoundException("Could not locate root volume with name=\"" + name
                    + "\" on file service \"" + fileService.getName() + "\"");
        }
        return rv;
    }

    private Optional<UserVolume> getUserVolume(TransientObjectManager tom, String userVolumeName,
            String fileServiceIdentifer, String rootVolumeName, String userVolumeOwnerName) {
        Query q = tom.createQuery(
                "SELECT uv from UserVolume uv WHERE uv.name = :name AND uv.container.resourceContext.uuid = :fileServiceIdentifer AND uv.rootVolume.name = :rootVolumeName AND uv.owner.username = :ownerUserName")
                .setParameter("name", userVolumeName).setParameter("fileServiceIdentifer", fileServiceIdentifer)
                .setParameter("rootVolumeName", rootVolumeName).setParameter("ownerUserName", userVolumeOwnerName);
        return Optional.ofNullable(tom.queryOne(q, UserVolume.class));
    }

    private Optional<DataVolume> getDataVolume(TransientObjectManager tom, String fileServiceIdentifer,
            String dataVolumeName) {
        Query q = tom.createQuery(
                "SELECT dv from DataVolume dv WHERE dv.name = :name AND dv.fileService.resourceContext.uuid = :fileServiceIdentifer")
                .setParameter("name", dataVolumeName).setParameter("fileServiceIdentifer", fileServiceIdentifer);
        return Optional.ofNullable(tom.queryOne(q, DataVolume.class));
    }

    List<DataVolume> getDataVolumes(FileService fs) {
        // (hopefully temporary) hack for unit tests
        // skip queries if there is no non-mocked tom
        Query q = fs.getTom().createQuery("SELECT dv FROM DataVolume dv WHERE dv.fileService = :fs");
        if (q == null)
            return emptyList();
        q.setParameter("fs", fs);
        try {
            return fs.getTom().queryJPA(q, DataVolume.class);
        } catch (VOURPException e) {
            throw new IllegalStateException(e);
        }
    }

    /*
     * Convert InvalidTOMException to a runtime exception. This probably should be
     * wrapped in a vo-urp-dependent exception, but at least this avoids classes in
     * higher layers from having to catch this.
     */
    private void persistUncheckingException(TransientObjectManager tom) {
        try {
            tom.persist();
        } catch (InvalidTOMException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * This method registers a "servicevolume", that is a uservolume that is owned
     * by a Resource that contained in another service (i.e. *not* the file
     * service).<br/>
     * 
     * That service is identified by the servicetoken inside the serviceVolume. The
     * volume will generally be
     * 
     * @param owner                the user who will own the new volume, though it
     *                             can only be fully managed in the context of the
     *                             owning service
     * @param fileServiceIdentifer
     * @param rootVolumeName
     * @param serviceVolume        details about the new volume, including whether
     *                             another Resource might "own" the volume.
     * @return RegisteredServiceVolumeModel
     * @throws InvalidTOMException
     */
    public RegisteredServiceVolumeModel registerServiceVolume(UserProfile owner, String fileServiceIdentifer,
            String rootVolumeName, RegisterNewServiceVolumeModel serviceVolume) throws InvalidTOMException{
        TransientObjectManager tom = owner.getTom();

        ServiceAccount serviceAccount = RACMUtil.findServiceAccount(serviceVolume.getServiceToken(), tom);

        FileService fs = getFileserviceObject(tom, fileServiceIdentifer);
        RootVolume rv = getRootVolume(tom, rootVolumeName, fs);

        // TODO the following checks:
        if (serviceVolume.getOwningResourceUUID() == null)
            throw new IllegalArgumentException("Service volume must have an owning resource UUID");

        // 1. can the serviceaccount create a uservolume in the rootvolume?
        if (!accessControl.canCreateServiceVolume(serviceVolume.getServiceToken(), rv))
            throw new InsufficientPermissionsException("service cannot create a service volume on root volume");

        // 2. IF there is a resource that should own the volume, is it owned by the
        // service account? And does it exist?
        Resource owningResource = racmUtil.queryServiceResource(serviceAccount, serviceVolume.getOwningResourceUUID());
        if (owningResource == null)
            throw new IllegalArgumentException("Service volume request made with non-existing owning resource UUID");

        if (getUserVolume(tom, serviceVolume.getName(), fs.getResourceContext().getUuid(), rv.getName(),
                owner.getUsername()).isPresent()) {
            throw new RegistrationInvalidException("User volume name must be unique for each user");
        }

        UserVolume uv = mapper.createServiceVolume(owner.getUser(), serviceVolume, rv, fs);
        
        ChangeSet changeSet = tom.newChangeSet();
        changeSet.add(uv);
        changeSet.add(uv.getResource());
        
        List<Privilege> privs = racmUtil.assignPrivileges(serviceAccount, uv.getResource(), A_FILESERVICE_USERVOLUME_READ,
                A_FILESERVICE_USERVOLUME_WRITE, A_FILESERVICE_USERVOLUME_DELETE, A_FILESERVICE_USERVOLUME_GRANT);
        
        for (NewSharedWithEntity share : serviceVolume.getShares()) {
            SciserverEntity e = RACMUtil.querySciServerEntity(tom, share.getName(), share.getType());
            privs.addAll(racmUtil.assignPrivileges(e, uv.getResource(), share.getAllowedActions().toArray(new String[] {})));
        }
        for(Privilege priv : privs)
            changeSet.add(priv);

        AssociatedResource ar = new AssociatedResource(owningResource);
        ar.setOwnership(OwnershipCategory.OWNED);
        ar.setResource(uv.getResource());
        ar.setUsage(serviceVolume.getUsage());
        
        changeSet.add(ar);

        tom.persistChangeSet(changeSet, true);

        return mapper.getRegisteredServiceVolume(uv, owningResource, ar);
    }

    /**
     * Delete a user volume owned by a resource on a service.<br/>
     * Deleting the associatedresource should be automatic from the FK relationship
     * it has to the re
     * 
     * @param up
     * @param fileServiceIdentifer
     * @param rootVolumeName
     * @param userVolumeOwnerName
     * @param userVolumeName
     * @param serviceToken
     */
    public void deleteServiceVolume(UserProfile up, String fileServiceIdentifer, String rootVolumeName,
            String userVolumeOwnerName, String userVolumeName, String serviceToken) {
        UserVolume uv = getUserVolume(up.getTom(), userVolumeName, fileServiceIdentifer, rootVolumeName,
                userVolumeOwnerName).orElseThrow(() -> new ResourceNotFoundException(UNKNOWN_USER_VOLUME_MESSAGE));

        if (!accessControl.isUserVolumeOwnedByService(serviceToken, uv))
            throw new InsufficientPermissionsException("user volume is not owned by service.");

        if (!accessControl.canServiceDeleteUserVolume(serviceToken, uv))
            throw new InsufficientPermissionsException("delete this user volume");

        ArrayList<MetadataObject> os = new ArrayList<>();
        os.add(uv);
        os.add(uv.getResource());
        up.getTom().delete(os);
    }

}