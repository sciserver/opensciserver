/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.compute.core.container;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.sciserver.compute.AppConfig;
import org.sciserver.compute.core.registry.Image;
import org.sciserver.compute.core.registry.Registry;
import org.springframework.util.StringUtils;


public abstract class ContainerManager {
    private Image image;

    public ContainerManager(Image image) {
        this.image = image;
    }

    public Image getImage() {
        return image;
    }

    public Registry getRegistry() {
        return image.getRegistry();
    }

    /**
     * Adds configuration volume (if enabled) to "volumesFrom" array in Docker API.
     */
    protected void addConfigVolume(ArrayNode volumesFrom) {
        String configVolume = AppConfig.getInstance().getAppSettings().getConfigVolume();
        if (!StringUtils.isEmpty(configVolume)) {
            volumesFrom.add(configVolume + ":ro");
        }
    }
}
