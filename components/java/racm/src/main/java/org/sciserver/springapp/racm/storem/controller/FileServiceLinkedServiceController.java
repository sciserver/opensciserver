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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

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
@Tag(name = "File service linked services",
     description = "Manage volumes a file service exposes to another registered SciServer "
                   + "service.")
@SecurityRequirement(name = "serviceToken")
public class FileServiceLinkedServiceController {

    private final FileServiceManager fsManager;

    @Autowired
    FileServiceLinkedServiceController(FileServiceManager fsService) {
        this.fsManager = fsService;
    }

    @Operation(
        summary = "Create a user volume owned by a service rather than a user.",
        description = "Registers a volume under the root volume whose owner is a resource of the "
                      + "calling service, so that the service manages it on the user's behalf. "
                      + "The registered volume, including the name RACM gave it, is returned.",
        parameters = {
            @Parameter(name = "fileServiceIdentifer", in = ParameterIn.PATH,
                       description = "Name or UUID of the file service holding the root volume."),
            @Parameter(name = "rootVolumeName", in = ParameterIn.PATH,
                       description = "Name of the root volume to create the service volume under. "
                       + "It may be URL-encoded.")
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                       description = "RegisterNewServiceVolumeModel as JSON, naming the volume "
                       + "and the resource that will own it."))
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "400",
                     description = "The request is not valid; the response body carries the "
                                  + "reason."),
        @ApiResponse(responseCode = "401",
                     description = "No user token was supplied, or it is not valid."),
        @ApiResponse(responseCode = "403",
                     description = "The caller may not create a volume under this root volume."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
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
    @Operation(
        summary = "Delete a user volume owned by a service.",
        description = "Removes RACM's record of a volume owned by a resource of the calling "
                      + "service, together with the shares granted over it. The files are not "
                      + "touched.",
        parameters = {
            @Parameter(name = "fileServiceIdentifer", in = ParameterIn.PATH,
                       description = "Name or UUID of the file service holding the volume."),
            @Parameter(name = "rootVolumeName", in = ParameterIn.PATH,
                       description = "Name of the root volume the volume sits under."),
            @Parameter(name = "owner", in = ParameterIn.PATH,
                       description = "Owner of the volume, as recorded when it was registered."),
            @Parameter(name = "userVolumeName", in = ParameterIn.PATH,
                       description = "Name of the volume to delete."),
            @Parameter(name = "X-Service-Token", in = ParameterIn.HEADER,
                       description = "Service token of the service that owns the volume. "
                                     + "Required.")
        })
    @ApiResponses({
        @ApiResponse(responseCode = "204",
                     description = "The volume was deleted."),
        @ApiResponse(responseCode = "401",
                     description = "No user token was supplied, or it is not valid."),
        @ApiResponse(responseCode = "403",
                     description = "The service token does not own this volume."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
    @DeleteMapping("/fileservice/{fileServiceIdentifer}/rootVolume/{rootVolumeName}/serviceVolume/{owner}/{userVolumeName}")
    public void unregisterServiceVolume(@PathVariable String fileServiceIdentifer, @PathVariable String rootVolumeName,
            @PathVariable String owner, @PathVariable String userVolumeName, 
            @RequestHeader(value=SERVICE_TOKEN_HEADER, required=true) String serviceToken, @AuthenticationPrincipal UserProfile up) {
        fsManager.deleteServiceVolume(up, fileServiceIdentifer, rootVolumeName, owner, userVolumeName, serviceToken);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Change who a service-owned volume is shared with.",
        description = "Applies a set of sharing changes to a volume owned by a service, each "
                      + "naming the user or group involved and the actions it gains or loses. "
                      + "Entities the request does not mention keep the access they have.",
        parameters = {
            @Parameter(name = "fileServiceIdentifer", in = ParameterIn.PATH,
                       description = "Name or UUID of the file service holding the volume."),
            @Parameter(name = "rootVolumeName", in = ParameterIn.PATH,
                       description = "Name of the root volume the volume sits under."),
            @Parameter(name = "owner", in = ParameterIn.PATH,
                       description = "Owner of the volume, as recorded when it was registered."),
            @Parameter(name = "userVolumeName", in = ParameterIn.PATH,
                       description = "Name of the volume whose sharing is changed.")
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                       description = "An array of UpdateSharedWithEntry as JSON, each naming an "
                       + "entity and the actions it gains or loses."))
    @ApiResponses({
        @ApiResponse(responseCode = "204",
                     description = "The sharing was updated."),
        @ApiResponse(responseCode = "400",
                     description = "The request is not valid; the response body carries the "
                                  + "reason."),
        @ApiResponse(responseCode = "401",
                     description = "No user token was supplied, or it is not valid."),
        @ApiResponse(responseCode = "403",
                     description = "The caller may not change the sharing of this volume."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
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
    
	@Operation(
	    summary = "List the actions the caller may perform on a service-owned volume.",
	    description = "Returns the actions the calling user is permitted on a volume owned by the "
	                  + "calling service, as a result of columns and rows.",
	    parameters = {
	        @Parameter(name = "fileServiceIdentifer", in = ParameterIn.PATH,
	                   description = "Name or UUID of the file service holding the volume."),
	        @Parameter(name = "rootVolumeName", in = ParameterIn.PATH,
	                   description = "Name of the root volume the volume sits under."),
	        @Parameter(name = "owner", in = ParameterIn.PATH,
	                   description = "Owner of the volume, as recorded when it was registered."),
	        @Parameter(name = "userVolumeName", in = ParameterIn.PATH,
	                   description = "Name of the volume."),
	        @Parameter(name = "X-Service-Token", in = ParameterIn.HEADER,
	                   description = "Service token of the service that owns the volume. "
	                                 + "Required.")
	    })
	@ApiResponses({
	    @ApiResponse(responseCode = "200"),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "403",
	                 description = "The service token does not own this volume."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
	@GetMapping(value="/fileservice/{fileServiceIdentifer}/rootVolume/{rootVolumeName}/serviceVolume/{owner}/{userVolumeName}/allowedActions")
	public NativeQueryResult queryServiceVolumeActions(@PathVariable String fileServiceIdentifer, @PathVariable String rootVolumeName,
			@PathVariable String owner,
			@PathVariable String userVolumeName, @RequestHeader(value=SERVICE_TOKEN_HEADER, required=true)  String serviceToken, @AuthenticationPrincipal UserProfile up) {
		NativeQueryResult allowedActions = fsManager.getServiceVolumeActions(up, fileServiceIdentifer, serviceToken, rootVolumeName, owner, userVolumeName);
		return allowedActions;
	}

}
