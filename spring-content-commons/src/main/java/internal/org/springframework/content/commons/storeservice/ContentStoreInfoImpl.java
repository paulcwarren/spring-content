package internal.org.springframework.content.commons.storeservice;

import java.io.Serializable;

import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.storeservice.ContentStoreInfo;

public class ContentStoreInfoImpl implements ContentStoreInfo {

	private Class<?> storeInterface;
	private Class<?> storeDomainClass;
	private Object storeImpl;
	
	public ContentStoreInfoImpl(Class<?> interfaceClass, Class<?> storeDomainClass, Store<Serializable> storeImpl) {
		this.storeInterface = interfaceClass;
		this.storeDomainClass = storeDomainClass;
		this.storeImpl = storeImpl;
	}

	public ContentStoreInfoImpl(Class<?> interfaceClass, Class<?> storeDomainClass, ContentStore<Object,Serializable> storeImpl) {
		this.storeInterface = interfaceClass;
		this.storeDomainClass = storeDomainClass;
		this.storeImpl = storeImpl;
	}

	@Override
	public Class<?> getInterface() {
		return this.storeInterface;
	}

	@Override
	public Class<?> getDomainObjectClass() {
		return this.storeDomainClass;
	}

	@Deprecated
	@Override
	public ContentStore<Object,Serializable> getImpementation() {
		if (storeImpl instanceof ContentStore) {
			return (ContentStore<Object,Serializable>)this.storeImpl;
		} 
		return null;
	}

	@Override
	public <T> T getImplementation(Class<? extends T> clazz) {
		if (clazz.isAssignableFrom(storeImpl.getClass())) {
			return (T)storeImpl;
		}
		return null;
	}
}
