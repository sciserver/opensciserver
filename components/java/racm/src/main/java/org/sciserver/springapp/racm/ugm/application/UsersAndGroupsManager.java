package org.sciserver.springapp.racm.ugm.application;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.Query;

import org.eclipse.persistence.config.QueryHints;
import org.ivoa.dm.VOURPException;
import org.ivoa.dm.model.InvalidTOMException;
import org.ivoa.dm.model.MetadataObject;
import org.ivoa.dm.model.TransientObjectManager;
import org.sciserver.racm.resourcecontext.model.AssociatedSciserverEntityModel;
import org.sciserver.racm.resources.model.ResourceModel;
import org.sciserver.racm.ugm.model.CreateLinkedGroupModel;
import org.sciserver.racm.ugm.model.GroupAction;
import org.sciserver.racm.ugm.model.GroupInfo;
import org.sciserver.racm.ugm.model.MemberUserModel;
import org.sciserver.racm.ugm.model.PersonalUserInfo;
import org.sciserver.racm.ugm.model.PublicGroupModel;
import org.sciserver.racm.ugm.model.PublicGroupModel.Admin;
import org.sciserver.racm.ugm.model.UpdateGroupInfo;
import org.sciserver.racm.utils.model.NativeQueryResult;
import org.sciserver.springapp.racm.login.InsufficientPermissionsException;
import org.sciserver.springapp.racm.login.LoginPortalAccess;
import org.sciserver.springapp.racm.storem.application.RegistrationInvalidException;
import org.sciserver.springapp.racm.ugm.domain.ActionsOnResource;
import org.sciserver.springapp.racm.ugm.domain.UGMModelsMapper;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.sciserver.springapp.racm.utils.RACM;
import org.sciserver.springapp.racm.utils.RACMNames;
import org.sciserver.springapp.racm.utils.RACMUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.jhu.file.UserVolume;
import edu.jhu.job.DatabaseContext;
import edu.jhu.job.DockerImage;
import edu.jhu.job.VolumeContainer;
import edu.jhu.rac.AssociatedSciEntity;
import edu.jhu.rac.OwnershipCategory;
import edu.jhu.rac.Resource;
import edu.jhu.user.GroupAccessibility;
import edu.jhu.user.GroupRole;
import edu.jhu.user.Member;
import edu.jhu.user.MemberStatus;
import edu.jhu.user.Party;
import edu.jhu.user.SciserverEntity;
import edu.jhu.user.ServiceAccount;
import edu.jhu.user.User;
import edu.jhu.user.UserGroup;
import edu.jhu.user.UserVisibility;

@Service
public class UsersAndGroupsManager {
	private final RACMUtil racmUtil;
	private final RACM racm;
	private final LoginPortalAccess loginPortalAccess;
	@Autowired
	public UsersAndGroupsManager(RACMUtil racmUtil, RACM racm, LoginPortalAccess loginPortalAccess) {
		this.racmUtil = racmUtil;
		this.racm = racm;
		this.loginPortalAccess = loginPortalAccess;
	}
/**
	 * Call this method, rather than registerUser, from a method that creates more than just the user before persisting the tom.<br/>
	 * @param userName
	 * @param userId
	 * @param contactEmail
	 * @param tom
	 * @return
	 * @throws VOURPException
	 */
	public User newUser(String userName, String userId, String contactEmail, TransientObjectManager tom) {
		User user = queryUserByName(userName, tom);
		if(user != null)
			throw new IllegalArgumentException(
					String.format("User with userName '%s' already exists",userName));
		user = new User(tom);
		user.setUserId(userId);
		user.setUsername(userName);
		user.setContactEmail(contactEmail);
		user.setVisibility(UserVisibility.PUBLIC);
		addUserToPublicGroup(user);
		return user;
	}

	private UserProfile registerUser(String userName, String userId, String contactEmail, TransientObjectManager tom) {
		User user = newUser(userName,userId, contactEmail, tom);
		try {
			tom.persist();
		} catch (InvalidTOMException e) {
			throw new IllegalArgumentException("Invalid user created", e);
		}
		return new UserProfile(user);
	}
	/**
	 * Accept or decline invitation.<br/>
	 * Note, if group is owned by a resource, the servietoken MUST NOT be null and MUST identify the reqourcecontext owning the groupe.
	 * This indicates that users can only accept such invitations IF they come from the service, as that service MAY want to know when the user enters.
	 * Possibly for example to add a user volume for the user (in a coursewrae context).
	 * @param groupId
	 * @param up
	 * @param accepts if true the invitation is accepted, otherwise it is declined.
	 * @return
	 * @throws VOURPException
	 */
	public UserGroup acceptInvitation(Long groupId, UserProfile up, boolean accepts, String serviceToken) throws VOURPException{
		// check whether a resource exists owning this group. checks also whether the specified service owns that resource.
		// this method will throw an exception if the resource exists but is not owned by the service token
		Resource owningResource = checkGroupOwningResource(groupId,serviceToken, up.getTom());

		UserGroup ug = queryGroup(groupId, up.getTom());
		if(ug != null){
			MemberStatus status = (accepts?MemberStatus.ACCEPTED:MemberStatus.DECLINED);
			for(Member i: ug.getMember())
			{
				if(i.getScisEntity() == up.getUser() && i.getStatus() == MemberStatus.INVITED) {
					i.setStatus(status);
					up.getTom().persist();
					return ug;
				}
			}
		}
		throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,
				String.format("User %s was not invited to group with ID %d",up.getUsername(), groupId));
	}
	/**
	 * Join a public group.<br/>
	 * @param groupId
	 * @param up
	 * @param serviceToken
	 * @return
	 * @throws VOURPException
	 */
	public boolean joinPublicGroup(Long groupId, UserProfile up, String serviceToken) throws VOURPException{
		Resource owningResource = checkGroupOwningResource(groupId,serviceToken, up.getTom());

		UserGroup ug = queryGroup(groupId, up.getTom());
		boolean ok = false;
		if(ug != null){
			if(ug.getAccessibility() != GroupAccessibility.PUBLIC)
				throw new VOURPException(VOURPException.UNAUTHORIZED,String.format("User %s tried to join a non-public UserGroup with ID %d",up.getUsername(), groupId));
			for(Member i: ug.getMember())
			{
				if(i.getScisEntity() == up.getUser()) {
					if(i.getStatus() == MemberStatus.INVITED || i.getStatus() == MemberStatus.DECLINED|| i.getStatus() == MemberStatus.WITHDRAWN) {
						ok = true;
						i.setStatus(MemberStatus.ACCEPTED);
					} else
						return true;

				}
			}
			if(!ok)
			{
				Member i = new Member(ug);
				i.setMemberRole(GroupRole.MEMBER);
				i.setStatus(MemberStatus.ACCEPTED);
				i.setScisEntity(up.getUser());
				ok = true;
			}
			if(ok)
				up.getTom().persist();
			return true;
		}
		else 
			throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,
				String.format("User %s tried to join a non-existing USerGroup with ID %d",up.getUsername(), groupId));
	}

	/**
	 * User leaves the group identified by the groupId.<br/>eave the
	 * @param groupId
	 * @param up
	 * @return
	 * @throws VOURPException
	 */
	public UserGroup leaveGroup(Long groupId, UserProfile up, String serviceToken) throws VOURPException{
		// check whether a resource exists owning this group. checks also whether the specified service owns that resource.
		// this method will throw an exception if the resource exists but is not owned by the service token
		Resource owningResource = checkGroupOwningResource(groupId,serviceToken, up.getTom());

		UserGroup ug = queryGroup(groupId, up.getTom());
		if(ug != null && RACMNames.USERGROUP_PUBLIC.equals(ug.getName()))
			throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,
					String.format("Illegal attempt made to remove user %s from public group",up.getUsername()));

		if(ug != null){
			for(Member i:ug.getMember())
			{
				if(i.getScisEntity() == up.getUser() && i.getStatus() == MemberStatus.ACCEPTED){
//					i.setStatus(MemberStatus.DECLINED);
					ug.getMember().remove(i);
					up.getTom().persist();
					return ug;
				}
			}
		}
		throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,
				String.format("User %s was not a member of group with ID %d",up.getUsername(), groupId));
	}


	/**
	 * Return the Member object if the user was already invited. Return null otherwise.<br/>
	 */
	private Member checkMembership(UserGroup ug, String username) {
		// check invitee is not already member or was already invited
		if(ug.getMember() == null)
			return null;
		for(Member i: ug.getMember())
		{
			SciserverEntity s = i.getScisEntity();
			if(s instanceof User){
				User u = (User)s;
				if(u.getUsername().equals(username))
					return i;
			}
		}
		return null;
	}


	/**
	 * Attempt an update of the member's status/role to the one represented by the model.
	 * Return false if state was not updated, true otherwise.<br/>
	 * @param m
	 * @param mum
	 * @param action
	 * @param user the user attempting the action. May be the member, or the owner, or an admin of the group.
	 * 	Certain actions (ADD) can ony be done by an owner/admin who is also a system admin.
	 * @return
	 */
	private boolean attemptUpdateMemberState(Member m, MemberUserModel mum, GroupAction action, UserProfile user, String serviceToken)
		{

		GroupRole mumRole = GroupRole.fromValue(mum.getRole().value());

		// a user can be ADD-ed by a user whoi is an admin, or when the group is owned by a Resource and the serviceToken corresponds to its ResourceContext
		boolean canAdd = user.isAdmin();
		try {
			Resource owningResource = checkGroupOwningResource(m.getContainer().getId(), serviceToken, user.getTom());
			if(owningResource != null)
				canAdd = true;  // services can ADD users as members if they want
		} catch(VOURPException e) {
			// TODO can we swallow exception??? 
		}
		
		MemberStatus mumStatus = MemberStatus.fromValue(mum.getStatus().value());
		if (m.getStatus() == MemberStatus.OWNER)
			return false; // don't even try to change the existing owner

		if(mumRole == GroupRole.OWNER || mumStatus == MemberStatus.OWNER)
			throw new IllegalArgumentException("Attempted to set a user to an owner state or role");

		switch(m.getMemberRole()){
			case OWNER: return false; // no actions can change an OWNER's
			case ADMIN:
				switch(m.getStatus()){
					case OWNER: return false;
					case INVITED:
						// ACCEPT INVITATION
						if(MemberStatus.ACCEPTED == mumStatus &&
						(user.getUser() == m.getScisEntity() || serviceToken != null)){ // service account is allowed to do this as well
							m.setStatus(MemberStatus.ACCEPTED);
							return true;
						}
						// DECLINE INVITATION
						if(MemberStatus.DECLINED==mumStatus && user.getUser() == m.getScisEntity()){
							m.setStatus(MemberStatus.DECLINED);
							return true;
						}
						// ADD INVITED
						if(MemberStatus.ACCEPTED==mumStatus && canAdd && user.isGroupEditor(m.getContainer())) {
							m.setStatus(MemberStatus.ACCEPTED);
							return true;
						}
						// REMOVE INVITED
						if(MemberStatus.WITHDRAWN == mumStatus && user.isGroupEditor(m.getContainer())) {
							m.setStatus(MemberStatus.WITHDRAWN);
							return true;
						}
						// DEMOTE INVITED
						if(MemberStatus.INVITED == mumStatus && mumRole == GroupRole.MEMBER && user.isGroupEditor(m.getContainer())){
							m.setMemberRole(mumRole);
							return true;
						}
						return false;
					case ACCEPTED:
						// WITHDRAW
						if(MemberStatus.WITHDRAWN == mumStatus && user.isGroupEditor(m.getContainer())){
								m.setStatus(MemberStatus.WITHDRAWN);
								return true;
						}
						// LEAVE
						if(MemberStatus.DECLINED == mumStatus && m.getScisEntity() == user.getUser()){
							m.setStatus(MemberStatus.DECLINED);
							return true;
						}
						// DEMOTE
						if (MemberStatus.ACCEPTED == mumStatus && mumRole == GroupRole.MEMBER && user.isGroupEditor(m.getContainer())) {
							m.setMemberRole(mumRole);
							return true;
						}
						return false;
					case WITHDRAWN:
						// && action == GroupAction.INVITE
						if(MemberStatus.INVITED == mumStatus && user.isGroupEditor(m.getContainer())){
							m.setStatus(MemberStatus.INVITED);
							m.setMemberRole(mumRole);
							return true;
						}
						// && action == GroupAction.ADD
						if(MemberStatus.ACCEPTED == mumStatus &&  canAdd && user.isGroupEditor(m.getContainer())){
							m.setStatus(MemberStatus.ACCEPTED);
							m.setMemberRole(mumRole);
							return true;
						}
						return false;
					case DECLINED:
						// REMOVE DECLINED only ADMIN can remove a member with status DECLINED. In this way can be reinvited later.
						if(MemberStatus.WITHDRAWN == mumStatus && user.isGroupEditor(m.getContainer())) {
							m.setStatus(MemberStatus.WITHDRAWN);
							return true;
						}
						return false;
					default:
						throw new IllegalArgumentException("Unknown member status: " + m.getStatus());
				}
			case MEMBER:
				switch(m.getStatus()){
					case OWNER: return false;
					case INVITED:
						// ACCEPT INVITATION  && action == GroupAction.ACCEPT
						if(MemberStatus.ACCEPTED == mumStatus && m.getScisEntity() == user.getUser()){
							m.setStatus(MemberStatus.ACCEPTED);
							return true;
						}
						// DECLINE INVITATION  && action == GroupAction.DECLINE
						if(MemberStatus.DECLINED == mumStatus  && m.getScisEntity() == user.getUser()){
							m.setStatus(MemberStatus.DECLINED);
							return true;
						}
						// ADD INVITED, possibly promotes as well
						//  && action == GroupAction.ADD
						if(MemberStatus.ACCEPTED == mumStatus && canAdd && user.isGroupEditor(m.getContainer())) {
							m.setStatus(MemberStatus.ACCEPTED);
							m.setMemberRole(mumRole);
							return true;
						}
						// REMOVE INVITED
						// && action == GroupAction.REMOVE
						if(MemberStatus.WITHDRAWN == mumStatus && user.isGroupEditor(m.getContainer())) {
							m.setStatus(MemberStatus.WITHDRAWN);
							return true;
						}
						// PROMOTE INVITED
						if(MemberStatus.INVITED == mumStatus && mumRole == GroupRole.ADMIN && user.isGroupEditor(m.getContainer())){
							m.setMemberRole(mumRole);
							return true;
						}
						return false;
					case ACCEPTED:
						// PROMOTE ACCEPTED (action == GroupAction.ADD || action == GroupAction.INVITE)
						if(MemberStatus.ACCEPTED == mumStatus && mumRole == GroupRole.ADMIN  && user.isGroupEditor(m.getContainer())){
							m.setMemberRole(mumRole);
							return true;
						}
						// REMOVE ACCEPTED  action == GroupAction.REMOVE
						if(MemberStatus.WITHDRAWN == mumStatus && user.isGroupEditor(m.getContainer())) {
							m.setStatus(MemberStatus.WITHDRAWN);
							return true;
						}
						// LEAVE  action == GroupAction.LEAVE
						if(MemberStatus.DECLINED == mumStatus && m.getScisEntity() == user.getUser()) {
							m.setStatus(MemberStatus.DECLINED);
							return true;
						}
						return false;
					case WITHDRAWN:
						if(MemberStatus.INVITED == mumStatus && action == GroupAction.INVITE && user.isGroupEditor(m.getContainer())){
							m.setStatus(MemberStatus.INVITED);
							m.setMemberRole(mumRole);
							return true;
						}
						if(MemberStatus.ACCEPTED == mumStatus && action == GroupAction.ADD  && user.isGroupEditor(m.getContainer()) && canAdd){
							m.setStatus(MemberStatus.ACCEPTED);
							m.setMemberRole(mumRole);
							return true;
						}
						return false;
					case DECLINED:
						// REMOVE DECLINED only ADMIN can remove a member with status DECLINED. In this way can be reinvited later.
						if(MemberStatus.WITHDRAWN == mumStatus && user.isGroupEditor(m.getContainer())) {
							m.setStatus(MemberStatus.WITHDRAWN);
							return true;
						}
						// allow users to be invited to groups again after they decline
						if(MemberStatus.INVITED == mumStatus && action == GroupAction.INVITE && user.isGroupEditor(m.getContainer())){
							m.setStatus(MemberStatus.INVITED);
							m.setMemberRole(mumRole);
							return true;
						}
						return false;
					default:
						throw new IllegalArgumentException("Unknown member status: " + m.getStatus());
			}
			default:
				throw new IllegalArgumentException("Unknown member role: " + m.getMemberRole());
		}
	}


	public static MemberStatus getMemberStatus(MemberUserModel mur) {
		return MemberStatus.valueOf(mur.getRole().value());
	}

	/**
	 * Accept a user in the group.<br/>
	 *
	 * @param ug
	 * @param invite
	 * @return
	 * @throws VOURPException
	 */
	private Member addGroupMember(UserGroup ug, UserProfile gadmin, MemberUserModel invite, String serviceToken) throws VOURPException {


		Member member = checkMembership(ug, invite.getUsername());
		if(member != null){
			attemptUpdateMemberState(member, invite, GroupAction.ADD, gadmin, serviceToken);
			return member;
		}
		User u = queryUserByName(invite.getUsername(), ug.getTom());
		if(u == null) 
	          throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,String.format("Cannot find user with username=%s",invite.getUsername()));
		member = new Member(ug);
		member.setScisEntity(u);
		// default role (INVITED), unless serviceToken != null in which case the actual role the service requests is used
		member.setStatus(serviceToken == null?MemberStatus.INVITED:MemberStatus.valueOf(invite.getStatus().value()));
		member.setMemberRole(GroupRole.valueOf(invite.getRole().value()));
		attemptUpdateMemberState(member, invite, GroupAction.ADD, gadmin, serviceToken);

		return member;
	}

	private Member inviteGroupMember(UserGroup ug, UserProfile gadmin, MemberUserModel member, String serviceToken) throws VOURPException {
		Member i = checkMembership(ug, member.getUsername());
		if(i != null){
			attemptUpdateMemberState(i, member, GroupAction.INVITE, gadmin, serviceToken);
			return i;
		}
		TransientObjectManager tom = ug.getTom();
		Query q = tom.createQuery("select u from User u where u.username=:username").setParameter("username",member.getUsername());
		User u = tom.queryOne(q, User.class);
		if(u == null)
			throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,String.format("Cannot find user with username=%s",member.getUsername()));
		if(u == ug.getOwner())
			return null;
		i = new Member(ug);
		i.setMemberRole(GroupRole.fromValue(member.getRole().value()));
		i.setStatus(MemberStatus.INVITED);
		i.setScisEntity(u);

		return i;
	}

	/**
	 * It is assumed that the caller has checked whether the user is allowed to remove the member.
	 * This should include checking whether the group is owned by a service and that the owning service is making the request.<br/>
	 * @param ug
	 * @param withdraw
	 * @return
	 * @throws VOURPException
	 */
	private Member removeGroupMember(UserGroup ug, UserProfile user, MemberUserModel withdraw, String serviceToken) {
		Member member = checkMembership(ug, withdraw.getUsername());
		if (member != null) {
			attemptUpdateMemberState(member, withdraw, GroupAction.REMOVE, user, serviceToken);
		}
		return member;
	}

	public void addUserToPublicGroup(User user) {
		// Add user to public group
		Query q = user.getTom().createQuery("select g from UserGroup g where g.name=:name")
				.setParameter("name", RACMNames.USERGROUP_PUBLIC);
		UserGroup ug = user.getTom().queryOne(q,UserGroup.class);

		if(ug == null)
			throw new IllegalStateException(
					String.format("Public group cannot be found. First initialize RACM database before adding user %s",user.getUsername()));
		Member m = new Member(ug);
		m.setScisEntity(user);
		m.setStatus(MemberStatus.ACCEPTED);
		m.setMemberRole(GroupRole.MEMBER); // get default role (MEMBER)
	}

	public List<User> queryPublicUsers(UserProfile up, String users) throws VOURPException{
		TransientObjectManager tom = up.getTom();
		Query q;
		if(users != null) {
			List<String> list = Arrays.asList(users.split(","));
			q = tom.createQuery("select o from User o where o.visibility=:v and o.username in :list order by o.username").setParameter("v", UserVisibility.PUBLIC).setParameter("list",list);
		} else {
			q = tom.createQuery("select o from User o where o.visibility=:v order by o.username").setParameter("v", UserVisibility.PUBLIC);
		}
		q.setHint(QueryHints.LEFT_FETCH, "o.party");
		return tom.queryJPA(q, User.class);
	}
	public User queryUserByName(String userName, TransientObjectManager tom) {
		Query q = tom.createQuery("select o from User o where o.username=:username").setParameter("username", userName);
		return tom.queryOne(q, User.class);
	}
	/**
	 * Utility method to query User in a predefined TOM context.<br/>
	 * @param userId
	 * @param tom
	 * @return
	 */
	public User queryUserByUserId(String userId, TransientObjectManager tom) {
		Query q = tom.createQuery("select u from User u where u.userId=:userid").setParameter("userid", userId);
		return tom.queryOne(q, User.class);
	}

	public User getRequestUser(String userId,TransientObjectManager tom) throws VOURPException{
		Query query = tom.createQuery("select o from User o where o.userId = :userid").setParameter("userid",userId);
		List<MetadataObject> os = tom.queryJPA(query);
		if(os.size() == 1){
			return (User)os.get(0);
		} else if(os.isEmpty()){
			return null;
		} else {
			throw new VOURPException(VOURPException.ILLEGAL_STATE,String.format("Found multiple users for userId=%s",userId));
		}
	}

	public List<UserGroup> queryAllGroups(TransientObjectManager tom) throws VOURPException{
		Query q = tom.createQuery("SELECT g FROM UserGroup g "
				+ "ORDER BY g.name");
		return tom.queryJPA(q, UserGroup.class);
	}

	public List<ServiceAccount> queryAllServiceAccounts(TransientObjectManager tom) throws VOURPException{
        Query q = tom.createQuery("SELECT sa FROM ServiceAccount sa ");
        return tom.queryJPA(q, ServiceAccount.class);
    }

	
	/**
	 * Get information about public groups.
	 * Include owner and admins and resources directly shared with the group (NO actions included).
	 * Also include indication whether user is a member.
	 * 
	 * @param up
	 * @return
	 */
	public Collection<PublicGroupModel> queryPublicGroups(UserProfile up) {
		//String columns = "id,name, description,adminid,memberrole, username,contactEmail";
		String sql="select * from racm.PublicGroups(?) order by name,memberrole desc, username";
		Query q = up.getTom().createNativeQuery(sql);
		q.setParameter(1, up.getId());
		List<?> rows = up.getTom().executeNativeQuery(q);
		HashMap<Long, PublicGroupModel> groups = new HashMap<>();
		for(Object o:rows) {
			int i = 0;
			Object[] row = (Object[])o;
			Long id = (Long)row[i++];
			PublicGroupModel pgm = groups.get(id);
			if(pgm == null) {
				pgm = new PublicGroupModel(id);
				pgm.setName((String)row[i++]);
				pgm.setDescription((String)row[i++]);
				pgm.setUserRole((String)row[i++]);
				pgm.setUserStatus((String)row[i++]);
				groups.put(id, pgm);
			} else
				i=5;
			PublicGroupModel.Admin admin = new PublicGroupModel.Admin((Long)row[i++]);
			admin.setRole(org.sciserver.racm.ugm.model.GroupRole.valueOf((String)row[i++]));
			admin.setUsername((String)row[i++]);
			admin.setContactEmail((String)row[i++]);
			pgm.addAdmin(admin);
		}
		
		//columns = " groupId,resourceId, publisherDID,resourceUUID,resourceName, resourceDescription,
		//           resourcetype,contextClass,racmEndpoint, resourceContextDescription,resourceContextUUID";
		sql="select * from racm.PublicGroupResources order by groupId,contextclass, resourcetype,resourcename";
		q = up.getTom().createNativeQuery(sql);
		rows = up.getTom().executeNativeQuery(q);
		
		for(Object o:rows) {
			int i = 0;
			Object[] row = (Object[])o;
			PublicGroupModel pgm = groups.get((Long)row[i++]);
			if(pgm == null) // RUNTIME ERROR
				continue;
			Long resourceId=(Long)row[i++];
			String publisherDID=(String)row[i++];
			String uuid = (String)row[i++];
			String name = (String)row[i++];
			String description = (String)row[i++];
			String resourceTypeName = (String)row[i++];
			ResourceModel rm = new ResourceModel(resourceId,publisherDID,uuid,name,description,resourceTypeName);
			rm.setContextClassName((String)row[i++]);
			rm.setResourceContextDescription((String)row[i++]);
			rm.setResourceContextUUID((String)row[i++]);
			pgm.addResource(rm);
		}
		return groups.values();
	}
	
	private UserGroup queryGroup(Long id, TransientObjectManager tom) {
		Query q = tom.createQuery("select g from UserGroup g where g.id=:id").setParameter("id", id);
		return tom.queryOne(q,UserGroup.class);
	}

	private Resource queryResourceForServiceAccount(String serviceToken, String resourceUUID, TransientObjectManager tom) {
		Query q = tom.createQuery(
				  "select r from Resource r join r.container rc join rc.account a "
				+ "where r.uuid=:uuid and a.serviceToken=:token")
				.setParameter("token", serviceToken)
				.setParameter("uuid", resourceUUID);
		return tom.queryOne(q,Resource.class);
	}

	/**
	 * return the group identified by the groupid, and which must be owned by the specified service account.<br/>
	 * 
	 * @param serviceToken
	 * @param groupId
	 * @param tom
	 * @return
	 */
	public GroupInfo queryResourceOwnedGroup(String serviceToken, long groupId, TransientObjectManager tom) {
		Resource or = queryGroupOwningResource(tom, groupId);

		if(or != null) {
			ServiceAccount sa = or.getContainer().getAccount();
			UserGroup ug = null;
			if(sa != null && sa.getServiceToken().equals(serviceToken)) {
				ug=queryGroup(groupId, tom);
				if(ug != null)
					return UGMModelsMapper.map(ug, true);
			}
		}
		return null;
	}
	/**
	 * return all the groups owned by the identified resource, and which must be owned by the specified service account.<br/>
	 * 
	 * @param serviceToken
	 * @param groupId
	 * @param tom
	 * @return
	 */
	public List<GroupInfo> queryResourceOwnedGroups(String serviceToken, String resourceUUID, TransientObjectManager tom) throws VOURPException{
		Query q = tom.createQuery("select ug from Resource r, r.associatedGroup ag, UserGroup ug"
				+ " where r.uuid = :uuid and r.container.account.serviceToken=:token and ag.sciEntity.id=ug.id and ag.ownership=:owned");
		q.setParameter("token", serviceToken);
		q.setParameter("uuid", resourceUUID);
		q.setParameter("owned", OwnershipCategory.OWNED);
		List<UserGroup> ugs = tom.queryJPA(q, UserGroup.class);
		ArrayList<GroupInfo> gis = new ArrayList<GroupInfo>();
		if(ugs != null) {
			for(UserGroup ug : ugs)
				gis.add(UGMModelsMapper.map(ug));
		}
		return gis;
	}
	public List<UserGroup> queryEditableGroups(UserProfile up){
		TransientObjectManager tom = up.getTom();

		String jpq = "select ug " +
				"  from UserGroup ug JOIN User u JOIN ug.member m " +
				" where u.username=:name " +
				"    and m.memberRole in(edu.jhu.user.GroupRole.OWNER, edu.jhu.user.GroupRole.ADMIN) " +
				"    and m.scisEntity=u " +
				"    and m.status in (edu.jhu.user.MemberStatus.OWNER, edu.jhu.user.MemberStatus.ACCEPTED)";

		Query q = tom.createQuery(jpq).setParameter("name", up.getUsername());
		List<MetadataObject> l = tom.queryJPA(q,false);
		List<UserGroup> ugs = new ArrayList<>();
		for(MetadataObject o: l)
		{
			UserGroup ug = (UserGroup)o;
			ugs.add(ug);
		}
		return ugs;
	}


	public Hashtable<Long, Resource> queryOwnedEditableGroups(UserProfile up) {
		TransientObjectManager tom = up.getTom();
		String jpq="select r, ug.id as id \r\n" +
				" from Resource r join fetch r.resourceType rt join r.associatedGroup ae join UserGroup ug on ae.sciEntity=ug\r\n" +
				"JOIN User u JOIN ug.member m \r\n" +
				"where u.username=:name\r\n" +
				"   and m.memberRole in(edu.jhu.user.GroupRole.OWNER, edu.jhu.user.GroupRole.ADMIN) \r\n" +
				"    and m.scisEntity=u\r\n" +
				"  and m.status in (edu.jhu.user.MemberStatus.OWNER, edu.jhu.user.MemberStatus.ACCEPTED)";
		Query q = tom.createQuery(jpq);
		q.setParameter("name", up.getUsername());
		List<?> tuples = tom.customJPQL(q,false);
		Hashtable<Long, Resource> result = new Hashtable<>();
		for(Object o: tuples) {
			Object[] or = (Object[])o;
			Resource r = (Resource)or[0];
			Long gid=(Long)or[1];
			result.put(gid, r);
		}
		return result;
	}

	public UserGroup queryEditableGroup(UserProfile up, Long groupid){
		TransientObjectManager tom = up.getTom();
		String jpq = "select ug " +
				"  from UserGroup ug JOIN User u JOIN ug.member m " +
				" where ug.id=:groupid and u.username=:name " +
				"    and m.memberRole in(edu.jhu.user.GroupRole.OWNER, edu.jhu.user.GroupRole.ADMIN) " +
				"    and m.scisEntity=u " +
				"    and m.status in (edu.jhu.user.MemberStatus.OWNER, edu.jhu.user.MemberStatus.ACCEPTED)";
		Query q = tom.createQuery(jpq);
		q.setParameter("name", up.getUsername());
		q.setParameter("groupid", groupid);
		return tom.queryOne(q, UserGroup.class);
	}

	public UserGroup queryGroupWithMembership(UserProfile up, long groupid) {
		TransientObjectManager tom = up.getTom();
		String groupQuery = "SELECT ug FROM UserGroup ug JOIN User u JOIN ug.member m" +
				" where ug.id=:groupid and u.username=:name " +
				"and m.scisEntity=u " +
				"and m.status in (edu.jhu.user.MemberStatus.OWNER, edu.jhu.user.MemberStatus.ACCEPTED)";
		Query q = tom.createQuery(groupQuery)
				.setParameter("name", up.getUsername())
				.setParameter("groupid", groupid);
		return tom.queryOne(q, UserGroup.class);
	}

	public Resource queryGroupOwningResource(TransientObjectManager tom, long groupid) {
		String groupQuery = "SELECT r FROM Resource r join r.associatedGroup ae join UserGroup ug on ug = ae.sciEntity where ug.id=:groupid ";
		Query q = tom.createQuery(groupQuery).setParameter("groupid", groupid);
		return tom.queryOne(q, Resource.class);
	}


	/**
	 * Return all groups the specified user is a member of, together with the status of the user's membership.<br/>
	 * Include groups owned by user.
	 * @param up
	 * @return
	 */
	public NativeQueryResult queryMemberGroups(UserProfile up){
		TransientObjectManager tom = up.getTom();
		String args = "id,name,description,role,status,owner,ownerVisibility";
		String sql = String.format(
				  "with ug as ("
				+ "select g.*, o.username as owner, o.visibility as ownerVisibility, m.memberRole as role, m.status "
				+ "from UserGroup g, [User] o, Member m "
				+ " where g.id=m.containerId "
				+ "   and o.id = g.ownerId "
				+ "   and m.sciSentityId=? "
				+ "   and m.status in ('INVITED','ACCEPTED','OWNER')) "
				+ "select %s from ug",args);
		Query q = tom.createNativeQuery(sql);
		q.setParameter(1, up.getId());
		List<?> rows = tom.executeNativeQuery(q);
		NativeQueryResult r=new NativeQueryResult();
		r.setColumns(args);
		r.setRows(rows);

		return r;
	}

	public Optional<UserGroup> queryGroup(String name, TransientObjectManager tom) {
		Query q = tom.createQuery("select g from UserGroup g where g.name=:name").setParameter("name", name);
		List<MetadataObject> l = tom.queryJPA(q,false);
		if(l.size() == 1)
			return Optional.of((UserGroup)l.get(0));
		if(l.isEmpty())
			return Optional.empty();
		throw new IllegalStateException(String.format("Found > 1 groups with name '%s'",name));
	}
    public Optional<ServiceAccount> queryServiceAccount(String name, TransientObjectManager tom) {
        Query q = tom.createQuery("select sa from ServiceAccount sa where sa.identity.publisherDID=:name").setParameter("name", name);
        List<MetadataObject> l = tom.queryJPA(q,false);
        if(l.size() == 1)
            return Optional.of((ServiceAccount)l.get(0));
        if(l.isEmpty())
            return Optional.empty();
        throw new IllegalStateException(String.format("Found > 1 service accounts with publisherDID '%s'",name));
    }
	public UserGroup manageGroup(GroupInfo gi, UserProfile creator, String serviceToken) throws VOURPException {
		if(gi.getId() == null)
			return createGroup(gi, creator, serviceToken);
		else
			return updateGroup(gi,creator, serviceToken);
	}

	public PersonalUserInfo manageUserProfile(PersonalUserInfo ui, UserProfile up) throws VOURPException{
		User u = up.getUser();
		if(ui.getUsername() != null && !u.getUsername().equals(ui.getUsername()))
			throw new VOURPException("Cannot change username");
		if(ui.getContactEmail() != null && !u.getContactEmail().equals(ui.getContactEmail())){
			throw new VOURPException("Cannot change email");
		}
		if (ui.getVisibility() != null) {
			u.setVisibility(UserVisibility.fromValue(ui.getVisibility()));
		}

		u.setPreferences(ui.getPreferences());
		Party p = u.getParty();
		if (p == null){
			p = new Party(up.getTom());
			u.setParty(p);
		}
		if(p.getTom() == null)
			p.setTom(u.getTom());
		p.setAffiliation(ui.getAffiliation());
		p.setFullName(ui.getFullname() == null ? "" : ui.getFullname());
		u.getTom().persist();
		return ui;
	}
	/**
	 * Create a UserGroup for the specified GroupInfo, setting the specified user as the owner.<br/>
	 * Also add the owner as an explicit member.
	 *
	 * Note, we assume that if a service wants to create a group "for a resource", the GroupInfo's "owning resource must be set.
	 * This cannot be complete, e.g. entityId on associatedsciserverentity canniot be set etc.
	 *
	 * @param gi
	 * @param creator
	 * @return
	 * @throws VOURPException
	 */
	private UserGroup createGroup(GroupInfo gi, UserProfile creator, String serviceToken) throws VOURPException{
		// check name. Must not be null and be unique.
		Resource owningResource = null;
		if(gi.getOwningResource() != null) {
			// there must be a non-null serviceToken that identifies a serviceaccount held by the resourcecontext owning the resource.
			owningResource = queryResourceForServiceAccount(serviceToken, gi.getOwningResource().getResource().getUUID(),creator.getTom());
			if(owningResource == null)
			{
				throw new VOURPException(VOURPException.UNAUTHORIZED,"when creating a Group that should be owned by a Resource, the service token correspond nto the resource's resourcecontext service account ");
			}
		}

		String name = gi.getGroupName();
		if(name == null || name.trim().length() == 0)
			throw new VOURPException("Name of group must not be empty");

		if(gi.getAccessibility() == org.sciserver.racm.ugm.model.GroupAccessibility.PUBLIC) {
			if(!racm.canUserDoRootAction(creator.getUsername(),RACMNames.A_CREATE_PUBLIC_GROUP)) {
				throw new VOURPException(VOURPException.UNAUTHORIZED,"User is not allowed to create a public user group");
			};
		}
		if (gi.getAccessibility() == org.sciserver.racm.ugm.model.GroupAccessibility.SYSTEM) {
			throw new UnsupportedOperationException("Cannot add system groups");
		}


		TransientObjectManager tom = creator.getTom();
		UserGroup ug = newGroup(creator.getUser(), gi.getGroupName(), gi.getAccessibility(),tom);
		if(owningResource != null)
		{
			AssociatedSciserverEntityModel aem = gi.getOwningResource().getAssociation();
			AssociatedSciEntity ae = new AssociatedSciEntity(owningResource);
			ae.setOwnership(aem.isOwned()?OwnershipCategory.OWNED:OwnershipCategory.LINKED);
			ae.setUsage(aem.getUsage());
			ae.setSciEntity(ug);
		}

		ug.setDescription(gi.getDescription());

		for(MemberUserModel im: gi.getMemberUsers()){
			MemberStatus imStatus = MemberStatus.fromValue(im.getStatus().value());
			if(!im.getUsername().equals(creator.getUsername())){
				if(imStatus == MemberStatus.INVITED)
					inviteGroupMember(ug, creator, im, serviceToken);
				else if(imStatus == MemberStatus.ACCEPTED)
					addGroupMember(ug, creator, im, serviceToken);
				// no other legal options
			}
		}

		tom.persist();
		return ug;
	}

	public UserGroup createLinkedGroup(CreateLinkedGroupModel gi, UserProfile creator, String resourceUUID, String serviceToken) throws VOURPException{
		// check name. Must not be null and be unique.
		Resource linkingResource = queryResourceForServiceAccount(serviceToken, resourceUUID,creator.getTom());
		if(linkingResource == null)
			throw new VOURPException(VOURPException.UNAUTHORIZED,"when creating a Group that should be owned by a Resource, the service token correspond nto the resource's resourcecontext service account ");

		if(gi.getAccessibility() == org.sciserver.racm.ugm.model.GroupAccessibility.PUBLIC) {
			if(!racm.canUserDoRootAction(creator.getUsername(),RACMNames.A_CREATE_PUBLIC_GROUP)) {
				throw new VOURPException(VOURPException.UNAUTHORIZED,"User is not allowed to create a public user group");
			};
		}

		String name = gi.getGroupName();
		if(name == null || name.trim().length() == 0)
			throw new VOURPException("Name of group must not be empty");

		TransientObjectManager tom = creator.getTom();
		UserGroup ug = newGroup(creator, gi.getGroupName());
		if(linkingResource != null)
		{
			AssociatedSciEntity ae = new AssociatedSciEntity(linkingResource);
			ae.setOwnership(gi.isOwned()?OwnershipCategory.OWNED:OwnershipCategory.LINKED);
			ae.setUsage(gi.getUsage());
			ae.setSciEntity(ug);
		}

		ug.setDescription(gi.getDescription());

		for(MemberUserModel im: gi.getMemberUsers()){
			MemberStatus imStatus = MemberStatus.fromValue(im.getStatus().value());
			if(!im.getUsername().equals(creator.getUsername())){
				if(imStatus == MemberStatus.INVITED)
					inviteGroupMember(ug, creator, im,serviceToken);
				else if(imStatus == MemberStatus.ACCEPTED)
					addGroupMember(ug, creator, im,serviceToken);
				// no other legal options
			}
		}

		tom.persist();
		return ug;
	}
	/**
	 * This method SHOULD be used to define a new group for a user as it will ensure the owner gets added as a member with status OWNER,
	 * as its owner and that the groiup's unique name is checked.<br/>
	 * @param creator
	 * @param name
	 * @return
	 * @throws VOURPException
	 */
	private UserGroup newGroup(UserProfile creator, String groupname) {
		return newGroup(creator.getUser(), groupname, creator.getTom());
	}

	/**
	 * This method SHOULD be used to define a new group for a user as it will ensure the owner gets added as a member with status OWNER,
	 * as its owner and that the group's unique name is checked.<br/>
	 * @param creator
	 * @param name
	 * @return
	 * @throws VOURPException
	 */
	private UserGroup newGroup(User creator, String name, org.sciserver.racm.ugm.model.GroupAccessibility accessibility, TransientObjectManager tom) {
		validateGroupName(tom, name);

		UserGroup ug = new UserGroup(creator.getTom());
		ug.setOwner(creator);
		ug.setName(name);
		GroupAccessibility ac = GroupAccessibility.fromValue(accessibility.name());
		ug.setAccessibility(ac);

		Member owner = new Member(ug);
		owner.setScisEntity(creator);
		owner.setStatus(MemberStatus.OWNER);
		owner.setMemberRole(GroupRole.OWNER);
		return ug;
	}
	private UserGroup newGroup(User creator, String name, TransientObjectManager tom) {
		return this.newGroup(creator,name,org.sciserver.racm.ugm.model.GroupAccessibility.PRIVATE, tom);
	}

	private void validateGroupName(TransientObjectManager tom, String name, Long...idsToIgnore) {
		Optional<UserGroup> existingGroup = queryGroup(name, tom);
		if (existingGroup.isPresent() &&
				!Arrays.asList(idsToIgnore).contains(existingGroup.get().getId())) {
			throw new RegistrationInvalidException(
				String.format("A group with name '%s' already exists",
						name));
		}
	}

	/**
	 * Checks whether the identified group is owned by a resource.
	 * If so, checks whether the specified service owns that resource.
	 * if not, throw and exception.
	 * if yes, returns the resource.
	 *
	 * @param groupId
	 * @param serviceToken
	 * @param up
	 * @return
	 * @throws VOURPException
	 */
	private Resource checkGroupOwningResource(long groupId, String serviceToken,TransientObjectManager tom) throws VOURPException{
		Resource owningResource = queryGroupOwningResource(tom, groupId);
		if(owningResource != null) {
			ServiceAccount serviceAccount = owningResource.getContainer().getAccount();
			if(serviceAccount == null || !serviceAccount.getServiceToken().equals(serviceToken))
				throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT, "A group owned by a resource can only be edited by its service");
		}
		return owningResource;
	}

	/**
	 * Update a group's information
	 * @param up The user with access to this group
	 * @param groupId The id of the group
	 * @param input description of changes to make to this group
	 * @throws VOURPException
	 */
	public UserGroup updateGroup(UserProfile up, long groupId, UpdateGroupInfo input, String serviceToken) throws VOURPException {
		TransientObjectManager tom = up.getTom();
		UserGroup ug = queryEditableGroup(up, groupId);
		
		// check if group is owned, which also checks if it is owned by the identified serviceToken 
		checkGroupOwningResource(groupId, serviceToken, up.getTom());
		
		
		if (ug == null)
			throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,
					String.format(
							"A group with id '%s' does not exist or "
							+ "cannot be edited by user '%s'", groupId,
							up.getUsername()));

		if (RACMNames.USERGROUP_PUBLIC.equals(ug.getName())
				|| RACMNames.USERGROUP_ADMIN.equals(ug.getName()))
			throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,
					"Illegal attempt made to edit the 'public' or 'admin' groups.");

		if(input.getName() != null){
			validateGroupName(tom, input.getName(), ug.getId());
			ug.setName(input.getName());
		}
		if(input.getDescription() != null)
			ug.setDescription(input.getDescription());

		updateAccessibility(ug, input.getAccessibility(), up.getUser());

		tom.persist();
		return ug;
	}

	private void updateAccessibility(UserGroup ug, org.sciserver.racm.ugm.model.GroupAccessibility updateAcc, User user) throws VOURPException{
		if (updateAcc == null) return;
		GroupAccessibility newAcc = GroupAccessibility.valueOf(updateAcc.name());
		if(newAcc != ug.getAccessibility()) {
			// Not allowed to change a group to or from SYSTEM!!!
			if (ug.getAccessibility() == GroupAccessibility.SYSTEM)
				throw new VOURPException(VOURPException.UNAUTHORIZED,
						"No user is allowed to change accessibility of SYSTEM group");
			if(newAcc == GroupAccessibility.SYSTEM)
				throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,
						"Illegal attempt made to change group to SYSTEM group");

			// only privileged users can make a group PUBLIC
			if(newAcc == GroupAccessibility.PUBLIC && !canUserCreatePublicGroup(user))
				throw new VOURPException(VOURPException.UNAUTHORIZED,
						"User is not allowed to make a PRIVATE group PUBLIC");
			// assuming that if an attempt is made to make a group PRIVATE from PUBLIC and the oser has such privileges, this is ok, not test necessray.
			ug.setAccessibility(newAcc);
		}
	}


	public SharedResourceResult shareResource(UserProfile up,
			long groupid,
			ActionsOnResource actionsOnResource,
			String serviceToken) throws VOURPException {
		TransientObjectManager tom = up.getTom();
		MetadataObject entity;

		// check whether a resource exists owning this group. checks also whether the specified service owns that resource.
		// this method will throw an exception if the resource exists but is not owned by the service token
		Resource owningResource = checkGroupOwningResource(groupid,serviceToken, up.getTom());

		Resource resource;
		UserGroup ug;
		try {
			ug = tom.find(UserGroup.class, groupid);
			entity = tom.find(
					actionsOnResource.getType().getVOURPClass(),
					actionsOnResource.getEntityId());
			resource = getResourceFromVOURPObject(entity);
		} catch (NullPointerException ignored) {
			throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,
					"Cannot find group or resource");
		}

		if (!racm.canUserGrantOnResource(up.getUsername(), resource.getUuid())) {
			throw new InsufficientPermissionsException("grant resource to group");
		}

		if (ug.getName().equals(RACMNames.USERGROUP_PUBLIC) && !up.isAdmin()) {
			throw new InsufficientPermissionsException("grant resource to public group");
		}

		racmUtil.deleteAllPrivileges(up.getTom(), ug.getId(), resource);
		racmUtil.assignPrivileges(ug, resource,
				actionsOnResource.getActions().toArray(new String[] {}));
		tom.persist();
		return new SharedResourceResult(entity, ug);
	}

	public class SharedResourceResult {
		private final MetadataObject entity;
		private final UserGroup ug;
		private SharedResourceResult(MetadataObject entity, UserGroup ug) {
			this.entity = entity;
			this.ug = ug;
		}
		public String getResourceName() {
			if (entity instanceof UserVolume) {
				return ((UserVolume)entity).getName();
			} else if (entity instanceof VolumeContainer) {
				return ((VolumeContainer)entity).getName();
			} else if (entity instanceof DatabaseContext) {
				return ((DatabaseContext)entity).getName();
			} else if (entity instanceof DockerImage) {
				return ((DockerImage)entity).getName();
			} else if (entity instanceof Resource) {
				return ((Resource)entity).getName();
			} else {
				return String.format("Unknown type: %s with id %d",
						entity.getClassName(),
						entity.getId());
			}
		}
		public long getGroupId() {
			return ug.getId();
		}
		public String getGroupName() {
			return ug.getName();
		}
	}

	/**
	 * Pass in object with a getResource method and call it, returning the Resource.<br/>
	 * @param obj
	 * @return
	 */
	private <T extends MetadataObject> Resource getResourceFromVOURPObject(T obj) {
		if (obj instanceof Resource) return (Resource) obj;
		Method getResource;
		try {
			getResource = obj.getClass().getMethod("getResource");
			return (Resource) getResource.invoke(obj);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new IllegalArgumentException("Expected an object with a getResource() method", e);
		}
	}

	/**
	 * Update the group identified by the GroupInfo object.<br/>
	 * the user must be the owner of the group or an admin of the group.<br/>
	 * @param gi
	 * @param user
	 * @return
	 * @throws VOURPException
	 */
	private UserGroup updateGroup(GroupInfo gi, UserProfile user, String serviceToken) throws VOURPException{
		// check name, 'public' group may not be edited through this method.
		if(RACMNames.USERGROUP_PUBLIC.equals(gi.getGroupName()))
			throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,"Illegal attempt made to edit the 'public' group.");

		TransientObjectManager tom = user.getTom();
		UserGroup ug = queryEditableGroup(user,gi.getId());
		if(ug == null)
			throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,String.format("A group with id '%s' does not exist or cannot be edited by user '%s'",gi.getId(),user.getUsername()));

		// check whether a resource exists owning this group. checks also whether the specified service owns that resource.
		// this method will throw an exception if the resource exists but is not owned by the service token
		Resource owningResource = checkGroupOwningResource(gi.getId(),serviceToken, user.getTom());
		
		// update the group: accessibility, description and invitations. 

		updateAccessibility(ug, gi.getAccessibility(), user.getUser());

		ug.setDescription(gi.getDescription());
		for(MemberUserModel im: gi.getMemberUsers()){
			MemberStatus imStatus = MemberStatus.fromValue(im.getStatus().value());
			if(MemberStatus.INVITED == imStatus) // do we want this test?
				inviteGroupMember(ug, user, im, serviceToken); // no-op if user has already been invited or is already a member. Will re-invite users who declined an invitation in the past.
			else if(MemberStatus.ACCEPTED == imStatus) // do we want this test? only allowed if user is an admin
				addGroupMember(ug, user, im, serviceToken); // no-op if user has already been invited or is already a member. Will re-invite users who declined an invitation in the past.
			else if(MemberStatus.WITHDRAWN == imStatus) {
				// check membership - if membership = null do nothing, else set status = "WITHDRAWN"
				Member rm = removeGroupMember(ug, user, im, serviceToken);
				if(rm != null && rm.getStatus() == MemberStatus.WITHDRAWN)
					ug.getMember().remove(rm);
			}
		}

		tom.persist();
		return ug;
	}
	/**
	 * Update the group identified by the GroupInfo object.<br/>
	 * the user must be the owner of the group or an admin of the group.<br/>
	 * @param gi
	 * @param user
	 * @return
	 * @throws VOURPException
	 */
	public UserGroup deleteGroup(Long groupid, UserProfile user, String serviceToken) throws VOURPException {
		// check name, 'public' and 'admin' groups may not be deleted

		// check whether a resource exists owning this group. checks also whether the specified service owns that resource.
		// this method will throw an exception if the resource exists but is not owned by the service token
		Resource owningResource = checkGroupOwningResource(groupid,serviceToken, user.getTom());

		TransientObjectManager tom = user.getTom();
		UserGroup ug = queryEditableGroup(user, groupid);

		if (ug == null)
			throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,
					String.format("A group with id '%d' does not exist or cannot be edited by user '%s'", groupid,
							user.getUsername()));

		if (ug.getOwner() != user.getUser())
			throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT, "Only owner is allowed to delete a group");

		if (RACMNames.USERGROUP_PUBLIC.equals(ug.getName()))
			throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,
					"Illegal attempt made to delete the 'public' group.");
		if (RACMNames.USERGROUP_ADMIN.equals(ug.getName()))
			throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,
					"Illegal attempt made to delete the 'admin' group.");

		tom.remove(ug);
		tom.persist();
		return ug;
	}

	/**
	 * Assuming 'commaSeperatedGroups' is a comma separated list of group names, retrieve them and
	 * return them as array.<br/>
	 *
	 * TODO who asks for this? Does it need an authority?
	 *
	 * @param commaSeperatedGroups
	 * @return
	 */
	public UserGroup[] findGroups(String commaSeperatedGroups, TransientObjectManager tom) throws VOURPException {
		Map<String, UserGroup> ugs = new HashMap<>();
		if (commaSeperatedGroups == null || commaSeperatedGroups.trim().length() == 0)
			return new UserGroup[] {};
		String[] groups = commaSeperatedGroups.split("[,]");
		for (String rawGroupName : groups) {
			String group = rawGroupName.trim();
			if (group.length() > 0) {
				if (ugs.get(group) != null)
					continue;
				UserGroup ug = queryGroup(group, tom).orElseThrow(() ->
					new VOURPException(String.format("Group %s not found", group))
				);
				ugs.put(group, ug);
			}
		}
		return ugs.values().toArray(new UserGroup[] {});
	}

	public UserProfile createUser(String userName, String password, String contactEmail, TransientObjectManager tom)
			throws IOException {
		String userId = loginPortalAccess.userIdForUserName(userName);
		if (userId == null) {
			org.sciserver.springapp.racm.login.User user = loginPortalAccess.createUser(userName, password, contactEmail);
			userId = user.getUserId();
		}
		User u = queryUserByName(userName, tom);
		UserProfile up;
		if (u == null)
			up = registerUser(userName, userId, contactEmail, tom);
		else
			up = new UserProfile(u);
		return up;
	}

	public static boolean entityIsThePublicGroup(SciserverEntity e) {
		return e instanceof UserGroup &&
				((UserGroup) e).getName().equals(RACMNames.USERGROUP_PUBLIC);
	}
	public static boolean entityIsAPublicGroup(SciserverEntity e) {
		return e instanceof UserGroup &&
				((UserGroup) e).getAccessibility() == GroupAccessibility.PUBLIC;
	}
	public boolean canUserCreatePublicGroup(User u) {
		return racm.canUserDoRootAction(u.getUsername(), RACMNames.A_CREATE_PUBLIC_GROUP);
	}

}
