package internal.org.springframework.content.commons.storeservice;

import java.io.Serializable;
import java.util.function.Supplier;

import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.storeservice.StoreInfo;

public class StoreInfoImpl implements StoreInfo {

	private Class<?> storeInterface;
	private Class<?> storeDomainClass;
	private Object storeImpl;
    private Supplier<Store<Serializable>> storeSupplier;

	public StoreInfoImpl(Class<?> interfaceClass, Class<?> storeDomainClass,
						 Store<Serializable> storeImpl) {
		this.storeInterface = interfaceClass;
		this.storeDomainClass = storeDomainClass;
		this.storeImpl = storeImpl;
	}

    public StoreInfoImpl(Class<?> interfaceClass, Class<?> storeDomainClass, Supplier<Store<Serializable>> storeSupplier) {
        this.storeInterface = interfaceClass;
        this.storeDomainClass = storeDomainClass;
        this.storeSupplier = storeSupplier;
    }

	@Override
	public Class<?> getInterface() {
		return this.storeInterface;
	}

	@Override
	public Class<?> getDomainObjectClass() {
		return this.storeDomainClass;
	}

	@SuppressWarnings("unchecked")
    @Override
	public <T> T getImplementation(Class<? extends T> clazz) {

	    if (storeImpl == null) {
	        storeImpl = storeSupplier.get();
	    }

		if (storeImpl != null && clazz.isAssignableFrom(storeImpl.getClass())) {
			return (T) storeImpl;
		}

		return null;
	}
}
