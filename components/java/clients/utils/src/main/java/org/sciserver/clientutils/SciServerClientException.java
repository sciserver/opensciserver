package org.sciserver.clientutils;

public class SciServerClientException extends Exception {

    private int httpCode;

    public SciServerClientException(String message, int code) {
        super(message);
        this.httpCode = code;
    }

    public int httpCode() {
        return httpCode;
    }
}
