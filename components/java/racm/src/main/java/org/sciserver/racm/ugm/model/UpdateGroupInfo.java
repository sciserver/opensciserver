package org.sciserver.racm.ugm.model;

import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdateGroupInfo {
	private final String name;
	private final String description;
	private GroupAccessibility accessibility;

	@JsonCreator
	public UpdateGroupInfo(
			@JsonProperty("name") String name,
			@JsonProperty("description") String description,
			@JsonProperty("accessibility") GroupAccessibility accessibility) {
		this.name = name;
		this.description = description;
		this.accessibility = accessibility;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public GroupAccessibility getAccessibility() {
		return accessibility;
	}
}
