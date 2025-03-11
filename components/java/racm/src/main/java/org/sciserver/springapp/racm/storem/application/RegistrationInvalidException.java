package org.sciserver.springapp.racm.storem.application;

public class RegistrationInvalidException extends RuntimeException {
	private static final long serialVersionUID = 2900266892086200066L;

	public RegistrationInvalidException(String message) {
		super(message);
	}
}
