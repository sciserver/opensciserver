/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0. 
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.compute.core.client.httpproxy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
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


public class HttpProxyClientImpl implements HttpProxyClient {
    private HttpClientFactory httpClientFactory;
    private URL baseURL;

    private static void ensureSuccessStatusCode(CloseableHttpResponse res) throws Exception {
        int code = res.getStatusLine().getStatusCode();
        if (code < 200 || code >= 300) {
            throw new Exception(res.getStatusLine().toString());
        }
    }

    public HttpProxyClientImpl(URL baseURL) {
        this(baseURL, null, null);
    }

    public HttpProxyClientImpl(URL baseURL, byte[] certData, byte[] keyData) {
        this.baseURL = baseURL;
        this.httpClientFactory = new HttpClientFactory(certData, keyData);
    }

    @Override
    public void setProxy(String route, String target) throws Exception {
        setProxy(route, target, null);
    }

    @Override
    public void setProxy(String route, String target, String userid) throws Exception {
        setProxy(route, target, userid, -1);
    }

    @Override
    public void setProxy(String route, String target, String userid, long containerid) throws Exception {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        String jsonUserId = factory.textNode(userid).toString();
        String jsonContainerId = factory.numberNode(containerid).toString();
        String jsonTarget = factory.textNode(target).toString();

        CloseableHttpClient httpClient = httpClientFactory.createHttpClient();
        try {
            URL url = new URL(baseURL, "api/routes" + route);
            HttpPost request = new HttpPost(url.toURI());

            String requestAsString = null;

            if (userid == null) {
                requestAsString = "" 
                    + "{" 
                    + "    `target`: $TARGET" 
                    + "}";
            } else {
                requestAsString = "" 
                    + "{"
                    + "    `target`: $TARGET,"
                    + "    `user_id`: $USER_ID,"
                    + "    `container_id`: $CONTAINER_ID"
                    + "}";
            }

            requestAsString = requestAsString
                    .replace(" ", "")
                    .replace('`', '"')
                    .replace("$USER_ID", jsonUserId)
                    .replace("$CONTAINER_ID", jsonContainerId)
                    .replace("$TARGET", jsonTarget);

            request.addHeader("Content-Type", "application/json");
            HttpEntity body = new ByteArrayEntity(requestAsString.getBytes("UTF-8"));
            request.setEntity(body);

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
    public void deleteProxy(String route) throws Exception {
        CloseableHttpClient httpClient = httpClientFactory.createHttpClient();
        try {
            URL url = new URL(baseURL, "api/routes" + route);
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
    public JsonNode getRoutes(Date inactiveSince, int timeout) throws Exception {
        CloseableHttpClient httpClient = httpClientFactory.createHttpClient();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

            URL url = new URL(baseURL, "api/routes?inactive_since=" + sdf.format(inactiveSince));
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
