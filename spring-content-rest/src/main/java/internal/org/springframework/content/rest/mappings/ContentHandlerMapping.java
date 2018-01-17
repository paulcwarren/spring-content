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
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.storeservice.ContentStoreInfo;
import org.springframework.content.commons.storeservice.ContentStoreService;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.UrlPathHelper;

import internal.org.springframework.content.rest.annotations.ContentRestController;
import internal.org.springframework.content.rest.utils.ContentStoreUtils;

public class ContentHandlerMapping extends RequestMappingHandlerMapping {
	
	private static MediaType hal = MediaType.parseMediaType("application/hal+json");
	private static MediaType json = MediaType.parseMediaType("application/json");

	private ContentStoreService contentStores;
	
	public ContentHandlerMapping(ContentStoreService contentStores) {
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
		if (path.length < 3)
			return null;
		
		ContentStoreInfo info2 = ContentStoreUtils.findStore(contentStores, path[1]);
		if (info2 != null && isHalOrJsonRequest(request) == false) {
			return super.lookupHandlerMethod(lookupPath, request);
		}
		return null; 
	}

	@Override
	protected CorsConfiguration getCorsConfiguration(Object handler, HttpServletRequest request) {
		CorsConfiguration corsConfiguration = super.getCorsConfiguration(handler, request);
		String lookupPath = getUrlPathHelper().getLookupPathForRequest(request);

		String[] path = lookupPath.split("/");
		if (path.length < 3)
			return corsConfiguration;
		
		ContentStoreInfo info2 = ContentStoreUtils.findStore(contentStores, path[1]);
		if (info2 == null) {
			return corsConfiguration;
		}

		CorsConfigurationBuilder builder = new CorsConfigurationBuilder();
		CorsConfiguration storeCorsConfiguration = builder.build(info2.getInterface());  
		
		return corsConfiguration == null ? storeCorsConfiguration
				: corsConfiguration.combine(storeCorsConfiguration);
	}

	private boolean isHalOrJsonRequest(HttpServletRequest request) {
		String method = request.getMethod();
		if ("GET".equals(method) || "DELETE".equals(method)) {
			String accept = request.getHeader("Accept");
			if (accept != null) {
				try {
					List<MediaType> mediaTypes = MediaType.parseMediaTypes(accept);
					for (MediaType mediaType : mediaTypes) {
						if (mediaType.equals(hal) || mediaType.equals(json)) {
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
						if (mediaType.equals(hal) || mediaType.equals(json)) {
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
			return new StoreCondition(typeAnnotation, this.contentStores, method);
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
		private Method method;
		
		public StoreCondition(StoreType typeAnnotation, ContentStoreService stores, Method method) {
			storeType = typeAnnotation.value();
			this.stores = stores;
			this.method = method;
		}

		@Override
		public StoreCondition combine(StoreCondition other) {
			throw new UnsupportedOperationException();
		}

		@Override
		public StoreCondition getMatchingCondition(HttpServletRequest request) {
			String path = new UrlPathHelper().getPathWithinApplication(request);
			String[] segments = path.split("/");
			if (segments.length < 3) {
				return null;
			}
			ContentStoreInfo info = ContentStoreUtils.findStore(stores, segments[1]);
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
			String[] segments = path.split("/");
			if (segments.length < 3) {
				return false;
			}
			ContentStoreInfo info = ContentStoreUtils.findStore(stores, segments[1]);
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
