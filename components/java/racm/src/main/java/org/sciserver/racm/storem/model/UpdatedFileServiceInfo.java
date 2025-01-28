package org.sciserver.racm.storem.model;

import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdatedFileServiceInfo {
	private final Optional<String> name;
	private final Optional<String> description;
	private final Optional<String> apiEndpoint;

	@JsonCreator
	public UpdatedFileServiceInfo(
			@JsonProperty("name") Optional<String> name,
			@JsonProperty("description") Optional<String> description,
			@JsonProperty("apiEndpoint") Optional<String> apiEndpoint) {
		this.name = Objects.requireNonNull(name);
		this.description = Objects.requireNonNull(description);
		this.apiEndpoint = Objects.requireNonNull(apiEndpoint);
	}

	public Optional<String> getName() {
		return name;
	}

	public Optional<String> getDescription() {
		return description;
	}

	public Optional<String> getApiEndpoint() {
		return apiEndpoint;
	}
}
