package org.sciserver.racm.rctree.model;


public class RoleAssignmentModel extends AccessControlModel{

	private Long roleId;
	private String roleName;
	public RoleAssignmentModel(){
		super();
	}
	public RoleAssignmentModel(Long id){
		super(id);
	}

	/**
	 * To be used when saving the model to the DB.<br/>
	 * @param resourceId
	 * @param scisId
	 * @param actionId
	 */
	public RoleAssignmentModel(Long resourceId, Long scisId, Long roleId){
		super(resourceId,scisId);
		this.roleId = roleId;
	}
	public Long getRoleId() {
		return roleId;
	}

	public void setRoleId(Long roleId) {
		this.roleId = roleId;
	}

	public String getRoleName() {
		return roleName;
	}

	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}
}
