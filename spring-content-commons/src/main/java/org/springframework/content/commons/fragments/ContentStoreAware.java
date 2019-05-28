package org.springframework.content.commons.fragments;

import org.springframework.content.commons.repository.ContentStore;

public interface ContentStoreAware {

	void setDomainClass(Class<?> domainClass);
	void setIdClass(Class<?> idClass);
	void setContentStore(ContentStore store);

}
