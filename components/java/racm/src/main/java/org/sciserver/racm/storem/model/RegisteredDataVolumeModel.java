package org.sciserver.racm.storem.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RegisteredDataVolumeModel {
	private final long id;
	private final String name;
	private final String description;
	private final String displayName;
	private final String pathOnFileSystem;
	private final String url;

	@JsonCreator
	public RegisteredDataVolumeModel(
			@JsonProperty("id") Long id,
			@JsonProperty("name") String name,
			@JsonProperty("description") String description,
			@JsonProperty("displayName") String displayName,
			@JsonProperty("pathOnFileSystem") String pathOnFileSystem,
			@JsonProperty("url") String url) {
		this.id = Objects.requireNonNull(id);
		this.name = Objects.requireNonNull(name);
		this.description = description;
		this.displayName = displayName;
		this.pathOnFileSystem = Objects.requireNonNull(pathOnFileSystem);
		this.url = url;
	}

	public long getId() {
		return id;
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

	@Override
	public int hashCode() {
		return Objects.hash(description, displayName, id, name, pathOnFileSystem, url);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RegisteredDataVolumeModel other = (RegisteredDataVolumeModel) obj;
		return Objects.equals(description, other.description) && Objects.equals(displayName, other.displayName)
				&& id == other.id && Objects.equals(name, other.name)
				&& Objects.equals(pathOnFileSystem, other.pathOnFileSystem) && Objects.equals(url, other.url);
	}

	@Override
	public String toString() {
		return "RegisteredDataVolumeModel [id=" + id + ", name=" + name + ", description=" + description + ", path="
				+ pathOnFileSystem + ", url=" + url + "]";
	}
}
