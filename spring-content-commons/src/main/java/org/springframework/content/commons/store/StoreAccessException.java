package org.springframework.content.commons.store;

public class StoreAccessException extends org.springframework.content.commons.repository.StoreAccessException {

	public StoreAccessException(String msg) {
		super(msg);
	}

	public StoreAccessException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
