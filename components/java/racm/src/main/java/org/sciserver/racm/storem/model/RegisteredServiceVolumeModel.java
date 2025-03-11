package org.sciserver.racm.storem.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import java.util.Optional;

public final class RegisteredServiceVolumeModel {
    private final Long id;
	private final String name;
	private final String volumeUUID;
	private final String description;
	private final String relativePath;
	private final String owner;
	private final String serviceUUID;
    private final String owningResourceUUID;
    private final String ownership;
    private final String usage;

	@JsonCreator
	public RegisteredServiceVolumeModel(
	        @JsonProperty("id") Long id,
	        @JsonProperty("volumeUUID") String volumeUUID,
			@JsonProperty("name") String name,
			@JsonProperty("description") String description,
			@JsonProperty("relativePath") String relativePath,
			@JsonProperty("owner") String owner,
			@JsonProperty("serviceUUID") String serviceUUID,
			@JsonProperty("owningResourceUUID") String owningResourceUUID,
			@JsonProperty("ownership") String ownership,
	        @JsonProperty("usage") String usage) {
	    this.id = Objects.requireNonNull(id);
	    this.volumeUUID = Objects.requireNonNull(volumeUUID);
		this.name = Objects.requireNonNull(name);
		this.description = Objects.requireNonNull(description);
		this.relativePath = Objects.requireNonNull(relativePath);
		this.owner = Objects.requireNonNull(owner);
        this.serviceUUID = Objects.requireNonNull(serviceUUID);
        this.owningResourceUUID = Objects.requireNonNull(owningResourceUUID);
        this.ownership = Objects.requireNonNull(ownership);
        this.usage =Objects.requireNonNull(usage);
	}

	public String getRelativePath() {
		return relativePath;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

    public Long getId() {
        return id;
    }

    public String getVolumeUUID() {
		return volumeUUID;
	}

	public String getOwningResourceUUID() {
        return owningResourceUUID;
    }

    public String getOwner() {
        return owner;
    }

    public String getServiceUUID() {
        return serviceUUID;
    }
    
	public String getOwnership() {
		return ownership;
	}
    
	public String getUsage() {
		return usage;
	}
}
