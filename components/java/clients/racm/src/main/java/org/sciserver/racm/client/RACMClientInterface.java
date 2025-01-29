package org.sciserver.racm.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.sciserver.clientutils.Client;
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
import org.sciserver.racm.ugm.model.PersonalUserInfo;
import org.sciserver.racm.utils.model.NativeQueryResult;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 Retrofit interface for RACM client. The client implementation should wrap these in a getSyncReponse to return the
 underlying type.
*/
public interface RACMClientInterface {
    @GET("rest/privileges")
    Call<JsonNode> getPrivilegesCall(@Query("resourceuuid") String resourceuuid,
            @Header(Client.AUTH_TOKEN_HEADER) String userToken);

    @GET("rest/resources")
    Call<JsonNode> queryUserResourcesCall(@Header(Client.AUTH_TOKEN_HEADER) String userToken);

    @POST("/resources")
    Call<ResourceGrants> postResourceGrantsCall(@Header(Client.AUTH_TOKEN_HEADER) String userToken,
            @Body ResourceGrants resourceGrants);

    @GET("rest/rc/{resourceContextUUID}/root/{action}")
    Call<Boolean> canUserDoRootActionCall(@Path("resourceContextUUID") String resourceContextUUID,
            @Path("action") String action, @Header(Client.AUTH_TOKEN_HEADER) String userToken);

    @GET("rest/rc/resource/{resourceUUID}/action/{action}")
    Call<Boolean> canUserDoResourceActionCall(@Path("resourceUUID") String resourceUUID, @Path("action") String action,
            @Header(Client.AUTH_TOKEN_HEADER) String userToken);

    @POST("rc/{resourceContextUUID}/resource")
    Call<RegisteredResourceModel> createResourceCall(@Path("resourceContextUUID") String resourceContextUUID,
            @Body NewResourceModel resource, @Header(Client.AUTH_TOKEN_HEADER) String userToken);

    @DELETE("rc/{resourceContextUUID}/resource/{resourceUUID}")
    Call<Void> deleteResourceCall(@Path("resourceContextUUID") String resourceContextUUID,
            @Path("resourceUUID") String resourceUUID, @Header(Client.AUTH_TOKEN_HEADER) String userToken);

    @GET("rc/{resourceContextUUID}/resource/{resourceUUID}")
    Call<ResourceFromUserPerspectiveModel> getResourceCall(@Path("resourceContextUUID") String resourceContextUUID,
            @Path("resourceUUID") String resourceUUID, @Header(Client.AUTH_TOKEN_HEADER) String userToken);

    @GET("rc/{resourceContextUUID}/serviceresource/{resourceUUID}")
    Call<ServiceResourceFromUserPerspectiveModel> getServiceResourceCall(
            @Path("resourceContextUUID") String resourceContextUUID, @Path("resourceUUID") String resourceUUID,
            @Header(Client.AUTH_TOKEN_HEADER) String userToken);

    @POST("rc/{resourceContextUUID}/resource/{resourceUUID}/metadata")
    Call<Boolean> updateResourceMetadataCall(@Path("resourceContextUUID") String resourceContextUUID,
            @Path("resourceUUID") String resourceUUID, @Query("name") String name,
            @Query("description") String description, @Header(Client.AUTH_TOKEN_HEADER) String userToken);

    @POST("ugm/rest/groups")
    Call<GroupInfo> manageGroupCall(@Body GroupInfo groupInfo, @Header(Client.AUTH_TOKEN_HEADER) String useToken);

    @PUT("ugm/rest/{resourceUUID}/groups")
    Call<GroupInfo> createLinkedGroupCall(@Body CreateLinkedGroupModel linkedGroup,
            @Header(Client.AUTH_TOKEN_HEADER) String userToken, @Path("resourceUUID") String resourceUUID);

    @POST("rc/{resourceContextUUID}/resource/{resourceUUID}/privileges")
    Call<Void> addNewPrivilegeCall(@Path("resourceContextUUID") String resourceContextUUID,
            @Path("resourceUUID") String resourceUUID, @Body List<PrivilegeModel> newPrivileges,
            @Header(Client.AUTH_TOKEN_HEADER) String userToken);

    @POST("rc/{resourceContextUUID}/resource/{resourceUUID}/associatedSciserverEntity")
    Call<Void> associateWithSciserverEntityCall(@Path("resourceContextUUID") String resourceContextUUID,
            @Path("resourceUUID") String resourceUUID, @Body AssociatedSciserverEntityModel associatedSciserverEntity,
            @Header(Client.AUTH_TOKEN_HEADER) String userToken);

    @GET("ugm/rest/groups")
    Call<JsonNode> getGroupsUserIsMemberOfCall(
        @Header(Client.AUTH_TOKEN_HEADER) String useToken);

    @GET("ugm/rest/mygroups")
    Call<List<GroupInfo>> getUserGroupsCall(
        @Header(Client.AUTH_TOKEN_HEADER) String userToken);

    @GET("ugm/rest/mygroups/{groupid}")
    Call<GroupInfo> queryMyGroupCall(@Path("groupid") Long groupid,
            @Header(Client.AUTH_TOKEN_HEADER) String userToken,
            @Header(Client.SERVICE_TOKEN_HEADER) String serviceToken);

    @GET("ugm/rest/resourcegroup/{groupid}")
    Call<GroupInfo> queryResourceOwnedGroupCall(@Path("groupid") Long groupid,
            @Header(Client.AUTH_TOKEN_HEADER) String userToken,
            @Header(Client.SERVICE_TOKEN_HEADER) String serviceToken);

    @GET("rc/{resourceContextUUID}/resources")
    Call<Set<ResourceFromUserPerspectiveModel>> queryUserRCResources(@Header(Client.AUTH_TOKEN_HEADER) String userToken,
            @Path("resourceContextUUID") String resourceContextUUID);

    @GET("rest/rc/{resourceContextUUID}/resources")
    Call<List<UserResourceModel>> queryUserRCResourcesAsTableCall(@Header(Client.AUTH_TOKEN_HEADER) String userToken,
            @Path("resourceContextUUID") String resourceContextUUID);

    @GET("ugm/rest/user")
    Call<PersonalUserInfo> getUserCall(@Header(Client.AUTH_TOKEN_HEADER) String userToken);

    @GET("jobm/rest/compm/checkId")
    Call<Void> checkServiceIdCall(@Header(Client.SERVICE_TOKEN_HEADER) String serviceToken);

    @GET("jobm/rest/jobs/{jobId}")
    Call<COMPMJobModel> jobStatusCall(@Header(Client.AUTH_TOKEN_HEADER) String userToken, @Path("jobId") Long jobId);

    @GET("jobm/rest/jobs/{jobId}")
    Call<RDBJobModel> rdbJobStatusCall(@Header(Client.AUTH_TOKEN_HEADER) String userToken, @Path("jobId") Long jobId);

    @GET("jobm/rest/rdbjobs")
    Call<List<RDBJobModel>> queryUserRdbJobsCall(@Header(Client.AUTH_TOKEN_HEADER) String userToken);

    @GET("jobm/rest/computedomains/rdb")
    Call<List<RDBComputeDomainModel>> queryRDBComputeDomainsCall(@Header(Client.AUTH_TOKEN_HEADER) String userToken);

    @POST("jobm/rest/computedomains/rdb")
    Call<RDBComputeDomainModel> registerRDBComputeDomainCall(@Body RDBComputeDomainModel rdbComputeDomainModel,
            @Query("admins") String admins, @Header(Client.AUTH_TOKEN_HEADER) String userToken);

    @POST("jobm/rest/computedomains/rdb/{domainId}")
    Call<DatabaseContextModel> registerRDBComputeDbContextCall(@Path("domainId") Long domainId,
            @Body DatabaseContextModel databaseContextModel, @Query("admins") String admins,
            @Header(Client.AUTH_TOKEN_HEADER) String userToken);

    @GET("jobm/rest/computedomains")
    Call<List<UserDockerComputeDomainModel>> getUserComputeDomainsCall(
        @Header(Client.AUTH_TOKEN_HEADER) String userToken,
        @Query("batch") Boolean batch);

    @GET("rc/{resourceContextUUID}/pubdid")
    Call<Collection<String>> getResourceUUIDsForPubDID(
            @Header(Client.AUTH_TOKEN_HEADER) String userToken,
            @Path("resourceContextUUID") String resourceContextUUID,
            @Query("pubdid") String pubdid);

    @GET("storem/fileservices")
    Call<List<MinimalFileServiceModel>> getFileServicesCall(@Header(Client.AUTH_TOKEN_HEADER) String userToken);

    @GET("storem/fileservice/{fileServiceIdentifer}")
    Call<FileServiceModel> getDetailsOfFileService(
        @Header(Client.AUTH_TOKEN_HEADER) String userToken,
        @Path("fileServiceIdentifer") String fileServiceIdentifier);

    @GET("storem/fileservice/{fileServiceIdentifer}/rootVolume/{rootVolumeName}/userVolume/{owner}/{userVolumeName}/"
         + "allowedActions")
    Call<JsonNode> queryUserVolumeActions(
        @Header(Client.AUTH_TOKEN_HEADER) String userToken,
        @Path("fileServiceIdentifer") String fileServiceIdentifier,
        @Path("rootVolumeName") String rootVolumeName,
        @Path("owner") String owner,
        @Path("userVolumeName") String userVolumeName);

    @GET("storem/fileservice/{fileServiceIdentifer}/rootVolume/{rootVolumeName}/serviceVolume/{owner}/"
            + "{serviceVolumeName}/allowedActions")
    Call<JsonNode> queryServiceVolumeActions(
            @Header(Client.AUTH_TOKEN_HEADER) String userToken,
            @Header(RegisterNewServiceVolumeModel.SERVICE_TOKEN_HEADER) String serviceToken,
            @Path("fileServiceIdentifer") String fileServiceIdentifier,
            @Path("rootVolumeName") String rootVolumeName,
            @Path("owner") String owner,
            @Path("serviceVolumeName") String serviceVolumeName);

    @GET("storem/fileservice/{fileServiceIdentifer}/dataVolume/{dataVolumeName}/allowedActions")
    Call<JsonNode> queryDataVolumeActions(
        @Header(Client.AUTH_TOKEN_HEADER) String userToken,
        @Path("fileServiceIdentifer") String fileServiceIdentifier,
        @Path("dataVolumeName") String dataVolumeName);

    @POST("storem/fileservice/{fileServiceIdentifer}/rootVolume/{rootVolumeName}/userVolumes")
    Call<Void> newUserVolume(
        @Header(Client.AUTH_TOKEN_HEADER) String userToken,
        @Path("fileServiceIdentifer") String fileServiceIdentifier,
        @Path("rootVolumeName") String rootVolumeName,
        @Body RegisterNewUserVolumeModel userVolume);

    @POST("storem/fileservice/{fileServiceIdentifer}/rootVolume/{rootVolumeName}/serviceVolumes")
    Call<RegisteredServiceVolumeModel> newServiceVolume(
        @Header(Client.AUTH_TOKEN_HEADER) String userToken,
        @Path("fileServiceIdentifer") String fileServiceIdentifier,
        @Path("rootVolumeName") String rootVolumeName,
        @Body RegisterNewServiceVolumeModel serviceVolume);

    @DELETE("storem/fileservice/{fileServiceIdentifier}/rootVolume/{rootVolumeName}/userVolume/{ownerName}/"
            + "{userVolumeName}")
    Call<Void> unregisterUserVolume(
        @Header(Client.AUTH_TOKEN_HEADER) String userToken,
        @Path("fileServiceIdentifier") String fileServiceIdentifier,
        @Path("rootVolumeName") String rootVolumeName,
        @Path("ownerName") String ownerName,
        @Path("userVolumeName") String userVolumeName);

    @DELETE("storem/fileservice/{fileServiceIdentifier}/rootVolume/{rootVolumeName}/serviceVolume/{owner}/"
            + "{serviceVolumeName}")
    Call<Void> unregisterServiceVolume(
        @Header(Client.AUTH_TOKEN_HEADER) String userToken,
        @Header(RegisterNewServiceVolumeModel.SERVICE_TOKEN_HEADER) String serviceToken,
        @Path("fileServiceIdentifier") String fileServiceIdentifier,
        @Path("rootVolumeName") String rootVolumeName,
        @Path("owner") String owner,
        @Path("serviceVolumeName") String serviceVolumeName);

    @PATCH("storem/fileservice/{fileServiceIdentifier}/rootVolume/{rootVolumeName}/userVolume/{ownerName}/"
           + "{userVolumeName}/sharedWith")
    Call<Void> shareUserVolume(
        @Header(Client.AUTH_TOKEN_HEADER) String userToken,
        @Path("fileServiceIdentifier") String fileServiceIdentifier,
        @Path("rootVolumeName") String rootVolumeName,
        @Path("ownerName") String ownerName,
        @Path("userVolumeName") String userVolumeName,
        @Body List<UpdateSharedWithEntry> sharedWithEntities);

    @PATCH("storem/fileservice/{fileServiceIdentifier}/dataVolume/{dataVolumeName}/sharedWith")
    Call<Void> shareDataVolume(
        @Header(Client.AUTH_TOKEN_HEADER) String userToken,
        @Path("fileServiceIdentifier") String fileServiceIdentifier,
        @Path("dataVolumeName") String dataVolumeName,
        @Body List<UpdateSharedWithEntry> sharedWithEntities);

    @PATCH("storem/fileservice/{fileServiceIdentifier}/rootVolume/{rootVolumeName}/userVolume/{ownerName}/"
           + "{userVolumeName}")
    Call<Void> editUserVolume(
        @Header(Client.AUTH_TOKEN_HEADER) String userToken,
        @Path("fileServiceIdentifier") String fileserviceIdentifier,
        @Path("rootVolumeName") String rootVolumeName,
        @Path("ownerName") String ownerName,
        @Path("userVolumeName") String userVolumeName,
        @Body UpdatedUserVolumeInfo volumeInfo);

}
