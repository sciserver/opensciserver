/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.compute.model.admin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicVolumeInfo {
    private Long id;

    @JsonProperty("domain_id")
    private long domainId;

    private String name;

    private String description;

    @JsonProperty("docker_ref")
    private String dockerRef;

    @JsonProperty("selected_by_default")
    private boolean selectedByDefault;

    public boolean isSelectedByDefault() {
        return selectedByDefault;
    }

    public void setSelectedByDefault(boolean selectedByDefault) {
        this.selectedByDefault = selectedByDefault;
    }

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
