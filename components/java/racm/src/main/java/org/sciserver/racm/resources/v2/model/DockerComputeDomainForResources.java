package org.sciserver.racm.resources.v2.model;

public class DockerComputeDomainForResources {
	private final String name;
	private final String description;
	private final String apiEndpoint;
	public DockerComputeDomainForResources(String name, String description, String apiEndpoint) {
		this.name = name;
		this.description = description;
		this.apiEndpoint = apiEndpoint;
	}
	public String getName() {
		return name;
	}
	public String getDescription() {
		return description;
	}
	public String getApiEndpoint() {
		return apiEndpoint;
	}
}