/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0. 
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.compute.core.registry;

import org.sciserver.racm.jobm.model.ComputeDomainUserVolumeModel;
import org.sciserver.racm.jobm.model.UserDockerComputeDomainModel;


public class RACMVolumeImage extends VolumeImage {

    private boolean writable;

    public static RACMVolumeImage fromDomain(UserDockerComputeDomainModel domain, long id, Registry registry) {
        return fromModel(domain.getUserVolumes().stream().filter(v -> v.getId() == id).findAny().get(), domain.getId(),
                registry);
    }

    public static RACMVolumeImage fromModel(ComputeDomainUserVolumeModel model, long domainId, Registry registry) {
        RACMVolumeImage image = new RACMVolumeImage(registry);
        image.setName(model.getName());
        image.setId(model.getId());
        image.setContainerManagerClass("org.sciserver.compute.core.container.RACMVolumeManager");
        image.setDockerRef("idies");
        image.setLocalPathTemplate(model.getFullPath());
        image.setContainerPath(
                "/home/idies/workspace/" + model.getRootVolumeName() + "/" + model.getOwner() + "/" + model.getName());
        image.setDescription(model.getDescription());
        image.setDomainId(domainId);
        image.setWritable(model.getAllowedActions().contains("write"));
        return image;
    }

    public RACMVolumeImage(Registry registry) {
        super(registry);
    }

    public boolean isWritable() {
        return writable;
    }

    public void setWritable(boolean readOnly) {
        this.writable = readOnly;
    }

}
