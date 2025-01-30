/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0. 
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.compute.core.registry;


public class Image extends RegistryObject {

    private String name;

    private String description;

    private String dockerRef;

    private long domainId;

    private String containerManagerClass;

    public Image(Registry registry) {
        super(registry);
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

    public long getDomainId() {
        return domainId;
    }

    public void setDomainId(long domainId) {
        this.domainId = domainId;
    }

    public String getContainerManagerClass() {
        return containerManagerClass;
    }

    public void setContainerManagerClass(String containerManagerClass) {
        this.containerManagerClass = containerManagerClass;
    }

    public Domain getDomain() throws Exception {
        return registry.getDomain(domainId);
    }
}
