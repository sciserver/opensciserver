package org.sciserver.springapp.racm.resourcecontext.controllers;

import org.sciserver.springapp.racm.utils.controller.JsonAPIHelper;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.fasterxml.jackson.databind.JsonNode;

@ControllerAdvice("org.sciserver.racm.resourcecontext.controllers")
public class InvalidServiceTokenHandler {
	private final JsonAPIHelper jsonAPIHelper;

	InvalidServiceTokenHandler(JsonAPIHelper jsonAPIHelper) {
		this.jsonAPIHelper = jsonAPIHelper;
	}

	@ExceptionHandler(InvalidServiceToken.class)
	@Order(Ordered.HIGHEST_PRECEDENCE)
	public ResponseEntity<JsonNode> handleInvalidServiceToken() {
		return jsonAPIHelper.notAuthorizedServiceTokenEntity();
	}
}
