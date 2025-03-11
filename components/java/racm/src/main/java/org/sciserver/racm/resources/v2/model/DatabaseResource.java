package org.sciserver.racm.resources.v2.model;

import java.util.List;

public class DatabaseResource extends AbstractResourceModel {
	private final RDBComputeDomain rdbComputeDomain;
	public DatabaseResource(long entityId, String resourceUUID, String name, String description,
			List<ActionModel> allowedActions, List<ActionModel> possibleActions,
			RDBComputeDomain rdbComputeDomain) {
		super(entityId, resourceUUID, name, description, allowedActions, possibleActions, "DATABASE");
		this.rdbComputeDomain = rdbComputeDomain;
	}

	public RDBComputeDomain getRdbComputeDomain() {
		return rdbComputeDomain;
	}

	public static class RDBComputeDomain {
		private final String name;
		private final String description;
		private final String apiEndpoint;
		private final String vendor;
		public RDBComputeDomain(String name, String description, String apiEndpoint, String vendor) {
			this.name = name;
			this.description = description;
			this.apiEndpoint = apiEndpoint;
			this.vendor = vendor;
		}
		public String getName() {
			return name;
		}
		public String getDescription() {
			return description;
		}
		public String getApiEndpoint() {
			return apiEndpoint;
		}
		public String getVendor() {
			return vendor;
		}
	}
}
