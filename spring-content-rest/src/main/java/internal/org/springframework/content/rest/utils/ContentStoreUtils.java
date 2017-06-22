package internal.org.springframework.content.rest.utils;

import org.atteo.evo.inflector.English;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.storeservice.ContentStoreInfo;
import org.springframework.content.commons.storeservice.ContentStoreService;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;

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

	public static ContentStoreInfo findContentStore(ContentStoreService stores, String store) {
		
		for (ContentStoreInfo info : stores.getStores(ContentStore.class)) {
			if (store.equals(storePath(info))) {
				return info;
			}
		}
		return null;
	}

	public static ContentStoreInfo findStore(ContentStoreService stores, String store) {
		for (ContentStoreInfo info : stores.getStores(Store.class)) {
			if (store.equals(storePath(info))) {
				return info;
			}
		}
		return null;
	}
	
	public static String storePath(ContentStoreInfo info) {
		Class<?> clazz = info.getInterface();
		ContentStoreRestResource annotation = AnnotationUtils.findAnnotation(clazz, ContentStoreRestResource.class);
		String path = annotation == null ? null : annotation.path().trim();
		path = StringUtils.hasText(path) ? path : English.plural(StringUtils.uncapitalize(getSimpleName(info)));
		return path;
	}
	
	public static String getSimpleName(ContentStoreInfo info) {
		Class<?> clazz = info.getDomainObjectClass();
		return clazz != null ? clazz.getSimpleName() : stripStoreName(info.getImplementation(Store.class));
	}

	public static String stripStoreName(Store implementation) {
		return implementation.getClass().getSimpleName().replaceAll("Store", "");
	}
}
