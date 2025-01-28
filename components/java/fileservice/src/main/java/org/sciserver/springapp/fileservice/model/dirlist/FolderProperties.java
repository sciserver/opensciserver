package org.sciserver.springapp.fileservice.model.dirlist;

public class FolderProperties {

    private String name;
    private String lastModified;
    private String creationTime;

    public FolderProperties(String name, String lastModified, String creationTime) {
        this.name = name;
        this.lastModified = lastModified;
        this.creationTime = creationTime;
    }

    public String getName() {
        return name;
    }

    public String getLastModified() {
        return lastModified;
    }

    public String getCreationTime() {
        return creationTime;
    }
}
