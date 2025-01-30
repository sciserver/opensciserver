/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0. 
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.compute.core.registry;

import java.sql.SQLException;


public class VolumeContainer extends Container {
    private long nodeId;

    private long volumeImageId;

    public VolumeContainer(Registry registry) {
        super(registry);
    }

    public long getNodeId() {
        return nodeId;
    }

    public void setNodeId(long nodeId) {
        this.nodeId = nodeId;
    }

    public long getVolumeImageId() {
        return volumeImageId;
    }

    public void setVolumeImageId(long volumeImageId) {
        this.volumeImageId = volumeImageId;
    }

    @Override
    public Image getImage() throws Exception {
        return registry.getVolumeImage(volumeImageId);
    }

    @Override
    public Node getNode() throws Exception {
        return registry.getNode(nodeId);
    }

    @Override
    public void register() throws Exception {
        registry.registerVolumeContainer(this);
    }

    @Override
    public void unregister() throws SQLException {
        registry.unregisterVolumeContainer(this);
    }

    @Override
    public void update() throws SQLException {
        registry.updateVolumeContainer(this);
    }
}
