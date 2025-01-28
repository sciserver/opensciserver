package org.sciserver.racm.workspace.model;

import java.util.ArrayList;
import java.util.List;

import org.sciserver.racm.utils.model.RACMBaseModel;

public class WorkspaceGroupModel extends RACMBaseModel{


	private String groupName;
	private String description;

	/** The resources to which this group has access directly or through membership in another group.<br/> */
	private List<WorkspaceResourceModel> groupResources;

	private List<WorkspaceUserModel> memberUsers;

	public WorkspaceGroupModel() {
		super();
	}

	public WorkspaceGroupModel(Long id) {
		super(id);
	}

	public List<WorkspaceResourceModel> getGroupResources() {
		return groupResources;
	}

	public void setGroupResources(List<WorkspaceResourceModel> groupResources) {
		this.groupResources = groupResources;
	}

	public void addResource(WorkspaceResourceModel r){
		if(this.groupResources == null)
			this.groupResources = new ArrayList<>();
		this.groupResources.add(r);
	}
	public void addMember(WorkspaceUserModel r){
		if(this.memberUsers == null)
			this.memberUsers = new ArrayList<>();
		this.memberUsers.add(r);
	}
	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<WorkspaceUserModel> getMemberUsers() {
		return memberUsers;
	}

	public void setMemberUsers(List<WorkspaceUserModel> memberUsers) {
		this.memberUsers = memberUsers;
	}

}
