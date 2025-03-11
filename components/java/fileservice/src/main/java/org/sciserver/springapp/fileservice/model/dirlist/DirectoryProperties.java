package org.sciserver.springapp.fileservice.model.dirlist;

import java.util.ArrayList;

public class DirectoryProperties {

    private String name;
    private String lastModified;
    private String creationTime;
    private ArrayList<FolderProperties> folders;
    private ArrayList<FileProperties> files;

    public DirectoryProperties(String name, String lastModified, String creationTime, 
                                ArrayList<FolderProperties> folders, ArrayList<FileProperties> files) {
        this.name = name;
        this.lastModified = lastModified;
        this.creationTime = creationTime;
        this.folders = folders;
        this.files = files;
    }

    /**
     * The `getName()` function in Java returns the value of the `name` variable.
     * 
     * 

     * @return The `name` variable is being returned.
     */
    public String getName() {
        return name;
    }

    public String getLastModified() {
        return lastModified;
    }

    public String getCreationTime() {
        return creationTime;
    }

    public ArrayList<FolderProperties> getFolders() {
        return folders;
    }

    public ArrayList<FileProperties> getFiles() {
        return files;
    }
}
