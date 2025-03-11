package org.sciserver.springapp.racm.admin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends RuntimeException {
	private static final long serialVersionUID = 2801502680185022978L;
	public ForbiddenException(String message){
		super(message);
	}
	public ForbiddenException() {
		this("You are not authorized to view this page");
	}
}