package org.sciserver.springapp.fileservice.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DownloadMultipleFilesRequest class created to receive file
 * request body for multiple download endpoint.
 */

@Schema(description = "Class created to receive file request body for multiple download endpoint.")
public class DownloadMultipleFilesRequest {
    @Schema(description = "Full file path, starting with a topVolume")
    public String filePath;

    @Schema(description = "Entity tag value")
    public String etag;

    @Schema(description = "Maximum file size")
    public int maxSize;

    /**
     * DownloadMultipleFilesRequest constructor.
     */
    public DownloadMultipleFilesRequest(String filePath, String etag, int maxSize) {
        this.filePath = filePath;
        this.etag = etag;
        this.maxSize = maxSize;
    }
}

