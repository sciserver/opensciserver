package org.sciserver.springapp.racm.ugm.domain;

import edu.jhu.rac.Resource;

public class ResourceOwnedGroup {

	private final Resource resource;
	private final Long groupId;
	public ResourceOwnedGroup(Resource resource, Long groupId) {
		if(resource == null || groupId == null)
			throw new NullPointerException("Neither resource nore groupId can be null in ResourceOwnedGroup");
		this.resource = resource;
		this.groupId = groupId;
	}
	public Resource getResource() {
		return resource;
	}
	public Long getGroupId() {
		return groupId;
	}
}
