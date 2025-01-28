package org.sciserver.compute;

public class UnauthorizedException extends Exception {

	public UnauthorizedException(String string) {
		super(string);
	}

	public UnauthorizedException(String string, Throwable ex) {
		super(string, ex);
	}
}
