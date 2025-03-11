/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.compute.model.admin;

import com.fasterxml.jackson.annotation.JsonInclude;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class K8sClusterInfo {
    private long id;
    private String name;
    private String description;
    private String apiUrl;
    private String apiToken;
    private String publicUrl;
    private long domainId;
    private boolean enabled;
    private String namespace;
    private float memOvercommit;
    private float cpuOvercommit;

    public void setId(long id) {
        this.id = id;
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

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public String getPublicUrl() {
        return publicUrl;
    }

    public void setPublicUrl(String publicUrl) {
        this.publicUrl = publicUrl;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public float getMemOvercommit() {
        return memOvercommit;
    }

    public void setMemOvercommit(float memOvercommit) {
        this.memOvercommit = memOvercommit;
    }

    public float getCpuOvercommit() {
        return cpuOvercommit;
    }

    public void setCpuOvercommit(float cpuOvercommit) {
        this.cpuOvercommit = cpuOvercommit;
    }

    public long getDomainId() {
        return domainId;
    }

    public void setDomainId(long domainId) {
        this.domainId = domainId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
