package internal.com.emc.spring.content.rest.utils;

import com.emc.spring.content.commons.storeservice.ContentStoreInfo;
import com.emc.spring.content.commons.storeservice.ContentStoreService;

import internal.com.emc.spring.content.rest.annotations.ContentStoreRestResource;

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
