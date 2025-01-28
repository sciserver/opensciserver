package org.sciserver.racm.jobm.model;

import org.sciserver.racm.utils.model.RACMBaseModel;

public abstract class ComputeResourceModel extends RACMBaseModel{

	private String name;
	private String description;

	public ComputeResourceModel() {
		super();
	}

	public ComputeResourceModel(Long id) {
		super(id);
	}

	public ComputeResourceModel(String id) {
		super(id);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}
