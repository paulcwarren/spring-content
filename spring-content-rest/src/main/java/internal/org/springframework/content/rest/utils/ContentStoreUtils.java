package internal.org.springframework.content.rest.utils;

import java.io.InputStream;
import java.io.Serializable;
import java.util.List;

import internal.org.springframework.content.rest.io.AssociatedResource;
import internal.org.springframework.content.rest.io.RenderedResource;
import org.atteo.evo.inflector.English;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.storeservice.ContentStoreInfo;
import org.springframework.content.commons.storeservice.ContentStoreService;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.rest.StoreRestResource;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;
import org.springframework.util.StringUtils;

import internal.org.springframework.content.rest.annotations.ContentStoreRestResource;

import static java.lang.String.format;

public final class ContentStoreUtils {

	private ContentStoreUtils() {
	}

	/**
	 * Given a store and a collection of mime types this method will iterate the
	 * mime-types to find a data source of mathcing content.  This might be the content itself
	 * or, if the store implements Renderable, a rendition.
	 * 
	 * @param store store the store to fetch the content from
	 * @param entity the entity whose content is being fetched
	 * @param property
	 * @param mimeTypes the requested mime types
	 * @return resource plan (a wrapper around the resource to serve and the resolved mimetype)
	 */
	@SuppressWarnings("unchecked")
	public static ResourcePlan resolveResource(ContentStore<Object, Serializable> store, Object entity, Object property, List<MediaType> mimeTypes) {
		Object entityMimeType = BeanUtils.getFieldWithAnnotation(property != null ? property : entity, org.springframework.content.commons.annotations.MimeType.class);
		if (entityMimeType == null)
			return null;

		MediaType targetMimeType = MediaType.valueOf(entityMimeType.toString());

		MediaType.sortBySpecificityAndQuality(mimeTypes);

		MediaType[] arrMimeTypes = mimeTypes.toArray(new MediaType[] {});

		Serializable contentId = (Serializable)BeanUtils.getFieldWithAnnotation(property != null ? property : entity, ContentId.class);

		Resource r = null;
		MimeType mimeType = null;
		for (int i = 0; i < arrMimeTypes.length /*&& content == null*/; i++) {
			mimeType = arrMimeTypes[i];
			if (mimeType.includes(targetMimeType)) {
				r = ((Store)store).getResource(contentId);
				mimeType = targetMimeType;
				break;
			}
			else if (store instanceof Renderable) {
				InputStream content = ((Renderable<Object>) store).getRendition(property != null ? property : entity, mimeType.toString());
				if (content != null) {
 					Resource original = ((Store)store).getResource(contentId);
					r = new RenderedResource(content, entity, original);
					break;
				}
			}
		}

		if (r != null) {
			r = new AssociatedResource(entity, r);
		}

		return new ResourcePlan(r, mimeType);
	}

	public static ContentStoreInfo findContentStore(ContentStoreService stores,
			Class<?> contentEntityClass) {

		for (ContentStoreInfo info : stores.getStores(ContentStore.class)) {
			if (contentEntityClass.equals(info.getDomainObjectClass()))
				return info;
		}
		return null;
	}

	public static ContentStoreInfo findContentStore(ContentStoreService stores,
			String store) {

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
		String path = null;

		ContentStoreRestResource oldAnnotation = AnnotationUtils.findAnnotation(clazz,
				ContentStoreRestResource.class);
		if (oldAnnotation != null) {
			path = oldAnnotation == null ? null : oldAnnotation.path().trim();
		}
		else {
			StoreRestResource newAnnotation = AnnotationUtils.findAnnotation(clazz,
					StoreRestResource.class);
			path = newAnnotation == null ? null : newAnnotation.path().trim();
		}
		path = StringUtils.hasText(path) ? path
				: English.plural(StringUtils.uncapitalize(getSimpleName(info)));
		return path;
	}

	public static String getSimpleName(ContentStoreInfo info) {
		Class<?> clazz = info.getDomainObjectClass();
		return clazz != null ? clazz.getSimpleName()
				: stripStoreName(info.getImplementation(Store.class));
	}

	public static String stripStoreName(Store implementation) {
		return implementation.getClass().getSimpleName().replaceAll("Store", "");
	}

	public static class ResourcePlan {

		private Resource resource;
		private MimeType mimeType;

		public ResourcePlan(Resource r, MimeType mimeType) {
			this.resource = r;
			this.mimeType = mimeType;
		}

		public Resource getResource() {
			return resource;
		}

		public MimeType getMimeType() {
			return mimeType;
		}
	}
}
