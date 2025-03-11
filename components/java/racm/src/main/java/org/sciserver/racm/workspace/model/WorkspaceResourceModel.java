package org.sciserver.racm.workspace.model;

import java.util.ArrayList;
import java.util.List;

import org.sciserver.racm.utils.model.RACMBaseModel;

public class WorkspaceResourceModel extends RACMBaseModel{

	private String name;
	private String pubDID;
	private String contextClass;
	private String resourceType;
	private String resourceContextAPIEndpoint;
	private List<String> actions = new ArrayList<>();
	public WorkspaceResourceModel() {
		super();
	}

	public WorkspaceResourceModel(Long id) {
		super(id);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPubDID() {
		return pubDID;
	}

	public void setPubDID(String pubDID) {
		this.pubDID = pubDID;
	}

	public String getContextClass() {
		return contextClass;
	}

	public void setContextClass(String contextClass) {
		this.contextClass = contextClass;
	}

	public String getResourceType() {
		return resourceType;
	}

	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}

	public List<String> getActions() {
		return actions;
	}

	public void setActions(List<String> actions) {
		this.actions = actions;
	}

	public void addAction(String action){
		actions.add(action);
	}

	public String getResourceContextAPIEndpoint() {
		return resourceContextAPIEndpoint;
	}

	public void setResourceContextAPIEndpoint(String resourceContextAPIEndpoint) {
		this.resourceContextAPIEndpoint = resourceContextAPIEndpoint;
	}


}
