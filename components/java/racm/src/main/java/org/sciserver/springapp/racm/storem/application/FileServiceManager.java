package org.sciserver.springapp.racm.storem.application;

import java.util.List;
import java.util.Set;

import org.apache.commons.validator.routines.UrlValidator;
import org.ivoa.dm.VOURPException;
import org.sciserver.racm.storem.model.FileServiceModel;
import org.sciserver.racm.storem.model.MinimalFileServiceModel;
import org.sciserver.racm.storem.model.RegisterNewDataVolumeModel;
import org.sciserver.racm.storem.model.RegisterNewFileServiceModel;
import org.sciserver.racm.storem.model.RegisterNewRootVolumeModel;
import org.sciserver.racm.storem.model.RegisterNewServiceVolumeModel;
import org.sciserver.racm.storem.model.RegisterNewUserVolumeModel;
import org.sciserver.racm.storem.model.RegisteredDataVolumeModel;
import org.sciserver.racm.storem.model.RegisteredFileServiceModel;
import org.sciserver.racm.storem.model.RegisteredRootVolumeModel;
import org.sciserver.racm.storem.model.RegisteredServiceVolumeModel;
import org.sciserver.racm.storem.model.RegisteredUserVolumeModel;
import org.sciserver.racm.storem.model.UpdateSharedWithEntry;
import org.sciserver.racm.storem.model.UpdatedDataVolumeInfo;
import org.sciserver.racm.storem.model.UpdatedFileServiceInfo;
import org.sciserver.racm.storem.model.UpdatedRootVolumeInfo;
import org.sciserver.racm.storem.model.UpdatedUserVolumeInfo;
import org.sciserver.racm.utils.model.NativeQueryResult;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/*
 * A service which implements the logic (default permissions, validation, etc)
 * needed for File Service-related objects. It is expected that controllers and such will use
 * this service to interact with the repository.
 */
@Service
public class FileServiceManager {
	private final FileServiceRepository repo;

	private static final long URL_VALIDATOR_OPTIONS = UrlValidator.ALLOW_LOCAL_URLS + UrlValidator.NO_FRAGMENTS;
	private final UrlValidator anyURLValidator = new UrlValidator(URL_VALIDATOR_OPTIONS);

	@Autowired
	public FileServiceManager(FileServiceRepository repo) {
		this.repo = repo;
	}

	public List<String> getFileServiceEndpoints(UserProfile up) {
		return repo.getFileServiceEndpoints(up);
	}

	public List<MinimalFileServiceModel> getMinimalFileServices(UserProfile up) {
		return repo.getMinimalFileServices(up);
	}

	public FileServiceModel getFileService(UserProfile up, String fileServiceIdentifer) {
		return repo.getFileService(up, fileServiceIdentifer);
	}

    public FileServiceModel getFileServiceFAST(UserProfile up, String fileServiceIdentifer) {
        return repo.getFileServiceFAST(up, fileServiceIdentifer);
    }

    public RegisteredFileServiceModel getFileServiceFromToken(String token) {
		return repo.getRegisteredFileServiceFromToken(token);
	}

	public RegisteredFileServiceModel registerFileService(UserProfile up, RegisterNewFileServiceModel fileService) {
		validateFileService(fileService);
		fileService.getIdentifier().ifPresent((identifier) -> {
			if (repo.existsByIdentifier(up, identifier)) {
				throw new DuplicateIdentifierException();
			}
		});
		if (repo.existsByName(up, fileService.getName())) {
			throw new RegistrationInvalidException("File Service names must be unique");
		}

		fileService.getRootVolumes().forEach(this::validateRootVolume);

		return repo.registerFileService(up, fileService);
	}

	public RegisteredRootVolumeModel registerRootVolume(UserProfile up, String fileServiceIdentifer, RegisterNewRootVolumeModel rootVolume) {
		validateRootVolume(rootVolume);
		return repo.registerRootVolume(up, fileServiceIdentifer, rootVolume);
	}

	public RegisteredUserVolumeModel registerUserVolume(UserProfile up, String fileServiceIdentifer, 
	        String rootVolumeName, RegisterNewUserVolumeModel userVolume) throws VOURPException{
		validateUserVolume(userVolume);

        return repo.registerUserVolume(up, fileServiceIdentifer, rootVolumeName, userVolume);
	}

    public RegisteredDataVolumeModel registerDataVolume(UserProfile up, String fileServiceIdentifer,
			RegisterNewDataVolumeModel dataVolume) {
		validateDataVolume(dataVolume);

		return repo.registerDataVolume(up, fileServiceIdentifer, dataVolume);
	}

	public Set<String> getRootVolumeActions(UserProfile up, String fileServiceIdentifer, String rootVolumeName) {
		return repo.getRootVolumeActions(up, fileServiceIdentifer, rootVolumeName);
	}

	public NativeQueryResult getDataVolumeActions(UserProfile up, String fileServiceIdentifer, String dataVolumeName) {
		return repo.getDataVolumeActions(up, fileServiceIdentifer, dataVolumeName);
	}

	public NativeQueryResult getUserVolumeActions(UserProfile up, String fileServiceIdentifer, String rootVolumeName, String userVolumeOwnerName, String userVolumeName) {
		return repo.getUserVolumeActions(up, fileServiceIdentifer, rootVolumeName, userVolumeOwnerName, userVolumeName);
	}

	public NativeQueryResult getServiceVolumeActions(UserProfile up, String fileServiceIdentifer, String serviceToken, String rootVolumeName, String userVolumeOwnerName, String userVolumeName) {
		return repo.getServiceVolumeActions(up, fileServiceIdentifer, serviceToken, rootVolumeName, userVolumeOwnerName, userVolumeName);
	}
	
    /** 
     * Remove the privileges the specified UserProfile has on the uservolume owned BY ANOTHE RUSER.<br/>
     * @param up
     * @param fileServiceIdentifer
     * @param rootVolumeName
     * @param userVolumeOwnerName
     * @param userVolumeName
     */
    public void removeMyShares (UserProfile up, String fileServiceIdentifer, String rootVolumeName, String userVolumeOwnerName, String userVolumeName) {
        repo.removeMyShares(up, fileServiceIdentifer, rootVolumeName, userVolumeOwnerName, userVolumeName);
    }
    
	public void updateSharing(UserProfile up, String fileServiceIdentifer, String rootVolumeName, String userVolumeOwnerName, String userVolumeName, UpdateSharedWithEntry[] updatedEntries) {
		for (UpdateSharedWithEntry updatedEntry : updatedEntries)
			updateSharing(up, fileServiceIdentifer, rootVolumeName, userVolumeOwnerName, userVolumeName, updatedEntry);
	}

	private void updateSharing(UserProfile up, String fileServiceIdentifer, String rootVolumeName, String userVolumeOwnerName, String userVolumeName, UpdateSharedWithEntry updatedEntry) {
		repo.updateSharing(up, fileServiceIdentifer, rootVolumeName, userVolumeOwnerName, userVolumeName, updatedEntry);
	}

	public void updateSharingOfDataVolumes(UserProfile up, String fileServiceIdentifer, String dataVolumeName, UpdateSharedWithEntry[] updatedEntries) {
		for (UpdateSharedWithEntry updatedEntry : updatedEntries)
			updateSharingOfDataVolumes(up, fileServiceIdentifer, dataVolumeName, updatedEntry);
	}

	private void updateSharingOfDataVolumes(UserProfile up, String fileServiceIdentifer, String dataVolumeName, UpdateSharedWithEntry updatedEntry) {
		repo.updateSharingOfDataVolumes(up, fileServiceIdentifer, dataVolumeName, updatedEntry);
	}

	private void validateUserVolume(RegisterNewUserVolumeModel userVolume) {
		if (userVolume.getName().isEmpty() || userVolume.getName().contains("/"))
			throw new RegistrationInvalidException("User volume name must be a non-empty path segment, i.e., not contain '/'.");
	}
	
    public void deleteUserVolume(UserProfile up, String fileServiceIdentifer, String rootVolumeName, String userVolumeOwnerName, String userVolumeName) {
		repo.deleteUserVolume(up, fileServiceIdentifer, rootVolumeName, userVolumeOwnerName, userVolumeName);
	}

    public void deleteServiceVolume(UserProfile up, String fileServiceIdentifer, String rootVolumeName, String userVolumeOwnerName, String userVolumeName, 
            String serviceToken) {
        repo.deleteServiceVolume(up, fileServiceIdentifer, rootVolumeName, userVolumeOwnerName, userVolumeName, serviceToken);
    }
    
	public void deleteRootVolume(UserProfile up, String fileServiceIdentifer, String rootVolumeName) {
		repo.deleteRootVolume(up, fileServiceIdentifer, rootVolumeName);
	}

	public void deleteDataVolume(UserProfile up, String fileServiceIdentifer, String dataVolumeName) {
		repo.deleteDataVolume(up, fileServiceIdentifer, dataVolumeName);
	}

	public void deleteFileService(UserProfile up, String fileServiceIdentifer) {
		repo.deleteFileService(up, fileServiceIdentifer);
	}

	public void updateUserVolume(UserProfile up, String fileServiceIdentifer, String rootVolumeName,
			String userVolumeOwnerName, String userVolumeName, UpdatedUserVolumeInfo updatedUserVolumeInfo) {
		repo.updateUserVolume(up, fileServiceIdentifer, rootVolumeName, userVolumeOwnerName, userVolumeName, updatedUserVolumeInfo);
	}

	public void updateRootVolume(UserProfile up, String fileServiceIdentifier, String rootVolumeName,
			UpdatedRootVolumeInfo updatedRootVolumeInfo) {
		repo.updateRootVolume(up, fileServiceIdentifier, rootVolumeName, updatedRootVolumeInfo);
	}

	public void updateFileService(UserProfile up, String fileServiceIdentifier,
			UpdatedFileServiceInfo updatedFileServiceInfo) {

		repo.updateFileService(up, fileServiceIdentifier, updatedFileServiceInfo);
	}

	public void updateDataVolume(UserProfile up, String fileServiceIdentifier, String dataVolumeName,
			UpdatedDataVolumeInfo updatedDataVolumeInfo) {
		repo.updateDataVolume(up, fileServiceIdentifier, dataVolumeName, updatedDataVolumeInfo);
	}

	private void validateRootVolume(RegisterNewRootVolumeModel rootVolume) {
		if(rootVolume.getName().isEmpty() || rootVolume.getName().contains("/"))
			throw new RegistrationInvalidException("Root volume name must be a non-empty path segment, i.e., not contain '/'.");
	}

	private void validateDataVolume(RegisterNewDataVolumeModel dataVolume) {
		if(dataVolume.getName().isEmpty() || dataVolume.getName().contains("/"))
			throw new RegistrationInvalidException("Root volume name must be a non-empty path segment, i.e., not contain '/'.");
	}

	private void validateFileService(RegisterNewFileServiceModel fileService) {
		if(!anyURLValidator.isValid(fileService.getApiEndpoint()))
			throw new RegistrationInvalidException("File service must be a valid https URL.");
	}

    private void validateServiceVolume(RegisterNewServiceVolumeModel serviceVolume) {
        if (serviceVolume.getName().isEmpty() || serviceVolume.getName().contains("/"))
            throw new RegistrationInvalidException("Service volume name must be a non-empty path segment, i.e., not contain '/'.");
    }
    public RegisteredServiceVolumeModel registerServiceVolume(UserProfile owner, String fileServiceIdentifer, 
            String rootVolumeName, RegisterNewServiceVolumeModel serviceVolume) throws VOURPException {
        return repo.registerServiceVolume(owner, fileServiceIdentifer, rootVolumeName, serviceVolume);
    }

}
