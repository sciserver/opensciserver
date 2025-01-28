package org.sciserver.racm.cctree.model;

import java.util.List;

import org.sciserver.racm.cctree.model.ActionModel;
import org.sciserver.racm.cctree.model.RoleModel;
import org.sciserver.racm.utils.model.RACMMVCBaseModel;

public class ResourceTypeModel extends RACMMVCBaseModel {
	private long contextClassId;
	private String name;
	private String description;
	private List<RoleModel> roles;
	private List<ActionModel> actions;

	public ResourceTypeModel(){}

	public ResourceTypeModel(String id){
		super(id);
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public long getContextClassId() {
		return contextClassId;
	}
	public void setContextClassId(long contextClassId) {
		this.contextClassId = contextClassId;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}


	public List<RoleModel> getRoles() {
		return roles;
	}

	public void setRoles(List<RoleModel> roles) {
		this.roles = roles;
	}

	public List<ActionModel> getActions() {
		return actions;
	}

	public void setActions(List<ActionModel> actions) {
		this.actions = actions;
	}



}
