/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under a modified Apache 2.0 license.
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.springapp.fileservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sciserver.authentication.client.User;
import org.sciserver.racm.storem.model.RegisteredFileServiceModel;


public class Config {
    private static final Logger LOG = LogManager.getLogger();

    private String racmEndpoint;

    private String serviceId;
    private String racmUUID;
    private String testFileNameEnding;
    private Set<PosixFilePermission> defaultDirPerms;
    private Set<PosixFilePermission> defaultFilePerms;

    private RegisteredFileServiceModel fileService;

    private ArrayNode defaultUserVolumes;
    private Properties properties;



    // TODO better system for detecting whether POSIX permissions can be used
    public static final int FS_WINDOWS = 1;
    private int fileSystem = 0;
    private ConcurrentMap<String, User> users = new ConcurrentHashMap<>();

    public Config() { }

    public boolean isWindows() {
        return fileSystem == FS_WINDOWS;
    }

    public void load() throws IOException, Exception {
        this.properties = new Properties();
        String configFilename = System.getenv("SCISERVER_PROPERTIES_FILE");
        InputStream input;
        if (configFilename != null) {
            LOG.info("Loading config from {}", configFilename);
            input = new FileInputStream(configFilename);
        } else {
            LOG.info("Loading config from built-in application.properties");
            input = Thread.currentThread().getContextClassLoader().getResourceAsStream("application.properties");
        }
        this.properties.load(input);

        this.fileService = this.getFileServiceDefinition(this.properties);
        this.racmEndpoint = this.properties.getProperty("RACM.endpoint");
        this.serviceId = this.properties.getProperty("File-service.serviceId");
        this.racmUUID = this.properties.getProperty("RACM.resourcecontext.uuid");
        this.testFileNameEnding = this.properties.getProperty("File-service.health.testFileNameEnding");
        this.defaultDirPerms = PosixFilePermissions.fromString(
            this.properties.getProperty("File-service.defaultDirPerms", "rwx------"));
        this.defaultFilePerms = PosixFilePermissions.fromString(
            this.properties.getProperty("File-service.defaultFilePerms", "rw-------"));

        ObjectMapper mapper = new ObjectMapper();
        this.defaultUserVolumes = (ArrayNode) mapper.readTree(
            this.properties.getProperty("File-service.default.uservolumes"));

        String os = System.getProperty("os.name");
        if (os.toLowerCase().contains("windows")) {
            this.fileSystem = FS_WINDOWS;
        }
   }


    public RegisteredFileServiceModel getFileServiceDefinition(Properties properties) throws Exception {
        String url = properties.getProperty("RACM.endpoint");
        if (!url.endsWith("/")) {
            url = url + "/";
        }
        url = url + "storem/fileservice/" + properties.getProperty("RACM.resourcecontext.uuid");
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);
            httpGet.addHeader("Content-Type", "application/json");
            httpGet.addHeader("Accept", "application/json");
            httpGet.addHeader("X-Service-Auth-ID", properties.getProperty("File-service.serviceId"));

            try (CloseableHttpResponse res = httpClient.execute(httpGet)) {
                ensureSuccessStatusCode(res);
                res.getEntity().getContent();
                ObjectMapper mapper = new ObjectMapper();
                JsonNode json = mapper.readTree(res.getEntity().getContent());
                if (json != null) {
                    try {
                        fileService = mapper.treeToValue(json, RegisteredFileServiceModel.class);
                    } catch (Exception e) {
                        throw new Exception(
                            "Unable to get FileService definition:" + e.getMessage() == null ? "" : e.getMessage());
                    }
                }
                return fileService;
            }
        }
    }

    private static void ensureSuccessStatusCode(CloseableHttpResponse res) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        int code = res.getStatusLine().getStatusCode();
        if (code < 200 || code >= 300) {
            String content = "";
            try {
                content = mapper.readTree(res.getEntity().getContent().toString()).toString();
            } catch (Exception ignored) { 

            }
            throw new Exception(res.getStatusLine().toString() + " " + content);
        }
    }


    public RegisteredFileServiceModel getFileService() {
        return fileService;
    }

    public String getRacmEndpoint() {
        return racmEndpoint;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getRacmUUID() {
        return racmUUID;
    }

    public ArrayNode getDefaultUserVolumes() {
        return defaultUserVolumes;
    }

    public String getTestFileNameEnding() {
        return testFileNameEnding;
    }

    public Set<PosixFilePermission> getDefaultDirPerms() {
        return defaultDirPerms;
    }

    public Set<PosixFilePermission> getDefaultFilePerms() {
        return defaultFilePerms;
    }

    public ConcurrentMap<String, User> getUsers() {
        return users;
    }

    public Properties getProperties() {
        return properties;
    }

}
