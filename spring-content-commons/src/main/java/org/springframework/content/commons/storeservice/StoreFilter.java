package org.springframework.content.commons.storeservice;

public interface StoreFilter {
	String name();
	boolean matches(StoreInfo info);
}
