package org.sciserver.racm.storem.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.sciserver.racm.resources.model.NewSharedWithEntity;

/**
 * This class represents a data structure defining how a service may create a user volume that is owned by a resource of another service.<br/>
 * 
 * 
 * 
 * @author Gerard Lemson
 *
 */
public final class RegisterNewServiceVolumeModel {
    /** name of the user volume */
    private final String name;
    /** description of the servicevolume */
    private final String description;
    /** */
    private final String relativePath;
    /** 
     * possible share definition of the new user volume: user with possible actions<br/>
     */
    private final List<NewSharedWithEntity> shares;

    public static final String SERVICE_TOKEN_HEADER = "X-Service-Token";
    /**
     * uuid of the service (resourcecontext) whose resource owns the new volume.<br/>
     * Or should this be the serviceToken?
     */
    private final String serviceToken;
    /**
     * uuid of the resource on the service that will own the new volume
     */
    private final String owningResourceUUID;
    
    private final String usage;

    @JsonCreator
    public RegisterNewServiceVolumeModel(
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("relativePath") String relativePath,
            @JsonProperty("shares") List<NewSharedWithEntity> shares,
            @JsonProperty("serviceToken") String serviceToken,
            @JsonProperty("owningResourceUUID") String owningResourceUUID,
            @JsonProperty("usage") String usage) {
        this.name = Objects.requireNonNull(name);
        this.description = Objects.requireNonNull(description);
        this.relativePath = Objects.requireNonNull(relativePath);
        this.shares = shares;
        this.serviceToken = Objects.requireNonNull(serviceToken);
        this.owningResourceUUID = Objects.requireNonNull(owningResourceUUID);
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

    public String getServiceToken() {
        return serviceToken;
    }

    public String getOwningResourceUUID() {
        return owningResourceUUID;
    }

    public List<NewSharedWithEntity> getShares() {
        return shares;
    }

	public String getUsage() {
		return usage;
	}
}
