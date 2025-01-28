package org.sciserver.springapp.racm.collaboration.controller;

import java.util.Optional;

import org.sciserver.springapp.racm.collaboration.application.CollaborationManager;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.sciserver.springapp.racm.utils.controller.JsonAPIHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping("/collaborations")
public class CollaborationController {
	private final CollaborationManager collaborationManager;
	private final JsonAPIHelper jsonAPIHelper;

	@Autowired
	CollaborationController(CollaborationManager collaborationManager,
			JsonAPIHelper jsonAPIHelper) {
		this.collaborationManager = collaborationManager;
		this.jsonAPIHelper = jsonAPIHelper;
	}

	@GetMapping
	public ResponseEntity<?> getCollaborations(@AuthenticationPrincipal UserProfile up) {
		try {
			return ResponseEntity.ok(collaborationManager.getCollaborations(up));
		} catch(Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity(
					"unable to retrieve collaborations",
					Optional.of(up), e);
		}
	}
}
