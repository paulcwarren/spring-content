package internal.org.springframework.content.commons.store.factory;

import org.springframework.content.commons.repository.Store;

public interface StoreFactory {

	public Class<? extends Store> getStoreInterface();

	public <T> T getStore();

}
