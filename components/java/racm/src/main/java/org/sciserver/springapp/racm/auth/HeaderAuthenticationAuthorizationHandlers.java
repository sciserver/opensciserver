package org.sciserver.springapp.racm.auth;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sciserver.springapp.racm.utils.controller.JsonAPIHelper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class HeaderAuthenticationAuthorizationHandlers {
	@Component
	public static class ReturnUnauthenticatedJsonMessage implements AuthenticationEntryPoint {
		private final ObjectMapper mapper;
		private final JsonAPIHelper jsonAPIHelper;
		public ReturnUnauthenticatedJsonMessage(ObjectMapper mapper, JsonAPIHelper jsonAPIHelper) {
			this.mapper = mapper;
			this.jsonAPIHelper = jsonAPIHelper;
		}

		// based on https://stackoverflow.com/a/46656635/239003
		@Override
		public void commence(HttpServletRequest request, HttpServletResponse response,
				AuthenticationException exception)
				throws IOException, ServletException {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			response.setHeader("WWW-Authenticate", SciServerHeaderAuthenticationFilter.AUTH_HEADER);

			ResponseEntity<JsonNode> entity = jsonAPIHelper.notAuthenticatedEntity();
			PrintWriter writer = response.getWriter();
			mapper.writeValue(writer, entity.getBody());
			writer.flush();
		}
	}

	@Component
	public static class ReturnUnauthorizedJsonMessage implements AccessDeniedHandler {
		private final ObjectMapper mapper;
		private final JsonAPIHelper jsonAPIHelper;
		public ReturnUnauthorizedJsonMessage(ObjectMapper mapper, JsonAPIHelper jsonAPIHelper) {
			this.mapper = mapper;
			this.jsonAPIHelper = jsonAPIHelper;
		}

		@Override
		public void handle(HttpServletRequest request, HttpServletResponse response,
				AccessDeniedException exception)
				throws IOException, ServletException {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);

			ResponseEntity<JsonNode> entity = jsonAPIHelper.notAuthorizedEntity();
			PrintWriter writer = response.getWriter();
			mapper.writeValue(writer, entity.getBody());
			writer.flush();
		}
	}
}
