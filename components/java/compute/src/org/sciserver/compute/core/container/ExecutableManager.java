/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.compute.core.container;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URL;
import org.sciserver.authentication.client.AuthenticatedUser;
import org.sciserver.compute.core.registry.Container;
import org.sciserver.compute.core.registry.ExecutableContainer;
import org.sciserver.compute.core.registry.VolumeImage;
import org.sciserver.racm.jobm.model.VolumeContainerModel;


public interface ExecutableManager {
    ExecutableContainer createContainer(String name, String description, AuthenticatedUser user,
        Iterable<VolumeContainerModel> publicVolumes, Iterable<VolumeImage> userVolumeImages) throws Exception;

    ExecutableContainer createContainer(String name, String description, AuthenticatedUser user,
        Iterable<VolumeContainerModel> publicVolumes, Iterable<VolumeImage> userVolumeImages, String[] commands)
        throws Exception;

    ExecutableContainer createContainer(String name, String description, AuthenticatedUser user,
        Iterable<VolumeContainerModel> publicVolumes, Iterable<VolumeImage> userVolumeImages,
        String[] commands, boolean isJob) throws Exception;

    void deleteContainer(ExecutableContainer container) throws Exception;

    void injectToken(ExecutableContainer container, String token) throws Exception;

    void startContainer(ExecutableContainer container) throws Exception;

    void stopContainer(ExecutableContainer container) throws Exception;

    void setProxy(ExecutableContainer container) throws Exception;

    void deleteProxy(ExecutableContainer container) throws Exception;

    JsonNode getInfo(Container container) throws Exception;

    String getStdOut(Container container) throws Exception;

    String getStdErr(Container container) throws Exception;

    URL getProxyUrl(ExecutableContainer container) throws Exception;
}
