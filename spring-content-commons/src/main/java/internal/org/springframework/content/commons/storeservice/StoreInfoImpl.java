package internal.org.springframework.content.commons.storeservice;

import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.storeservice.StoreInfo;

import java.io.Serializable;

public class StoreInfoImpl implements StoreInfo {

	private Class<?> storeInterface;
	private Class<?> storeDomainClass;
	private Object storeImpl;

	public StoreInfoImpl(Class<?> interfaceClass, Class<?> storeDomainClass,
						 Store<Serializable> storeImpl) {
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

	@Override
	public <T> T getImplementation(Class<? extends T> clazz) {
		if (clazz.isAssignableFrom(storeImpl.getClass())) {
			return (T) storeImpl;
		}
		return null;
	}
}
