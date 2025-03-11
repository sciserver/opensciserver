package org.sciserver.racm.resourcecontext.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ResourceFromUserPerspectiveModel {
	private final long id;
	private final String publisherDID;
	private final String uuid;
	private final String name;
	private final String description;
	private final String resourceTypeName;
	private final Set<ActionModel> allowedActions;
	private final Set<AssociatedResourceModel> associatedResources;
	private final Set<AssociatedSciserverEntityModel> associatedSciserverEntities;
	@JsonCreator
	public ResourceFromUserPerspectiveModel(@JsonProperty("id") long id, 
			@JsonProperty("publisherDID") String publisherDID,
			@JsonProperty("uuid") String uuid,
			@JsonProperty("name") String name, @JsonProperty("description") String description, @JsonProperty("resourceTypeName")String resourceTypeName,
			@JsonProperty("allowedActions") Collection<ActionModel> allowedActions,
			@JsonProperty("associatedResources") Collection<AssociatedResourceModel> associatedResources,
			@JsonProperty("associatedSciserverEntities") Collection<AssociatedSciserverEntityModel> associatedSciserverEntities) {
		this.id = id;
		this.publisherDID = publisherDID;
		this.uuid = uuid;
		this.name = name;
		this.description = description;
		this.resourceTypeName = resourceTypeName;
		this.allowedActions = new HashSet<>(allowedActions);
		this.associatedResources = new HashSet<>(associatedResources);
		this.associatedSciserverEntities = new HashSet<>(associatedSciserverEntities);
	}
	public long getId() {
		return id;
	}
	public String getPublisherDID() {
		return publisherDID;
	}
	public String getUUID() {
		return uuid;
	}
	public String getName() {
		return name;
	}
	public String getDescription() {
		return description;
	}
	public String getResourceTypeName() {
		return resourceTypeName;
	}
	public Set<ActionModel> getAllowedActions() {
		return allowedActions;
	}
	public Set<AssociatedResourceModel> getAssociatedResources() {
		return associatedResources;
	}
	public Set<AssociatedSciserverEntityModel> getAssociatedSciserverEntities() {
		return associatedSciserverEntities;
	}
}
