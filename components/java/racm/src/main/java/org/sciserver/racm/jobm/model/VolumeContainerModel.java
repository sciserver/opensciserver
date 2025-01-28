package org.sciserver.racm.jobm.model;

public class VolumeContainerModel extends ComputeResourceModel{

	private Boolean writable=false;
	public VolumeContainerModel() {
		super();
		// TODO Auto-generated constructor stub
	}

	public VolumeContainerModel(Long id) {
		super(id);
		// TODO Auto-generated constructor stub
	}

	public VolumeContainerModel(String id) {
		super(id);
		// TODO Auto-generated constructor stub
	}

	public Boolean isWritable() {
		return writable;
	}

	public void setWritable(Boolean writable) {
		this.writable = writable;
	}

}
