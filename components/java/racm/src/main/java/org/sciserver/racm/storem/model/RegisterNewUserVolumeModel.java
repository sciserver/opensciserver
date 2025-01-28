package org.sciserver.racm.storem.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import java.util.Optional;

public final class RegisterNewUserVolumeModel {
	private final String name;
	private final String description;
	private final String relativePath;
	private final Optional<String> owner;

	public RegisterNewUserVolumeModel(String name, String description, String relativePath, String owner) {
		this(name, description, relativePath, Optional.of(owner));
	}

	@JsonCreator
	public RegisterNewUserVolumeModel(
			@JsonProperty("name") String name,
			@JsonProperty("description") String description,
			@JsonProperty("relativePath") String relativePath,
			@JsonProperty("owner") Optional<String> owner) {
		this.name = Objects.requireNonNull(name);
		this.description = Objects.requireNonNull(description);
		this.relativePath = Objects.requireNonNull(relativePath);
		this.owner = Objects.requireNonNull(owner);
	}

	public String getRelativePath() {
		return relativePath;
	}

	public Optional<String> getOwner() {
		return owner;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public boolean equals(final Object other) {
		if (!(other instanceof RegisterNewUserVolumeModel)) {
			return false;
		}
		RegisterNewUserVolumeModel castOther = (RegisterNewUserVolumeModel) other;
		return Objects.equals(name, castOther.name) && Objects.equals(description, castOther.description)
				&& Objects.equals(relativePath, castOther.relativePath) && Objects.equals(owner, castOther.owner);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, description, relativePath, owner);
	}

	@Override
	public String toString() {
		return "RegisterNewUserVolumeModel [name=" + name + ", description=" + description + ", relativePath="
				+ relativePath + ", owner=" + owner + "]";
	}
}
