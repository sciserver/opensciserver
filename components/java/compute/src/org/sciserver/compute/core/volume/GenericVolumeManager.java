/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.compute.core.volume;

import io.kubernetes.client.openapi.models.V1Volume;
import io.kubernetes.client.openapi.models.V1VolumeMount;
import java.util.List;
import org.sciserver.compute.core.registry.GenericVolume;


public abstract class GenericVolumeManager {

    private GenericVolume volume;

    public GenericVolumeManager(GenericVolume volume) {
        this.volume = volume;
    }

    public GenericVolume getVolume() {
        return volume;
    }

    public abstract List<V1Volume> getK8sVolumes(String name) throws Exception;

    public abstract List<V1VolumeMount> getK8sVolumeMounts(String name) throws Exception;
}
