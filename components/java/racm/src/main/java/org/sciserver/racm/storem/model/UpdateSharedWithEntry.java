package org.sciserver.racm.storem.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.sciserver.racm.resources.model.SciServerEntityType;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdateSharedWithEntry {
	private final Optional<Long> id;
	private final Optional<String> name;
	private final SciServerEntityType type;
	private final List<String> allowedActions;

	public UpdateSharedWithEntry(
			@JsonProperty("id") Optional<Long> id,
			@JsonProperty("name") Optional<String> name,
			@JsonProperty("type") SciServerEntityType type,
			@JsonProperty("allowedActions") List<String> allowedActions) {
		this.id = Objects.requireNonNull(id);
		this.name = Objects.requireNonNull(name);
		this.type = Objects.requireNonNull(type);
		this.allowedActions = Collections.unmodifiableList(Objects.requireNonNull(allowedActions));
	}

	public Optional<Long> getId() {
		return id;
	}

	public Optional<String> getName() {
		return name;
	}

	public SciServerEntityType getType() {
		return type;
	}

	public List<String> getAllowedActions() {
		return allowedActions;
	}

	@Override
	public boolean equals(final Object other) {
		if (!(other instanceof UpdateSharedWithEntry)) {
			return false;
		}
		UpdateSharedWithEntry castOther = (UpdateSharedWithEntry) other;
		return Objects.equals(id, castOther.id) && Objects.equals(name, castOther.name)
				&& Objects.equals(type, castOther.type)
				&& Objects.equals(allowedActions, castOther.allowedActions);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, type, allowedActions);
	}

	@Override
	public String toString() {
		return "UpdateSharedWithEntry [id=" + id + ", name=" + name +
				", type=" + type + ", allowedActions=" + allowedActions + "]";
	}
}
