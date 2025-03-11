package org.sciserver.springapp.racm.storem.controller;

import static org.sciserver.springapp.racm.auth.SciServerHeaderAuthenticationFilter.AUTH_HEADER;

import org.sciserver.racm.storem.model.RegisteredFileServiceModel;
import org.sciserver.springapp.racm.utils.controller.ResourceNotFoundException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@FileServiceTokenRequired
@RequestMapping(value="/storem")
public class FileServiceTokenRequiredController {
	@GetMapping(value="/fileservice/{fileServiceIdentifer}",
			headers = "!"+AUTH_HEADER)
	public RegisteredFileServiceModel getDetailsOfFileService(@PathVariable String fileServiceIdentifer,
			RegisteredFileServiceModel fileService) {
		if (!fileService.getIdentifier().equals(fileServiceIdentifer))
			throw new ResourceNotFoundException("Couldn't find identifier");

		return fileService;
	}
}
