package org.sciserver.springapp.racm.resourcecontext.domain;

import static java.util.Collections.emptySet;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Resource {
	private final long id;
	private static final long TRANSIENT_ID = -1;
	private final String uuid;
	private final String resourceContextUUID;
	private String publisherDID;
	private String name;
	private String description;
	private String resourceTypeName;
	private final Set<AssociatedResource> associatedResources = new HashSet<>();
	private final Set<AssociatedSciserverEntity> associatedSciserverEntities = new HashSet<>();

	private Resource(long id, String uuid, String resourceContextUUID, String publisherDID,
			String name, String description,
			String resourceTypeName, Collection<AssociatedResource> associatedResources,
			Collection<AssociatedSciserverEntity> associatedSciserverEntities) {
		this.id = id;
		this.uuid = uuid;
		this.resourceContextUUID = resourceContextUUID;
		this.publisherDID = publisherDID;
		setName(name);
		this.description = description;
		this.resourceTypeName = resourceTypeName;
		this.associatedResources.addAll(associatedResources);
		this.associatedSciserverEntities.addAll(associatedSciserverEntities);
	}

	public static Resource createNew(String resourceContextUUID, String publisherDID,
			String name, String description,
			String resourceTypeName) {
		return new Resource(TRANSIENT_ID, null, resourceContextUUID, publisherDID,
				name, description, resourceTypeName, emptySet(), emptySet());
	}

	public static Resource createFromExisting(long id, String uuid, String resourceContextUUID,
			String publisherDID, String name, String description,
			String resourceTypeName, Collection<AssociatedResource> associatedResources,
			Collection<AssociatedSciserverEntity> associatedSciserverEntities) {
		return new Resource(id, uuid, resourceContextUUID, publisherDID, name, description,
				resourceTypeName, associatedResources, associatedSciserverEntities);
	}

	private void setName(String name) {
		this.name = name;
	}

	public long id() {
		return id;
	}

	public String uuid() {
		return uuid;
	}

	public boolean isTransient() {
		return id == TRANSIENT_ID;
	}
	public String publisherDID() {
		return publisherDID;
	}

	public String name() {
		return name;
	}

	public String description() {
		return description;
	}

	public String resourceTypeName() {
		return resourceTypeName;
	}

	public String resourceContextUUID() {
		return resourceContextUUID;
	}

	public void addAssociationWithResource(AssociatedResource newAssociatedResource) {
		associatedResources.add(newAssociatedResource);
	}

	public void addAssociationWithSciserverEntity(AssociatedSciserverEntity newAssociatedSciserverEntity) {
		associatedSciserverEntities.add(newAssociatedSciserverEntity);
	}

	public Set<AssociatedResource> associatedResources() {
		return associatedResources;
	}

	public Set<AssociatedSciserverEntity> associatedSciserverEntities() {
		return associatedSciserverEntities;
	}

}
