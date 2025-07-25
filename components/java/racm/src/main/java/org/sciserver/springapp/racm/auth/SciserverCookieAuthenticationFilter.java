package org.sciserver.springapp.racm.auth;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sciserver.springapp.racm.login.LoginPortalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import com.fasterxml.jackson.databind.JsonNode;

import retrofit2.Response;

@Component
@ConditionalOnWebApplication
public class SciserverCookieAuthenticationFilter extends AbstractPreAuthenticatedProcessingFilter  {
	private static final String AUTH_COOKIE = "portalCookie";
	private static final Logger LOG = LogManager.getLogger();
	private final LoginPortalService loginPortalService;

	SciserverCookieAuthenticationFilter(LoginPortalService loginPortalService) {
		this.loginPortalService = loginPortalService;
	}

	@Override
	protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
		String token = tokenFromRequest(request);

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
				LOG.debug("Obtained keystone id {} from {} cookie", keystoneId, AUTH_COOKIE);
				return keystoneId;
			}
		}
		return null;
	}

	@Override
	protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
		return tokenFromRequest(request);
	}

	private String tokenFromRequest(HttpServletRequest request) {
		Cookie cookie = WebUtils.getCookie(request, AUTH_COOKIE);
		return Optional.ofNullable(cookie)
				.map(Cookie::getValue)
				.orElse(null);
	}

	@Autowired
	@Qualifier("cookieAuthenticationManager")
	@Override
	public void setAuthenticationManager(AuthenticationManager authenticationManager) {
		super.setAuthenticationManager(authenticationManager);
	}
}
