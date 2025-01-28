package org.sciserver.compute.model.admin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NodeInfo {
	private Long id;
	
	private String name;
	
	private String description;
	
	@JsonProperty("docker_api_url")
	private String dockerApiUrl;
	
	@JsonProperty("docker_api_client_cert")
	private String dockerApiClientCert;

	@JsonProperty("docker_api_client_key")
	private String dockerApiClientKey;
	
	@JsonProperty("proxy_api_url")
	private String proxyApiUrl;
	
	@JsonProperty("proxy_api_client_cert")
	private String proxyApiClientCert;

	@JsonProperty("proxy_api_client_key")
	private String proxyApiClientKey;

	@JsonProperty("proxy_base_url")
	private String proxyBaseUrl;
	
	@JsonProperty("domain_id")
	private long domainId;

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

	public String getDockerApiUrl() {
		return dockerApiUrl;
	}

	public void setDockerApiUrl(String dockerApiUrl) {
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

	public String getProxyApiUrl() {
		return proxyApiUrl;
	}

	public void setProxyApiUrl(String proxyApiUrl) {
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

	public String getProxyBaseUrl() {
		return proxyBaseUrl;
	}

	public void setProxyBaseUrl(String proxyBaseUrl) {
		this.proxyBaseUrl = proxyBaseUrl;
	}

	public long getDomainId() {
		return domainId;
	}

	public void setDomainId(long domainId) {
		this.domainId = domainId;
	}
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
