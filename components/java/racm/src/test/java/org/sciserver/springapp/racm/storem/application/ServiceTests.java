package org.sciserver.springapp.racm.storem.application;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.Optional;
import org.ivoa.dm.VOURPException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sciserver.racm.storem.model.RegisterNewFileServiceModel;
import org.sciserver.racm.storem.model.RegisterNewRootVolumeModel;
import org.sciserver.racm.storem.model.RegisterNewUserVolumeModel;
import org.sciserver.springapp.racm.storem.application.FileServiceManager;
import org.sciserver.springapp.racm.storem.application.FileServiceRepository;
import org.sciserver.springapp.racm.storem.application.RegistrationInvalidException;

public class ServiceTests {
	private FileServiceRepository repo = mock(FileServiceRepository.class);
	private FileServiceManager manager = new FileServiceManager(repo);

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void rootVolumeEmptyName() {
		RegisterNewRootVolumeModel newRootVolume = new RegisterNewRootVolumeModel("", "", "", false);

		thrown.expect(RegistrationInvalidException.class);
		thrown.expectMessage(is("Root volume name must be a non-empty path segment, i.e., not contain '/'."));

		manager.registerRootVolume(null, null, newRootVolume);
	}

	@Test
	public void rootVolumeInvalidName() {
		RegisterNewRootVolumeModel newRootVolume = new RegisterNewRootVolumeModel("some/name", "", "", true);

		thrown.expect(RegistrationInvalidException.class);
		thrown.expectMessage(is("Root volume name must be a non-empty path segment, i.e., not contain '/'."));

		manager.registerRootVolume(null, null, newRootVolume);
	}

	@Test
	public void userVolumeEmptyName() {
		RegisterNewUserVolumeModel newUserVolume = new RegisterNewUserVolumeModel("", "", "", Optional.empty());

		thrown.expect(RegistrationInvalidException.class);
		thrown.expectMessage(is("User volume name must be a non-empty path segment, i.e., not contain '/'."));
		try {
		    manager.registerUserVolume(null, "", "", newUserVolume);
		}catch(VOURPException e) {
		    
		}
	}

	@Test
	public void userVolumeInvalidName() {
		RegisterNewUserVolumeModel newUserVolume = new RegisterNewUserVolumeModel("a/name", "", "", Optional.empty());

		thrown.expect(RegistrationInvalidException.class);
		thrown.expectMessage(is("User volume name must be a non-empty path segment, i.e., not contain '/'."));

		try {
		    manager.registerUserVolume(null, "", "", newUserVolume);
		} catch(VOURPException e) {
		    
		}
	}

	@Test
	public void fileServiceNotURI() {
		RegisterNewFileServiceModel newFileService =
				new RegisterNewFileServiceModel("file service", "", "not-a-url", null, null,
						Collections.emptyList());

		thrown.expect(RegistrationInvalidException.class);
		thrown.expectMessage(is("File service must be a valid https URL."));

		manager.registerFileService(null, newFileService);
	}

	@Test
	public void fileServiceURLNotEmpty() {
		RegisterNewFileServiceModel newFileService = new RegisterNewFileServiceModel(
				"file service", "", "", null, null, Collections.emptyList());

		thrown.expect(RegistrationInvalidException.class);
		thrown.expectMessage(is("File service must be a valid https URL."));

		manager.registerFileService(null, newFileService);
	}
}
