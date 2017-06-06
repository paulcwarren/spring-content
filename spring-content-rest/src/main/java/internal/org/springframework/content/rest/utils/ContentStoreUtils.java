package internal.org.springframework.content.rest.utils;

import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.storeservice.ContentStoreInfo;
import org.springframework.content.commons.storeservice.ContentStoreService;

import internal.org.springframework.content.rest.annotations.ContentStoreRestResource;

public final class ContentStoreUtils {

	private ContentStoreUtils() {}
	
	public static ContentStoreInfo findContentStore(ContentStoreService stores, Class<?> contentEntityClass) {
		
		for (ContentStoreInfo info : stores.getStores(ContentStore.class)) {
			ContentStoreRestResource restResource = (ContentStoreRestResource) info.getInterface().getAnnotation(ContentStoreRestResource.class);
			if (restResource != null && contentEntityClass.equals(info.getDomainObjectClass()))
				return info;
		}
		return null;
	}

	public static ContentStoreInfo findContentStore(ContentStoreService stores, String path) {
		
		for (ContentStoreInfo info : stores.getStores(ContentStore.class)) {
			ContentStoreRestResource restResource = (ContentStoreRestResource) info.getInterface().getAnnotation(ContentStoreRestResource.class);
			if (restResource != null && restResource.path().trim().length() > 0 && path.startsWith(restResource.path()))
				return info;
		}
		return null;
	}

	public static ContentStoreInfo findStore(ContentStoreService stores, String path) {
		
		for (ContentStoreInfo info : stores.getStores(Store.class)) {
			ContentStoreRestResource restResource = (ContentStoreRestResource) info.getInterface().getAnnotation(ContentStoreRestResource.class);
			if (restResource != null && restResource.path().trim().length() > 0 && path.startsWith(restResource.path()))
				return info;
		}
		return null;
	}
}
