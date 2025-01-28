package org.sciserver.racm.storem.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.sciserver.racm.resources.model.SharedWithEntity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class UserVolumeModel extends FileServiceResourceModel{
    private final String relativePath;
    private final String owner;

    @JsonCreator
    public UserVolumeModel(@JsonProperty("id") Long id, @JsonProperty("resourceUUID") String resourceUUID,
            @JsonProperty("name") String name, @JsonProperty("description") String description,
            @JsonProperty("relativePath") String relativePath, @JsonProperty("owner") String owner,
            @JsonProperty("allowedActions") List<String> allowedActions,
            @JsonProperty("sharedWith") List<SharedWithEntity> sharedWith) {
        super(id,resourceUUID,name,description,allowedActions,sharedWith);
        this.relativePath = Objects.requireNonNull(relativePath);
        this.owner = Objects.requireNonNull(owner);
    }

    public UserVolumeModel(Long id, String resourceUUID, String name, String description,
            String relativePath, String owner, List<String> allowedActions) {
        this(id,resourceUUID,name,description,relativePath, owner, allowedActions, new ArrayList<SharedWithEntity>());
    }

    public String getRelativePath() {
        return relativePath;
    }

    public String getOwner() {
        return owner;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getResourceUUID(), getName(), getDescription(), getRelativePath(), getOwner(), getAllowedActions(), getSharedWith());
    }
    
    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof UserVolumeModel)) {
            return false;
        }
        UserVolumeModel castOther = (UserVolumeModel) other;
        return super.equals(castOther) &&
               Objects.equals(relativePath, castOther.relativePath) && Objects.equals(owner, castOther.owner);
    }
}
