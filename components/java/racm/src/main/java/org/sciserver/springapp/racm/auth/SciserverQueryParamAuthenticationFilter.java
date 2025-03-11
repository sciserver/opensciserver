package org.sciserver.springapp.racm.auth;

import java.io.IOException;
import java.io.UncheckedIOException;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sciserver.springapp.racm.login.LoginPortalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import retrofit2.Response;

@Component
@ConditionalOnWebApplication
public class SciserverQueryParamAuthenticationFilter extends AbstractPreAuthenticatedProcessingFilter {
	private static final Logger LOG = LogManager.getLogger();
	private final LoginPortalService loginPortalService;

	SciserverQueryParamAuthenticationFilter(LoginPortalService loginPortalService) {
		this.loginPortalService = loginPortalService;
	}

	@Override
	protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
		String token = request.getParameter("token");

		if(token != null) {
			Response<JsonNode> loginPortalResponse;
			try {
				loginPortalResponse = loginPortalService.getTokenInfo(token).execute();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			if (loginPortalResponse.code() == 200) {
				String keystoneId = loginPortalResponse.body()
						.get("token").get("user").get("id").textValue();
				LOG.debug("Obtained keystone id {} from query string", keystoneId);
				return keystoneId;
			}
		}
		return null;
	}

	@Override
	protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
		return request.getParameter("token");
	}


	@Autowired
	@Qualifier("cookieAuthenticationManager")
	@Override
	public void setAuthenticationManager(AuthenticationManager authenticationManager) {
		super.setAuthenticationManager(authenticationManager);
	}

	@Autowired
	@Qualifier("cookieSaver")
	@Override
	public void setAuthenticationSuccessHandler(AuthenticationSuccessHandler authenticationSuccessHandler) {
		super.setAuthenticationSuccessHandler(authenticationSuccessHandler);
	}
}
