/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.compute.model.admin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImageInfo {

    private Long id;

    @JsonProperty("domain_id")
    private long domainId;

    private String name;

    private String description;

    @JsonProperty("docker_ref")
    private String dockerRef;

    @JsonProperty("container_manager_class")
    private String containerManagerClass;

    public long getDomainId() {
        return domainId;
    }

    public void setDomainId(long domainId) {
        this.domainId = domainId;
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

    public String getDockerRef() {
        return dockerRef;
    }

    public void setDockerRef(String dockerRef) {
        this.dockerRef = dockerRef;
    }

    public String getContainerManagerClass() {
        return containerManagerClass;
    }

    public void setContainerManagerClass(String containerManagerClass) {
        this.containerManagerClass = containerManagerClass;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
