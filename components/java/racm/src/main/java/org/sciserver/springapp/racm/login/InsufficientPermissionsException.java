package org.sciserver.springapp.racm.login;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class InsufficientPermissionsException extends RuntimeException {
	private static final long serialVersionUID = -7608875315024804041L;

	public InsufficientPermissionsException(String actionDescription) {
		super("User is not allowed to " + actionDescription + ".");
	}
}
