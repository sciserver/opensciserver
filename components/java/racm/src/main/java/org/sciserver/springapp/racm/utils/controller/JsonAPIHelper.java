package org.sciserver.springapp.racm.utils.controller;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ivoa.dm.VOURPException;
import org.sciserver.springapp.racm.auth.SciServerHeaderAuthenticationFilter;
import org.sciserver.springapp.racm.login.InsufficientPermissionsException;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.sciserver.springapp.racm.utils.RACMException;
import org.sciserver.springapp.racm.utils.RACMUtil;
import org.sciserver.springapp.racm.utils.logging.LogUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class JsonAPIHelper {
	private static final Logger LOG = LogManager.getLogger();
	private ObjectMapper om = RACMUtil.newObjectMapper();

	public ResponseEntity<JsonNode> success(Object o) {
		JsonNode json = om.valueToTree(o);
		return new ResponseEntity<>(json, HttpStatus.OK);
	}

	public ResponseEntity<JsonNode> logAndReturnJsonExceptionEntity(
			String text, Optional<UserProfile> up, Exception e) {
		return logAndReturnJsonExceptionEntity(text, up, e, false);
	}

	public ResponseEntity<JsonNode> logAndReturnJsonExceptionEntity(
			String text, Optional<UserProfile> up, Exception e, boolean isJOBM) {
		HttpStatus http;
		if (e instanceof RACMException)
			http = HttpStatus.UNAUTHORIZED;
		else if (e instanceof InsufficientPermissionsException) {
			http = HttpStatus.FORBIDDEN;
		} else if (e instanceof VOURPException) {
			int error = ((VOURPException) e).getErrorCode();
			if (error == VOURPException.ILLEGAL_ARGUMENT)
				http = HttpStatus.BAD_REQUEST;
			else
				http = HttpStatus.INTERNAL_SERVER_ERROR;
		} else {
			http = HttpStatus.INTERNAL_SERVER_ERROR;
		}
		return logAndReturnJsonExceptionEntity(text, up, e, http, isJOBM);
	}

	public ResponseEntity<JsonNode> logAndReturnJsonExceptionEntity(
			String text, Optional<UserProfile> up,
			Exception e, HttpStatus status) {
		return logAndReturnJsonExceptionEntity(text, up, e, status, false);
	}

	public ResponseEntity<JsonNode> logAndReturnJsonExceptionEntity(
			String text, Optional<UserProfile> up,
			Exception e, HttpStatus status,
			boolean isJOBM) {
		LOG.error(text, e);
		LogUtils.buildLog()
			.logError()
			.forJOBM(isJOBM)
			.errorText(text)
			.exception(e)
			.user(up)
			.log();
		return new ResponseEntity<>(jsonException(text), status);
	}

	private JsonNode jsonException(String message) {
		ObjectNode on = om.createObjectNode();
		on.put("status", "error");
		on.put("error", message);
		return on;
	}

	public ResponseEntity<JsonNode> notAuthenticatedEntity() {
        return new ResponseEntity<>(jsonException(
                "Not authenticated. Add valid token as " + SciServerHeaderAuthenticationFilter.AUTH_HEADER + " header"),
				HttpStatus.UNAUTHORIZED);
	}

	public ResponseEntity<JsonNode> notAuthorizedEntity() {
		return new ResponseEntity<>(
				jsonException("Not authorized to access this endpoint"),
				HttpStatus.FORBIDDEN);
	}

	public ResponseEntity<JsonNode> notAuthorizedServiceEntity() {
		return new ResponseEntity<>(
				jsonException("Not authorized. Add valid UUID as X-Service-Auth-ID header"),
				HttpStatus.UNAUTHORIZED);
	}

	public ResponseEntity<JsonNode> notAuthorizedServiceTokenEntity() {
		return new ResponseEntity<>(
				jsonException("Not authorized. Add valid service token as X-RACM-Service-Token header"),
				HttpStatus.UNAUTHORIZED);
	}
}