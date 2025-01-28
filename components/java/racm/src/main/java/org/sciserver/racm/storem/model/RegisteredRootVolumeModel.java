package org.sciserver.racm.storem.model;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class RegisteredRootVolumeModel {
	private final String name;
	private final String description;
	private final String pathOnFileSystem;
	private final Set<RegisteredUserVolumeModel> userVolumes = new HashSet<>();
	private final boolean containsSharedVolumes;

	@JsonCreator
	public RegisteredRootVolumeModel(
			@JsonProperty("name") String name,
			@JsonProperty("description") String description,
			@JsonProperty("pathOnFileSystem") String pathOnFileSystem,
			@JsonProperty(value="containsSharedVolumes", required=true) boolean containsSharedVolumes) {
		this.name = Objects.requireNonNull(name);
		this.description = Objects.requireNonNull(description);
		this.pathOnFileSystem = Objects.requireNonNull(pathOnFileSystem);
		this.containsSharedVolumes = containsSharedVolumes;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getPathOnFileSystem() {
		return pathOnFileSystem;
	}

	public Set<RegisteredUserVolumeModel> getUserVolumes() {
		return userVolumes;
	}

	public boolean isContainsSharedVolumes() {
		return containsSharedVolumes;
	}

	@Override
	public boolean equals(final Object other) {
		if (!(other instanceof RegisteredRootVolumeModel)) {
			return false;
		}
		RegisteredRootVolumeModel castOther = (RegisteredRootVolumeModel) other;
		return Objects.equals(name, castOther.name) && Objects.equals(description, castOther.description)
				&& Objects.equals(pathOnFileSystem, castOther.pathOnFileSystem)
				&& Objects.equals(userVolumes, castOther.userVolumes)
				&& Objects.equals(containsSharedVolumes, castOther.containsSharedVolumes);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, description, pathOnFileSystem, userVolumes, containsSharedVolumes);
	}

	@Override
	public String toString() {
		return "RegisteredRootVolumeModel [name=" + name + ", description=" + description + ", pathOnFileSystem="
				+ pathOnFileSystem + ", userVolumes=" + userVolumes + ", containsSharedVolumes=" + containsSharedVolumes
				+ "]";
	}
}
