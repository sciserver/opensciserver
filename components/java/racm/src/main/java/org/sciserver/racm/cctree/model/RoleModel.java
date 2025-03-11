package org.sciserver.racm.cctree.model;

import java.util.List;

import org.sciserver.racm.cctree.model.ActionModel;
import org.sciserver.racm.utils.model.RACMMVCBaseModel;

public class RoleModel  extends RACMMVCBaseModel {
	private long resourceTypeId;
	private String name;
	private String description;
	private List<ActionModel> assignedActions;
	private List<ActionModel> availableActions;

	public RoleModel(){}

	public RoleModel(String roleId){
		super(roleId);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public long getResourceTypeId() {
		return this.resourceTypeId;
	}
	public void setResourceTypeId(long resourceTypeId) {
		this.resourceTypeId = resourceTypeId;
	}

	public List<ActionModel> getAssignedActions() {
		return assignedActions;
	}

	public void setAssignedActions(List<ActionModel> assignedActionModels) {
		this.assignedActions = assignedActionModels;
	}

	public boolean hasAssignedActions() {
		return this.assignedActions != null
				&& !this.assignedActions.isEmpty();
	}

	public List<ActionModel> getAvailableActions() {
		return availableActions;
	}

	public void setAvailableActions(List<ActionModel> availableActionModels) {
		this.availableActions = availableActionModels;
	}

	public boolean hasAvailableActions() {
		return this.availableActions != null
				&& !this.availableActions.isEmpty();
	}

}
