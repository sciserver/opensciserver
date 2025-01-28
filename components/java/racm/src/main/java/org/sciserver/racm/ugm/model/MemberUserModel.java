package org.sciserver.racm.ugm.model;

import org.sciserver.racm.utils.model.RACMBaseModel;


public class MemberUserModel extends RACMBaseModel {

	private UserInfo user;
	private GroupRole role;
	private MemberStatus status;

	public MemberUserModel(Long id) {
		super(id);
	}
	public MemberUserModel(String id) {
		super(id);
	}
	public MemberUserModel(){

	}

	public UserInfo getUser() {
		return user;
	}
	public String getUsername(){
		return (user == null?null:user.getUsername());
	}
	public void setUser(UserInfo user) {
		this.user = user;
	}

	public Long getUserid() {
		return (user == null?null:user.getId());
	}
	// some no-ops, here so that the spring code ca deal with incoming parameters, even if only user object should be set.
	// UGLY!!!!
	public void setUserid(Long id){}
	public void setUsername(String name){}

	public GroupRole getRole() {
		return role;
	}
	public void setRole(GroupRole role) {
		this.role = role;
	}

	public MemberStatus getStatus() {
		return status;
	}
	public void setStatus(MemberStatus status) {
		this.status =status;
	}

}
