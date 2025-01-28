package org.sciserver.racm.resourcecontext.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AssociatedSciserverEntityModel {
	private final String usage;
	private final boolean owned;
	private final long entityId;
	private final EntityType entityType;

	@JsonCreator
	public AssociatedSciserverEntityModel(
			@JsonProperty("usage") String usage,
			@JsonProperty("owned") boolean owned,
			@JsonProperty("entityId") long entityId,
			@JsonProperty("entityType") EntityType entityType) {
		this.usage = usage;
		this.owned = owned;
		this.entityId = entityId;
		this.entityType = entityType;
	}

	public AssociatedSciserverEntityModel(String usage, boolean owned, long entityId, String entityType) {
		this(usage, owned, entityId, EntityType.valueOf(entityType));
	}

	public String getUsage() {
		return usage;
	}
	public boolean isOwned() {
		return owned;
	}
	public long getEntityId() {
		return entityId;
	}
	public EntityType getEntityType() {
		return entityType;
	}

	public enum EntityType {
		GROUP, USER, SERVICE
	}
}
