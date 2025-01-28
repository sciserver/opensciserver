package org.sciserver.racm.resources.v2.model;

import java.util.List;

public class GenericResource extends AbstractResourceModel {
	private final String resourceType;
	private final ResourceContext resourceContext;
	private final ContextClass contextClass;

	public GenericResource(long entityId, String resourceUUID, String name, String description,
			List<ActionModel> allowedActions, List<ActionModel> possibleActions, String resourceType,
			ResourceContext resourceContext, ContextClass contextClass) {
		super(entityId, resourceUUID, name, description, allowedActions, possibleActions, "RESOURCE");
		this.resourceType = resourceType;
		this.resourceContext = resourceContext;
		this.contextClass = contextClass;
	}

	public String getResourceType() {
		return resourceType;
	}

	public ResourceContext getResourceContext() {
		return resourceContext;
	}

	public ContextClass getContextClass() {
		return contextClass;
	}

	public static class ResourceContext {
		private final String racmEndpoint;
		private final String description;
		public ResourceContext(String racmEndpoint, String description) {
			this.racmEndpoint = racmEndpoint;
			this.description = description;
		}
		public String getRacmEndpoint() {
			return racmEndpoint;
		}
		public String getDescription() {
			return description;
		}
	}

	public static class ContextClass {
		private final String name;
		private final String description;
		public ContextClass(String name, String description) {
			this.name = name;
			this.description = description;
		}
		public String getName() {
			return name;
		}
		public String getDescription() {
			return description;
		}
	}
}
