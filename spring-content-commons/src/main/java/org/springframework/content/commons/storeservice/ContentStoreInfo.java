package org.springframework.content.commons.storeservice;

import java.io.Serializable;

import org.springframework.content.commons.repository.ContentStore;

public interface ContentStoreInfo {

	/**
	 * Returns the Store's interface class
	 * 
	 * @return interface class
	 */
	public Class<?> getInterface();

	/**
	 * Returns the Store's domain object class if applicable. In cases where the
	 * Store does not have a domain class, returns null
	 * 
	 * @return domain object class
	 */
	public Class<?> getDomainObjectClass();

	/**
	 * Returns the Store's implementation
	 * 
	 * @param clazz
	 *            the type of the implementation
	 * @return the implementation, or null
	 */
	public <T> T getImplementation(Class<? extends T> clazz);

	/**
	 * Returns the Store's implementation
	 * 
	 * @return the store's implementation (implementing ContentStore), or null
	 */
	@Deprecated
	public ContentStore<Object, Serializable> getImpementation();

}
