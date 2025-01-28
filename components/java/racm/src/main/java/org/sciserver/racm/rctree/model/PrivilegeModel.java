package org.sciserver.racm.rctree.model;

public class PrivilegeModel extends AccessControlModel{

	private Long actionId;
	private String actionName;
	
	public PrivilegeModel(Long id){
		super(id);
	}
	public PrivilegeModel(){
		
	}

	/**
	 * To be used when saving the model to the DB.<br/>
	 * @param resourceId
	 * @param scisId
	 * @param actionId
	 */
	public PrivilegeModel(Long resourceId, Long scisId, Long actionId){
		super(resourceId,scisId);
		this.actionId = actionId;
	}
	public Long getActionId() {
		return actionId;
	}
	public void setActionId(Long actionId) {
		this.actionId = actionId;
	}
	public String getActionName() {
		return actionName;
	}
	public void setActionName(String actionName) {
		this.actionName = actionName;
	}
}
