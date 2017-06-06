package internal.org.springframework.content.rest.mappings;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FilenameUtils;
import org.springframework.content.commons.annotations.Content;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.storeservice.ContentStoreInfo;
import org.springframework.content.commons.storeservice.ContentStoreService;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.UrlPathHelper;

import internal.org.springframework.content.rest.annotations.ContentRestController;
import internal.org.springframework.content.rest.utils.ContentStoreUtils;
import internal.org.springframework.content.rest.utils.PersistentEntityUtils;
import internal.org.springframework.content.rest.utils.RepositoryUtils;

public class ContentHandlerMapping extends RequestMappingHandlerMapping {
	
	private static MediaType halJson = MediaType.parseMediaType("application/hal+json");

	private Repositories repositories = null;
	private ResourceMappings repositoryMappings;
	private ContentStoreService contentStores;
	
	public ContentHandlerMapping(Repositories repositories, ResourceMappings repositoryMappings, ContentStoreService contentStores) {
		this.repositories = repositories;
		this.repositoryMappings = repositoryMappings;
		this.contentStores = contentStores;
		setOrder(Ordered.LOWEST_PRECEDENCE - 200);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping#isHandler(java.lang.Class)
	 */
	@Override
	protected boolean isHandler(Class<?> beanType) {
		return AnnotationUtils.findAnnotation(beanType, ContentRestController.class) != null;
	}

	/* (non-Javadoc)
	 * @see org.springframework.web.servlet.handler.AbstractHandlerMethodMapping#lookupHandlerMethod(java.lang.String, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected HandlerMethod lookupHandlerMethod(String lookupPath, HttpServletRequest request) 
			throws Exception {
		
		// is a content property, if so look up a handler method?
		String[] path = lookupPath.split("/");
		if (path.length < 3 )
			return null;
		
		ContentStoreInfo info2 = ContentStoreUtils.findStore(contentStores, lookupPath);
		if (info2 != null && isHalRequest(request) == false) {
			return super.lookupHandlerMethod(lookupPath, request);
		}

		ResourceMetadata mapping = RepositoryUtils.findRepositoryMapping(repositories, repositoryMappings, path[1]);
		if (mapping == null)
			return null;
		
		Class<?> domainType = mapping.getDomainType();
		
		PersistentEntity<?,?> entity = PersistentEntityUtils.findPersistentEntity(repositories, domainType);
		if (null == entity)
			return null;
		
		if (isContentEntityRequestMapping(path)) {
			if (isHalRequest(request)) {
				return null;
			} else {
				ContentStoreInfo info = ContentStoreUtils.findContentStore(contentStores, entity.getType());
				if (info != null) {
					return super.lookupHandlerMethod(lookupPath, request);
				}
			}
		} else if (isContentPropertyRequestMapping(path)) {
			PersistentProperty<?> prop = entity.getPersistentProperty(path[3]);
			if (null != prop) {
				if (prop.isArray() || prop.isCollectionLike()) {
					Class<?> fieldType = prop.getComponentType();
					ContentStoreInfo info = ContentStoreUtils.findContentStore(contentStores, fieldType);
					if (info != null) {
						return super.lookupHandlerMethod(lookupPath, request);
					}
				} else {
					Class<?> fieldType = prop.getRawType();
					ContentStoreInfo info = ContentStoreUtils.findContentStore(contentStores, fieldType);
					if (info != null) {
						return super.lookupHandlerMethod(lookupPath, request);
					}
				}
			} else {
				if (entity.getType().isAnnotationPresent(Content.class)) {
					return super.lookupHandlerMethod(lookupPath, request);
				}
			}
		}
		return null; 
	}

	private boolean isContentPropertyRequestMapping(String[] path) {
		return path.length > 3;
	}

	private boolean isContentEntityRequestMapping(String[] path) {
		return path.length == 3;
	}

	private boolean isHalRequest(HttpServletRequest request) {
		String method = request.getMethod();
		if ("GET".equals(method) || "DELETE".equals(method)) {
			String accept = request.getHeader("Accept");
			if (accept != null) {
				try {
					List<MediaType> mediaTypes = MediaType.parseMediaTypes(accept);
					for (MediaType mediaType : mediaTypes) {
						if (mediaType.equals(halJson)) {
							return true;
						}
					}
				} catch (InvalidMediaTypeException imte) {
					return true;
				}
			}
		} else if ("PUT".equals(method) || "POST".equals(method)) {
			String contentType = request.getHeader("Content-Type");
			if (contentType != null) {
				try {
					List<MediaType> mediaTypes = MediaType.parseMediaTypes(contentType);
						for (MediaType mediaType : mediaTypes) {
						if (mediaType.equals(halJson)) {
							return true;
						}
					}
				} catch (InvalidMediaTypeException imte) {
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	protected void detectHandlerMethods(final Object handler) {
		super.detectHandlerMethods(handler);
	}
	
	/**
	 * Store requests have to be handled by different RequestMappings
	 * based on whether the request is targeting a Store or content associated
	 * with an Entity
	 */
	@Override
	protected RequestCondition<?> getCustomMethodCondition(Method method) {
		StoreType typeAnnotation = AnnotationUtils.findAnnotation(method, StoreType.class);
		if (typeAnnotation != null) {
			return new StoreCondition(typeAnnotation, this.contentStores);
		}
		return null;
	}

	
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	public static @interface StoreType {
		String value() default "store";
	}
	
	public static class StoreCondition implements RequestCondition<StoreCondition> {
		
		private String storeType = "store";
		private ContentStoreService stores;
		
		public StoreCondition(StoreType typeAnnotation, ContentStoreService stores) {
			storeType = typeAnnotation.value();
			this.stores = stores;
		}

		@Override
		public StoreCondition combine(StoreCondition other) {
			throw new UnsupportedOperationException();
		}

		@Override
		public StoreCondition getMatchingCondition(HttpServletRequest request) {
			String path = new UrlPathHelper().getPathWithinApplication(request);
			ContentStoreInfo info = ContentStoreUtils.findStore(stores, path);
			if (info != null && 
					(Store.class.isAssignableFrom(info.getInterface()) && "store".equals(storeType)) ||
					(ContentStore.class.isAssignableFrom(info.getInterface()) && "contentstore".equals(storeType))
				) {
				return this;
			}

			return null;
		}

		@Override
		public int compareTo(StoreCondition other, HttpServletRequest request) {
			if (this.isMappingForRequest(request) && other.isMappingForRequest(request) == false)
				return 1;
			else if (this.isMappingForRequest(request) == false && other.isMappingForRequest(request))
				return -1;
			else {
				String path = new UrlPathHelper().getPathWithinApplication(request);
				String filename = FilenameUtils.getName(path);
				String extension = FilenameUtils.getExtension(filename);
				if (extension != null && "store".equals(storeType)) {
					return -1;
				} else if (extension != null && "contentstore".equals(storeType)) {
					return 1;
				}
				return 0;
			}
		}
		
		public boolean isMappingForRequest(HttpServletRequest request) {
			String path = new UrlPathHelper().getPathWithinApplication(request);
			ContentStoreInfo info = ContentStoreUtils.findStore(stores, path);
			if (info != null && 
					(Store.class.isAssignableFrom(info.getInterface()) && "store".equals(storeType)) ||
					(ContentStore.class.isAssignableFrom(info.getInterface()) && "contentstore".equals(storeType))
				) {
				return true;
			} 
			return false;
		}
	}
}
