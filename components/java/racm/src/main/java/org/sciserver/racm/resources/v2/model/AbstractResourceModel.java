package org.sciserver.racm.resources.v2.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

abstract class AbstractResourceModel implements ResourceModel {
	private final long entityId;
	private final String resourceUUID;
	private final String name;
	private final String description;
	private final List<ActionModel> allowedActions;
	@JsonInclude(Include.NON_EMPTY)
	private final List<ActionModel> possibleActions;
	private final String type;

	protected AbstractResourceModel(long entityId, String resourceUUID, String name, String description,
			List<ActionModel> allowedActions, List<ActionModel> possibleActions, String type) {
		this.entityId = entityId;
		this.resourceUUID = resourceUUID;
		this.name = name;
		this.description = description;
		this.allowedActions = allowedActions;
		this.possibleActions = possibleActions;
		this.type = type;
	}

	@Override
	public long getEntityId() {
		return entityId;
	}

	@Override
	public String getResourceUUID() {
		return resourceUUID;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public List<ActionModel> getAllowedActions() {
		return allowedActions;
	}

	@Override
	public List<ActionModel> getPossibleActions() {
		return possibleActions;
	}

	@Override
	public String getType() {
		return type;
	}
}
