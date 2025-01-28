package org.sciserver.racm.storem.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class RegisterNewFileServiceModel {
	private final String name;
	private final String description;
	private final String apiEndpoint;
	private final List<RegisterNewRootVolumeModel> rootVolumes;
	private final String serviceToken;
	private final String identifier;

	@JsonCreator
	public RegisterNewFileServiceModel(
			@JsonProperty("name") String name,
			@JsonProperty("description") String description,
			@JsonProperty("apiEndpoint") String apiEndpoint,
			@JsonProperty(value="serviceToken", required=false) String serviceToken,
			@JsonProperty(value="identifier", required=false) String identifier,
			@JsonProperty("rootVolumes") List<RegisterNewRootVolumeModel> rootVolumes) {
		this.name = Objects.requireNonNull(name);
		this.description = Objects.requireNonNull(description);
		this.apiEndpoint = Objects.requireNonNull(apiEndpoint);
		this.serviceToken = serviceToken;
		this.identifier = identifier;

		if (rootVolumes == null)
			this.rootVolumes = Collections.emptyList();
		else
			this.rootVolumes = Collections.unmodifiableList(rootVolumes);
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

	public List<RegisterNewRootVolumeModel> getRootVolumes() {
		return rootVolumes;
	}

	public Optional<String> getServiceToken() {
		return Optional.ofNullable(serviceToken);
	}

	public Optional<String> getIdentifier() {
		return Optional.ofNullable(identifier);
	}

	@Override
	public String toString() {
		return "RegisterNewFileServiceModel [name=" + name + ", description=" + description + ", apiEndpoint="
				+ apiEndpoint + ", rootVolumes=" + rootVolumes + ", serviceToken=" + serviceToken + ", identifier="
				+ identifier + "]";
	}
}
