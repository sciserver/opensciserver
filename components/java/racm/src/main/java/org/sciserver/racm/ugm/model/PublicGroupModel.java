package org.sciserver.racm.ugm.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.sciserver.racm.resources.model.ResourceModel;
import org.sciserver.racm.utils.model.RACMBaseModel;

public class PublicGroupModel  extends RACMBaseModel{
	public static class Admin {
		private Long id;
		private GroupRole role;
		private String username, contactEmail;
		public String getContactEmail() {
			return contactEmail;
		}
		public void setContactEmail(String contactEmail) {
			this.contactEmail = contactEmail;
		}
		public Admin(Long id){
			this.id = id;
		}
		public String getUsername() {
			return username;
		}
		public void setUsername(String username) {
			this.username = username;
		}
		public Long getId() {
			return id;
		}
		public GroupRole getRole() {
			return role;
		}
		public void setRole(GroupRole role) {
			this.role = role;
		}
	}
	public static final List<GroupRole> validRoles = Arrays.asList(GroupRole.ADMIN,GroupRole.OWNER);
	
	private String groupName;
	private String description;
	// the role and status the current user has on this group
	private String userRole = null;
	private String userStatus = null;

	private List<Admin> admins = new ArrayList<>();
	
	private List<ResourceModel> resources = new ArrayList<>();
	
	public PublicGroupModel(){}

	public PublicGroupModel(long id)
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
	public List<Admin> getAdmins() {
		return admins;
	}

	public void setAdmins(List<Admin> admins) {
		for(Admin m: admins)
			addAdmin(m);
	}
	public void addAdmin(Admin admin) {
		if(validRoles.contains(admin.getRole()))
			admins.add(admin);
	}

	public List<ResourceModel> getResources() {
		return resources;
	}

	public void addResource(ResourceModel rm) {
		this.resources.add(rm);
	}

	public String getUserRole() {
		return userRole;
	}

	public void setUserRole(String userRole) {
		this.userRole = userRole;
	}

	public String getUserStatus() {
		return userStatus;
	}

	public void setUserStatus(String userStatus) {
		this.userStatus = userStatus;
	}
}
