package org.sciserver.springapp.racm.login;

import org.sciserver.springapp.racm.auth.SciServerHeaderAuthenticationFilter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class NotAuthorizedException extends RuntimeException {
	private static final long serialVersionUID = 5717595646933483517L;

	public NotAuthorizedException() {
		super(String.format("Not authenticated or token has expired. Add valid token as %s header", SciServerHeaderAuthenticationFilter.AUTH_HEADER));
	}
}
