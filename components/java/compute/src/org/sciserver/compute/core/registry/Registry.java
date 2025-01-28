/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/
package org.sciserver.compute.core.registry;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.sciserver.compute.core.client.docker.DockerClient;
import org.sciserver.compute.core.client.httpproxy.HttpProxyClient;
import org.sciserver.compute.model.admin.DomainInfo;
import org.sciserver.compute.model.admin.ImageInfo;
import org.sciserver.compute.model.admin.NodeInfo;
import org.sciserver.compute.model.admin.PublicVolumeInfo;
import org.sciserver.compute.model.admin.SlotsInfo;
import org.sciserver.compute.model.admin.K8sClusterInfo;

public interface Registry {

    void migrateToLatestChanges(String changelogPath) throws Exception;

    DockerClient createDockerClient(URL apiURL, String certFile, String keyFile) throws IOException;

    HttpProxyClient createHttpProxyClient(URL apiURL, String certFile, String keyFile) throws IOException;

    String getCertificateRoot();

    Domain getDomain(long domainId) throws Exception;

    Object createContainerManager(Image image) throws Exception;

    Node getNode(long nodeId) throws Exception;

    Slot getSlot(long slotId) throws Exception;

    ExecutableContainer getContainer(long containerId) throws Exception;

    ExecutableImage getExecutableImage(long imageId) throws Exception;

    PublicVolume getPublicVolume(long volumeId) throws Exception;

    K8sCluster getK8sCluster(long id) throws Exception;

    void startContainer(ExecutableContainer executableContainer) throws Exception;

    void stopContainer(ExecutableContainer executableContainer) throws Exception;

    void setProxy(ExecutableContainer executableContainer) throws Exception;

    void deleteProxy(ExecutableContainer executableContainer) throws Exception;

    void registerExecutableContainer(ExecutableContainer executableContainer) throws Exception;

    void unregisterExecutableContainer(ExecutableContainer executableContainer) throws SQLException;

    void updateExecutableContainer(ExecutableContainer executableContainer) throws SQLException;

    void setExecutableContainerJson(Long containerId, String json) throws Exception;

    Image getVolumeImage(long volumeImageId) throws Exception;

    void registerVolumeContainer(VolumeContainer volumeContainer) throws Exception;

    void unregisterVolumeContainer(VolumeContainer volumeContainer) throws SQLException;

    void updateVolumeContainer(VolumeContainer volumeContainer) throws SQLException;

    void updatePublicVolumes(Iterable<PublicVolume> volumes, ExecutableContainer executableContainer)
            throws SQLException;

    void updateUserVolumes(Iterable<VolumeContainer> volumes, ExecutableContainer executableContainer)
            throws SQLException;

    void registerExecutableContainer(ExecutableContainer executableContainer, Domain domain) throws Exception;

    Iterable<VolumeContainer> getUserVolumes(String userId, Node node) throws Exception;

    String getSettings(String key) throws Exception;

    Iterable<ExecutableContainer> getContainers(String userId) throws Exception;

    Iterable<ExecutableContainer> getInactiveContainers(Date inactiveSince, boolean unknown) throws Exception;

    Iterable<ExecutableContainer> getExpiredContainers() throws Exception;

    Iterable<Domain> getDomains() throws Exception;

    Iterable<Node> getNodes(Domain domain) throws Exception;

    Iterable<ExecutableImage> getExecutableImages(Domain domain) throws Exception;

    Iterable<VolumeImage> getVolumeImages(Domain domain) throws Exception;

    Iterable<PublicVolume> getPublicVolumes(Domain domain) throws Exception;

    Iterable<VolumeContainer> getAttachedUserVolumes(ExecutableContainer container) throws Exception;

    Iterable<PublicVolume> getAttachedPublicVolumes(ExecutableContainer container) throws Exception;

    Iterable<K8sCluster> getK8sClusters(Domain domain) throws Exception;

    Iterable<DaskCluster> getDaskClusters(String userId) throws Exception;

    ExecutableImage getExecutableImage(Domain domain, String imageName) throws Exception;

    long getUsedSlots(Node node) throws Exception;

    long getTotalSlots(Node node) throws Exception;

    Iterable<RACMVolumeContainer> getRACMUserVolumes(String userId, Node node) throws Exception;

    void registerRACMVolumeContainer(VolumeContainer volumeContainer) throws Exception;

    void unregisterRACMVolumeContainer(VolumeContainer volumeContainer) throws SQLException;

    void updateRACMVolumeContainer(VolumeContainer volumeContainer) throws SQLException;

    void acquireLock() throws Exception;

    void releaseLock() throws Exception;

    Iterable<ExecutableContainer> getContainers() throws Exception;

    void updateAccessTime(ExecutableContainer container) throws Exception;

    /* Admin */

    long adminCreateDomain(DomainInfo domainInfo) throws Exception;

    long adminCreateNode(NodeInfo nodeInfo) throws Exception;

    long adminCreateImage(ImageInfo imageInfo) throws Exception;

    void adminCreateSlots(SlotsInfo slotsInfo) throws Exception;

    long adminCreatePublicVolume(PublicVolumeInfo publicVolumeInfo) throws Exception;

    long adminCreateK8sCluster(K8sClusterInfo k8sCluster) throws Exception;


    List<NodeInfo> adminGetNodes() throws Exception;

    List<SlotsInfo> adminGetSlots() throws Exception;

    List<ImageInfo> adminGetImages() throws Exception;

    List<PublicVolumeInfo> adminGetPublicVolumes() throws Exception;

    List<DomainInfo> adminGetDomains() throws Exception;

    List<K8sClusterInfo> adminGetK8sCluster() throws Exception;

    boolean isK8sDomain(Domain domain) throws Exception;

    DaskCluster getDaskCluster(long id) throws Exception;

    void updateDaskCluster(DaskCluster daskCluster) throws SQLException;

    void registerDaskCluster(DaskCluster daskCluster) throws Exception;

    DaskCluster getDaskCluster(String externalRef) throws Exception;

    GenericVolume getGenericVolume(long id) throws Exception;

    void linkDaskCluster(ExecutableContainer var1, DaskCluster var2) throws Exception;
}
