/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.compute.model.admin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenericVolumeInfo {
    private Long id;

    @JsonProperty("domain_id")
    private long domainId;

    private String name;

    private String description;

    @JsonProperty("volume_manager_class")
    private String volumeManagerClass;

    @JsonProperty("source")
    private String[] source;

    @JsonProperty("mount_path")
    private String[] mountPath;

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

    public String[] getSource() {
        return source;
    }

    public void setSource(String[] source) {
        this.source = source;
    }

    public String[] getMountPath() {
        return mountPath;
    }

    public void setMountPath(String[] mountPath) {
        this.mountPath = mountPath;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVolumeManagerClass() {
        return volumeManagerClass;
    }

    public void setVolumeManagerClass(String volumeManagerClass) {
        this.volumeManagerClass = volumeManagerClass;
    }

}
