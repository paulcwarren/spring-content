package internal.org.springframework.content.rest.utils;

import org.springframework.content.common.storeservice.ContentStoreInfo;
import org.springframework.content.common.storeservice.ContentStoreService;

import internal.org.springframework.content.rest.annotations.ContentStoreRestResource;

public final class ContentStoreUtils {

	private ContentStoreUtils() {}
	
	public static ContentStoreInfo findContentStore(ContentStoreService stores, Class<?> contentEntityClass) {
		
		for (ContentStoreInfo info : stores.getContentStores()) {
			ContentStoreRestResource restResource = (ContentStoreRestResource) info.getInterface().getAnnotation(ContentStoreRestResource.class);
			if (restResource != null && info.getDomainObjectClass().equals(contentEntityClass))
				return info;
		}
		return null;
	}

}
