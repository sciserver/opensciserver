package org.sciserver.springapp.fileservice.model.dirlist;

public class DirectoryListing {

    private DirectoryProperties root;
    private String queryPath;

    public DirectoryListing(DirectoryProperties root, String queryPath) {
        this.root = root;
        this.queryPath = queryPath;
    }

    public DirectoryProperties getRoot() {
        return root;
    }

    public String getQueryPath() {
        return queryPath;
    }
}
