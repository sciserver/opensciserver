package org.sciserver.springapp.fileservice;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Optional;
import org.sciserver.authentication.client.AuthenticatedUser;
import org.sciserver.clientutils.SciServerClientException;
import org.sciserver.racm.client.RACMClient;
import org.sciserver.racm.storem.model.FileServiceModel;
import org.sciserver.racm.storem.model.RegisterNewUserVolumeModel;
import org.sciserver.racm.storem.model.RootVolumeModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
public class VolumesManager {
    @Autowired
    private RACMClient racmClient;
    @Value("${RACM.resourcecontext.uuid}")
    private String fileServiceIdentifier;
    @Autowired
    private Config config;

    public FileServiceModel getVolumesAndLazilyCreate(AuthenticatedUser user) throws IOException, SciServerClientException {
        FileServiceModel fileServiceInfo = racmClient.getDetailsOfFileService(user.getToken(), fileServiceIdentifier);
        boolean updated = false;
        assert fileServiceInfo != null;
        for (RootVolumeModel rootVolume : fileServiceInfo.getRootVolumes()) {
            if (rootVolume.getAllowedActions().contains("create")) {
                File baseDir = new File(rootVolume.getPathOnFileSystem(), user.getUserId());
                if (!baseDir.exists()) {
                    Files.createDirectories(
                        baseDir.toPath(),
                        PosixFilePermissions.asFileAttribute(config.getDefaultDirPerms()));
                }
                if (!rootVolume.isContainsSharedVolumes() && rootVolume.getUserVolumes().isEmpty()) {
                    String relativePath = user.getUserId() + "/";
                    RegisterNewUserVolumeModel newUserVolume = new RegisterNewUserVolumeModel(
                            rootVolume.getName(), getDescriptionForRootVolume(rootVolume.getName(), user.getUserName()),
                            relativePath, Optional.empty());
                    racmClient.newUserVolume(user.getToken(),
                                             fileServiceIdentifier, rootVolume.getName(), newUserVolume);
                    updated = true;
                }
            }
        }
        if (updated) {
            fileServiceInfo = racmClient.getDetailsOfFileService(user.getToken(), fileServiceIdentifier);
        }
        return fileServiceInfo;
    }

    private String getDescriptionForRootVolume(String rootVolumeName, String username) {
        switch (rootVolumeName) {
            case "persistent":
                return "Long term storage available for each user";
            case "scratch":
                return "Transient storage that may be periodically cleared.";
            default:
                return "Volume created by " + username;
        }
    }
}
