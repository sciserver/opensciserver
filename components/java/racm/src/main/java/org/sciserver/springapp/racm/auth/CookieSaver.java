package org.sciserver.springapp.racm.auth;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class CookieSaver extends SimpleUrlAuthenticationSuccessHandler {
	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws ServletException, IOException {
		if (!(authentication instanceof PreAuthenticatedAuthenticationToken))
			return;

		Cookie sessionCookie = new Cookie("portalCookie",
				authentication.getCredentials().toString());
		sessionCookie.setSecure(request.isSecure());
		sessionCookie.setHttpOnly(false); // Dashboard's JavaScript uses this cookie
		response.addCookie(sessionCookie);

		super.onAuthenticationSuccess(request, response, authentication);
	}

	@Override
	protected String determineTargetUrl(HttpServletRequest request,
			HttpServletResponse response) {
		return request.getRequestURL().toString();
	}
}
