package org.sciserver.racm.resourcecontext.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PrivilegeModel {
	private final String actionName;
	private final Long sciserverEntityId;
	private final String sciserverEntityName;
	private final String sciserverEntityType;
	@JsonCreator
	public PrivilegeModel(
			@JsonProperty("name") String actionName,
			@JsonProperty(value="sciserverEntityId", required=false) Long sciserverEntityId,
			@JsonProperty(value="sciserverEntityName", required=false) String sciserverEntityName,
			@JsonProperty(value="sciserverEntityType", required=false) String sciserverEntityType) {
		this.actionName = actionName;
		this.sciserverEntityId = sciserverEntityId;
		this.sciserverEntityName = sciserverEntityName;
		this.sciserverEntityType = sciserverEntityType;
	}
	public String getActionName() {
		return actionName;
	}
	public Long getSciserverEntityId() {
		return sciserverEntityId;
	}
	public String getSciserverEntityName() {
		return sciserverEntityName;
	}
	public String getSciserverEntityType() {
		return sciserverEntityType;
	}
}
