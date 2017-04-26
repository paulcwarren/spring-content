package org.springframework.content.commons.repository;

import org.springframework.core.NestedRuntimeException;

public class StoreAccessException extends NestedRuntimeException {

    public StoreAccessException(String msg) {
        super(msg);
    }

    public StoreAccessException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
