package org.sciserver.racm.resourcecontext.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RegisteredResourceModel {
	private final long id;
	private final String publisherDID;
	private final String uuid;
	private final String name;
	private final String description;
	private final String resourceTypeName;

	@JsonCreator
	public RegisteredResourceModel(@JsonProperty("id") long id,
		@JsonProperty("publisherDID") String publisherDID,
		@JsonProperty("uuid") String uuid,
		@JsonProperty("name") String name,
		@JsonProperty("description") String description,
		@JsonProperty("resourceTypeName") String resourceTypeName) {

		this.id = id;
		this.publisherDID = publisherDID;
		this.uuid = uuid;
		this.name = name;
		this.description = description;
		this.resourceTypeName = resourceTypeName;
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
}
