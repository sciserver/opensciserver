package org.sciserver.springapp.racm.resourcecontext.domain;

import java.util.Objects;

public final class AssociatedSciserverEntity {
	private final long entityId;
	private final EntityType entityType;
	private final String usage;
	private final boolean owned;

	public AssociatedSciserverEntity(long entityId, String entityType, String usage, boolean owned) {
		this.entityId = entityId;
		this.entityType = EntityType.valueOf(entityType);
		this.usage = usage;
		this.owned = owned;
	}

	public long entityId() {
		return entityId;
	}

	public EntityType entityType() {
		return entityType;
	}

	public String usage() {
		return usage;
	}

	public boolean owned() {
		return owned;
	}

	public enum EntityType {
		GROUP, USER, SERVICE;
	}

	@Override
	public int hashCode() {
		return Objects.hash(entityId, entityType, owned, usage);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AssociatedSciserverEntity other = (AssociatedSciserverEntity) obj;
		return entityId == other.entityId && entityType == other.entityType && owned == other.owned
				&& Objects.equals(usage, other.usage);
	}
}
