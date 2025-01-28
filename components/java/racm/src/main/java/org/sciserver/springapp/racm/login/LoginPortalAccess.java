package org.sciserver.springapp.racm.login;

import static org.sciserver.springapp.racm.auth.SciServerHeaderAuthenticationFilter.AUTH_HEADER;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.sciserver.springapp.racm.auth.SciServerHeaderAuthenticationFilter;
import org.sciserver.springapp.racm.config.LoginConfig;
import org.sciserver.springapp.racm.utils.RACMUtil;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class LoginPortalAccess {
    private static final String ACCEPT_HEADER = "Accept";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private final LoginConfig loginConfig;

    public LoginPortalAccess(LoginConfig loginConfig) {
        this.loginConfig = loginConfig;
    }

    public String getTrustId(String trustorToken, String trusteeName) throws IOException {
        Trustee trustee = new Trustee();
        trustee.setUserName(trusteeName);

        ObjectMapper mapper = RACMUtil.newObjectMapper();
        JsonNode json = mapper.valueToTree(trustee);

        String trustId = null;

        String url = loginConfig.getLoginPortalUrl() + "/api/trusts";
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.addHeader(CONTENT_TYPE_HEADER, MediaType.APPLICATION_JSON_VALUE);
            httpPost.addHeader(ACCEPT_HEADER, MediaType.APPLICATION_JSON_VALUE);
            httpPost.addHeader(AUTH_HEADER, trustorToken);
            HttpEntity body = new ByteArrayEntity(json.toString().getBytes(StandardCharsets.UTF_8));
            httpPost.setEntity(body);

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity responseEntity = response.getEntity();
            try {
                ensureSuccessStatusCode(response);
                IOUtils.copy(responseEntity.getContent(), buffer);
                trustId = buffer.toString();
            } finally {
                response.close();
            }
        }

        return trustId;
    }

    public String getTrustedToken(String user, String password, String trustId) throws IOException {
        Auth auth = new Auth();
        auth.setUser(user);
        auth.setPassword(password);
        auth.setTrust(trustId);

        ObjectMapper mapper = RACMUtil.newObjectMapper();
        JsonNode json = mapper.valueToTree(auth);

        String url = loginConfig.getLoginPortalUrl() + "/api/auth";
        String token = null;
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.addHeader(CONTENT_TYPE_HEADER, MediaType.APPLICATION_JSON_VALUE);
            httpPost.addHeader(ACCEPT_HEADER, MediaType.APPLICATION_JSON_VALUE);

            HttpEntity body = new ByteArrayEntity(json.toString().getBytes(StandardCharsets.UTF_8));
            httpPost.setEntity(body);

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity responseEntity = response.getEntity();
            try {
                ensureSuccessStatusCode(response);
                IOUtils.copy(responseEntity.getContent(), buffer);
                token = buffer.toString();
            } finally {
                response.close();
            }
        }

        return token;
    }

    private String getAdminToken() throws IOException {
        return getTrustedToken(loginConfig.getLoginAdmin().getUsername(), loginConfig.getLoginAdmin().getPassword(),
                null);
    }

    JsonNode validateUser(String token) throws IOException {
        String url = loginConfig.getLoginPortalUrl() + "/api/validate/" + token;
        String json = null;
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);
            httpGet.addHeader(CONTENT_TYPE_HEADER, MediaType.APPLICATION_JSON_VALUE);
            httpGet.addHeader(ACCEPT_HEADER, MediaType.APPLICATION_JSON_VALUE);
            httpGet.addHeader(AUTH_HEADER, token);

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            CloseableHttpResponse res = httpClient.execute(httpGet);

            try {
                ensureSuccessStatusCode(res);
                res.getEntity().getContent();
                IOUtils.copy(res.getEntity().getContent(), buffer);
                json = buffer.toString();
            } finally {
                res.close();
            }
        }

        ObjectMapper mapper = RACMUtil.newObjectMapper();
        return mapper.readTree(json);
    }

    public String userIdForUserName(String userName) {
        JsonNode info = userInfoForUserName(userName);
        if (info != null && info.has("user_id")) {
            return info.get("user_id").textValue();
        }
        return null;
    }

    public JsonNode userInfoForUserName(String userName) {
        String url = loginConfig.getLoginPortalUrl() + "/api/users?name=" + userName;
        String json = null;
        JsonNode e = null;
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);
            httpGet.addHeader(CONTENT_TYPE_HEADER, MediaType.APPLICATION_JSON_VALUE);
            httpGet.addHeader(ACCEPT_HEADER, MediaType.APPLICATION_JSON_VALUE);

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            CloseableHttpResponse res = httpClient.execute(httpGet);

            try {
                ensureSuccessStatusCode(res);
                res.getEntity().getContent();
                IOUtils.copy(res.getEntity().getContent(), buffer);
                json = buffer.toString();
                ObjectMapper mapper = RACMUtil.newObjectMapper();
                JsonNode jn = mapper.readTree(json);
                if (jn.isArray()) {
                    Iterator<JsonNode> it = jn.elements();
                    int size = 0;
                    while (it.hasNext()) {
                        size++;
                        if (size > 1)
                            throw new IllegalStateException("Multiple users in LoginPortal with name " + userName);
                        e = it.next();
                    }
                }
            } finally {
                res.close();
            }
        } catch (IOException exc) {
            throw new LoginPortalHTTPException("Unable to communicate with login portal", exc);
        }

        return e;
    }

    public User createUser(String userName, String password, String contactEmail) throws IOException {
        User user = new User();
        user.setUserName(userName);
        user.setPassword(password);
        user.setEmail(contactEmail);

        String adminToken = getAdminToken();

        ObjectMapper mapper = RACMUtil.newObjectMapper();
        JsonNode json = mapper.valueToTree(user);

        String url = loginConfig.getLoginPortalUrl() + "/api/users";
        String jUser = null;
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.addHeader(CONTENT_TYPE_HEADER, MediaType.APPLICATION_JSON_VALUE);
            httpPost.addHeader(ACCEPT_HEADER, MediaType.APPLICATION_JSON_VALUE);
            httpPost.addHeader(AUTH_HEADER, adminToken);

            HttpEntity body = new ByteArrayEntity(json.toString().getBytes(StandardCharsets.UTF_8));
            httpPost.setEntity(body);

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity responseEntity = response.getEntity();
            try {
                ensureSuccessStatusCode(response);
                IOUtils.copy(responseEntity.getContent(), buffer);
                jUser = buffer.toString();
                user = User.fromJson(mapper.readTree(jUser));
            } finally {
                response.close();
            }
        } catch (IOException e) {
            throw new LoginPortalHTTPException("Unable to communicate with login portal", e);
        }

        return user;

    }

    public void deleteUser(String userId) throws IOException {
        String adminToken = getAdminToken();
        String url = loginConfig.getLoginPortalUrl() + "/api/users/" + userId;
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            HttpDelete httpDelete = new HttpDelete(url);
            httpDelete.addHeader(AUTH_HEADER, adminToken);
            CloseableHttpResponse res = httpClient.execute(httpDelete);
            try {
                ensureSuccessStatusCode(res);
            } finally {
                res.close();
            }
        } finally {
            httpClient.close();
        }
    }

    private int ensureSuccessStatusCode(CloseableHttpResponse res) {
        int code = res.getStatusLine().getStatusCode();
        if (code < 200 || code >= 300) {
            throw new LoginPortalHTTPException(res.getStatusLine().toString());
        }
        return code;
    }

    public static class LoginPortalHTTPException extends RuntimeException {
        private static final long serialVersionUID = 8718409957903846172L;

        public LoginPortalHTTPException(String message) {
            super(message);
        }

        public LoginPortalHTTPException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
