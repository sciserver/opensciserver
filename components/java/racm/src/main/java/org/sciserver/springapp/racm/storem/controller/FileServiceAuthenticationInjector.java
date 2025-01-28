package org.sciserver.springapp.racm.storem.controller;

import static org.sciserver.springapp.racm.auth.SciServerHeaderAuthenticationFilter.SERVICE_TOKEN_HEADER;

import javax.servlet.http.HttpServletRequest;

import org.sciserver.racm.storem.model.RegisteredFileServiceModel;
import org.sciserver.springapp.racm.login.InsufficientPermissionsException;
import org.sciserver.springapp.racm.storem.application.FileServiceManager;
import org.sciserver.springapp.racm.utils.controller.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.util.UrlPathHelper;

@ControllerAdvice(annotations=FileServiceTokenRequired.class)
public class FileServiceAuthenticationInjector {
	private final FileServiceManager fileServiceManager;

	@Autowired
	public FileServiceAuthenticationInjector(FileServiceManager service) {
		this.fileServiceManager = service;
	}

	@ModelAttribute
	public RegisteredFileServiceModel verifyServiceToken(HttpServletRequest request) {
		String fileServiceToken = request.getHeader(SERVICE_TOKEN_HEADER);
		RegisteredFileServiceModel fs;
		try {
			fs = fileServiceManager.getFileServiceFromToken(fileServiceToken);
		} catch (ResourceNotFoundException e) {
			throw new InsufficientPermissionsException(
					"access this API without valid service token");
		}
		String requestedPath = new UrlPathHelper().getPathWithinApplication(request);
		if (!requestedPath.startsWith("/storem/fileservice/" + fs.getIdentifier()))
			throw new InsufficientPermissionsException(
					"access this API without valid service token");
		return fs;
	}
}
