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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@CrossOrigin
@RestController
@FileServiceTokenRequired
@RequestMapping(value = "/storem")
@Tag(name = "File service contents",
     description = "Inspect and manage the volumes of one file service. Called by a FileService "
                 + "instance on behalf of a user.")
@SecurityRequirement(name = "serviceToken")
@SecurityRequirement(name = "userToken")
public class FileServiceUserRequiredController {
    private final FileServiceManager fsManager;

    @Autowired
    FileServiceUserRequiredController(FileServiceManager fsService) {
        this.fsManager = fsService;
    }

    @Operation(
        summary = "Get a file service and its volumes, by the slower original query.",
        description = "Returns the file service with the root, data and user volumes the calling "
                      + "user may see. Superseded by the identical endpoint without the /OLD "
                      + "suffix, which answers the same question faster; this one is kept for "
                      + "comparison.",
        parameters = {
            @Parameter(name = "fileServiceIdentifer", in = ParameterIn.PATH,
                       description = "Name or UUID of the file service.")
        })
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "401",
                     description = "No user token was supplied, or it is not valid."),
        @ApiResponse(responseCode = "403",
                     description = "The caller may not see this file service."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
    @GetMapping(value = "/fileservice/{fileServiceIdentifer}/OLD", headers = AUTH_HEADER)
    public FileServiceModel getDetailsOfFileService(@PathVariable String fileServiceIdentifer,
            @AuthenticationPrincipal UserProfile up) {
        return fsManager.getFileService(up, fileServiceIdentifer);
    }

    @Operation(
        summary = "Get a file service and the volumes the caller may use.",
        description = "Returns the file service with its root volumes, data volumes and user "
                      + "volumes, each carrying the actions the calling user is allowed to "
                      + "perform on it. This is the endpoint a FileService client uses to "
                      + "discover what to offer the user. Called without a user token but with a "
                      + "file service's own service token, the same path returns that file "
                      + "service's registration instead, for a FileService instance discovering "
                      + "its own configuration.",
        parameters = {
            @Parameter(name = "fileServiceIdentifer", in = ParameterIn.PATH,
                       description = "Name or UUID of the file service.")
        })
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "401",
                     description = "No user token was supplied, or it is not valid."),
        @ApiResponse(responseCode = "403",
                     description = "The caller may not see this file service."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
    @GetMapping(value = "/fileservice/{fileServiceIdentifer}", headers = AUTH_HEADER)
    public FileServiceModel getDetailsOfFileServiceFAST(@PathVariable String fileServiceIdentifer,
            @AuthenticationPrincipal UserProfile up) {
        return fsManager.getFileServiceFAST(up, fileServiceIdentifer);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Unregister a file service.",
        description = "Removes the file service from RACM, together with the volumes registered "
                      + "on it and the rights granted over them. The files themselves are not "
                      + "touched; this removes only RACM's record of them.",
        parameters = {
            @Parameter(name = "fileServiceIdentifer", in = ParameterIn.PATH,
                       description = "Name or UUID of the file service to unregister.")
        })
    @ApiResponses({
        @ApiResponse(responseCode = "204",
                     description = "The file service was unregistered."),
        @ApiResponse(responseCode = "401",
                     description = "No user token was supplied, or it is not valid."),
        @ApiResponse(responseCode = "403",
                     description = "The caller may not unregister this file service."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
    @DeleteMapping("/fileservice/{fileServiceIdentifer}")
    public void unregisterFileService(@PathVariable String fileServiceIdentifer,
            @AuthenticationPrincipal UserProfile up) {
        fsManager.deleteFileService(up, fileServiceIdentifer);
    }

    @Operation(
        summary = "List the actions the caller may perform on a root volume.",
        description = "Returns the names of the actions the calling user is permitted on the root "
                      + "volume, so that a client can decide what to offer.",
        parameters = {
            @Parameter(name = "fileServiceIdentifer", in = ParameterIn.PATH,
                       description = "Name or UUID of the file service holding the root volume."),
            @Parameter(name = "rootVolumeName", in = ParameterIn.PATH,
                       description = "Name of the root volume.")
        })
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "401",
                     description = "No user token was supplied, or it is not valid."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
    @GetMapping(value = "/fileservice/{fileServiceIdentifer}/rootVolume/{rootVolumeName}/allowedActions")
    public Set<String> queryRootVolumeActions(@PathVariable String fileServiceIdentifer,
            @PathVariable String rootVolumeName, @AuthenticationPrincipal UserProfile up) {

        Set<String> allowedActions = fsManager.getRootVolumeActions(up, fileServiceIdentifer, rootVolumeName);
        if (allowedActions.isEmpty())
            throw new ResourceNotFoundException();
        else
            return allowedActions;
    }

    @Operation(
        summary = "List the actions the caller may perform on a data volume.",
        description = "Returns the names of the actions the calling user is permitted on the data "
                      + "volume, so that a client can decide what to offer.",
        parameters = {
            @Parameter(name = "fileServiceIdentifer", in = ParameterIn.PATH,
                       description = "Name or UUID of the file service holding the data volume."),
            @Parameter(name = "dataVolumeName", in = ParameterIn.PATH,
                       description = "Name of the data volume.")
        })
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "401",
                     description = "No user token was supplied, or it is not valid."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
    @GetMapping(value = "/fileservice/{fileServiceIdentifer}/dataVolume/{dataVolumeName}/allowedActions")
    public NativeQueryResult queryDataVolumeActions(@PathVariable String fileServiceIdentifer,
            @PathVariable String dataVolumeName, @AuthenticationPrincipal UserProfile up) {

        NativeQueryResult allowedActions = fsManager.getDataVolumeActions(up, fileServiceIdentifer, dataVolumeName);
        return allowedActions;
    }

    @Operation(
        summary = "List the actions the caller may perform on a user volume.",
        description = "Returns the names of the actions the calling user is permitted on the user "
                      + "volume, which may be one they own or one shared with them.",
        parameters = {
            @Parameter(name = "fileServiceIdentifer", in = ParameterIn.PATH,
                       description = "Name or UUID of the file service holding the volume."),
            @Parameter(name = "rootVolumeName", in = ParameterIn.PATH,
                       description = "Name of the root volume the user volume sits under."),
            @Parameter(name = "owner", in = ParameterIn.PATH,
                       description = "Username of the user who owns the user volume."),
            @Parameter(name = "userVolumeName", in = ParameterIn.PATH,
                       description = "Name of the user volume.")
        })
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "401",
                     description = "No user token was supplied, or it is not valid."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
    @GetMapping(value = "/fileservice/{fileServiceIdentifer}/rootVolume/{rootVolumeName}/userVolume/{owner}/{userVolumeName}/allowedActions")
    public NativeQueryResult queryUserVolumeActions(@PathVariable String fileServiceIdentifer,
            @PathVariable String rootVolumeName, @PathVariable String owner, @PathVariable String userVolumeName,
            @AuthenticationPrincipal UserProfile up) {

        NativeQueryResult allowedActions = fsManager.getUserVolumeActions(up, fileServiceIdentifer, rootVolumeName,
                owner, userVolumeName);
        return allowedActions;
    }

    @Operation(
        summary = "Register a root volume on a file service.",
        description = "Adds a root volume, the top-level storage area under which user volumes "
                      + "are created. Registering it in RACM is what makes it available to users; "
                      + "nothing is created on disk.",
        parameters = {
            @Parameter(name = "fileServiceIdentifer", in = ParameterIn.PATH,
                       description = "Name or UUID of the file service to register the root "
                                     + "volume on.")
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                       description = "RegisterNewRootVolumeModel as JSON, naming the root volume "
                       + "and the path it maps to."))
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "400",
                     description = "The request is not valid; the response body carries the "
                                  + "reason."),
        @ApiResponse(responseCode = "401",
                     description = "No user token was supplied, or it is not valid."),
        @ApiResponse(responseCode = "403",
                     description = "The caller may not register a root volume on this file "
                                  + "service."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
    @PostMapping("/fileservice/{fileServiceIdentifer}/rootVolumes")
    public ResponseEntity<Void> newRootVolume(@PathVariable String fileServiceIdentifer,
            @RequestBody RegisterNewRootVolumeModel rootVolume, HttpServletResponse response,
            @AuthenticationPrincipal UserProfile up) {

        RegisteredRootVolumeModel newRootVolume = fsManager.registerRootVolume(up, fileServiceIdentifer, rootVolume);
        return ResponseEntity.created(MvcUriComponentsBuilder
                .fromMethodName(getClass(), "unregisterRootVolume", fileServiceIdentifer, newRootVolume.getName(), null)
                .build().toUri()).build();
    }

    @Operation(
        summary = "Register a data volume on a file service.",
        description = "Adds a data volume, a published storage area that is shared with users "
                      + "rather than owned by one. Registering it in RACM is what makes it "
                      + "available; nothing is created on disk.",
        parameters = {
            @Parameter(name = "fileServiceIdentifer", in = ParameterIn.PATH,
                       description = "Name or UUID of the file service to register the data "
                                     + "volume on.")
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                       description = "RegisterNewDataVolumeModel as JSON, naming the data volume "
                       + "and the path it maps to."))
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "400",
                     description = "The request is not valid; the response body carries the "
                                  + "reason."),
        @ApiResponse(responseCode = "401",
                     description = "No user token was supplied, or it is not valid."),
        @ApiResponse(responseCode = "403",
                     description = "The caller may not register a data volume on this file "
                                  + "service."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
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
    @Operation(
        summary = "Unregister a data volume.",
        description = "Removes RACM's record of the data volume and the rights granted over it. "
                      + "The files are not touched.",
        parameters = {
            @Parameter(name = "fileServiceIdentifer", in = ParameterIn.PATH,
                       description = "Name or UUID of the file service holding the data volume."),
            @Parameter(name = "dataVolumeName", in = ParameterIn.PATH,
                       description = "Name of the data volume to unregister.")
        })
    @ApiResponses({
        @ApiResponse(responseCode = "204",
                     description = "The data volume was unregistered."),
        @ApiResponse(responseCode = "401",
                     description = "No user token was supplied, or it is not valid."),
        @ApiResponse(responseCode = "403",
                     description = "The caller may not unregister this data volume."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
    @DeleteMapping("/fileservice/{fileServiceIdentifer}/dataVolume/{dataVolumeName}")
    public void unregisterDataVolume(@PathVariable String fileServiceIdentifer, @PathVariable String dataVolumeName,
            @AuthenticationPrincipal UserProfile up) {
        String decodedDataName = UriUtils.decode(dataVolumeName, "UTF-8");
        fsManager.deleteDataVolume(up, fileServiceIdentifer, decodedDataName);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Unregister a root volume.",
        description = "Removes RACM's record of the root volume, together with the user volumes "
                      + "under it and the rights granted over them. The files are not touched.",
        parameters = {
            @Parameter(name = "fileServiceIdentifer", in = ParameterIn.PATH,
                       description = "Name or UUID of the file service holding the root volume."),
            @Parameter(name = "rootVolumeName", in = ParameterIn.PATH,
                       description = "Name of the root volume to unregister.")
        })
    @ApiResponses({
        @ApiResponse(responseCode = "204",
                     description = "The root volume was unregistered."),
        @ApiResponse(responseCode = "401",
                     description = "No user token was supplied, or it is not valid."),
        @ApiResponse(responseCode = "403",
                     description = "The caller may not unregister this root volume."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
    @DeleteMapping("/fileservice/{fileServiceIdentifer}/rootVolume/{rootVolumeName}")
    public void unregisterRootVolume(@PathVariable String fileServiceIdentifer, @PathVariable String rootVolumeName,
            @AuthenticationPrincipal UserProfile up) {
        fsManager.deleteRootVolume(up, fileServiceIdentifer, rootVolumeName);
    }

    @Operation(
        summary = "Create a user volume under a root volume.",
        description = "Registers a user volume owned by the calling user. On success the Location "
                      + "header of the response carries the URL at which that volume can be "
                      + "deleted again.",
        parameters = {
            @Parameter(name = "fileServiceIdentifer", in = ParameterIn.PATH,
                       description = "Name or UUID of the file service holding the root volume."),
            @Parameter(name = "rootVolumeName", in = ParameterIn.PATH,
                       description = "Name of the root volume to create the user volume under. It "
                       + "may be URL-encoded.")
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                       description = "RegisterNewUserVolumeModel as JSON, naming the user volume "
                       + "and describing it."))
    @ApiResponses({
        @ApiResponse(responseCode = "201",
                     description = "The user volume was created; the Location header points at "
                                   + "it."),
        @ApiResponse(responseCode = "400",
                     description = "The request is not valid; the response body carries the "
                                  + "reason."),
        @ApiResponse(responseCode = "401",
                     description = "No user token was supplied, or it is not valid."),
        @ApiResponse(responseCode = "403",
                     description = "The caller may not create a user volume under this root "
                                  + "volume."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
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
    @Operation(
        summary = "Delete a user volume.",
        description = "Removes RACM's record of the user volume and the shares granted over it. "
                      + "The files are not touched.",
        parameters = {
            @Parameter(name = "fileServiceIdentifer", in = ParameterIn.PATH,
                       description = "Name or UUID of the file service holding the volume."),
            @Parameter(name = "rootVolumeName", in = ParameterIn.PATH,
                       description = "Name of the root volume the user volume sits under."),
            @Parameter(name = "owner", in = ParameterIn.PATH,
                       description = "Username of the user who owns the user volume."),
            @Parameter(name = "userVolumeName", in = ParameterIn.PATH,
                       description = "Name of the user volume to delete.")
        })
    @ApiResponses({
        @ApiResponse(responseCode = "204",
                     description = "The user volume was deleted."),
        @ApiResponse(responseCode = "401",
                     description = "No user token was supplied, or it is not valid."),
        @ApiResponse(responseCode = "403",
                     description = "The caller may not delete this user volume."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
    @DeleteMapping("/fileservice/{fileServiceIdentifer}/rootVolume/{rootVolumeName}/userVolume/{owner}/{userVolumeName}")
    public void unregisterUserVolume(@PathVariable String fileServiceIdentifer, @PathVariable String rootVolumeName,
            @PathVariable String owner, @PathVariable String userVolumeName, @AuthenticationPrincipal UserProfile up) {
        fsManager.deleteUserVolume(up, fileServiceIdentifer, rootVolumeName, owner, userVolumeName);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Change a user volume's details.",
        description = "Applies a partial update to the user volume: only the fields present in "
                      + "the request are changed. Sharing is changed through the sharedWith "
                      + "endpoint instead.",
        parameters = {
            @Parameter(name = "fileServiceIdentifer", in = ParameterIn.PATH,
                       description = "Name or UUID of the file service holding the volume."),
            @Parameter(name = "rootVolumeName", in = ParameterIn.PATH,
                       description = "Name of the root volume the user volume sits under."),
            @Parameter(name = "owner", in = ParameterIn.PATH,
                       description = "Username of the user who owns the user volume."),
            @Parameter(name = "userVolumeName", in = ParameterIn.PATH,
                       description = "Name of the user volume to update.")
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                       description = "UpdatedUserVolumeInfo as JSON, carrying only the fields to "
                                     + "change."))
    @ApiResponses({
        @ApiResponse(responseCode = "204",
                     description = "The user volume was updated."),
        @ApiResponse(responseCode = "400",
                     description = "The request is not valid; the response body carries the "
                                  + "reason."),
        @ApiResponse(responseCode = "401",
                     description = "No user token was supplied, or it is not valid."),
        @ApiResponse(responseCode = "403",
                     description = "The caller may not modify this user volume."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
    @PatchMapping("/fileservice/{fileServiceIdentifer}/rootVolume/{rootVolumeName}/userVolume/{owner}/{userVolumeName}")
    public void editUserVolume(@PathVariable String fileServiceIdentifer, @PathVariable String rootVolumeName,
            @PathVariable String owner, @PathVariable String userVolumeName,
            @RequestBody UpdatedUserVolumeInfo updatedUserVolumeInfo, @AuthenticationPrincipal UserProfile up) {
        fsManager.updateUserVolume(up, fileServiceIdentifer, rootVolumeName, owner, userVolumeName,
                updatedUserVolumeInfo);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Change a root volume's details.",
        description = "Applies a partial update to the root volume: only the fields present in "
                      + "the request are changed.",
        parameters = {
            @Parameter(name = "fileServiceIdentifier", in = ParameterIn.PATH,
                       description = "Name or UUID of the file service holding the root volume."),
            @Parameter(name = "rootVolumeName", in = ParameterIn.PATH,
                       description = "Name of the root volume to update.")
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                       description = "UpdatedRootVolumeInfo as JSON, carrying only the fields to "
                                     + "change."))
    @ApiResponses({
        @ApiResponse(responseCode = "204",
                     description = "The root volume was updated."),
        @ApiResponse(responseCode = "400",
                     description = "The request is not valid; the response body carries the "
                                  + "reason."),
        @ApiResponse(responseCode = "401",
                     description = "No user token was supplied, or it is not valid."),
        @ApiResponse(responseCode = "403",
                     description = "The caller may not modify this root volume."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
    @PatchMapping("/fileservice/{fileServiceIdentifier}/rootVolume/{rootVolumeName}")
    public void editRootVolume(@PathVariable String fileServiceIdentifier, @PathVariable String rootVolumeName,
            @RequestBody UpdatedRootVolumeInfo updatedRootVolumeInfo, @AuthenticationPrincipal UserProfile up) {
        fsManager.updateRootVolume(up, fileServiceIdentifier, rootVolumeName, updatedRootVolumeInfo);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Change a data volume's details.",
        description = "Applies a partial update to the data volume: only the fields present in "
                      + "the request are changed. Sharing is changed through the sharedWith "
                      + "endpoint instead.",
        parameters = {
            @Parameter(name = "fileServiceIdentifier", in = ParameterIn.PATH,
                       description = "Name or UUID of the file service holding the data volume."),
            @Parameter(name = "dataVolumeName", in = ParameterIn.PATH,
                       description = "Name of the data volume to update.")
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                       description = "UpdatedDataVolumeInfo as JSON, carrying only the fields to "
                                     + "change."))
    @ApiResponses({
        @ApiResponse(responseCode = "204",
                     description = "The data volume was updated."),
        @ApiResponse(responseCode = "400",
                     description = "The request is not valid; the response body carries the "
                                  + "reason."),
        @ApiResponse(responseCode = "401",
                     description = "No user token was supplied, or it is not valid."),
        @ApiResponse(responseCode = "403",
                     description = "The caller may not modify this data volume."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
    @PatchMapping("/fileservice/{fileServiceIdentifier}/dataVolume/{dataVolumeName}")
    public void editDataVolume(@PathVariable String fileServiceIdentifier, @PathVariable String dataVolumeName,
            @RequestBody UpdatedDataVolumeInfo updatedDataVolumeInfo, @AuthenticationPrincipal UserProfile up) {
        fsManager.updateDataVolume(up, fileServiceIdentifier, dataVolumeName, updatedDataVolumeInfo);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Change a file service's details.",
        description = "Applies a partial update to the file service registration, such as its "
                      + "name or the URL at which clients reach it. Only the fields present in "
                      + "the request are changed.",
        parameters = {
            @Parameter(name = "fileServiceIdentifier", in = ParameterIn.PATH,
                       description = "Name or UUID of the file service to update.")
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                       description = "UpdatedFileServiceInfo as JSON, carrying only the fields "
                                     + "to change."))
    @ApiResponses({
        @ApiResponse(responseCode = "204",
                     description = "The file service was updated."),
        @ApiResponse(responseCode = "400",
                     description = "The request is not valid; the response body carries the "
                                  + "reason."),
        @ApiResponse(responseCode = "401",
                     description = "No user token was supplied, or it is not valid."),
        @ApiResponse(responseCode = "403",
                     description = "The caller may not modify this file service."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
    @PatchMapping("/fileservice/{fileServiceIdentifier}")
    public void editFileService(@PathVariable String fileServiceIdentifier,
            @RequestBody UpdatedFileServiceInfo updatedFileServiceInfo, @AuthenticationPrincipal UserProfile up) {
        fsManager.updateFileService(up, fileServiceIdentifier, updatedFileServiceInfo);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Change who a user volume is shared with.",
        description = "Applies a set of sharing changes to the user volume, each naming the user "
                      + "or group involved and the actions it gains or loses. Entities the "
                      + "request does not mention keep the access they have.",
        parameters = {
            @Parameter(name = "fileServiceIdentifer", in = ParameterIn.PATH,
                       description = "Name or UUID of the file service holding the volume."),
            @Parameter(name = "rootVolumeName", in = ParameterIn.PATH,
                       description = "Name of the root volume the user volume sits under."),
            @Parameter(name = "owner", in = ParameterIn.PATH,
                       description = "Username of the user who owns the user volume."),
            @Parameter(name = "userVolumeName", in = ParameterIn.PATH,
                       description = "Name of the user volume whose sharing is changed.")
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
                     description = "The caller may not change the sharing of this user volume."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
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
    @Operation(
        summary = "Give up the caller's own share of someone else's user volume.",
        description = "Removes the shares the calling user holds on a user volume owned by "
                      + "another user, so that it no longer appears among their volumes. The "
                      + "owner keeps the volume and is otherwise unaffected. The caller must not "
                      + "be the owner; an owner deletes the volume instead.",
        parameters = {
            @Parameter(name = "fileServiceIdentifer", in = ParameterIn.PATH,
                       description = "Name or UUID of the file service holding the volume."),
            @Parameter(name = "rootVolumeName", in = ParameterIn.PATH,
                       description = "Name of the root volume the user volume sits under."),
            @Parameter(name = "owner", in = ParameterIn.PATH,
                       description = "Username of the user who owns the user volume. It must not "
                       + "be the calling user."),
            @Parameter(name = "userVolumeName", in = ParameterIn.PATH,
                       description = "Name of the user volume the caller is giving up.")
        })
    @ApiResponses({
        @ApiResponse(responseCode = "204",
                     description = "The caller's shares were removed."),
        @ApiResponse(responseCode = "400",
                     description = "The request is not valid; the response body carries the "
                                  + "reason."),
        @ApiResponse(responseCode = "401",
                     description = "No user token was supplied, or it is not valid."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
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
    @Operation(
        summary = "Change who a data volume is shared with.",
        description = "Applies a set of sharing changes to the data volume, each naming the user "
                      + "or group involved and the actions it gains or loses. Entities the "
                      + "request does not mention keep the access they have.",
        parameters = {
            @Parameter(name = "fileServiceIdentifer", in = ParameterIn.PATH,
                       description = "Name or UUID of the file service holding the data volume."),
            @Parameter(name = "dataVolumeName", in = ParameterIn.PATH,
                       description = "Name of the data volume whose sharing is changed.")
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
                     description = "The caller may not change the sharing of this data volume."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
    @PatchMapping("/fileservice/{fileServiceIdentifer}/dataVolume/{dataVolumeName}/sharedWith")
    public void shareDataVolume(@PathVariable String fileServiceIdentifer, @PathVariable String dataVolumeName,
            @AuthenticationPrincipal UserProfile up, @RequestBody UpdateSharedWithEntry... updatedSharing) {
        fsManager.updateSharingOfDataVolumes(up, fileServiceIdentifer, dataVolumeName, updatedSharing);

        LogUtils.buildLog().forFileService().showInUserHistory().user(up).sentence().subject(up.getUsername())
                .verb("shared").predicate("data volume '%s'", dataVolumeName)
                .extraField("fileServiceIdentifier", fileServiceIdentifer).log();
    }
}