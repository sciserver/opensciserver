package org.sciserver.racm.resources.v2.model;

import java.util.List;

public class VolumeContainerResource extends AbstractResourceModel {
	private final DockerComputeDomainForResources dockerComputeDomain;
	public VolumeContainerResource(long entityId, String resourceUUID, String name, String description,
			List<ActionModel> allowedActions, List<ActionModel> possibleActions,
			DockerComputeDomainForResources dockerComputeDomain) {
		super(entityId, resourceUUID, name, description, allowedActions, possibleActions, "VOLUMECONTAINER");
		this.dockerComputeDomain = dockerComputeDomain;
	}

	public DockerComputeDomainForResources getDockerComputeDomain() {
		return dockerComputeDomain;
	}
}
