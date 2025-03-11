package org.sciserver.springapp.fileservice.model;

import java.util.ArrayList;
import org.springframework.http.HttpStatus;

import io.swagger.v3.oas.annotations.media.Schema;


/**
 * DownloadMultipleFilesResponse class created to send back
 * response body for multiple download endpoint.
 */
@Schema(description = "This class represents the response body for the multiple file donwload endpoint.")
public class DownloadMultipleFilesResponse {

    @Schema(description = "HTTP response status code")
    private HttpStatus statusCode;

    @Schema(description = "Array with file data")
    private ArrayList<FileDataResponse> data;

    @Schema(description = "Error messsage")
    private String error;

    public DownloadMultipleFilesResponse(ArrayList<FileDataResponse> data) {
        this.statusCode = HttpStatus.OK;
        this.data = data;
    }

    public DownloadMultipleFilesResponse(String error) {
        this.statusCode = HttpStatus.INTERNAL_SERVER_ERROR;
        this.error = error;
    }

    public ArrayList<FileDataResponse> getFileDataResponse() {
        return this.data;
    }

    public HttpStatus getStatusCode() {
        return this.statusCode;
    }

    public String geterror() {
        return this.error;
    }

}
