package org.sciserver.racm.resourcecontext.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AssociatedResourceModel {
	private final String usage;
	private final String resourceDescription;
	private final boolean owned;
	private final String resourceUUID;
	private String resourceType;
	private List<String> actions;

	@JsonCreator
	public AssociatedResourceModel(
			@JsonProperty("usage") String usage,
			@JsonProperty("resourceDescription") String resourceDescription,
			@JsonProperty("owned") boolean owned,
			@JsonProperty("resourceUUID") String resourceUUID) {
		this.usage = usage;
		this.owned = owned;
		this.resourceDescription = resourceDescription;
		this.resourceUUID = resourceUUID;
		this.actions = new ArrayList<String>();
	}
	public String getUsage() {
		return usage;
	}
	public boolean isOwned() {
		return owned;
	}
	public String getResourceUUID() {
		return resourceUUID;
	}
	public String getResourceDescription() {
		return resourceDescription;
	}
	public String getResourceType() {
		return resourceType;
	}
	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}
	public List<String> getActions() {
		return actions;
	}
	public void addAction(String action){
		this.actions.add(action);
	}
}
