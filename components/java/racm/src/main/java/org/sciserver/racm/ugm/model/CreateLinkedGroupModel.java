package org.sciserver.racm.ugm.model;

import java.util.ArrayList;
import java.util.List;

import org.sciserver.racm.resourcecontext.model.RegisteredResourceModel;
import org.sciserver.racm.utils.model.RACMBaseModel;

import com.fasterxml.jackson.annotation.JsonProperty;

import edu.jhu.rac.OwnershipCategory;

public class CreateLinkedGroupModel extends RACMBaseModel{

	private UserInfo owner;
	private String groupName;
	private String description;
	private GroupAccessibility accessibility = GroupAccessibility.PRIVATE;
	
	public String getUsage() {
		return usage;
	}

	public void setUsage(String usage) {
		this.usage = usage;
	}

	public boolean isOwned() {
		return isOwned;
	}

	public void setOwned(boolean isOwned) {
		this.isOwned = isOwned;
	}

	private String usage = "";
	private boolean isOwned = false;
		

	private List<MemberUserModel> memberUsers = new ArrayList<>();
	// TBD will we allow groups as members of groups?
	// TBD how about ServiceAccount-s?
	private List<MemberGroupModel> memberGroups = new ArrayList<>();
	
	public CreateLinkedGroupModel(){}

	public CreateLinkedGroupModel(long id)
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

	public GroupAccessibility getAccessibility() {
		return accessibility;
	}

	public void setAccessibility(GroupAccessibility accessibility) {
		this.accessibility = accessibility;
	}

}
