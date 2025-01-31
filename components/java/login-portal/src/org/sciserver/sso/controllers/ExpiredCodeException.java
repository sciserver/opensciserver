package org.sciserver.sso.controllers;

import java.time.Instant;

class ExpiredCodeException extends Exception {
	private static final long serialVersionUID = -3420412826282744915L;
	private final Instant creationTime;
	ExpiredCodeException(Instant creationTime, String message) {
		super(message);
		this.creationTime = creationTime;
	}
	@Override
	public String toString() {
		return getClass().getName()
				+ ": " + getMessage()
				+ " - Code created at "
				+ creationTime.toString();
	}
}