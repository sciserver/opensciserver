/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0. 
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.compute.core.container;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import org.sciserver.authentication.client.AuthenticatedUser;
import org.sciserver.compute.AppConfig;
import org.sciserver.compute.core.client.docker.DockerClient;
import org.sciserver.compute.core.registry.ContainerStatus;
import org.sciserver.compute.core.registry.Domain;
import org.sciserver.compute.core.registry.ExecutableContainer;
import org.sciserver.compute.core.registry.Image;
import org.sciserver.compute.core.registry.Node;
import org.sciserver.compute.core.registry.PublicVolume;
import org.sciserver.compute.core.registry.RACMVolumeImage;
import org.sciserver.compute.core.registry.Registry;
import org.sciserver.compute.core.registry.RegistryLock;
import org.sciserver.compute.core.registry.VolumeImage;
import org.sciserver.racm.jobm.model.VolumeContainerModel;


public class NvidiaExecutableManager2 extends DockerBaseExecutableManager implements ExecutableManager {

    public NvidiaExecutableManager2(Image image) {
        super(image);
    }

    @Override
    public ExecutableContainer createContainer(String name, String description, AuthenticatedUser user,
            Iterable<VolumeContainerModel> publicVolumes, Iterable<VolumeImage> userVolumeImages) throws Exception {

        return createContainer(name, description, user, publicVolumes, userVolumeImages,
                new String[] { "/opt/startup.sh" }, false);
    }

    @Override
    public ExecutableContainer createContainer(String name, String description, AuthenticatedUser user,
            Iterable<VolumeContainerModel> publicVolumes, Iterable<VolumeImage> userVolumeImages, String[] commands)
            throws Exception {

        return createContainer(name, description, user, publicVolumes, userVolumeImages, commands, false);
    }

    @Override
    public ExecutableContainer createContainer(String name, String description, AuthenticatedUser user,
            Iterable<VolumeContainerModel> publicVolumes, Iterable<VolumeImage> userVolumeImages, String[] commands,
            boolean isJob) throws Exception {

        Registry registry = getRegistry();
        try (RegistryLock lock = new RegistryLock(registry)) {
            ExecutableContainer container = new ExecutableContainer(registry);
            container.setName(name);
            container.setDescription(description);
            container.setExecutableImageId(getImage().getId());
            container.setUserId(user.getUserId());
            registry.registerExecutableContainer(container, getImage().getDomain());

            Node node = container.getSlot().getNode();
            DockerClient dockerClient = node.createDockerClient();

            List<PublicVolume> registryPubVolumes = new ArrayList<PublicVolume>();
            for (VolumeContainerModel vcm : publicVolumes) {
                PublicVolume pv = registry.getPublicVolume(Long.parseLong(vcm.getPublisherDID()));
                pv.setWritable(vcm.isWritable());
                registryPubVolumes.add(pv);
            }

            JsonNode containerJson = getContainerJson(getImage(), container, user, node, commands, registryPubVolumes,
                    userVolumeImages, isJob);

            container.setDockerRef(dockerClient.createContainer(containerJson));
            container.setStatus(ContainerStatus.CREATED);
            container.update();
            container.start();
            container.setProxy();

            return container;
        }
    }

    @Override
    public void deleteContainer(ExecutableContainer container) throws Exception {
        Node node = container.getNode();
        String ref = container.getDockerRef();

        if (ref != null && !ref.isEmpty()) {
            DockerClient dockerClient = node.createDockerClient();
            dockerClient.deleteContainer(container.getDockerRef());
        }

        try {
            container.deleteProxy();
        } catch (Exception e) {
            // Ignore all errors
        }

        container.unregister();
    }

    @Override
    public void injectToken(ExecutableContainer container, String token) throws Exception {
        DockerClient docker = container.getNode().createDockerClient();

        JsonNodeFactory factory = JsonNodeFactory.instance;

        ObjectNode execJson = factory.objectNode().put("AttachStdin", false).put("AttachStdout", false)
                .put("AttachStderr", false).put("Tty", false);

        execJson.putArray("Cmd").add("bash").add("-c").add("echo '" + token + "' > /home/idies/keystone.token");

        ObjectNode startJson = factory.objectNode().put("Detach", false).put("Tty", false);

        docker.exec(container.getDockerRef(), execJson, startJson);
    }

    private JsonNode getContainerJson(Image image, ExecutableContainer container, AuthenticatedUser user, Node node,
            String[] commands, Iterable<PublicVolume> publicVolumes, Iterable<VolumeImage> userVolumeImages,
            boolean isJob) throws Exception {

        JsonNodeFactory factory = JsonNodeFactory.instance;

        ObjectNode result = factory.objectNode()
                .put("HostName", "")
                .put("Memory", 0)
                .put("MemorySwap", 0)
                .put("AttachStdin", false)
                .put("AttachStdout", true)
                .put("AttachStderr", true)
                .putNull("PortSpecs")
                .put("Privileged", false)
                .put("Tty", true)
                .put("OpenStdin", false)
                .put("StdinOnce", false)
                .putNull("Dns")
                .put("Image", image.getDockerRef())
                .put("WorkingDir", "")
                .put("User", "");

        ArrayNode env = result.putArray("Env");
        env.add(String.format("SCISERVER_USER_ID=%s", user.getUserId()));
        env.add(String.format("SCISERVER_USER_NAME=%s", user.getUserName()));

        result.putObject("Volumes");
        result.putArray("VolumesFrom");

        ArrayNode cmd = result.putArray("Cmd");
        for (int i = 0; i < commands.length; i++) {
            cmd.add(commands[i]);
        }
        cmd.add(node.getProxyBaseUrl().getPath() + container.getExternalRef());

        ObjectNode hostConfig = result.putObject("HostConfig");
        ObjectNode deviceRequests = hostConfig.putArray("DeviceRequests").addObject();

        deviceRequests.put("Driver", "").put("Count", -1).putNull("DeviceIDs").putArray("Capabilities").addArray()
                .add("gpu");

        deviceRequests.putObject("Options");

        Domain domain = container.getImage().getDomain();
        if (domain.getMaxMemory() != 0) {
            hostConfig.put("Memory", domain.getMaxMemory());
        }
        if (domain.getNanoCpus() != 0) {
            hostConfig.put("NanoCpus", domain.getNanoCpus());
        }

        ArrayNode volumesFrom = hostConfig.putArray("VolumesFrom");
        for (PublicVolume vol : publicVolumes) {
            volumesFrom.add(vol.getDockerRef() + (vol.isWritable() ? ":rw" : ":ro"));
        }
        addConfigVolume(volumesFrom);

        ArrayNode binds = hostConfig.putArray("Binds");
        for (VolumeImage img : userVolumeImages) {
            binds.add(String.format(img.getLocalPathTemplate(), user.getUserId()) + ":" + img.getContainerPath()
                    + (((RACMVolumeImage) img).isWritable() ? ":rw" : ":ro"));
        }

        hostConfig.putObject("PortBindings").putArray("8888/tcp").addObject()
                .put("HostPort", String.valueOf(container.getSlot().getPortNumber())).put("HostIp", "127.0.0.1");

        if ("zfs".equals(node.getStatus().at("/Driver").asText())) {
            hostConfig.putObject("StorageOpt").put("size",
                    AppConfig.getInstance().getAppSettings().getMaxContainerSize());
        }

        result.putObject("Labels")
                .put(ContainerLabel.USER_ID, user.getUserId())
                .put(ContainerLabel.DOMAIN_NAME, domain.getName())
                .put(ContainerLabel.DOMAIN_ID, String.valueOf(domain.getId()))
                .put(ContainerLabel.IMAGE_NAME, image.getName());

        return result;
    }
}
