package org.sciserver.racm.jobm.model;

import org.sciserver.racm.utils.model.VOURPBaseModel;

public class RootVolumeOnComputeDomainModel extends VOURPBaseModel{

	private String pathOnCD;
	private String displayName;
	/**
	 * VO-URP id of the rootVolume also mounted on the compute domain
	 */
	private Long rootVolumeId;

	public RootVolumeOnComputeDomainModel() {
		super();
	}

	public RootVolumeOnComputeDomainModel(Long id) {
		super(id);
	}

	public RootVolumeOnComputeDomainModel(String id) {
		super(id);
	}

	public String getPathOnCD() {
		return pathOnCD;
	}

	public void setPathOnCD(String pathOnCD) {
		this.pathOnCD = pathOnCD;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public Long getRootVolumeId() {
		return rootVolumeId;
	}

	public void setRootVolumeId(Long rootVolumeId) {
		this.rootVolumeId = rootVolumeId;
	}

}
