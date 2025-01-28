package org.sciserver.racm.resources.model;

import java.util.ArrayList;
import java.util.List;


/**
 * @author gerard
 *
 */
public class UserResourceModel {

	private static final String SQL_COLUMN_NAMES = "resourceuuid,action,resourceType,resourceName, resourcePubDID";
	public static final String SQL =
			String.format("select distinct %s from racm.userActionsOnContext(?,?) order by 1,2",SQL_COLUMN_NAMES);

	private String resourceType;
	private String resourceuuid;
	private String resourceName;
	private String resourcePubDID;
	private List<String> actions;
	/**
	 * Initialize with a Object[] array in order:
	 *
	 * @param row
	 */
	public UserResourceModel(Object[] row){
		int i = 0;
		this.resourceuuid=(String)row[i++];
		String action=(String)row[i++];
		this.actions = new ArrayList<>();
		this.actions.add(action);
		this.resourceType=(String)row[i++];
		this.resourceName=(String)row[i++];
		this.resourcePubDID=(String)row[i++];
	}

	// Empty constructor needed for Jackson
	public UserResourceModel() {
	}


	public String getResourceType() {
		return resourceType;
	}

	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}

	public String getResourceuuid() {
		return resourceuuid;
	}

	public void setResourceuuid(String resourceuuid) {
		this.resourceuuid = resourceuuid;
	}

	public List<String> getActions() {
		return actions;
	}

	public void addAction(String action) {
		this.actions.add(action);
	}

	public String getResourceName() {
		return resourceName;
	}

	public void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}

	public String getResourcePubDID() {
		return resourcePubDID;
	}

	public void setResourcePubDID(String resourcePubDID) {
		this.resourcePubDID = resourcePubDID;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((resourceuuid == null) ? 0 : resourceuuid.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UserResourceModel other = (UserResourceModel) obj;
		if (resourceuuid == null) {
			if (other.resourceuuid != null)
				return false;
		} else if (!resourceuuid.equals(other.resourceuuid)) {
			return false;
		}
		return true;
	}
}
