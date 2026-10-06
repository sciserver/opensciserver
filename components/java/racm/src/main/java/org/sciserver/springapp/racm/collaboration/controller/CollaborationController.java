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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@CrossOrigin
@RestController
@RequestMapping("/collaborations")
@Tag(name = "Collaborations", description = "Query the collaborations the calling user belongs to.")
public class CollaborationController {
	private final CollaborationManager collaborationManager;
	private final JsonAPIHelper jsonAPIHelper;

	@Autowired
	CollaborationController(CollaborationManager collaborationManager,
			JsonAPIHelper jsonAPIHelper) {
		this.collaborationManager = collaborationManager;
		this.jsonAPIHelper = jsonAPIHelper;
	}

	@Operation(
	    summary = "List the collaborations the caller belongs to.",
	    description = "Returns the collaborations of which the calling user is a member, with the "
	                  + "other members of each.")
	@ApiResponses({
	    @ApiResponse(responseCode = "200"),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
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
