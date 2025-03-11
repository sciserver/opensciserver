package org.sciserver.racm.storem.model;

import edu.jhu.user.User;
import java.util.Objects;


public final class RegisterNewUserVolumeWithOwnerModel {
	private final String name;
	private final String description;
	private final String relativePath;
	private final User owner;

	public RegisterNewUserVolumeWithOwnerModel(String name, String description, String relativePath, User owner) {
		this.name = Objects.requireNonNull(name);
		this.description = Objects.requireNonNull(description);
		this.relativePath = Objects.requireNonNull(relativePath);
		this.owner = Objects.requireNonNull(owner);
	}

	public String getRelativePath() {
		return relativePath;
	}

	public User getOwner() {
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
		if (!(other instanceof RegisterNewUserVolumeWithOwnerModel)) {
			return false;
		}
		RegisterNewUserVolumeWithOwnerModel castOther = (RegisterNewUserVolumeWithOwnerModel) other;
		return Objects.equals(name, castOther.name) && Objects.equals(description, castOther.description)
				&& Objects.equals(relativePath, castOther.relativePath) && Objects.equals(owner, castOther.owner);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, description, relativePath, owner);
	}

	@Override
	public String toString() {
		return "RegisterNewUserVolumeWithOwnerModel [name=" + name + ", description=" + description + ", relativePath="
				+ relativePath + ", owner=" + owner + "]";
	}
}
