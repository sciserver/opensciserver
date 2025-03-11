package org.sciserver.springapp.fileservice.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "This class represents the request body containing "
                    + "the information needed for moving data within the file system, "
                    + "from one path to a destination path.")
public class MoveDataRequestBody {

    @Schema(description = "Name of the destination FileService.")
    private String destinationFileService;

    @Schema(description = "Name of the destination rootVolume. Needed if destinationDataVolume is not set.")
    private String destinationRootVolume;

    @Schema(description = "Name of the destination dataVolume. Needed if destinationRootVolume is not set.")
    private String destinationDataVolume;

    @Schema(description = "Name of the destination userVolume. Needed if destinationRootVolume is set.")
    private String destinationUserVolume;

    @Schema(description = "Destination path.")
    private String destinationPath;

    @Schema(description = "Owner of the destination userVolume. Needed if destinationRootVolume is set.")
    private String destinationOwnerName;

    public void setDestinationFileService(String destinationFileService) {
        this.destinationFileService = destinationFileService;
    }

    public String getDestinationFileService() {
        return destinationFileService;
    }

    public void setDestinationRootVolume(String destinationRootVolume) {
        this.destinationRootVolume = destinationRootVolume;
    }

    public String getDestinationRootVolume() {
        return destinationRootVolume;
    }

    public void setDestinationDataVolume(String destinationDataVolume) {
        this.destinationDataVolume = destinationDataVolume;
    }

    public String getDestinationDataVolume() {
        return destinationDataVolume;
    }

    public void setDestinationUserVolume(String destinationUserVolume) {
        this.destinationUserVolume = destinationUserVolume;
    }

    public String getDestinationUserVolume() {
        return destinationUserVolume;
    }

    public void setDestinationPath(String destinationPath) {
        this.destinationPath = destinationPath;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public void setDestinationOwnerName(String destinationOwnerName) {
        this.destinationOwnerName = destinationOwnerName;
    }

    public String getDestinationOwnerName() {
        return destinationOwnerName;
    }
}
