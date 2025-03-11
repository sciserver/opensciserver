package org.sciserver.springapp.fileservice.dao;

public class ManagerVolumeDTO {
    private final String rootVolumeName;
    private final String relativePath;

    public ManagerVolumeDTO(String rootVolumeName, String relativePath) {

        this.rootVolumeName = rootVolumeName;
        this.relativePath = relativePath;
    }

    public String getRootVolumeName() {
        return rootVolumeName;
    }

    public String getRelativePath() {
        return relativePath;
    }
}
