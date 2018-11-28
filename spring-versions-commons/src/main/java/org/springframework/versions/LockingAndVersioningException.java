package org.springframework.versions;

public class LockingAndVersioningException extends RuntimeException {

    public LockingAndVersioningException() {
    }

    public LockingAndVersioningException(String message) {
        super(message);
    }

    public LockingAndVersioningException(String message, Throwable cause) {
        super(message, cause);
    }

    public LockingAndVersioningException(Throwable cause) {
        super(cause);
    }

    public LockingAndVersioningException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
