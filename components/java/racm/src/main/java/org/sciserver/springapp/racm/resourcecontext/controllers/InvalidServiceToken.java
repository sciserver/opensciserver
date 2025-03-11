package org.sciserver.springapp.racm.resourcecontext.controllers;

public class InvalidServiceToken extends RuntimeException {
	private static final long serialVersionUID = -6376361757232864311L;
	public InvalidServiceToken() {
		super("Invalid service token");
	}
}