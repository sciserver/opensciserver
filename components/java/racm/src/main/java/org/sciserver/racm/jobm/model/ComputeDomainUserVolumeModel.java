package org.sciserver.racm.jobm.model;


import java.util.ArrayList;
import java.util.List;

import org.sciserver.racm.utils.model.RACMBaseModel;

public final class ComputeDomainUserVolumeModel extends RACMBaseModel {
	private Long computeDomainId;
	private String name;
	private String description;
	private String fullPath;
	private String owner;
	private String ownerId;
	private String resourceUUID;
	private String fileServiceAPIEndpoint;
	private String rootVolumeName;
	private Boolean isShareable;
	public Boolean getIsShareable() {
		return isShareable;
	}

	public void setIsShareable(Boolean isShareable) {
		this.isShareable = isShareable;
	}

	private List<String> allowedActions;
	public ComputeDomainUserVolumeModel(Long id) {
		super(id);
	}

	public ComputeDomainUserVolumeModel(String id) {
		super(id);
	}

	public ComputeDomainUserVolumeModel(){

	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getOwner() {
		return owner;
	}

	public List<String> getAllowedActions() {
		return allowedActions;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public void setAllowedActions(List<String> allowedActions) {
		this.allowedActions = allowedActions;
	}

	public Long getComputeDomainId() {
		return computeDomainId;
	}

	public void setComputeDomainId(Long computeDomainId) {
		this.computeDomainId = computeDomainId;
	}

	public String getFullPath() {
		return fullPath;
	}

	public void setFullPath(String fullPath) {
		this.fullPath = fullPath;
	}

	public void addAllowedAction(String action){
		if(allowedActions == null)
			allowedActions = new ArrayList<>();
		allowedActions.add(action);
	}

	public String getResourceUUID() {
		return resourceUUID;
	}

	public void setResourceUUID(String resourceUUID) {
		this.resourceUUID = resourceUUID;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public String getFileServiceAPIEndpoint() {
		return fileServiceAPIEndpoint;
	}

	public void setFileServiceAPIEndpoint(String fileServiceAPIEndpoint) {
		this.fileServiceAPIEndpoint = fileServiceAPIEndpoint;
	}

	public String getRootVolumeName() {
		return rootVolumeName;
	}

	public void setRootVolumeName(String rootVolumeName) {
		this.rootVolumeName = rootVolumeName;
	}
}
