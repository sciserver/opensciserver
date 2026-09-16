package org.sciserver.springapp.racm.utils.controller;

public class ResourceContextNotFoundException extends RuntimeException {
    public ResourceContextNotFoundException(String uuid) {
        super("ResourceContext with UUID = '" + uuid + "' not found.");
    }
}
