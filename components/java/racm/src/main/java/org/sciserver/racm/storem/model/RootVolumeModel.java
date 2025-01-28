package org.sciserver.racm.storem.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class RootVolumeModel extends FileServiceResourceModel {
    private final String pathOnFileSystem;
    private final boolean containsSharedVolumes;
    private List<UserVolumeModel> userVolumes;

    @JsonCreator
    public RootVolumeModel(@JsonProperty("id") Long id, @JsonProperty("resourceUUID") String resourceUUID,
            @JsonProperty("name") String name, @JsonProperty("description") String description,
            @JsonProperty("pathOnFileSystem") String pathOnFileSystem,
            @JsonProperty("containsSharedVolumes") Boolean containsSharedVolumes,
            @JsonProperty("userVolumes") List<UserVolumeModel> userVolumes,
            @JsonProperty("allowedActions") List<String> allowedActions) {
        
        this(id,resourceUUID,name,description,pathOnFileSystem,containsSharedVolumes,allowedActions);
        this.userVolumes = userVolumes;
    }

    public RootVolumeModel(Long id, String resourceUUID, String name, String description,
            String pathOnFileSystem, Boolean containsSharedVolumes, List<String> allowedActions) {
        
        super(id,resourceUUID,name,description,allowedActions, null);
        this.pathOnFileSystem = Objects.requireNonNull(pathOnFileSystem);
        this.containsSharedVolumes = Objects.requireNonNull(containsSharedVolumes);
        this.userVolumes = new ArrayList<UserVolumeModel>();
    }

    public String getPathOnFileSystem() {
        return pathOnFileSystem;
    }

    public boolean isContainsSharedVolumes() {
        return containsSharedVolumes;
    }

    public List<UserVolumeModel> getUserVolumes() {
        return userVolumes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getResourceUUID(), getName(), getDescription(), getPathOnFileSystem(), 
                isContainsSharedVolumes(), getUserVolumes(), getAllowedActions());
    }
    
    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof RootVolumeModel)) {
            return false;
        }
        RootVolumeModel castOther = (RootVolumeModel) other;
        return super.equals(castOther) 
                && Objects.equals(pathOnFileSystem, castOther.pathOnFileSystem)
                && Objects.equals(containsSharedVolumes, castOther.containsSharedVolumes)
                && Objects.equals(userVolumes, castOther.userVolumes);
    }

    public void addUserVolume(UserVolumeModel uv) {
        this.userVolumes.add(uv);
    }
}
