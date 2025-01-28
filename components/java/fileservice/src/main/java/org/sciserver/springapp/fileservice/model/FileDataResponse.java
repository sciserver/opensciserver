package org.sciserver.springapp.fileservice.model;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

/**
 * FileDataResponse class created to store base64 file contents or error for
 * each file on Multiple download file request.
 */
@Schema(description = "Holds the information and data of each file in the multiple file donwload endpoint.")
public class FileDataResponse {
    @Schema(description = "HTTP status code")
    private HttpStatus statusCode;

    @Schema(description = "File path, starting with the topVolume.")
    private String filePath;

    @Schema(description = "File content, Base64-encoded.")
    private String data;

    @Schema(description = "Error message.")
    private String error;

    /**
     * FileDataResponse constructor. Receives filePath and file contents in base64
     * string form.
     */
    
    public FileDataResponse(String filePath, String data) {
        this.statusCode = HttpStatus.OK;
        this.filePath = filePath;
        this.data = data;
    }

    /**
     * FileDataResponse constructor. Receives filePath, error code and error
     * message.
     */
    public FileDataResponse(String filePath, HttpStatus statusCode, String error) {
        this.filePath = filePath;
        this.statusCode = statusCode;
        this.error = error;
    }

    public HttpStatus getStatusCode() {
        return this.statusCode;
    }

    public String getFilePath() {
        return this.filePath;
    }

    public String getData() {
        return this.data;
    }

    public String getError() {
        return this.error;
    }

}
