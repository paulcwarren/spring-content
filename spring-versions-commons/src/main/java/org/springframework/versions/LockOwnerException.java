package org.springframework.versions;

import org.springframework.core.NestedRuntimeException;
import org.springframework.lang.Nullable;

public class LockOwnerException extends NestedRuntimeException {

    public LockOwnerException(String msg) {
        super(msg);
    }

    public LockOwnerException(@Nullable String msg, @Nullable Throwable cause) {
        super(msg, cause);
    }
}
