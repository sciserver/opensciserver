package org.sciserver.springapp.racm.login;

import org.ivoa.dm.VOURPException;
import org.ivoa.dm.model.TransientObjectManager;
import org.sciserver.springapp.racm.ugm.application.UsersAndGroupsManager;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.sciserver.springapp.racm.utils.VOURPContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import edu.jhu.user.User;
import edu.jhu.user.UserVisibility;

@Service
public class UserProfileSource {
    public static final String COOKIE_NAME = "portalCookie";
    private final VOURPContext vourpContext;
    private final UsersAndGroupsManager usersAndGroupsManager;
    private final LoginPortalAccess loginPortalAccess;

    @Autowired
    public UserProfileSource(VOURPContext vourpContext, UsersAndGroupsManager usersAndGroupsManager,
            LoginPortalAccess loginPortalAccess) {
        this.vourpContext = vourpContext;
        this.usersAndGroupsManager = usersAndGroupsManager;
        this.loginPortalAccess = loginPortalAccess;
    }

    /**
     * Return a user profile for a user with an authenticated token and keystone id.
     *
     * TEMPORARY (?): In case the user exists in keystone and the token is valid,
     * but no user exists in VO-URP, create the latter. Expecially during
     * development this may happen when the VO-URP database has been reset. Creating
     * the VO-URP user on the fly in that case facilitates the dev work.
     *
     * @param preAuthToken
     * @return UserProfile
     */
    public UserProfile getOrCreateUserProfile(PreAuthenticatedAuthenticationToken preAuthToken) {
        TransientObjectManager tom = vourpContext.newTOM();
        String keystoneId = preAuthToken.getPrincipal().toString();
        String token = preAuthToken.getCredentials().toString();
        try {
            // check if user exists
            User user = usersAndGroupsManager.getRequestUser(keystoneId, tom);

            if (user == null) {
                user = new User(tom);
                user.setUserId(keystoneId);
                retrieveUserInfo(user, token);
                user.setVisibility(UserVisibility.PUBLIC);
                usersAndGroupsManager.addUserToPublicGroup(user);

                tom.persist();
            }

            UserProfile up = new UserProfile(user);
            up.setToken(token);
            return up;
        } catch (VOURPException e) {
            throw new IllegalStateException(e);
        }
    }

    private void retrieveUserInfo(User user, String token) throws VOURPException {
        JsonNode json = null;
        try {
            json = loginPortalAccess.validateUser(token);
        } catch (Exception e) {
            throw new VOURPException(e);
        }
        JsonNode jUserName = json.get("username");
        if (jUserName != null)
            user.setUsername(jUserName.asText());
        else
            throw new VOURPException("Cannot find username for Keystone userid");

        JsonNode jEmail = json.get("email");
        if (jEmail != null)
            user.setContactEmail(jEmail.asText());
    }
}
