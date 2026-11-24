package org.sciserver.testservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Account cleanup uses existing APIs; its privileged credentials never leave this process. */
@Component
public class AccountProvider {
    private final Settings settings;
    private final ObjectMapper mapper;
    private final RemoteHttp http;
    private final RacmProfiles profiles;

    @org.springframework.beans.factory.annotation.Autowired
    public AccountProvider(Settings settings, ObjectMapper mapper, RacmProfiles profiles) {
        this(settings, mapper, new RemoteHttp(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5)).build()), profiles);
    }

    AccountProvider(Settings settings, ObjectMapper mapper, RemoteHttp http, RacmProfiles profiles) {
        this.settings = settings;
        this.mapper = mapper;
        this.http = http;
        this.profiles = profiles;
    }

    private ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Account provider unavailable; cleanup pending");
    }

    private RemoteHttp.Reply request(String url, String method, String token, Object body) {
        try {
            return http.call(URI.create(url), method, "X-Auth-Token", token,
                    body == null ? null : mapper.writeValueAsString(body));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw unavailable();
        } catch (Exception exception) {
            throw unavailable();
        }
    }

    private JsonNode json(RemoteHttp.Reply reply) {
        if (reply.status != 200) {
            throw unavailable();
        }
        try {
            return mapper.readTree(reply.body);
        } catch (Exception exception) {
            throw unavailable();
        }
    }

    private String login(String username, String password) {
        RemoteHttp.Reply reply = request(settings.baseUrl + "/login-portal/api/auth", "POST", null,
                Map.of("username", username, "password", password));
        if (reply.status != 200 || reply.body.isBlank() || reply.body.length() > 8192) {
            throw unavailable();
        }
        try {
            String token = reply.body.trim();
            return token.startsWith("\"") ? mapper.readTree(token).asText() : token;
        } catch (Exception exception) {
            throw unavailable();
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private JsonNode collection(String path, String field, String admin) {
        JsonNode values = json(request(settings.accountKeystoneUrl + path, "GET", admin, null)).path(field);
        if (!values.isArray() || values.size() > 1) {
            throw unavailable();
        }
        return values;
    }

    /** Refuse pre-existing identities before handing out normal registration credentials. */
    public void assertAbsent(String username) {
        String admin = login(settings.accountAdminUser, settings.accountAdminPassword);
        if (collection("/users?name=" + encode(username), "users", admin).size() != 0
                || collection("/projects?name=" + encode(username), "projects", admin).size() != 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reserved account identity is already in use");
        }
    }

    /** Prove control using the broker-issued password, then return exact provider identifiers. */
    public ObjectNode identify(String username, String password) {
        String admin = login(settings.accountAdminUser, settings.accountAdminPassword);
        JsonNode users = collection("/users?name=" + encode(username), "users", admin);
        if (users.size() == 0) {
            // An interrupted registration can create a project before creating its user.
            // Without changing the registration API, that project's ownership cannot be proved.
            if (collection("/projects?name=" + encode(username), "projects", admin).size() != 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Partial registration has no verifiable account; manual ownership review required");
            }
            return null;
        }
        JsonNode user = users.get(0);
        String token = login(username, password);
        JsonNode identity = json(request(settings.baseUrl + "/login-portal/api/validate/" + encode(token),
                "GET", null, null));
        if (!username.equals(identity.path("username").asText())
                || !user.path("id").asText().equals(identity.path("user_id").asText())
                || !username.equals(user.path("name").asText())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account ownership proof failed");
        }
        String projectId = user.path("default_project_id").asText();
        if (!projectId.matches("[A-Za-z0-9-]{1,128}")
                || !user.path("id").asText().matches("[A-Za-z0-9-]{1,128}")
                || user.path("domain_id").asText().isBlank()) {
            throw unavailable();
        }
        JsonNode project = json(request(settings.accountKeystoneUrl + "/projects/" + encode(projectId),
                "GET", admin, null)).path("project");
        if (projectId.isBlank() || !username.equals(project.path("name").asText())
                || !user.path("domain_id").asText().equals(project.path("domain_id").asText())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account project ownership mismatch");
        }
        return mapper.createObjectNode().put("userId", user.path("id").asText()).put("projectId", projectId)
                .put("domainId", user.path("domain_id").asText());
    }

    private JsonNode getOrMissing(String path, String field, String admin) {
        RemoteHttp.Reply reply = request(settings.accountKeystoneUrl + path, "GET", admin, null);
        if (reply.status == 404) {
            return mapper.missingNode();
        }
        JsonNode value = json(reply).path(field);
        if (!value.isObject()) {
            throw unavailable();
        }
        return value;
    }

    /** Only callers with a broker ownership record may select this identity. */
    public Map<String, Object> inspectRacm(String username, JsonNode identity) {
        return profiles.inspect(username, identity.path("userId").asText());
    }

    /** Report exact record presence without using a user session that could recreate a profile. */
    public Map<String, Object> state(String username, JsonNode identity) {
        String admin = login(settings.accountAdminUser, settings.accountAdminPassword);
        return Map.of("keystoneUserAbsent", getOrMissing("/users/" + encode(identity.path("userId").asText()),
                "user", admin).isMissingNode(),
                "keystoneProjectAbsent", getOrMissing("/projects/" + encode(identity.path("projectId").asText()),
                "project", admin).isMissingNode(), "racm", inspectRacm(username, identity));
    }

    /** Idempotent deletion uses only the exact IDs already persisted after ownership proof. */
    public void delete(String username, JsonNode identity) {
        if (!identity.path("userId").asText().matches("[A-Za-z0-9-]{1,128}")
                || !identity.path("projectId").asText().matches("[A-Za-z0-9-]{1,128}")
                || identity.path("domainId").asText().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid recorded account identity");
        }
        String admin = login(settings.accountAdminUser, settings.accountAdminPassword);
        String userPath = "/users/" + encode(identity.path("userId").asText());
        String projectPath = "/projects/" + encode(identity.path("projectId").asText());
        JsonNode user = getOrMissing(userPath, "user", admin);
        JsonNode project = getOrMissing(projectPath, "project", admin);
        if ((!user.isMissingNode() && (!username.equals(user.path("name").asText())
                || !identity.path("projectId").asText().equals(user.path("default_project_id").asText())
                || !identity.path("domainId").asText().equals(user.path("domain_id").asText())))
                || (!project.isMissingNode() && (!username.equals(project.path("name").asText())
                || !identity.path("domainId").asText().equals(project.path("domain_id").asText())))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Recorded account ownership changed");
        }
        // Do not delete the login account when its RACM dependents still require owner access.
        profiles.delete(username, identity.path("userId").asText());
        for (String path : new String[] {userPath, projectPath}) {
            RemoteHttp.Reply reply = request(settings.accountKeystoneUrl + path, "DELETE", admin, null);
            if (reply.status != 204 && reply.status != 404) {
                throw unavailable();
            }
        }
        if (!getOrMissing(userPath, "user", admin).isMissingNode()
                || !getOrMissing(projectPath, "project", admin).isMissingNode()) {
            throw unavailable();
        }
        // Retry-safe reconciliation for a profile recreated by an already in-flight request.
        profiles.delete(username, identity.path("userId").asText());
    }
}
