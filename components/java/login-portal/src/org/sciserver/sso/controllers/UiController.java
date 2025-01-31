/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under a modified Apache 2.0 license.
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/
package org.sciserver.sso.controllers;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.representations.AccessToken;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;
import org.sciserver.sso.AppConfig;
import org.sciserver.sso.AuthenticationService;
import org.sciserver.sso.DatabaseService;
import org.sciserver.sso.EmailService;
import org.sciserver.sso.EncryptedMessageService;
import org.sciserver.sso.Utility;
import org.sciserver.sso.keystone.User;
import org.sciserver.sso.keystone.KeystoneService.NotAuthorizedException;
import org.sciserver.sso.model.ApprovalRequest;
import org.sciserver.sso.model.ApprovalStatus;
import org.sciserver.sso.model.ErrorContent;
import org.sciserver.sso.model.LoginInfo;
import org.sciserver.sso.model.PasswordInfo;
import org.sciserver.sso.model.RegistrationInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.sciserver.sso.model.UserMapping;

@Controller
public class UiController {
    static final String COOKIE_NAME = "portalCookie";
    private static ObjectMapper mapper = new ObjectMapper();
    static final String LOGIN_INFO_MESSAGE = "info_message";
    static final String LOGIN_ERROR_MESSAGE = "error_message";

    @Autowired
    AppConfig appConfig;

    @Autowired
    AuthenticationService authenticationService;

    @Autowired
    EncryptedMessageService encryptedMessages;

    @Autowired
    EmailService emailService;

    @Autowired
    UIUtils uiUtils;

    @Autowired
    DatabaseService databaseService;

    @RequestMapping(value = {"/logout", "/Account/Logout"}, method = RequestMethod.GET)
    public String logout(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(required = false) String callbackUrl) throws Exception {

        request.logout();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }

        String token = uiUtils.getTokenFromCookie(request, response);

        String userName = null;
        String userId = null;
        try {
            if (token != null) {

                try {
                    JsonNode node = appConfig.getKeystoneService().validateToken(token);
                    userId = node.get("token").get("user").get("id").asText();
                    userName = node.get("token").get("user").get("name").asText();
                } catch (Exception e) {
                }

                appConfig.getKeystoneService().revokeToken(token);
                uiUtils.deleteCookie(response);
            }

            String resturnedString = null;

            if (StringUtils.isNotBlank(callbackUrl)) {
                URL url = new URL(callbackUrl);
                if (!Utility.isHostAllowed(url.getHost(), appConfig.getAppSettings().getAllowedHosts())) {
                    throw new Exception("Host not allowed");
                }
                resturnedString = "redirect:" + callbackUrl;
            } else {
                resturnedString = "redirect:/login";
            }

            ObjectNode sentence = mapper.createObjectNode();
            sentence.put("subject", userName);
            sentence.put("verb", "logged out");
            sentence.put("predicate", "from the Login Portal");
            Utility.LogMessage(userName + " logged out from the Login Portal", "LogOut", userName, userId, token,
                    request, sentence, true);

            return resturnedString;

        } catch (Exception ex) {
            Utility.LogExceptionMessage("LogOut", userName, userId, token, request, ex, null,
                    HttpStatus.INTERNAL_SERVER_ERROR);
            throw ex;
        }
    }

    @RequestMapping(value = {"/info"}, method = RequestMethod.GET)
    public String info(Model model, HttpServletRequest request, HttpServletResponse response) throws Exception {

        String token = uiUtils.getTokenFromCookie(request, response);

        if (token == null) {
            return "redirect:/login";
        }

        JsonNode root = appConfig.getKeystoneService().validateToken(token);
        String username = root.get("token").get("user").get("name").asText();
        model.addAttribute("username", username);
        return "info";
    }

    @RequestMapping(value = {"/linked-accounts"}, method = RequestMethod.GET)
    public String linkedAccounts(Model model, HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        String token = uiUtils.getTokenFromCookie(request, response);
        if (token == null) {
            return "redirect:/login";
        }

        JsonNode root = appConfig.getKeystoneService().validateToken(token);
        String userId = root.get("token").get("user").get("id").asText();

        model.addAttribute("accounts", databaseService.listMappings(userId));

        return "linked-accounts";
    }

    @RequestMapping(value = {"/", "/login", "/Account/Login"}, method = RequestMethod.GET)
    public String loginForm(Model model, HttpServletRequest request, HttpServletResponse response,
            @RequestParam(required = false) String callbackUrl) throws Exception {

        String token = uiUtils.getTokenFromCookie(request, response);
        String queryString = "";
        if (StringUtils.isNotBlank(callbackUrl)) {
            queryString = "callbackUrl=" + callbackUrl;
        }
        model.addAttribute("queryString", queryString);

        if (token == null) {
            model.addAttribute("loginInfo", new LoginInfo());
            return "login";
        } else {
            if (StringUtils.isNotBlank(callbackUrl) || appConfig.getAppSettings().getDefaultCallback() != null) {

                URL url = new URL(StringUtils.isNotBlank(callbackUrl) ? callbackUrl
                        : appConfig.getAppSettings().getDefaultCallback());
                if (!Utility.isHostAllowed(url.getHost(), appConfig.getAppSettings().getAllowedHosts())) {
                    throw new Exception("Host not allowed");
                }
                String newUrl = url.getProtocol() + "://" + url.getAuthority() + url.getPath() + "?"
                        + ((url.getQuery() == null) ? "" : url.getQuery()) + "&token=" + token;

                return "redirect:" + newUrl;
            } else {
                return "redirect:/info";
            }
        }
    }

    @RequestMapping(value = {"/", "/login", "/Account/Login"}, method = RequestMethod.POST)
    public String loginSubmit(@ModelAttribute LoginInfo loginInfo, HttpServletRequest request,
            HttpServletResponse response, Model model, @RequestParam(required = false) String callbackUrl)
            throws Exception {

        try {
            return uiUtils.login(loginInfo.getUsername(), loginInfo.getPassword(), request, response, callbackUrl);
        } catch (NotAuthorizedException e) {
            model.addAttribute(LOGIN_ERROR_MESSAGE, "Incorrect user name or password");
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        } catch (TooManyLoginAttemptsException e) {
            model.addAttribute(LOGIN_ERROR_MESSAGE, e.getMessage());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        }

        model.addAttribute("queryString", StringUtils.isNotBlank(callbackUrl) ? "callbackUrl=" + callbackUrl : "");
        loginInfo.setPassword("");
        return "login";
    }

    @RequestMapping(value = "/change-password", method = RequestMethod.GET)
    public String changePasswordForm(Model model, HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        String token = uiUtils.getTokenFromCookie(request, response);
        if (token == null) {
            return "redirect:/login";
        }

        JsonNode root = appConfig.getKeystoneService().validateToken(token);
        String username = root.get("token").get("user").get("name").asText();
        model.addAttribute("username", username);

        model.addAttribute("passwordInfo", new PasswordInfo());
        return "change-password";
    }

    @RequestMapping(value = "/change-password", method = RequestMethod.POST)
    public String changePasswordSubmit(@ModelAttribute RegistrationInfo passwordInfo, HttpServletRequest request,
            HttpServletResponse response, RedirectAttributes redirectAttrs) throws Exception {

        String userId = null;
        String userName = null;
        String token = null;
        try {
            token = uiUtils.getTokenFromCookie(request, response);
            if (token == null) {
                return "redirect:/login";
            }

            JsonNode root = appConfig.getKeystoneService().validateToken(token);
            userId = root.get("token").get("user").get("id").asText();
            userName = root.get("token").get("user").get("name").asText();

            appConfig.getKeystoneService().changePassword(userId, passwordInfo.getPassword());

            ObjectNode sentence = mapper.createObjectNode();
            sentence.put("subject", userName);
            sentence.put("verb", "changed");
            sentence.put("predicate", "SciServer account password");
            Utility.LogMessage(userName + " changed SciServer password", "ChangePassword", userName, userId, token,
                    request, sentence, null);

            redirectAttrs.addFlashAttribute(LOGIN_INFO_MESSAGE, "Password successfully changed.");
        } catch (Exception e) {
            ObjectNode info = mapper.createObjectNode();
            info.put("userName", userName);
            info.put("verb", "change");
            Utility.LogExceptionMessage("ChangePassword", userName, userId, token, request, e, info,
                    HttpStatus.INTERNAL_SERVER_ERROR);

            throw e;
        }
        return "redirect:login";
    }

    @RequestMapping(value = "/keycloak-sso", method = RequestMethod.GET)
    public String keycloakSso(Model model, HttpServletRequest request, HttpServletResponse response,
            @RequestParam(required = false) String callbackUrl, RedirectAttributes redir) throws Exception {

        KeycloakPrincipal<?> principal =
                (KeycloakPrincipal<?>) ((KeycloakAuthenticationToken) request.getUserPrincipal()).getPrincipal();
        AccessToken accessToken = principal.getKeycloakSecurityContext().getToken();
        String keycloakUserId = accessToken.getOtherClaims().get("user_id").toString();
        String keycloakUsername = accessToken.getPreferredUsername();
        ObjectNode sentence = mapper.createObjectNode().put("subject", keycloakUsername).put("verb", "signed in")
                .put("predicate", "using Keycloak");
        Utility.LogMessage(keycloakUsername + " signed in using Keycloak", "KeycloakSignIn", null, null, null, request,
                sentence, null);

        UserMapping mapping = databaseService.getUserMapping(keycloakUserId);

        String trustId;
        if (mapping == null) {
            redir.addFlashAttribute("callbackUrl", callbackUrl);
            redir.addFlashAttribute("");
            return "redirect:keycloak-link";
        } else {
            trustId = mapping.getKeystoneTrustId();
        }

        String trusteeUser = appConfig.getAppSettings().getKeystoneTrusteeUser();
        String trusteePassword = appConfig.getAppSettings().getKeystoneTrusteePassword();
        String keystoneToken = appConfig.getKeystoneService().getTrustedToken(trusteeUser, trusteePassword, trustId);

        JsonNode root = appConfig.getKeystoneService().validateToken(keystoneToken);
        String username = root.get("token").get("user").get("name").asText();
        User user = appConfig.getKeystoneService().getUsers(username).iterator().next();

        return uiUtils.login(keystoneToken, request, response, callbackUrl, user, keycloakUsername);
    }
    
    @RequestMapping(value = "/keycloak-link", method = RequestMethod.GET)
    public String keycloakLinkForm(Model model, HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        KeycloakPrincipal<?> principal =
                (KeycloakPrincipal<?>) ((KeycloakAuthenticationToken) request.getUserPrincipal()).getPrincipal();
        AccessToken accessToken = principal.getKeycloakSecurityContext().getToken();

        String preferredUsername = accessToken.getPreferredUsername();
        String username = preferredUsername.split("@")[0];
        StringBuffer buff = new StringBuffer(username);
        for (int i = 0; i < buff.length(); i++) {
            if (!Character.toString(buff.charAt(i)).matches("[a-zA-Z0-9_]")) {
                buff.setCharAt(i, '_');
            }
        }
        username = buff.toString();
        RegistrationInfo registrationInfo = new RegistrationInfo();
        registrationInfo.setUsername(username);
        registrationInfo.setEmail(accessToken.getEmail());

        // Generate a password that is consistent with our policies (at least one of: upper-case character, lower-case
        // character, digit character, and special character; length >= 8)
        List<CharacterRule> validPasswordRules = Arrays.asList(new CharacterRule(EnglishCharacterData.UpperCase, 1),
                new CharacterRule(EnglishCharacterData.LowerCase, 1), new CharacterRule(EnglishCharacterData.Digit, 1),
            new CharacterRule(EnglishCharacterData.Special, 1));
        PasswordGenerator generator = new PasswordGenerator();
        String password = generator.generatePassword(12, validPasswordRules);
        registrationInfo.setPassword(password);
        registrationInfo.setConfirmPassword(password);

        registrationInfo.setKeycloakUserId(accessToken.getOtherClaims().get("user_id").toString());
        registrationInfo.setKeycloakUsername(preferredUsername);

        Map<String, ?> inputFlashMap = RequestContextUtils.getInputFlashMap(request);

        model.addAttribute("loginInfo", new LoginInfo());
        model.addAttribute("preferredUsername", preferredUsername);
        model.addAttribute("registrationInfo", registrationInfo);
        model.addAttribute("callbackUrl", (String) inputFlashMap.get("callbackUrl"));

        return "keycloak-link";
    }

    @RequestMapping(value = "/keycloak-link", method = RequestMethod.POST)
    public String keycloakLinkSubmit(@ModelAttribute LoginInfo loginInfo, @ModelAttribute String callbackUrl,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        KeycloakPrincipal<?> principal =
                (KeycloakPrincipal<?>) ((KeycloakAuthenticationToken) request.getUserPrincipal()).getPrincipal();
        AccessToken accessToken = principal.getKeycloakSecurityContext().getToken();

        authenticationService.linkKeycloakUser(loginInfo.getUsername(), loginInfo.getPassword(),
                accessToken.getOtherClaims().get("user_id").toString(), accessToken.getPreferredUsername(), request);

        return "redirect:keycloak-sso?callbackUrl=" + callbackUrl;
    }
    
	@RequestMapping(value = {"/approval-requests"}, method = RequestMethod.GET)
	public String approvalRequests(Model model, HttpServletRequest request, HttpServletResponse response) throws Exception {
		String token = uiUtils.getTokenFromCookie(request, response);
		if (!authenticationService.isSystemActionAllowed("acceptRegistrationRequest", token)) {
			throw new Exception("Unauthorized");
		}
		
		model.addAttribute("approvalRequests", databaseService.getApprovalRequests());
		
		return "approval-requests";
	}
	
	@RequestMapping(value = {"/process-approval-request"}, method = RequestMethod.GET)
	public String approvalRequests(HttpServletRequest request, HttpServletResponse response,
			@RequestParam("userId") String keystoneUserId, @RequestParam("action") String action) throws Exception {
		
		String token = uiUtils.getTokenFromCookie(request, response);
		if (!authenticationService.isSystemActionAllowed("acceptRegistrationRequest", token)) {
			throw new Exception("Unauthorized");
		}
		
		ApprovalRequest req = databaseService.getApprovalRequest(keystoneUserId);
		ApprovalStatus status = req.getStatus();
		
		if ("accept".equalsIgnoreCase(action)) {
			status = ApprovalStatus.ACCEPTED;
			appConfig.getKeystoneService().setUserEnabled(keystoneUserId, true);
			emailService.sendRegistrationApprovalEmail(req.getEmail(), true);
		} else if ("reject".equalsIgnoreCase(action)) {
			status = ApprovalStatus.REJECTED;
			emailService.sendRegistrationApprovalEmail(req.getEmail(), false);
		} else {
			throw new Exception("Action must be either 'accept' or 'reject'");
		}
		
		databaseService.setApprovalRequestStatus(keystoneUserId, status);
		
		return "redirect:approval-requests";
	}

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String exceptionHandler(Model model, Exception ex, HttpServletResponse response) {
        Utility.LogExceptionLocally(ex);
        model.addAttribute("error", new ErrorContent(ex));
        return "error";
    }
}
