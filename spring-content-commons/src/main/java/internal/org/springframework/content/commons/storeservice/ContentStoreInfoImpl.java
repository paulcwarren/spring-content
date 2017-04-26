package internal.org.springframework.content.commons.storeservice;

import java.io.Serializable;

import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.storeservice.ContentStoreInfo;

public class ContentStoreInfoImpl implements ContentStoreInfo {

	private Class<?> storeInterface;
	private Class<?> storeDomainClass;
	private ContentStore<Object,Serializable> storeImpl;
	
	public ContentStoreInfoImpl(Class<?> class1, Class<?> storeDomainClass, ContentStore<Object,Serializable> storeImpl) {
		this.storeInterface = class1;
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

	@Override
	public ContentStore<Object,Serializable> getImpementation() {
		return this.storeImpl;
	}
	
}
