package org.sciserver.racm.storem.model;

import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdatedRootVolumeInfo {
	private final Optional<String> name;
	private final Optional<String> description;
	private final Optional<String> pathOnFileSystem;
	private final Optional<Boolean> containsSharedVolumes;

	@JsonCreator
	public UpdatedRootVolumeInfo(
			@JsonProperty("name") Optional<String> name,
			@JsonProperty("description") Optional<String> description,
			@JsonProperty("pathOnFileSystem") Optional<String> pathOnFileSystem,
			@JsonProperty("containsSharedVolumes") Optional<Boolean> containsSharedVolumes) {
		this.name = Objects.requireNonNull(name);
		this.description = Objects.requireNonNull(description);
		this.pathOnFileSystem = Objects.requireNonNull(pathOnFileSystem);
		this.containsSharedVolumes = Objects.requireNonNull(containsSharedVolumes);
	}

	public Optional<String> getName() {
		return name;
	}

	public Optional<String> getDescription() {
		return description;
	}

	public Optional<String> getPathOnFileSystem() {
		return pathOnFileSystem;
	}

	public Optional<Boolean> getContainsSharedVolumes() {
		return containsSharedVolumes;
	}
}
