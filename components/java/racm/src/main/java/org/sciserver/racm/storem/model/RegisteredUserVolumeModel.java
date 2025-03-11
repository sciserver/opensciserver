package org.sciserver.racm.storem.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class RegisteredUserVolumeModel {
	private final long id;
	private final String name;
	private final String description;
	private final String relativePath;
	private final String owner;

	@JsonCreator
	public RegisteredUserVolumeModel(
			@JsonProperty("id") Long id,
			@JsonProperty("name") String name,
			@JsonProperty("description") String description,
			@JsonProperty("relativePath") String relativePath,
			@JsonProperty("owner") String owner) {
		this.id = Objects.requireNonNull(id);
		this.name = Objects.requireNonNull(name);
		this.description = Objects.requireNonNull(description);
		this.relativePath = Objects.requireNonNull(relativePath);
		this.owner = Objects.requireNonNull(owner);
	}

	public String getRelativePath() {
		return relativePath;
	}

	public String getOwner() {
		return owner;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public long getId() {
		return id;
	}

	@Override
	public boolean equals(final Object other) {
		if (!(other instanceof RegisteredUserVolumeModel)) {
			return false;
		}
		RegisteredUserVolumeModel castOther = (RegisteredUserVolumeModel) other;
		return Objects.equals(id, castOther.id) && Objects.equals(name, castOther.name)
				&& Objects.equals(description, castOther.description)
				&& Objects.equals(relativePath, castOther.relativePath) && Objects.equals(owner, castOther.owner);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, description, relativePath, owner);
	}

	@Override
	public String toString() {
		return "RegisteredUserVolumeModel [id=" + id + ", name=" + name + ", description=" + description
				+ ", relativePath=" + relativePath + ", owner=" + owner + "]";
	}
}
