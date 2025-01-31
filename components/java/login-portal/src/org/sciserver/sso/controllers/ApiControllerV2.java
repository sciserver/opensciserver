/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under a modified Apache 2.0 license. 
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/
package org.sciserver.sso.controllers;

import java.util.List;
import java.util.ArrayList;

import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sciserver.sso.AppConfig;
import org.sciserver.sso.AuthenticationService;
import org.sciserver.sso.CasJobs;
import org.sciserver.sso.DatabaseService;
import org.sciserver.sso.Utility;
import org.sciserver.sso.keystone.Auth;
import org.sciserver.sso.keystone.Trustee;
import org.sciserver.sso.keystone.User;
import org.sciserver.sso.keystone.KeystoneService.NotAuthorizedException;
import org.sciserver.sso.model.ErrorContent;
import org.sciserver.sso.model.PasswordInfo;
import org.sciserver.sso.model.Rule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

@CrossOrigin(origins = "*", exposedHeaders = "X-Subject-Token")
@RestController
public class ApiControllerV2 {
    private static ObjectMapper mapper = new ObjectMapper();
    
    @Autowired
    AppConfig appConfig;

    @Autowired
    AuthenticationService authenticationService;

    @Autowired
    ServletContext context;
    
	@Autowired
	DatabaseService databaseService;
	
    @RequestMapping(value="/api/tokens/{token}", method = RequestMethod.HEAD)
    public void tokenHead(@PathVariable String token) throws Exception {
        appConfig.getKeystoneService().checkToken(token);
    }
    
    @RequestMapping(value="/api/tokens/{token}", method = RequestMethod.GET)
    public JsonNode tokenGet(@PathVariable String token, HttpServletResponse response) throws Exception {
        return appConfig.getKeystoneService().validateToken(token);
    }

    @RequestMapping(value="/api/validate/{token}", method = RequestMethod.GET)
    public User validateTokenGetUser(@PathVariable String token) throws Exception {
        String userId = appConfig.getKeystoneService().validateToken(token).get("token").get("user").get("id").textValue();
        return appConfig.getKeystoneService().getUserById(userId);
    }

    // default empty string for cookie is to ensure we throw a 401
    // rather than 500 (for improper call) when no cookie is supplied
    @RequestMapping(value="/api/check-cookie-token/{userid}", method = RequestMethod.HEAD)
    public void checkCookieToken(@PathVariable String userid, @CookieValue(value = "portalCookie", defaultValue = "") String token) throws Exception {
	String tokenUserId = appConfig.getKeystoneService().validateToken(token).get("token").get("user").get("id").textValue();
	if (!tokenUserId.equals(userid))
            throw new NotAuthorizedException();
    }

    @ExceptionHandler(NotAuthorizedException.class)
    @ResponseStatus(value = HttpStatus.UNAUTHORIZED)
    public void handleUnauthorizedException(Exception ex) {
    }
    
    @RequestMapping(value="/api/trusts", method = RequestMethod.POST)
    public String trustGet(@RequestBody Trustee trustee, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String trustorToken = request.getHeader("X-Auth-Token");
        return appConfig.getKeystoneService().getTrustId(trustorToken, trustee.getUserName());
    }
    
    @RequestMapping(value="/api/auth", method = RequestMethod.POST)
    public String authenticate(@RequestBody Auth auth, HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
    	String userName = null;
    	String userId = null;
    	String token = null;
    	try{
    		try{
    			userName = auth.getUserName();
    			userId = appConfig.getKeystoneService().getUserId(userName);
    		}catch(Exception ignored){}
    		
	        if (auth.getTrustId() == null) {
	            token = authenticationService.login(auth.getUserName(), auth.getPassword());
	        } else {
	        	token = appConfig.getKeystoneService().getTrustedToken(auth.getUserName(), auth.getPassword(), auth.getTrustId());
	        }

	        
	        ObjectNode sentence = mapper.createObjectNode();
	        sentence.put("subject", userName);
	        sentence.put("verb", "authenticated");
	        sentence.put("predicate", "with the Login Portal");
	        Utility.LogMessage(userName + " authenticated with the LoginPortal", "Authenticate_v2", userName, userId, token, request, sentence, true);
	        
	        return token;
    	}catch(Exception ex){
    		Utility.LogExceptionMessage("Authenticate_v2", userName, userId, token, request, ex, null, HttpStatus.INTERNAL_SERVER_ERROR);
    		throw ex;
    	}
        
    }
    
    @RequestMapping(value="/api/password", method = RequestMethod.POST)
    public void passwordChange(@RequestBody PasswordInfo passwordInfo, HttpServletRequest request, HttpServletResponse response) throws Exception {
    	String token = Utility.getTokenFromHeader(request, response);
    	JsonNode root = appConfig.getKeystoneService().validateToken(token);
    	String userName = root.get("token").get("user").get("name").asText();
    	String userId = root.get("token").get("user").get("id").asText();
    	
    	appConfig.getKeystoneService().changePassword(
                userId,
                passwordInfo.getPassword());
        
        ObjectNode sentence = mapper.createObjectNode();
        sentence.put("subject", userName);
        sentence.put("verb", "changed");
        sentence.put("predicate", "SciServer account password");
        Utility.LogMessage(userName + " changed SciServer password", "ChangePassword", userName, userId, token, request, sentence, null);
    }
    
    @RequestMapping(value="/api/users/{userId}", method = RequestMethod.GET)
    public User usersGet(@PathVariable String userId, HttpServletResponse response) throws Exception {
        User user = appConfig.getKeystoneService().getUserById(userId);
        user.setEmail("");
        return user;
    }
    
    @RequestMapping(value= {"/api/health","/health"}, method = RequestMethod.GET)
    public JsonNode health() throws Exception {
    	return appConfig.getKeystoneService().getInfo();
    }
    
    @RequestMapping(value="/api/users", method = RequestMethod.GET)
    public Iterable<User> usersGet(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String userName = request.getParameter("name");
        Iterable<User> users = appConfig.getKeystoneService().getUsers(userName);
        users.forEach(user -> user.setEmail(""));
        return users;
    }
    
    @RequestMapping(value="/api/users", method = RequestMethod.POST)
    public User usersCreate(@RequestBody User user, @RequestHeader("X-Auth-Token") String token,
    		HttpServletRequest request, HttpServletResponse response) throws Exception {
    	String userId;
    	String userName = null;
        String email = null;
        try{
        	userName = user.getUserName();
        	email = user.getEmail();

        	if (!appConfig.getKeystoneService().isAdmin(token)) {
        		throw new Exception("Trying to create a new user, but without an admin token");
        	}
        	userId = authenticationService.createUser(userName, user.getPassword(), email, context);
	        
	        ObjectNode sentence = mapper.createObjectNode();
	        sentence.put("subject", userName);
	        sentence.put("verb", "created");
	        sentence.put("predicate", "a SciServer user account.");
	        Utility.LogMessage(user.getUserName() + " created a SciServer user account.", "CreateAccount", userName, userId, null, request, sentence, true);
	        
	        return appConfig.getKeystoneService().getUserById(userId);
        }catch(Exception ex){
        	
        	ObjectNode info = mapper.createObjectNode();
	        info.put("userName", userName);
	        info.put("email", email);
			Utility.LogExceptionMessage("CreateAccount", null, null, null, request, ex, info, HttpStatus.INTERNAL_SERVER_ERROR);
        	throw ex;
        }
    }
    
    @RequestMapping(value="/api/users/{userId}", method = RequestMethod.DELETE)
    public void usersDelete(@PathVariable String userId, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String token = request.getHeader("X-Auth-Token");
        if (appConfig.getKeystoneService().isAdmin(token)) {
    		if (appConfig.getAppSettings().isCjEnabled()) {
    			CasJobs.deleteUser(userId);
    		}
            appConfig.getKeystoneService().unregisterUser(userId);
        } else {
            throw new Exception("Unauthorized");
        }
    }

    @RequestMapping(value="/api/users/{userId}/disable", method = RequestMethod.POST)
    public void usersDisable(@PathVariable String userId, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String token = request.getHeader("X-Auth-Token");
        if (appConfig.getKeystoneService().isAdmin(token)) {
            appConfig.getKeystoneService().setUserEnabled(userId, false);
        } else {
            throw new Exception("Unauthorized");
        }
    }

	@RequestMapping(value="/api/accounts/{id}", method = RequestMethod.DELETE) 
	public void unlinkAccount(@PathVariable Long id, HttpServletRequest request, HttpServletResponse response) throws Exception {
		String token = request.getHeader("X-Auth-Token");
		JsonNode root = appConfig.getKeystoneService().validateToken(token);
		String username = root.get("token").get("user").get("name").asText();
		String userId = root.get("token").get("user").get("id").asText();
		
		Map<Long, String> mappings = databaseService.listMappings(userId);
		if (mappings.containsKey(id)) {
			databaseService.unlinkAccount(id);
			ObjectNode sentence = new ObjectMapper().createObjectNode();
			sentence.put("subject", username);
			sentence.put("verb", "unlinked");
			sentence.put("predicate", "an external account " + mappings.get(id));
			Utility.LogMessage(username + " unlinked an external account "+ mappings.get(id), "KeycloakUnlink", username, userId, token, request, sentence, true);
		}
	}

	@RequestMapping(value="/api/accounts", method = RequestMethod.GET) 
	public JsonNode getAccounts(HttpServletRequest request, HttpServletResponse response) throws Exception {
		class KeyValuePair<S,T> {
			private S key;
			private T value;
			public KeyValuePair(S key, T value) {
				this.key = key;
				this.value = value;
			}
			public S getKey() {
				return key;
			}
			public void setKey(S key) {
				this.key = key;
			}
			public T getValue() {
				return value;
			}
			public void setValue(T value) {
				this.value = value;
			}
		}
		
		String token = request.getHeader("X-Auth-Token");
		JsonNode root = appConfig.getKeystoneService().validateToken(token);
		String userId = root.get("token").get("user").get("id").asText();
		Map<Long, String> mappings = databaseService.listMappings(userId);
		mappings.keySet().stream().map((Long x) -> new KeyValuePair<Long, String>(x, mappings.get(x))).collect(Collectors.toList());
		
		return new ObjectMapper().valueToTree(mappings.keySet().stream().map((Long x) -> new KeyValuePair<Long, String>(x, mappings.get(x))).collect(Collectors.toList()));
	}

	@RequestMapping(value="/api/rules", method = RequestMethod.GET)
	public List<Rule> getRules(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String token = request.getHeader("X-Auth-Token");
		if (!appConfig.getKeystoneService().isAdmin(token)) 
			throw new Exception("Unauthorized");
		
		if (!appConfig.getAppSettings().isEmailFilteringEnabled())
			throw new Exception("Email filtering is not enabled");
		
		return databaseService.getRules();
	}
	
	@RequestMapping(value="/api/rules", method = RequestMethod.POST)
	public void setRules(@RequestBody List<Rule> rules, HttpServletRequest request, HttpServletResponse response) throws Exception{
		String token = request.getHeader("X-Auth-Token");
		if (!appConfig.getKeystoneService().isAdmin(token)) 
			throw new Exception("Unauthorized");
		
		if (!appConfig.getAppSettings().isEmailFilteringEnabled())
			throw new Exception("Email filtering is not enabled");
		
		databaseService.setRules(rules);
	}
	
    @ExceptionHandler(Exception.class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public Object handleException(Exception ex) {
    	Utility.LogExceptionLocally(ex);
        JsonNodeFactory factory = JsonNodeFactory.instance;
        return factory.objectNode().putPOJO("error", new ErrorContent(ex));
    }
}
