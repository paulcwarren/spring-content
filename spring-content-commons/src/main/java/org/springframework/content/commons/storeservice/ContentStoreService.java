package org.springframework.content.commons.storeservice;

public interface ContentStoreService {
	
	public ContentStoreInfo[] getStores(Class<?> storeType);

	@Deprecated
	public ContentStoreInfo[] getContentStores();
}
