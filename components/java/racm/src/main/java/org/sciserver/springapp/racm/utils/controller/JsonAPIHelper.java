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
		return logAndReturnJsonExceptionEntity(text, up, e, statusFor(e), isJOBM);
	}

	/**
	 * Maps an exception to the HTTP status it should be reported as.
	 *
	 * <p>Note VOURPException.ILLEGAL_STATE deliberately maps to 500: those sites signal a violated
	 * server-side invariant, which is what a 500 is for.
	 *
	 * @param e the exception to map
	 * @return the HTTP status to report
	 */
	static HttpStatus statusFor(Exception e) {
		if (e instanceof RACMException)
			return HttpStatus.UNAUTHORIZED;
		if (e instanceof InsufficientPermissionsException)
			return HttpStatus.FORBIDDEN;
		if (e instanceof VOURPException) {
			int error = ((VOURPException) e).getErrorCode();
			if (error == VOURPException.ILLEGAL_ARGUMENT)
				return HttpStatus.BAD_REQUEST;
			if (error == VOURPException.UNAUTHORIZED)
				return HttpStatus.FORBIDDEN;
			return HttpStatus.INTERNAL_SERVER_ERROR;
		}
		return HttpStatus.INTERNAL_SERVER_ERROR;
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