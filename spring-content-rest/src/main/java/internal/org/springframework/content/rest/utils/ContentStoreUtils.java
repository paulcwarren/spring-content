package internal.org.springframework.content.rest.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;

import org.atteo.evo.inflector.English;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.OriginalFileName;
import org.springframework.content.commons.io.MedializedResource;
import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.storeservice.ContentStoreInfo;
import org.springframework.content.commons.storeservice.ContentStoreService;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

import internal.org.springframework.content.commons.renditions.RenditionContext;
import internal.org.springframework.content.rest.StoreRestResource;
import internal.org.springframework.content.rest.annotations.ContentStoreRestResource;

public final class ContentStoreUtils {

	private ContentStoreUtils() {}
	
	/**
	 * Given a store and a collection of mime types this method will iterate the
	 * mime-types returning the first input stream that it can find from the store
	 * itself or, if the store implements Renderable from a rendition.
	 * 
	 * @param store
	 *            store the store to fetch the content from
	 * @param mimeTypes
	 *            the mime types requested
	 * @param entity
	 *            the entity whose content is being fetched
	 * @param headers
	 *            headers that will be sent back to the client
	 * 
	 * @return input stream
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public static InputStream getContent(ContentStore<Object, Serializable> store, Object entity,
			List<MediaType> mimeTypes, HttpHeaders headers) {
		InputStream content = null;
		String cName = "content";
		String httpNameHeader = "x-file-name";

		Object entityMimeType = BeanUtils.getFieldWithAnnotation(entity,
				org.springframework.content.commons.annotations.MimeType.class);
		if (entityMimeType == null)
			return content;

		// content name
		Field contentName = BeanUtils.getFieldWithAnnotationField(entity,
				org.springframework.content.commons.annotations.ContentName.class);
		if (contentName != null) {
			Annotation cntNameAn = contentName
					.getAnnotation(org.springframework.content.commons.annotations.ContentName.class);
			cName = BeanUtils.getFieldValue(entity, contentName).toString();
			if (cntNameAn != null) {
				httpNameHeader = AnnotationUtils.getValue(cntNameAn, "httpHeader").toString();
			}
		}

		MediaType targetMimeType = MediaType.valueOf(entityMimeType.toString());

		MediaType.sortBySpecificityAndQuality(mimeTypes);

		MediaType[] arrMimeTypes = mimeTypes.toArray(new MediaType[] {});

		// Modified to show download
		Object originalFileName = BeanUtils.getFieldWithAnnotation(entity, OriginalFileName.class);
		if (originalFileName != null) {
			headers.setContentDispositionFormData("attachment", (String) originalFileName);
		}

		for (int i = 0; i < arrMimeTypes.length && content == null; i++) {
			MediaType mimeType = arrMimeTypes[i];
			if (!(mimeType.isConcrete() && targetMimeType.isConcrete()
					&& mimeType.getType().equals(targetMimeType.getType())
					&& mimeType.getSubtype().equals(targetMimeType.getSubtype()) && !mimeType.equals(targetMimeType)) && // If
																															// the
																															// same
																															// but
																															// with
																															// parameters,
																															// use
																															// renderer...
					mimeType.includes(targetMimeType)) { // ... or load original in compatibility mode
				headers.setContentType(targetMimeType);

				// content name header
				headers.set(httpNameHeader, cName);

				// long contentLength = 0L;
				Object len = BeanUtils.getFieldWithAnnotation(entity, ContentLength.class);
				if (len != null)
					headers.setContentLength(Long.parseLong(len.toString()));

				content = store.getContent(entity);
				break;
			} else if (store instanceof Renderable) {
				Resource ret = ((Renderable<Object>) store).getRendition(entity, mimeType.toString());
				if (ret != null) {
					// we have to use mime arrived from rendition due to it can be one of possible
					// requested
					String mt = (ret instanceof MedializedResource) ? ((MedializedResource) ret).getMime()
							: "application/octet-stream";
					headers.setContentType(MediaType.valueOf(mt));

					// determine file extension TODO: make it better!!!
					String mtExt = ".new"
					// String mtExt = allMimeTypes.forName(mime.toString()).getExtension();

					// content name header
					headers.set(httpNameHeader, cName + mtExt);

					try {
						content = ret.getInputStream();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();

						content = null;
					}
				}
			}
		}
		return content;
	}

	public static ContentStoreInfo findContentStore(ContentStoreService stores, Class<?> contentEntityClass) {

		for (ContentStoreInfo info : stores.getStores(ContentStore.class)) {
			if (contentEntityClass.equals(info.getDomainObjectClass()))
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
		String path = null;

		ContentStoreRestResource oldAnnotation = AnnotationUtils.findAnnotation(clazz, ContentStoreRestResource.class);
		if (oldAnnotation != null) {
			path = oldAnnotation == null ? null : oldAnnotation.path().trim();
		} else {
			StoreRestResource newAnnotation = AnnotationUtils.findAnnotation(clazz, StoreRestResource.class);
			path = newAnnotation == null ? null : newAnnotation.path().trim();
		}
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
