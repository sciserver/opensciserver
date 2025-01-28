/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0. 
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/
package org.sciserver.compute.core.registry;

import java.io.IOException;
import java.net.URL;
import java.util.Date;

import org.sciserver.compute.core.client.docker.DockerClient;
import org.sciserver.compute.core.client.httpproxy.HttpProxyClient;

import com.fasterxml.jackson.databind.JsonNode;

public class Node extends RegistryObject {

	private String name;

	private String description;

	private URL dockerApiUrl;

	private String dockerApiClientCert;

	private String dockerApiClientKey;

	private URL proxyApiUrl;

	private String proxyApiClientCert;

	private String proxyApiClientKey;

	private URL proxyBaseUrl;

	private long domainId;

	private boolean enabled;

	public Node(Registry registry) {
		super(registry);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public URL getDockerApiUrl() {
		return dockerApiUrl;
	}

	public void setDockerApiUrl(URL dockerApiUrl) {
		this.dockerApiUrl = dockerApiUrl;
	}

	public String getDockerApiClientCert() {
		return dockerApiClientCert;
	}

	public void setDockerApiClientCert(String dockerApiClientCert) {
		this.dockerApiClientCert = dockerApiClientCert;
	}

	public String getDockerApiClientKey() {
		return dockerApiClientKey;
	}

	public void setDockerApiClientKey(String dockerApiClientKey) {
		this.dockerApiClientKey = dockerApiClientKey;
	}

	public URL getProxyApiUrl() {
		return proxyApiUrl;
	}

	public void setProxyApiUrl(URL proxyApiUrl) {
		this.proxyApiUrl = proxyApiUrl;
	}

	public String getProxyApiClientCert() {
		return proxyApiClientCert;
	}

	public void setProxyApiClientCert(String proxyApiClientCert) {
		this.proxyApiClientCert = proxyApiClientCert;
	}

	public String getProxyApiClientKey() {
		return proxyApiClientKey;
	}

	public void setProxyApiClientKey(String proxyApiClientKey) {
		this.proxyApiClientKey = proxyApiClientKey;
	}

	public URL getProxyBaseUrl() {
		return proxyBaseUrl;
	}

	public void setProxyBaseUrl(URL proxyBaseUrl) {
		this.proxyBaseUrl = proxyBaseUrl;
	}

	public long getDomainId() {
		return domainId;
	}

	public void setDomainId(long domainId) {
		this.domainId = domainId;
	}

	public Domain getDomain() throws Exception {
		return registry.getDomain(domainId);
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public DockerClient createDockerClient() throws IOException {
		return registry.createDockerClient(dockerApiUrl, dockerApiClientCert, dockerApiClientKey);

	}

	public HttpProxyClient createProxyClient() throws IOException {
		return registry.createHttpProxyClient(proxyApiUrl, proxyApiClientCert, proxyApiClientKey);
	}

	public JsonNode getStatus() throws Exception {
		DockerClient client = createDockerClient();
		JsonNode json = client.getInfo(5 * 1000);
		return json;
	}

	public JsonNode getProxyRoutes(Date inactiveSince) throws Exception {
		HttpProxyClient client = createProxyClient();
		JsonNode json = client.getRoutes(inactiveSince, 5 * 1000);
		return json;
	}
	
	public long getTotalSlots() throws Exception {
		return registry.getTotalSlots(this);
	}
	
	public long getUsedSlots() throws Exception {
		return registry.getUsedSlots(this);
	}
}
