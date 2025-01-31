/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under a modified Apache 2.0 license. 
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/
package org.sciserver.sso.controllers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sciserver.sso.AppConfig;
import org.sciserver.sso.AuthenticationService;
import org.sciserver.sso.Utility;
import org.sciserver.sso.model.ErrorContent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
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
public class ApiControllerV1 {
    private static ObjectMapper mapper = new ObjectMapper();

    @Autowired
    AppConfig appConfig;

    @Autowired
    AuthenticationService authenticationService;

    @RequestMapping(value="/keystone/v2.0/tokens/{token}", method = RequestMethod.HEAD)
    public void tokenHead20(@PathVariable String token) throws Exception {
        appConfig.getKeystoneService().checkToken(token);
    }
    
    @RequestMapping(value="/keystone/v2.0/tokens/{token}", method = RequestMethod.GET)
    public JsonNode tokenGet20(@PathVariable String token, HttpServletResponse response) throws Exception {
        return appConfig.getKeystoneService().validateToken20(token);
    }
    
    @RequestMapping(value="/keystone/v3/tokens/{token}", method = RequestMethod.HEAD)
    public void tokenHead(@PathVariable String token) throws Exception {
        appConfig.getKeystoneService().checkToken(token);
    }
    
    @RequestMapping(value="/keystone/v3/tokens/{token}", method = RequestMethod.GET)
    public JsonNode tokenGet(@PathVariable String token, HttpServletResponse response) throws Exception {
        return appConfig.getKeystoneService().validateToken(token);
    }
    
    @RequestMapping(value="/keystone/v3/users/{userId}", method = RequestMethod.GET)
    public JsonNode userGet(@PathVariable String userId, HttpServletResponse response) throws Exception {
        JsonNode user = appConfig.getKeystoneService().getUser(userId);
        ObjectNode userInfo = (ObjectNode) user.get("user");
        userInfo.put("email", "");
        return user;
    }
    
    @RequestMapping(value="/keystone/v3/tokens", method = RequestMethod.POST)
    public String authenticate(@RequestBody JsonNode auth, HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
    	String userName = null;
    	String userId = null;
    	String password = null; 
    	String token = null;
    	try{
	    	try{
	    		userName = auth.get("auth").get("identity").get("password").get("user").get("name").asText();
	    		password = auth.get("auth").get("identity").get("password").get("user").get("password").asText();
	    	}catch(Exception ex){
	    		throw new Exception("Incorrect request body format. User's name and/or password could not be parsed.");
	    	}
	    	token = authenticationService.login(userName, password);
	        userId = appConfig.getKeystoneService().getUserId(userName);
	        response.setHeader("X-Subject-Token", token);
	        
	        ObjectNode sentence = mapper.createObjectNode();
	        sentence.put("subject", userName);
	        sentence.put("verb", "authenticated");
	        sentence.put("predicate", "with the Login Portal");
	        Utility.LogMessage(userName + " authenticated with the LoginPortal", "Authenticate_v1", userName, userId, token, request, sentence, true);
	        
	        return token;
	        
    	}catch(Exception e){
    		Utility.LogExceptionMessage("Authenticate_v1", userName, userId, token, request, e, null, HttpStatus.INTERNAL_SERVER_ERROR);
    		throw e;
    	}
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
