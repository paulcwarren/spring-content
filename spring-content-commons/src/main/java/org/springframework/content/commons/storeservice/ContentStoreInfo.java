package org.springframework.content.commons.storeservice;

import java.io.Serializable;

import org.springframework.content.commons.repository.ContentStore;

public interface ContentStoreInfo {
	
	Class<? extends ContentStore<Object, Serializable>> getInterface();
	Class<?> getDomainObjectClass();
	ContentStore<Object, Serializable> getImplementation();
	
}
