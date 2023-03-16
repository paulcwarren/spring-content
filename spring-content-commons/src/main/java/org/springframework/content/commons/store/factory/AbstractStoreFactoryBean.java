package org.springframework.content.commons.store.factory;

import org.springframework.content.commons.repository.Store;

public abstract class AbstractStoreFactoryBean
		extends org.springframework.content.commons.repository.factory.AbstractStoreFactoryBean {

	protected AbstractStoreFactoryBean(Class<? extends Store> storeInterface) {
		super(storeInterface);
	}
}
