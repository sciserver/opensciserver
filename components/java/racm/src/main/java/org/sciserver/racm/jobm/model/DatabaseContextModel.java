package org.sciserver.racm.jobm.model;

import org.sciserver.racm.utils.model.RACMBaseModel;

public class DatabaseContextModel extends RACMBaseModel{

	private String racmUUID;
	private String name;
	private String description;
	public DatabaseContextModel() {
		super();
	}

	public DatabaseContextModel(Long id) {
		super(id);
	}

	public DatabaseContextModel(String id) {
		super(id);
	}

	@Override
	public String getRacmUUID() {
		return racmUUID;
	}

	@Override
	public void setRacmUUID(String racmUUID) {
		this.racmUUID = racmUUID;
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
