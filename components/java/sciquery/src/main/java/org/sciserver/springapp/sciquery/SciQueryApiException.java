package org.sciserver.springapp.sciquery;

/**
 * Custom exception class for handling errors in the SciQuery API.
 */
public class SciQueryApiException extends Exception {
    public SciQueryApiException() {
        super();
    }

    public SciQueryApiException(String message) {
        super(message);
    }

    public SciQueryApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public SciQueryApiException(Throwable cause) {
        super(cause);
    }
}
