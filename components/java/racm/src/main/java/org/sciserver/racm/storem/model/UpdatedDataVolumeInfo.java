package org.sciserver.racm.storem.model;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdatedDataVolumeInfo {
	private final Optional<String> name;
	private final Optional<String> description;
	private final Optional<String> displayName;
	private final Optional<String> pathOnFileSystem;
	private final Optional<String> url;

	@JsonCreator
	public UpdatedDataVolumeInfo(
			@JsonProperty("name") Optional<String> name,
			@JsonProperty("description") Optional<String> description,
			@JsonProperty("displayName") Optional<String> displayName,
			@JsonProperty("pathOnFileSystem") Optional<String> pathOnFileSystem,
			@JsonProperty("url") Optional<String> url) {
		this.name = name;
		this.description = description;
		this.displayName = displayName;
		this.pathOnFileSystem = pathOnFileSystem;
		this.url = url;
	}

	public Optional<String> getName() {
		return name;
	}
	public Optional<String> getDescription() {
		return description;
	}

	public Optional<String> getDisplayName() {
		return displayName;
	}

	public Optional<String> getPathOnFileSystem() {
		return pathOnFileSystem;
	}

	public Optional<String> getUrl() {
		return url;
	}
}
