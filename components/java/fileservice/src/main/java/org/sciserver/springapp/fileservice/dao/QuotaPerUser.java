package org.sciserver.springapp.fileservice.dao;


import java.util.Objects;

public class QuotaPerUser {
    private final long numberOfFilesUsed;
    private final long numberOfFilesQuota;
    private final long numberOfBytesUsed;
    private final long numberOfBytesQuota;
    private final String username;
    private final long rootVolumeId;

    public QuotaPerUser(long numberOfFilesUsed,
                        long numberOfFilesQuota,
                        long numberOfBytesUsed,
                        long numberOfBytesQuota,
                        String username,
                        long rootVolumeId) {
        this.numberOfFilesUsed = numberOfFilesUsed;
        this.numberOfFilesQuota = numberOfFilesQuota;
        this.numberOfBytesUsed = numberOfBytesUsed;
        this.numberOfBytesQuota = numberOfBytesQuota;
        this.username = username;
        this.rootVolumeId = rootVolumeId;
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

    public long getRootVolumeId() {
        return rootVolumeId;
    }

    public String getUsername() {
        return this.username;
    }

    public String getType() {
        return "PER_USER";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        QuotaPerUser that = (QuotaPerUser) o;
        return numberOfFilesUsed == that.numberOfFilesUsed &&
                numberOfFilesQuota == that.numberOfFilesQuota &&
                numberOfBytesUsed == that.numberOfBytesUsed &&
                numberOfBytesQuota == that.numberOfBytesQuota &&
                Objects.equals(username, that.username) &&
                rootVolumeId == that.rootVolumeId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(numberOfFilesUsed, numberOfFilesQuota, numberOfBytesUsed,
                numberOfBytesQuota, username, rootVolumeId);
    }
}
