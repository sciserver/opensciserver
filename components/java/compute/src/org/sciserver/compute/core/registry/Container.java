/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/
package org.sciserver.compute.core.registry;

import java.sql.SQLException;

import org.sciserver.compute.core.client.docker.DockerClient;
import org.sciserver.compute.core.client.docker.LogSource;
import org.sciserver.compute.core.container.ExecutableManager;

import com.fasterxml.jackson.databind.JsonNode;

public abstract class Container extends RegistryObject {

	private String userId;

	private String externalRef;

	private String dockerRef;

	private String status;

	public abstract Image getImage() throws Exception;

	public abstract Node getNode() throws Exception;

	public abstract void register() throws Exception;

	public abstract void unregister() throws SQLException;

	public abstract void update() throws SQLException;

	public Container(Registry registry) {
		super(registry);
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getExternalRef() {
		return externalRef;
	}

	public void setExternalRef(String externalRef) {
		this.externalRef = externalRef;
	}

	public String getDockerRef() {
		return dockerRef;
	}

	public void setDockerRef(String dockerRef) {
		this.dockerRef = dockerRef;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public JsonNode getInfo() throws Exception {
		ExecutableManager m = (ExecutableManager)registry.createContainerManager(getImage());
		return m.getInfo(this);
	}

	public String getStdOut() throws Exception {
		ExecutableManager m = (ExecutableManager)registry.createContainerManager(getImage());
		return m.getStdOut(this);
	}

	public String getStdErr() throws Exception {
		ExecutableManager m = (ExecutableManager)registry.createContainerManager(getImage());
		return m.getStdErr(this);
	}
}
