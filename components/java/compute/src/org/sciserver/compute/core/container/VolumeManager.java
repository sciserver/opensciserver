/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0. 
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/
package org.sciserver.compute.core.container;

import org.sciserver.compute.core.registry.Node;
import org.sciserver.compute.core.registry.VolumeContainer;
import org.sciserver.compute.core.registry.VolumeImage;

public interface VolumeManager {

	VolumeContainer createContainer(String userId, Node node, VolumeImage image) throws Exception;

	void deleteContainer(VolumeContainer container) throws Exception;

}
