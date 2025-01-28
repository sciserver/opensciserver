package org.sciserver.racm.collaboration.model;

public class ResourceOwnedGroup {

	private Long groupId;
	private String groupName, resourceUUID, resourceType;
	
	public ResourceOwnedGroup() {}
	
	
	public Long getGroupId() {
		return groupId;
	}
	public void setGroupId(Long groupId) {
		this.groupId = groupId;
	}
	public String getGroupName() {
		return groupName;
	}
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}
	public String getResourceUUID() {
		return resourceUUID;
	}
	public void setResourceUUID(String resourceUUID) {
		this.resourceUUID = resourceUUID;
	}
	public String getResourceType() {
		return resourceType;
	}
	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}
	
}
