package internal.org.springframework.content.rest.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import internal.org.springframework.content.rest.annotations.ContentStoreRestResource;
import org.atteo.evo.inflector.English;

import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.storeservice.*;
import org.springframework.content.rest.StoreRestResource;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.StringUtils;

import static org.springframework.util.StringUtils.trimTrailingCharacter;

public final class StoreUtils {

	private StoreUtils() {
	}

	public static StoreFilter withStorePath(String storePath) {
		return new StoreFilter() {
			@Override
			public String name() {
				return storePath;
			}
			@Override
			public boolean matches(StoreInfo info) {
				return storePath.equals(storePath(info));
			}
		};
	}

	public static String storePath(StoreInfo info) {
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

	public static String getSimpleName(StoreInfo info) {
		Class<?> clazz = info.getDomainObjectClass();
		return clazz != null ? clazz.getSimpleName()
				: stripStoreName(info.getInterface());
	}

	public static String stripStoreName(Class<?> iface) {
		return iface.getSimpleName().replaceAll("Store", "");
	}

	public static String propertyName(String fieldName) {
		String name = stripId(fieldName);
		name = stripLength(name);
		return stripMimeType(name);
	}

	private static String stripId(String fieldName) {
		String name = fieldName.replaceAll("_Id", "");
		name = name.replaceAll("Id", "");
		name = name.replaceAll("_id", "");
		return name.replaceAll("id", "");
	}

	private static String stripLength(String fieldName) {
		String name = fieldName.replaceAll("_Length", "");
		name = name.replaceAll("Length", "");
		name = name.replaceAll("_length", "");
		name = name.replaceAll("length", "");
		name = fieldName.replaceAll("_len", "");
		name = name.replaceAll("Len", "");
		name = name.replaceAll("_len", "");
		return name.replaceAll("len", "");
	}

	private static String stripMimeType(String fieldName) {
		String name = fieldName.replaceAll("_MimeType", "");
		name = name.replaceAll("_Mime_Type", "");
		name = name.replaceAll("MimeType", "");
		name = name.replaceAll("_mimetype", "");
		name = name.replaceAll("_mime_type", "");
		name = name.replaceAll("mimetype", "");
		name = fieldName.replaceAll("_ContentType", "");
		name = name.replaceAll("_Content_Type", "");
		name = name.replaceAll("ContentType", "");
		name = name.replaceAll("_contenttype", "");
		name = name.replaceAll("_content_type", "");
		return name.replaceAll("contenttype", "");
	}

	public static String storeLookupPath(String lookupPath, URI baseUri) {

		Assert.notNull(lookupPath, "Lookup path must not be null!");

		// Temporary fix for SPR-13455
		lookupPath = lookupPath.replaceAll("//", "/");

		lookupPath = trimTrailingCharacter(lookupPath, '/');

		if (baseUri.isAbsolute()) {
			throw new UnsupportedOperationException("Absolute BaseUri is not supported");
		}

		String uri = baseUri.toString();

		if (!StringUtils.hasText(uri)) {
			return lookupPath;
		}

		uri = uri.startsWith("/") ? uri : "/".concat(uri);
		return lookupPath.startsWith(uri) ? lookupPath.substring(uri.length(), lookupPath.length()) : null;
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

	public static class NonExistentResource implements Resource {

		@Override
		public boolean exists() {
			return false;
		}

		@Override
		public URL getURL() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public URI getURI() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public File getFile() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public long contentLength() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public long lastModified() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public Resource createRelative(String s) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getFilename() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getDescription() {
			throw new UnsupportedOperationException();
		}

		@Override
		public InputStream getInputStream() throws IOException {
			throw new UnsupportedOperationException();
		}
	}
}
