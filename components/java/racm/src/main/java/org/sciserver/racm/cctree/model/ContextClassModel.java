package org.sciserver.racm.cctree.model;

import java.util.List;

import org.sciserver.racm.cctree.model.ResourceTypeModel;
import org.sciserver.racm.utils.model.RACMMVCBaseModel;

public class ContextClassModel extends RACMMVCBaseModel{
	private String name;
	private String description;
	private String release;
	private List<ResourceTypeModel> resourceTypes;

	public ContextClassModel() {
		super();
	}


	public ContextClassModel(String id) {
		super(id);
	}


	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getRelease() {
		return release;
	}
	public void setRelease(String release) {
		this.release = release;
	}
	public List<ResourceTypeModel> getResourceTypes() {
		return resourceTypes;
	}
	public void setResourceTypes(List<ResourceTypeModel> resourceTypes) {
		this.resourceTypes = resourceTypes;
	}
}
