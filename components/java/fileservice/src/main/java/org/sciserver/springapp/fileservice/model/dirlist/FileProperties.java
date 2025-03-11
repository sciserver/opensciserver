package org.sciserver.springapp.fileservice.model.dirlist;

import java.io.File;
import java.nio.file.attribute.BasicFileAttributes;

public class FileProperties {

    private String name;
    private Long size;
    private String lastModified;
    private String creationTime;

    public FileProperties(String name, Long size, String lastModified, String creationTime) {
        this.name = name;
        this.size = size;
        this.lastModified = lastModified;
        this.creationTime = creationTime;
    }

    public FileProperties(File file, BasicFileAttributes attr) {
        this.name = file.getName();
        this.size = attr.size();
        this.lastModified = attr.lastModifiedTime().toString();
        this.creationTime = attr.creationTime().toString();
    }

    
    public String getName() {
        return name;
    }

    public Long getSize() {
        return size;
    }

    public String getLastModified() {
        return lastModified;
    }

    public String getCreationTime() {
        return creationTime;
    }
}
