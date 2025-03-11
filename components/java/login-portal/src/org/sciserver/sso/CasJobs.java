/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under a modified Apache 2.0 license. 
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/
package org.sciserver.sso;

import java.io.ByteArrayOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class CasJobs {
    private static final Logger LOG = LogManager.getLogger(CasJobs.class);
    
    public static long getWsidByName(String name) throws Exception {
    	AppConfig config = AppConfig.getInstance();
        String token = config.getKeystoneService().getToken(
        		config.getAppSettings().getCjAdminUser(), 
        		config.getAppSettings().getCjAdminProject(), 
        		config.getAppSettings().getCjAdminPassword());
        
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            HttpGet httpGet = new HttpGet(config.getAppSettings().getCjBaseUrl() + "cjusers?name=" + name);
            httpGet.addHeader("X-Auth-Token", token);
            
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            CloseableHttpResponse response = httpClient.execute(httpGet);
            HttpEntity responseEntity = response.getEntity();
            String responseAsString = null;

            try {
                if (response.getStatusLine().getStatusCode() == 404) return -1;
                ensureSuccessStatusCode(response);
                IOUtils.copy(responseEntity.getContent(), buffer);
                responseAsString = buffer.toString();
            } finally {
                EntityUtils.consumeQuietly(responseEntity);
                response.close();
            }
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(responseAsString);
            long wsid = -1;
            try {
                wsid = node.at("/WebServicesId").asLong();
            } catch (Exception ex) {
                // not found
            }
            return wsid;
            
        } finally {
            httpClient.close();
        }
    }

    public static long getWsidByEmail(String email) throws Exception {
    	AppConfig config = AppConfig.getInstance();
        String token = config.getKeystoneService().getToken(
        		config.getAppSettings().getCjAdminUser(), 
        		config.getAppSettings().getCjAdminProject(), 
        		config.getAppSettings().getCjAdminPassword());

        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            HttpGet httpGet = new HttpGet(config.getAppSettings().getCjBaseUrl() + "cjusers?email=" + email);
            httpGet.addHeader("X-Auth-Token", token);
            
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            CloseableHttpResponse response = httpClient.execute(httpGet);
            HttpEntity responseEntity = response.getEntity();
            String responseAsString = null;
            try {
                if (response.getStatusLine().getStatusCode() == 404) return -1;
                ensureSuccessStatusCode(response);
                IOUtils.copy(responseEntity.getContent(), buffer);
                responseAsString = buffer.toString();
            } finally {
                EntityUtils.consumeQuietly(responseEntity);
                response.close();
            }
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(responseAsString);
            long wsid = -1;
            try {
                wsid = node.at("/WebServicesId").asLong();
            } catch (Exception ex ){
                // not found
            }
            return wsid;
            
        } finally {
            httpClient.close();
        }
    }

    public static void authenticate(long wsid, String password) throws Exception {
    	AppConfig config = AppConfig.getInstance();
        String token = config.getKeystoneService().getToken(
        		config.getAppSettings().getCjAdminUser(), 
        		config.getAppSettings().getCjAdminProject(), 
        		config.getAppSettings().getCjAdminPassword());

        JsonNodeFactory factory = JsonNodeFactory.instance;
        String jWsid = factory.numberNode(wsid).toString();
        String jPassword = factory.textNode(password).toString();
        
        String requestAsString = ""
                + "{" 
                + "    `wsid`: $WSID,"
                + "    `password`: $PASSWORD"
                + "}";
        
        requestAsString = requestAsString
                .replace(" ", "")
                .replace('`', '"')
                .replace("$WSID", jWsid)
                .replace("$PASSWORD", jPassword);
        
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            HttpPost httpPost = new HttpPost(config.getAppSettings().getCjBaseUrl() + "cjusers/authenticate");
            httpPost.addHeader("Content-Type", "application/json");
            httpPost.addHeader("X-Auth-Token", token);
            HttpEntity body = new ByteArrayEntity(requestAsString.getBytes("UTF-8"));
            httpPost.setEntity(body);

            CloseableHttpResponse response = httpClient.execute(httpPost);
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

    public static void link(long wsid, String keystoneUserId) throws Exception {
    	AppConfig config = AppConfig.getInstance();
        String token = config.getKeystoneService().getToken(
        		config.getAppSettings().getCjAdminUser(),
        		config.getAppSettings().getCjAdminProject(),
        		config.getAppSettings().getCjAdminPassword());

        String requestAsString = "\"" + keystoneUserId + "\"";
        
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            HttpPost httpPost = new HttpPost(config.getAppSettings().getCjBaseUrl() + "cjusers/" + wsid);
            httpPost.addHeader("Content-Type", "application/json");
            httpPost.addHeader("X-Auth-Token", token);
            HttpEntity body = new ByteArrayEntity(requestAsString.getBytes("UTF-8"));
            httpPost.setEntity(body);

            CloseableHttpResponse response = httpClient.execute(httpPost);
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

    public static void registerUser(String keystoneUserId) throws Exception {
    	AppConfig config = AppConfig.getInstance();
        String token = config.getKeystoneService().getToken(
        		config.getAppSettings().getCjAdminUser(), 
        		config.getAppSettings().getCjAdminProject(),
        		config.getAppSettings().getCjAdminPassword());
        
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            HttpPut httpPut = new HttpPut(config.getAppSettings().getCjBaseUrl() + "users/" + keystoneUserId);
            httpPut.addHeader("X-Auth-Token", token);

            CloseableHttpResponse response = httpClient.execute(httpPut);
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
    
	public static void deleteUser(String keystoneUserId) throws Exception {
		
		AppConfig config = AppConfig.getInstance();
        String token = config.getKeystoneService().getToken(
        		config.getAppSettings().getCjAdminUser(), 
        		config.getAppSettings().getCjAdminProject(), 
        		config.getAppSettings().getCjAdminPassword());
        
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            HttpDelete httpDelete = new HttpDelete(config.getAppSettings().getCjBaseUrl() + "users/" + keystoneUserId);
            httpDelete.addHeader("X-Auth-Token", token);

            CloseableHttpResponse response = httpClient.execute(httpDelete);
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
	
    private static void ensureSuccessStatusCode(CloseableHttpResponse response) throws Exception {
        int code = response.getStatusLine().getStatusCode();
        if (code < 200 || code >= 300) {
            throw new Exception(response.getStatusLine().toString());
        }
    }


}
