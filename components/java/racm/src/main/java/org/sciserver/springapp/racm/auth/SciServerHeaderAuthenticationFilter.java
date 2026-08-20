package org.sciserver.springapp.racm.auth;

import java.io.IOException;
import java.io.UncheckedIOException;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sciserver.springapp.loginterceptor.Log;
import org.sciserver.springapp.racm.login.LoginPortalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import retrofit2.Response;
import sciserver.logging.ServiceLogTimer;

@Component
@ConditionalOnWebApplication
public class SciServerHeaderAuthenticationFilter extends AbstractPreAuthenticatedProcessingFilter {
	public static final String AUTH_HEADER = "X-Auth-Token";
    public static final String SERVICE_TOKEN_HEADER = "X-Service-Auth-ID";
	private static final Logger LOG = LogManager.getLogger();
	private final LoginPortalService loginPortalService;

	SciServerHeaderAuthenticationFilter(LoginPortalService loginPortalService) {
		this.loginPortalService = loginPortalService;
	}

	@Override
	protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
		String headerToken = request.getHeader(AUTH_HEADER);

		if(headerToken != null) {
			Response<JsonNode> loginPortalResponse;
			try {
			    ServiceLogTimer timer = Log.get().startTimer("SciServerHeaderAuthenticationFilter.getPreAuthenticatedPrincipal:getTokenInfo [ms]");
				loginPortalResponse = loginPortalService.getTokenInfo(headerToken).execute();
				timer.stop();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			if (loginPortalResponse.code() == 200) {
				String keystoneId = loginPortalResponse.body()
						.get("token").get("user").get("id").textValue();
				LOG.debug("Obtained keystone id {} from token passed in via {} header", keystoneId, AUTH_HEADER);
				return keystoneId;
			}
		}
		return null;
	}

	@Override
	protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
		return request.getHeader(AUTH_HEADER);
	}

	@Autowired
	@Qualifier("headerAuthenticationManager")
	@Override
	public void setAuthenticationManager(AuthenticationManager authenticationManager) {
		super.setAuthenticationManager(authenticationManager);
	}
}
