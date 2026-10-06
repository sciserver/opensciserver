package org.sciserver.springapp.racm.storem.controller;

import static org.sciserver.springapp.racm.auth.SciServerHeaderAuthenticationFilter.AUTH_HEADER;

import org.sciserver.racm.storem.model.RegisteredFileServiceModel;
import org.sciserver.springapp.racm.utils.controller.ResourceNotFoundException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
@RequestMapping(value="/storem")
@Tag(name = "File service self-registration",
     description = "Called by a FileService instance to retrieve its own registration.")
@SecurityRequirement(name = "serviceToken")
public class FileServiceTokenRequiredController {
	@Operation(
	    summary = "Get a file service's own registration, as that file service.",
	    description = "Returns the registration RACM holds for the file service the request's "
	                  + "service token identifies, including its root and data volumes. It is "
	                  + "served only when no user token is present: with one, the user-facing "
	                  + "endpoint at the same path answers instead. The identifier in the path "
	                  + "must be the file service's own, otherwise the registration is not found.",
	    parameters = {
	        @Parameter(name = "fileServiceIdentifer", in = ParameterIn.PATH,
	                   description = "Identifier of the file service. It must match the one the "
	                   + "service token identifies.")
	    })
	@ApiResponses({
	    @ApiResponse(responseCode = "200"),
	    @ApiResponse(responseCode = "401",
	                 description = "No service token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "404",
	                 description = "The identifier in the path is not the one the service token "
	                              + "identifies."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
	@GetMapping(value="/fileservice/{fileServiceIdentifer}",
			headers = "!"+AUTH_HEADER)
	public RegisteredFileServiceModel getDetailsOfFileService(@PathVariable String fileServiceIdentifer,
			RegisteredFileServiceModel fileService) {
		if (!fileService.getIdentifier().equals(fileServiceIdentifer))
			throw new ResourceNotFoundException("Couldn't find identifier");

		return fileService;
	}
}
