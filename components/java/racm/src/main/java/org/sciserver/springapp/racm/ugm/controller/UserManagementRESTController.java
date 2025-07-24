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
import org.sciserver.springapp.loginterceptor.Log;
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
import sciserver.logging.ServiceLogTimer;

@RestController
@CrossOrigin
@RequestMapping("/ugm/rest")
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
	@PostMapping("/groups/accept")
	public ResponseEntity<JsonNode> acceptInvitation(@RequestParam Long groupId, @AuthenticationPrincipal UserProfile up, 
			@RequestHeader(required=false, value=SERVICE_TOKEN_HEADER)  String serviceToken) {
		return respondToInvitation(groupId, true, up, serviceToken);
	}

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

	@GetMapping("/users/public")
	public ResponseEntity<JsonNode> queryVisibleUsersAndGroups(@AuthenticationPrincipal UserProfile up,
			@RequestParam(name="users",required=false) String usersFilter) throws VOURPException {
		try {
		    ServiceLogTimer timer = Log.get().startTimer("queryPublicUsers [ms]");
		    List<User> users = usersAndGroupsManager.queryPublicUsers(up, usersFilter);
		    timer.stop();
			List<UserInfo> uis = new ArrayList<>();
			for (User u : users)
				uis.add(UGMModelsMapper.map(u));

			SciServerEntities ents = new SciServerEntities();
			ents.setUsers(uis);
			if(usersFilter == null) { // also query for groups is users not explicitly requested
			    timer = Log.get().startTimer("queryAllGroups [ms]");
				ents.setGroups(usersAndGroupsManager.queryAllGroups(up.getTom())
					.stream()
					.filter(ug -> up.isAdmin() || !ug.getName().equals(RACMNames.USERGROUP_PUBLIC))
					.map(ug -> UGMModelsMapper.map(ug, false))
					.collect(toList()));
                timer.stop();
			}

			if(up.isAdmin()) {
                timer = Log.get().startTimer("queryAllServiceAccounts [ms]");
			    ents.setServices(usersAndGroupsManager.queryAllServiceAccounts(up.getTom()).stream().map(sa -> UGMModelsMapper.map(sa)).collect(toList()));
                timer.stop();
			}
			
			JsonNode json = om.valueToTree(ents);
			return new ResponseEntity<>(json, HttpStatus.OK);
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity("Error querying visible users and groups",
					Optional.of(up), e);
		}
	}

}
