package org.sciserver.springapp.racm.utils.controller;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.sciserver.springapp.racm.login.InsufficientPermissionsException;
import org.sciserver.springapp.racm.storem.application.DuplicateIdentifierException;
import org.sciserver.springapp.racm.storem.application.RegistrationInvalidException;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.fasterxml.jackson.databind.JsonNode;

@ControllerAdvice
public class RACMErrorHandler extends ResponseEntityExceptionHandler {
	private final JsonAPIHelper jsonAPIHelper;
	public RACMErrorHandler(JsonAPIHelper jsonAPIHelper) {
		this.jsonAPIHelper = jsonAPIHelper;
	}

	@ExceptionHandler
	@ResponseBody
	ResponseEntity<JsonNode> handleAllOtherExceptions(HttpServletRequest request, Throwable ex,
			@AuthenticationPrincipal UserProfile up) {
		HttpStatus status = getStatus(request);
		Exception exception = ex instanceof Exception
				? (Exception) ex
				: new Exception(ex);

		return jsonAPIHelper.logAndReturnJsonExceptionEntity(
				"Unknown error", Optional.ofNullable(up), exception, status);
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<JsonNode> handleResourceNotFound(ResourceNotFoundException ex) {
		return jsonAPIHelper.logAndReturnJsonExceptionEntity(
				"Resource not found or not readable by this user",
				Optional.empty(), ex, HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(DuplicateIdentifierException.class)
	public ResponseEntity<JsonNode> handleDuplicateError(DuplicateIdentifierException ex) {
		return jsonAPIHelper.logAndReturnJsonExceptionEntity(
				ex.getMessage(), Optional.empty(), ex, HttpStatus.CONFLICT);
	}

	@ExceptionHandler(RegistrationInvalidException.class)
	public ResponseEntity<JsonNode> handleValidationError(RegistrationInvalidException ex) {
		return jsonAPIHelper.logAndReturnJsonExceptionEntity(
				ex.getMessage(), Optional.empty(), ex, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(InsufficientPermissionsException.class)
	@ResponseBody
	public ResponseEntity<JsonNode> handleForbidden(InsufficientPermissionsException ex) {
		return jsonAPIHelper.logAndReturnJsonExceptionEntity(
				ex.getMessage(), Optional.empty(), ex, HttpStatus.FORBIDDEN);
	}

	private HttpStatus getStatus(HttpServletRequest request) {
		Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
		if (statusCode == null) {
			return HttpStatus.INTERNAL_SERVER_ERROR;
		}
		return HttpStatus.valueOf(statusCode);
	}
}
