package org.sciserver.racm.resourcecontext.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class NewResourceModel {
	private final String publisherDID;
	private final String name;
	private final String description;
	private final String resourceTypeName;

	@JsonCreator
	public NewResourceModel(
			@JsonProperty("publisherDID") String publisherDID,
			@JsonProperty("name") String name,
			@JsonProperty("description") String description,
			@JsonProperty("resourceTypeName") String resourceTypeName) {
		this.publisherDID = publisherDID;
		this.name = name;
		this.description = description;
		this.resourceTypeName = resourceTypeName;
	}
	public String getPublisherDID() {
		return publisherDID;
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

}
