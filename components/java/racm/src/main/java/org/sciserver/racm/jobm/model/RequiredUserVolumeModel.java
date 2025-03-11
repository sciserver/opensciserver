package org.sciserver.racm.jobm.model;

import org.sciserver.racm.utils.model.RACMBaseModel;

public class RequiredUserVolumeModel extends RACMBaseModel {

	private Long userVolumeId;
	private Boolean needsWriteAccess;
	private String fullPath;
	
	public RequiredUserVolumeModel() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	public RequiredUserVolumeModel(Long id) {
		super(id);
		// TODO Auto-generated constructor stub
	}

	public RequiredUserVolumeModel(String id) {
		super(id);
		// TODO Auto-generated constructor stub
	}

	public Long getUserVolumeId() {
		return userVolumeId;
	}

	public void setUserVolumeId(Long userVolumeId) {
		this.userVolumeId = userVolumeId;
	}

	public Boolean isNeedsWriteAccess() {
		return needsWriteAccess;
	}

	public void setNeedsWriteAccess(Boolean readOnly) {
		this.needsWriteAccess = readOnly;
	}

	public String getFullPath() {
		return fullPath;
	}

	public void setFullPath(String fullPath) {
		this.fullPath = fullPath;
	}

	
}
