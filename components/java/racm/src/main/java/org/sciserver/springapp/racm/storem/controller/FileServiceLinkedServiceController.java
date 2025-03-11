package org.sciserver.springapp.racm.storem.controller;

import static org.sciserver.racm.storem.model.RegisterNewServiceVolumeModel.SERVICE_TOKEN_HEADER;
import javax.servlet.http.HttpServletResponse;

import org.ivoa.dm.VOURPException;
import org.sciserver.racm.storem.model.RegisterNewServiceVolumeModel;
import org.sciserver.racm.storem.model.RegisteredServiceVolumeModel;
import org.sciserver.racm.storem.model.UpdateSharedWithEntry;
import org.sciserver.racm.utils.model.NativeQueryResult;
import org.sciserver.springapp.racm.storem.application.FileServiceManager;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

/**
 * Methods in this class require a fileservice serviceToken as well as a serviceToken for a 
 * 
 * 
 * @author Gerard
 *
 */
@CrossOrigin
@RestController
@FileServiceTokenRequired
@RequestMapping(value="/storem")
public class FileServiceLinkedServiceController {

    private final FileServiceManager fsManager;

    @Autowired
    FileServiceLinkedServiceController(FileServiceManager fsService) {
        this.fsManager = fsService;
    }

    @PostMapping("/fileservice/{fileServiceIdentifer}/rootVolume/{rootVolumeName}/serviceVolumes")
    public ResponseEntity<RegisteredServiceVolumeModel> newServiceVolume(@PathVariable String fileServiceIdentifer,
            @PathVariable String rootVolumeName, @RequestBody RegisterNewServiceVolumeModel serviceVolume,
            HttpServletResponse response, @AuthenticationPrincipal UserProfile up) {
        try {
            String decodedRootName = UriUtils.decode(rootVolumeName, "UTF-8");
            RegisteredServiceVolumeModel newUserVolume = fsManager.registerServiceVolume(up, fileServiceIdentifer, decodedRootName, serviceVolume);
            return new ResponseEntity<RegisteredServiceVolumeModel>(newUserVolume, HttpStatus.OK);
        } catch(VOURPException e) {
            // TODO should log as well
            return new ResponseEntity(e,HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    /**
     * Remove a uservolume that is owned by a resource on a certain service.<br/>
     * @param fileServiceIdentifer
     * @param rootVolumeName
     * @param owner
     * @param userVolumeName
     * @param serviceToken
     * @param owningResourceUUID
     * @param up
     */
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/fileservice/{fileServiceIdentifer}/rootVolume/{rootVolumeName}/serviceVolume/{owner}/{userVolumeName}")
    public void unregisterServiceVolume(@PathVariable String fileServiceIdentifer, @PathVariable String rootVolumeName,
            @PathVariable String owner, @PathVariable String userVolumeName, 
            @RequestHeader(value=SERVICE_TOKEN_HEADER, required=true) String serviceToken, @AuthenticationPrincipal UserProfile up) {
        fsManager.deleteServiceVolume(up, fileServiceIdentifer, rootVolumeName, owner, userVolumeName, serviceToken);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/fileservice/{fileServiceIdentifer}/rootVolume/{rootVolumeName}/serviceVolume/{owner}/{userVolumeName}/sharedWith")
    public void shareServiceVolume(@PathVariable String fileServiceIdentifer, @PathVariable String rootVolumeName,
            @PathVariable String owner, @PathVariable String userVolumeName, @AuthenticationPrincipal UserProfile up,
            @RequestBody UpdateSharedWithEntry ...updatedSharing) {
        fsManager.updateSharing(up, fileServiceIdentifer, rootVolumeName, owner, userVolumeName, updatedSharing);

        LogUtils.buildLog()
            .forFileService()
            .showInUserHistory()
            .user(up)
            .sentence()
                .subject(up.getUsername())
                .verb("shared")
                .predicate("%s's '%s' volume '%s'", owner, rootVolumeName, userVolumeName)
            .extraField("userVolumeName", userVolumeName)
            .extraField("rootVolumeName", rootVolumeName)
            .extraField("fileServiceIdentifier", fileServiceIdentifer)
            .log();
    }
    
	@GetMapping(value="/fileservice/{fileServiceIdentifer}/rootVolume/{rootVolumeName}/serviceVolume/{owner}/{userVolumeName}/allowedActions")
	public NativeQueryResult queryServiceVolumeActions(@PathVariable String fileServiceIdentifer, @PathVariable String rootVolumeName,
			@PathVariable String owner,
			@PathVariable String userVolumeName, @RequestHeader(value=SERVICE_TOKEN_HEADER, required=true)  String serviceToken, @AuthenticationPrincipal UserProfile up) {
		NativeQueryResult allowedActions = fsManager.getServiceVolumeActions(up, fileServiceIdentifer, serviceToken, rootVolumeName, owner, userVolumeName);
		return allowedActions;
	}

}
