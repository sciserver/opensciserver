package org.sciserver.racm.resources.v2.model;

import java.util.List;

public class UserVolumeResource extends AbstractResourceModel {
	private final String owner;
	private final RootVolume rootVolume;
	private final FileService fileService;

	public UserVolumeResource(long entityId, String resourceUUID, String name, String description,
			List<ActionModel> allowedActions, List<ActionModel> possibleActions, String owner,
			RootVolume rootVolume, FileService fileService) {
		super(entityId, resourceUUID, name, description, allowedActions, possibleActions, "USERVOLUME");
		this.owner = owner;
		this.rootVolume = rootVolume;
		this.fileService = fileService;
	}

	public String getOwner() {
		return owner;
	}

	public RootVolume getRootVolume() {
		return rootVolume;
	}

	public FileService getFileService() {
		return fileService;
	}

	/* This kind of code is what auto value, lombok,
	 * kotlin data classes, immutables, etc are useful for */
	public static class RootVolume {
		private final String name;
		private final String description;
		private final long id;
		private final boolean containsSharableVolumes;
		public RootVolume(String name, String description, long id, boolean containsSharableVolumes) {
			this.name = name;
			this.description = description;
			this.id = id;
			this.containsSharableVolumes = containsSharableVolumes;
		}
		public String getName() {
			return name;
		}
		public String getDescription() {
			return description;
		}
		public long getId() {
			return id;
		}
		public boolean isContainsSharableVolumes() {
			return containsSharableVolumes;
		}
	}

	public static class FileService {
		private final String identifier;
		private final String name;
		private final String description;
		private final String apiEndpoint;
		public FileService(String identifier, String name, String description, String apiEndpoint) {
			this.identifier = identifier;
			this.name = name;
			this.description = description;
			this.apiEndpoint = apiEndpoint;
		}
		public String getIdentifier() {
			return identifier;
		}
		public String getName() {
			return name;
		}
		public String getDescription() {
			return description;
		}
		public String getApiEndpoint() {
			return apiEndpoint;
		}
	}
}
