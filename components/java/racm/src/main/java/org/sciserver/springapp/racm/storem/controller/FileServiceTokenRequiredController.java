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
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;

@CrossOrigin
@RestController
@FileServiceTokenRequired
@RequestMapping(value="/storem")
@Tag(name = "File service self-registration",
     description = "Called by a FileService instance to retrieve its own registration.")
@SecurityRequirement(name = "serviceToken")
public class FileServiceTokenRequiredController {
	/**
	 * Hidden from the API document, and not by preference: OpenAPI keys an operation by path
	 * and method, and this handler shares both with getDetailsOfFileServiceFAST, which answers
	 * the same request when a user token is present. Two entries cannot coexist there, and
	 * springdoc fails the whole document rather than one path when asked to build them. The
	 * behaviour described here is documented on that operation instead.
	 */
	@Hidden
	@GetMapping(value="/fileservice/{fileServiceIdentifer}",
			headers = "!"+AUTH_HEADER)
	public RegisteredFileServiceModel getDetailsOfFileService(@PathVariable String fileServiceIdentifer,
			RegisteredFileServiceModel fileService) {
		if (!fileService.getIdentifier().equals(fileServiceIdentifer))
			throw new ResourceNotFoundException("Couldn't find identifier");

		return fileService;
	}
}
