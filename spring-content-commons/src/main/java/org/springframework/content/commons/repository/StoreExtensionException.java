package org.springframework.content.commons.repository;

import org.springframework.core.NestedRuntimeException;

public class StoreExtensionException extends NestedRuntimeException {

    public StoreExtensionException(String msg) {
        super(msg);
    }

    public StoreExtensionException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
