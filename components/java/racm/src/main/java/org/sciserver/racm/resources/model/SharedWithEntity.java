package org.sciserver.racm.resources.model;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SharedWithEntity extends NewSharedWithEntity {
	private final long id;

	public SharedWithEntity(
			@JsonProperty("id") Long id,
			@JsonProperty("name") String name,
			@JsonProperty("type") SciServerEntityType type,
			@JsonProperty("allowedActions") List<String> allowedActions) {
	    super(name,type,allowedActions);
		this.id = Objects.requireNonNull(id);
	}
    public SharedWithEntity(Long id, String name, SciServerEntityType type) {
        super(name,type);
        this.id = Objects.requireNonNull(id);
    }

	public long getId() {
		return id;
	}
}
