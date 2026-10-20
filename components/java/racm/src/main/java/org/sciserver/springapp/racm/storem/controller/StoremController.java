package org.sciserver.springapp.racm.storem.controller;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.sciserver.racm.storem.model.MinimalFileServiceModel;
import org.sciserver.racm.storem.model.RegisterNewFileServiceModel;
import org.sciserver.racm.storem.model.RegisteredFileServiceModel;
import org.sciserver.racm.storem.model.StoremModel;
import org.sciserver.springapp.racm.storem.application.FileServiceManager;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.sciserver.springapp.racm.utils.logging.LogUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@CrossOrigin
@RestController
@RequestMapping(value="/storem")
@Tag(name = "File services",
     description = "List and register the file services known to RACM.")
public class StoremController {
	private final FileServiceManager fsManager;

	@Autowired
	StoremController(FileServiceManager fsService) {
		this.fsManager = fsService;
	}

	@Operation(
	    summary = "List the endpoints of the file services known to RACM.",
	    description = "Returns the API endpoints of the registered file services, so a client can "
	                  + "discover where to send file operations.")
	@ApiResponses({
	    @ApiResponse(responseCode = "200",
	                 description = "The API endpoints of the registered file services."),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
	@GetMapping
	public StoremModel getStorem(@AuthenticationPrincipal UserProfile up) {
		return new StoremModel(
				fsManager.getFileServiceEndpoints(up));
	}

	@ResponseStatus(value = HttpStatus.CREATED)
	@Operation(
	    summary = "Register a file service.",
	    description = "Registers a new file service with RACM, creating the resource context that "
	                  + "will hold its root volumes and user volumes. Returns the registration, "
	                  + "including the identifier and service token the file service must use "
	                  + "thereafter.",
	    requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
	                   description = "RegisterNewFileServiceModel as JSON, naming the service "
                    + "and its API endpoint."))
	@ApiResponses({
	    @ApiResponse(responseCode = "201",
	                 description = "The file service was registered."),
	    @ApiResponse(responseCode = "400",
	                 description = "The request is not valid; the response body carries the "
	                               + "reason."),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "409",
	                 description = "A file service with that identifier is already registered."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
	@PostMapping("/fileservices")
	public RegisteredFileServiceModel newFileService(@RequestBody RegisterNewFileServiceModel fileService,
			HttpServletResponse response, @AuthenticationPrincipal UserProfile up) {

		RegisteredFileServiceModel newFileService = fsManager.registerFileService(up, fileService);
		String location = MvcUriComponentsBuilder
				.fromMethodName(FileServiceUserRequiredController.class, "getDetailsOfFileService", newFileService.getIdentifier(), null)
				.buildAndExpand().encode().toUriString();
		response.setHeader("Location", location);
		LogUtils.buildLog()
			.forFileService()
			.showInUserHistory()
			.user(up)
			.sentence()
				.subject(up.getUsername())
				.verb("registered")
				.predicate("file service '%s'", fileService.getName())
			.extraField("fileServiceIdentifier", newFileService.getIdentifier())
			.log();
		return newFileService;
	}

	@Operation(
	    summary = "List the file services known to RACM.",
	    description = "Returns each registered file service with its identifier, name, "
	                  + "description and API endpoint. The identifier is the resource context "
	                  + "uuid used in the per-file-service endpoints.")
	@ApiResponses({
	    @ApiResponse(responseCode = "200",
	                 description = "Each registered file service, with its identifier, name, description and API "
	                               + "endpoint."),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
	@GetMapping("/fileservices")
	public List<MinimalFileServiceModel> getFileServices(@AuthenticationPrincipal UserProfile up) {
		return fsManager.getMinimalFileServices(up);
	}
}