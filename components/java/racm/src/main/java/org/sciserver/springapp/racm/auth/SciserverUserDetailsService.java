package org.sciserver.springapp.racm.auth;

import org.sciserver.springapp.racm.login.UserProfileSource;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class SciserverUserDetailsService
        implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {
    private final UserProfileSource userProfileSource;

    SciserverUserDetailsService(UserProfileSource userProfileSource) {
        this.userProfileSource = userProfileSource;
    }

    @Override
    public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken token) {
        return userProfileSource.getOrCreateUserProfile(token);
    }
}
