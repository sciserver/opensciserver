package org.sciserver.springapp.racm.storem.controller;

import static org.sciserver.springapp.racm.auth.SciServerHeaderAuthenticationFilter.AUTH_HEADER;

import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import org.ivoa.dm.VOURPException;
import org.sciserver.racm.storem.model.FileServiceModel;
import org.sciserver.racm.storem.model.RegisterNewDataVolumeModel;
import org.sciserver.racm.storem.model.RegisterNewRootVolumeModel;
import org.sciserver.racm.storem.model.RegisterNewUserVolumeModel;
import org.sciserver.racm.storem.model.RegisteredDataVolumeModel;
import org.sciserver.racm.storem.model.RegisteredRootVolumeModel;
import org.sciserver.racm.storem.model.RegisteredUserVolumeModel;
import org.sciserver.racm.storem.model.UpdateSharedWithEntry;
import org.sciserver.racm.storem.model.UpdatedDataVolumeInfo;
import org.sciserver.racm.storem.model.UpdatedFileServiceInfo;
import org.sciserver.racm.storem.model.UpdatedRootVolumeInfo;
import org.sciserver.racm.storem.model.UpdatedUserVolumeInfo;
import org.sciserver.racm.utils.model.NativeQueryResult;
import org.sciserver.springapp.racm.storem.application.FileServiceManager;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.sciserver.springapp.racm.utils.controller.ResourceNotFoundException;
import org.sciserver.springapp.racm.utils.logging.LogUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.util.UriUtils;

@CrossOrigin
@RestController
@FileServiceTokenRequired
@RequestMapping(value = "/storem")
public class FileServiceUserRequiredController {
    private final FileServiceManager fsManager;

    @Autowired
    FileServiceUserRequiredController(FileServiceManager fsService) {
        this.fsManager = fsService;
    }

    @GetMapping(value = "/fileservice/{fileServiceIdentifer}/OLD", headers = AUTH_HEADER)
    public FileServiceModel getDetailsOfFileService(@PathVariable String fileServiceIdentifer,
            @AuthenticationPrincipal UserProfile up) {
        return fsManager.getFileService(up, fileServiceIdentifer);
    }

    @GetMapping(value = "/fileservice/{fileServiceIdentifer}", headers = AUTH_HEADER)
    public FileServiceModel getDetailsOfFileServiceFAST(@PathVariable String fileServiceIdentifer,
            @AuthenticationPrincipal UserProfile up) {
        return fsManager.getFileServiceFAST(up, fileServiceIdentifer);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/fileservice/{fileServiceIdentifer}")
    public void unregisterFileService(@PathVariable String fileServiceIdentifer,
            @AuthenticationPrincipal UserProfile up) {
        fsManager.deleteFileService(up, fileServiceIdentifer);
    }

    @GetMapping(value = "/fileservice/{fileServiceIdentifer}/rootVolume/{rootVolumeName}/allowedActions")
    public Set<String> queryRootVolumeActions(@PathVariable String fileServiceIdentifer,
            @PathVariable String rootVolumeName, @AuthenticationPrincipal UserProfile up) {

        Set<String> allowedActions = fsManager.getRootVolumeActions(up, fileServiceIdentifer, rootVolumeName);
        if (allowedActions.isEmpty())
            throw new ResourceNotFoundException();
        else
            return allowedActions;
    }

    @GetMapping(value = "/fileservice/{fileServiceIdentifer}/dataVolume/{dataVolumeName}/allowedActions")
    public NativeQueryResult queryDataVolumeActions(@PathVariable String fileServiceIdentifer,
            @PathVariable String dataVolumeName, @AuthenticationPrincipal UserProfile up) {

        NativeQueryResult allowedActions = fsManager.getDataVolumeActions(up, fileServiceIdentifer, dataVolumeName);
        return allowedActions;
    }

    @GetMapping(value = "/fileservice/{fileServiceIdentifer}/rootVolume/{rootVolumeName}/userVolume/{owner}/{userVolumeName}/allowedActions")
    public NativeQueryResult queryUserVolumeActions(@PathVariable String fileServiceIdentifer,
            @PathVariable String rootVolumeName, @PathVariable String owner, @PathVariable String userVolumeName,
            @AuthenticationPrincipal UserProfile up) {

        NativeQueryResult allowedActions = fsManager.getUserVolumeActions(up, fileServiceIdentifer, rootVolumeName,
                owner, userVolumeName);
        return allowedActions;
    }

    @PostMapping("/fileservice/{fileServiceIdentifer}/rootVolumes")
    public ResponseEntity<Void> newRootVolume(@PathVariable String fileServiceIdentifer,
            @RequestBody RegisterNewRootVolumeModel rootVolume, HttpServletResponse response,
            @AuthenticationPrincipal UserProfile up) {

        RegisteredRootVolumeModel newRootVolume = fsManager.registerRootVolume(up, fileServiceIdentifer, rootVolume);
        return ResponseEntity.created(MvcUriComponentsBuilder
                .fromMethodName(getClass(), "unregisterRootVolume", fileServiceIdentifer, newRootVolume.getName(), null)
                .build().toUri()).build();
    }

    @PostMapping("/fileservice/{fileServiceIdentifer}/dataVolumes")
    public ResponseEntity<Void> newDataVolume(@PathVariable String fileServiceIdentifer,
            @RequestBody RegisterNewDataVolumeModel dataVolume, HttpServletResponse response,
            @AuthenticationPrincipal UserProfile up) {
        RegisteredDataVolumeModel newDataVolume = fsManager.registerDataVolume(up, fileServiceIdentifer, dataVolume);
        return ResponseEntity.created(MvcUriComponentsBuilder
                .fromMethodName(getClass(), "unregisterDataVolume", fileServiceIdentifer, newDataVolume.getName(), null)
                .build().toUri()).build();
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/fileservice/{fileServiceIdentifer}/dataVolume/{dataVolumeName}")
    public void unregisterDataVolume(@PathVariable String fileServiceIdentifer, @PathVariable String dataVolumeName,
            @AuthenticationPrincipal UserProfile up) {
        String decodedDataName = UriUtils.decode(dataVolumeName, "UTF-8");
        fsManager.deleteDataVolume(up, fileServiceIdentifer, decodedDataName);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/fileservice/{fileServiceIdentifer}/rootVolume/{rootVolumeName}")
    public void unregisterRootVolume(@PathVariable String fileServiceIdentifer, @PathVariable String rootVolumeName,
            @AuthenticationPrincipal UserProfile up) {
        fsManager.deleteRootVolume(up, fileServiceIdentifer, rootVolumeName);
    }

    @PostMapping("/fileservice/{fileServiceIdentifer}/rootVolume/{rootVolumeName}/userVolumes")
    public ResponseEntity<Void> newUserVolume(@PathVariable String fileServiceIdentifer,
            @PathVariable String rootVolumeName, @RequestBody RegisterNewUserVolumeModel userVolume,
            HttpServletResponse response, @AuthenticationPrincipal UserProfile up) {

        String decodedRootName = UriUtils.decode(rootVolumeName, "UTF-8");
        RegisteredUserVolumeModel newUserVolume = null;
        
        try {
            newUserVolume = fsManager.registerUserVolume(up, fileServiceIdentifer, decodedRootName, userVolume);
            return ResponseEntity
                    .created(
                            MvcUriComponentsBuilder
                                    .fromMethodName(getClass(), "unregisterUserVolume", fileServiceIdentifer,
                                            decodedRootName, newUserVolume.getOwner(), newUserVolume.getName(), null)
                                    .build().toUri())
                    .build();
        } catch(VOURPException e) {
            // TODO should log as well
            return new ResponseEntity(e,HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/fileservice/{fileServiceIdentifer}/rootVolume/{rootVolumeName}/userVolume/{owner}/{userVolumeName}")
    public void unregisterUserVolume(@PathVariable String fileServiceIdentifer, @PathVariable String rootVolumeName,
            @PathVariable String owner, @PathVariable String userVolumeName, @AuthenticationPrincipal UserProfile up) {
        fsManager.deleteUserVolume(up, fileServiceIdentifer, rootVolumeName, owner, userVolumeName);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/fileservice/{fileServiceIdentifer}/rootVolume/{rootVolumeName}/userVolume/{owner}/{userVolumeName}")
    public void editUserVolume(@PathVariable String fileServiceIdentifer, @PathVariable String rootVolumeName,
            @PathVariable String owner, @PathVariable String userVolumeName,
            @RequestBody UpdatedUserVolumeInfo updatedUserVolumeInfo, @AuthenticationPrincipal UserProfile up) {
        fsManager.updateUserVolume(up, fileServiceIdentifer, rootVolumeName, owner, userVolumeName,
                updatedUserVolumeInfo);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/fileservice/{fileServiceIdentifier}/rootVolume/{rootVolumeName}")
    public void editRootVolume(@PathVariable String fileServiceIdentifier, @PathVariable String rootVolumeName,
            @RequestBody UpdatedRootVolumeInfo updatedRootVolumeInfo, @AuthenticationPrincipal UserProfile up) {
        fsManager.updateRootVolume(up, fileServiceIdentifier, rootVolumeName, updatedRootVolumeInfo);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/fileservice/{fileServiceIdentifier}/dataVolume/{dataVolumeName}")
    public void editDataVolume(@PathVariable String fileServiceIdentifier, @PathVariable String dataVolumeName,
            @RequestBody UpdatedDataVolumeInfo updatedDataVolumeInfo, @AuthenticationPrincipal UserProfile up) {
        fsManager.updateDataVolume(up, fileServiceIdentifier, dataVolumeName, updatedDataVolumeInfo);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/fileservice/{fileServiceIdentifier}")
    public void editFileService(@PathVariable String fileServiceIdentifier,
            @RequestBody UpdatedFileServiceInfo updatedFileServiceInfo, @AuthenticationPrincipal UserProfile up) {
        fsManager.updateFileService(up, fileServiceIdentifier, updatedFileServiceInfo);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/fileservice/{fileServiceIdentifer}/rootVolume/{rootVolumeName}/userVolume/{owner}/{userVolumeName}/sharedWith")
    public void shareUserVolume(@PathVariable String fileServiceIdentifer, @PathVariable String rootVolumeName,
            @PathVariable String owner, @PathVariable String userVolumeName, @AuthenticationPrincipal UserProfile up,
            @RequestBody UpdateSharedWithEntry... updatedSharing) {
        fsManager.updateSharing(up, fileServiceIdentifer, rootVolumeName, owner, userVolumeName, updatedSharing);

        LogUtils.buildLog().forFileService().showInUserHistory().user(up).sentence().subject(up.getUsername())
                .verb("shared").predicate("%s's '%s' volume '%s'", owner, rootVolumeName, userVolumeName)
                .extraField("userVolumeName", userVolumeName).extraField("rootVolumeName", rootVolumeName)
                .extraField("fileServiceIdentifier", fileServiceIdentifer).log();
    }
    /**
     * This method allows a user to unshare a user volume not owned by them.
     * 
     * @param fileServiceIdentifer
     * @param rootVolumeName
     * @param owner                MUST NOT be the same user as the specified up
     * @param userVolumeName
     * @param up                   MUST NOT be the owner
     */
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/fileservice/{fileServiceIdentifer}/rootVolume/{rootVolumeName}/userVolume/{owner}/{userVolumeName}/shares")
    public void unshareUserVolumeWithMe(@PathVariable String fileServiceIdentifer, @PathVariable String rootVolumeName,
            @PathVariable String owner, @PathVariable String userVolumeName, @AuthenticationPrincipal UserProfile up) {
        fsManager.removeMyShares(up, fileServiceIdentifer, rootVolumeName, owner, userVolumeName);

        LogUtils.buildLog().forFileService().showInUserHistory().user(up).sentence().subject(up.getUsername())
                .verb("unshare").predicate("%s's '%s' volume '%s'", owner, rootVolumeName, userVolumeName)
                .extraField("userVolumeName", userVolumeName).extraField("rootVolumeName", rootVolumeName)
                .extraField("fileServiceIdentifier", fileServiceIdentifer).log();
    }
    
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/fileservice/{fileServiceIdentifer}/dataVolume/{dataVolumeName}/sharedWith")
    public void shareDataVolume(@PathVariable String fileServiceIdentifer, @PathVariable String dataVolumeName,
            @AuthenticationPrincipal UserProfile up, @RequestBody UpdateSharedWithEntry... updatedSharing) {
        fsManager.updateSharingOfDataVolumes(up, fileServiceIdentifer, dataVolumeName, updatedSharing);

        LogUtils.buildLog().forFileService().showInUserHistory().user(up).sentence().subject(up.getUsername())
                .verb("shared").predicate("data volume '%s'", dataVolumeName)
                .extraField("fileServiceIdentifier", fileServiceIdentifer).log();
    }
}