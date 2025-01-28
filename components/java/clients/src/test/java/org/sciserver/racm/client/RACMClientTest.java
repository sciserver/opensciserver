package org.sciserver.racm.client;


import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.sciserver.authentication.client.UnauthenticatedException;
import org.sciserver.clientutils.SciServerClientException;
import org.sciserver.racm.jobm.model.UserDockerComputeDomainModel;
import org.sciserver.racm.rctree.model.ResourceGrants;
import org.sciserver.racm.resourcecontext.model.NewResourceModel;
import org.sciserver.racm.resourcecontext.model.RegisteredResourceModel;
import org.sciserver.racm.resources.model.NewSharedWithEntity;
import org.sciserver.racm.resources.model.SciServerEntityType;
import org.sciserver.racm.storem.model.UpdateSharedWithEntry;
import org.sciserver.racm.storem.model.UpdatedUserVolumeInfo;
import org.sciserver.racm.ugm.model.GroupInfo;
import org.sciserver.racm.storem.model.FileServiceModel;
import org.sciserver.racm.storem.model.RegisterNewServiceVolumeModel;
import org.sciserver.racm.storem.model.RegisterNewUserVolumeModel;
import org.sciserver.racm.utils.model.NativeQueryResult;
import org.testng.annotations.*;

import static org.testng.Assert.*;

public class RACMClientTest {

    private RACMClient racmClient;
    private RACMClient racmClientNoToken;
    private MockWebServer mockServer;
    private boolean clearRequest = true;

    private void setupResponseFromFile(String filename) throws IOException {
        String data = new String(Files.readAllBytes(Paths.get(filename)));
        setupResponseFromString(data);
    }

    private void setupResponseFromFile(String filename, int code) throws IOException {
        String data = new String(Files.readAllBytes(Paths.get(filename)));
        setupResponseFromString(data, code);
    }

    private void setupResponseFromString(String string) {
        mockServer.enqueue(new MockResponse()
                           .setResponseCode(200)
                           .setBody(string));
    }

    private void setupResponseFromString(String string, int code) {
        mockServer.enqueue(new MockResponse()
                           .setResponseCode(code)
                           .setBody(string));
    }

    @BeforeClass
    public void setUp() {
        mockServer = new MockWebServer();
        racmClient = new RACMClient(mockServer.url("/").toString(), "fake-service-token");
        racmClientNoToken = new RACMClient(mockServer.url("/").toString());
    }

    @AfterMethod
    public void popRequest() throws InterruptedException, IOException {
        if (clearRequest) {
            mockServer.takeRequest();
        }
        clearRequest = true;
    }

    @Test
    public void callWithParseErrorThrows502() {
        setupResponseFromString("{}");
        try {
            racmClient.queryUserResources("fake-token");
        } catch (SciServerClientException e) {
            assertEquals(e.httpCode(), 502);
        }
    }

    @Test
    public void callWithNon200ThrowsSameCode() throws IOException {
        setupResponseFromFile("src/test/data/queryUserResourcesResponse.json", 401);
        try {
            racmClient.queryUserResources("fake-token");
        } catch (SciServerClientException e) {
            assertEquals(e.httpCode(), 401);
        }
    }

    @Test
    public void callIncludesServiceTokenHeader() throws InterruptedException {
        setupResponseFromString("{}");
        try {
            racmClient.queryUserResources("fake-token");
        } catch (SciServerClientException e) {
            // don't care, just need to make sure header is included in request
        }
        RecordedRequest req = mockServer.takeRequest();
        assertEquals(req.getHeader("X-Service-Auth-ID"), "fake-service-token");
        clearRequest = false;
    }

    @Test(expectedExceptions = SciServerClientException.class)
    public void callWithIncompatibleTypesExceptsTest() throws SciServerClientException, IOException {
        // the ResourceGrants type is incompatible with RegisteredResourceModel
        setupResponseFromFile("src/test/data/postResourceGrantsResponse.json");
        NewResourceModel model = new NewResourceModel("a", "b", "c", "d");
        racmClient.createResource("fake-uuid", model, "fake-token");
    }

    @Test
    public void queryUserResourcesCallTest() throws SciServerClientException, IOException {
        setupResponseFromFile("src/test/data/queryUserResourcesResponse.json");
        NativeQueryResult res = null;
        res = racmClient.queryUserResources("fake-token");
        String[] expected = {"a", "b"};
        assertEquals(res.getColumns(), expected);
    }

    @Test
    public void postResourceGrantsTest() throws SciServerClientException, IOException {
        setupResponseFromFile("src/test/data/postResourceGrantsResponse.json");
        ResourceGrants res = null;
        ResourceGrants grants = new ResourceGrants();
        res = racmClient.postResourceGrants("fake-token", grants);
        assertEquals(res.getResourceId(), "fakeid");
    }

    @Test
    public void canUserDoRootActionTest() throws SciServerClientException {
        setupResponseFromString("true");
        Boolean res = false;
        res = racmClient.canUserDoRootAction("resourceContextUUID", "action", "fake-token");
        assertTrue(res);
    }

    @Test
    public void createCourseTest() throws SciServerClientException, IOException {
        setupResponseFromFile("src/test/data/registeredResourceModel.json");
        RegisteredResourceModel res = null;
        NewResourceModel model = new NewResourceModel("a", "b", "c", "d");
        res = racmClient.createResource("fake-uuid", model, "fake-token");
        assertEquals(res.getName(), "fake-registeredResourceModel");
    }

    @Test
    public void deleteResourceTest() throws SciServerClientException, InterruptedException {
        setupResponseFromString("{}");
        racmClient.deleteResource("resourceContextUUID", "resourceUUID", "userToken");
    }

    @Test
    public void noServiceTokenInstantiationTest() throws InterruptedException {
        setupResponseFromString("{}");
        try {
            racmClientNoToken.canUserDoRootAction("a", "b", "f-token");
        } catch (SciServerClientException e) {
            // dont care, just need to ensure no service token present
        }
        RecordedRequest req = mockServer.takeRequest();
        assertNull(req.getHeader("X-RACM-Service-Token"));
        clearRequest = false;
    }

    @Test
    public void getDetailsOfFileserviceTest() throws IOException, SciServerClientException {
        setupResponseFromFile("src/test/data/getFileServiceResponse.json");
        FileServiceModel res = racmClient.getDetailsOfFileService("fake-token", "fake-fs-identifier");
        assertEquals(res.getName(), "FileService");
        assertEquals(res.getIdentifier(), "2beab3b2-26f0-4eaf-ad6b-3b37279baee9");
    }

    @Test
    public void queryUserVolumeActionsTest() throws Exception {
        setupResponseFromFile("src/test/data/queryUserVolumeActions.json");
        VolumeActions res = racmClient.queryUserVolumeActions(
            "fake-token", "fake-fs-identifier", "rootVol", "owner", "uVName");
        assertTrue(res.exists());
        res.requireActions("delete", "grant", "read", "write");
    }

    @Test
    public void queryServiceVolumeActionsTest() throws Exception {
        setupResponseFromFile("src/test/data/queryServiceVolumeActions.json");
        VolumeActions res = racmClient.queryServiceVolumeActions(
            "fake-token", "fake-service-token", "fake-fs-identifier", "rootVol", "owner", "sVName");
        res.requireActions(Arrays.asList("delete", "grant", "read", "write"));
    }

    @Test
    public void queryDataVolumeActionsTest() throws IOException, SciServerClientException {
        setupResponseFromFile("src/test/data/queryDataVolumeActions.json");
        VolumeActions res = racmClient.queryDataVolumeActions(
            "fake-token", "fake-fs-identifier", "dataVol");
        assertTrue(res.hasActions("read"));
        assertFalse(res.hasActions("grant"));
    }

    @Test(expectedExceptions = Exception.class)
    public void volumeActionsRequiredThrowsTest() throws Exception {
        setupResponseFromFile("src/test/data/queryDataVolumeActions.json");
        VolumeActions res = racmClient.queryDataVolumeActions(
            "fake-token", "fake-fs-identifier", "dataVol");
        res.requireActions("read", "grant");
    }

    @Test
    public void nonExistentVolumeTest() throws Exception {
        setupResponseFromFile("src/test/data/queryEmptyVolumeActions.json");
        VolumeActions res = racmClient.queryDataVolumeActions(
            "fake-token", "fake-fs-identifier", "dataVol");
        assertFalse(res.exists());
    }

    @Test void newUserVolumeTest() throws SciServerClientException, InterruptedException {
        setupResponseFromString("");
        RegisterNewUserVolumeModel newUv = new RegisterNewUserVolumeModel("name", "desc", "relPath", Optional.of("owner12345678"));
        racmClient.newUserVolume("fake-token", "fake-fs-identifier", "rootVol", newUv);
        RecordedRequest req = mockServer.takeRequest();
        // we need to ensure that the Optional is given underlying value, not jsonified Optional object
        assertTrue(req.getUtf8Body().contains("owner12345678"));
        clearRequest = false;
    }

    @Test void newServiceVolumeTest() throws IOException, SciServerClientException {
        setupResponseFromFile("src/test/data/newServiceVolume.json");
        List<NewSharedWithEntity> newSharedEntityList = Arrays.asList(
            new NewSharedWithEntity("name", SciServerEntityType.USER, Arrays.asList("read", "write")));
        RegisterNewServiceVolumeModel newSv = new RegisterNewServiceVolumeModel(
            "name", "desc", "relPath", newSharedEntityList, "service-token", "ownerUUID", "usage");
        racmClient.newServiceVolume("fake-token", "fake-fs-identifier", "rootVol", newSv);
    }

    @Test void unregisterUserVolumeTest() throws SciServerClientException {
        setupResponseFromString("");
        racmClient.unregisterUserVolume("fake-token", "fake-fs-identifier", "rootVol", "owner", "uVName");
    }

    @Test void updateUserVolumeShareTest() throws SciServerClientException {
        setupResponseFromString("");
        List<UpdateSharedWithEntry> shareList = new ArrayList<UpdateSharedWithEntry>();
        shareList.add(new UpdateSharedWithEntry(Optional.of(Long.valueOf(1)), Optional.of("name"),
                                                SciServerEntityType.USER, Arrays.asList("read", "write")));
        racmClient.shareUserVolume("fake-token", "fake-fs-identifier", "rootVol", "owner", "uVName",
                                   shareList);
    }

    @Test void updateDataVolumeShareTest() throws SciServerClientException {
        setupResponseFromString("");
        List<UpdateSharedWithEntry> shareList = new ArrayList<UpdateSharedWithEntry>();
        shareList.add(new UpdateSharedWithEntry(Optional.of(Long.valueOf(1)), Optional.of("name"),
                                                SciServerEntityType.USER, Arrays.asList("read", "write")));
        racmClient.shareDataVolume("fake-token", "fake-fs-identifier", "dataVol",
                                   shareList);
    }

    @Test
    void editUserVolumeTest() throws SciServerClientException {
        setupResponseFromString("");
        racmClient.editUserVolume("fake-token", "fake-fs-identifier", "rootVolumeName", "ownerName", "userVolumeName",
                                  new UpdatedUserVolumeInfo(Optional.of("name"), Optional.of("description")));
    }

    @Test
    void testQueryComputeDomain() throws SciServerClientException, IOException  {
        setupResponseFromFile("src/test/data/userdockercomputedomain.json");
        List<UserDockerComputeDomainModel> d = racmClient.getUserComputeDomainsInteractive("fake-token");
        assertEquals(d.size(), 2);
        assertEquals(d.get(0).getName(), "testComputeDomain1");
        assertEquals(d.get(1).getName(), "testComputeDomain2");
        assertEquals(d.get(1).getVolumes().size(), 2);
    }

    @Test
    void testQueryUserGroups() throws SciServerClientException, IOException {
        setupResponseFromFile("src/test/data/ownedoradmingrouplist.json");
        List<GroupInfo> g = racmClient.getUserGroups("fake-token");
        assertEquals(g.size(), 2);
        assertTrue(g.stream().anyMatch(x -> x.getGroupName().equals("admin")));
    }

    @Test
    void testCheckServiceIDAuthorized() throws SciServerClientException, UnauthenticatedException {
        setupResponseFromString("");
        racmClient.checkServiceToken("junk-token");
    }

    @Test(expectedExceptions = UnauthenticatedException.class)
    void testCheckServiceIDUnauthorizedRaises() throws SciServerClientException, UnauthenticatedException {
        setupResponseFromString("", 403);
        racmClient.checkServiceToken("junk-token");
    }

    @Test(expectedExceptions = SciServerClientException.class)
    void testCheckServiceIDOtherErrorRaises() throws SciServerClientException, UnauthenticatedException {
        setupResponseFromString("", 500);
        racmClient.checkServiceToken("junk-token");
    }

    @Test
    void testGetGroupsUserIsMemberOfReturnsOnlyMemberGroups() throws Exception {
        setupResponseFromFile("src/test/data/allusergroupslist.json");
        List<String> groups = racmClient.getGroupsUserIsMemberOf("fake-token");
        List<String> expectedGroups = Arrays.asList("testOwnerStatus", "public", "admin");
        assert(groups.containsAll(expectedGroups));
        assert(!groups.contains("testDeclinedStatus"));
        assert(!groups.contains("testInvitedStatus"));
    }
}
