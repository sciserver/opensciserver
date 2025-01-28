package org.sciserver.compute.core.registry;

import java.util.Date;

public class DaskCluster extends RegistryObject {
	
	private String name;
	private String description;
	private Date createdAt;
	private String externalRef;
	private long k8sClusterId;
	private long imageId;
	private String status;
	private String userId;
	
	public DaskCluster(Registry registry) {
		super(registry);
	}

	public K8sCluster getK8sCluster() throws Exception {
		return registry.getK8sCluster(k8sClusterId);
	}
	
	public Image getImage() throws Exception {
		return registry.getExecutableImage(imageId);
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

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public String getExternalRef() {
		return externalRef;
	}

	public void setExternalRef(String externalRef) {
		this.externalRef = externalRef;
	}

	public long getK8sClusterId() {
		return k8sClusterId;
	}

	public void setK8sClusterId(long k8sClusterId) {
		this.k8sClusterId = k8sClusterId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public long getImageId() {
		return imageId;
	}

	public void setImageId(long imageId) {
		this.imageId = imageId;
	}

	public String getDashboardUrl() throws Exception {
		K8sCluster k8s = getK8sCluster();
		return k8s.getPublicUrl() + getExternalRef() + "/dashboard/";
	}
}
