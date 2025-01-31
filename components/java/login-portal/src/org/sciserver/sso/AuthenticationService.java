/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under a modified Apache 2.0 license. 
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/
package org.sciserver.sso;

import java.sql.SQLException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.time.Duration;
import java.time.Instant;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.sciserver.sso.controllers.TooManyLoginAttemptsException;
import org.sciserver.sso.keystone.InvalidPasswordException;
import org.sciserver.sso.keystone.KeystoneService.NotAuthorizedException;
import org.sciserver.sso.model.RuleType;
import org.sciserver.sso.model.UserMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class AuthenticationService {
	@Autowired
	AppConfig appConfig;
	
	@Autowired
	DatabaseService databaseService;
	
	public String login(String userName, String password) throws Exception {
		LoginStats stats = appConfig.getLoginStats().computeIfAbsent(userName,
				ignored -> new LoginStats(0));

		if (stats.getAttempts().incrementAndGet() > appConfig.getAppSettings().getMaxLoginAttempts()) {
			int waitMinutes = appConfig.getAppSettings().getWaitMinutes();

			long minutesElapsed = Duration
					.between(stats.getFirstAttemptTime(), Instant.now())
					.toMinutes();
			if (minutesElapsed < waitMinutes) {
				throw new TooManyLoginAttemptsException("Maximum number of login attempts reached. Please try again in "
						+ (waitMinutes - minutesElapsed) + " minutes."); 
			} else {
				appConfig.getLoginStats().put(userName, new LoginStats(1));
			}
		}

		String token = appConfig.getKeystoneService().getToken(userName, userName, password);
		appConfig.getLoginStats().remove(userName);
		return token;
	}

	public void verifyUser(String userName, String password, String email, ServletContext context) throws
			InvalidUserException, CreatedUserWithSameCredientialsException,
			InvalidPasswordException, IOException {
		
		try {
			if (appConfig.getAppSettings().isEmailFilteringEnabled()) {
				RuleType ruleType = Utility.getEmailRuleType(email, databaseService.getRules(), context);
				if (ruleType.equals(RuleType.DENY)) {
					throw new InvalidUserException("Email rejected by filter");
				}
			}
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		
		if (userName == null || !userName.matches("^[a-zA-Z0-9_]+$")) {
			throw new InvalidUserException("User name must only contain "
					+ "the letters A-z, numbers, or underscores");
		}

		appConfig.getKeystoneService().validatePassword(userName, password);

		if (email == null || !email.contains("@")) {
			throw new InvalidUserException("Invalid email address "
					+ (email == null ? "" : email));
		}
		if (appConfig.getKeystoneService().tryGettingUserId(userName).isPresent()) {
			String userId;
			try {
				userId = login(userName, password);
			} catch (TooManyLoginAttemptsException e) {
				throw new InvalidUserException("User name '" + userName + "' already exists."
						+ " Cannot check if your credentials match: " + e.getMessage());
			} catch (NotAuthorizedException e) {
				throw new InvalidUserException("User name '" + userName + "' already exists");
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			throw new CreatedUserWithSameCredientialsException(userId);
		}

		try {
			if (!appConfig.getKeystoneService().getUsersFromEmail(email).isEmpty()) {
				throw new InvalidUserException("An account with the email address "
						+ email + " already exists");
			}
		} catch (Exception e) {
			// Any errors from a non-200 response from Keystone are significant here
			throw new RuntimeException(e);
		}
	}

	public class InvalidUserException extends Exception {
		private static final long serialVersionUID = -3570244350279653515L;
		private InvalidUserException(String message) {
			super(message);
		}
	}

	public class CreatedUserWithSameCredientialsException extends Exception {
		private static final long serialVersionUID = -2419285318218615155L;
		private final String userId;
		private CreatedUserWithSameCredientialsException(String userId) {
			super();
			this.userId = userId;
		}
		public String getUserId() {
			return userId;
		}
	}

	public String createUser(String userName, String password, String email, ServletContext context) throws Exception {
		verifyUser(userName, password, email, context);
		if (appConfig.getAppSettings().isCjEnabled()) {
			return appConfig.getKeystoneService().registerUserCj(userName, email, password);
		} else {
			return appConfig.getKeystoneService().registerUser(userName, email, password);
		}
	}
	
	public void linkKeycloakUser(String userName, String password, String keycloakUserId, String externalUsername, HttpServletRequest request) throws Exception {
		String trustorToken = appConfig.getKeystoneService().getToken(userName, userName, password);
		String trustorUserId = appConfig.getKeystoneService().getUserId(userName);
		String trustId = appConfig.getKeystoneService().getTrustId(trustorToken, appConfig.getAppSettings().getKeystoneTrusteeUser());
		UserMapping mapping = new UserMapping();
		mapping.setExternalUserId(keycloakUserId);
		mapping.setKeystoneUserId(trustorUserId);
		mapping.setKeystoneTrustId(trustId);
		mapping.setExternalUsername(externalUsername);
		databaseService.addUserMapping(mapping);
		
		ObjectNode sentence = new ObjectMapper().createObjectNode();
		sentence.put("subject", userName);
		sentence.put("verb", "linked");
		sentence.put("predicate", "an external account " + externalUsername);
		Utility.LogMessage(userName + " linked an external account "+ externalUsername, "KeycloakLink", userName, trustorUserId, trustorToken, request, sentence, true);
	}
	
	public boolean isSystemActionAllowed(String action, String token) throws Exception {
		boolean result = false;
		
		CloseableHttpClient httpClient = HttpClients.createDefault();
		try {
			HttpGet httpGet = new HttpGet(appConfig.getAppSettings().getRacmUrl() +
					"/rest/rc/"+ appConfig.getAppSettings().getRacmSystemRcUuid() +
					"/root/" + action);
			
			httpGet.addHeader("X-Auth-Token", token);
			
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
			result = mapper.readValue(responseAsString, boolean.class);
		} finally {
			httpClient.close();
		}
		
		return result;
	}
}
