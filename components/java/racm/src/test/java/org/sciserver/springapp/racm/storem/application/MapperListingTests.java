package org.sciserver.springapp.racm.storem.application;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sciserver.springapp.racm.storem.application.STOREMConstants.A_FILESERVICE_ROOTVOLUME_CREATE;
import static org.sciserver.springapp.racm.storem.application.STOREMConstants.A_FILESERVICE_USERVOLUME_DELETE;
import static org.sciserver.springapp.racm.storem.application.STOREMConstants.A_FILESERVICE_USERVOLUME_GRANT;
import static org.sciserver.springapp.racm.storem.application.STOREMConstants.A_FILESERVICE_USERVOLUME_READ;
import static org.sciserver.springapp.racm.storem.application.STOREMConstants.A_FILESERVICE_USERVOLUME_WRITE;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.sciserver.racm.storem.model.FileServiceModel;
import org.sciserver.racm.storem.model.RegisteredFileServiceModel;
import org.sciserver.racm.storem.model.RootVolumeModel;
import org.sciserver.racm.storem.model.UserVolumeModel;
import org.sciserver.springapp.racm.storem.application.StoremMapper;
import org.sciserver.springapp.racm.utils.RACMUtil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import edu.jhu.file.FileService;
import edu.jhu.file.RootVolume;
import edu.jhu.file.UserVolume;
import edu.jhu.rac.Resource;
import edu.jhu.rac.ResourceContext;
import edu.jhu.user.User;

public class MapperListingTests {
	private RACMUtil racmUtil = mock(RACMUtil.class);
	private StoremMapper mapper = new StoremMapper(racmUtil);
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	public void listUserVolumeNoActions() {
		UserVolume userVolume = mock(UserVolume.class);

		User owner = mock(User.class);
		when(owner.getId()).thenReturn(42L);
		when(userVolume.getOwner()).thenReturn(owner);
		when(userVolume.getRelativePath()).thenReturn("my relative/path/to/a user's /folder");
		when(userVolume.getResource()).thenReturn(mock(Resource.class));

		assertFalse(mapper.getListingDTO(userVolume, Collections.emptyMap(), Collections.emptyMap()).isPresent());
	}

	@Test
	public void listUserVolume() throws IOException {
		UserVolume userVolume = mock(UserVolume.class);

		User owner = mock(User.class);
		when(owner.getUsername()).thenReturn("sciserverUser1");
		when(userVolume.getId()).thenReturn(2L);
		when(userVolume.getName()).thenReturn("Name of volume");
		when(userVolume.getDescription()).thenReturn("Description of volume");
		when(userVolume.getOwner()).thenReturn(owner);
		when(userVolume.getRelativePath()).thenReturn("my relative/path/to/a user's /folder");
		when(userVolume.getResource()).thenReturn(mock(Resource.class));
		when(userVolume.getResource().getId()).thenReturn(1L);
        when(userVolume.getResource().getUuid()).thenReturn("abcde-fghij");

		Map<Long, List<String>> actions = new HashMap<>();
		actions.put(1L, Arrays.asList(
				A_FILESERVICE_USERVOLUME_WRITE,
				A_FILESERVICE_USERVOLUME_DELETE));

		UserVolumeModel userVolumeDTO = mapper.getListingDTO(userVolume, actions, Collections.emptyMap()).get();

		URL filename = getClass().getClassLoader().getResource("storem_json/list_uservolume.json");
		assertThat(userVolumeDTO,
				is(equalTo(objectMapper.readValue(filename, UserVolumeModel.class))));
	}

	@Test
	public void listRootVolume() throws IOException {
		RootVolume rootVolume = mock(RootVolume.class);
		UserVolume userVolume = mock(UserVolume.class);
		User volumeOwner = mock(User.class);

		when(volumeOwner.getUsername()).thenReturn("sciserverUser1");
		when(userVolume.getId()).thenReturn(5L);
		when(userVolume.getName()).thenReturn("My volume name");
		when(userVolume.getDescription()).thenReturn("My volume description");
		when(userVolume.getOwner()).thenReturn(volumeOwner);
		when(userVolume.getRelativePath()).thenReturn("yourusername/");
		when(userVolume.getRootVolume()).thenReturn(rootVolume);
		when(userVolume.getResource()).thenReturn(mock(Resource.class));
		when(userVolume.getResource().getId()).thenReturn(1L);
        when(userVolume.getResource().getUuid()).thenReturn("abcde-fghij");

		when(rootVolume.getName()).thenReturn("Persistence Data - It's for keeps\u2122");
		when(rootVolume.getPathOnFileSystem()).thenReturn("/media/big_volume/persistence");
		when(rootVolume.getContainsSharedVolumes()).thenReturn(true);
		when(rootVolume.getDescription()).thenReturn("This should hold the files you care about."
				+ " Examples may include your thesis work or your students' thesis work."
				+ " Storing SSNs here is not recommended (yet).");
		when(rootVolume.getResource()).thenReturn(mock(Resource.class));
		when(rootVolume.getResource().getId()).thenReturn(5L);
        when(rootVolume.getResource().getUuid()).thenReturn("abcde-fghij");
		when(rootVolume.getId()).thenReturn(42L);

		Map<Long, List<String>> actions = new HashMap<>();
		actions.put(1L, Collections.singletonList(A_FILESERVICE_USERVOLUME_WRITE));

		RootVolumeModel rootVolumeDTO = mapper.getListingDTO(rootVolume, Collections.singletonList(userVolume),
				actions, Collections.emptyMap()).get();

		URL filename = getClass().getClassLoader().getResource("storem_json/list_rootvolume.json");
		assertThat(rootVolumeDTO,
				is(equalTo(objectMapper.readValue(filename, RootVolumeModel.class))));
	}

	@Test
	public void listRootVolumeNoUserVolumes() throws IOException {
		RootVolume rootVolume = mock(RootVolume.class);

		when(rootVolume.getName()).thenReturn("Persistence Data - It's for keeps\u2122");
		when(rootVolume.getPathOnFileSystem()).thenReturn("/media/big_volume/persistence");
		when(rootVolume.getDescription()).thenReturn("This should hold the files you care about."
				+ " Examples may include your thesis work or your students' thesis work."
				+ " Storing SSNs here is not recommended (yet).");
		when(rootVolume.getResource()).thenReturn(mock(Resource.class));
		when(rootVolume.getResource().getId()).thenReturn(1L);
		when(rootVolume.getId()).thenReturn(101L);
        when(rootVolume.getResource().getUuid()).thenReturn("abcde-fghij");

		Map<Long, List<String>> actions = new HashMap<>();
		actions.put(1L, Collections.singletonList(A_FILESERVICE_ROOTVOLUME_CREATE));

		RootVolumeModel rootVolumeDTO = mapper.getListingDTO(rootVolume, Collections.emptyList(),
				actions, Collections.emptyMap()).get();

		URL filename = getClass().getClassLoader().getResource("storem_json/list_rootvolume_no_uservolumes.json");
		assertThat(rootVolumeDTO,
				is(equalTo(objectMapper.readValue(filename, RootVolumeModel.class))));
	}

	@Test
	public void listFileService() throws IOException {
		RootVolume rootVolume1 = mock(RootVolume.class);
		UserVolume userVolume1 = mock(UserVolume.class);
		RootVolume rootVolume2 = mock(RootVolume.class);
		UserVolume userVolume2 = mock(UserVolume.class);

		when(userVolume1.getOwner()).thenReturn(mock(User.class));
		when(userVolume1.getOwner().getUsername()).thenReturn("sciserverUser1");
		when(userVolume1.getId()).thenReturn(0L);
		when(userVolume1.getName()).thenReturn("A volume");
		when(userVolume1.getDescription()).thenReturn("");
		when(userVolume1.getRelativePath()).thenReturn("/my new/new user volume/");
		when(userVolume1.getRootVolume()).thenReturn(rootVolume1);
		when(userVolume1.getResource()).thenReturn(mock(Resource.class));
		when(userVolume1.getResource().getId()).thenReturn(1L);
        when(userVolume1.getResource().getUuid()).thenReturn("abcde-fghij");

		when(userVolume2.getOwner()).thenReturn(mock(User.class));
		when(userVolume2.getOwner().getUsername()).thenReturn("scientist095802980571");
		when(userVolume2.getId()).thenReturn(9L);
		when(userVolume2.getName()).thenReturn("ASDFASDFSECRETFDSAFSFD");
		when(userVolume2.getDescription()).thenReturn("A place to store secret bday party plans");
		when(userVolume2.getRelativePath()).thenReturn("~myuser");
		when(userVolume2.getRootVolume()).thenReturn(rootVolume2);
		when(userVolume2.getResource()).thenReturn(mock(Resource.class));
		when(userVolume2.getResource().getId()).thenReturn(2L);
        when(userVolume2.getResource().getUuid()).thenReturn("abcde-fghij");

		when(rootVolume1.getName()).thenReturn("This should work fine");
		when(rootVolume1.getPathOnFileSystem()).thenReturn("/mnt/A path with spaces");
		when(rootVolume1.getDescription()).thenReturn("Better three hours too soon than a minute too late..");
		when(rootVolume1.getContainsSharedVolumes()).thenReturn(true);
		when(rootVolume1.getResource()).thenReturn(mock(Resource.class));
		when(rootVolume1.getResource().getId()).thenReturn(5L);
        when(rootVolume1.getResource().getUuid()).thenReturn("abcde-fghij");
		when(rootVolume1.getId()).thenReturn(10L);

		when(rootVolume2.getName()).thenReturn("Crazy Volume");
		when(rootVolume2.getPathOnFileSystem()).thenReturn("/A Path/On a *nix/FileSystem/can _.have/anything;'\"/even \uD83D\uDE06");
		when(rootVolume2.getDescription()).thenReturn("A root volume that is going to be a good test of well-written scripts if they use absolute paths");
		when(rootVolume2.getContainsSharedVolumes()).thenReturn(false);
		when(rootVolume2.getResource()).thenReturn(mock(Resource.class));
		when(rootVolume2.getResource().getId()).thenReturn(6L);
        when(rootVolume2.getResource().getUuid()).thenReturn("abcde-fghij");
		when(rootVolume2.getId()).thenReturn(20L);

		Map<Long, List<String>> actions = new HashMap<>();
		actions.put(1L, Arrays.asList(
				A_FILESERVICE_USERVOLUME_DELETE, A_FILESERVICE_USERVOLUME_GRANT,
				A_FILESERVICE_USERVOLUME_READ, A_FILESERVICE_USERVOLUME_WRITE));
		actions.put(2L, Arrays.asList(
				A_FILESERVICE_USERVOLUME_READ, A_FILESERVICE_USERVOLUME_WRITE));
		actions.put(6L, Arrays.asList(
				A_FILESERVICE_ROOTVOLUME_CREATE));

		FileService fileservice = mock(FileService.class);
		when(fileservice.getName()).thenReturn("My New File Service");
		when(fileservice.getId()).thenReturn(1L);
		when(fileservice.getDescription()).thenReturn("A service for reaching files");
		when(fileservice.getApiEndpoint()).thenReturn("https://my.fileservice.url.example.com/");
		when(fileservice.getRootVolume()).thenReturn(Arrays.asList(rootVolume1, rootVolume2));
		when(fileservice.getUserVolumes()).thenReturn(Arrays.asList(userVolume1, userVolume2));
		when(fileservice.getResourceContext()).thenReturn(mock(ResourceContext.class));
		when(fileservice.getResourceContext().getUuid()).thenReturn("53cf7625-a629-4631-b4c5-77c33d6fbb65");

		FileServiceModel fileServiceDTO = mapper.getListingDTO(fileservice,
				emptyList(),
				actions, Collections.emptyMap()).get();

		URL filename = getClass().getClassLoader().getResource("storem_json/list_fileservice.json");
		assertThat(fileServiceDTO,
				is(equalTo(objectMapper.readValue(filename, FileServiceModel.class))));
	}

	@Test
	public void listFileserviceNoActions() {
		RootVolume rootVolume1 = mock(RootVolume.class);
		UserVolume userVolume1 = mock(UserVolume.class);
		RootVolume rootVolume2 = mock(RootVolume.class);
		UserVolume userVolume2 = mock(UserVolume.class);

		when(userVolume1.getOwner()).thenReturn(mock(User.class));
		when(userVolume1.getOwner().getId()).thenReturn(2L);
		when(userVolume1.getRelativePath()).thenReturn("/my new/new user volume/");
		when(userVolume1.getRootVolume()).thenReturn(rootVolume1);
		when(userVolume1.getResource()).thenReturn(mock(Resource.class));
		when(userVolume1.getResource().getId()).thenReturn(1L);

		when(userVolume2.getOwner()).thenReturn(mock(User.class));
		when(userVolume2.getOwner().getId()).thenReturn(4L);
		when(userVolume2.getRelativePath()).thenReturn("~myuser");
		when(userVolume2.getRootVolume()).thenReturn(rootVolume2);
		when(userVolume2.getResource()).thenReturn(mock(Resource.class));
		when(userVolume2.getResource().getId()).thenReturn(2L);

		when(rootVolume1.getName()).thenReturn("This should work fine");
		when(rootVolume1.getPathOnFileSystem()).thenReturn("/mnt/A path with spaces");
		when(rootVolume1.getDescription()).thenReturn("Better three hours too soon than a minute too late..");
		when(rootVolume1.getResource()).thenReturn(mock(Resource.class));
		when(rootVolume1.getResource().getId()).thenReturn(5L);

		when(rootVolume2.getName()).thenReturn("Crazy Volume");
		when(rootVolume2.getPathOnFileSystem()).thenReturn("/A Path/On a *nix/FileSystem/can _.have/anything;'\"/even \uD83D\uDE06");
		when(rootVolume2.getDescription()).thenReturn("A root volume that is going to be a good test of well-written scripts if they use absolute paths");
		when(rootVolume2.getResource()).thenReturn(mock(Resource.class));
		when(rootVolume2.getResource().getId()).thenReturn(6L);

		Map<Long, List<String>> actions = Collections.emptyMap();

		FileService fileservice = mock(FileService.class);
		when(fileservice.getName()).thenReturn("My New File Service");
		when(fileservice.getId()).thenReturn(1L);
		when(fileservice.getDescription()).thenReturn("A service for reaching files");
		when(fileservice.getApiEndpoint()).thenReturn("https://my.fileservice.url.example.com/");
		when(fileservice.getRootVolume()).thenReturn(Arrays.asList(rootVolume1, rootVolume2));
		when(fileservice.getUserVolumes()).thenReturn(Arrays.asList(userVolume1, userVolume2));
		when(fileservice.getResourceContext()).thenReturn(mock(ResourceContext.class));
		when(fileservice.getResourceContext().getUuid()).thenReturn("53cf7625-a629-4631-b4c5-77c33d6fbb65");

		assertFalse(mapper.getListingDTO(fileservice, emptyList(),
				actions, Collections.emptyMap()).isPresent());
	}

	@Test
	public void listFileserviceAsFileservice() throws IOException {
		RootVolume rootVolume1 = mock(RootVolume.class);
		UserVolume userVolume1 = mock(UserVolume.class);
		RootVolume rootVolume2 = mock(RootVolume.class);
		UserVolume userVolume2 = mock(UserVolume.class);

		when(userVolume1.getOwner()).thenReturn(mock(User.class));
		when(userVolume1.getOwner().getUsername()).thenReturn("sciserverUser1");
		when(userVolume1.getId()).thenReturn(32L);
		when(userVolume1.getName()).thenReturn("A volume");
		when(userVolume1.getDescription()).thenReturn("");
		when(userVolume1.getRelativePath()).thenReturn("/my new/new user volume/");
		when(userVolume1.getRootVolume()).thenReturn(rootVolume1);
		when(userVolume1.getResource()).thenReturn(mock(Resource.class));
		when(userVolume1.getResource().getId()).thenReturn(1L);

		when(userVolume2.getOwner()).thenReturn(mock(User.class));
		when(userVolume2.getOwner().getUsername()).thenReturn("scientist095802980571");
		when(userVolume2.getId()).thenReturn(10L);
		when(userVolume2.getName()).thenReturn("ASDFASDFSECRETFDSAFSFD");
		when(userVolume2.getDescription()).thenReturn("A place to store secret bday party plans");
		when(userVolume2.getRelativePath()).thenReturn("~myuser");
		when(userVolume2.getRootVolume()).thenReturn(rootVolume2);
		when(userVolume2.getResource()).thenReturn(mock(Resource.class));
		when(userVolume2.getResource().getId()).thenReturn(2L);

		when(rootVolume1.getName()).thenReturn("This should work fine");
		when(rootVolume1.getPathOnFileSystem()).thenReturn("/mnt/A path with spaces");
		when(rootVolume1.getContainsSharedVolumes()).thenReturn(true);
		when(rootVolume1.getDescription()).thenReturn("Better three hours too soon than a minute too late..");
		when(rootVolume1.getResource()).thenReturn(mock(Resource.class));
		when(rootVolume1.getResource().getId()).thenReturn(5L);

		when(rootVolume2.getName()).thenReturn("Crazy Volume");
		when(rootVolume2.getPathOnFileSystem()).thenReturn("/A Path/On a *nix/FileSystem/can _.have/anything;'\"/even \uD83D\uDE06");
		when(rootVolume2.getContainsSharedVolumes()).thenReturn(false);
		when(rootVolume2.getDescription()).thenReturn("A root volume that is going to be a good test of well-written scripts if they use absolute paths");
		when(rootVolume2.getResource()).thenReturn(mock(Resource.class));
		when(rootVolume2.getResource().getId()).thenReturn(6L);

		FileService fileservice = mock(FileService.class);
		when(fileservice.getName()).thenReturn("My New File Service");
		when(fileservice.getId()).thenReturn(1L);
		when(fileservice.getDescription()).thenReturn("A service for reaching files");
		when(fileservice.getApiEndpoint()).thenReturn("https://my.fileservice.url.example.com/");
		when(fileservice.getServiceToken()).thenReturn("a9edab7b-4fb0-4527-bc24-e9daba99adf7");
		when(fileservice.getRootVolume()).thenReturn(Arrays.asList(rootVolume1, rootVolume2));
		when(fileservice.getUserVolumes()).thenReturn(Arrays.asList(userVolume1, userVolume2));
		when(fileservice.getResourceContext()).thenReturn(mock(ResourceContext.class));
		when(fileservice.getResourceContext().getUuid()).thenReturn("53cf7625-a629-4631-b4c5-77c33d6fbb65");

		RegisteredFileServiceModel fileServiceDTO = mapper.getRegisteredFileServiceView(
				fileservice, emptyList());

		URL filename = getClass().getClassLoader().getResource("storem_json/list_fileservice_from_fileservice.json");
		assertThat(fileServiceDTO,
				is(equalTo(objectMapper.readValue(filename, RegisteredFileServiceModel.class))));
	}

	@Before
	public void setupObjectMapper() {
		objectMapper.registerModule(new Jdk8Module());
	}
}
