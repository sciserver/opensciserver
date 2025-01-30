/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.compute.core.registry;

import com.fasterxml.jackson.databind.JsonNode;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.sql.SQLException;
import java.util.Date;
import org.sciserver.compute.core.container.ExecutableManager;


public class ExecutableContainer extends Container {
    private String name;
    private String description;
    private Date createdAt;
    private Date accessedAt;
    private long slotId;
    private long executableImageId;
    private Long daskClusterId = null;
    private ExecutableManager executableManager;
    private String json;

    public ExecutableContainer(Registry registry) {
        super(registry);
    }

    @Override
    public Registry getRegistry() {
        return registry;
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

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getAccessedAt() {
        return accessedAt;
    }

    public void setAccessedAt(Date accessedAt) {
        this.accessedAt = accessedAt;
    }

    public long getSlotId() {
        return slotId;
    }

    public void setSlotId(long slotId) {
        this.slotId = slotId;
    }

    public long getExecutableImageId() {
        return executableImageId;
    }

    public void setExecutableImageId(long executableImageId) {
        this.executableImageId = executableImageId;
    }

    public Slot getSlot() throws Exception {
        return registry.getSlot(slotId);
    }

    @Override
    public Image getImage() throws Exception {
        return registry.getExecutableImage(executableImageId);
    }

    @Override
    public Node getNode() throws Exception {
        return getSlot().getNode();
    }

    @Override
    public void register() throws Exception {
        registry.registerExecutableContainer(this);
    }

    @Override
    public void unregister() throws SQLException {
        registry.unregisterExecutableContainer(this);
    }

    @Override
    public void update() throws SQLException {
        registry.updateExecutableContainer(this);
    }

    public ExecutableManager getExecutableManager() throws Exception {
        if (executableManager == null) {
            Image image = getImage();
            Class cl = Class.forName(image.getContainerManagerClass());
            Constructor constructor = cl.getDeclaredConstructor(new Class[] { Image.class });
            executableManager = (ExecutableManager) constructor.newInstance(image);
        }
        return executableManager;
    }

    public void start() throws Exception {
        getExecutableManager().startContainer(this);
    }

    public void stop() throws Exception {
        getExecutableManager().stopContainer(this);
    }

    public void delete() throws Exception {
        getExecutableManager().deleteContainer(this);
    }

    public void setProxy() throws Exception {
        getExecutableManager().setProxy(this);
    }

    public void deleteProxy() throws Exception {
        getExecutableManager().deleteProxy(this);
    }

    public void updatePublicVolumes(Iterable<PublicVolume> volumes) throws SQLException {
        registry.updatePublicVolumes(volumes, this);
    }

    public void updateUserVolumes(Iterable<VolumeContainer> volumes) throws SQLException {
        registry.updateUserVolumes(volumes, this);
    }

    public Iterable<PublicVolume> getPublicVolumes() throws Exception {
        return registry.getAttachedPublicVolumes(this);
    }

    public Iterable<VolumeContainer> getUserVolumes() throws Exception {
        return registry.getAttachedUserVolumes(this);
    }

    public boolean isRunning() throws Exception {
        JsonNode node = this.getInfo();
        return node.get("State").get("Running").asBoolean();
    }

    public URL getProxyUrl() throws Exception {
        return getExecutableManager().getProxyUrl(this);
    }

    public DaskCluster getDaskCluster() throws Exception {
        if (this.daskClusterId == null) {
            return null;
        }
        return registry.getDaskCluster(this.daskClusterId);
    }

    public Long getDaskClusterId() {
        return daskClusterId;
    }

    public void setDaskClusterId(Long daskClusterId) {
        this.daskClusterId = daskClusterId;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String value) {
        json = value;
    }
}
