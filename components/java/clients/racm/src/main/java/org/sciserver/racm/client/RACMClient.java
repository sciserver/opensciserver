package org.sciserver.racm.client;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.sciserver.authentication.client.UnauthenticatedException;
import org.sciserver.clientutils.Client;
import org.sciserver.clientutils.SciServerClientException;
import org.sciserver.racm.jobm.model.COMPMJobModel;
import org.sciserver.racm.jobm.model.DatabaseContextModel;
import org.sciserver.racm.jobm.model.RDBComputeDomainModel;
import org.sciserver.racm.jobm.model.RDBJobModel;
import org.sciserver.racm.jobm.model.UserDockerComputeDomainModel;
import org.sciserver.racm.rctree.model.ResourceGrants;
import org.sciserver.racm.resourcecontext.model.AssociatedSciserverEntityModel;
import org.sciserver.racm.resourcecontext.model.NewResourceModel;
import org.sciserver.racm.resourcecontext.model.PrivilegeModel;
import org.sciserver.racm.resourcecontext.model.RegisteredResourceModel;
import org.sciserver.racm.resourcecontext.model.ResourceFromUserPerspectiveModel;
import org.sciserver.racm.resourcecontext.model.ServiceResourceFromUserPerspectiveModel;
import org.sciserver.racm.resources.model.UserResourceModel;
import org.sciserver.racm.storem.model.FileServiceModel;
import org.sciserver.racm.storem.model.MinimalFileServiceModel;
import org.sciserver.racm.storem.model.RegisterNewServiceVolumeModel;
import org.sciserver.racm.storem.model.RegisterNewUserVolumeModel;
import org.sciserver.racm.storem.model.RegisteredServiceVolumeModel;
import org.sciserver.racm.storem.model.UpdateSharedWithEntry;
import org.sciserver.racm.storem.model.UpdatedUserVolumeInfo;
import org.sciserver.racm.ugm.model.CreateLinkedGroupModel;
import org.sciserver.racm.ugm.model.GroupInfo;
import org.sciserver.racm.ugm.model.MemberStatus;
import org.sciserver.racm.ugm.model.PersonalUserInfo;
import org.sciserver.racm.utils.model.NativeQueryResult;
import retrofit2.Call;

/**
 * Client for calling remote RACM APIs.
 *
 * <p>At the moment, all calls are synchronous. Successful calls return the desired
 * object type. JSON parse errors in a 200 response along other IO errors
 * preventing a valid HTTP response raise a SciServerClientException with a 502
 * httpCode. All other responses raise a SciServerClientException with the
 * origin httpCode and message.
 */
public class RACMClient extends Client<RACMClientInterface> {
    private ObjectMapper fieldOnlyObjectMapper;

    public RACMClient(String racmEndpoint) {
        this(racmEndpoint, null);
    }

    public RACMClient(String racmEndpoint, String racmServiceToken) {
        super(racmEndpoint, racmServiceToken, RACMClientInterface.class);
        fieldOnlyObjectMapper = new ObjectMapper();
        fieldOnlyObjectMapper.setVisibility(PropertyAccessor.ALL, Visibility.NONE);
        fieldOnlyObjectMapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
    }

    public JsonNode getPrivileges(String resourceUUID, String userToken) throws SciServerClientException {
        Call<JsonNode> call = retrofitAdapter.getPrivilegesCall(resourceUUID, userToken);
        return getSyncResponse(call);
    }

    /**
     * This method explicitly parses the JsonNode using a specially configured
     * ObjectMapper.<br>
     * This is because the default one deserializer tries using
     * NativeQueryResult::setColumns(String) setter with a String[].
     *
     * <p>TODO Consider more elegant solution for this. Configured, or whatever. We
     * could insist on NativeQueryResult having (only) a setter with String[].
     * Philosophically one might claim a server need not be designed for some custom
     * client that decides to choose some "weird" json-based message transfer. I.e.
     * it is up to the client to deal with quirks in the server, such as we
     * implement here.
     *
     * @param userToken single -sign-on token assigned to the user
     * @return NativeQueryResult containing the resources a user has access to with
     *         the actions the user can apply to the resource
     * @throws SciServerClientException when something bad happens
     */
    public NativeQueryResult queryUserResources(String userToken) throws SciServerClientException {
        Call<JsonNode> call = retrofitAdapter.queryUserResourcesCall(userToken);
        JsonNode jn = getSyncResponse(call);
        if (jn != null) {
            try {
                NativeQueryResult nqr = fieldOnlyObjectMapper.treeToValue(jn, NativeQueryResult.class);
                return nqr;
            } catch (JsonProcessingException e) {
                throw new SciServerClientException("Error parsing JsonNode to NativeQueryResult", 500);
            }
        } else {
            return null;
        }
    }

    public ResourceGrants postResourceGrants(String userToken, ResourceGrants resourceGrants)
            throws SciServerClientException {
        Call<ResourceGrants> call = retrofitAdapter.postResourceGrantsCall(userToken, resourceGrants);
        return getSyncResponse(call);
    }

    public Boolean canUserDoRootAction(String resourceContextUUID, String action, String userToken)
            throws SciServerClientException {
        Call<Boolean> call = retrofitAdapter.canUserDoRootActionCall(resourceContextUUID, action, userToken);
        return getSyncResponse(call);
    }

    public Boolean canUserDoResourceAction(String resourceUUID, String action, String userToken)
            throws SciServerClientException {
        Call<Boolean> call = retrofitAdapter.canUserDoResourceActionCall(resourceUUID, action, userToken);
        return getSyncResponse(call);
    }

    public RegisteredResourceModel createResource(String resourceContextUUID, NewResourceModel resource,
            String userToken) throws SciServerClientException {
        Call<RegisteredResourceModel> call = retrofitAdapter.createResourceCall(resourceContextUUID, resource,
                userToken);
        return getSyncResponse(call);
    }

    public Void deleteResource(String resourceContextUUID, String resourceUUID, String userToken)
            throws SciServerClientException {
        Call<Void> call = retrofitAdapter.deleteResourceCall(resourceContextUUID, resourceUUID, userToken);
        return getSyncResponse(call);
    }

    public ResourceFromUserPerspectiveModel getResource(String resourceContextUUID, String resourceUUID,
            String userToken) throws SciServerClientException {
        Call<ResourceFromUserPerspectiveModel> call = retrofitAdapter.getResourceCall(resourceContextUUID, resourceUUID,
                userToken);
        return getSyncResponse(call);
    }

    public ServiceResourceFromUserPerspectiveModel getServiceResource(String resourceContextUUID, String resourceUUID,
            String userToken) throws SciServerClientException {
        Call<ServiceResourceFromUserPerspectiveModel> call = retrofitAdapter.getServiceResourceCall(resourceContextUUID,
                resourceUUID, userToken);
        return getSyncResponse(call);
    }

    public Boolean updateResourceMetadata(String resourceContextUUID, String resourceUUID, String name,
            String description, String userToken) throws SciServerClientException {
        Call<Boolean> call = retrofitAdapter.updateResourceMetadataCall(resourceContextUUID, resourceUUID, name,
                description, userToken);
        return getSyncResponse(call);
    }

    public GroupInfo manageGroup(GroupInfo groupInfo, String useToken) throws SciServerClientException {
        Call<GroupInfo> call = retrofitAdapter.manageGroupCall(groupInfo, useToken);
        return getSyncResponse(call);
    }

    public GroupInfo createLinkedGroup(CreateLinkedGroupModel linkedGroup, String userToken, String resourceUUID)
            throws SciServerClientException {
        Call<GroupInfo> call = retrofitAdapter.createLinkedGroupCall(linkedGroup, userToken, resourceUUID);
        return getSyncResponse(call);
    }

    public Void addNewPrivilege(String resourceContextUUID, String resourceUUID, List<PrivilegeModel> newPrivileges,
            String userToken) throws SciServerClientException {
        Call<Void> call = retrofitAdapter.addNewPrivilegeCall(resourceContextUUID, resourceUUID, newPrivileges,
                userToken);
        return getSyncResponse(call);
    }

    public Void associateWithSciserverEntity(String resourceContextUUID, String resourceUUID,
            AssociatedSciserverEntityModel associatedSciserverEntity, String userToken)
            throws SciServerClientException {
        Call<Void> call = retrofitAdapter.associateWithSciserverEntityCall(resourceContextUUID, resourceUUID,
                associatedSciserverEntity, userToken);
        return getSyncResponse(call);
    }

    /**
     * Return a list of groups the user is an OWNER or ADMIN of (e.g. has write permission to) and info therein.
     *
     * @param userToken the authentication token of User
     * @return list of group info
     *
     * @throws SciServerClientException when something bad happens
     */
    public List<GroupInfo> getUserGroups(String userToken) throws SciServerClientException {
        Call<List<GroupInfo>> call = retrofitAdapter.getUserGroupsCall(userToken);
        return getSyncResponse(call);
    }

    /**
     * Return a list of group NAMES that the user is considered a member of, e.g. accepted or owned.
     *
     * @param userToken the authentication token of User
     * @return list of group names
     *
     * @throws SciServerClientException when call fails or serialization issues
     * @throws Exception in case of unexpected errors in the data
     */
    public List<String> getGroupsUserIsMemberOf(String userToken) throws Exception {
        NativeQueryResult res = fieldOnlyObjectMapper.treeToValue(
            getSyncResponse(retrofitAdapter.getGroupsUserIsMemberOfCall(userToken)),
            NativeQueryResult.class);
        List<String> groups = new ArrayList<String>();
        int nameidx = Arrays.asList(res.getColumns()).indexOf("name");
        int statusidx = Arrays.asList(res.getColumns()).indexOf("status");
        for (Object[] row : res.getRows()) {
            String status = (String) (row[statusidx]);
            if (status.equals(MemberStatus.ACCEPTED.value()) || status.equals(MemberStatus.OWNER.value())) {
                groups.add((String) (row[nameidx]));
            }
        }
        return groups;
    }

    public GroupInfo queryMyGroup(Long groupid, String userToken, String serviceToken)
            throws SciServerClientException {
        Call<GroupInfo> call = retrofitAdapter.queryMyGroupCall(groupid, userToken, serviceToken);
        return getSyncResponse(call);
    }

    public GroupInfo queryResourceOwnedGroup(Long groupid, String userToken, String serviceToken)
            throws SciServerClientException {
        Call<GroupInfo> call = retrofitAdapter.queryResourceOwnedGroupCall(groupid, userToken, serviceToken);
        return getSyncResponse(call);
    }

    public Set<ResourceFromUserPerspectiveModel> queryUserRCResources(String userToken, String resourceContextUUID)
            throws SciServerClientException {
        Call<Set<ResourceFromUserPerspectiveModel>> call = retrofitAdapter.queryUserRCResources(userToken,
                resourceContextUUID);
        return getSyncResponse(call);
    }

    public List<UserResourceModel> queryUserRCResourcesAsTable(String userToken, String resourceContextUUID)
            throws SciServerClientException {
        Call<List<UserResourceModel>> call = retrofitAdapter.queryUserRCResourcesAsTableCall(userToken,
                resourceContextUUID);
        return getSyncResponse(call);
    }

    public PersonalUserInfo getUser(String userToken) throws SciServerClientException {
        Call<PersonalUserInfo> call = retrofitAdapter.getUserCall(userToken);
        return getSyncResponse(call);
    }

    /**
     * Check a service token against compm endpoint.
     *
     * @param serviceToken service token to check
     *
     * @throws UnauthenticatedException in case token is not valid or not authorized
     * @throws SciServerClientException in case of bad response or IO issue
     */
    public void checkServiceToken(String serviceToken) throws SciServerClientException, UnauthenticatedException {
        Call<Void> call = retrofitAdapter.checkServiceIdCall(serviceToken);
        try {
            getSyncResponse(call);
        } catch (SciServerClientException e) {
            if (e.httpCode() >= 400 && e.httpCode() < 500) {
                throw new UnauthenticatedException("service token not valid or not authorized");
            }
            throw e;
        }
    }


    /**
     * Return information about the status of the specified job.  Status is returned only if the user is allowed to
     * view information about the specified job.
     *
     * @param userToken token
     * @param jobId job ID
     * @return COMPMJobModel status of the job from the user perspective
     *
     * @throws SciServerClientException in case of bad response or IO issue
     */
    public COMPMJobModel jobStatus(String userToken, Long jobId) throws SciServerClientException {
        Call<COMPMJobModel> call = retrofitAdapter.jobStatusCall(userToken, jobId);
        return getSyncResponse(call);
    }

    /**
     * Return information about the status of the specified RDB job.  Status is returned only if the user is allowed to
     * view information about the specified RDB job.
     *
     * @param userToken token
     * @param jobId job ID
     * @return RDBJobModel status of the job from the user perspective
     *
     * @throws SciServerClientException in case of bad response or IO issue
     */
    public RDBJobModel rdbJobStatus(String userToken, Long jobId) throws SciServerClientException {
        Call<RDBJobModel> call = retrofitAdapter.rdbJobStatusCall(userToken, jobId);
        return getSyncResponse(call);
    }

    /**
     * Return information about the status of the user's submitted RDB jobs.
     *
     * @param userToken token
     * @return List of RDBJobModel statuses of the user's jobs
     *
     * @throws SciServerClientException in case of bad response or IO issue
     */
    public List<RDBJobModel> queryUserRdbJobs(String userToken) throws SciServerClientException {
        Call<List<RDBJobModel>> call = retrofitAdapter.queryUserRdbJobsCall(userToken);
        return getSyncResponse(call);
    }

    /**
     * Register a new RDB Compute Domain in RACM.
     *
     * @param rdbComputeDomainModel basic information about the new RDB Compute Domain
     * @param admins comma-separated list of groups to have admin privileges for the domain
     * @param userToken token
     * @return Newly registered RDBComputeDomainModel with fields populated by RACM
     *
     * @throws SciServerClientException in case of bad response or IO issue
     */
    public RDBComputeDomainModel registerRDBComputeDomain(RDBComputeDomainModel rdbComputeDomainModel, String admins,
            String userToken) throws SciServerClientException {
        Call<RDBComputeDomainModel> call = retrofitAdapter.registerRDBComputeDomainCall(rdbComputeDomainModel, admins,
                userToken);
        return getSyncResponse(call);
    }

    /**
     * Register a new Database Context to an existing RDB Compute Domain in RACM.
     *
     * @param domainId RACM ID for the RDB Compute Domain to which this Database Context is to be added
     * @param databaseContextModel basic information about the new Database Context
     * @param admins comma-separated list of groups to have admin privileges for the context
     * @param userToken token
     * @return Newly registered DatabaseContextModel with fields populated by RACM
     *
     * @throws SciServerClientException in case of bad response or IO issue
     */
    public DatabaseContextModel registerRDBComputeDbContext(Long domainId, DatabaseContextModel databaseContextModel,
            String admins, String userToken) throws SciServerClientException {
        Call<DatabaseContextModel> call = retrofitAdapter.registerRDBComputeDbContextCall(domainId,
                databaseContextModel, admins, userToken);
        return getSyncResponse(call);
    }

    /**
     * Return a list of the RDB compute domains known to RACM as can be seen by user with token.
     *
     * @param userToken token
     * @return List of RDBComputeDomainModel descriptions of RDB compute domains from user perspective
     *
     * @throws SciServerClientException in case of bad response or IO issue
     */
    public List<RDBComputeDomainModel> queryRDBComputeDomainsCall(String userToken) throws SciServerClientException {
        Call<List<RDBComputeDomainModel>> call = retrofitAdapter.queryRDBComputeDomainsCall(userToken);
        return getSyncResponse(call);
    }


    /**
     * Return a list of the interactive compute domains accessible to the user.
     *
     * @param userToken token
     * @return List of UserDockerComputeDomainModel descriptions of compute domains from user perspective
     *
     * @throws SciServerClientException in case of bad response or IO issue
     */
    public List<UserDockerComputeDomainModel> getUserComputeDomainsInteractive(String userToken)
        throws SciServerClientException {
        Call<List<UserDockerComputeDomainModel>> call = retrofitAdapter.getUserComputeDomainsCall(userToken, false);
        return getSyncResponse(call);
    }

    /**
     * Return a list of the batch (jobs) compute domains accessible to the user.
     *
     * @param userToken token
     * @return List of UserDockerComputeDomainModel descriptions of compute domains from user perspective
     *
     * @throws SciServerClientException in case of bad response or IO issue
     */
    public List<UserDockerComputeDomainModel> getUserComputeDomainsBatch(String userToken)
        throws SciServerClientException {
        Call<List<UserDockerComputeDomainModel>> call = retrofitAdapter.getUserComputeDomainsCall(userToken, true);
        return getSyncResponse(call);
    }

    public Collection<String> getResourceUUIDsForPubDID(String userToken, String resourceContextUUID, String pubdid)
            throws SciServerClientException {
        Call<Collection<String>> call =
                retrofitAdapter.getResourceUUIDsForPubDID(userToken, resourceContextUUID, pubdid);
        return getSyncResponse(call);
    }

    /**
     * Return a list of the fileservices known to RACM as can be seen by user with token, with minimal details.
     *
     * @param userToken token
     * @return List of MinimalFileServiceModel descriptions of the fileservices from user perspective
     *
     * @throws SciServerClientException in case of bad response or IO issue
     */
    public List<MinimalFileServiceModel> getFileServices(String userToken) throws SciServerClientException {
        Call<List<MinimalFileServiceModel>> call = retrofitAdapter.getFileServicesCall(userToken);
        return getSyncResponse(call);
    }

    /**
     * Return details about a fileservice known to RACM as can be seen by user with token. This includes api endpoint,
     * root volumes, user volumes, etc.
     *
     * @param userToken token
     * @param fileServiceIdentifier as known by racm to uniquely identify fileservice
     * @return FileServiceModel description of fileservice from user perspective
     *
     * @throws SciServerClientException in case of bad response or IO issue
     */
    public FileServiceModel getDetailsOfFileService(String userToken, String fileServiceIdentifier)
        throws SciServerClientException {
        Call<FileServiceModel> call = retrofitAdapter.getDetailsOfFileService(userToken, fileServiceIdentifier);
        return getSyncResponse(call);
    }

    /**
     * Get actions user with token is allowed to perform on the uservolume. While racm returns a generic
     * NativeQueryResult type, The important information in the result is the permission names in rows, so we treat as
     * list of these Strings in client. The server could probably be made to return a more useful type for this call in
     * the future.
     *
     * @param userToken token
     * @param fileServiceIdentifier as known by racm to uniquely identify fileservice
     * @param rootVolumeName e.g. Storage/Temporary
     * @param owner owner of user volume
     * @param userVolumeName name of user volume
     * @return A list of permissions as strings, e.g. "read", "write", "grant", etc.
     *
     * @throws SciServerClientException in case of bad response or IO issue
     */
    public VolumeActions queryUserVolumeActions(String userToken, String fileServiceIdentifier,
                                                String rootVolumeName, String owner, String userVolumeName)
        throws SciServerClientException {
        Call<JsonNode> call = retrofitAdapter.queryUserVolumeActions(
            userToken, fileServiceIdentifier, rootVolumeName, owner, userVolumeName);
        return new VolumeActions(getSyncResponse(call));
    }

    /**
     * Same as queryUserVolumeActions, but for service volumes.
     *
     * @param userToken token
     * @param serviceToken ServiceAccount.serviceToken of the ResourceContext owning the volume that is
     *        queried for actions the user can perform
     * @param fileServiceIdentifier as known by racm to uniquely identify fileservice
     * @param rootVolumeName e.g. Storage/Temporary
     * @param owner owner of service volume
     * @param serviceVolumeName name of service volume
     * @return A list of permissions as strings, e.g. "read", "write", "grant", etc.
     *
     * @throws SciServerClientException in case of bad response or IO issue
     */
    public VolumeActions queryServiceVolumeActions(
            String userToken, String serviceToken, String fileServiceIdentifier, String rootVolumeName, String owner,
            String serviceVolumeName) throws SciServerClientException {
        Call<JsonNode> call = retrofitAdapter.queryServiceVolumeActions(userToken, serviceToken, fileServiceIdentifier,
                rootVolumeName, owner, serviceVolumeName);
        return new VolumeActions(getSyncResponse(call));
    }

    /**
     * Same as queryUserVolumeActions, but for data volumes.
     *
     * @param userToken token
     * @param fileServiceIdentifier as known by racm to uniquely identify fileservice
     * @param dataVolumeName name of data volume
     * @return A list of permissions as strings, e.g. "read", "write", "grant", etc.
     *
     * @throws SciServerClientException in case of bad response or IO issue
     */
    public VolumeActions queryDataVolumeActions(String userToken, String fileServiceIdentifier, String dataVolumeName)
        throws SciServerClientException {
        Call<JsonNode> call = retrofitAdapter.queryDataVolumeActions(userToken, fileServiceIdentifier, dataVolumeName);
        return new VolumeActions(getSyncResponse(call));
    }

    /**
     * Register metadata for a new user volume in the given root volume in RACM.
     *
     * @param userToken token
     * @param fileServiceIdentifier as known by racm to uniquely identify fileservice
     * @param rootVolumeName name of root volume in which to create user volume, e.g. Storage/Temporary
     * @param userVolume new user volume object. Note: name must be unique per user per root volume
     *
     * @throws SciServerClientException in case of bad response or IO issue
     */
    public Void newUserVolume(String userToken, String fileServiceIdentifier,
                              String rootVolumeName, RegisterNewUserVolumeModel userVolume)
        throws SciServerClientException {
        Call<Void> call = retrofitAdapter.newUserVolume(userToken, fileServiceIdentifier, rootVolumeName, userVolume);
        return getSyncResponse(call);
    }

    /**
     * Register metadata for a new service volume in the given root volume in RACM.
     *
     * @param userToken token
     * @param fileServiceIdentifier as known by racm to uniquely identify fileservice
     * @param rootVolumeName name of root volume in which to create user volume, e.g. Storage/Temporary
     * @param serviceVolume new service volume object. Note: name must be unique per owner per root volume
     *
     * @throws SciServerClientException in case of bad response or IO issue
     */
    public RegisteredServiceVolumeModel newServiceVolume(String userToken, String fileServiceIdentifier,
                              String rootVolumeName, RegisterNewServiceVolumeModel serviceVolume)
        throws SciServerClientException {
        Call<RegisteredServiceVolumeModel> call = retrofitAdapter.newServiceVolume(
            userToken, fileServiceIdentifier, rootVolumeName, serviceVolume);
        return getSyncResponse(call);
    }

    /**
     * Unregister metadata for user volume in RACM.
     *
     * @param userToken token
     * @param fileServiceIdentifier as known by racm to uniquely identify fileservice
     * @param rootVolumeName name of root volume in which to create user volume, e.g. Storage/Temporary
     * @param ownerName owner of user volume
     * @param userVolumeName name of user volume
     *
     * @throws SciServerClientException in case of bad response or IO issue
     */
    public Void unregisterUserVolume(String userToken, String fileServiceIdentifier, String rootVolumeName,
                                     String ownerName, String userVolumeName)
        throws SciServerClientException {
        Call<Void> call = retrofitAdapter.unregisterUserVolume(userToken, fileServiceIdentifier, rootVolumeName,
                                                               ownerName, userVolumeName);
        return getSyncResponse(call);
    }

    /**
     * Unregister metadata for service volume in RACM.
     *
     * @param userToken token
     * @param serviceToken ServiceAccount.serviceToken of the ResourceContext owning the volume to be
     *        unregistered
     * @param fileServiceIdentifier as known by racm to uniquely identify fileservice
     * @param rootVolumeName name of root volume in which to create user volume, e.g. Storage/Temporary
     * @param ownerName owner of service volume
     * @param serviceVolumeName name of service volume
     *
     * @throws SciServerClientException in case of bad response or IO issue
     */
    public Void unregisterServiceVolume(
            String userToken, String serviceToken, String fileServiceIdentifier, String rootVolumeName,
            String ownerName, String serviceVolumeName) throws SciServerClientException {
        Call<Void> call = retrofitAdapter.unregisterServiceVolume(userToken, serviceToken, fileServiceIdentifier,
                rootVolumeName, ownerName, serviceVolumeName);
        return getSyncResponse(call);
    }

    /**
     * Update share settigns for user volume.
     *
     * @param userToken token
     * @param fileServiceIdentifier as known by racm to uniquely identify fileservice
     * @param rootVolumeName name of root volume in which to create user volume, e.g. Storage/Temporary
     * @param ownerName owner of user volume
     * @param userVolumeName name of user volume
     * @param sharedWithEntities a list of UpdateSharedWithEntry
     *
     * @throws SciServerClientException in case of bad response or IO issue
     */
    public Void shareUserVolume(String userToken, String fileServiceIdentifier, String rootVolumeName, String ownerName,
                                String userVolumeName, List<UpdateSharedWithEntry> sharedWithEntities)
        throws SciServerClientException {
        Call<Void> call = retrofitAdapter.shareUserVolume(userToken, fileServiceIdentifier, rootVolumeName, ownerName,
                                                          userVolumeName, sharedWithEntities);
        return getSyncResponse(call);

    }

    /**
     * Updating share settings for data volume.
     *
     * @param userToken token
     * @param fileServiceIdentifier as known by racm to uniquely identify fileservice
     * @param dataVolumeName name of data volume
     * @param sharedWithEntities a list of UpdateSharedWithEntry
     *
     * @throws SciServerClientException in case of bad response or IO issue
     */
    public Void shareDataVolume(String userToken, String fileServiceIdentifier, String dataVolumeName,
                                List<UpdateSharedWithEntry> sharedWithEntities)
        throws SciServerClientException {
        Call<Void> call = retrofitAdapter.shareDataVolume(userToken, fileServiceIdentifier, dataVolumeName,
                                                          sharedWithEntities);
        return getSyncResponse(call);

    }

    /**
     * Update info for user volume.
     *
     * @param userToken token
     * @param fileServiceIdentifier as known by racm to uniquely identify fileservice
     * @param rootVolumeName root volume
     * @param ownerName name of owner
     * @param userVolumeName name of user volume
     * @param volumeInfo an UpdatedUserVolumeInfo struct
     *
     * @throws SciServerClientException in case of bad response or IO issue
     */
    public Void editUserVolume(String userToken, String fileServiceIdentifier, String rootVolumeName,
                              String ownerName, String userVolumeName, UpdatedUserVolumeInfo volumeInfo)
        throws SciServerClientException {
        Call<Void> call = retrofitAdapter.editUserVolume(
            userToken, fileServiceIdentifier, rootVolumeName, ownerName, userVolumeName, volumeInfo);
        return getSyncResponse(call);
    }

}
