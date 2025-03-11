package org.sciserver.springapp.racm.utils;

public class RACMException extends Exception {
	private static final long serialVersionUID = 2029795504597883955L;
	private final String username;
	private final String actionname;
	private final String contextUUID;
	private final String resourceTypeId;
	private final String resourceType;
	private final String resourceUUID;

	public RACMException(String username, String contextUUID, String resourceTypeId, String resourceType,
			String actionname) {
		this.username = username;
		this.contextUUID = contextUUID;
		this.resourceTypeId = resourceTypeId;
		this.resourceType = resourceType;
		this.actionname = actionname;

		this.resourceUUID = null;
	}

	public RACMException(String username, String resourceUUID, String actionname) {
		this.username = username;
		this.resourceUUID = resourceUUID;
		this.actionname = actionname;

		this.contextUUID = null;
		this.resourceTypeId = null;
		this.resourceType = null;
	}

	public String getUsername() {
		return username;
	}

	public String getContextUUID() {
		return contextUUID;
	}

	public String getResourceTypeId() {
		return resourceTypeId;
	}

	public String getActionname() {
		return actionname;
	}

	public String getResourceType() {
		return resourceType;
	}

	public String getResourceUUID() {
		return resourceUUID;
	}
}
