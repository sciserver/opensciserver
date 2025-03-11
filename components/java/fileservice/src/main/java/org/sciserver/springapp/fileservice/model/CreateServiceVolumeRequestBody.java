package org.sciserver.springapp.fileservice.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.sciserver.racm.resources.model.NewSharedWithEntity;


@Schema(description = "This class represents the request body containing "
                    + "the information needed for creating a service volume.")
public class CreateServiceVolumeRequestBody {

    @Schema(description = "SciServer ID of the user")
    private String userId;
    
    @Schema(description = "SciServer ID of the owning resource")
    private String owningResourceUUID;
    
    @Schema(description = "UUID of the resource context in RACM")
    private String resourceContextUUID;
    
    @Schema(description = "List of SciServer entities this volume is shared with.")
    private List<NewSharedWithEntity> shares;

    @Schema(description = "Description of this serviceVolume")
    private String description;
    
    @Schema(description = "Usage of this serviceVolume")
    private String usage;
    
    @Schema(description = "Ownership of this serviceVolume")
    private String ownership; //OWNED

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }


    public void setResourceContextUUID(String resourceContextUUID) {
        this.resourceContextUUID = resourceContextUUID;
    }

    public String getResourceContextUUID() {
        return resourceContextUUID;
    }

    public List<NewSharedWithEntity> getShares() {
        return shares;
    }

    public void setShares(List<NewSharedWithEntity> shares) {
        this.shares = shares;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String getUsage() {
        return usage;
    }

    public void setUsage(String usage) throws Exception {

        if (usage != null && (usage.trim().length() > 0)) {
            this.usage = usage.trim();
        } else {
            throw new Exception(
                "Invalid argument in create service volume request:  : Null value for service volume usage.");
        }
    }

    public String getOwnership() {
        return ownership;
    }

    public void setOwnership(String ownership) throws Exception {
        if (ownership != null && (ownership.trim().length() > 0)) {
            this.ownership = ownership.trim();
        } else {
            throw new Exception("Invalid argument in create service volume request: "
            + "Null value for service volume ownership.");
        }
    }

    public String getOwningResourceUUID() {
        return owningResourceUUID;
    }

    public void setOwningResourceUUID(String owningResourceUUID) {
        this.owningResourceUUID = owningResourceUUID;
    }


}
