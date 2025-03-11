package org.sciserver.racm.rctree.model;

import org.sciserver.racm.utils.model.VOURPBaseModel;

public abstract class AccessControlModel extends VOURPBaseModel{

	private Long resourceId;
	private Long scisId;
	private String scisName;
	private String scisType;

	public AccessControlModel(Long id){
		super(id);
	}
	public AccessControlModel(){
	}
	public AccessControlModel(Long resourceId, Long scisId){
		this.resourceId = resourceId;
		this.scisId = scisId;
	}

	/**
	 * to be used when querying privileges form the database.<br/>
	 * @param p
	 */
	public AccessControlModel(long id) {
		super(id);
	}

	/**
	 * To be used by spring.<br/>
	 * @return
	 */
	public Long getResourceId() {
		return resourceId;
	}
	public void setResourceId(Long resourceId) {
		this.resourceId = resourceId;
	}
	public Long getScisId() {
		return scisId;
	}
	public void setScisId(Long scisId) {
		this.scisId = scisId;
	}
	public String getScisName() {
		return scisName;
	}
	public void setScisName(String scisName) {
		this.scisName = scisName;
	}
	public String getScisType() {
		return scisType;
	}
	public void setScisType(String scisType) {
		this.scisType = scisType;
	}

}
