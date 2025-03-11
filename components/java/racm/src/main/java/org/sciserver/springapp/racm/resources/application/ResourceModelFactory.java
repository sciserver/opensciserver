package org.sciserver.springapp.racm.resources.application;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.sciserver.racm.resources.v2.model.ActionModel;
import org.sciserver.racm.resources.v2.model.DataVolumeResource;
import org.sciserver.racm.resources.v2.model.DatabaseResource;
import org.sciserver.racm.resources.v2.model.DockerComputeDomainForResources;
import org.sciserver.racm.resources.v2.model.DockerImageResource;
import org.sciserver.racm.resources.v2.model.GenericResource;
import org.sciserver.racm.resources.v2.model.ResourceModel;
import org.sciserver.racm.resources.v2.model.UserVolumeResource;
import org.sciserver.racm.resources.v2.model.VolumeContainerResource;
import org.springframework.stereotype.Component;

import edu.jhu.file.DataVolume;
import edu.jhu.file.FileService;
import edu.jhu.file.RootVolume;
import edu.jhu.file.UserVolume;
import edu.jhu.job.DatabaseContext;
import edu.jhu.job.DockerComputeDomain;
import edu.jhu.job.DockerImage;
import edu.jhu.job.RDBComputeDomain;
import edu.jhu.job.VolumeContainer;
import edu.jhu.rac.Action;
import edu.jhu.rac.ActionCategory;
import edu.jhu.rac.ContextClass;
import edu.jhu.rac.Resource;
import edu.jhu.rac.ResourceContext;

@Component
public class ResourceModelFactory {
	private ActionModel convertAction(Action action) {
		return new ActionModel(action.getName(), action.getDescription(), action.getCategory());
	}

	public ResourceModel convertToResourceModel(UserVolume userVolume, List<Action> allowedActions) {
		RootVolume rv = userVolume.getRootVolume();
		FileService fs = userVolume.getContainer();
		return new UserVolumeResource(userVolume.getId(), userVolume.getResource().getUuid(),
				userVolume.getName(), userVolume.getDescription(),
				convertToActions(allowedActions),
				getPossibleActionsIfGrantable(userVolume.getResource(), allowedActions),
				userVolume.getOwner().getUsername(),
				new UserVolumeResource.RootVolume(
						rv.getName(), rv.getDescription(), rv.getId(), rv.getContainsSharedVolumes()),
				new UserVolumeResource.FileService(
						fs.getResourceContext().getUuid(),
						fs.getName(),
						fs.getDescription(), fs.getApiEndpoint()));
	}

	public ResourceModel convertToResourceModel(DockerImage dockerImage, List<Action> allowedActions) {
		Resource resource = dockerImage.getResource();
		DockerComputeDomain dockerComputeDomain = dockerImage.getContainer();
		return new DockerImageResource(dockerImage.getId(), resource.getUuid(),
				dockerImage.getName(), dockerImage.getDescription(),
				convertToActions(allowedActions),
				getPossibleActionsIfGrantable(resource, allowedActions),
				new DockerComputeDomainForResources(
						dockerComputeDomain.getName(),
						dockerComputeDomain.getDescription(),
						dockerComputeDomain.getApiEndpoint()));
	}

	public ResourceModel convertToResourceModel(VolumeContainer volumeContainer, List<Action> allowedActions) {
		Resource resource = volumeContainer.getResource();
		DockerComputeDomain dockerComputeDomain = volumeContainer.getContainer();
		return new VolumeContainerResource(volumeContainer.getId(), resource.getUuid(),
				volumeContainer.getName(), volumeContainer.getDescription(),
				convertToActions(allowedActions),
				getPossibleActionsIfGrantable(resource, allowedActions),
				new DockerComputeDomainForResources(
						dockerComputeDomain.getName(),
						dockerComputeDomain.getDescription(),
						dockerComputeDomain.getApiEndpoint()));
	}

	public ResourceModel convertToResourceModel(DataVolume dataVolume, List<Action> allowedActions) {
		FileService fs = dataVolume.getFileService();
		return new DataVolumeResource(dataVolume.getId(), dataVolume.getResource().getUuid(),
				dataVolume.getName(), dataVolume.getDescription(),
				convertToActions(allowedActions),
				getPossibleActionsIfGrantable(dataVolume.getResource(), allowedActions),
				new UserVolumeResource.FileService(
						fs.getResourceContext().getUuid(),
						fs.getName(),
						fs.getDescription(), fs.getApiEndpoint()),
				dataVolume.getDisplayName(),
				dataVolume.getPathOnFileSystem(),
				dataVolume.getUrl());
	}

	public ResourceModel convertToResourceModel(DatabaseContext databaseContext, List<Action> allowedActions) {
		Resource resource = databaseContext.getResource();
		RDBComputeDomain rdbComputeDomain = databaseContext.getContainer();
		return new DatabaseResource(databaseContext.getId(), resource.getUuid(),
				databaseContext.getName(), databaseContext.getDescription(),
				convertToActions(allowedActions),
				getPossibleActionsIfGrantable(resource, allowedActions),
				new DatabaseResource.RDBComputeDomain(
						rdbComputeDomain.getName(),
						rdbComputeDomain.getDescription(),
						rdbComputeDomain.getApiEndpoint(),
						rdbComputeDomain.getVendor().toString()));
	}

	public ResourceModel convertToResourceModel(Resource resource, List<Action> allowedActions) {
		ResourceContext resourceContext = resource.getContainer();
		ContextClass contextClass = resourceContext.getContextClass();
		return new GenericResource(resource.getId(), resource.getUuid(), resource.getName(),
				resource.getDescription(),
				convertToActions(allowedActions),
				getPossibleActionsIfGrantable(resource, allowedActions),
				resource.getResourceType().getName(),
				new GenericResource.ResourceContext(
						resourceContext.getRacmEndpoint(), resourceContext.getDescription()),
				new GenericResource.ContextClass(
						contextClass.getName(), contextClass.getDescription()));
	}

	private List<ActionModel> convertToActions(List<Action> actions) {
		return actions.stream().map(this::convertAction).collect(Collectors.toList());
	}

	private List<ActionModel> getPossibleActionsIfGrantable(Resource resource, List<Action> allowedActions) {
		return allowedActions.stream().filter(a -> a.getCategory() == ActionCategory.G).findFirst()
			.map(a -> convertToActions(resource.getResourceType().getAction()))
			.orElse(Collections.emptyList());
	}
}
