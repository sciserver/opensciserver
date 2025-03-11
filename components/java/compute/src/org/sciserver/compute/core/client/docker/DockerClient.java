/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0. 
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.compute.core.client.docker;

import com.fasterxml.jackson.databind.JsonNode;


public interface DockerClient {
    String createContainer(JsonNode containerDef) throws Exception;

    void deleteContainer(String containerId) throws Exception;

    JsonNode getContainerInfo(String containerId) throws Exception;

    String getContainerLogs(String containerId, int logSource) throws Exception;

    void startContainer(String containerId) throws Exception;

    void stopContainer(String containerId) throws Exception;

    void exec(String containerId, JsonNode execJson, JsonNode startJson) throws Exception;

    JsonNode listImages() throws Exception;

    JsonNode getInfo(int timeout) throws Exception;
}
