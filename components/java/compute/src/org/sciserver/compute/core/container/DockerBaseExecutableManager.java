/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/
package org.sciserver.compute.core.container;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.sciserver.compute.core.client.docker.DockerClient;
import org.sciserver.compute.core.client.docker.LogSource;
import org.sciserver.compute.core.client.httpproxy.HttpProxyClient;
import org.sciserver.compute.core.client.httpproxy.HttpProxyClientImpl;
import org.sciserver.compute.core.registry.Container;
import org.sciserver.compute.core.registry.ExecutableContainer;
import org.sciserver.compute.core.registry.Image;
import org.sciserver.compute.core.registry.Node;

import com.fasterxml.jackson.databind.JsonNode;

public abstract class DockerBaseExecutableManager extends ContainerManager {

    public DockerBaseExecutableManager(Image image) {
        super(image);
    }

    public JsonNode getInfo(Container container) throws Exception {
        DockerClient dockerClient = container.getNode().createDockerClient();
        return dockerClient.getContainerInfo(container.getDockerRef());
    }

    public void startContainer(ExecutableContainer container) throws Exception {
        DockerClient dockerClient = container.getNode().createDockerClient();
        dockerClient.startContainer(container.getDockerRef());
    }

    public void stopContainer(ExecutableContainer container) throws Exception {
        DockerClient dockerClient = container.getNode().createDockerClient();
        dockerClient.stopContainer(container.getDockerRef());
    }

    public HttpProxyClient createHttpProxyClient(ExecutableContainer container, URL apiURL, String certFile, String keyFile) throws IOException {
        String certificateRoot = container.getRegistry().getCertificateRoot();
        return new HttpProxyClientImpl(apiURL, Files.readAllBytes(Paths.get(certificateRoot, certFile)),
                                       Files.readAllBytes(Paths.get(certificateRoot, keyFile)));
    }


    public void setProxy(ExecutableContainer container) throws Exception {
        Node node = container.getNode();
        HttpProxyClient proxy = createHttpProxyClient(container,
            node.getProxyApiUrl(), node.getProxyApiClientCert(), node.getProxyApiClientKey()
        );
        String externalPath = node.getProxyBaseUrl().getPath() + container.getExternalRef();
        proxy.setProxy(externalPath, "http://127.0.0.1:" + container.getSlot().getPortNumber(),
                       container.getUserId(), container.getId());
    }

    public void deleteProxy(ExecutableContainer container) throws Exception {
        Node node = container.getSlot().getNode();
        HttpProxyClient proxy = createHttpProxyClient(container,
            node.getProxyApiUrl(), node.getProxyApiClientCert(), node.getProxyApiClientKey()
        );
        String externalPath = node.getProxyBaseUrl().getPath() + container.getExternalRef();
        proxy.deleteProxy(externalPath);
    }

    public String getStdOut(Container container) throws Exception {
        DockerClient dockerClient = container.getNode().createDockerClient();
        return dockerClient.getContainerLogs(container.getDockerRef(), LogSource.STD_OUT);
    }

    public String getStdErr(Container container) throws Exception {
        DockerClient dockerClient = container.getNode().createDockerClient();
        return dockerClient.getContainerLogs(container.getDockerRef(), LogSource.STD_ERR);
    }

    public URL getProxyUrl(ExecutableContainer container) throws Exception {
        return container.getNode().getProxyBaseUrl();
    }


}
