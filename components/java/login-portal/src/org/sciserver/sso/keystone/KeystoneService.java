/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under a modified Apache 2.0 license. 
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/
package org.sciserver.sso.keystone;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.passay.CharacterRule;
import org.passay.DictionaryRule;
import org.passay.DictionarySubstringRule;
import org.passay.EnglishCharacterData;
import org.passay.EnglishSequenceData;
import org.passay.IllegalSequenceRule;
import org.passay.LengthRule;
import org.passay.PasswordData;
import org.passay.PasswordValidator;
import org.passay.RuleResult;
import org.passay.WhitespaceRule;
import org.passay.dictionary.ArrayWordList;
import org.passay.dictionary.WordListDictionary;
import org.passay.dictionary.WordLists;
import org.passay.dictionary.sort.ArraysSort;
import org.sciserver.sso.AppSettings;
import org.sciserver.sso.CasJobs;
import org.sciserver.sso.Utility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class KeystoneService {
    private static final String KEYSTONE_SUBJECT_TOKEN = "X-Subject-Token";
    private static final String KEYSTONE_TOKEN_HEADER = "X-Auth-Token";
    private AppSettings appSettings;

    public KeystoneService(AppSettings appSettings) {
        this.appSettings = appSettings;
    }

    public String getAdminToken() throws Exception {
        String token = appSettings.getKeystoneAdminToken();
        if (token == null || token.isEmpty()) {
            token = getToken(appSettings.getKeystoneAdminUser(), appSettings.getKeystoneAdminProject(),
                    appSettings.getKeystoneAdminPassword());
        }
        return token;
    }

    private String getProjectId(String projectName) throws Exception {
        String result = null;
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(appSettings.getKeystoneUrl() + "v3/projects?name=" + projectName);
            httpGet.addHeader(KEYSTONE_TOKEN_HEADER, getAdminToken());

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            CloseableHttpResponse response = httpClient.execute(httpGet);
            HttpEntity responseEntity = response.getEntity();
            String responseAsString = null;
            try {
                Utility.ensureSuccessStatusCode(response);
                IOUtils.copy(responseEntity.getContent(), buffer);
                responseAsString = buffer.toString();
            } finally {
                EntityUtils.consumeQuietly(responseEntity);
                response.close();
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(responseAsString).at("/projects/0/id");
            if (!node.isMissingNode()) {
                result = node.asText();
            }

        }

        return result;
    }

    public Iterable<User> getUsers(String userName) throws Exception {
        if (userName == null) {
            return Collections.emptyList();
        }

        ArrayList<User> result = new ArrayList<>();
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String path = "v3/users?name=" + userName;

            HttpGet httpGet = new HttpGet(appSettings.getKeystoneUrl() + path);
            httpGet.addHeader(KEYSTONE_TOKEN_HEADER, getAdminToken());

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            CloseableHttpResponse response = httpClient.execute(httpGet);
            HttpEntity responseEntity = response.getEntity();
            String responseAsString = null;
            try {
                Utility.ensureSuccessStatusCode(response);
                IOUtils.copy(responseEntity.getContent(), buffer);
                responseAsString = buffer.toString();
            } finally {
                EntityUtils.consumeQuietly(responseEntity);
                response.close();
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(responseAsString).at("/users");

            if (!node.isMissingNode()) {
                for (JsonNode userNode : node) {
                    User user = new User();
                    user.password = null;
                    user.userId = userNode.at("/id").asText();
                    user.userName = userNode.at("/name").asText();
                    user.email = userNode.at("/email").asText();
                    result.add(user);
                }
            }
        }

        return result;
    }

    public List<User> getUsersFromEmail(String email) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String path = "v3/users";

            HttpGet httpGet = new HttpGet(appSettings.getKeystoneUrl() + path);
            httpGet.addHeader(KEYSTONE_TOKEN_HEADER, getAdminToken());

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            CloseableHttpResponse response = httpClient.execute(httpGet);
            HttpEntity responseEntity = response.getEntity();
            String responseAsString = null;
            try {
                Utility.ensureSuccessStatusCode(response);
                IOUtils.copy(responseEntity.getContent(), buffer);
                responseAsString = buffer.toString();
            } finally {
                EntityUtils.consumeQuietly(responseEntity);
                response.close();
            }

            ArrayList<User> result = new ArrayList<>();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode users = mapper.readTree(responseAsString).at("/users");
            if (!users.isMissingNode()) {
                for (JsonNode userNode : users) {
                    if (email.equals(userNode.at("/email").asText())) {
                        User u = new User();
                        u.password = null;
                        u.userId = userNode.at("/id").asText();
                        u.userName = userNode.at("/name").asText();
                        u.email = userNode.at("/email").asText();
                        result.add(u);
                    }
                }
            }
            return result;

        }
    }

    public User getUserById(String userId) throws Exception {
        User result = null;
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String path = "v3/users/" + userId;

            HttpGet httpGet = new HttpGet(appSettings.getKeystoneUrl() + path);
            httpGet.addHeader(KEYSTONE_TOKEN_HEADER, getAdminToken());

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            CloseableHttpResponse response = httpClient.execute(httpGet);
            HttpEntity responseEntity = response.getEntity();
            String responseAsString = null;
            try {
                Utility.ensureSuccessStatusCode(response);
                IOUtils.copy(responseEntity.getContent(), buffer);
                responseAsString = buffer.toString();
            } finally {
                EntityUtils.consumeQuietly(responseEntity);
                response.close();
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(responseAsString).at("/user");

            if (!node.isMissingNode()) {
                User user = new User();
                user.password = null;
                user.userId = node.at("/id").asText();
                user.userName = node.at("/name").asText();
                user.email = node.at("/email").asText();
                result = user;
            }

        }

        return result;
    }

    public String getUserId(String userName) throws Exception {
        String result = null;
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(appSettings.getKeystoneUrl() + "v3/users?name=" + userName);
            httpGet.addHeader(KEYSTONE_TOKEN_HEADER, getAdminToken());

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            CloseableHttpResponse response = httpClient.execute(httpGet);
            HttpEntity responseEntity = response.getEntity();
            String responseAsString = null;
            try {
                Utility.ensureSuccessStatusCode(response);
                IOUtils.copy(responseEntity.getContent(), buffer);
                responseAsString = buffer.toString();
            } finally {
                EntityUtils.consumeQuietly(responseEntity);
                response.close();
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(responseAsString).at("/users/0/id");

            if (!node.isMissingNode()) {
                result = node.asText();
            }

        }

        return result;
    }

    public Optional<String> tryGettingUserId(String userName) {
        String output = null;
        try {
            output = getUserId(userName);
        } catch (Exception ignored) {
            // We assume this exception is because of a 404. It may be wrong, but as long as getUserId only throws
            // generic exceptions, who knows.
        }
        return Optional.ofNullable(output);
    }

    public String getTrustId(String trustorToken, String trusteeName) throws Exception {
        String result = null;
        JsonNode trustorJson = validateToken(trustorToken);
        String trustorUserId = trustorJson.at("/token/user/id").asText();
        String projectId = trustorJson.at("/token/project/id").asText();
        String trusteeUserId = getUserId(trusteeName);
        boolean isOriginalToken = trustorJson.at("/token/OS-TRUST:trust").isMissingNode();

        JsonNodeFactory factory = JsonNodeFactory.instance;
        String jsonTrustorUserId = factory.textNode(trustorUserId).toString();
        String jsonTrusteeUserId = factory.textNode(trusteeUserId).toString();
        String jsonProjectId = factory.textNode(projectId).toString();

        String requestAsString = ""
                + "{"
                + "    `trust`: {"
                + "        `impersonation`: true,"
                + "        `allow_redelegation`: true,"
                // redelegation_count for trusts created from trusted (i.e. not original) tokens is assigned by
                // Keystone automatically based on the original redelegation_count value, though it can be set 
                // manually to something lower. Supplying a value higher than allowed in this case will lead to a
                // 403 error, so it's better to simply omit it.
                +          (isOriginalToken ? "`redelegation_count`: 2," : "")
                + "        `project_id`: $PROJECT_ID,"
                + "        `roles`: ["
                + "            {"
                + "                `name`: `user`"
                + "            }"
                + "         ],"
                + "        `trustee_user_id`: $TRUSTEE_USER_ID,"
                + "        `trustor_user_id`: $TRUSTOR_USER_ID"
                + "    }"
                + "}";

        requestAsString = requestAsString.replace(" ", "")
                .replace('`', '"')
                .replace("$PROJECT_ID", jsonProjectId)
                .replace("$TRUSTEE_USER_ID", jsonTrusteeUserId)
                .replace("$TRUSTOR_USER_ID", jsonTrustorUserId);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(appSettings.getKeystoneUrl() + "v3/OS-TRUST/trusts");
            HttpEntity body = new ByteArrayEntity(requestAsString.getBytes("UTF-8"));
            httpPost.addHeader(KEYSTONE_TOKEN_HEADER, trustorToken);
            httpPost.addHeader("Content-Type", "application/json");
            httpPost.setEntity(body);

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity responseEntity = response.getEntity();
            String responseAsString = null;
            try {
                Utility.ensureSuccessStatusCode(response);
                IOUtils.copy(responseEntity.getContent(), buffer);
                responseAsString = buffer.toString();
            } finally {
                EntityUtils.consumeQuietly(responseEntity);
                response.close();
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(responseAsString).at("/trust/id");

            if (!node.isMissingNode()) {
                result = node.asText();
            }

        }

        return result;
    }

    public String getTrustedToken(String user, String password, String trustId) throws Exception {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        String jsonUser = factory.textNode(user).toString();
        String jsonPassword = factory.textNode(password).toString();
        String jsonTrustId = factory.textNode(trustId).toString();
        String jsonDefaultDomainId = factory.textNode(appSettings.getDefaultDomainId()).toString();

        String requestAsString = ""
                + "{"
                + "    `auth`: {"
                + "        `identity`: {"
                + "            `methods`: ["
                + "                `password`"
                + "            ],"
                + "            `password`: {"
                + "                `user`: {"
                + "                    `domain`: {"
                + "                        `id`: $DEFAULT_DOMAIN_ID"
                + "                    },"
                + "                    `name`: $USER,"
                + "                    `password`: $PASSWORD"
                + "                }"
                + "            }"
                + "        },"
                + "        `scope`: {"
                + "            `OS-TRUST:trust`: {"
                + "                    `id`: $TRUST_ID"
                + "            }"
                + "        }"
                + "    }"
                + "}";

        requestAsString = requestAsString.replace(" ", "")
                .replace('`', '"')
                .replace("$USER", jsonUser)
                .replace("$PASSWORD", jsonPassword)
                .replace("$TRUST_ID", jsonTrustId)
                .replace("$DEFAULT_DOMAIN_ID", jsonDefaultDomainId);

        String token = null;

        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            HttpPost httpPost = new HttpPost(appSettings.getKeystoneUrl() + "v3/auth/tokens");
            HttpEntity body = new ByteArrayEntity(requestAsString.getBytes("UTF-8"));
            httpPost.addHeader(KEYSTONE_TOKEN_HEADER, getAdminToken());
            httpPost.addHeader("Content-Type", "application/json");
            httpPost.setEntity(body);

            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity responseEntity = response.getEntity();
            try {
                Utility.ensureSuccessStatusCode(response);
                token = response.getFirstHeader(KEYSTONE_SUBJECT_TOKEN).getValue();
            } finally {
                EntityUtils.consumeQuietly(responseEntity);
                response.close();
            }

        } finally {
            httpClient.close();
        }

        return token;
    }

    public String getToken(String user, String project, String password) throws Exception {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        String jsonUser = factory.textNode(user).toString();
        String jsonPassword = factory.textNode(password).toString();
        String jsonProject = factory.textNode(project).toString();
        String jsonDefaultDomainId = factory.textNode(appSettings.getDefaultDomainId()).toString();

        String requestAsString = ""
                + "{"
                + "    `auth`: {"
                + "        `identity`: {"
                + "            `methods`: ["
                + "                `password`"
                + "            ],"
                + "            `password`: {"
                + "                `user`: {"
                + "                    `domain`: {"
                + "                        `id`: $DEFAULT_DOMAIN_ID"
                + "                    },"
                + "                    `name`: $USER,"
                + "                    `password`: $PASSWORD"
                + "                }"
                + "            }"
                + "        },"
                + "        `scope`: {"
                + "            `project`: {"
                + "                `domain`: {"
                + "                    `id`: $DEFAULT_DOMAIN_ID"
                + "                },"
                + "                `name`: $PROJECT"
                + "            }"
                + "        }"
                + "    }"
                + "}";

        requestAsString = requestAsString.replace(" ", "")
                .replace('`', '"')
                .replace("$USER", jsonUser)
                .replace("$PASSWORD", jsonPassword)
                .replace("$PROJECT", jsonProject)
                .replace("$DEFAULT_DOMAIN_ID", jsonDefaultDomainId);

        String token = null;

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(appSettings.getKeystoneUrl() + "v3/auth/tokens");
            HttpEntity body = new ByteArrayEntity(requestAsString.getBytes("UTF-8"));
            httpPost.addHeader("Content-Type", "application/json");
            httpPost.setEntity(body);

            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity responseEntity = response.getEntity();
            try {
                Utility.ensureSuccessStatusCode(response);
                token = response.getFirstHeader(KEYSTONE_SUBJECT_TOKEN).getValue();
            } finally {
                EntityUtils.consumeQuietly(responseEntity);
                response.close();
            }

        }

        return token;
    }

    public void revokeToken(String token) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpDelete httpDelete = new HttpDelete(appSettings.getKeystoneUrl() + "v3/auth/tokens");
            httpDelete.addHeader(KEYSTONE_TOKEN_HEADER, getAdminToken());
            httpDelete.addHeader(KEYSTONE_SUBJECT_TOKEN, token);

            CloseableHttpResponse response = httpClient.execute(httpDelete);
            HttpEntity responseEntity = response.getEntity();
            try {
                Utility.ensureSuccessStatusCode(response);
            } finally {
                EntityUtils.consumeQuietly(responseEntity);
                response.close();
            }
        }
    }

    public JsonNode validateToken(String token) throws Exception {
        String responseAsString;

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(appSettings.getKeystoneUrl() + "v3/auth/tokens");
            httpGet.addHeader(KEYSTONE_TOKEN_HEADER, getAdminToken());
            httpGet.addHeader(KEYSTONE_SUBJECT_TOKEN, token);

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            CloseableHttpResponse response = httpClient.execute(httpGet);
            HttpEntity responseEntity = response.getEntity();
            try {
                Utility.ensureSuccessStatusCode(response);
                IOUtils.copy(responseEntity.getContent(), buffer);
                responseAsString = buffer.toString();
            } finally {
                EntityUtils.consumeQuietly(responseEntity);
                response.close();
            }

        }

        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(responseAsString);
    }

    public JsonNode validateToken20(String token) throws Exception {
        String responseAsString;

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(appSettings.getKeystoneUrl() + "v2.0/tokens/" + token);
            httpGet.addHeader(KEYSTONE_TOKEN_HEADER, getAdminToken());

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            CloseableHttpResponse response = httpClient.execute(httpGet);
            HttpEntity responseEntity = response.getEntity();
            try {
                Utility.ensureSuccessStatusCode(response);
                IOUtils.copy(responseEntity.getContent(), buffer);
                responseAsString = buffer.toString();
            } finally {
                EntityUtils.consumeQuietly(responseEntity);
                response.close();
            }

        }

        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(responseAsString);
    }

    public JsonNode getInfo() throws Exception {
        String responseAsString;
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(appSettings.getKeystoneUrl());

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            CloseableHttpResponse response = httpClient.execute(httpGet);
            HttpEntity responseEntity = response.getEntity();
            try {
                if (response.getStatusLine().getStatusCode() != 300) {
                    Utility.ensureSuccessStatusCode(response);
                }
                IOUtils.copy(responseEntity.getContent(), buffer);
                responseAsString = buffer.toString();
            } finally {
                EntityUtils.consumeQuietly(responseEntity);
                response.close();
            }
        }
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(responseAsString);
    }

    public void checkToken(String token) throws Exception {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            HttpHead httpHead = new HttpHead(appSettings.getKeystoneUrl() + "v3/auth/tokens");
            httpHead.addHeader(KEYSTONE_TOKEN_HEADER, getAdminToken());
            httpHead.addHeader(KEYSTONE_SUBJECT_TOKEN, token);

            CloseableHttpResponse response = httpClient.execute(httpHead);
            HttpEntity responseEntity = response.getEntity();
            try {
                Utility.ensureSuccessStatusCode(response);
            } finally {
                EntityUtils.consumeQuietly(responseEntity);
                response.close();
            }

        } finally {
            httpClient.close();
        }
    }

    private String createUser(String name, String email, String password, String defaultProjectId) throws Exception {
        String result = null;
        JsonNodeFactory factory = JsonNodeFactory.instance;
        String jsonUser = factory.textNode(name).toString();
        String jsonEmail = factory.textNode(email).toString();
        String jsonPassword = factory.textNode(password).toString();
        String jsonProjectId = factory.textNode(defaultProjectId).toString();
        String jsonDefaultDomainId = factory.textNode(appSettings.getDefaultDomainId()).toString();

        String requestAsString = ""
                + "{"
                + "    `user`: {"
                + "        `name`: $USER,"
                + "        `email`: $EMAIL,"
                + "        `password`: $PASSWORD,"
                + "        `domain_id`: $DEFAULT_DOMAIN_ID,"
                + "        `default_project_id`: $PROJECT_ID"
                + "    }"
                + "}";

        requestAsString = requestAsString.replace(" ", "")
                .replace('`', '"')
                .replace("$USER", jsonUser)
                .replace("$EMAIL", jsonEmail)
                .replace("$PASSWORD", jsonPassword)
                .replace("$PROJECT_ID", jsonProjectId)
                .replace("$DEFAULT_DOMAIN_ID", jsonDefaultDomainId);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(appSettings.getKeystoneUrl() + "v3/users");
            httpPost.addHeader("Content-Type", "application/json");
            httpPost.addHeader(KEYSTONE_TOKEN_HEADER, getAdminToken());
            HttpEntity body = new ByteArrayEntity(requestAsString.getBytes("UTF-8"));
            httpPost.setEntity(body);

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity responseEntity = response.getEntity();
            String responseAsString = null;
            try {
                Utility.ensureSuccessStatusCode(response);
                IOUtils.copy(responseEntity.getContent(), buffer);
                responseAsString = buffer.toString();
            } finally {
                EntityUtils.consumeQuietly(responseEntity);
                response.close();
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(responseAsString).at("/user/id");

            if (!node.isMissingNode()) {
                result = node.asText();
            }

        }

        return result;
    }

    public void setUserEnabled(String userId, boolean value) throws Exception {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        String jsonEnabled = factory.booleanNode(value).toString();
        
        String requestAsString = ""
                + "{"
                + "    `user`: {"
                + "        `enabled`: $ENABLED"
                + "    }"
                + "}";

        requestAsString = requestAsString.replace(" ", "")
                .replace('`', '"')
                .replace("$ENABLED", jsonEnabled);
        
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPatch httpPatch = new HttpPatch(appSettings.getKeystoneUrl() + "v3/users/" + userId);
            httpPatch.addHeader("Content-Type", "application/json");
            httpPatch.addHeader(KEYSTONE_TOKEN_HEADER, getAdminToken());
            HttpEntity body = new ByteArrayEntity(requestAsString.getBytes("UTF-8"));
            httpPatch.setEntity(body);

            CloseableHttpResponse response = httpClient.execute(httpPatch);
            HttpEntity responseEntity = response.getEntity();
            try {
                Utility.ensureSuccessStatusCode(response);
            } finally {
                EntityUtils.consumeQuietly(responseEntity);
                response.close();
            }
        }
    }
    
    private String createProject(String name) throws Exception {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        String jsonProject = factory.textNode(name).toString();
        String jsonDefaultDomainId = factory.textNode(appSettings.getDefaultDomainId()).toString();

        String requestAsString = ""
                + "{"
                + "    `project`: {"
                + "        `name`: $PROJECT,"
                + "        `domain_id`: $DEFAULT_DOMAIN_ID"
                + "    }"
                + "}";

        requestAsString = requestAsString.replace(" ", "")
                .replace('`', '"')
                .replace("$PROJECT", jsonProject)
                .replace("$DEFAULT_DOMAIN_ID", jsonDefaultDomainId);

        String projectId = null;
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(appSettings.getKeystoneUrl() + "v3/projects");
            httpPost.addHeader("Content-Type", "application/json");
            httpPost.addHeader(KEYSTONE_TOKEN_HEADER, getAdminToken());
            HttpEntity body = new ByteArrayEntity(requestAsString.getBytes("UTF-8"));
            httpPost.setEntity(body);

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity responseEntity = response.getEntity();
            String responseAsString = null;
            try {
                Utility.ensureSuccessStatusCode(response);
                response.getEntity().getContent();
                IOUtils.copy(response.getEntity().getContent(), buffer);
                responseAsString = buffer.toString();
            } finally {
                EntityUtils.consumeQuietly(responseEntity);
                response.close();
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(responseAsString);
            projectId = node.get("project").get("id").asText();

        }

        return projectId;
    }

    private String getRoleId(String name) throws Exception {

        String roleId = null;
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(appSettings.getKeystoneUrl() + "v3/roles?name=" + name);
            httpGet.addHeader(KEYSTONE_TOKEN_HEADER, getAdminToken());

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            CloseableHttpResponse response = httpClient.execute(httpGet);
            HttpEntity responseEntity = response.getEntity();
            String responseAsString = null;
            try {
                Utility.ensureSuccessStatusCode(response);
                IOUtils.copy(responseEntity.getContent(), buffer);
                responseAsString = buffer.toString();
            } finally {
                EntityUtils.consumeQuietly(responseEntity);
                response.close();
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(responseAsString);
            roleId = node.get("roles").get(0).get("id").asText();

        }

        return roleId;
    }

    private String grant(String projectId, String userId, String roleId) throws Exception {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            HttpPut httpPut = new HttpPut(appSettings.getKeystoneUrl()
                    + "v3/projects/" + projectId
                    + "/users/" + userId
                    + "/roles/" + roleId);
            httpPut.addHeader(KEYSTONE_TOKEN_HEADER, getAdminToken());

            CloseableHttpResponse response = httpClient.execute(httpPut);
            HttpEntity responseEntity = response.getEntity();
            try {
                Utility.ensureSuccessStatusCode(response);
            } finally {
                EntityUtils.consumeQuietly(responseEntity);
                response.close();
            }
        } finally {
            httpClient.close();
        }

        return roleId;
    }

    public JsonNode getUser(String userId) throws Exception {
        String responseAsString;
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(appSettings.getKeystoneUrl() + "v3/users/" + userId);
            httpGet.addHeader(KEYSTONE_TOKEN_HEADER, getAdminToken());

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            CloseableHttpResponse response = httpClient.execute(httpGet);
            HttpEntity responseEntity = response.getEntity();
            try {
                Utility.ensureSuccessStatusCode(response);
                IOUtils.copy(responseEntity.getContent(), buffer);
                responseAsString = buffer.toString();
            } finally {
                EntityUtils.consumeQuietly(responseEntity);
                response.close();
            }

        }

        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(responseAsString);
    }

    public static class NotAuthorizedException extends Exception {
        private static final long serialVersionUID = -7710964799393091084L;

        public NotAuthorizedException() {
            super("HTTP/1.1 401 Unauthorized");
        }
    }

    public String registerUser(String name, String email, String password) throws Exception {
        String projectId = createProject(name);
        String userId = createUser(name, email, password, projectId);
        String roleId = getRoleId("user");
        grant(projectId, userId, roleId);

        return userId;
    }

    public String unregisterUser(String userId) throws Exception {
        User user = getUserById(userId);
        String projectId = getProjectId(user.getUserName());
        deleteUser(userId);
        deleteProject(projectId);
        return userId;
    }

    private void deleteUser(String userId) throws Exception {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            HttpDelete httpDelete = new HttpDelete(appSettings.getKeystoneUrl() + "v3/users/" + userId);
            httpDelete.addHeader(KEYSTONE_TOKEN_HEADER, getAdminToken());

            CloseableHttpResponse response = httpClient.execute(httpDelete);
            HttpEntity responseEntity = response.getEntity();
            try {
                Utility.ensureSuccessStatusCode(response);
            } finally {
                EntityUtils.consumeQuietly(responseEntity);
                response.close();
            }
        } finally {
            httpClient.close();
        }
    }

    private void deleteProject(String projectId) throws Exception {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            HttpDelete httpDelete = new HttpDelete(appSettings.getKeystoneUrl() + "v3/projects/" + projectId);
            httpDelete.addHeader(KEYSTONE_TOKEN_HEADER, getAdminToken());

            CloseableHttpResponse response = httpClient.execute(httpDelete);
            HttpEntity responseEntity = response.getEntity();
            try {
                Utility.ensureSuccessStatusCode(response);
            } finally {
                EntityUtils.consumeQuietly(responseEntity);
                response.close();
            }
        } finally {
            httpClient.close();
        }
    }

    // http://www.passay.org/reference/
    public void validatePassword(String userName, String password) throws InvalidPasswordException, IOException {
        // Prevent use of commonly used or known cracked passwords, plus 'sciserver'.  This is the top 10k passwords
        // listed in Troy Hunt's Have I Been Pwned (https://haveibeenpwned.com) data set.  Downloaded from
        // https://www.ncsc.gov.uk/static-assets/documents/PwnedPasswordsTop100k.txt
        String pwnedPasswordPath = 
                getClass().getClassLoader().getResource("/PwnedPasswordsTop10k-sciserver.txt").getPath();
        File pwnedPasswordFile = new File(URLDecoder.decode(pwnedPasswordPath, "UTF-8"));
        DictionaryRule dictionaryRule = new DictionaryRule(
            new WordListDictionary(WordLists.createFromReader(
                new FileReader[] { new FileReader(pwnedPasswordFile) },
                false,  // True for case sensitivity, false otherwise
                new ArraysSort()  // Dictionaries must be sorted
            ))
        );

        // Prevent use of 'sciserver' or the submitted user name as a substring of the password
        DictionarySubstringRule dictionarySubstringRule = new DictionarySubstringRule(
            new WordListDictionary(new ArrayWordList(
                new String[] { "sciserver", userName }, false, new ArraysSort()
            ))
        );

        PasswordValidator validator = new PasswordValidator(Arrays.asList(
            // length between 8 and 128 characters
            new LengthRule(8, 128),
            // at least one upper-case character
            new CharacterRule(EnglishCharacterData.UpperCase, 1),
            // at least one lower-case character
            new CharacterRule(EnglishCharacterData.LowerCase, 1),
            // at least one digit character
            new CharacterRule(EnglishCharacterData.Digit, 1),
            // at least one symbol (special character)
            new CharacterRule(EnglishCharacterData.Special, 1),
            // define some illegal sequences that will fail when >= 4 chars long
            // alphabetical is of the form 'abcd', numerical is '3456', qwerty is 'asdf'
            // the false parameter indicates that wrapped sequences are allowed; e.g. 'xyzabc'
            new IllegalSequenceRule(EnglishSequenceData.Alphabetical, 4, false),
            new IllegalSequenceRule(EnglishSequenceData.Numerical, 4, false),
            new IllegalSequenceRule(EnglishSequenceData.USQwerty, 4, false),
            // no whitespace
            new WhitespaceRule(),
            dictionaryRule, dictionarySubstringRule
        ));

        RuleResult result = validator.validate(new PasswordData(password));
        if (!result.isValid()) {
            throw new InvalidPasswordException(String.join("  ", validator.getMessages(result)));
        }
    }

    public void changePassword(String userId, String password) throws Exception {
        validatePassword(userId, password);

        JsonNodeFactory factory = JsonNodeFactory.instance;
        String jsonPassword = factory.textNode(password).toString();

        String requestAsString = ""
                + "{"
                + "    `user`: {"
                + "        `password`: $PASSWORD"
                + "    }"
                + "}";

        requestAsString = requestAsString.replace(" ", "")
                .replace('`', '"')
                .replace("$PASSWORD", jsonPassword);

        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            HttpPatch httpPatch = new HttpPatch(appSettings.getKeystoneUrl() + "v3/users/" + userId);
            httpPatch.addHeader(KEYSTONE_TOKEN_HEADER, getAdminToken());
            HttpEntity body = new ByteArrayEntity(requestAsString.getBytes("UTF-8"));
            httpPatch.setEntity(body);

            CloseableHttpResponse response = httpClient.execute(httpPatch);
            HttpEntity responseEntity = response.getEntity();
            try {
                Utility.ensureSuccessStatusCode(response);
            } finally {
                EntityUtils.consumeQuietly(responseEntity);
                response.close();
            }
        } finally {
            httpClient.close();
        }
    }

    public boolean isAdmin(String token) throws Exception {
        JsonNode node = validateToken(token);
        boolean isAdmin = false;
        JsonNode roles = node.at("/token/roles");
        for (JsonNode role : roles) {
            if ("admin".equals(role.at("/name").asText())) {
                isAdmin = true;
                break;
            }
        }
        return isAdmin;
    }

    public String registerUserCj(String userName, String email, String password) throws Exception {
        String keystoneUserId;

        // Check if Keystone user exists
        String existingUser = getUserId(userName);
        if (existingUser != null && !existingUser.isEmpty()) {
            throw new Exception("User name already exists");
        }

        // Check if CasJobs user exists
        long wsid = CasJobs.getWsidByName(userName);

        if (wsid != -1) {
            // Check if password matches
            CasJobs.authenticate(wsid, password);

            // Create a new SciServer user
            keystoneUserId = registerUser(userName, email, password);

            // Link the two accounts
            CasJobs.link(wsid, keystoneUserId);
        } else {
            // Check if CasJobs user with the same email exists
            wsid = CasJobs.getWsidByEmail(email);
            if (wsid != -1) {
                throw new Exception("CasJobs user with the same email already exists");
            } else {
                // Create a new SciServer and CasJobs user
                keystoneUserId = registerUser(userName, email, password);

                CasJobs.registerUser(keystoneUserId);
            }
        }

        return keystoneUserId;
    }
}
