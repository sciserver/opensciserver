package org.sciserver.racm.workspace.model;

import java.util.ArrayList;
import java.util.List;

/**
 * All the groups a user is a member of with their contents.<br/>
 * @author gerard
 *
 */
public class WorkspaceGroupsModel {

	private String username;
	private String userid;
	private List<WorkspaceGroupModel> groups;
	public WorkspaceGroupsModel(){
		this.groups = new ArrayList<>();
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getUserid() {
		return userid;
	}
	public void setUserid(String userid) {
		this.userid = userid;
	}
	public List<WorkspaceGroupModel> getGroups() {
		return groups;
	}
	public void setGroups(List<WorkspaceGroupModel> groups) {
		this.groups = groups;
	}
	public void addGroup(WorkspaceGroupModel wgm){
		this.groups.add(wgm);
	}
}
