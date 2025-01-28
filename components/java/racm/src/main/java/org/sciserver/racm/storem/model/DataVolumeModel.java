package org.sciserver.racm.storem.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.sciserver.racm.resources.model.SharedWithEntity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class DataVolumeModel extends FileServiceResourceModel {
    private final String displayName;
    private final String pathOnFileSystem;
    private final String url;

    @JsonCreator
    public DataVolumeModel(@JsonProperty("id") Long id, @JsonProperty("resourceUUID") String resourceUUID,
            @JsonProperty("name") String name, @JsonProperty("description") String description,
            @JsonProperty("displayName") String displayName, @JsonProperty("pathOnFileSystem") String pathOnFileSystem,
            @JsonProperty("url") String url, @JsonProperty("allowedActions") List<String> allowedActions,
            @JsonProperty("sharedWith") List<SharedWithEntity> sharedWith) {
        this(id,resourceUUID,name,description,displayName,pathOnFileSystem, url,allowedActions);
        if(sharedWith != null)
            this.addSharedWith(sharedWith);
    }

    public DataVolumeModel(Long id, String resourceUUID, String name, String description,
            String displayName, String pathOnFileSystem, String url, List<String> allowedActions) {
        super(id,resourceUUID,name,description,allowedActions);
        this.displayName = displayName;
        this.pathOnFileSystem = Objects.requireNonNull(pathOnFileSystem);
        this.url = url;
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
        return Objects.hash(getAllowedActions(), getResourceUUID(), getDescription(), getDisplayName(), getId(), 
                getName(), getPathOnFileSystem(), getSharedWith(), getUrl());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DataVolumeModel))
            return false;
        DataVolumeModel other = (DataVolumeModel) obj;
        return super.equals(other)
                && Objects.equals(displayName, other.displayName) && Objects.equals(url, other.url)
                && Objects.equals(pathOnFileSystem, other.pathOnFileSystem) ;
    }

}
