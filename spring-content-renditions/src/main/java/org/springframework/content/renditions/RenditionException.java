package org.springframework.content.renditions;

import org.springframework.core.NestedRuntimeException;

public class RenditionException extends NestedRuntimeException {
    public RenditionException(String msg) {
        super(msg);
    }

    public RenditionException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
