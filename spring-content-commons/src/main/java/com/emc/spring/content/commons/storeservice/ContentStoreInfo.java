package com.emc.spring.content.commons.storeservice;

import java.io.Serializable;

import com.emc.spring.content.commons.repository.ContentStore;

public interface ContentStoreInfo {
	
	public Class<? extends ContentStore<Object, Serializable>> getInterface();
	public Class<?> getDomainObjectClass();
	public ContentStore<Object, Serializable> getImpementation();
	
}
