package org.sciserver.racm.storem.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public final class RegisterNewRootVolumeModel {
	private final String pathOnFileSystem;
	private final String description;
	private final String name;
	private final boolean containsSharedVolumes;

	@JsonCreator
	public RegisterNewRootVolumeModel(
			@JsonProperty("name") String name,
			@JsonProperty("description") String description,
			@JsonProperty("pathOnFileSystem") String pathOnFileSystem,
			@JsonProperty("containsSharedVolumes") boolean containsSharedVolumes) {
		this.pathOnFileSystem = pathOnFileSystem;
		this.description = description;
		this.name = name;
		this.containsSharedVolumes = containsSharedVolumes;
	}

	public String getPathOnFileSystem() {
		return pathOnFileSystem;
	}

	public String getDescription() {
		return description;
	}

	public String getName() {
		return name;
	}

	public boolean isContainsSharedVolumes() {
		return containsSharedVolumes;
	}

	@Override
	public boolean equals(final Object other) {
		if (!(other instanceof RegisterNewRootVolumeModel)) {
			return false;
		}
		RegisterNewRootVolumeModel castOther = (RegisterNewRootVolumeModel) other;
		return Objects.equals(pathOnFileSystem, castOther.pathOnFileSystem)
				&& Objects.equals(description, castOther.description) && Objects.equals(name, castOther.name)
				&& Objects.equals(containsSharedVolumes, castOther.containsSharedVolumes);
	}

	@Override
	public int hashCode() {
		return Objects.hash(pathOnFileSystem, description, name, containsSharedVolumes);
	}

	@Override
	public String toString() {
		return "RegisterNewRootVolumeModel [pathOnFileSystem=" + pathOnFileSystem + ", description=" + description
				+ ", name=" + name + ", containsSharedVolumes=" + containsSharedVolumes + "]";
	}
}
