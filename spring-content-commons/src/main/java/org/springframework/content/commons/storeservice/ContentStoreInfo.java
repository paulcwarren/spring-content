package org.springframework.content.commons.storeservice;

import java.io.Serializable;

import org.springframework.content.commons.repository.ContentStore;

public interface ContentStoreInfo {
	
	public Class<?> getInterface();
	public Class<?> getDomainObjectClass();
	public ContentStore<Object, Serializable> getImpementation();
	
}
