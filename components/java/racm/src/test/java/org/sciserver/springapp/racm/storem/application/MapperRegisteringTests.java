package org.sciserver.springapp.racm.storem.application;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;

import org.ivoa.dm.VOURPException;
import org.ivoa.dm.model.TransientObjectManager;
import org.junit.Before;
import org.junit.Test;
import org.sciserver.racm.storem.model.RegisterNewFileServiceModel;
import org.sciserver.racm.storem.model.RegisterNewRootVolumeModel;
import org.sciserver.racm.storem.model.RegisterNewUserVolumeModel;
import org.sciserver.racm.storem.model.RegisterNewUserVolumeWithOwnerModel;
import org.sciserver.springapp.racm.storem.application.STOREMConstants;
import org.sciserver.springapp.racm.storem.application.StoremMapper;
import org.sciserver.springapp.racm.utils.RACMUtil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import edu.jhu.file.FileService;
import edu.jhu.file.RootVolume;
import edu.jhu.file.UserVolume;
import edu.jhu.rac.ContextClass;
import edu.jhu.rac.ResourceContext;
import edu.jhu.rac.ResourceType;
import edu.jhu.user.User;

public class MapperRegisteringTests {
	private StoremMapper mapper;
	private TransientObjectManager tom;
	private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Method to ensure this class could in principle be run.<br/>
     */
    @Test
    public void noop() {}

//	@Test
	public void createFileServiceWithNoVolumes() throws IOException {
		URL filename = getClass().getClassLoader().getResource("storem_json/register_fileservice_no_volumes.json");
		FileService fs = mapper.createFileService(
				objectMapper.readValue(filename, RegisterNewFileServiceModel.class),
				tom);
		assertThat(fs.getName(), is("My New File Service"));
		assertThat(fs.getDescription(), is("A service for reaching files"));
		assertThat(fs.getApiEndpoint(), is("https://my.fileservice.url.example.com/"));
		assertThat(fs.getResourceContext(), is(notNullValue()));
		assertThat(fs.getResourceContext().getRacmEndpoint(), is("https://my.fileservice.url.example.com/"));
		try {
			UUID.fromString(fs.getServiceToken());
		} catch (IllegalArgumentException e) {
			fail("Expected valid UUID service token, got " + fs.getServiceToken());
		}

		assertTrue(fs.isValid());
		assertTrue(fs.getResourceContext().isValid());
	}

//	@Test
	public void createFileServiceWithRootVolumes() throws IOException {
		URL filename = getClass().getClassLoader().getResource("storem_json/register_fileservice_with_rootvolumes.json");
		FileService fs = mapper.createFileService(
				objectMapper.readValue(filename, RegisterNewFileServiceModel.class),
				tom);
		assertThat(fs.getName(), is("My New File Service"));
		assertThat(fs.getResourceContext(), is(notNullValue()));
		assertThat(fs.getResourceContext().getRacmEndpoint(), is("https://my.fileservice.url.example.com/"));
		assertThat(fs.getResourceContext().getResource().size(), is(2));

		assertThat(fs.getRootVolume().size(), is(2));
		assertThat(fs.getRootVolume().get(0).getPathOnFileSystem(),
				is("/A Path/On a *nix/FileSystem/can _.have/anything;'\"/even \uD83D\uDE06"));
		assertThat(fs.getRootVolume().get(0).getResource().getPublisherDID(), is("Crazy Volume"));
		assertTrue(fs.getRootVolume().get(0).getContainsSharedVolumes());
		assertThat(fs.getRootVolume().get(1).getName(), is("This should work fine"));
		assertThat(fs.getRootVolume().get(1).getDescription(), is("Better three hours too soon than a minute too late.."));
		assertThat(fs.getRootVolume().get(1).getPathOnFileSystem(), is("/mnt/A path with spaces"));
		assertFalse(fs.getRootVolume().get(1).getContainsSharedVolumes());

		assertTrue(fs.isValid());
		assertTrue(fs.getResourceContext().isValid());
		for(RootVolume rv : fs.getRootVolume()) {
			assertTrue(rv.isValid());
		}
	}

//	@Test
	public void createFileService() throws IOException {
		URL filename = getClass().getClassLoader().getResource("storem_json/register_fileservice.json");
		FileService fs = mapper.createFileService(
				objectMapper.readValue(filename, RegisterNewFileServiceModel.class),
				tom);
		assertThat(fs.getName(), is("My New File Service"));
		assertThat(fs.getResourceContext(), is(notNullValue()));
		assertThat(fs.getResourceContext().getRacmEndpoint(), is("https://my.fileservice.url.example.com/"));

		assertThat(fs.getRootVolume().size(), is(2));
		assertThat(fs.getRootVolume().get(0).getPathOnFileSystem(),
				is("/A Path/On a *nix/FileSystem/can _.have/anything;'\"/even \uD83D\uDE06"));
		assertFalse(fs.getRootVolume().get(0).getContainsSharedVolumes());
		assertThat(fs.getRootVolume().get(1).getName(), is("This should work fine"));
		assertThat(fs.getRootVolume().get(1).getDescription(), is("Better three hours too soon than a minute too late.."));
		assertTrue(fs.getRootVolume().get(1).getContainsSharedVolumes());


		assertThat(fs.getResourceContext().getResource().size(), is(2));

		assertTrue(fs.isValid());
		assertTrue(fs.getResourceContext().isValid());
		for(RootVolume rv : fs.getRootVolume()) {
			assertTrue(rv.validationErrors(), rv.isValid());
		}
	}

//	@Test
	public void createRootVolume() throws IOException {
		URL filename = getClass().getClassLoader().getResource("storem_json/register_rootvolume.json");
		FileService fileService = mock(FileService.class);
		ResourceContext resourceContext = mock(ResourceContext.class);
		when(fileService.getTom()).thenReturn(tom);
		when(fileService.getResourceContext()).thenReturn(resourceContext);

		RootVolume rv = mapper.createRootVolume(
				objectMapper.readValue(filename, RegisterNewRootVolumeModel.class),
				fileService);

		assertThat(rv.getName(), is("Crazy Volume"));
		assertThat(rv.getDescription(), is("A root volume that is going to be a good test of well-written scripts if they use absolute paths"));
		assertThat(rv.getPathOnFileSystem(), is("/A Path/On a *nix/FileSystem/can _.have/anything;'\"/even \uD83D\uDE06"));
		assertThat(rv.getResource().getPublisherDID(), is("Crazy Volume"));
		assertTrue(rv.getContainsSharedVolumes());

		assertTrue(rv.isValid());
	}

//	@Test
	public void createUserVolume() throws IOException {
		URL filename = getClass().getClassLoader().getResource("storem_json/register_uservolume.json");
		FileService fileService = mock(FileService.class);
		ResourceContext resourceContext = mock(ResourceContext.class);
		when(fileService.getTom()).thenReturn(tom);
		when(fileService.getResourceContext()).thenReturn(resourceContext);
		RootVolume rv = new RootVolume(fileService);
		rv.setName("scratch");
		User owner = mock(User.class);

		RegisterNewUserVolumeModel newUserVolume = objectMapper.readValue(filename, RegisterNewUserVolumeModel.class);
		RegisterNewUserVolumeWithOwnerModel newUserVolumeWithOwner = new RegisterNewUserVolumeWithOwnerModel(
				newUserVolume.getName(),
				newUserVolume.getDescription(),
				newUserVolume.getRelativePath(),
				owner);

		UserVolume uv = mapper.createUserVolume(newUserVolumeWithOwner, rv, fileService);
		assertThat(uv.getOwner(), is(owner));
		assertThat(uv.getName(), is("My volume"));
		assertThat(uv.getDescription(), is("A source for my files"));
		assertThat(uv.getRelativePath(), is("/path/to/userfolder"));
		assertThat(uv.getRootVolume(), is(rv));

		assertThat(uv.getResource(), is(not(nullValue())));
		assertThat(uv.getResource().getPublisherDID(), is("scratch/My volume"));
		verify(resourceContext).addResource(uv.getResource());

		assertTrue(uv.isValid());
		assertTrue(uv.getResource().isValid());
	}

//	@Test
	public void createUserVolumeWithExplicitOwner() throws IOException {
		URL filename = getClass().getClassLoader().getResource("storem_json/register_uservolume_with_owner.json");
		FileService fileService = mock(FileService.class);
		ResourceContext resourceContext = mock(ResourceContext.class);
		when(fileService.getTom()).thenReturn(tom);
		when(fileService.getResourceContext()).thenReturn(resourceContext);
		RootVolume rv = new RootVolume(fileService);
		rv.setName("scratch");
		User owner = mock(User.class);

		RegisterNewUserVolumeModel newUserVolume = objectMapper.readValue(filename, RegisterNewUserVolumeModel.class);
		when(owner.getUsername()).thenReturn("sciserverUser1");
		RegisterNewUserVolumeWithOwnerModel newUserVolumeWithOwner = new RegisterNewUserVolumeWithOwnerModel(
				newUserVolume.getName(),
				newUserVolume.getDescription(),
				newUserVolume.getRelativePath(),
				owner);

		UserVolume uv = mapper.createUserVolume(newUserVolumeWithOwner, rv, fileService);
		assertThat(uv.getName(), is("Name of My Volume"));
		assertThat(uv.getDescription(), is("description"));
		assertThat(uv.getOwner(), is(owner));
		assertThat(uv.getRelativePath(), is("/path/to/userfolder"));
		assertThat(uv.getRootVolume(), is(rv));

		assertThat(uv.getResource(), is(not(nullValue())));
		assertThat(uv.getResource().getPublisherDID(), is("scratch/sciserverUser1/Name of My Volume"));
		verify(resourceContext).addResource(uv.getResource());

		assertTrue(uv.isValid());
		assertTrue(uv.getResource().isValid());
	}

//	@Before
	public void setupMockTom() throws VOURPException {
		tom = mock(TransientObjectManager.class);

		RACMUtil racmUtil = mock(RACMUtil.class);
		doCallRealMethod()
			.when(racmUtil)
			.newResource(any(ResourceContext.class));
		doReturn(new ContextClass(tom))
			.when(racmUtil)
			.queryContextClass(STOREMConstants.CC_FILESERVICE_NAME, tom);
		doAnswer(x -> new ResourceType(new ContextClass(tom)))
			.when(racmUtil)
			.queryResourceType(any(String.class), any(String.class), any(TransientObjectManager.class));

		mapper = new StoremMapper(racmUtil);
	}

//	@Before
	public void setupObjectMapper() {
		objectMapper.registerModule(new Jdk8Module());
	}
}
