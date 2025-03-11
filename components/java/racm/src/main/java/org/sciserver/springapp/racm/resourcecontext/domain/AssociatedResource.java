package org.sciserver.springapp.racm.resourcecontext.domain;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;

public final class AssociatedResource {
	private final String resourceUUID;
	private final String usage;
	private final String resourceDescription;
	private final boolean owned;
	private String resourceType;
	private List<String> actions;
	
	
	public String resourceUUID() {
		return resourceUUID;
	}
	public String usage() {
		return usage;
	}
	public boolean isOwned() {
		return owned;
	}
	
	
	public AssociatedResource(String resourceUUID, String usage, String resourceDescription, boolean owned) {
		this.resourceUUID = resourceUUID;
		this.usage = usage;
		this.resourceDescription = resourceDescription;
		this.owned = owned;
	}
	
	public String getResourceType() {
		return resourceType;
	}
	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}
	public List<String> getActions() {
		return actions;
	}
	public void setActions(List<String> actions) {
		this.actions = actions;
	}
	@Override
	public int hashCode() {
		return Objects.hash(owned, resourceUUID, usage);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AssociatedResource other = (AssociatedResource) obj;
		return owned == other.owned && Objects.equals(resourceUUID, other.resourceUUID)
				&& Objects.equals(usage, other.usage);
	}
	
	
	public String getResourceDescription() {
		return resourceDescription;
	}
	@Override
	public String toString() {
		return "AssociatedResource [resourceUUID=" + resourceUUID + ", usage=" + usage + ", owned=" + owned + "]";
	}
}
