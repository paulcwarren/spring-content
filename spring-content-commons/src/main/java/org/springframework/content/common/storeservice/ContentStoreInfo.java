package org.springframework.content.common.storeservice;

import java.io.Serializable;

import org.springframework.content.common.repository.ContentStore;

public interface ContentStoreInfo {
	
	public Class<? extends ContentStore<Object, Serializable>> getInterface();
	public Class<?> getDomainObjectClass();
	public ContentStore<Object, Serializable> getImpementation();
	
}
