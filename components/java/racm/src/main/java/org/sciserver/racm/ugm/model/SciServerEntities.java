package org.sciserver.racm.ugm.model;

import java.util.List;

public class SciServerEntities {

	private List<UserInfo> users;
	private List<GroupInfo> groups;
    private List<ServiceAccountModel> services;
	public List<UserInfo> getUsers() {
		return users;
	}
	public void setUsers(List<UserInfo> users) {
		this.users = users;
	}
	public List<GroupInfo> getGroups() {
		return groups;
	}
	public void setGroups(List<GroupInfo> groups) {
		this.groups = groups;
	}
    public List<ServiceAccountModel> getServices() {
        return services;
    }
    public void setServices(List<ServiceAccountModel> services) {
        this.services = services;
    }
	
}

