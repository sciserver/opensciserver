package org.sciserver.springapp.racm.workspace.controller;

import java.util.Optional;

import org.sciserver.racm.workspace.model.WorkspaceGroupsModel;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.sciserver.springapp.racm.utils.controller.JsonAPIHelper;
import org.sciserver.springapp.racm.workspace.application.GroupResourcesManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

@CrossOrigin
@RestController
@RequestMapping("workspace")
public class WorkspaceController {
	private final GroupResourcesManager workspaceManager;
	public final JsonAPIHelper jsonAPIHelper;

	@Autowired
	WorkspaceController(GroupResourcesManager workspaceManager, JsonAPIHelper jsonAPIHelper) {
		this.workspaceManager = workspaceManager;
		this.jsonAPIHelper = jsonAPIHelper;
	}

	@GetMapping("/groups")
	public ResponseEntity<JsonNode> submitJob(@AuthenticationPrincipal UserProfile up) {
		try {

			WorkspaceGroupsModel wgsm = workspaceManager.getWorkspaceGroups(up);
			return jsonAPIHelper.success(wgsm);
		} catch (Exception e) {
	  		return jsonAPIHelper.logAndReturnJsonExceptionEntity(
	  				"Error retrieving group resources", Optional.of(up), e);
		}
	}
}
