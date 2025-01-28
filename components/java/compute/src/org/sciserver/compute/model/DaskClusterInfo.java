/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/
package org.sciserver.compute.model;

public class DaskClusterInfo {
	private long id;
	private String externalRef;
	private String userId;
	private String dashboardUrl;
	private DaskConnectionInfo connection;
	
	public String getDashboardUrl() {
		return dashboardUrl;
	}
	public void setDashboardUrl(String dashboardUrl) {
		this.dashboardUrl = dashboardUrl;
	}
	public String getExternalRef() {
		return externalRef;
	}
	public void setExternalRef(String refId) {
		this.externalRef = refId;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public DaskConnectionInfo getConnection() {
		return connection;
	}
	public void setConnection(DaskConnectionInfo connection) {
		this.connection = connection;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
}
