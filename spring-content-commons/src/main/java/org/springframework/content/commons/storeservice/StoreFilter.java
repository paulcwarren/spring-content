package org.springframework.content.commons.storeservice;

public interface StoreFilter {
	public boolean matches(ContentStoreInfo info);
}
