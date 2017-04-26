package org.springframework.content.commons.repository.factory;

public interface StoreFactory {

	public Class<?> getStoreInterface();
	public <T> T getStore();
	
}
