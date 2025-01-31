package org.sciserver.sso.controllers;

public class TooManyLoginAttemptsException extends Exception {
	private static final long serialVersionUID = -7710964799393091084L;
	public TooManyLoginAttemptsException(String message) {
		super(message);
	}
	public TooManyLoginAttemptsException(String message, Throwable cause) {
		super(message, cause);
	}
}