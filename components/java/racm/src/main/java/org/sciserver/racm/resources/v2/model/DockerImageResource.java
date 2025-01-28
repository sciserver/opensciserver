package org.sciserver.racm.resources.v2.model;

import java.util.List;

public class DockerImageResource extends AbstractResourceModel {
	private final DockerComputeDomainForResources dockerComputeDomain;
	public DockerImageResource(long entityId, String resourceUUID, String name, String description,
			List<ActionModel> allowedActions, List<ActionModel> possibleActions,
			DockerComputeDomainForResources dockerComputeDomain) {
		super(entityId, resourceUUID, name, description, allowedActions, possibleActions, "DOCKERIMAGE");
		this.dockerComputeDomain = dockerComputeDomain;
	}

	public DockerComputeDomainForResources getDockerComputeDomain() {
		return dockerComputeDomain;
	}
}
