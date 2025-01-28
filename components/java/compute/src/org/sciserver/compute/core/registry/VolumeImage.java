/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0. 
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/
package org.sciserver.compute.core.registry;

import org.sciserver.compute.core.container.VolumeManager;

public class VolumeImage extends Image {

	private String localPathTemplate;

	private String containerPath;

	public VolumeImage(Registry registry) {
		super(registry);
	}

	public String getLocalPathTemplate() {
		return localPathTemplate;
	}

	public void setLocalPathTemplate(String localPathTemplate) {
		this.localPathTemplate = localPathTemplate;
	}

	public String getContainerPath() {
		return containerPath;
	}

	public void setContainerPath(String containerPath) {
		this.containerPath = containerPath;
	}

	public VolumeManager createContainerManager() throws Exception {
		return (VolumeManager) registry.createContainerManager(this);
	}
}
