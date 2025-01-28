/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0. 
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/
package org.sciserver.compute.core.registry;

public class PublicVolume extends RegistryObject {

	private String name;

	private String description;

	private long domainId;

	private String dockerRef;

	private boolean writable = false;
	
	private boolean selectedByDefault = false;
	
	public PublicVolume(Registry registry) {
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

	public long getDomainId() {
		return domainId;
	}

	public void setDomainId(long domainId) {
		this.domainId = domainId;
	}

	public String getDockerRef() {
		return dockerRef;
	}

	public void setDockerRef(String dockerRef) {
		this.dockerRef = dockerRef;
	}

	public Domain getDomain() throws Exception {
		return registry.getDomain(domainId);
	}
	
	public boolean isWritable() {
		return writable;
	}

	public void setWritable(boolean writeable) {
		this.writable = writeable;
	}

	public boolean isSelectedByDefault() {
		return selectedByDefault;
	}

	public void setSelectedByDefault(boolean selectedByDefault) {
		this.selectedByDefault = selectedByDefault;
	}
}
