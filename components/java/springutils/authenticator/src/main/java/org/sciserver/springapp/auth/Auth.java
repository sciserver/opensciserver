package org.sciserver.springapp.auth;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import org.sciserver.authentication.client.AuthenticatedUser;
import org.sciserver.authentication.client.CachingAuthenticationClient;
import org.sciserver.authentication.client.UnauthenticatedException;
import org.sciserver.clientutils.SciServerClientException;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
Authentication interface for use in controllers. Applications that include this package can obtain the current
authorized user by calling Auth.get(), which will return the authorized user or throw an exception. There is no need to
explicitly pass a token or cookie, as this obtains those from the standard locations.
 */
@Service
public class Auth implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    private static AuthenticatedUser getAndSetAttributes() throws UnauthenticatedException, SciServerClientException {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
            .getRequest();
        CachingAuthenticationClient authClient = context.getBean(CachingAuthenticationClient.class);
        String token = request.getHeader(AuthConfig.tokenHeader);
        if (token == null) {
            if (request.getCookies() != null) {
                for (Cookie c : request.getCookies()) {
                    if (c.getName().equals(AuthConfig.tokenCookie)) {
                        token = c.getValue();
                        break;
                    }
                }
            }
        }
        if (token == null) {
            throw new UnauthenticatedException("No token provided, cannot authenticate user");
        }
        AuthenticatedUser user = authClient.getAuthenticatedUser(token);
        request.setAttribute(AuthConfig.reqAttrBase + "userName", user.getUserName());
        request.setAttribute(AuthConfig.reqAttrBase + "userId", user.getUserId());
        return user;
    }


    /**
     * Get the authenticated user for the current request, call this in a controller to both signal authentication is
     * required and to obtain user details.
     *
     * @return Authenticated user, see sciserver-clients for more.
     *
     * @throws UnauthenticatedException if the user token is not passed or the user is not authenticated.
     * @throws SciServerClientException problems other than above, e.g. could not connect to login portal.
     */
    public static AuthenticatedUser get() throws UnauthenticatedException, SciServerClientException {
        return getAndSetAttributes();
    }

    /**
     * Run the authentication flow but without returning the user. This populates cache but does not require
     * authentication. It is designed primarily for use in the interceptor and not in application code.
     */
    public static void prefetch() {
        try {
            getAndSetAttributes();
        } catch (Exception e) {
            // exceptions pushed up to controller IFF authentication necessary, using Auth.get()
        }
    }

    /**
     * Get the authentication client singleton. The bean is global in scope and can be used to make arbitrary calls.
     *
     * @return the authentication client, see sciserver-clients for more.
     */
    public static CachingAuthenticationClient getClient() {
        return context.getBean(CachingAuthenticationClient.class);
    }

}
