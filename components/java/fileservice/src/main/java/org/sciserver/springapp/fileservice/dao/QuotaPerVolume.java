package org.sciserver.springapp.fileservice.dao;


import java.util.Objects;

public class QuotaPerVolume {
    private final long numberOfFilesUsed;
    private final long numberOfFilesQuota;
    private final long numberOfBytesUsed;
    private final long numberOfBytesQuota;
    private final long userVolumeId;
    private final long rootVolumeId;

    public QuotaPerVolume(long numberOfFilesUsed,
                            long numberOfFilesQuota,
                            long numberOfBytesUsed,
                            long numberOfBytesQuota,
                            long userVolumeId,
                            long rootVolumeId) {
        this.numberOfFilesUsed = numberOfFilesUsed;
        this.numberOfFilesQuota = numberOfFilesQuota;
        this.numberOfBytesUsed = numberOfBytesUsed;
        this.numberOfBytesQuota = numberOfBytesQuota;
        this.userVolumeId = userVolumeId;
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

    public long getUserVolumeId() {
        return this.userVolumeId;
    }

    public String getType() {
        return "PER_VOLUME";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        QuotaPerVolume that = (QuotaPerVolume) o;
        return numberOfFilesUsed == that.numberOfFilesUsed 
                                 && numberOfFilesQuota == that.numberOfFilesQuota 
                                 && numberOfBytesUsed == that.numberOfBytesUsed 
                                 && numberOfBytesQuota == that.numberOfBytesQuota 
                                 && userVolumeId == that.userVolumeId 
                                 && rootVolumeId == that.rootVolumeId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(numberOfFilesUsed, numberOfFilesQuota, numberOfBytesUsed,
                numberOfBytesQuota, userVolumeId, rootVolumeId);
    }
}
