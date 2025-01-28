package org.sciserver.racm.collaboration.model;

import java.util.List;
import java.util.Objects;

public class ResourceWithAnyType extends ResourceFacade {
	private final String resourceType;
	private final ResourceContextInfo resourceContext;

	ResourceWithAnyType(String name, String description, List<String> actions, String resourceType,
			long resourceId, ResourceContextInfo resourceContext) {
		super(name, description, resourceId, resourceId, TYPE.RESOURCE, actions);
		this.resourceType = resourceType;
		this.resourceContext = resourceContext;
	}

	public String getResourceType() {
		return resourceType;
	}

	public ResourceContextInfo getResourceContext() {
		return resourceContext;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(resourceContext, resourceType);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ResourceWithAnyType other = (ResourceWithAnyType) obj;
		return Objects.equals(resourceContext, other.resourceContext)
				&& Objects.equals(resourceType, other.resourceType);
	}



	public static class ResourceContextInfo {
		private final String name;
		private final String description;
		private final String contextClassName;
		public ResourceContextInfo(String name, String description, String contextClassName) {
			this.name = name;
			this.description = description;
			this.contextClassName = contextClassName;
		}
		public String getName() {
			return name;
		}
		public String getDescription() {
			return description;
		}
		public String getContextClassName() {
			return contextClassName;
		}
		@Override
		public int hashCode() {
			return Objects.hash(contextClassName, description, name);
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ResourceContextInfo other = (ResourceContextInfo) obj;
			return Objects.equals(contextClassName, other.contextClassName)
					&& Objects.equals(description, other.description) && Objects.equals(name, other.name);
		}
	}
}
