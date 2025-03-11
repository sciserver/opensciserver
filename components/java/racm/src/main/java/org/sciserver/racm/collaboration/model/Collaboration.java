package org.sciserver.racm.collaboration.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.hateoas.RepresentationModel;

public class Collaboration extends RepresentationModel {
	private final String name;
	private final String description;
	private final String type;
	private final List<ResourceFacade> resources;
	private final List<CollaborationMember> members;
	private final long groupId;

	public Collaboration(String name, String description, String type, List<ResourceFacade> resources, List<CollaborationMember> members, long groupId) {
		this.name = Objects.requireNonNull(name);
		this.description = description;
		this.type = Objects.requireNonNull(type);
		this.resources = Collections.unmodifiableList(resources);
		this.members = Collections.unmodifiableList(members);
		this.groupId = groupId;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getType() {
		return type;
	}

	public List<ResourceFacade> getResources() {
		return resources;
	}

	public List<CollaborationMember> getMembers() {
		return members;
	}

	public long getGroupId() {
		return groupId;
	}
}
