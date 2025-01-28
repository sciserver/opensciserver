package org.ivoa.dm;

public class VOURPException extends Exception {
    private static final long serialVersionUID = 4939306912232113495L;
    public static final int UNKNOWN_CAUSE = 1;
    public static final int ILLEGAL_STATE = 2;
    public static final int ILLEGAL_ARGUMENT = 4;
    public static final int CAUGHT_EXCEPTION = 8;
    public static final int UNAUTHORIZED = 16;
    private int errorCode = 0;

    public VOURPException(int code, String message, Throwable cause) {
        super(message, cause);
        if (cause != null)
            this.errorCode = code | CAUGHT_EXCEPTION;
        else
            this.errorCode = code;
    }

    public VOURPException(int code, String message) {
        this(code, message, null);
    }

    public VOURPException(int code, Throwable cause) {
        this(code, null, cause);
    }

    public VOURPException(String message) {
        this(UNKNOWN_CAUSE, message, null);
    }

    public VOURPException(Throwable cause) {
        this(0, null, cause);
    }

    public int getErrorCode() {
        return errorCode;
    }

}
