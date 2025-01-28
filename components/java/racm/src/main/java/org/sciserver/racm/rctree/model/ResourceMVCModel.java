package org.sciserver.racm.rctree.model;

import java.util.List;

import org.sciserver.racm.cctree.model.ResourceTypeModel;
import org.sciserver.racm.utils.model.RACMMVCBaseModel;
/**
 * Representation of vo-urp Resource.
 *
 * @author gerard
 *
 */
public class ResourceMVCModel extends RACMMVCBaseModel{

	/**
	 * vo-urp Id of the containing ResourceContext. May be used by RACM web app for example to identify parent
	 */
	private long containerId;
	/**
	 * UUID of parent ResourceContext. Useed by a "real" ResourceContext when identifying itself
	 */
	private String contextUUID;
	/**
	 * ID of this resource as used by the "real" resource context that owns this resource.
	 */
	private String publisherDID;
	private String name;
	private String description;
	private String uuid;
	private ResourceTypeModel resourceTypeModel;
	private List<ResourceTypeModel>	availableResourceTypeModels;
	public ResourceMVCModel(){}

	public ResourceMVCModel(String id){
		super(id);
	}

	public ResourceMVCModel(Long id){
		super(id);
	}
	public String getContextUUID() {
		return contextUUID;
	}

	public void setContextUUID(String contextUUID) {
		this.contextUUID = contextUUID;
	}

	@Override
	public String getPublisherDID() {
		return publisherDID;
	}

	@Override
	public void setPublisherDID(String contextIdentifier) {
		this.publisherDID = contextIdentifier;
	}

	public ResourceTypeModel getResourceTypeModel() {
		return resourceTypeModel;
	}
	public void setResourceTypeModel(ResourceTypeModel resourceTypeModel) {
		this.resourceTypeModel = resourceTypeModel;
	}

	public long getContainerId() {
		return containerId;
	}
	public void setContainerId(long containerId) {
		this.containerId = containerId;
	}
	public List<ResourceTypeModel> getAvailableResourceTypeModels() {
		return availableResourceTypeModels;
	}
	public void setAvailableResourceTypeModels(List<ResourceTypeModel> availableResourceTypeModels) {
		this.availableResourceTypeModels = availableResourceTypeModels;
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

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

}
