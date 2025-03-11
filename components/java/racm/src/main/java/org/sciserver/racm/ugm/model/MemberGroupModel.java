package org.sciserver.racm.ugm.model;

import org.sciserver.racm.utils.model.RACMBaseModel;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class MemberGroupModel extends RACMBaseModel {
	
	private GroupInfo group;
	
	public MemberGroupModel(Long id) {
		super(id);
		// TODO Auto-generated constructor stub
	}
	public MemberGroupModel(String id) {
		super(id);
		// TODO Auto-generated constructor stub
	}
	public MemberGroupModel(){
		
	}
	public GroupInfo getGroup() {
		return group;
	}
	@JsonIgnore
	public String getGroupName(){
		return (group == null?null:group.getGroupName());
	}
	public void setGroup(GroupInfo _group) {
		this.group = _group;
	}
	@JsonIgnore
	public Long getGroupId() {
		return (group == null?null:group.getId());
	}

}
