package com.emc.spring.content.commons.repository.factory;

import java.io.Serializable;

import com.emc.spring.content.commons.repository.ContentStore;

public interface ContentStoreFactory {

	public Class<? extends ContentStore<Object,Serializable>> getContentStoreInterface();
	public ContentStore<Object,Serializable> getContentStore();
	
}
