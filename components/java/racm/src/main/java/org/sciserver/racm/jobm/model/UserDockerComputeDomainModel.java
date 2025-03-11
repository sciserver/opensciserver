package org.sciserver.racm.jobm.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sciserver.racm.utils.model.RACMBaseModel;

public class UserDockerComputeDomainModel extends RACMBaseModel{
	private String apiEndpoint;
	private String name;
	private String description;
	private List<VolumeContainerModel> volumes;
	private List<DockerImageModel> images;
	private List<ComputeDomainUserVolumeModel> userVolumes; // these must be included when returning visible components for the user
	public UserDockerComputeDomainModel(){}
	public UserDockerComputeDomainModel(long id){
		super(id);
	}
	public UserDockerComputeDomainModel(String id){
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
	public List<ComputeDomainUserVolumeModel> getUserVolumes() {
		return userVolumes;
	}
	public void setUserVolumes(List<ComputeDomainUserVolumeModel> userVolumes) {
		this.userVolumes = userVolumes;
	}
	public void addUserVolumes(Collection<ComputeDomainUserVolumeModel> uv){
		if(this.userVolumes == null){
			this.userVolumes = new ArrayList<>();
		}
		this.userVolumes.addAll(uv);
	}
	public void addUserVolume(ComputeDomainUserVolumeModel uv){
		if(this.userVolumes == null){
			this.userVolumes = new ArrayList<>();
		}
		this.userVolumes.add(uv);
	}
}
