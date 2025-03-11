package org.sciserver.springapp.racm.utils.controller;

public class ResourceNotFoundException extends RuntimeException {
	private static final long serialVersionUID = 3263958043817060767L;

	public ResourceNotFoundException() {
	}

	public ResourceNotFoundException(String message) {
		super(message);
	}
}
