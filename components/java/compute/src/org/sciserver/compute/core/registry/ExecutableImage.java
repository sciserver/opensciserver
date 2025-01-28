/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0. 
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/
package org.sciserver.compute.core.registry;

import org.sciserver.compute.core.container.ExecutableManager;

public class ExecutableImage extends Image {

	public ExecutableImage(Registry registry) {
		super(registry);
	}

	public ExecutableManager createContainerManager() throws Exception {
		return (ExecutableManager) registry.createContainerManager(this);
	}
}
