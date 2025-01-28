package org.sciserver.compute.core.registry;

import java.lang.reflect.Constructor;

import org.sciserver.compute.core.volume.GenericVolumeManager;

public class GenericVolume extends RegistryObject {
	private String name;
	private String description;
	private String source;
	private String mountPath;
	private String volumeManagerClass;
	private long domainId;
	private boolean writable = false;
	
	public GenericVolume(Registry registry) {
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
	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
	}
	public String getMountPath() {
		return mountPath;
	}
	public void setMountPath(String mountPath) {
		this.mountPath = mountPath;
	}
	public long getDomainId() {
		return domainId;
	}
	public void setDomainId(long domainId) {
		this.domainId = domainId;
	}
	public boolean isWritable() {
		return writable;
	}
	public void setWritable(boolean writable) {
		this.writable = writable;
	}
	public String getVolumeManagerClass() {
		return volumeManagerClass;
	}
	public void setVolumeManagerClass(String volumeManagerClass) {
		this.volumeManagerClass = volumeManagerClass;
	}
	
	public GenericVolumeManager createVolumeManager() throws Exception {
		Class cl = Class.forName(getVolumeManagerClass());
		Constructor constructor = cl.getDeclaredConstructor(new Class[] { GenericVolume.class });
		return (GenericVolumeManager) constructor.newInstance(this);
	}
}
