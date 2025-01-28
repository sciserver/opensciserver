package org.sciserver.compute.core.registry;

import java.net.URL;

public class K8sCluster extends RegistryObject {
	private String name;
	private String description;
	private URL apiUrl;
	private String apiToken;
	private URL publicUrl;
	private long domainId;
	private boolean enabled;
	private String namespace;
	private float memOvercommit;
	private float cpuOvercommit;

	public K8sCluster(Registry registry) {
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

	public URL getApiUrl() {
		return apiUrl;
	}

	public void setApiUrl(URL apiUrl) {
		this.apiUrl = apiUrl;
	}

	public String getApiToken() {
		return apiToken;
	}

	public void setApiToken(String apiToken) {
		this.apiToken = apiToken;
	}

	public URL getPublicUrl() {
		return publicUrl;
	}

	public void setPublicUrl(URL publicUrl) {
		this.publicUrl = publicUrl;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public float getMemOvercommit() {
		return memOvercommit;
	}

	public void setMemOvercommit(float memOvercommit) {
		this.memOvercommit = memOvercommit;
	}

	public float getCpuOvercommit() {
		return cpuOvercommit;
	}

	public void setCpuOvercommit(float cpuOvercommit) {
		this.cpuOvercommit = cpuOvercommit;
	}

	public long getDomainId() {
		return domainId;
	}

	public void setDomainId(long domainId) {
		this.domainId = domainId;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Domain getDomain() throws Exception {
		return registry.getDomain(domainId);
	}
}