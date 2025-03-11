package org.sciserver.racm.storem.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RegisterNewDataVolumeModel {
	private final String name;
	private final String description;
	private final String displayName;
	private final String pathOnFileSystem;
	private final String url;

	@JsonCreator
	public RegisterNewDataVolumeModel(
			@JsonProperty("name") String name,
			@JsonProperty("description") String description,
			@JsonProperty("displayName") String displayName,
			@JsonProperty("pathOnFileSystem") String pathOnFileSystem,
			@JsonProperty("url") String url) {
		this.name = Objects.requireNonNull(name);
		this.description = description;
		this.displayName = displayName;
		this.pathOnFileSystem = Objects.requireNonNull(pathOnFileSystem);
		this.url = url;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getPathOnFileSystem() {
		return pathOnFileSystem;
	}

	public String getUrl() {
		return url;
	}
}
