package org.sciserver.racm.resources.v2.model;

import java.util.List;

import org.sciserver.racm.resources.v2.model.UserVolumeResource.FileService;

public class DataVolumeResource extends AbstractResourceModel {
	private final FileService fileService;
	private final String displayName;
	private final String pathOnFileSystem;
	private final String url;

	public DataVolumeResource(long entityId, String resourceUUID, String name, String description,
			List<ActionModel> allowedActions, List<ActionModel> possibleActions,
			FileService fileService, String displayName, String pathOnFileSystem,
			String url) {
		super(entityId, resourceUUID, name, description, allowedActions, possibleActions, "DATAVOLUME");
		this.fileService = fileService;
		this.displayName = displayName;
		this.pathOnFileSystem = pathOnFileSystem;
		this.url = url;
	}

	public FileService getFileService() {
		return fileService;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getPathOnFileSystem() {
		return pathOnFileSystem;
	}

	public String getUrl() {
		return url;
	}
}
