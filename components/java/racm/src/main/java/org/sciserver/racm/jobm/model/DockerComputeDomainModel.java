package org.sciserver.racm.jobm.model;

import java.util.List;

import org.sciserver.racm.utils.model.RACMBaseModel;
/**
 * This class represents metadata by which a DockerComputeDomain is registered.<br/> 
 * It isn to be used to create or update a registered DOckerComputeDomain.
 * @author gerard
 *
 */
public class DockerComputeDomainModel extends RACMBaseModel{
	private String apiEndpoint;
	private String name;
	private String description;
	private List<VolumeContainerModel> volumes;
	private List<DockerImageModel> images;
	private List<RootVolumeOnComputeDomainModel> rootVolumes;
	
	public DockerComputeDomainModel(){}
	public DockerComputeDomainModel(long id){
		super(id);
	}
	public DockerComputeDomainModel(String id){
		super(id);
	}
	public String getApiEndpoint() {
		return apiEndpoint;
	}
	public void setApiEndpoint(String apiEndpoint) {
		this.apiEndpoint = apiEndpoint;
	}
	public List<VolumeContainerModel> getVolumes() {
		return volumes;
	}
	public void setVolumes(List<VolumeContainerModel> volumes) {
		this.volumes = volumes;
	}
	public List<DockerImageModel> getImages() {
		return images;
	}
	public void setImages(List<DockerImageModel> images) {
		this.images = images;
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
	public List<RootVolumeOnComputeDomainModel> getRootVolumes() {
		return rootVolumes;
	}
	public void setRootVolumes(List<RootVolumeOnComputeDomainModel> rootVolumes) {
		this.rootVolumes = rootVolumes;
	}
	
}
