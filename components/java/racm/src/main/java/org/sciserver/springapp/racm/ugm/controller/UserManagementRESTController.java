package org.sciserver.springapp.racm.ugm.controller;

import static java.util.stream.Collectors.toList;
import static org.sciserver.springapp.racm.auth.SciServerHeaderAuthenticationFilter.SERVICE_TOKEN_HEADER;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.ivoa.dm.VOURPException;
import org.sciserver.racm.resourcecontext.model.AssociatedSciserverEntityModel;
import org.sciserver.racm.resourcecontext.model.AssociatedSciserverEntityModel.EntityType;
import org.sciserver.racm.resourcecontext.model.RegisteredResourceModel;
import org.sciserver.racm.ugm.model.CreateLinkedGroupModel;
import org.sciserver.racm.ugm.model.GroupInfo;
import org.sciserver.racm.ugm.model.PersonalUserInfo;
import org.sciserver.racm.ugm.model.PublicGroupModel;
import org.sciserver.racm.ugm.model.SciEntityOwningResource;
import org.sciserver.racm.ugm.model.SciServerEntities;
import org.sciserver.racm.ugm.model.UpdateGroupInfo;
import org.sciserver.racm.ugm.model.UserInfo;
import org.sciserver.racm.utils.model.NativeQueryResult;
import org.sciserver.springapp.racm.ugm.application.UsersAndGroupsManager;
import org.sciserver.springapp.racm.ugm.application.UsersAndGroupsManager.SharedResourceResult;
import org.sciserver.springapp.racm.ugm.domain.ActionsOnResource;
import org.sciserver.springapp.racm.ugm.domain.UGMModelsMapper;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.sciserver.springapp.racm.utils.RACMNames;
import org.sciserver.springapp.racm.utils.RACMUtil;
import org.sciserver.springapp.racm.utils.controller.RACMController;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import edu.jhu.rac.AssociatedSciEntity;
import edu.jhu.rac.OwnershipCategory;
import edu.jhu.rac.Resource;
import edu.jhu.rac.ResourceContext;
import edu.jhu.user.ServiceAccount;
import edu.jhu.user.User;
import edu.jhu.user.UserGroup;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@CrossOrigin
@RequestMapping("/ugm/rest")
@Tag(name = "Users and groups",
     description = "Manage the calling user's profile, their groups, and the resources shared "
                 + "with those groups.")
public class UserManagementRESTController extends RACMController {
	private static final String LOGGING_VERB_GROUP_UPDATED = "updated";
	private static final String LOGGING_VERB_GROUP_CREATED = "created";
	private static final String LOGGING_FIELD_GROUP_ID = "group";
	private static final String LOGGING_PREDICATE_GROUP_NAME = "group '%s'";

	private final ObjectMapper om;
	private final UsersAndGroupsManager usersAndGroupsManager;

	@PersistenceContext
	private EntityManager em;

	@Autowired
	public UserManagementRESTController(UsersAndGroupsManager usersAndGroupsManager) {
		this.om = RACMUtil.newObjectMapper();
		this.usersAndGroupsManager = usersAndGroupsManager;
	}


	/**
	 * Add a resource that owns the specified group.
	 * Checks that indeed the  
	 * @param gi
	 * @param r
	 */
	private static void addOwningResource(GroupInfo gi, Resource r) {
		if( r != null)
		{
			for(AssociatedSciEntity ae: r.getAssociatedGroup()) {
				if(ae.getSciEntity().getId().equals(gi.getId())
						&& ae.getOwnership() == OwnershipCategory.OWNED) {
					RegisteredResourceModel rrm = new RegisteredResourceModel(
							r.getId(), r.getPublisherDID(), r.getUuid()
							, r.getName(), r.getDescription(), r.getResourceType().getName());
					AssociatedSciserverEntityModel asem = new AssociatedSciserverEntityModel(ae.getUsage(), ae.getOwnership() == OwnershipCategory.OWNED, gi.getId(), EntityType.GROUP);
					gi.setOwningResource(new SciEntityOwningResource(rrm,asem));
					break;
				}
			}
		}
	}
	@Operation(
	    summary = "List the groups the caller owns.",
	    description = "Returns groups whose owner is the calling user, with their members and the "
	                  + "resources shared with them.")
	@ApiResponses({
	    @ApiResponse(responseCode = "200"),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
	@GetMapping("/mygroups")
	public ResponseEntity<JsonNode> queryMyGroups(@AuthenticationPrincipal UserProfile up) {
		try {
			List<UserGroup> gs = usersAndGroupsManager.queryEditableGroups(up);
			Hashtable<Long,Resource> rogs = usersAndGroupsManager.queryOwnedEditableGroups(up);
			List<GroupInfo> gis = new ArrayList<>();
			for (UserGroup g : gs) {
				GroupInfo gi = UGMModelsMapper.map(g);
				addOwningResource(gi, rogs.get(gi.getId()));
				gis.add(gi);
			}
			return jsonAPIHelper.success(gis);
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity("Error retrieving list of editable groups",
					Optional.of(up), e);
		}
	}

	/**
	 * If the token identifies the owner, return the group specified by the
	 * groupid.<br/>
	 *
	 * @param groupid
	 * @param request
	 * @param response
	 * @return
	 * @throws VOURPException
	 */
	@Operation(
	    summary = "Get one group the caller owns.",
	    description = "Returns the group with its members, invitations and shared resources. The "
	                  + "caller must own the group, or supply the service token of the resource "
	                  + "context that owns it. Also served at /mygroups/{groupid}.",
	    parameters = {
	        @Parameter(name = "groupid", in = ParameterIn.PATH,
	                   description = "Identifier of the group."),
	        @Parameter(name = "X-Service-Auth-ID", in = ParameterIn.QUERY,
	                   description = "Service token of the resource context owning the group, when acting for a "
                    + "service rather than a user. Omit for an ordinary user request.")
	    })
	@ApiResponses({
	    @ApiResponse(responseCode = "200"),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "403",
	                 description = "The caller neither owns the group nor supplied a service token "
	                              + "for its owner."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
	@GetMapping(value = { "/groups/{groupid}", "/mygroups/{groupid}" })
	public ResponseEntity<JsonNode> queryMyGroup(@PathVariable Long groupid, @AuthenticationPrincipal UserProfile up,
			@RequestHeader(required=false, value=SERVICE_TOKEN_HEADER)  String serviceToken) {
		try {
			UserGroup g = usersAndGroupsManager.queryGroupWithMembership(up, groupid);
			if (g == null) {
				throw new VOURPException(VOURPException.UNAUTHORIZED, "Not allowed to view this group");
			}
			GroupInfo gi = UGMModelsMapper.map(g, true); // TODO is it important to add owningresource ALWAYS?
			Resource or = usersAndGroupsManager.queryGroupOwningResource(up.getTom(), g.getId());
			addOwningResource(gi, or);
			return jsonAPIHelper.success(gi);
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity("Error retrieving group info", Optional.of(up), e);
		}
	}
	@Operation(
	    summary = "Get one group owned by a resource rather than a user.",
	    description = "Returns a group whose owner is a registered resource. Intended for the "
	                  + "service that owns it, identified by its service token.",
	    parameters = {
	        @Parameter(name = "groupid", in = ParameterIn.PATH,
	                   description = "Identifier of the group."),
	        @Parameter(name = "X-Service-Auth-ID", in = ParameterIn.QUERY,
	                   description = "Service token of the resource context owning the group.")
	    })
	@ApiResponses({
	    @ApiResponse(responseCode = "200"),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "403",
	                 description = "The service token does not own this group."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
	@GetMapping(value = { "/resourcegroup/{groupid}" })
	public ResponseEntity<JsonNode> queryResourceOwnedGroup(@PathVariable Long groupid, @AuthenticationPrincipal UserProfile up,
			@RequestHeader(required=true, value=SERVICE_TOKEN_HEADER)  String serviceToken) {
		try {
			GroupInfo g = usersAndGroupsManager.queryResourceOwnedGroup(serviceToken, groupid, up.getTom());
			if (g == null) {
				throw new VOURPException(VOURPException.UNAUTHORIZED, "Not allowed to view this group");
			}
			return jsonAPIHelper.success(g);
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity("Error retrieving group info", Optional.of(up), e);
		}
	}

	@Operation(
	    summary = "List the groups owned by one resource.",
	    description = "Returns every group owned by the identified resource. Intended for the "
	                  + "service that owns it, identified by its service token.",
	    parameters = {
	        @Parameter(name = "resourceUUID", in = ParameterIn.PATH,
	                   description = "Identifier of the resource whose groups are returned."),
	        @Parameter(name = "X-Service-Auth-ID", in = ParameterIn.QUERY,
	                   description = "Service token of the resource context owning the resource.")
	    })
	@ApiResponses({
	    @ApiResponse(responseCode = "200"),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "403",
	                 description = "The service token does not own this resource."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
	@GetMapping(value = { "/resourcegroups/{resourceUUID}" })
	public ResponseEntity<JsonNode> queryResourceOwnedGroups(@PathVariable String resourceUUID, @AuthenticationPrincipal UserProfile up,
			@RequestHeader(required=true, value=SERVICE_TOKEN_HEADER)  String serviceToken) {
		try {
			List<GroupInfo> g = usersAndGroupsManager.queryResourceOwnedGroups(serviceToken, resourceUUID, up.getTom());
			if (g == null) {
				throw new VOURPException(VOURPException.UNAUTHORIZED, "Not allowed to view this resources groups");
			}
			return jsonAPIHelper.success(g);
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity("Error retrieving group info", Optional.of(up), e);
		}
	}
	/**
	 * If the token identifies the owner, return the group specified by the
	 * groupid.<br/>
	 *
	 * @param groupid
	 * @param request
	 * @param response
	 * @return
	 * @throws VOURPException
	 */
	@Operation(
	    summary = "Delete a group the caller owns.",
	    description = "Removes the group along with its memberships and the shares granted to it. "
	                  + "The caller must own the group, or supply the service token of the resource "
	                  + "context that owns it. Also served at /mygroups/{groupid}.",
	    parameters = {
	        @Parameter(name = "groupid", in = ParameterIn.PATH,
	                   description = "Identifier of the group to delete."),
	        @Parameter(name = "X-Service-Auth-ID", in = ParameterIn.QUERY,
	                   description = "Service token of the resource context owning the group, when acting for a "
                    + "service rather than a user.")
	    })
	@ApiResponses({
	    @ApiResponse(responseCode = "200"),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "403",
	                 description = "The caller neither owns the group nor supplied a service token "
	                              + "for its owner."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
	@DeleteMapping(value = { "/groups/{groupid}", "/mygroups/{groupid}" })
	public ResponseEntity<JsonNode> deleteMyGroup(@PathVariable Long groupid, @AuthenticationPrincipal UserProfile up,
			@RequestHeader(required=false, value=SERVICE_TOKEN_HEADER)  String serviceToken) {
		try {
			UserGroup ug = usersAndGroupsManager.deleteGroup(groupid, up, serviceToken);
			JsonNode json = om.valueToTree("OK");

			LogUtils.buildLog()
				.showInUserHistory()
				.user(up)
				.sentence()
					.subject(up.getUsername())
					.verb("deleted")
					.predicate(LOGGING_PREDICATE_GROUP_NAME, ug.getName())
				.extraField(LOGGING_FIELD_GROUP_ID, groupid)
				.log();
			return new ResponseEntity<>(json, HttpStatus.OK);
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity("Error deleting group", Optional.of(up), e);
		}
	}

	@Operation(
	    summary = "List the groups the caller belongs to.",
	    description = "Returns every group of which the calling user is a member, whoever owns it, "
	                  + "together with the caller's membership status in each.")
	@ApiResponses({
	    @ApiResponse(responseCode = "200"),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
	@GetMapping("/groups")
	public ResponseEntity<JsonNode> queryUserGroups(@AuthenticationPrincipal UserProfile up) {
		try {
			NativeQueryResult r = usersAndGroupsManager.queryMemberGroups(up);
			JsonNode json = om.valueToTree(r);
			return new ResponseEntity<>(json, HttpStatus.OK);
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity("Error retrieving groups", Optional.of(up), e);
		}
	}

	@Operation(
	    summary = "List the public groups.",
	    description = "Returns groups marked PUBLIC, which any user may join without an "
	                  + "invitation.")
	@ApiResponses({
	    @ApiResponse(responseCode = "200"),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
	@GetMapping("/publicgroups")
	public ResponseEntity<JsonNode> queryPublicGroups(@AuthenticationPrincipal UserProfile up) {
		try {
			Collection<PublicGroupModel> r = usersAndGroupsManager.queryPublicGroups(up);
			JsonNode json = om.valueToTree(r);
			return new ResponseEntity<>(json, HttpStatus.OK);
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity("Error retrieving groups", Optional.of(up), e);
		}
	}
	/**
	 * Manage the group POSTED to this method.<r/> If a group with the submitted name
	 * (and possibly Id) does not already exist, create a new one. Otherwise update
	 * the description and invitations.
	 *
	 * @param jobModel
	 * @param request
	 * @param response
	 * @return
	 */
	@Operation(
	    summary = "Create a group, or update one that already exists.",
	    description = "If no group with the submitted name and id exists, a new one is created "
	                  + "with the caller as owner. Otherwise its description and invitations are "
	                  + "updated.",
	    parameters = {
	        @Parameter(name = "X-Service-Auth-ID", in = ParameterIn.QUERY,
	                   description = "Service token of the resource context that should own the group, when "
                    + "creating a group owned by a service rather than a user.")
	    },
	    requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
	                   description = "GroupInfo as JSON. Include the id to update an existing group; omit it to "
                    + "create a new one."))
	@ApiResponses({
	    @ApiResponse(responseCode = "200"),
	    @ApiResponse(responseCode = "400",
	                 description = "The request is not valid; the response body carries the reason."),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "403",
	                 description = "The caller may not create or modify this group."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
	@PostMapping("/groups")
	public ResponseEntity<JsonNode> manageGroup(@RequestBody String s,
			@AuthenticationPrincipal UserProfile up,
			@RequestHeader(required=false, value=SERVICE_TOKEN_HEADER)  String serviceToken) {
		GroupInfo gi = null;
		try {
			ObjectMapper mapper = RACMUtil.newObjectMapper();
			ObjectNode node = mapper.readValue(s, ObjectNode.class);

			if (node != null) {
				gi = mapper.convertValue(node, GroupInfo.class);
				if (RACMNames.USERGROUP_PUBLIC.equals(gi.getGroupName())) {
					throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,
							"Illegal attempt made to edit the public group through the API.");
				}
			} else {
				throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT, "No valid Json posted to groups endpoint");
			}
			// validation of group/user combination done in manageGroup
			// TODO should serviceToken be passed along?
			UserGroup ug = usersAndGroupsManager.manageGroup(gi, up, serviceToken);
			
			
			String verb = gi.getId() == null ? LOGGING_VERB_GROUP_CREATED : LOGGING_VERB_GROUP_UPDATED;
			LogUtils.buildLog()
				.showInUserHistory()
				.user(up)
				.sentence()
					.subject(up.getUsername())
					.verb(verb)
					.predicate(LOGGING_PREDICATE_GROUP_NAME, gi.getGroupName())
				.extraField(LOGGING_FIELD_GROUP_ID, ug.getId())
				.log();

			gi = UGMModelsMapper.map(ug, true);
			JsonNode json = om.valueToTree(gi);
			return new ResponseEntity<>(json, HttpStatus.OK);
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity(
			        String.format("Error modifying group info or creating group: %s ",e.getMessage()) ,
					Optional.of(up), e);
		}
	}

	/**
	 * Manage the group POSTED to this method.<r/> If a group with the submitted name
	 * (and possibly Id) does not already exist, create a new one. Otherwise update
	 * the description and invitations.
	 *
	 * @param jobModel
	 * @param request
	 * @param response
	 * @return
	 */
	@Operation(
	    summary = "Create a group owned by a resource rather than a user.",
	    description = "Creates a group whose owner is the identified resource, so that the service "
	                  + "managing that resource controls the membership. Requires the service token "
	                  + "of the resource context that owns the resource.",
	    parameters = {
	        @Parameter(name = "resourceUUID", in = ParameterIn.PATH,
	                   description = "Identifier of the resource that will own the group."),
	        @Parameter(name = "X-Service-Auth-ID", in = ParameterIn.QUERY,
	                   description = "Service token of the resource context owning the resource.")
	    },
	    requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
	                   description = "CreateLinkedGroupModel as JSON, naming the group and the resource it "
                    + "belongs to."))
	@ApiResponses({
	    @ApiResponse(responseCode = "200"),
	    @ApiResponse(responseCode = "400",
	                 description = "The request is not valid; the response body carries the reason."),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "403",
	                 description = "The service token does not own this resource."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
	@PutMapping("/{resourceUUID}/groups")
	public ResponseEntity<JsonNode>createGroup(@RequestBody String s,
			@AuthenticationPrincipal UserProfile up,
			@PathVariable String resourceUUID,
			@RequestHeader(required=false, value=SERVICE_TOKEN_HEADER)  String serviceToken) {
		CreateLinkedGroupModel cgm = null;
		try {
			ObjectMapper mapper = RACMUtil.newObjectMapper();
			ObjectNode node = mapper.readValue(s, ObjectNode.class);

			if (node != null) {
				cgm = mapper.convertValue(node, CreateLinkedGroupModel.class);
				if (RACMNames.USERGROUP_PUBLIC.equals(cgm.getGroupName())) {
					throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,
							"Illegal attempt made to edit the public group through the API.");
				}
			} else {
				throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT, "No valid Json posted to groups endpoint");
			}
			// validation done in manageGroup
			UserGroup ug = usersAndGroupsManager.createLinkedGroup(cgm, up, resourceUUID, serviceToken);
			
			
			String verb = cgm.getId() == null ? LOGGING_VERB_GROUP_CREATED : LOGGING_VERB_GROUP_UPDATED;
			LogUtils.buildLog()
				.showInUserHistory()
				.user(up)
				.sentence()
					.subject(up.getUsername())
					.verb(verb)
					.predicate(LOGGING_PREDICATE_GROUP_NAME, cgm.getGroupName())
				.extraField(LOGGING_FIELD_GROUP_ID, ug.getId())
				.log();

			GroupInfo gi = UGMModelsMapper.map(ug, true);
			JsonNode json = om.valueToTree(gi);
			return new ResponseEntity<>(json, HttpStatus.OK);
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity("Error modifying group info or creating group",
					Optional.of(up), e);
		}
	}
	
	/**
	 * TODO make safe against updating owned groups
	 * @param up
	 * @param groupid
	 * @param input
	 * @return
	 */
	@Operation(
	    summary = "Update a group's details.",
	    description = "Applies a partial update to the group: only the fields present in the "
	                  + "request are changed.",
	    parameters = {
	        @Parameter(name = "groupid", in = ParameterIn.PATH,
	                   description = "Identifier of the group to update."),
	        @Parameter(name = "X-Service-Auth-ID", in = ParameterIn.QUERY,
	                   description = "Service token of the resource context owning the group, when acting for a "
                    + "service rather than a user.")
	    },
	    requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
	                   description = "UpdateGroupInfo as JSON, carrying only the fields to change."))
	@ApiResponses({
	    @ApiResponse(responseCode = "204",
	                 description = "The group was updated."),
	    @ApiResponse(responseCode = "400",
	                 description = "The request is not valid; the response body carries the reason."),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "403",
	                 description = "The caller may not modify this group."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
	@PatchMapping(value = "/groups/{groupid}")
	public ResponseEntity<JsonNode> updateGroup(@AuthenticationPrincipal UserProfile up, @PathVariable long groupid,
			@RequestBody UpdateGroupInfo input, @RequestHeader(required=false, value=SERVICE_TOKEN_HEADER) String serviceToken) {
		try {
			UserGroup ug = usersAndGroupsManager.updateGroup(up, groupid, input, serviceToken);
			LogUtils.buildLog()
				.showInUserHistory()
				.user(up)
				.sentence()
					.subject(up.getUsername())
					.verb(LOGGING_VERB_GROUP_UPDATED)
					.predicate(LOGGING_PREDICATE_GROUP_NAME, ug.getName())
				.extraField(LOGGING_FIELD_GROUP_ID, groupid)
				.log();
			return ResponseEntity.noContent().build();
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity("Error updating group info", Optional.of(up), e);
		}
	}

	@Operation(
	    summary = "Grant a group permission to act on a resource.",
	    description = "Gives every member of the group the named actions on the identified "
	                  + "resource. The caller must be allowed to grant on that resource.",
	    parameters = {
	        @Parameter(name = "groupid", in = ParameterIn.PATH,
	                   description = "Identifier of the group being granted the actions."),
	        @Parameter(name = "actions", in = ParameterIn.QUERY,
	                   description = "Names of the actions to grant, repeated or comma-separated."),
	        @Parameter(name = "resourceType", in = ParameterIn.QUERY,
	                   description = "Kind of resource being shared. Accepted values are the names of the "
                    + "ActionsOnResource TYPE enum."),
	        @Parameter(name = "entityId", in = ParameterIn.QUERY,
	                   description = "Identifier of the resource being shared."),
	        @Parameter(name = "X-Service-Auth-ID", in = ParameterIn.QUERY,
	                   description = "Service token of the resource context owning the resource, when acting for "
                    + "a service rather than a user.")
	    })
	@ApiResponses({
	    @ApiResponse(responseCode = "204",
	                 description = "The actions were granted."),
	    @ApiResponse(responseCode = "400",
	                 description = "The request is not valid; the response body carries the reason."),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "403",
	                 description = "The caller may not grant on this resource."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
	@PutMapping("/groups/{groupid}/sharedResources")
	public ResponseEntity<JsonNode> shareResource(@AuthenticationPrincipal UserProfile up, @PathVariable long groupid,
			@RequestParam(name = "actions", required = true) List<String> actions,
			@RequestParam(name = "resourceType", required = true) String resourceType,
			@RequestParam(name = "entityId", required = true) Long entityId,
			@RequestHeader(required=false, value=SERVICE_TOKEN_HEADER)  String serviceToken) {

		try {
			SharedResourceResult result = usersAndGroupsManager.shareResource(up, groupid,
					new ActionsOnResource(entityId, actions, ActionsOnResource.TYPE.valueOf(resourceType)), serviceToken);
			String predicate = String.format("%s '%s' with group '%s'",
					resourceType.toLowerCase(), result.getResourceName(),
					result.getGroupName());
			LogUtils.buildLog()
				.showInUserHistory()
				.user(up)
				.sentence()
					.subject(up.getUsername())
					.verb("shared")
					.predicate(predicate)
				.extraField(LOGGING_FIELD_GROUP_ID, result.getGroupId())
				.extraField("entityId", entityId)
				.extraField("resourceType", resourceType)
				.log();
			return ResponseEntity.noContent().build();
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity("Error sharing resources with group", Optional.of(up),
					e);
		}
	}

	@Operation(
	    summary = "Create or update the caller's user profile.",
	    description = "Registers the calling user in RACM if they are not known yet, and updates "
	                  + "the profile fields carried in the request.",
	    requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
	                   description = "UserProfile fields as JSON."))
	@ApiResponses({
	    @ApiResponse(responseCode = "200"),
	    @ApiResponse(responseCode = "400",
	                 description = "The request is not valid; the response body carries the reason."),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
	@PostMapping("/user")
	public ResponseEntity<JsonNode> manageUserProfile(@RequestBody String s, @AuthenticationPrincipal UserProfile up) {
		PersonalUserInfo ui = null;
		try {
			ObjectMapper mapper = RACMUtil.newObjectMapper();
			ObjectNode node = mapper.readValue(s, ObjectNode.class);

			if (node != null) {
				ui = mapper.convertValue(node, PersonalUserInfo.class);
			} else {
				throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT, "No valid Json posted to user endpoint");
			}

			// validation of group/user combination is done in manageGroup
			ui = usersAndGroupsManager.manageUserProfile(ui, up);
			LogUtils.buildLog()
				.showInUserHistory()
				.user(up)
				.sentence()
					.subject(up.getUsername())
					.verb(LOGGING_VERB_GROUP_UPDATED)
					.predicate("user profile")
				.log();
			JsonNode json = om.valueToTree(ui);
			return new ResponseEntity<>(json, HttpStatus.OK);
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity("Error modifying user profile", Optional.of(up), e);
		}
	}

	@Operation(
	    summary = "Get the caller's user profile.",
	    description = "Returns the calling user's profile as RACM holds it, including their "
	                  + "visibility setting.")
	@ApiResponses({
	    @ApiResponse(responseCode = "200"),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
	@GetMapping("/user")
	public ResponseEntity<JsonNode> queryUserProfile(@AuthenticationPrincipal UserProfile up) {
		try {
			PersonalUserInfo ui = UGMModelsMapper.mapPersonalInfo(up.getUser());
			JsonNode json = om.valueToTree(ui);
			return new ResponseEntity<>(json, HttpStatus.OK);
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity("Error retrieving user info", Optional.of(up), e);
		}
	}

	/**
	 *
	 * @param groupId
	 * @param request
	 * @param response
	 * @return
	 */
	@Operation(
	    summary = "Accept an invitation to join a group.",
	    description = "Turns a pending invitation for the calling user into membership of the "
	                  + "group.",
	    parameters = {
	        @Parameter(name = "groupId", in = ParameterIn.QUERY,
	                   description = "Identifier of the group whose invitation is being accepted."),
	        @Parameter(name = "X-Service-Auth-ID", in = ParameterIn.QUERY,
	                   description = "Service token of the resource context owning the group, when acting for a "
                    + "service rather than a user.")
	    })
	@ApiResponses({
	    @ApiResponse(responseCode = "200"),
	    @ApiResponse(responseCode = "400",
	                 description = "There is no pending invitation for this user and group."),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
	@PostMapping("/groups/accept")
	public ResponseEntity<JsonNode> acceptInvitation(@RequestParam Long groupId, @AuthenticationPrincipal UserProfile up, 
			@RequestHeader(required=false, value=SERVICE_TOKEN_HEADER)  String serviceToken) {
		return respondToInvitation(groupId, true, up, serviceToken);
	}

	@Operation(
	    summary = "Join a public group.",
	    description = "Adds the calling user to a group marked PUBLIC, which needs no invitation. "
	                  + "Returns true when the user became a member.",
	    parameters = {
	        @Parameter(name = "groupId", in = ParameterIn.QUERY,
	                   description = "Identifier of the public group to join."),
	        @Parameter(name = "X-Service-Auth-ID", in = ParameterIn.QUERY,
	                   description = "Service token of the resource context owning the group, when acting for a "
                    + "service rather than a user.")
	    })
	@ApiResponses({
	    @ApiResponse(responseCode = "200"),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "403",
	                 description = "The group is not public, so it cannot be joined without an "
	                              + "invitation."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
	@PostMapping("/groups/join")
	public boolean joinPublicGroup(@RequestParam Long groupId, @AuthenticationPrincipal UserProfile up, 
			@RequestHeader(required=false, value=SERVICE_TOKEN_HEADER)  String serviceToken) {
		try {
			boolean ok = usersAndGroupsManager.joinPublicGroup(groupId, up, serviceToken);
			return ok;
		} catch (Exception e) {
			return false;//jsonAPIHelper.logAndReturnJsonExceptionEntity("Error joining group", Optional.of(up), e);
		}
	}

	@Operation(
	    summary = "Leave a group.",
	    description = "Removes the calling user's membership. The owner of a group cannot leave "
	                  + "it; delete the group instead.",
	    parameters = {
	        @Parameter(name = "groupId", in = ParameterIn.QUERY,
	                   description = "Identifier of the group to leave."),
	        @Parameter(name = "X-Service-Auth-ID", in = ParameterIn.QUERY,
	                   description = "Service token of the resource context owning the group, when acting for a "
                    + "service rather than a user.")
	    })
	@ApiResponses({
	    @ApiResponse(responseCode = "200"),
	    @ApiResponse(responseCode = "400",
	                 description = "The caller is not a member of this group, or owns it."),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
	@PostMapping("/groups/leave")
	public ResponseEntity<JsonNode> leaveGroup(@RequestParam Long groupId, @AuthenticationPrincipal UserProfile up,
			@RequestHeader(required=false, value=SERVICE_TOKEN_HEADER)  String serviceToken) {
		try {
			UserGroup ug = usersAndGroupsManager.leaveGroup(groupId, up, serviceToken);
			LogUtils.buildLog()
				.showInUserHistory()
				.user(up)
				.sentence()
					.subject(up.getUsername())
					.verb("left")
					.predicate(LOGGING_PREDICATE_GROUP_NAME, ug.getName())
				.extraField(LOGGING_FIELD_GROUP_ID, groupId)
				.log();
			JsonNode json = om.valueToTree("OK");
			return new ResponseEntity<>(json, HttpStatus.OK);
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity("Error leaving group", Optional.of(up), e);
		}
	}

	/**
	 *
	 * @param groupId
	 * @param request
	 * @param response
	 * @return
	 */
	@Operation(
	    summary = "Decline an invitation to join a group.",
	    description = "Withdraws a pending invitation for the calling user without joining the "
	                  + "group.",
	    parameters = {
	        @Parameter(name = "groupId", in = ParameterIn.QUERY,
	                   description = "Identifier of the group whose invitation is being declined."),
	        @Parameter(name = "X-Service-Auth-ID", in = ParameterIn.QUERY,
	                   description = "Service token of the resource context owning the group, when acting for a "
                    + "service rather than a user.")
	    })
	@ApiResponses({
	    @ApiResponse(responseCode = "200"),
	    @ApiResponse(responseCode = "400",
	                 description = "There is no pending invitation for this user and group."),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
	@PostMapping("/groups/decline")
	public ResponseEntity<JsonNode> declineInvitation(@RequestParam Long groupId, @AuthenticationPrincipal UserProfile up, 
			@RequestHeader(required=false, value=SERVICE_TOKEN_HEADER)  String serviceToken) {
		return respondToInvitation(groupId, false, up, serviceToken);
	}

	/**
	 *
	 * @param groupId
	 * @param accept
	 * @param request
	 * @param response
	 * @return
	 */
	private ResponseEntity<JsonNode> respondToInvitation(Long groupId, boolean accept, UserProfile up, String serviceToken) {
		try {
			UserGroup ug = usersAndGroupsManager.acceptInvitation(groupId, up, accept, serviceToken);

			String action = accept ? "accepted" : "declined";
			LogUtils.buildLog()
				.showInUserHistory()
				.user(up)
				.sentence()
					.subject(up.getUsername())
					.verb(action)
					.predicate("invitation to group '%s'", ug.getName())
				.extraField(LOGGING_FIELD_GROUP_ID, groupId)
				.log();
			GroupInfo gi = UGMModelsMapper.map(ug);
			JsonNode json = om.valueToTree(gi);
			return new ResponseEntity<>(json, HttpStatus.OK);
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity("Error responding to group invitation",
					Optional.of(up), e);
		}
	}

	@Operation(
	    summary = "List the users, and optionally groups, visible to the caller.",
	    description = "Returns users whose visibility is PUBLIC, so that they can be invited to "
	                  + "groups or granted access to resources. Groups are returned alongside the "
	                  + "users only when no filter is given.",
	    parameters = {
	        @Parameter(name = "users", in = ParameterIn.QUERY,
	                   description = "Restrict the result to users matching this filter, and omit groups from "
                    + "the response. Matching semantics to be confirmed.")
	    })
	@ApiResponses({
	    @ApiResponse(responseCode = "200"),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
	@GetMapping("/users/public")
	public ResponseEntity<JsonNode> queryVisibleUsersAndGroups(@AuthenticationPrincipal UserProfile up,
			@RequestParam(name="users",required=false) String usersFilter) throws VOURPException {
		try {
			List<User> users = usersAndGroupsManager.queryPublicUsers(up, usersFilter);
			List<UserInfo> uis = new ArrayList<>();
			for (User u : users)
				uis.add(UGMModelsMapper.map(u));

			SciServerEntities ents = new SciServerEntities();
			ents.setUsers(uis);
			if(usersFilter == null) { // also query for groups is users not explicitly requested
				ents.setGroups(usersAndGroupsManager.queryAllGroups(up.getTom())
					.stream()
					.filter(ug -> up.isAdmin() || !ug.getName().equals(RACMNames.USERGROUP_PUBLIC))
					.map(ug -> UGMModelsMapper.map(ug, false))
					.collect(toList()));
			}

			if(up.isAdmin())
			    ents.setServices(usersAndGroupsManager.queryAllServiceAccounts(up.getTom()).stream().map(sa -> UGMModelsMapper.map(sa)).collect(toList()));
			
			JsonNode json = om.valueToTree(ents);
			return new ResponseEntity<>(json, HttpStatus.OK);
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity("Error querying visible users and groups",
					Optional.of(up), e);
		}
	}

}
