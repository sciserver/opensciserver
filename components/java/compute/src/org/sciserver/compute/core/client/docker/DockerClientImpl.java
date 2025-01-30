/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0. 
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.compute.core.client.docker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.sciserver.compute.core.client.HttpClientFactory;


public class DockerClientImpl implements DockerClient {

    private HttpClientFactory httpClientFactory;
    private URL baseURL;

    private static void ensureSuccessStatusCode(CloseableHttpResponse res) throws Exception {
        int code = res.getStatusLine().getStatusCode();
        if (code < 200 || code >= 300) {
            throw new Exception(res.getStatusLine().toString());
        }
    }

    public DockerClientImpl(URL baseURL) {
        this(baseURL, null, null);
    }

    public DockerClientImpl(URL baseURL, byte[] certData, byte[] keyData) {
        this.baseURL = baseURL;
        this.httpClientFactory = new HttpClientFactory(certData, keyData);
    }

    @Override
    public String createContainer(JsonNode containerDef) throws Exception {
        CloseableHttpClient httpClient = httpClientFactory.createHttpClient();
        try {
            URL url = new URL(baseURL, "containers/create");
            HttpPost request = new HttpPost(url.toURI());
            request.addHeader("Content-Type", "application/json");
            HttpEntity body = new ByteArrayEntity(containerDef.toString().getBytes("UTF-8"));
            request.setEntity(body);

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            CloseableHttpResponse response = httpClient.execute(request);
            HttpEntity responseEntity = response.getEntity();
            String responseAsString = null;

            try {
                ensureSuccessStatusCode(response);
                IOUtils.copy(responseEntity.getContent(), buffer);
                responseAsString = buffer.toString();
            } finally {
                EntityUtils.consumeQuietly(responseEntity);
                response.close();
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(responseAsString);

            return node.get("Id").asText();
        } finally {
            httpClient.close();
        }
    }

    @Override
    public void deleteContainer(String containerId) throws Exception {
        CloseableHttpClient httpClient = httpClientFactory.createHttpClient();
        try {
            URL url = new URL(baseURL, "containers/" + containerId + "?force=true");
            HttpDelete request = new HttpDelete(url.toURI());

            CloseableHttpResponse response = httpClient.execute(request);
            HttpEntity responseEntity = response.getEntity();
            try {
                ensureSuccessStatusCode(response);
            } finally {
                EntityUtils.consumeQuietly(responseEntity);
                response.close();
            }
        } finally {
            httpClient.close();
        }
    }

    @Override
    public JsonNode getContainerInfo(String containerId) throws Exception {
        CloseableHttpClient httpClient = httpClientFactory.createHttpClient();
        try {
            URL url = new URL(baseURL, "containers/" + containerId + "/json");
            HttpGet request = new HttpGet(url.toURI());

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            CloseableHttpResponse response = httpClient.execute(request);
            HttpEntity responseEntity = response.getEntity();
            String responseAsString = null;

            try {
                ensureSuccessStatusCode(response);
                IOUtils.copy(responseEntity.getContent(), buffer);
                responseAsString = buffer.toString();
            } finally {
                EntityUtils.consumeQuietly(responseEntity);
                response.close();
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(responseAsString);

            return node;
        } finally {
            httpClient.close();
        }
    }

    @Override
    public String getContainerLogs(String containerId, int logSource) throws Exception {
        CloseableHttpClient httpClient = httpClientFactory.createHttpClient();
        try {
            String query = "stdout=" + ((logSource & LogSource.STD_OUT) == 0 ? 0 : 1) + "&" + "stderr="
                    + ((logSource & LogSource.STD_ERR) == 0 ? 0 : 1);

            URL url = new URL(baseURL, "containers/" + containerId + "/logs?" + query);

            HttpGet request = new HttpGet(url.toURI());

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            CloseableHttpResponse response = httpClient.execute(request);
            HttpEntity responseEntity = response.getEntity();
            try {
                ensureSuccessStatusCode(response);
                IOUtils.copy(responseEntity.getContent(), buffer);
            } finally {
                EntityUtils.consumeQuietly(responseEntity);
                response.close();
            }

            return buffer.toString();
        } finally {
            httpClient.close();
        }
    }

    @Override
    public void startContainer(String containerId) throws Exception {
        CloseableHttpClient httpClient = httpClientFactory.createHttpClient();
        try {
            URL url = new URL(baseURL, "containers/" + containerId + "/start");
            HttpPost request = new HttpPost(url.toURI());

            CloseableHttpResponse response = httpClient.execute(request);
            HttpEntity responseEntity = response.getEntity();
            try {
                if (response.getStatusLine().getStatusCode() != 304) {
                    //Not modified
                    ensureSuccessStatusCode(response);
                }
            } finally {
                EntityUtils.consumeQuietly(responseEntity);
                response.close();
            }
        } finally {
            httpClient.close();
        }
    }

    @Override
    public void stopContainer(String containerId) throws Exception {
        CloseableHttpClient httpClient = httpClientFactory.createHttpClient();
        try {
            URL url = new URL(baseURL, "containers/" + containerId + "/stop");
            HttpPost request = new HttpPost(url.toURI());

            CloseableHttpResponse response = httpClient.execute(request);
            HttpEntity responseEntity = response.getEntity();
            try {
                ensureSuccessStatusCode(response);
            } finally {
                EntityUtils.consumeQuietly(responseEntity);
                response.close();
            }
        } finally {
            httpClient.close();
        }
    }

    @Override
    public void exec(String containerId, JsonNode execJson, JsonNode startJson) throws Exception {
        CloseableHttpClient httpClient = httpClientFactory.createHttpClient();
        try {
            URL url = new URL(baseURL, "containers/" + containerId + "/exec");
            HttpPost request = new HttpPost(url.toURI());
            request.addHeader("Content-Type", "application/json");
            HttpEntity body = new ByteArrayEntity(execJson.toString().getBytes("UTF-8"));
            request.setEntity(body);

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            CloseableHttpResponse response = httpClient.execute(request);
            HttpEntity responseEntity = response.getEntity();
            String responseAsString = null;

            try {
                ensureSuccessStatusCode(response);
                IOUtils.copy(responseEntity.getContent(), buffer);
                responseAsString = buffer.toString();
            } finally {
                EntityUtils.consumeQuietly(responseEntity);
                response.close();
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(responseAsString);

            String execId = node.get("Id").asText();

            url = new URL(baseURL, "exec/" + execId + "/start");
            request = new HttpPost(url.toURI());
            request.addHeader("Content-Type", "application/json");
            body = new ByteArrayEntity(startJson.toString().getBytes("UTF-8"));
            request.setEntity(body);

            response = httpClient.execute(request);

            try {
                ensureSuccessStatusCode(response);
            } finally {
                response.close();
            }
        } finally {
            httpClient.close();
        }
    }

    @Override
    public JsonNode listImages() throws Exception {
        CloseableHttpClient httpClient = httpClientFactory.createHttpClient();
        try {
            URL url = new URL(baseURL, "images/json");
            HttpGet request = new HttpGet(url.toURI());

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            CloseableHttpResponse response = httpClient.execute(request);
            HttpEntity responseEntity = response.getEntity();
            String responseAsString = null;

            try {
                ensureSuccessStatusCode(response);
                IOUtils.copy(responseEntity.getContent(), buffer);
                responseAsString = buffer.toString();
            } finally {
                EntityUtils.consumeQuietly(responseEntity);
                response.close();
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(responseAsString);

            return node;
        } finally {
            httpClient.close();
        }
    }

    @Override
    public JsonNode getInfo(int timeout) throws Exception {
        CloseableHttpClient httpClient = httpClientFactory.createHttpClient();
        try {
            URL url = new URL(baseURL, "info");
            HttpGet request = new HttpGet(url.toURI());
            RequestConfig config = RequestConfig.custom().setConnectTimeout(timeout).build();
            request.setConfig(config);

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            CloseableHttpResponse response = httpClient.execute(request);
            HttpEntity responseEntity = response.getEntity();
            String responseAsString = null;

            try {
                ensureSuccessStatusCode(response);
                IOUtils.copy(responseEntity.getContent(), buffer);
                responseAsString = buffer.toString();
            } finally {
                EntityUtils.consumeQuietly(responseEntity);
                response.close();
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(responseAsString);

            return node;
        } finally {
            httpClient.close();
        }
    }

}
