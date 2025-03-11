package org.sciserver.sso.keystone;

public class InvalidPasswordException extends Exception {
    private static final long serialVersionUID = 6184174817070894163L;

    public InvalidPasswordException(String message) {
		super(message);
	}

    public InvalidPasswordException(String message, Throwable cause) {
		super(message, cause);
	}
}
