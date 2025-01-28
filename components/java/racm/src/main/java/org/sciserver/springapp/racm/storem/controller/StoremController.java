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

@CrossOrigin
@RestController
@RequestMapping(value="/storem")
public class StoremController {
	private final FileServiceManager fsManager;

	@Autowired
	StoremController(FileServiceManager fsService) {
		this.fsManager = fsService;
	}

	@GetMapping
	public StoremModel getStorem(@AuthenticationPrincipal UserProfile up) {
		return new StoremModel(
				fsManager.getFileServiceEndpoints(up));
	}

	@ResponseStatus(value = HttpStatus.CREATED)
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

	@GetMapping("/fileservices")
	public List<MinimalFileServiceModel> getFileServices(@AuthenticationPrincipal UserProfile up) {
		return fsManager.getMinimalFileServices(up);
	}
}