package org.sciserver.springapp.fileservice.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "This class represents the request body containing "
                    + "the information needed for creating a user volume.")
public class CreateUserVolumeRequestBody {

    @Schema(description = "Description of this userVolume.")
    private String description;

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
