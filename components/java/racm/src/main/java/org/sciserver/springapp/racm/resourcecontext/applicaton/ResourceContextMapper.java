package org.sciserver.springapp.racm.resourcecontext.applicaton;

import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.sciserver.racm.resourcecontext.model.ActionModel;
import org.sciserver.racm.resourcecontext.model.AssociatedResourceModel;
import org.sciserver.racm.resourcecontext.model.AssociatedSciserverEntityModel;
import org.sciserver.racm.resourcecontext.model.NewResourceModel;
import org.sciserver.racm.resourcecontext.model.RegisteredResourceModel;
import org.sciserver.racm.resourcecontext.model.ResourceFromUserPerspectiveModel;
import org.sciserver.racm.resourcecontext.model.ServiceResourceFromUserPerspectiveModel;
import org.sciserver.springapp.racm.resourcecontext.domain.Action;
import org.sciserver.springapp.racm.resourcecontext.domain.AssociatedResource;
import org.sciserver.springapp.racm.resourcecontext.domain.AssociatedSciserverEntity;
import org.sciserver.springapp.racm.resourcecontext.domain.Resource;
import org.sciserver.springapp.racm.resourcecontext.vourp.ResourcePermissionProvider;
import org.sciserver.springapp.racm.resourcecontext.vourp.ResourceRepository;
import org.springframework.stereotype.Component;

@Component
public class ResourceContextMapper {
	private final ResourceRepository resourceRepository;
	private final ResourcePermissionProvider resourcePermissionProvider;
	ResourceContextMapper(ResourceRepository resourceRepository,
			ResourcePermissionProvider resourcePermissionProvider) {
		this.resourceRepository = resourceRepository;
		this.resourcePermissionProvider = resourcePermissionProvider;
	}

	public RegisteredResourceModel toDTO(Resource original) {
		return new RegisteredResourceModel(original.id(),
				original.publisherDID(), original.uuid(),
				original.name(), original.description(), original.resourceTypeName());
	}

	public Resource toDomainModel(String resourceContextUUID, NewResourceModel original) {
		return Resource.createNew(resourceContextUUID,
				original.getPublisherDID(),
				original.getName(),
				original.getDescription(),
				original.getResourceTypeName());
	}

	private ResourceFromUserPerspectiveModel toResourceFromUserPerspectiveModel(
			Resource resource, Set<Action> actions) {
		return new ResourceFromUserPerspectiveModel(resource.id(),
				resource.publisherDID(), resource.uuid(),
				resource.name(), resource.description(), resource.resourceTypeName(),
				toDTO(actions),
				toDTOAssociatedResource(resource.associatedResources()),
				toAssociatedEntityDTO(resource.associatedSciserverEntities()));
	}

	private Collection<AssociatedResourceModel> toDTOAssociatedResource(
			Collection<AssociatedResource> original) {
		return original.stream()
				.map(ar -> new AssociatedResourceModel(ar.usage(), ar.getResourceDescription(), ar.isOwned(), ar.resourceUUID()))
				.collect(toSet());
	}

	private Collection<AssociatedSciserverEntityModel> toAssociatedEntityDTO(
			Collection<AssociatedSciserverEntity> original) {
		return original.stream()
				.map(ar -> new AssociatedSciserverEntityModel(
						ar.usage(), ar.owned(), ar.entityId(), ar.entityType().name()))
				.collect(toSet());
	}

	public AssociatedResource toDomainModel(AssociatedResourceModel original) {
		return new AssociatedResource(original.getResourceUUID(), original.getUsage(), original.getResourceDescription(), original.isOwned());
	}

	public AssociatedSciserverEntity toDomainModel(AssociatedSciserverEntityModel original) {
		return new AssociatedSciserverEntity(original.getEntityId(), original.getEntityType().name(),
				original.getUsage(), original.isOwned());
	}

	private Set<ActionModel> toDTO(Set<Action> actions) {
		return actions.stream().map(this::toDTO).collect(toSet());
	}
	private ActionModel toDTO(Action action) {
		return new ActionModel(action.name(), ActionModel.ActionCategory.valueOf(action.category()));
	}

	public Set<ResourceFromUserPerspectiveModel> getResourcesWithActions(
			String username, String resourceContextUUID) {
		Map<String, Set<Action>> resourceActions = resourcePermissionProvider
				.getActionsAllowedInResourceContext(username, resourceContextUUID);
		if(resourceActions.isEmpty())
		    return null;
		return resourceRepository.getByUUIDs(resourceActions.keySet())
			.stream()
			.map(resource -> toResourceFromUserPerspectiveModel(
					resource,
					resourceActions.get(resource.uuid())
					))
			.collect(toSet());
	}

	public Collection<String> getResourceUUIDsForPubDID(String serviceToken, String resourceContextUUID, String pubDID) {
        return resourceRepository.getByPubDID(serviceToken, resourceContextUUID, pubDID);
    }

	public ServiceResourceFromUserPerspectiveModel getServiceResourceWithActions(String username, String resourceUUID) {
		Set<Action> actions = resourcePermissionProvider.getActionsForResource(username, resourceUUID);
		Set<ActionModel> actionSet = toDTO(actions);
		ServiceResourceFromUserPerspectiveModel model = resourceRepository.toServiceResourceFromUserPerspectiveModel(resourceUUID, username, actionSet);
		return model;		
	}
	
	public ResourceFromUserPerspectiveModel getResourceWithActions(String username, String resourceUUID) {
		Resource r = resourceRepository.get(resourceUUID);
		if (r == null)
		    return null;
		return toResourceFromUserPerspectiveModel(r,
				resourcePermissionProvider.getActionsForResource(username, resourceUUID));
	}

}
