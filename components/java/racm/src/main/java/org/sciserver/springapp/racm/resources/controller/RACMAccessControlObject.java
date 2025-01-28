package org.sciserver.springapp.racm.resources.controller;

public class RACMAccessControlObject {

	private String username;
	private String resourceContextUUID;
	private String resourcePubDID;
	private String resourceUUID;
	private String resourceType;
	private String action;
	private boolean hasAccess = false;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getResourceContextUUID() {
		return resourceContextUUID;
	}

	public void setResourceContextUUID(String resourceContextUUID) {
		this.resourceContextUUID = resourceContextUUID;
	}


	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public boolean isHasAccess() {
		return hasAccess;
	}

	public void setHasAccess(boolean hasAccess) {
		this.hasAccess = hasAccess;
	}

	public String getResourceType() {
		return resourceType;
	}

	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}

	public String getResourcePubDID() {
		return resourcePubDID;
	}

	public void setResourcePubDID(String resourcePubDID) {
		this.resourcePubDID = resourcePubDID;
	}

	public String getResourceUUID() {
		return resourceUUID;
	}

	public void setResourceUUID(String resourceUUID) {
		this.resourceUUID = resourceUUID;
	}

}
