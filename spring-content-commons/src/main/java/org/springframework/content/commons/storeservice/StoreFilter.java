package org.springframework.content.commons.storeservice;

public interface StoreFilter {
	boolean matches(StoreInfo info);
}
