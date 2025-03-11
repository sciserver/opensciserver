package org.sciserver.racm.utils.model;

public abstract class RACMBaseModel extends VOURPBaseModel {
	/** String by which a RACM object is registered, either as a RACM::Resource or RACM::ResoureContext.*/
	private String racmUUID;
	public RACMBaseModel(Long id) {
		super(id);
	}

	public RACMBaseModel(String id) {
		super(id);
	}

	public RACMBaseModel(){
  }

	public String getRacmUUID() {
		return racmUUID;
	}

	public void setRacmUUID(String racmUUID) {
		this.racmUUID = racmUUID;
	}
}
