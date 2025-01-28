/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/
package org.sciserver.compute.core.registry;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Semaphore;

import javax.sql.DataSource;

import org.sciserver.compute.core.client.docker.DockerClient;
import org.sciserver.compute.core.client.docker.DockerClientImpl;
import org.sciserver.compute.core.client.httpproxy.HttpProxyClient;
import org.sciserver.compute.core.client.httpproxy.HttpProxyClientImpl;
import org.sciserver.compute.model.admin.DomainInfo;
import org.sciserver.compute.model.admin.ImageInfo;
import org.sciserver.compute.model.admin.NodeInfo;
import org.sciserver.compute.model.admin.PublicVolumeInfo;
import org.sciserver.compute.model.admin.SlotsInfo;
import org.sciserver.compute.model.admin.K8sClusterInfo;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

public class RegistryImpl implements Registry {

    private DataSource dataSource;
    private String certificateRoot;
    private String settingsTable;
    private final Semaphore _createContainer = new Semaphore(1);

    public DataSource getDataSource() {
        return dataSource;
    }

    public String getCertificateRoot() {
        return certificateRoot;
    }

    public String getSettingsTable() {
        return settingsTable;
    }

    public RegistryImpl(DataSource dataSource, String certificateRoot, String settingsTable) {
        this.dataSource = dataSource;
        this.certificateRoot = certificateRoot;
        this.settingsTable = settingsTable;
    }

    @Override
    public void migrateToLatestChanges(String changelogPath) throws SQLException, LiquibaseException {
        try (Connection conn = dataSource.getConnection()) {
            Liquibase liquibase = new Liquibase(changelogPath,
                    new ClassLoaderResourceAccessor(),
                    new JdbcConnection(conn));
            liquibase.update((Contexts)null);
        }
    }

    @Override
    public Object createContainerManager(Image image) throws Exception {
        Class cl = Class.forName(image.getContainerManagerClass());
        Constructor constructor = cl.getDeclaredConstructor(new Class[] { Image.class });
        return constructor.newInstance(image);
    }

    @Override
    public DockerClient createDockerClient(URL apiURL, String certFile, String keyFile) throws IOException {
        return new DockerClientImpl(apiURL, Files.readAllBytes(Paths.get(certificateRoot, certFile)),
                Files.readAllBytes(Paths.get(certificateRoot, keyFile)));
    }

    @Override
    public HttpProxyClient createHttpProxyClient(URL apiURL, String certFile, String keyFile) throws IOException {
        return new HttpProxyClientImpl(apiURL, Files.readAllBytes(Paths.get(certificateRoot, certFile)),
                Files.readAllBytes(Paths.get(certificateRoot, keyFile)));
    }

    @Override
    public void deleteProxy(ExecutableContainer executableContainer) throws Exception {
        Node node = executableContainer.getSlot().getNode();
        HttpProxyClient proxy = createHttpProxyClient(node.getProxyApiUrl(), node.getProxyApiClientCert(),
                node.getProxyApiClientKey());

        String externalPath = node.getProxyBaseUrl().getPath() + executableContainer.getExternalRef();
        proxy.deleteProxy(externalPath);
    }

    @Override
    public ExecutableContainer getContainer(long executableContainerId) throws Exception {
        Connection conn = this.getConnection();
        PreparedStatement pstmt = conn.prepareStatement("SELECT a.*, c.`id` AS `cluster_id` FROM `executable_container` a LEFT OUTER JOIN `linked_dask_cluster` b ON a.`id` = b.`container_id` LEFT OUTER JOIN `dask_cluster` c ON b.`cluster_id` = c.`id` WHERE a.`id` = ? AND a.`status` <> ?");
        try {
            pstmt.setLong(1, executableContainerId);
            pstmt.setString(2, ContainerStatus.DELETED);
            Iterator<ExecutableContainer> i = this.getRegistryObjectsFromQuery(pstmt, ExecutableContainer.class).iterator();
            if (i.hasNext()) {
                ExecutableContainer executableContainer = i.next();
                return executableContainer;
            }
            throw new NotFoundException("Container not found");
        }
        finally {
            pstmt.close();
            conn.close();
        }
    }

    @Override
    public Domain getDomain(long domainId) throws Exception {
        Connection conn = getConnection();
        PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM `domain` WHERE `id` = ?");

        try {
            pstmt.setLong(1, domainId);
            Iterator<Domain> i = getRegistryObjectsFromQuery(pstmt, Domain.class).iterator();
            if (i.hasNext())
                return i.next();
            else
                throw new Exception("Domain not found");
        } finally {
            pstmt.close();
            conn.close();
        }
    }

    @Override
    public ExecutableImage getExecutableImage(long executableImageId) throws Exception {
        Connection conn = getConnection();
        PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM `executable_image` WHERE `id` = ?");

        try {
            pstmt.setLong(1, executableImageId);
            Iterator<ExecutableImage> i = getRegistryObjectsFromQuery(pstmt, ExecutableImage.class).iterator();
            if (i.hasNext())
                return i.next();
            else
                throw new Exception("ExecutableImage not found");
        } finally {
            pstmt.close();
            conn.close();
        }
    }

    @Override
    public Node getNode(long nodeId) throws Exception {
        Connection conn = getConnection();
        PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM `node` WHERE `id` = ?");

        try {
            pstmt.setLong(1, nodeId);
            Iterator<Node> i = getRegistryObjectsFromQuery(pstmt, Node.class).iterator();
            if (i.hasNext())
                return i.next();
            else
                throw new Exception("Node not found");
        } finally {
            pstmt.close();
            conn.close();
        }
    }

    @Override
    public DaskCluster getDaskCluster(long id) throws Exception {
        Connection conn = getConnection();
        PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM `dask_cluster` WHERE `id` = ?");

        try {
            pstmt.setLong(1, id);
            Iterator<DaskCluster> i = getRegistryObjectsFromQuery(pstmt, DaskCluster.class).iterator();
            if (i.hasNext())
                return i.next();
            else
                throw new Exception("DaskCluster not found");
        } finally {
            pstmt.close();
            conn.close();
        }
    }

    @Override
    public DaskCluster getDaskCluster(String externalRef) throws Exception {
        Connection conn = getConnection();
        PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM `dask_cluster` WHERE `external_ref` = ?");

        try {
            pstmt.setString(1, externalRef);
            Iterator<DaskCluster> i = getRegistryObjectsFromQuery(pstmt, DaskCluster.class).iterator();
            if (i.hasNext())
                return i.next();
            else
                throw new Exception("DaskCluster not found");
        } finally {
            pstmt.close();
            conn.close();
        }
    }

    @Override
    public PublicVolume getPublicVolume(long publicVolumeId) throws Exception {
        Connection conn = getConnection();
        PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM `public_volume` WHERE `id` = ?");

        try {
            pstmt.setLong(1, publicVolumeId);
            Iterator<PublicVolume> i = getRegistryObjectsFromQuery(pstmt, PublicVolume.class).iterator();
            if (i.hasNext())
                return i.next();
            else
                throw new Exception("Volume not found");
        } finally {
            pstmt.close();
            conn.close();
        }
    }

    @Override
    public Slot getSlot(long slotId) throws Exception {
        Connection conn = getConnection();
        PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM `slot` WHERE `id` = ?");

        try {
            pstmt.setLong(1, slotId);
            Iterator<Slot> i = getRegistryObjectsFromQuery(pstmt, Slot.class).iterator();
            if (i.hasNext())
                return i.next();
            else
                throw new Exception("Slot not found");
        } finally {
            pstmt.close();
            conn.close();
        }
    }

    @Override
    public Image getVolumeImage(long volumeImageId) throws Exception {
        Connection conn = getConnection();
        PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM `volume_image` WHERE `id` = ?");

        try {
            pstmt.setLong(1, volumeImageId);
            Iterator<VolumeImage> i = getRegistryObjectsFromQuery(pstmt, VolumeImage.class).iterator();
            if (i.hasNext())
                return i.next();
            else
                throw new Exception("VolumeImage not found");
        } finally {
            pstmt.close();
            conn.close();
        }
    }

    @Override
    public K8sCluster getK8sCluster(long id) throws Exception {
        Connection conn = getConnection();
        PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM `k8s_cluster` WHERE `id` = ?");

        try {
            pstmt.setLong(1, id);
            Iterator<K8sCluster> i = getRegistryObjectsFromQuery(pstmt, K8sCluster.class).iterator();
            if (i.hasNext())
                return i.next();
            else
                throw new Exception("K8s Cluster not found");
        } finally {
            pstmt.close();
            conn.close();
        }
    }

    @Override
    public void registerExecutableContainer(ExecutableContainer executableContainer) throws Exception {
        Connection conn = getConnection();
        try {
            PreparedStatement pstmt = conn.prepareStatement("INSERT INTO `executable_container` (" + "	`name`,"
                    + "	`description`," + "	`external_ref`," + "	`slot_id`," + "	`status`,"
                    + "	`executable_image_id`," + "	`user_id`," + "	`created_at`)" + "VALUES (" + "	?,"
                    + "	?," + "	UUID()," + "	?," + "	?," + "	?," + "	?," + "	NOW())");

            try {
                pstmt.setString(1, executableContainer.getName());
                pstmt.setString(2, executableContainer.getDescription());
                pstmt.setLong(3, executableContainer.getSlot().getId());
                pstmt.setString(4, ContainerStatus.REGISTERED);
                pstmt.setLong(5, executableContainer.getExecutableImageId());
                pstmt.setString(6, executableContainer.getUserId());
                pstmt.executeUpdate();
            } finally {
                pstmt.close();
            }

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT `id`,`external_ref`, `created_at`, `status` FROM `executable_container` WHERE `id` = LAST_INSERT_ID()");
            try {
                if (rs.next()) {
                    executableContainer.setId(rs.getLong("id"));
                    executableContainer.setExternalRef(rs.getString("external_ref"));
                    executableContainer.setCreatedAt(rs.getTimestamp("created_at"));
                    executableContainer.setStatus(rs.getString("status"));
                } else
                    throw new Exception("Unexpected error: result set is empty");
            } finally {
                rs.close();
                stmt.close();
            }
        } finally {
            conn.close();
        }
    }

    @Override
    public void registerDaskCluster(DaskCluster daskCluster) throws Exception {
        Connection conn = getConnection();
        try {
            PreparedStatement pstmt = conn.prepareStatement("INSERT INTO `dask_cluster` ("
                    + "`name`,"
                    + "`description`,"
                    + "`external_ref`,"
                    + "`k8s_cluster_id`,"
                    + "`status`,"
                    + "`image_id`,"
                    + "`user_id`,"
                    + "`created_at`)"
                    + "VALUES ("
                    + "?,"
                    + "?,"
                    + "CONCAT('dask-',UUID()),"
                    + "?,"
                    + "?,"
                    + "?,"
                    + "?,"
                    + "NOW())");

            try {
                pstmt.setString(1, daskCluster.getName());
                pstmt.setString(2, daskCluster.getDescription());
                pstmt.setLong(3, daskCluster.getK8sClusterId());
                pstmt.setString(4, DaskClusterStatus.REGISTERED);
                pstmt.setLong(5, daskCluster.getImageId());
                pstmt.setString(6, daskCluster.getUserId());
                pstmt.executeUpdate();
            } finally {
                pstmt.close();
            }

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT `id`, `external_ref`, `created_at`, `status` FROM `dask_cluster` WHERE `id` = LAST_INSERT_ID()");
            try {
                if (rs.next()) {
                    daskCluster.setId(rs.getLong("id"));
                    daskCluster.setExternalRef(rs.getString("external_ref"));
                    daskCluster.setCreatedAt(rs.getTimestamp("created_at"));
                    daskCluster.setStatus(rs.getString("status"));
                } else
                    throw new Exception("Unexpected error: result set is empty");
            } finally {
                rs.close();
                stmt.close();
            }
        } finally {
            conn.close();
        }
    }

    @Override
    public void registerExecutableContainer(ExecutableContainer executableContainer, Domain domain) throws Exception {
        Connection conn = getConnection();
        try {
            PreparedStatement pstmt = conn.prepareStatement("INSERT INTO `executable_container` (" + "	`Name`,"
                    + "	`description`," + "	`external_ref`," + "	`slot_id`," + "	`status`,"
                    + "	`executable_image_id`," + "	`user_id`," + "	`created_at`)" + "VALUES (" + "	?,"
                    + "	?," + "	UUID()," + "	next_available_slot(least_busy_node(?))," + "	?," + "	?,"
                    + "	?," + "	NOW())");

            try {
                pstmt.setString(1, executableContainer.getName());
                pstmt.setString(2, executableContainer.getDescription());
                pstmt.setLong(3, domain.getId());
                pstmt.setString(4, ContainerStatus.REGISTERED);
                pstmt.setLong(5, executableContainer.getExecutableImageId());
                pstmt.setString(6, executableContainer.getUserId());
                pstmt.executeUpdate();
            } finally {
                pstmt.close();
            }

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT `id`,`external_ref`, `created_at`, `status`, `slot_id` FROM `executable_container` WHERE `id` = LAST_INSERT_ID()");
            try {
                if (rs.next()) {
                    executableContainer.setId(rs.getLong("id"));
                    executableContainer.setExternalRef(rs.getString("external_ref"));
                    executableContainer.setCreatedAt(rs.getTimestamp("created_at"));
                    executableContainer.setStatus(rs.getString("status"));
                    executableContainer.setSlotId(rs.getLong("slot_id"));
                } else
                    throw new Exception("Unexpected error: result set is empty");
            } finally {
                rs.close();
                stmt.close();
            }
        } finally {
            conn.close();
        }
    }

    @Override
    public void setExecutableContainerJson(Long containerId, String json) throws Exception {
        Connection conn = getConnection();
        try {
            PreparedStatement pstmt = conn.prepareStatement("UPDATE `executable_container` SET `json` = ? WHERE `id` = ?");

            try {
                pstmt.setString(1, json);
                pstmt.setLong(2, containerId);
                pstmt.executeUpdate();
            } finally {
                pstmt.close();
            }
        } finally {
            conn.close();
        }
    }

    @Override
    public void registerVolumeContainer(VolumeContainer volumeContainer) throws Exception {
        Connection conn = getConnection();
        try {
            PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO `volume_container` ("
                  + "	`external_ref`,"
                  + "	`node_id`,"
                  + "	`status`,"
                  + "	`volume_image_id`,"
                  + "	`user_id`)"
                  + "VALUES ("
                  + "	UUID(),"
                  + "	?,"
                  + "	?,"
                  + "	?,"
                  + "	?)");

            try {
                pstmt.setLong(1, volumeContainer.getNodeId());
                pstmt.setString(2, ContainerStatus.REGISTERED);
                pstmt.setLong(3, volumeContainer.getVolumeImageId());
                pstmt.setString(4, volumeContainer.getUserId());
                pstmt.executeUpdate();
            } finally {
                pstmt.close();
            }

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT `id`,`external_ref`, `Status` FROM `volume_container` WHERE `id` = LAST_INSERT_ID()");
            try {
                if (rs.next()) {
                    volumeContainer.setId(rs.getLong("id"));
                    volumeContainer.setExternalRef(rs.getString("external_ref"));
                    volumeContainer.setStatus(rs.getString("status"));
                } else
                    throw new Exception("Unexpected error: result set is empty");
            } finally {
                rs.close();
                stmt.close();
            }
        } finally {
            conn.close();
        }
    }

    @Override
    public void registerRACMVolumeContainer(VolumeContainer volumeContainer) throws Exception {
        Connection conn = getConnection();
        try {
            PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO `racm_volume_container` ("
                  + "	`external_ref`,"
                  + "	`node_id`,"
                  + "	`status`,"
                  + "	`racm_id`,"
                  + "	`user_id`)"
                  + "VALUES ("
                  + "	UUID(),"
                  + "	?,"
                  + "	?,"
                  + "	?,"
                  + "	?)");

            try {
                pstmt.setLong(1, volumeContainer.getNodeId());
                pstmt.setString(2, ContainerStatus.REGISTERED);
                pstmt.setLong(3, volumeContainer.getVolumeImageId());
                pstmt.setString(4, volumeContainer.getUserId());
                pstmt.executeUpdate();
            } finally {
                pstmt.close();
            }

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT `id`,`external_ref`, `Status` FROM `racm_volume_container` WHERE `id` = LAST_INSERT_ID()");
            try {
                if (rs.next()) {
                    volumeContainer.setId(rs.getLong("id"));
                    volumeContainer.setExternalRef(rs.getString("external_ref"));
                    volumeContainer.setStatus(rs.getString("status"));
                } else
                    throw new Exception("Unexpected error: result set is empty");
            } finally {
                rs.close();
                stmt.close();
            }
        } finally {
            conn.close();
        }
    }

    @Override
    public void setProxy(ExecutableContainer executableContainer) throws Exception {
        Node node = executableContainer.getSlot().getNode();
        HttpProxyClient proxy = createHttpProxyClient(node.getProxyApiUrl(), node.getProxyApiClientCert(),
                node.getProxyApiClientKey());

        String externalPath = node.getProxyBaseUrl().getPath() + executableContainer.getExternalRef();
        proxy.setProxy(externalPath, "http://127.0.0.1:" + executableContainer.getSlot().getPortNumber(),
                executableContainer.getUserId(), executableContainer.getId());
    }

    @Override
    public void startContainer(ExecutableContainer executableContainer) throws Exception {
        Node node = executableContainer.getSlot().getNode();
        DockerClient docker = createDockerClient(node.getDockerApiUrl(), node.getDockerApiClientCert(),
                node.getDockerApiClientKey());
        docker.startContainer(executableContainer.getDockerRef());
    }

    @Override
    public void stopContainer(ExecutableContainer executableContainer) throws Exception {
        Node node = executableContainer.getSlot().getNode();
        DockerClient docker = createDockerClient(node.getDockerApiUrl(), node.getDockerApiClientCert(),
                node.getDockerApiClientKey());
        docker.stopContainer(executableContainer.getDockerRef());
    }

    @Override
    public void unregisterExecutableContainer(ExecutableContainer executableContainer) throws SQLException {
        Connection conn = getConnection();
        PreparedStatement pstmt = conn.prepareStatement("UPDATE `executable_container` SET status = ? WHERE `id` = ?");

        try {
            pstmt.setString(1, ContainerStatus.DELETED);
            pstmt.setLong(2, executableContainer.getId());
            pstmt.executeUpdate();
        } finally {
            pstmt.close();
            conn.close();
        }
    }

    @Override
    public void unregisterVolumeContainer(VolumeContainer volumeContainer) throws SQLException {
        Connection conn = getConnection();
        PreparedStatement pstmt = conn.prepareStatement("UPDATE `volume_container` SET status = ? WHERE `id` = ?");

        try {
            pstmt.setString(1, ContainerStatus.DELETED);
            pstmt.setLong(2, volumeContainer.getId());
            pstmt.executeUpdate();
        } finally {
            pstmt.close();
            conn.close();
        }
    }

    @Override
    public void unregisterRACMVolumeContainer(VolumeContainer volumeContainer) throws SQLException {
        Connection conn = getConnection();
        PreparedStatement pstmt = conn.prepareStatement("UPDATE `racm_volume_container` SET status = ? WHERE `id` = ?");

        try {
            pstmt.setString(1, ContainerStatus.DELETED);
            pstmt.setLong(2, volumeContainer.getId());
            pstmt.executeUpdate();
        } finally {
            pstmt.close();
            conn.close();
        }
    }

    @Override
    public void updateExecutableContainer(ExecutableContainer executableContainer) throws SQLException {
        Connection conn = getConnection();
        PreparedStatement pstmt = conn
                .prepareStatement("UPDATE `executable_container` SET `docker_ref` = ?, `status` = ? WHERE `id` = ?");

        try {
            pstmt.setString(1, executableContainer.getDockerRef());
            pstmt.setString(2, executableContainer.getStatus());
            pstmt.setLong(3, executableContainer.getId());
            pstmt.executeUpdate();
        } finally {
            pstmt.close();
            conn.close();
        }
    }

    @Override
    public void updateDaskCluster(DaskCluster daskCluster) throws SQLException {
        Connection conn = getConnection();
        PreparedStatement pstmt = conn
                .prepareStatement("UPDATE `dask_cluster` SET `status` = ? WHERE `id` = ?");

        try {
            pstmt.setString(1, daskCluster.getStatus());
            pstmt.setLong(2, daskCluster.getId());
            pstmt.executeUpdate();
        } finally {
            pstmt.close();
            conn.close();
        }
    }

    @Override
    public void updatePublicVolumes(Iterable<PublicVolume> volumes, ExecutableContainer executableContainer)
            throws SQLException {
        Connection conn = getConnection();
        PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO `attached_public_volume` (`executable_container_id`, `public_volume_id`) VALUES (?, ?)");

        try {
            for (PublicVolume volume : volumes) {
                pstmt.setLong(1, executableContainer.getId());
                pstmt.setLong(2, volume.getId());
                pstmt.executeUpdate();
            }
        } finally {
            pstmt.close();
            conn.close();
        }
    }

    @Override
    public void updateUserVolumes(Iterable<VolumeContainer> volumes, ExecutableContainer executableContainer)
            throws SQLException {
        Connection conn = getConnection();
        PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO `attached_user_volume` (`executable_container_id`, `volume_container_id`) VALUES (?, ?)");

        try {
            for (VolumeContainer volume : volumes) {
                pstmt.setLong(1, executableContainer.getId());
                pstmt.setLong(2, volume.getId());
                pstmt.executeUpdate();
            }
        } finally {
            pstmt.close();
            conn.close();
        }
    }

    @Override
    public void updateVolumeContainer(VolumeContainer volumeContainer) throws SQLException {
        Connection conn = getConnection();
        PreparedStatement pstmt = conn
                .prepareStatement("UPDATE `volume_container` SET `docker_ref` = ?, `status` = ? WHERE `id` = ?");

        try {
            pstmt.setString(1, volumeContainer.getDockerRef());
            pstmt.setString(2, volumeContainer.getStatus());
            pstmt.setLong(3, volumeContainer.getId());
            pstmt.executeUpdate();
        } finally {
            pstmt.close();
            conn.close();
        }
    }

    @Override
    public void updateRACMVolumeContainer(VolumeContainer volumeContainer) throws SQLException {
        Connection conn = getConnection();
        PreparedStatement pstmt = conn
                .prepareStatement("UPDATE `racm_volume_container` SET `docker_ref` = ?, `status` = ? WHERE `id` = ?");

        try {
            pstmt.setString(1, volumeContainer.getDockerRef());
            pstmt.setString(2, volumeContainer.getStatus());
            pstmt.setLong(3, volumeContainer.getId());
            pstmt.executeUpdate();
        } finally {
            pstmt.close();
            conn.close();
        }
    }

    private Connection getConnection() throws SQLException {
        return this.getDataSource().getConnection();
    }

    private Domain getDomainFromReader(ResultSet rs) throws Exception {
        Domain res = new Domain(this);

        res.setId(rs.getLong("id"));
        res.setName(rs.getString("name"));
        res.setDescription(rs.getString("description"));
        res.setMaxMemory(rs.getLong("max_memory"));
        res.setNanoCpus(rs.getBigDecimal("max_cpus").multiply(new BigDecimal(1000000000)).toBigInteger().longValueExact());
        res.setShmBytes(rs.getLong("shm_bytes"));
        res.setMaxSessionSecs(rs.getLong("max_session_secs"));
        res.setType(rs.getString("type"));

        return res;
    }

    private ExecutableContainer getExecutableContainerFromReader(ResultSet rs) throws Exception {
        ExecutableContainer res = new ExecutableContainer(this);

        res.setId(rs.getLong("id"));
        res.setDockerRef(rs.getString("docker_ref"));
        res.setExternalRef(rs.getString("external_ref"));
        res.setSlotId(rs.getLong("slot_id"));
        res.setUserId(rs.getString("user_id"));
        res.setCreatedAt(rs.getTimestamp("created_at"));
        res.setAccessedAt(rs.getTimestamp("accessed_at"));
        res.setExecutableImageId(rs.getLong("executable_image_id"));
        res.setDescription(rs.getString("description"));
        res.setName(rs.getString("name"));
        res.setStatus(rs.getString("status"));
        res.setJson(rs.getString("json"));
        if (rs.getObject("cluster_id") != null) {
            res.setDaskClusterId(rs.getLong("cluster_id"));
        }

        return res;
    }

    private ExecutableImage getExecutableImageFromReader(ResultSet rs) throws Exception {
        ExecutableImage res = new ExecutableImage(this);

        res.setId(rs.getLong("id"));
        res.setName(rs.getString("name"));
        res.setDescription(rs.getString("description"));
        res.setDomainId(rs.getLong("domain_id"));
        res.setDockerRef(rs.getString("docker_ref"));
        res.setContainerManagerClass(rs.getString("container_manager_class"));

        return res;
    }

    private Node getNodeFromReader(ResultSet rs) throws Exception {
        Node res = new Node(this);

        res.setId(rs.getLong("id"));
        res.setName(rs.getString("name"));
        res.setDescription(rs.getString("description"));
        res.setDomainId(rs.getLong("domain_id"));
        res.setDockerApiUrl(rs.getURL("docker_api_url"));
        res.setDockerApiClientCert(rs.getString("docker_api_client_cert"));
        res.setDockerApiClientKey(rs.getString("docker_api_client_key"));
        res.setProxyApiUrl(rs.getURL("proxy_api_url"));
        res.setProxyApiClientCert(rs.getString("proxy_api_client_cert"));
        res.setProxyApiClientKey(rs.getString("proxy_api_client_key"));
        res.setProxyBaseUrl(rs.getURL("proxy_base_url"));
        res.setEnabled(rs.getBoolean("enabled"));

        return res;
    }

    private PublicVolume getPublicVolumeFromReader(ResultSet rs) throws Exception {
        PublicVolume res = new PublicVolume(this);

        res.setId(rs.getLong("id"));
        res.setName(rs.getString("name"));
        res.setDescription(rs.getString("description"));
        res.setDomainId(rs.getLong("domain_id"));
        res.setDockerRef(rs.getString("docker_ref"));
        res.setSelectedByDefault(rs.getBoolean("selected_by_default"));

        return res;
    }

    private K8sCluster getK8sClusterFromReader(ResultSet rs) throws Exception {
        K8sCluster res = new K8sCluster(this);

        res.setId(rs.getLong("id"));
        res.setName(rs.getString("name"));
        res.setDescription(rs.getString("description"));
        res.setApiToken(rs.getString("api_token"));
        res.setApiUrl(rs.getURL("api_url"));
        res.setPublicUrl(rs.getURL("public_url"));
        res.setNamespace(rs.getString("namespace"));
        res.setMemOvercommit(rs.getBigDecimal("mem_overcommit_rate").floatValue());
        res.setCpuOvercommit(rs.getBigDecimal("cpu_overcommit_rate").floatValue());
        res.setDomainId(rs.getLong("domain_id"));
        res.setEnabled(rs.getBoolean("enabled"));

        return res;
    }

    private GenericVolume getGenericVolumeFromReader(ResultSet rs) throws Exception {
        GenericVolume res = new GenericVolume(this);

        res.setId(rs.getLong("id"));
        res.setName(rs.getString("name"));
        res.setDescription(rs.getString("description"));
        res.setSource(rs.getString("source"));
        res.setMountPath(rs.getString("mount_path"));
        res.setVolumeManagerClass(rs.getString("volume_manager_class"));
        res.setDomainId(rs.getLong("domain_id"));

        return res;
    }

    private DaskCluster getDaskClusterFromReader(ResultSet rs) throws Exception {
        DaskCluster res = new DaskCluster(this);

        res.setId(rs.getLong("id"));
        res.setName(rs.getString("name"));
        res.setDescription(rs.getString("description"));
        res.setExternalRef(rs.getString("external_ref"));
        res.setImageId(rs.getLong("image_id"));
        res.setUserId(rs.getString("user_id"));
        res.setCreatedAt(rs.getTimestamp("created_at"));
        res.setK8sClusterId(rs.getLong("k8s_cluster_id"));
        res.setStatus(rs.getString("status"));

        return res;
    }

    private <T extends RegistryObject> T getRegistryObjectFromResultSet(ResultSet rs, Class<T> type) throws Exception {
        if (type.equals(ExecutableContainer.class))
            return (T) getExecutableContainerFromReader(rs);
        if (type.equals(Domain.class))
            return (T) getDomainFromReader(rs);
        if (type.equals(Node.class))
            return (T) getNodeFromReader(rs);
        if (type.equals(ExecutableImage.class))
            return (T) getExecutableImageFromReader(rs);
        if (type.equals(VolumeImage.class))
            return (T) getVolumeImageFromReader(rs);
        if (type.equals(VolumeContainer.class))
            return (T) getVolumeContainerFromReader(rs);
        if (type.equals(PublicVolume.class))
            return (T) getPublicVolumeFromReader(rs);
        if (type.equals(Slot.class))
            return (T) getSlotFromReader(rs);
        if (type.equals(RACMVolumeContainer.class))
            return (T) getRACMVolumeContainerFromReader(rs);
        if (type.equals(K8sCluster.class))
            return (T) getK8sClusterFromReader(rs);
        if (type.equals(DaskCluster.class))
            return (T) getDaskClusterFromReader(rs);
        if (type.equals(GenericVolume.class))
            return (T) getGenericVolumeFromReader(rs);
        throw new Exception("Type not supported");
    }

    private <T extends RegistryObject> Iterable<T> getRegistryObjectsFromQuery(PreparedStatement command, Class<T> type)
            throws Exception {
        ArrayList<T> list = new ArrayList<T>();
        ResultSet rs = command.executeQuery();
        try {
            while (rs.next()) {
                list.add(getRegistryObjectFromResultSet(rs, type));
            }
        } finally {
            rs.close();
        }

        return list;
    }

    private Slot getSlotFromReader(ResultSet rs) throws Exception {
        Slot res = new Slot(this);

        res.setId(rs.getLong("id"));
        res.setNodeId(rs.getLong("node_id"));
        res.setPortNumber(rs.getInt("port_number"));

        return res;
    }

    private VolumeContainer getVolumeContainerFromReader(ResultSet rs) throws Exception {
        VolumeContainer res = new VolumeContainer(this);

        res.setId(rs.getLong("id"));
        res.setDockerRef(rs.getString("docker_ref"));
        res.setExternalRef(rs.getString("external_ref"));
        res.setNodeId(rs.getLong("node_id"));
        res.setUserId(rs.getString("user_id"));
        res.setVolumeImageId(rs.getLong("volume_image_id"));
        res.setStatus(rs.getString("status"));

        return res;
    }

    private RACMVolumeContainer getRACMVolumeContainerFromReader(ResultSet rs) throws Exception {
        RACMVolumeContainer res = new RACMVolumeContainer(this);

        res.setId(rs.getLong("id"));
        res.setDockerRef(rs.getString("docker_ref"));
        res.setExternalRef(rs.getString("external_ref"));
        res.setNodeId(rs.getLong("node_id"));
        res.setUserId(rs.getString("user_id"));
        res.setVolumeImageId(rs.getLong("racm_id"));
        res.setStatus(rs.getString("status"));

        return res;
    }

    private VolumeImage getVolumeImageFromReader(ResultSet rs) throws Exception {
        VolumeImage res = new VolumeImage(this);

        res.setId(rs.getLong("id"));
        res.setName(rs.getString("name"));
        res.setDescription(rs.getString("description"));
        res.setDomainId(rs.getLong("domain_id"));
        res.setDockerRef(rs.getString("docker_ref"));
        res.setContainerPath(rs.getString("container_path"));
        res.setLocalPathTemplate(rs.getString("local_path_template"));
        res.setContainerManagerClass(rs.getString("container_manager_class"));

        return res;
    }

    @Override
    public Iterable<VolumeContainer> getUserVolumes(String userId, Node node) throws Exception {
        Connection conn = getConnection();
        PreparedStatement pstmt = conn
                .prepareStatement("SELECT * FROM `volume_container` WHERE `node_id` = ? AND `user_id` = ? AND `status` <> ?");

        try {
            pstmt.setLong(1, node.getId());
            pstmt.setString(2, userId);
            pstmt.setString(3, ContainerStatus.DELETED);
            return getRegistryObjectsFromQuery(pstmt, VolumeContainer.class);
        } finally {
            pstmt.close();
            conn.close();
        }
    }

    @Override
    public Iterable<RACMVolumeContainer> getRACMUserVolumes(String userId, Node node) throws Exception {
        Connection conn = getConnection();
        PreparedStatement pstmt = conn
                .prepareStatement("SELECT * FROM `racm_volume_container` WHERE `node_id` = ? AND `user_id` = ? AND `status` <> ?");

        try {
            pstmt.setLong(1, node.getId());
            pstmt.setString(2, userId);
            pstmt.setString(3, ContainerStatus.DELETED);
            return getRegistryObjectsFromQuery(pstmt, RACMVolumeContainer.class);
        } finally {
            pstmt.close();
            conn.close();
        }
    }

    @Override
    public String getSettings(String key) throws Exception {
        Connection conn = getConnection();
        PreparedStatement pstmt = conn.prepareStatement("SELECT `value` FROM " + settingsTable + " WHERE `key` = ?");

        try {
            pstmt.setString(1, key);
            ResultSet rs = pstmt.executeQuery();
            try {
                if (rs.next()) {
                    return rs.getString("Value");
                } else
                    throw new Exception("Key '" + key + "' not found in `" + settingsTable + "`");
            } finally {
                rs.close();
            }
        } finally {
            pstmt.close();
            conn.close();
        }
    }

    @Override
    public Iterable<ExecutableContainer> getContainers(String userId) throws Exception {
        Connection conn = this.getConnection();
        PreparedStatement pstmt = conn.prepareStatement("SELECT a.*, c.`id` AS `cluster_id` FROM `executable_container` a LEFT OUTER JOIN `linked_dask_cluster` b ON a.`id` = b.`container_id` LEFT OUTER JOIN `dask_cluster` c ON b.`cluster_id` = c.`id` WHERE a.`user_id` = ? AND a.`status` <> ? ORDER BY a.`created_at` DESC");
        try {
            pstmt.setString(1, userId);
            pstmt.setString(2, ContainerStatus.DELETED);
            Iterable<ExecutableContainer> iterable = this.getRegistryObjectsFromQuery(pstmt, ExecutableContainer.class);
            return iterable;
        }
        finally {
            pstmt.close();
            conn.close();
        }
    }

    @Override
    public Iterable<ExecutableContainer> getInactiveContainers(Date inactiveSince, boolean unknown) throws Exception {
        Connection conn = this.getConnection();
        PreparedStatement pstmt = conn.prepareStatement("SELECT a.*, c.`id` AS `cluster_id` FROM `executable_container` a LEFT OUTER JOIN `linked_dask_cluster` b ON a.`id` = b.`container_id` LEFT OUTER JOIN `dask_cluster` c ON b.`cluster_id` = c.`id` WHERE a.`status` <> ? AND a.`created_at` < ? AND (" + (unknown ? "a.`accessed_at` IS NULL OR " : "") + "a.`accessed_at` < ?) ORDER BY a.`created_at` DESC");
        try {
            pstmt.setString(1, ContainerStatus.DELETED);
            pstmt.setTimestamp(2, new java.sql.Timestamp(inactiveSince.getTime()));
            pstmt.setTimestamp(3, new java.sql.Timestamp(inactiveSince.getTime()));
            Iterable<ExecutableContainer> iterable = this.getRegistryObjectsFromQuery(pstmt, ExecutableContainer.class);
            return iterable;
        }
        finally {
            pstmt.close();
            conn.close();
        }
    }

    @Override
    public Iterable<ExecutableContainer> getExpiredContainers() throws Exception {
        Connection conn = this.getConnection();
        PreparedStatement pstmt = conn.prepareStatement("SELECT a.*, c.`id` AS `cluster_id` FROM `executable_container` a LEFT OUTER JOIN `linked_dask_cluster` b ON a.`id` = b.`container_id` LEFT OUTER JOIN `dask_cluster` c ON b.`cluster_id` = c.`id` LEFT OUTER JOIN `executable_image` i ON a.`executable_image_id` = i.`id` LEFT OUTER JOIN `domain` d ON i.`domain_id` = d.`id` WHERE a.`status` <> ? AND (d.`max_session_secs` > 0 AND TIME_TO_SEC(TIMEDIFF(CURRENT_TIMESTAMP, a.`created_at`)) > d.`max_session_secs`)");
        try {
            pstmt.setString(1, ContainerStatus.DELETED);
            Iterable<ExecutableContainer> iterable = this.getRegistryObjectsFromQuery(pstmt, ExecutableContainer.class);
            return iterable;
        }
        finally {
            pstmt.close();
            conn.close();
        }
    }

    @Override
    public Iterable<Domain> getDomains() throws Exception {
        Connection conn = getConnection();
        PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM `domain`");

        try {
            return getRegistryObjectsFromQuery(pstmt, Domain.class);
        } finally {
            pstmt.close();
            conn.close();
        }
    }

    @Override
    public Iterable<Node> getNodes(Domain domain) throws Exception {
        Connection conn = getConnection();
        PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM `node` WHERE `domain_id` = ?");

        try {
            pstmt.setLong(1, domain.getId());
            return getRegistryObjectsFromQuery(pstmt, Node.class);
        } finally {
            pstmt.close();
            conn.close();
        }
    }

    @Override
    public Iterable<ExecutableImage> getExecutableImages(Domain domain) throws Exception {
        Connection conn = getConnection();
        PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM `executable_image` WHERE `domain_id` = ? ORDER BY `display_order`");

        try {
            pstmt.setLong(1, domain.getId());
            return getRegistryObjectsFromQuery(pstmt, ExecutableImage.class);
        } finally {
            pstmt.close();
            conn.close();
        }
    }

    @Override
    public Iterable<VolumeImage> getVolumeImages(Domain domain) throws Exception{
        Connection conn = getConnection();
        PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM `volume_image` WHERE `domain_id` = ? ORDER BY `display_order`");

        try {
            pstmt.setLong(1, domain.getId());
            return getRegistryObjectsFromQuery(pstmt, VolumeImage.class);
        } finally {
            pstmt.close();
            conn.close();
        }
    }

    @Override
    public Iterable<PublicVolume> getPublicVolumes(Domain domain) throws Exception {
        Connection conn = getConnection();
        PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM `public_volume` WHERE `domain_id` = ? ORDER BY `display_order`");

        try {
            pstmt.setLong(1, domain.getId());
            return getRegistryObjectsFromQuery(pstmt, PublicVolume.class);
        } finally {
            pstmt.close();
            conn.close();
        }
    }

    @Override
    public Iterable<K8sCluster> getK8sClusters(Domain domain) throws Exception {
        Connection conn = getConnection();
        PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM `k8s_cluster` WHERE `domain_id` = ?");

        try {
            pstmt.setLong(1, domain.getId());
            return getRegistryObjectsFromQuery(pstmt, K8sCluster.class);
        } finally {
            pstmt.close();
            conn.close();
        }
    }

    @Override
    public ExecutableImage getExecutableImage(Domain domain, String imageName) throws Exception {
        Connection conn = getConnection();
        PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM `executable_image` WHERE `domain_id` = ? AND `name` = ?");

        try {
            pstmt.setLong(1, domain.getId());
            pstmt.setString(2, imageName);
            Iterator<ExecutableImage> i = getRegistryObjectsFromQuery(pstmt, ExecutableImage.class).iterator();
            if (i.hasNext())
                return i.next();
            else
                throw new Exception("ExecutableImage not found");
        } finally {
            pstmt.close();
            conn.close();
        }
    }

    @Override
    public long getUsedSlots(Node node) throws Exception {
        Connection conn = getConnection();
        PreparedStatement pstmt = conn.prepareStatement("SELECT count(*) FROM `executable_container` a JOIN `slot` b ON a.slot_id = b.id WHERE `node_id` = ? AND `status` <> 'DELETED'");

        try {
            pstmt.setLong(1, node.getId());
            ResultSet rs = pstmt.executeQuery();
            try {
                if (rs.next()) {
                    return rs.getLong(1);
                } else
                    return 0;
            } finally {
                rs.close();
            }
        } finally {
            pstmt.close();
            conn.close();
        }
    }

    @Override
    public long getTotalSlots(Node node) throws Exception {
        Connection conn = getConnection();
        PreparedStatement pstmt = conn.prepareStatement("SELECT count(*) FROM `slot` WHERE `node_id` = ?");

        try {
            pstmt.setLong(1, node.getId());
            ResultSet rs = pstmt.executeQuery();
            try {
                if (rs.next()) {
                    return rs.getLong(1);
                } else
                    return 0;
            } finally {
                rs.close();
            }
        } finally {
            pstmt.close();
            conn.close();
        }
    }

    @Override
    public Iterable<VolumeContainer> getAttachedUserVolumes(ExecutableContainer container) throws Exception {
        Connection conn = getConnection();
        PreparedStatement pstmt = conn.prepareStatement("SELECT `volume_container`.* FROM `volume_container` JOIN `attached_user_volume` ON `volume_container_id`=`id` WHERE `executable_container_id` = ?");

        try {
            pstmt.setLong(1, container.getId());
            return getRegistryObjectsFromQuery(pstmt, VolumeContainer.class);
        } finally {
            pstmt.close();
            conn.close();
        }
    }

    @Override
    public Iterable<PublicVolume> getAttachedPublicVolumes(ExecutableContainer container) throws Exception {
        Connection conn = getConnection();
        PreparedStatement pstmt = conn.prepareStatement("SELECT `public_volume`.* FROM `public_volume` JOIN `attached_public_volume` ON `public_volume_id`=`id` WHERE `executable_container_id` = ? ORDER BY `display_order`");

        try {
            pstmt.setLong(1, container.getId());
            return getRegistryObjectsFromQuery(pstmt, PublicVolume.class);
        } finally {
            pstmt.close();
            conn.close();
        }
    }

    @Override
    public void acquireLock() throws Exception {
        _createContainer.acquire();
    }

    @Override
    public void releaseLock() {
        _createContainer.release();
    }

    @Override
    public Iterable<ExecutableContainer> getContainers() throws Exception {
        Connection conn = getConnection();
        PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM `executable_container` WHERE `status` <> ? ORDER BY `created_at` DESC");

        try {
            pstmt.setString(1, ContainerStatus.DELETED);
            return getRegistryObjectsFromQuery(pstmt, ExecutableContainer.class);
        } finally {
            pstmt.close();
            conn.close();
        }

    }

    @Override
    public long adminCreateDomain(DomainInfo domainInfo) throws Exception {
        Connection conn = getConnection();
        try {
            PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO domain(" +
                "`name`,`description`,`display_order`,`max_memory`, `max_cpus`, `shm_bytes`, `max_session_secs`) " +
                "values(?,?,0,?,?,?,?)");
            try {
                pstmt.setString(1, domainInfo.getName());
                pstmt.setString(2, domainInfo.getDescription());
                pstmt.setLong(3, domainInfo.getMaxMemory());
                pstmt.setLong(4, domainInfo.getNanoCpus());
                pstmt.setLong(5, domainInfo.getShmBytes());
                pstmt.setLong(6, domainInfo.getMaxSessionSecs());
                pstmt.executeUpdate();
            } finally {
                pstmt.close();
            }

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT `id` FROM `domain` WHERE `id` = LAST_INSERT_ID()");
            try {
                if (rs.next()) {
                    return rs.getLong("id");
                } else
                    throw new Exception("Unexpected error: result set is empty");
            } finally {
                rs.close();
                stmt.close();
            }
        }
        finally {
            conn.close();
        }
    }

    @Override
    public long adminCreateNode(NodeInfo nodeInfo) throws Exception {
        Connection conn = getConnection();
        try {
            PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO node("
                    + "`name`, "
                    + "`description`, "
                    + "`docker_api_url`, "
                    + "`docker_api_client_cert`, "
                    + "`docker_api_client_key`, "
                    + "`proxy_api_url`, "
                    + "`proxy_api_client_cert`, "
                    + "`proxy_api_client_key`, "
                    + "`proxy_base_url`, "
                    + "`domain_id`, "
                    + "`enabled`) "
                    + "values(?,?,?,?,?,?,?,?,?,?,?)");

            try {
                pstmt.setString(1, nodeInfo.getName());
                pstmt.setString(2, nodeInfo.getDescription());
                pstmt.setString(3, nodeInfo.getDockerApiUrl());
                pstmt.setString(4, nodeInfo.getDockerApiClientCert());
                pstmt.setString(5, nodeInfo.getDockerApiClientKey());
                pstmt.setString(6, nodeInfo.getProxyApiUrl());
                pstmt.setString(7, nodeInfo.getProxyApiClientCert());
                pstmt.setString(8, nodeInfo.getProxyApiClientKey());
                pstmt.setString(9, nodeInfo.getProxyBaseUrl());
                pstmt.setLong(10, nodeInfo.getDomainId());
                pstmt.setBoolean(11, true);
                pstmt.executeUpdate();
            } finally {
                pstmt.close();
            }

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT `id` FROM `node` WHERE `id` = LAST_INSERT_ID()");
            try {
                if (rs.next()) {
                    return rs.getLong("id");
                } else
                    throw new Exception("Unexpected error: result set is empty");
            } finally {
                rs.close();
                stmt.close();
            }
        }
        finally {
            conn.close();
        }
    }

    @Override
    public long adminCreateImage(ImageInfo imageInfo) throws Exception {
        Connection conn = getConnection();
        try {
            PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO executable_image("
                    + "`name`, "
                    + "`description`, "
                    + "`docker_ref`, "
                    + "`container_manager_class`, "
                    + "`domain_id`, "
                    + "`display_order`) "
                    + "values(?,?,?,?,?,0)");
            try {
                pstmt.setString(1, imageInfo.getName());
                pstmt.setString(2, imageInfo.getDescription());
                pstmt.setString(3, imageInfo.getDockerRef());
                pstmt.setString(4, imageInfo.getContainerManagerClass());
                pstmt.setLong(5, imageInfo.getDomainId());
                pstmt.executeUpdate();
            } finally {
                pstmt.close();
            }

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT `id` FROM `executable_image` WHERE `id` = LAST_INSERT_ID()");
            try {
                if (rs.next()) {
                    return rs.getLong("id");
                } else
                    throw new Exception("Unexpected error: result set is empty");
            } finally {
                rs.close();
                stmt.close();
            }
        }
        finally {
            conn.close();
        }
    }

    @Override
    public void adminCreateSlots(SlotsInfo slotsInfo) throws SQLException {
        Connection conn = getConnection();
        PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO slot("
                + "`node_id`, "
                + "`port_number`) "
                + "values(?,?)");
        try {
            for (int portNumber : slotsInfo.getPortNumbers()) {
                pstmt.setLong(1, slotsInfo.getNodeId());
                pstmt.setInt(2, portNumber);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        } finally {
            pstmt.close();
            conn.close();
        }
    }

    @Override
    public long adminCreatePublicVolume(PublicVolumeInfo publicVolumeInfo) throws Exception {
        Connection conn = getConnection();
        try {
            PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO public_volume("
                    + "`name`, "
                    + "`description`, "
                    + "`docker_ref`, "
                    + "`domain_id`, "
                    + "`display_order`, "
                    + "`selected_by_default`) "
                    + "values(?,?,?,?,0,?)");
            try {
                pstmt.setString(1, publicVolumeInfo.getName());
                pstmt.setString(2, publicVolumeInfo.getDescription());
                pstmt.setString(3, publicVolumeInfo.getDockerRef());
                pstmt.setLong(4, publicVolumeInfo.getDomainId());
                pstmt.setBoolean(5, publicVolumeInfo.isSelectedByDefault());
                pstmt.executeUpdate();
            } finally {
                pstmt.close();
            }

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT `id` FROM `public_volume` WHERE `id` = LAST_INSERT_ID()");
            try {
                if (rs.next()) {
                    return rs.getLong("id");
                } else
                    throw new Exception("Unexpected error: result set is empty");
            } finally {
                rs.close();
                stmt.close();
            }
        }
        finally {
            conn.close();
        }
    }

    @Override
    public long adminCreateK8sCluster(K8sClusterInfo k8sCluster) throws Exception {
        Connection conn = getConnection();
        try {
            PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO k8s_cluster("
                    + "`name`, "
                    + "`description`, "
                    + "`api_url`, "
                    + "`api_token`, "
                    + "`public_url`, "
                    + "`namespace`, "
                    + "`mem_overcommit_rate`, "
                    + "`cpu_overcommit_rate`, "
                    + "`domain_id`, "
                    + "`enabled`) "
                    + "values(?,?,?,?,?,?,?,?,?,?)");
            try {
                pstmt.setString(1, k8sCluster.getName());
                pstmt.setString(2, k8sCluster.getDescription());
                pstmt.setString(3, k8sCluster.getApiUrl().toString());
                pstmt.setString(4, k8sCluster.getApiToken());
                pstmt.setString(5, k8sCluster.getPublicUrl().toString());
                pstmt.setString(6, k8sCluster.getNamespace());
                pstmt.setFloat(7, k8sCluster.getMemOvercommit());
                pstmt.setFloat(8, k8sCluster.getCpuOvercommit());
                pstmt.setLong(9, k8sCluster.getDomainId());
                pstmt.setBoolean(10, k8sCluster.isEnabled());
                pstmt.executeUpdate();
            } finally {
                pstmt.close();
            }

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT `id` FROM `k8s_cluster` WHERE `id` = LAST_INSERT_ID()");
            try {
                if (rs.next()) {
                    return rs.getLong("id");
                } else
                    throw new Exception("Unexpected error: result set is empty");
            } finally {
                rs.close();
                stmt.close();
            }
        }
        finally {
            conn.close();
        }
    }

    @Override
    public List<NodeInfo> adminGetNodes() throws Exception {
        List<NodeInfo> result = new ArrayList<NodeInfo>();
        Connection conn = getConnection();
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT "
                    + "`id`, "
                    + "`name`, "
                    + "`description`, "
                    + "`docker_api_url`, "
                    + "`docker_api_client_cert`, "
                    + "`docker_api_client_key`, "
                    + "`proxy_api_url`, "
                    + "`proxy_api_client_cert`, "
                    + "`proxy_api_client_key`, "
                    + "`proxy_base_url`, "
                    + "`domain_id` "
                    + "FROM `node` "
                    + "ORDER BY `id`");

            try {
                while (rs.next()) {
                    NodeInfo item = new NodeInfo();
                    item.setId(rs.getLong("id"));
                    item.setName(rs.getString("name"));
                    item.setDescription(rs.getString("description"));
                    item.setDockerApiUrl(rs.getString("docker_api_url"));
                    item.setDockerApiClientKey(rs.getString("docker_api_client_key"));
                    item.setDockerApiClientCert(rs.getString("docker_api_client_cert"));
                    item.setProxyApiUrl(rs.getString("proxy_api_url"));
                    item.setProxyApiClientKey(rs.getString("proxy_api_client_key"));
                    item.setProxyApiClientCert(rs.getString("proxy_api_client_cert"));
                    item.setProxyBaseUrl(rs.getString("proxy_base_url"));
                    item.setDomainId(rs.getLong("domain_id"));
                    result.add(item);
                }
            } finally {
                rs.close();
                stmt.close();
            }
        }
        finally {
            conn.close();
        }
        return result;
    }

    @Override
    public List<SlotsInfo> adminGetSlots() throws Exception {
        List<SlotsInfo> result = new ArrayList<SlotsInfo>();
        Connection conn = getConnection();
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT "
                    + "`node_id`, "
                    + "`port_number` "
                    + "FROM `slot` "
                    + "ORDER BY `node_id`, `port_number`");

            try {
                long currentNodeId = Long.MIN_VALUE;
                SlotsInfo currentItem = null;
                while (rs.next()) {
                    long nodeId = rs.getLong("node_id");
                    if (currentNodeId != nodeId) {
                        result.add(currentItem);
                        currentItem = new SlotsInfo();
                        currentItem.setNodeId(nodeId);
                        currentItem.setPortNumbers(new ArrayList<Integer>());
                        currentNodeId = nodeId;
                    }
                    currentItem.getPortNumbers().add(rs.getInt("port_number"));
                }
            } finally {
                rs.close();
                stmt.close();
            }
        }
        finally {
            conn.close();
        }
        return result;
    }

    @Override
    public List<ImageInfo> adminGetImages() throws Exception {
        List<ImageInfo> result = new ArrayList<ImageInfo>();
        Connection conn = getConnection();
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT "
                    + "`id`, "
                    + "`name`, "
                    + "`description`, "
                    + "`docker_ref`, "
                    + "`container_manager_class`, "
                    + "`domain_id` "
                    + "FROM `executable_image` "
                    + "ORDER BY `id`");

            try {
                while (rs.next()) {
                    ImageInfo item = new ImageInfo();
                    item.setId(rs.getLong("id"));
                    item.setName(rs.getString("name"));
                    item.setDescription(rs.getString("description"));
                    item.setDockerRef(rs.getString("docker_ref"));
                    item.setContainerManagerClass(rs.getString("container_manager_class"));
                    item.setDomainId(rs.getLong("domain_id"));
                    result.add(item);
                }
            } finally {
                rs.close();
                stmt.close();
            }
        }
        finally {
            conn.close();
        }
        return result;
    }

    @Override
    public List<PublicVolumeInfo> adminGetPublicVolumes() throws Exception {
        List<PublicVolumeInfo> result = new ArrayList<PublicVolumeInfo>();
        Connection conn = getConnection();
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT "
                    + "`id`, "
                    + "`name`, "
                    + "`description`, "
                    + "`docker_ref`, "
                    + "`domain_id` "
                    + "FROM `public_volume` "
                    + "ORDER BY `id`");

            try {
                while (rs.next()) {
                    PublicVolumeInfo item = new PublicVolumeInfo();
                    item.setId(rs.getLong("id"));
                    item.setName(rs.getString("name"));
                    item.setDescription(rs.getString("description"));
                    item.setDockerRef(rs.getString("docker_ref"));
                    item.setDomainId(rs.getLong("domain_id"));
                    result.add(item);
                }
            } finally {
                rs.close();
                stmt.close();
            }
        }
        finally {
            conn.close();
        }
        return result;
    }

    @Override
    public List<DomainInfo> adminGetDomains() throws Exception {
        List<DomainInfo> result = new ArrayList<DomainInfo>();
        Connection conn = getConnection();
        try {
            Statement stmt = conn.createStatement();

            ResultSet rs = stmt.executeQuery(
                    "SELECT "
                    + "`id`, "
                    + "`name`, "
                    + "`description`, "
                    + "`max_memory` "
                    + "FROM `domain` "
                    + "ORDER BY `id`");

            try {
                while (rs.next()) {
                    DomainInfo item = new DomainInfo();
                    item.setId(rs.getLong("id"));
                    item.setName(rs.getString("name"));
                    item.setDescription(rs.getString("description"));
                    item.setMaxMemory(rs.getLong("max_memory"));
                    result.add(item);
                }
            } finally {
                rs.close();
                stmt.close();
            }
        }
        finally {
            conn.close();
        }
        return result;
    }

    @Override
    public List<K8sClusterInfo> adminGetK8sCluster() throws Exception {
        List<K8sClusterInfo> result = new ArrayList<K8sClusterInfo>();
        Connection conn = getConnection();
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT "
                    + "`id`, "
                    + "`name`, "
                    + "`description`, "
                    + "`api_url`, "
                    + "`api_token`, "
                    + "`public_url`, "
                    + "`namespace`, "
                    + "`mem_overcommit_rate`, "
                    + "`cpu_overcommit_rate`, "
                    + "`domain_id`, "
                    + "`enabled` "
                    + "FROM k8s_cluster ORDER BY id");
            try {
                while (rs.next()) {
                    K8sClusterInfo item = new K8sClusterInfo();
                    item.setId(rs.getLong("id"));
                    item.setName(rs.getString("name"));
                    item.setDescription(rs.getString("description"));
                    item.setApiToken(rs.getString("api_token"));
                    item.setApiUrl(rs.getString("api_url"));
                    item.setPublicUrl(rs.getString("public_url"));
                    item.setNamespace(rs.getString("namespace"));
                    item.setMemOvercommit(rs.getBigDecimal("mem_overcommit_rate").floatValue());
                    item.setCpuOvercommit(rs.getBigDecimal("cpu_overcommit_rate").floatValue());
                    item.setDomainId(rs.getLong("domain_id"));
                    item.setEnabled(rs.getBoolean("enabled"));
                    result.add(item);
                }
            } finally {
                rs.close();
                stmt.close();
            }
        }
        finally {
            conn.close();
        }
        return result;
    }

    @Override
    public void updateAccessTime(ExecutableContainer container) throws Exception {
        Connection conn = getConnection();
        PreparedStatement pstmt = conn
                .prepareStatement("UPDATE `executable_container` SET `accessed_at` = NOW() WHERE `id` = ?");

        try {
            pstmt.setLong(1, container.getId());
            pstmt.executeUpdate();
        } finally {
            pstmt.close();
            conn.close();
        }
    }

    @Override
    public boolean isK8sDomain(Domain domain) throws Exception {
        Connection conn = getConnection();
        PreparedStatement pstmt = conn.prepareStatement(
                "SELECT COUNT(*) n FROM `domain` a JOIN `k8s_cluster` b ON a.`id` = b.`domain_id` WHERE a.`id` = ?");
        try {
            pstmt.setLong(1, domain.getId());
            ResultSet rs = pstmt.executeQuery();
            try {
                if (rs.next()) return rs.getInt("n") > 0;
            } finally {
                rs.close();
                pstmt.close();
            }
        }
        finally {
            conn.close();
        }

        return false;
    }

    @Override
    public Iterable<DaskCluster> getDaskClusters(String userId) throws Exception {
        Connection conn = getConnection();
        PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM `dask_cluster` WHERE `user_id` = ? AND `status` <> ? ORDER BY `created_at` DESC");

        try {
            pstmt.setString(1, userId);
            pstmt.setString(2, DaskClusterStatus.DELETED);
            return getRegistryObjectsFromQuery(pstmt, DaskCluster.class);
        } finally {
            pstmt.close();
            conn.close();
        }
    }

    @Override
    public GenericVolume getGenericVolume(long id) throws Exception {
        Connection conn = getConnection();
        PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM `generic_volume` WHERE `id` = ?");

        try {
            pstmt.setLong(1, id);
            Iterator<GenericVolume> i = getRegistryObjectsFromQuery(pstmt, GenericVolume.class).iterator();
            if (i.hasNext())
                return i.next();
            else
                throw new Exception("K8s Volume not found");
        } finally {
            pstmt.close();
            conn.close();
        }
    }

    @Override
    public void linkDaskCluster(ExecutableContainer container, DaskCluster daskCluster) throws Exception {
        Connection conn = this.getConnection();
        PreparedStatement pstmt = conn.prepareStatement("INSERT `linked_dask_cluster` (container_id, cluster_id) VALUES (?, ?)");
        try {
            pstmt.setLong(1, container.getId());
            pstmt.setLong(2, daskCluster.getId());
            pstmt.executeUpdate();
        }
        finally {
            pstmt.close();
            conn.close();
        }
        container.setDaskClusterId(daskCluster.getId());
    }
}
