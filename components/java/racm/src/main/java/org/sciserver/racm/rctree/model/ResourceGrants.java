package org.sciserver.racm.rctree.model;

import java.util.ArrayList;
import java.util.List;

import org.sciserver.racm.cctree.model.ResourceTypeModel;
import org.sciserver.racm.utils.model.VOURPBaseModel;

/**
 * This class represents the granting of privileges or role assignments on a resource to a sciserverentity.<br/>
 * @author gerard
 *
 */
public class ResourceGrants extends VOURPBaseModel {
	private String contextuuid;
	private String resourceName;
	private String resourceId;
	private String resourceUUID;
	private String resourceDescription;
	private String contextClass;
	private ResourceTypeModel rtm;

	private List<PrivilegeModel> privileges = new ArrayList<>();
	private List<RoleAssignmentModel> roles = new ArrayList<>();


	public ResourceGrants(){}

	public ResourceGrants(long id){
		super(id);
	}
	public String getResourceName() {
		return resourceName;
	}
	public void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}
	public String getResourceId() {
		return resourceId;
	}
	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}
	public String getResourceDescription() {
		return resourceDescription;
	}
	public void setResourceDescription(String resourceDescription) {
		this.resourceDescription = resourceDescription;
	}
	public String getContextClass() {
		return contextClass;
	}
	public void setContextClass(String contextClass) {
		this.contextClass = contextClass;
	}
	public ResourceTypeModel getRtm() {
		return rtm;
	}
	public void setRtm(ResourceTypeModel rtm) {
		this.rtm = rtm;
	}
	public String getContextuuid() {
		return contextuuid;
	}
	public void setContextuuid(String contextuuid) {
		this.contextuuid = contextuuid;
	}
	public List<PrivilegeModel> getPrivileges() {
		return privileges;
	}
	public void setPrivileges(List<PrivilegeModel> privileges) {
		this.privileges = privileges;
	}
	public List<RoleAssignmentModel> getRoles() {
		return roles;
	}
	public void setRoles(List<RoleAssignmentModel> roles) {
		this.roles = roles;
	}

	public String getResourceUUID() {
		return resourceUUID;
	}

	public void setResourceUUID(String resourceUUID) {
		this.resourceUUID = resourceUUID;
	}

}
