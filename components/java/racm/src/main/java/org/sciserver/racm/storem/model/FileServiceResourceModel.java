package org.sciserver.racm.storem.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.sciserver.racm.resources.model.SharedWithEntity;

public abstract class FileServiceResourceModel {
    private long id;
    private String resourceUUID;
    private String name;
    private String description;
    private List<String> allowedActions;
    private List<SharedWithEntity> sharedWith;
    private Long owningResourceId;
    
    /**
     * Constructor to be used when all fields are known.<br/>
     * @param id
     * @param resourceUUID
     * @param name
     * @param description
     * @param allowedActions
     * @param sharedWith
     */
    public FileServiceResourceModel(long id, String resourceUUID, String name, String description,
            List<String> allowedActions, List<SharedWithEntity> sharedWith) {
        this.id = id;
        this.resourceUUID = resourceUUID;
        this.name = name;
        this.description = description;
        this.allowedActions = allowedActions;
        this.sharedWith = sharedWith;
    }

    /**
     * Constructor to be used when not all fields may be known, in particular sharedwith.<br/>
     * @param id
     * @param resourceUUID
     * @param name
     * @param description
     * @param allowedActions
     */
    public FileServiceResourceModel(long id, String resourceUUID, String name, String description,
            List<String> allowedActions) {
        this(id,resourceUUID,name,description,allowedActions,new ArrayList<SharedWithEntity>());
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<SharedWithEntity> getSharedWith() {
        return sharedWith;
    }

    public String getResourceUUID() {
        return resourceUUID;
    }

    public List<String> getAllowedActions() {
        return allowedActions;
    }

    public void addSharedWith(SharedWithEntity sh) {
        this.sharedWith.add(sh);
    }
    
    public void addSharedWith(List<SharedWithEntity> shs) {
        this.sharedWith.addAll(shs);
    }
    
    @Override
    public boolean equals(Object other) {
        if(other == this)
            return true;
        if (!(other instanceof FileServiceResourceModel))
            return false;
        FileServiceResourceModel castOther = (FileServiceResourceModel) other;
        return Objects.equals(name, castOther.name) && Objects.equals(description, castOther.description)
                && Objects.equals(resourceUUID, castOther.resourceUUID)
                && Objects.equals(sharedWith, castOther.sharedWith) 
                && Objects.equals(allowedActions, castOther.allowedActions) && Objects.equals(id, castOther.getId());
    }

    public Long getOwningResourceId() {
        return owningResourceId;
    }

    public void setOwningResourceId(Long owningResourceId) {
        this.owningResourceId = owningResourceId;
    }
}
