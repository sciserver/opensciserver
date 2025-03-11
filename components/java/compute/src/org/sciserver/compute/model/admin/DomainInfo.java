/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.compute.model.admin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class DomainInfo {
    private Long id;
    private String name;
    private String description;
    @JsonProperty("max_memory")
    private long maxMemory;
    private long nanoCpus;
    private long shmBytes;
    private long maxSessionSecs;

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

    public long getMaxMemory() {
        return maxMemory;
    }

    public void setMaxMemory(long maxMemory) {
        this.maxMemory = maxMemory;
    }

    public long getNanoCpus() {
        return nanoCpus;
    }

    public void setNanoCpus(long nanoCpus) {
        this.nanoCpus = nanoCpus;
    }

    public long getShmBytes() {
        return shmBytes;
    }

    public void setShmBytes(long shmBytes) {
        this.shmBytes = shmBytes;
    }

    public long getMaxSessionSecs() {
        return maxSessionSecs;
    }

    public void setMaxSessionSecs(long maxSessionSecs) {
        this.maxSessionSecs = maxSessionSecs;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
