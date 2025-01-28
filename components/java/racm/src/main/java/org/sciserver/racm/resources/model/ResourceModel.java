package org.sciserver.racm.resources.model;

import org.sciserver.racm.resourcecontext.model.RegisteredResourceModel;

public class ResourceModel extends RegisteredResourceModel {

	private String resourceContextDescription, resourceContextUUID,contextClassName;
	public ResourceModel(long id, String publisherDID, String uuid, String name, String description,
			String resourceTypeName) {
		super(id, publisherDID, uuid, name, description, resourceTypeName);
		// TODO Auto-generated constructor stub
	}
	public String getResourceContextDescription() {
		return resourceContextDescription;
	}
	public void setResourceContextDescription(String resourceContextDescription) {
		this.resourceContextDescription = resourceContextDescription;
	}
	public String getResourceContextUUID() {
		return resourceContextUUID;
	}
	public void setResourceContextUUID(String resourceContextUUID) {
		this.resourceContextUUID = resourceContextUUID;
	}
	public String getContextClassName() {
		return contextClassName;
	}
	public void setContextClassName(String contextClassName) {
		this.contextClassName = contextClassName;
	}

}
