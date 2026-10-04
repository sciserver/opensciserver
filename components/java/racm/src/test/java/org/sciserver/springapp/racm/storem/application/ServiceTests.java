package org.sciserver.springapp.racm.storem.application;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.sciserver.racm.storem.model.RegisterNewFileServiceModel;
import org.sciserver.racm.storem.model.RegisterNewRootVolumeModel;
import org.sciserver.racm.storem.model.RegisterNewUserVolumeModel;

public class ServiceTests {
	private FileServiceRepository repo = mock(FileServiceRepository.class);
	private FileServiceManager manager = new FileServiceManager(repo);

	@Test
	public void rootVolumeEmptyName() {
		RegisterNewRootVolumeModel newRootVolume = new RegisterNewRootVolumeModel("", "", "", false);

		RegistrationInvalidException thrown = assertThrows(RegistrationInvalidException.class,
				() -> manager.registerRootVolume(null, null, newRootVolume));

		assertThat(thrown.getMessage(), is("Root volume name must be a non-empty path segment, i.e., not contain '/'."));
	}

	@Test
	public void rootVolumeInvalidName() {
		RegisterNewRootVolumeModel newRootVolume = new RegisterNewRootVolumeModel("some/name", "", "", true);

		RegistrationInvalidException thrown = assertThrows(RegistrationInvalidException.class,
				() -> manager.registerRootVolume(null, null, newRootVolume));

		assertThat(thrown.getMessage(), is("Root volume name must be a non-empty path segment, i.e., not contain '/'."));
	}

	@Test
	public void userVolumeEmptyName() {
		RegisterNewUserVolumeModel newUserVolume = new RegisterNewUserVolumeModel("", "", "", Optional.empty());

		RegistrationInvalidException thrown = assertThrows(RegistrationInvalidException.class,
				() -> manager.registerUserVolume(null, "", "", newUserVolume));

		assertThat(thrown.getMessage(), is("User volume name must be a non-empty path segment, i.e., not contain '/'."));
	}

	@Test
	public void userVolumeInvalidName() {
		RegisterNewUserVolumeModel newUserVolume = new RegisterNewUserVolumeModel("a/name", "", "", Optional.empty());

		RegistrationInvalidException thrown = assertThrows(RegistrationInvalidException.class,
				() -> manager.registerUserVolume(null, "", "", newUserVolume));

		assertThat(thrown.getMessage(), is("User volume name must be a non-empty path segment, i.e., not contain '/'."));
	}

	@Test
	public void fileServiceNotURI() {
		RegisterNewFileServiceModel newFileService =
				new RegisterNewFileServiceModel("file service", "", "not-a-url", null, null,
						Collections.emptyList());

		RegistrationInvalidException thrown = assertThrows(RegistrationInvalidException.class,
				() -> manager.registerFileService(null, newFileService));

		assertThat(thrown.getMessage(), is("File service must be a valid https URL."));
	}

	@Test
	public void fileServiceURLNotEmpty() {
		RegisterNewFileServiceModel newFileService = new RegisterNewFileServiceModel(
				"file service", "", "", null, null, Collections.emptyList());

		RegistrationInvalidException thrown = assertThrows(RegistrationInvalidException.class,
				() -> manager.registerFileService(null, newFileService));

		assertThat(thrown.getMessage(), is("File service must be a valid https URL."));
	}
}
