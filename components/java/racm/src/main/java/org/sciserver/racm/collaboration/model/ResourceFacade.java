package org.sciserver.racm.collaboration.model;

import java.util.List;
import java.util.Objects;

import org.sciserver.racm.collaboration.model.ResourceWithAnyType.ResourceContextInfo;
import org.springframework.hateoas.RepresentationModel;

public class ResourceFacade extends RepresentationModel {
	public static ResourceFacade createResourceFacadeFromEntity(String name, String description, TYPE type, long resourceId,
			long entityId, List<String> actions) {
		return new ResourceFacade(name, description, entityId, resourceId, type, actions);
	}

	public static ResourceFacade createResourceFacadeFromResource(String name, String description,
			List<String> actions, String resourceType,
			long resourceId, ResourceContextInfo resourceContext) {
		return new ResourceWithAnyType(name, description, actions, resourceType, resourceId, resourceContext);
	}

	private final String name;
	private final String description;
	private final TYPE type;
	private final List<String> actions;
	private final long resourceId;
	private final long entityId;

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public TYPE getType() {
		return type;
	}

	public List<String> getActions() {
		return actions;
	}

	protected ResourceFacade(String name, String description, long entityId, long resourceId,
			TYPE type, List<String> actions) {
		this.name = name;
		this.description = description;
		this.type = type;
		this.actions = actions;
		this.resourceId = resourceId;
		this.entityId = entityId;
	}

	public enum TYPE {
		USERVOLUME, VOLUMECONTAINER, DATAVOLUME, DATABASE, DOCKERIMAGE, RESOURCE
	}

	public long getResourceId() {
		return resourceId;
	}

	public long getEntityId() {
		return entityId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(actions, description, entityId, name, resourceId, type);
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
		ResourceFacade other = (ResourceFacade) obj;
		return Objects.equals(actions, other.actions) && Objects.equals(description, other.description)
				&& entityId == other.entityId && Objects.equals(name, other.name) && resourceId == other.resourceId
				&& type == other.type;
	}
}
