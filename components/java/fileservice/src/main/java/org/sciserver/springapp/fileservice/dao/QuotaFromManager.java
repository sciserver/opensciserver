package org.sciserver.springapp.fileservice.dao;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QuotaFromManager {
    private final String rootVolumeName;
    private final String relativePath;
    private final long numberOfFilesUsed;
    private final long numberOfFilesQuota;
    private final long numberOfBytesUsed;
    private final long numberOfBytesQuota;

    @JsonCreator
    public QuotaFromManager(
            @JsonProperty("rootVolumeName") String rootVolumeName,
            @JsonProperty("relativePath") String relativePath,
            @JsonProperty("numberOfFilesUsed") long numberOfFilesUsed,
            @JsonProperty("numberOfFilesQuota") long numberOfFilesQuota,
            @JsonProperty("numberOfBytesUsed") long numberOfBytesUsed,
            @JsonProperty("numberOfBytesQuota") long numberOfBytesQuota) {
        this.rootVolumeName = rootVolumeName;
        this.relativePath = relativePath;
        this.numberOfFilesUsed = numberOfFilesUsed;
        this.numberOfFilesQuota = numberOfFilesQuota;
        this.numberOfBytesUsed = numberOfBytesUsed;
        this.numberOfBytesQuota = numberOfBytesQuota;
    }

    public String getRootVolumeName() {
        return rootVolumeName;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public long getNumberOfFilesUsed() {
        return numberOfFilesUsed;
    }

    public long getNumberOfFilesQuota() {
        return numberOfFilesQuota;
    }

    public long getNumberOfBytesUsed() {
        return numberOfBytesUsed;
    }

    public long getNumberOfBytesQuota() {
        return numberOfBytesQuota;
    }

    @Override
    public String toString() {
        return "QuotaFromManager [rootVolumeName=" + rootVolumeName + ", relativePath=" + relativePath 
                + ", numberOfFilesUsed=" + numberOfFilesUsed 
                + ", numberOfFilesQuota=" + numberOfFilesQuota 
                + ", numberOfBytesUsed=" + numberOfBytesUsed 
                + ", numberOfBytesQuota=" + numberOfBytesQuota 
                + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        QuotaFromManager that = (QuotaFromManager) o;
        return numberOfFilesUsed == that.numberOfFilesUsed &&
                numberOfFilesQuota == that.numberOfFilesQuota &&
                numberOfBytesUsed == that.numberOfBytesUsed &&
                numberOfBytesQuota == that.numberOfBytesQuota &&
                Objects.equals(rootVolumeName, that.rootVolumeName) &&
                Objects.equals(relativePath, that.relativePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            rootVolumeName, relativePath, numberOfFilesUsed, numberOfFilesQuota, numberOfBytesUsed, numberOfBytesQuota);
    }
}
