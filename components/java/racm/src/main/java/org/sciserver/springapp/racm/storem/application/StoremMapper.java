package org.sciserver.springapp.racm.storem.application;

import static java.util.stream.Collectors.toList;
import static org.sciserver.springapp.racm.storem.application.STOREMConstants.CC_FILESERVICE_NAME;
import static org.sciserver.springapp.racm.storem.application.STOREMConstants.RT_FILESERVICE_DATAVOLUME;
import static org.sciserver.springapp.racm.storem.application.STOREMConstants.RT_FILESERVICE_ROOTVOLUME;
import static org.sciserver.springapp.racm.storem.application.STOREMConstants.RT_FILESERVICE_USERVOLUME;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.ivoa.dm.VOURPException;
import org.ivoa.dm.model.TransientObjectManager;
import org.sciserver.racm.resources.model.SciServerEntityType;
import org.sciserver.racm.resources.model.SharedWithEntity;
import org.sciserver.racm.storem.model.DataVolumeModel;
import org.sciserver.racm.storem.model.FileServiceModel;
import org.sciserver.racm.storem.model.MinimalFileServiceModel;
import org.sciserver.racm.storem.model.RegisterNewDataVolumeModel;
import org.sciserver.racm.storem.model.RegisterNewFileServiceModel;
import org.sciserver.racm.storem.model.RegisterNewRootVolumeModel;
import org.sciserver.racm.storem.model.RegisterNewServiceVolumeModel;
import org.sciserver.racm.storem.model.RegisterNewUserVolumeWithOwnerModel;
import org.sciserver.racm.storem.model.RegisteredDataVolumeModel;
import org.sciserver.racm.storem.model.RegisteredFileServiceModel;
import org.sciserver.racm.storem.model.RegisteredRootVolumeModel;
import org.sciserver.racm.storem.model.RegisteredServiceVolumeModel;
import org.sciserver.racm.storem.model.RegisteredUserVolumeModel;
import org.sciserver.racm.storem.model.RootVolumeModel;
import org.sciserver.racm.storem.model.UserVolumeModel;
import org.sciserver.springapp.racm.utils.RACMUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;

import edu.jhu.file.DataVolume;
import edu.jhu.file.FileService;
import edu.jhu.file.RootVolume;
import edu.jhu.file.UserVolume;
import edu.jhu.rac.AssociatedResource;
import edu.jhu.rac.ContextClass;
import edu.jhu.rac.OwnershipCategory;
import edu.jhu.rac.Privilege;
import edu.jhu.rac.Resource;
import edu.jhu.rac.ResourceContext;
import edu.jhu.rac.ResourceType;
import edu.jhu.user.SciserverEntity;
import edu.jhu.user.ServiceAccount;
import edu.jhu.user.User;
import edu.jhu.user.UserGroup;

/*
 * Helper class for converting to/from vo-urp objects and DTOs.
 */
@Component
class StoremMapper {
	private final RACMUtil racmUtil;
	@Autowired
	public StoremMapper(RACMUtil racmUtil) {
		this.racmUtil = racmUtil;
	}

	Optional<FileServiceModel> getListingDTO(FileService source,
			List<DataVolume> dataVolumes, Map<Long, List<String>> allowedActions,
			Map<VOURPEntityWithResource, Map<SciserverEntity, List<Privilege>>> sharedWithInfo) {
		FileServiceModel model =  new FileServiceModel(
				source.getResourceContext().getUuid(),
				source.getName(),
				source.getDescription(),
				source.getApiEndpoint(),
				mapOptionals(source.getRootVolume(), x -> getListingDTO(x, source.getUserVolumes(), allowedActions, sharedWithInfo)),
				mapOptionals(dataVolumes, x -> getListingDTO(x, allowedActions, sharedWithInfo)));

		long numberOfActions = Stream.of(
				model.getRootVolumes().stream().map(RootVolumeModel::getAllowedActions),
				model.getRootVolumes().stream()
					.flatMap(rv -> rv.getUserVolumes().stream()).map(UserVolumeModel::getAllowedActions))
				.flatMap(x -> x)
				.flatMap(Collection::stream)
				.count();

		if (numberOfActions == 0)
			return Optional.empty();

		return Optional.of(model);
	}

	Optional<RootVolumeModel> getListingDTO(RootVolume source, Collection<UserVolume> allUserVolumes,
			Map<Long, List<String>> allowedActions,
			Map<VOURPEntityWithResource, Map<SciserverEntity, List<Privilege>>> sharedWithInfo) {
		Collection<String> actions = allowedActions.getOrDefault(source.getResource().getId(), Collections.emptyList());
		List<UserVolumeModel> userVolumes =
				mapOptionals(allUserVolumes, x -> getListingDTO(x, allowedActions, sharedWithInfo), uv -> uv.getRootVolume() == source);

		long numberOfActions = Stream.concat(
				actions.stream(),
				userVolumes.stream().map(UserVolumeModel::getAllowedActions).flatMap(List::stream))
				.count();

		if(numberOfActions == 0) return Optional.empty();
		return Optional.of(new RootVolumeModel(
				source.getId(),
				source.getResource().getUuid(),
				source.getName(),
				source.getDescription(),
				source.getPathOnFileSystem(),
				source.getContainsSharedVolumes(),
				userVolumes,
				allowedActions.getOrDefault(source.getResource().getId(), Collections.emptyList())));
	}

	Optional<UserVolumeModel> getListingDTO(UserVolume source, Map<Long, List<String>> allowedActions,
			Map<VOURPEntityWithResource, Map<SciserverEntity, List<Privilege>>> sharedWithInfo) {
		List<String> actions = allowedActions.getOrDefault(source.getResource().getId(), Collections.emptyList());
		Map<SciserverEntity, List<Privilege>> sharedWithForThisUserVolume =
				sharedWithInfo.getOrDefault(VOURPEntityWithResource.wrap(source), Collections.emptyMap());

		if(actions.isEmpty()) return Optional.empty();

		return Optional.of(new UserVolumeModel(
				source.getId(),
				source.getResource().getUuid(),
				source.getName(),
				source.getDescription(),
				source.getRelativePath(),
				source.getOwner().getUsername(),
				actions,
				getListingDTO(sharedWithForThisUserVolume)
				));
	}

	Optional<DataVolumeModel> getListingDTO(DataVolume source, Map<Long, List<String>> allowedActions,
			Map<VOURPEntityWithResource, Map<SciserverEntity, List<Privilege>>> sharedWithInfo) {
		List<String> actions = allowedActions.getOrDefault(source.getResource().getId(), Collections.emptyList());
		Map<SciserverEntity, List<Privilege>> sharedWithForThisDataVolume =
				sharedWithInfo.getOrDefault(VOURPEntityWithResource.wrap(source), Collections.emptyMap());

		if(actions.isEmpty()) return Optional.empty();

		return Optional.of(new DataVolumeModel(
				source.getId(),
				source.getResource().getUuid(),
				source.getName(),
				source.getDescription(),
				source.getDisplayName(),
				source.getPathOnFileSystem(),
				source.getUrl(),
				actions,
				getListingDTO(sharedWithForThisDataVolume)
				));
	}

	private List<SharedWithEntity> getListingDTO(Map<SciserverEntity, List<Privilege>> sharedWithInfo) {
		List<SharedWithEntity> output = new ArrayList<>(sharedWithInfo.size());
		for (Map.Entry<SciserverEntity, List<Privilege>> entry : sharedWithInfo.entrySet()) {
			List<String> actions = entry.getValue().stream().map(p -> p.getAction().getName()).collect(toList());
			if (entry.getKey() instanceof User) {
				User user = (User) entry.getKey();
				output.add(new SharedWithEntity(
						user.getId(), user.getUsername(), SciServerEntityType.USER,
						actions));
			} else if (entry.getKey() instanceof UserGroup) {
				UserGroup group = (UserGroup) entry.getKey();
				output.add(new SharedWithEntity(
						group.getId(), group.getName(), SciServerEntityType.GROUP,
						actions));
			} else if (entry.getKey() instanceof ServiceAccount) { 
			    // TODO decide what should be done with ServiceAccount-s
			    // here returned with its pubDID playing the role of its name 
			    ServiceAccount sa = (ServiceAccount) entry.getKey();
                output.add(new SharedWithEntity(
                        sa.getId(), sa.getPublisherDID(), SciServerEntityType.SERVICE,
                        actions));
            } else {
				throw new IllegalStateException("An unknown SciserverEntity was found. Please tell someone to update StoremMapper.getListingDTO(Privilege)");
			}
		}
		return output;
	}
	RegisteredFileServiceModel getRegisteredFileServiceView(FileService source, List<DataVolume> dataVolumes) {
		return new RegisteredFileServiceModel(
				source.getResourceContext().getUuid(),
				source.getName(),
				source.getServiceToken(),
				source.getDescription(),
				source.getApiEndpoint(),
				map(source.getRootVolume(), this::getRegisteredRootVolume),
				map(dataVolumes, this::getRegisteredDataVolume)
				);
	}

	MinimalFileServiceModel getMinimalFileServiceModel(FileService source) {
		return new MinimalFileServiceModel(
				source.getResourceContext().getUuid(),
				source.getName(),
				source.getDescription(),
				source.getApiEndpoint()
				);
	}

	RegisteredRootVolumeModel getRegisteredRootVolume(RootVolume rv) {
		return new RegisteredRootVolumeModel(
				rv.getName(),
				rv.getDescription(),
				rv.getPathOnFileSystem(),
				rv.getContainsSharedVolumes());
	}

	RegisteredDataVolumeModel getRegisteredDataVolume(DataVolume dv) {
		return new RegisteredDataVolumeModel(
				dv.getId(),
				dv.getName(),
				dv.getDescription(),
				dv.getDisplayName(),
				dv.getPathOnFileSystem(),
				dv.getUrl());
	}

    RegisteredUserVolumeModel getRegisteredUserVolume(UserVolume uv) {
        return new RegisteredUserVolumeModel(
                uv.getId(),
                uv.getName(),
                uv.getDescription(),
                uv.getRelativePath(),
                uv.getOwner().getUsername());
    }
    
   
    RegisteredServiceVolumeModel getRegisteredServiceVolume(UserVolume uv,Resource owningResource, AssociatedResource ar) {
        return new RegisteredServiceVolumeModel(
                uv.getId(),
                uv.getResource().getUuid(),
                uv.getName(),
                uv.getDescription(),
                uv.getRelativePath(),
                uv.getOwner().getUsername(),
                owningResource.getContainer().getUuid(),owningResource.getUuid(),
                ar.getOwnership().value(),
                ar.getUsage());
    }
    
	FileService createFileService(RegisterNewFileServiceModel source, TransientObjectManager tom) {
		FileService output = new FileService(tom);
		output.setName(source.getName());
		output.setDescription(source.getDescription());
		output.setApiEndpoint(source.getApiEndpoint());
		output.setServiceToken(
				source.getServiceToken()
				.orElse(UUID.randomUUID().toString()));

		createResourceContext(output,
				source.getIdentifier().orElse(UUID.randomUUID().toString())
				);

		output.setRootVolume(map(source.getRootVolumes(), r -> createRootVolume(r, output)));

		return output;
	}

	RootVolume createRootVolume(RegisterNewRootVolumeModel source, FileService fileservice) {
		RootVolume output = new RootVolume(fileservice);
		output.setName(source.getName());
		output.setDescription(source.getDescription());
		output.setPathOnFileSystem(source.getPathOnFileSystem());
		output.setContainsSharedVolumes(source.isContainsSharedVolumes());
		output.setResource(createResource(
				fileservice.getResourceContext(),
				RT_FILESERVICE_ROOTVOLUME,
				source.getName(),
				fileservice.getTom()
				));

		return output;
	}

	UserVolume createUserVolume(RegisterNewUserVolumeWithOwnerModel source, RootVolume rootVolume, FileService fileservice) {
		UserVolume output = new UserVolume(fileservice);
		output.setName(source.getName());
		output.setDescription(source.getDescription());
		output.setRelativePath(source.getRelativePath());
		output.setOwner(source.getOwner());
		output.setRootVolume(rootVolume);

		output.setResource(createResource(
				fileservice.getResourceContext(),
				RT_FILESERVICE_USERVOLUME,
				UriComponentsBuilder.fromPath(rootVolume.getName()).pathSegment(source.getOwner().getUsername()).pathSegment(source.getName()).build().toUriString(),
				fileservice.getTom()
				));

		return output;
	}
    UserVolume createServiceVolume(User owner, RegisterNewServiceVolumeModel source, RootVolume rootVolume, FileService fileservice) {
        UserVolume output = new UserVolume(fileservice);
        output.setName(source.getName());
        output.setDescription(source.getDescription());
        output.setRelativePath(source.getRelativePath());
        output.setOwner(owner);
        output.setRootVolume(rootVolume);

        output.setResource(createResource(
                fileservice.getResourceContext(),
                RT_FILESERVICE_USERVOLUME,
                source.getName(),
                UriComponentsBuilder.fromPath(rootVolume.getName()).pathSegment(owner.getUsername()).pathSegment(source.getName()).build().toUriString(),
                source.getDescription(),
                fileservice.getTom()
                ));

        return output;
    }
	DataVolume createDataVolume(RegisterNewDataVolumeModel source, FileService fileservice) {
		DataVolume output = new DataVolume(fileservice.getTom());
		output.setName(source.getName());
		output.setDescription(source.getDescription());
		output.setDisplayName(source.getDisplayName());
		output.setPathOnFileSystem(source.getPathOnFileSystem());
		output.setUrl(source.getUrl());
		output.setFileService(fileservice);

		output.setResource(createResource(
				fileservice.getResourceContext(),
				RT_FILESERVICE_DATAVOLUME,
				source.getDisplayName(),
				source.getName(),				
				source.getDescription(),
				fileservice.getTom()
				));
		return output;
	}

	private void createResourceContext(FileService fileService, String uuid) {
		TransientObjectManager tom = fileService.getTom();
		ResourceContext rc = new ResourceContext(tom);

		try {
			rc.setContextClass(getContextClass(tom));
		} catch (VOURPException e) {
			throw new IllegalStateException(e);
		}

		rc.setUuid(uuid);
		rc.setRacmEndpoint(fileService.getApiEndpoint());
		rc.setDescription("ResourceContext representing the File Service at " + fileService.getApiEndpoint());
		fileService.setResourceContext(rc);
	}

	private Resource createResource(ResourceContext context, String rt, String publisherDID, TransientObjectManager tom) {
		ResourceType resourceType = getResourceType(rt, tom);
		return createResource(context, publisherDID, resourceType);
	}
	
    private Resource createResource(ResourceContext context, String rt, String name, String publisherDID, String description, TransientObjectManager tom) {
        ResourceType resourceType = getResourceType(rt, tom);
        Resource r = createResource(context, publisherDID, resourceType);
        r.setName(name);
        r.setDescription(description);
        return r;
    }
    
	private Resource createResource(ResourceContext context, String publisherDID, ResourceType rt) {
		Resource resource = racmUtil.newResource(context);
		resource.setResourceType(rt);
		resource.setPublisherDID(publisherDID);
		return resource;
	}

	/* Map a potentially lazy-loaded collection
	 * This has the complication that old versions of eclipse link
	 * do not support the stream() API in Java 8.
	 * We defensively copy the collection to be safe.
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=433075 and
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=467470
	 * describes these and some of the design problems.
	 */
	private <T,E> List<T> mapOptionals(Collection<E> collection, Function<E, Optional<T>> func, Predicate<E> filter) {
		if(collection == null) return Collections.emptyList();
		return new ArrayList<>(collection).stream()
				.filter(filter)
				.map(func)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.collect(Collectors.toList());
	}
	private <T, E> List<T> mapOptionals(Collection<E> collection, Function<E, Optional<T>> func) {
		return mapOptionals(collection, func, x -> true);
	}

	private <T,E> List<T> map(Collection<E> collection, Function<E, T> func, Predicate<E> filter) {
		if(collection == null) return Collections.emptyList();
		return new ArrayList<>(collection).stream()
				.filter(filter)
				.map(func)
				.collect(Collectors.toList());
	}
	private <T, E> List<T> map(Collection<E> collection, Function<E, T> func) {
		return map(collection, func, x -> true);
	}

	private ResourceType getResourceType(String resourceTypeName, TransientObjectManager tom) {
		return racmUtil.queryResourceType(CC_FILESERVICE_NAME, resourceTypeName, tom);
	}

	private ContextClass getContextClass(TransientObjectManager tom) throws VOURPException {
		return racmUtil.queryContextClass(CC_FILESERVICE_NAME, tom);
	}
}
