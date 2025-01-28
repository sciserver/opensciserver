package org.sciserver.racm.storem.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class FileServiceModel {
	private final String identifier;
	private final String name;
	private final String description;
	private final String apiEndpoint;
	private final List<RootVolumeModel> rootVolumes;
	private final List<DataVolumeModel> dataVolumes;

	@JsonCreator
	public FileServiceModel(
			@JsonProperty("identifier") String identifier,
			@JsonProperty("name") String name,
			@JsonProperty("description") String description,
			@JsonProperty("apiEndpoint") String apiEndpoint,
			@JsonProperty("rootVolumes") List<RootVolumeModel> rootVolumes,
			@JsonProperty("dataVolumes") List<DataVolumeModel> dataVolumes) {
		this.identifier = Objects.requireNonNull(identifier);
		this.name = Objects.requireNonNull(name);
		this.description = Objects.requireNonNull(description);
		this.apiEndpoint = Objects.requireNonNull(apiEndpoint);
		this.rootVolumes = Collections.unmodifiableList(rootVolumes);
		this.dataVolumes = Optional.ofNullable(dataVolumes).map(Collections::unmodifiableList)
				.orElse(Collections.emptyList());
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

	public List<RootVolumeModel> getRootVolumes() {
		return rootVolumes;
	}

	public List<DataVolumeModel> getDataVolumes() {
		return dataVolumes;
	}

	@Override
	public int hashCode() {
		return Objects.hash(apiEndpoint, dataVolumes, description, identifier, name, rootVolumes);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FileServiceModel other = (FileServiceModel) obj;
		return Objects.equals(apiEndpoint, other.apiEndpoint) && Objects.equals(dataVolumes, other.dataVolumes)
				&& Objects.equals(description, other.description) && Objects.equals(identifier, other.identifier)
				&& Objects.equals(name, other.name) && Objects.equals(rootVolumes, other.rootVolumes);
	}

	@Override
	public String toString() {
		return "FileServiceModel [identifier=" + identifier + ", name=" + name + ", description=" + description
				+ ", apiEndpoint=" + apiEndpoint + ", rootVolumes=" + rootVolumes + ", dataVolumes=" + dataVolumes
				+ "]";
	}
}
