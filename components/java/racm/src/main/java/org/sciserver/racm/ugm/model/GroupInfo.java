package org.sciserver.racm.ugm.model;

import java.util.ArrayList;
import java.util.List;

import org.sciserver.racm.resourcecontext.model.RegisteredResourceModel;
import org.sciserver.racm.utils.model.RACMBaseModel;

public class GroupInfo extends RACMBaseModel{

	private UserInfo owner;
	private String groupName;
	private String description;
	private GroupAccessibility accessibility = GroupAccessibility.PRIVATE;
	// the object encaspulating information about the possible owning resource and its association to this group. 
	// if null, the Group is not owned by a resource.
	private SciEntityOwningResource owningResource = null;

	private List<MemberUserModel> memberUsers = new ArrayList<>();
	// TBD will we allow groups as members of groups?
	// TBD how about ServiceAccount-s?
	private List<MemberGroupModel> memberGroups = new ArrayList<>();
	
	public GroupInfo(){}

	public GroupInfo(long id)
	{
		super(id);
	}

	public String getGroupName() {
		return groupName;
	}
	public void setGroupName(String name) {
		this.groupName = name;
	}
	/* Alias for groupName */
	public void setName(String name) {
		setGroupName(name);
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public UserInfo getOwner() {
		return owner;
	}
	public void setOwner(UserInfo owner) {
		this.owner = owner;
	}

	public List<MemberUserModel> getMemberUsers() {
		return memberUsers;
	}
	public List<MemberGroupModel> getMemberGroups() {
		return memberGroups;
	}
	public void addInvitation(UserInfo user){
		MemberUserModel im = new MemberUserModel();
		im.setStatus(MemberStatus.INVITED);
		im.setUser(user);
		memberUsers.add(im);
	}

	public void setMemberUsers(List<MemberUserModel> memberUsers) {
		this.memberUsers = memberUsers;
	}

	public void setMemberGroups(List<MemberGroupModel> memberGroups) {
		this.memberGroups = memberGroups;
	}

	public SciEntityOwningResource getOwningResource() {
		return owningResource;
	}

	public void setOwningResource(SciEntityOwningResource owningResource) {
		this.owningResource = owningResource;
	}

	public GroupAccessibility getAccessibility() {
		return accessibility;
	}

	public void setAccessibility(GroupAccessibility accessibility) {
		this.accessibility = accessibility;
	}
}
