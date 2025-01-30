/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.compute.core.registry;


public class Domain extends RegistryObject {
    private String name;
    private String description;
    private long maxMemory;
    private long nanoCpus;
    private long shmBytes;
    private long maxSessionSecs;
    private String type;

    public Domain(Registry registry) {
        super(registry);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
