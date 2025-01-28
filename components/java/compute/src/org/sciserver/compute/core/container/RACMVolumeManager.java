/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/
package org.sciserver.compute.core.container;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.sciserver.compute.core.registry.Image;
import org.sciserver.compute.core.registry.Node;
import org.sciserver.compute.core.registry.RACMVolumeContainer;
import org.sciserver.compute.core.registry.VolumeContainer;
import org.sciserver.compute.core.registry.VolumeImage;

public class RACMVolumeManager extends ContainerManager implements VolumeManager {

    public RACMVolumeManager(Image image) {
        super(image);
    }

    @Override
    public VolumeContainer createContainer(String userId, Node node, VolumeImage image) throws Exception {
        /*
        RACMVolumeContainer container = new RACMVolumeContainer(getRegistry());
        container.setNodeId(node.getId());
        container.setVolumeImageId(image.getId());
        container.setUserId(userId);
        container.register();

        JsonNode containerJson = getContainerJson((RACMVolumeImage)image, userId);

        DockerClient dockerClient = node.createDockerClient();
        container.setDockerRef(dockerClient.createContainer(containerJson));
        container.setStatus(ContainerStatus.CREATED);
        container.update();

        return container;
        */
        throw new UnsupportedOperationException("Method is not supported");
    }

    @Override
    public void deleteContainer(VolumeContainer container) throws Exception {
        /*
        DockerClient dockerClient = container.getNode().createDockerClient();
        dockerClient.deleteContainer(container.getDockerRef());
        container.unregister();
        */
        throw new UnsupportedOperationException("Method is not supported");
    }

    /*
    private JsonNode getContainerJson(RACMVolumeImage image, String userId) {
        JsonNodeFactory factory = JsonNodeFactory.instance;

        ObjectNode result = factory.objectNode().put("HostName", "").put("User", "").put("Memory", 0)
                .put("MemorySwap", 0).put("AttachStdin", false).put("AttachStdout", true).put("AttachStderr", true)
                .putNull("PortSpecs").put("Privileged", false).put("Tty", false).put("OpenStdin", false)
                .put("StdinOnce", false).putNull("Env").putNull("Dns").put("Image", image.getDockerRef())
                .put("WorkingDir", "");

        result.putObject("Volumes").putObject(image.getContainerPath());

        result.putArray("VolumesFrom");
        result.putArray("Cmd").add("/bin/true");

        ObjectNode hostConfig = result.putObject("HostConfig");
        hostConfig.putArray("Binds")
                .add(String.format(image.getLocalPathTemplate(), userId) + ":" + image.getContainerPath() + ":rw");
        hostConfig.putObject("PortBindings");

        return result;
    }
    */
}
