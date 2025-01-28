package org.sciserver.racm.rctree.model;

import java.util.List;

import org.sciserver.racm.cctree.model.ContextClassModel;
import org.sciserver.racm.utils.model.RACMMVCBaseModel;

public class ResourceContextMVCModel extends RACMMVCBaseModel{
	private ContextClassModel contextClassModel;
	private String uuid;
	private String endpoint;
	private String description;
	private String label;
	private String secretToken;
	private List<ResourceMVCModel> resources;
	private List<ContextClassModel> availableContextClasseModels;

	public ResourceContextMVCModel(){}

	public ResourceContextMVCModel(String id){
		super(id);
	}

	public String getUuid() {
		return uuid;
	}
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	public String getEndpoint() {
		return endpoint;
	}
	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public List<ResourceMVCModel> getResources() {
		return resources;
	}
	public void setResources(List<ResourceMVCModel> resources) {
		this.resources = resources;
	}
	public List<ContextClassModel> getAvailableContextClasseModels() {
		return availableContextClasseModels;
	}
	public void setAvailableContextClasses(List<ContextClassModel> availableContextClassModels) {
		this.availableContextClasseModels = availableContextClassModels;
	}
	public ContextClassModel getContextClassModel() {
		return contextClassModel;
	}
	public void setContextClassModel(ContextClassModel contextClassModel) {
		this.contextClassModel = contextClassModel;
	}

	public String getSecretToken() {
		return secretToken;
	}

	public void setSecretToken(String secretToken) {
		this.secretToken = secretToken;
	}
}
