package org.sciserver.racm.jobm.model;

import java.util.ArrayList;
import java.util.List;

public class COMPMDockerJobModel extends COMPMJobModel {

	private String scriptURI;
	private String command;
	private String dockerComputeEndpoint;
	private String dockerComputeResourceContextUUID;
	private String[] fullDockerCommand;
	private String dockerImageName;
	private List<VolumeContainerModel> volumeContainers;
	public COMPMDockerJobModel(){
		this(null);
	}
	public COMPMDockerJobModel(Long id){
		super(id);
	}
	public String getCommand() {
		return command;
	}
	public void setCommand(String command) {
		this.command = command;
	}
	public List<VolumeContainerModel> getVolumeContainers() {
		return volumeContainers;
	}
	public void setVolumeContainers(List<VolumeContainerModel> volumeContainers) {
		if(volumeContainers != null)
			this.volumeContainers = volumeContainers;
		else
			this.volumeContainers = new ArrayList<>();
	}
	public String getDockerComputeEndpoint() {
		return dockerComputeEndpoint;
	}
	public void setDockerComputeEndpoint(String dockerComputeEndpoint) {
		this.dockerComputeEndpoint = dockerComputeEndpoint;
	}
	public String getDockerImageName() {
		return dockerImageName;
	}
	public void setDockerImageName(String dockerImageName) {
		this.dockerImageName = dockerImageName;
	}
	public String getDockerComputeResourceContextUUID() {
		return dockerComputeResourceContextUUID;
	}
	public void setDockerComputeResourceContextUUID(String dockerComputeResourceContextUUID) {
		this.dockerComputeResourceContextUUID = dockerComputeResourceContextUUID;
	}
	public String getScriptURI() {
		return scriptURI;
	}
	public void setScriptURI(String scriptURI) {
		this.scriptURI = scriptURI;
	}
	public String[] getFullDockerCommand() {
		return fullDockerCommand;
	}
	public void setFullDockerCommand(String[] fullDockerCommand) {
		this.fullDockerCommand = fullDockerCommand;
	}
}
