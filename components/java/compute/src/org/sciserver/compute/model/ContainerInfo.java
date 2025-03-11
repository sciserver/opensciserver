/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.compute.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Date;
import org.sciserver.compute.Utilities;
import org.sciserver.compute.core.registry.ExecutableContainer;


public class ContainerInfo {
    private long id;
    private String name;
    private String nodeName;
    private String status;
    private String imageName;
    private Date createdAt;
    private Date accessedAt;
    private String description;
    private String domainName;
    private long domainId;
    private String externalRef;
    private String dockerRef;
    private String attachedVolumes;
    private long maxSecs;
    private JsonNode json;

    public static ContainerInfo fromContainer(ExecutableContainer container) throws Exception {
        ContainerInfo info = new ContainerInfo();
        info.setId(container.getId());
        info.setDescription(container.getDescription());
        info.setStatus(container.getStatus());
        info.setName(container.getName());
        info.setCreatedAt(container.getCreatedAt());
        info.setAccessedAt(container.getAccessedAt());
        info.setDockerRef(container.getDockerRef());
        info.setExternalRef(container.getExternalRef());
        info.setImageName(container.getImage().getName());
        info.setNodeName(container.getNode().getName());
        info.setDomainName(container.getNode().getDomain().getName());
        info.setDomainId(container.getNode().getDomain().getId());
        info.setMaxSecs(container.getNode().getDomain().getMaxSessionSecs());
        info.setJson(container.getJson());
        return info;
    }

    public String getDisplayName() {
        return Utilities.getDisplayName(name);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public long getDomainId() {
        return domainId;
    }

    public void setDomainId(long domainId) {
        this.domainId = domainId;
    }

    public String getExternalRef() {
        return externalRef;
    }

    public void setExternalRef(String externalRef) {
        this.externalRef = externalRef;
    }

    public String getDockerRef() {
        return dockerRef;
    }

    public void setDockerRef(String dockerRef) {
        this.dockerRef = dockerRef;
    }

    public String getAttachedVolumes() {
        return attachedVolumes;
    }

    public void setAttachedVolumes(String attachedVolumes) {
        this.attachedVolumes = attachedVolumes;
    }

    public long getMaxSecs() {
        return maxSecs;
    }

    public void setMaxSecs(long maxSecs) {
        this.maxSecs = maxSecs;
    }

    public JsonNode getJson() {
        return json;
    }

    public void setJson(String json) {
        try {
            this.json = (new ObjectMapper()).readTree(json);
        } catch (Exception e) {
            // To support back-compat for containers that don't have the json
        }
    }
}
