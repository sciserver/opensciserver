package org.sciserver.racm.storem.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MinimalFileServiceModel {
	private final String identifier;
	private final String name;
	private final String description;
	private final String apiEndpoint;

	@JsonCreator
	public MinimalFileServiceModel(
			@JsonProperty("identifier") String identifier,
			@JsonProperty("name") String name,
			@JsonProperty("description") String description,
			@JsonProperty("apiEndpoint") String apiEndpoint) {
		this.identifier = Objects.requireNonNull(identifier);
		this.name = Objects.requireNonNull(name);
		this.description = Objects.requireNonNull(description);
		this.apiEndpoint = Objects.requireNonNull(apiEndpoint);
	}

	public String getIdentifier() {
		return identifier;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getApiEndpoint() {
		return apiEndpoint;
	}

	@Override
	public String toString() {
		return "RegisteredFileServiceModel [identifier=" + identifier + ", name=" + name +
				", description=" + description + ", apiEndpoint=" + apiEndpoint + "]";
	}

	@Override
	public boolean equals(final Object other) {
		if (!(other instanceof MinimalFileServiceModel)) {
			return false;
		}
		MinimalFileServiceModel castOther = (MinimalFileServiceModel) other;
		return Objects.equals(identifier, castOther.identifier)
				&& Objects.equals(name, castOther.name)
				&& Objects.equals(description, castOther.description)
				&& Objects.equals(apiEndpoint, castOther.apiEndpoint);
	}

	@Override
	public int hashCode() {
		return Objects.hash(identifier, name, description, apiEndpoint);
	}
}
