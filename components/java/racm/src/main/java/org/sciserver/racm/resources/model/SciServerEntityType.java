package org.sciserver.racm.resources.model;

public enum SciServerEntityType {
	USER("User"), GROUP("UserGroup"), SERVICE("ServiceAccount");

    public final String className;
    private SciServerEntityType(String className){
        this.className=className;
    }
}