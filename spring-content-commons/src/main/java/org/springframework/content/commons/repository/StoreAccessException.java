package org.springframework.content.commons.repository;

import org.springframework.core.NestedRuntimeException;

/**
 * @deprecated This class is deprecated. Use {@link org.springframework.content.commons.store.StoreAccessException} instead.
 */
public class StoreAccessException extends NestedRuntimeException {

	public StoreAccessException(String msg) {
		super(msg);
	}

	public StoreAccessException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
