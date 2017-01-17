package org.springframework.content.commons.repository;

import org.springframework.core.NestedRuntimeException;

public class ContentAccessException extends NestedRuntimeException {

    public ContentAccessException(String msg) {
        super(msg);
    }

    public ContentAccessException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
