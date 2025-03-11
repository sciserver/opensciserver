package org.sciserver.racm.storem.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class RegisteredFileServiceModel {
	private final String identifier;
	private final String name;
	private final String serviceToken;
	private final String description;
	private final String apiEndpoint;
	private final Set<RegisteredRootVolumeModel> rootVolumes;
	private final Set<RegisteredDataVolumeModel> dataVolumes;

	@JsonCreator
	public RegisteredFileServiceModel(
			@JsonProperty("identifier") String identifier,
			@JsonProperty("name") String name,
			@JsonProperty("serviceToken") String serviceToken,
			@JsonProperty("description") String description,
			@JsonProperty("apiEndpoint") String apiEndpoint,
			@JsonProperty("rootVolumes") Collection<RegisteredRootVolumeModel> rootVolumes,
			@JsonProperty("dataVolumes") Collection<RegisteredDataVolumeModel> dataVolumes) {
		this.identifier = Objects.requireNonNull(identifier);
		this.name = Objects.requireNonNull(name);
		this.serviceToken = Objects.requireNonNull(serviceToken);
		this.description = Objects.requireNonNull(description);
		this.apiEndpoint = Objects.requireNonNull(apiEndpoint);
		this.rootVolumes = Collections.unmodifiableSet(
				new HashSet<>(rootVolumes));
		if (dataVolumes != null) {
			this.dataVolumes = Collections.unmodifiableSet(
					new HashSet<>(dataVolumes));
		} else {
			this.dataVolumes = Collections.emptySet();
		}
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

	public Set<RegisteredRootVolumeModel> getRootVolumes() {
		return rootVolumes;
	}

	public String getServiceToken() {
		return serviceToken;
	}

	public Set<RegisteredDataVolumeModel> getDataVolumes() {
		return dataVolumes;
	}

	@Override
	public String toString() {
		return "RegisteredFileServiceModel [identifier=" + identifier + ", name=" + name + ", serviceToken="
				+ serviceToken + ", description=" + description + ", apiEndpoint=" + apiEndpoint + ", rootVolumes="
				+ rootVolumes + ", dataVolumes=" + dataVolumes + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(apiEndpoint, dataVolumes, description, identifier, name, rootVolumes, serviceToken);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RegisteredFileServiceModel other = (RegisteredFileServiceModel) obj;
		return Objects.equals(apiEndpoint, other.apiEndpoint) && Objects.equals(dataVolumes, other.dataVolumes)
				&& Objects.equals(description, other.description) && Objects.equals(identifier, other.identifier)
				&& Objects.equals(name, other.name) && Objects.equals(rootVolumes, other.rootVolumes)
				&& Objects.equals(serviceToken, other.serviceToken);
	}
}
