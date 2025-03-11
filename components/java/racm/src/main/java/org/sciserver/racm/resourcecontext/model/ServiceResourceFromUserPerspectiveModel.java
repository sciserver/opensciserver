package org.sciserver.racm.resourcecontext.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.sciserver.racm.utils.model.NativeQueryResult;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ServiceResourceFromUserPerspectiveModel {
	private long id;
	private String publisherDID;
	private String uuid;
	private String name;
	private String description;
	private String resourceTypeName;
	private Set<ActionModel> allowedActions;
	private Set<AssociatedResourceModel> associatedResources;
	private Set<AssociatedSciserverEntityModel> associatedSciserverEntities;

	@JsonCreator
	public ServiceResourceFromUserPerspectiveModel(@JsonProperty("id") long id,
			@JsonProperty("publisherDID") String publisherDID, @JsonProperty("uuid") String uuid,
			@JsonProperty("name") String name, @JsonProperty("description") String description,
			@JsonProperty("resourceTypeName") String resourceTypeName,
			@JsonProperty("allowedActions") Set<ActionModel> allowedActions,
			@JsonProperty("associatedResources") Set<AssociatedResourceModel> associatedResources,
			@JsonProperty("associatedSciserverEntities") Set<AssociatedSciserverEntityModel> associatedSciserverEntities) {
		this.id = id;
		this.publisherDID = publisherDID;
		this.uuid = uuid;
		this.name = name;
		this.description = description;
		this.resourceTypeName = resourceTypeName;
		this.allowedActions=allowedActions;
		this.associatedResources = associatedResources;
		this.associatedSciserverEntities = associatedSciserverEntities;
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