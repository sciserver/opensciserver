package org.sciserver.testservice;

import java.util.List;
import org.sciserver.clientutils.SciServerClientException;
import org.sciserver.racm.client.RACMClient;
import org.sciserver.racm.ugm.model.PersonalUserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Authenticate and check accepted administrator membership using the shared RACM client. */
@Component
public class AdminAccess {
    private final RACMClient client;

    /** Configure the shared RACM client with bounded timeouts and redirects disabled. */
    @Autowired
    public AdminAccess(Settings settings) {
        this(new ServiceRacmClient(settings.racmUrl + "/"));
    }

    AdminAccess(RACMClient client) {
        this.client = client;
    }

    /** Return the RACM-verified administrator username, or deny access. */
    public String require(String token) {
        if (token == null || token.isBlank() || token.length() > 8192 || token.contains("\n") || token.contains("\r")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A valid X-Auth-Token is required");
        }
        try {
            List<String> groups = client.getGroupsUserIsMemberOf(token);
            if (groups == null) {
                throw new IllegalStateException("Missing RACM memberships");
            }
            if (!groups.contains("admin")) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator membership required");
            }
            PersonalUserInfo user = client.getUser(token);
            if (user == null || user.getUsername() == null || user.getUsername().isBlank()) {
                throw new IllegalStateException("Missing RACM identity");
            }
            return user.getUsername();
        } catch (SciServerClientException exception) {
            int status = exception.httpCode();
            if (status == 401 || status == 403) {
                throw new ResponseStatusException(HttpStatus.valueOf(status), "RACM denied access");
            }
            throw unavailable();
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            // The shared membership decoder also reports malformed payloads as generic exceptions.
            // Never expose upstream response bodies, tokens or exception text to callers.
            throw unavailable();
        }
    }

    private ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "RACM authentication unavailable");
    }
}
