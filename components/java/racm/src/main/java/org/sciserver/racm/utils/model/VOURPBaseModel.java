package org.sciserver.racm.utils.model;

public abstract class VOURPBaseModel {

	private Long id;
	/**
	 * An identifier possibly assigned by the owner/publisher of a model object.
	 */
	private String publisherDID;

	public VOURPBaseModel(){
		this.id=null;
	}
	public VOURPBaseModel(Long id){
		this.id = id;
	}

	public VOURPBaseModel(String id){
		this.id = Long.valueOf(id);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	public String getPublisherDID() {
		return publisherDID;
	}
	public void setPublisherDID(String pubDID) {
		this.publisherDID = pubDID;
	}

}
