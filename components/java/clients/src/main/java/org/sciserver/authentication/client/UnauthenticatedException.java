package org.sciserver.authentication.client;

public class UnauthenticatedException extends Exception {

    public UnauthenticatedException() {
        super("User not authenticated!");
    }
    public UnauthenticatedException(String message) {
        super(message);
    }

}
