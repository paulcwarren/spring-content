package org.springframework.content.commons.repository.factory;

import java.io.Serializable;

import org.springframework.content.commons.repository.ContentStore;

public interface ContentStoreFactory {

	public Class<? extends ContentStore<Object,Serializable>> getContentStoreInterface();
	public ContentStore<Object,Serializable> getContentStore();
	
}
