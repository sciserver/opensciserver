package org.sciserver.springapp.racm.auth;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sciserver.springapp.racm.config.LoginConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class WebAuthenticationAuthorizationHandlers {
	@Component
	public static class RedirectToLoginPortal implements AuthenticationEntryPoint {
		@Autowired
		private LoginConfig loginConfig;

		@Override
		public void commence(HttpServletRequest request, HttpServletResponse response,
				AuthenticationException exception)
				throws IOException, ServletException {
			String callback = request.getRequestURL().toString();
			String loginUrl = loginConfig.getLoginPortalUrl() + "/login";

			response.sendRedirect(loginUrl + "?callbackUrl=" + callback);
		}
	}
}
