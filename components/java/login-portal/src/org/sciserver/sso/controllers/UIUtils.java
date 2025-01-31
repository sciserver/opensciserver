package org.sciserver.sso.controllers;

import static org.sciserver.sso.controllers.UiController.COOKIE_NAME;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.sciserver.sso.AppConfig;
import org.sciserver.sso.AuthenticationService;
import org.sciserver.sso.Utility;
import org.sciserver.sso.keystone.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class UIUtils {
    private final AppConfig appConfig;
    private final AuthenticationService authenticationService;
    private final ObjectMapper mapper = new ObjectMapper();

    UIUtils(AppConfig appConfig, AuthenticationService authenticationService) {
        this.appConfig = appConfig;
        this.authenticationService = authenticationService;
    }

    /**
     * Verify login and return redirect string.
     */
    String login(String userName, String password, HttpServletRequest request, HttpServletResponse response,
            String callbackUrl) throws Exception {
        String userId = null;
        String token = null;
        try {
            token = authenticationService.login(userName, password);
            User user = appConfig.getKeystoneService().getUsers(userName).iterator().next();
            return login(token, request, response, callbackUrl, user, null);

        } catch (Exception e) {
            Utility.LogExceptionMessage("LogIn", userName, userId, token, request, e, null,
                    HttpStatus.INTERNAL_SERVER_ERROR);
            throw (e);
        }
    }

    String login(String token, HttpServletRequest request, HttpServletResponse response, String callbackUrl, User user,
            String linkedAccountName) throws MalformedURLException, HostNotAllowedInRedirectException {
        setCookie(response, token);
        String returnedString = null;

        if (StringUtils.isNotBlank(callbackUrl) || appConfig.getAppSettings().getDefaultCallback() != null) {
            URL url = new URL(StringUtils.isNotEmpty(callbackUrl) ? callbackUrl
                    : appConfig.getAppSettings().getDefaultCallback());
            if (!Utility.isHostAllowed(url.getHost(), appConfig.getAppSettings().getAllowedHosts())) {
                throw new HostNotAllowedInRedirectException("Host not allowed");
            }
            String newUrl = url.getProtocol() + "://" + url.getAuthority() + url.getPath() + "?"
                    + ((url.getQuery() == null) ? "" : url.getQuery()) + "&token=" + token;

            returnedString = "redirect:" + newUrl;
        } else {
            returnedString = "redirect:/info";
        }

        String userName = user.getUserName();
        String userId = user.getUserId();
        ObjectNode sentence = mapper.createObjectNode()
                .put("subject", userName)
                .put("verb", "logged into")
                .put("predicate", "the Login Portal" + (linkedAccountName == null ? "" : " as " + linkedAccountName));
        Utility.LogMessage(
                userName + " logged into the Login Portal"
                        + (linkedAccountName == null ? "" : " as " + linkedAccountName),
                "LogIn", userName, userId, token, request, sentence, null);

        return returnedString;
    }

    private class HostNotAllowedInRedirectException extends Exception {
        private static final long serialVersionUID = -531407418717194710L;

        private HostNotAllowedInRedirectException(String message) {
            super(message);
        }
    }

    private void setCookie(HttpServletResponse response, String value) {
        Cookie cookie = new Cookie(COOKIE_NAME, value);
        cookie.setPath("/");
        cookie.setSecure(true);
        response.addCookie(cookie);
    }

    void deleteCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(COOKIE_NAME, null);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String getCookieValue(HttpServletRequest request) {
        String value = null;

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            cookies = new Cookie[0];
        }

        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                value = cookie.getValue();
            }
        }

        return value;
    }

    String getTokenFromCookie(HttpServletRequest request, HttpServletResponse response) {
        String token = getCookieValue(request);

        try {
            appConfig.getKeystoneService().checkToken(token);
        } catch (Exception e) {
            token = null;
            deleteCookie(response);
        }

        return token;
    }

    void verifyCodeUnexpired(Instant creationTime, String errorMessage) throws ExpiredCodeException {
        double ageOfCode = (double) Duration.between(creationTime, Instant.now()).getSeconds() / 60;
        if (ageOfCode >= appConfig.getAppSettings().getValidationCodeLifetimeMinutes()) {
            throw new ExpiredCodeException(creationTime, errorMessage);
        }
    }
}
