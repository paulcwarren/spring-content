package internal.org.springframework.content.rest.mappings;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import internal.org.springframework.content.rest.mappingcontext.ContentPropertyRequest;
import internal.org.springframework.content.rest.mappingcontext.ContentPropertyToRequestMappingContext;
import org.apache.commons.io.FilenameUtils;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.commons.storeservice.Stores;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.content.rest.config.RestConfiguration.Exclusions;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.MediaType;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.util.UrlPathHelper;

import internal.org.springframework.content.rest.annotations.ContentRestController;
import internal.org.springframework.content.rest.controllers.resolvers.EntityResolvers;
import internal.org.springframework.content.rest.utils.StoreUtils;

import static org.apache.commons.lang.StringUtils.join;

public class ContentHandlerMapping extends StoreAwareHandlerMapping {

	private static MediaType hal = MediaType.parseMediaType("application/hal+json");
	private static MediaType json = MediaType.parseMediaType("application/json");

	private Exclusions exclusions = null;
	private Stores contentStores;
	private EntityResolvers entityResolvers = null;
	private final ContentPropertyToRequestMappingContext requestMappingContext;

	public ContentHandlerMapping(Stores contentStores, EntityResolvers entityResolvers, ContentPropertyToRequestMappingContext requestMappingContext, RestConfiguration config) {
		super(config);
		initExclusions(exclusions, config);
		this.contentStores = contentStores;
		this.entityResolvers = entityResolvers;
		this.requestMappingContext = requestMappingContext;
		setOrder(Ordered.LOWEST_PRECEDENCE - 200);
	}

	private void initExclusions(Exclusions exclusions, RestConfiguration config) {
        this.exclusions = config.shortcutExclusions();
        if (this.exclusions.size() == 0) {
            this.exclusions
                .exclude("GET", MediaType.parseMediaType("application/json"))
                .exclude("GET", MediaType.parseMediaType("application/hal+json"))
                .exclude("DELETE", MediaType.parseMediaType("application/json"))
                .exclude("DELETE", MediaType.parseMediaType("application/hal+json"))
                .exclude("PUT", MediaType.parseMediaType("application/json"))
                .exclude("PUT", MediaType.parseMediaType("application/hal+json"))
                .exclude("POST", MediaType.parseMediaType("application/json"))
                .exclude("POST", MediaType.parseMediaType("application/hal+json"))
                .exclude("PATCH", MediaType.parseMediaType("*/*"))
                .exclude("HEAD", MediaType.parseMediaType("*/*"));
        }
    }

    /*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping#
	 * isHandler(java.lang.Class)
	 */
	@Override
	protected boolean isHandler(Class<?> beanType) {
		return AnnotationUtils.findAnnotation(beanType,ContentRestController.class) != null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.web.servlet.handler.AbstractHandlerMethodMapping#
	 * lookupHandlerMethod(java.lang.String, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected HandlerMethod lookupHandlerMethod(String lookupPath, HttpServletRequest request)
			throws Exception {

		String storeLookupPath = StoreUtils.storeLookupPath(lookupPath, this.getConfiguration().getBaseUri());

		if (storeLookupPath != null) {
			// is a content property, if so look up a handler method?
			String[] path = storeLookupPath.split("/");
			if (path.length < 3)
				return null;

			StoreInfo info2 = contentStores.getStore(Store.class, StoreUtils.withStorePath(path[1]));
			if (info2 != null) {

			    if (isFullyQualifiedContentPropertyRequest(path, info2)) {

					String resolvedContentPropertyPath = requestMappingContext.resolveContentPropertyPath(info2.getDomainObjectClass(), ContentPropertyRequest.from(storeLookupPath).getContentPropertyPath());
					String resolvedStoreLookupPath = ContentPropertyRequest.from(path[1], path[2], resolvedContentPropertyPath).getRequestURI();

					if (entityResolvers.hasPropertyFor(resolvedStoreLookupPath)) {
    			        return super.lookupHandlerMethod(lookupPath, request);
    			    }
			    } else if (this.getConfiguration().shortcutLinks()) {
    			    // for backward compatibility
    			    if (info2 != null && isExcludedShortcutContentPropertyRequest(request) == false) {
    	              return super.lookupHandlerMethod(lookupPath, request);
    			    }
			    }
			}
		}
		return null;
	}

	@Override
	protected boolean hasCorsConfigurationSource(Object handler) {
		return true;
	}

	@Override
	protected CorsConfiguration getCorsConfiguration(Object handler, HttpServletRequest request) {
		String lookupPath = getUrlPathHelper().getLookupPathForRequest(request);
		String storeLookupPath = StoreUtils.storeLookupPath(lookupPath, this.getConfiguration().getBaseUri());
		CorsConfiguration corsConfiguration = super.getCorsConfiguration(handler, request);

		if (storeLookupPath == null) {
			return corsConfiguration;
		}

		String[] path = storeLookupPath.split("/");
		if (path.length < 3)
			return corsConfiguration;

		StoreInfo info2 = contentStores.getStore(Store.class, StoreUtils.withStorePath(path[1]));
		if (info2 == null) {
			return corsConfiguration;
		}

		CorsConfigurationBuilder builder = new CorsConfigurationBuilder();
		CorsConfiguration storeCorsConfiguration = builder.build(info2.getInterface());

		return corsConfiguration == null ? storeCorsConfiguration : corsConfiguration.combine(storeCorsConfiguration);
	}

	private boolean isExcludedShortcutContentPropertyRequest(HttpServletRequest request) {
		String method = request.getMethod();

		List<MediaType> excludedMediaTypes = this.exclusions.get(method);
		if (excludedMediaTypes == null) {
		    return false;
		}

		String mediaTypes = "*/*";
		if ("GET".equals(method) || "DELETE".equals(method)) {
		    mediaTypes = request.getHeader("Accept");
		} else if ("PUT".equals(method) || "POST".equals(method)) {
            mediaTypes = request.getHeader("Content-Type");
		}

        List<MediaType> acceptedMediaTypes = MediaType.parseMediaTypes(mediaTypes);

        for (MediaType excludedMediaType : excludedMediaTypes) {
            for (MediaType acceptedMediaType : acceptedMediaTypes) {
    		    if (excludedMediaType.includes(acceptedMediaType)) {
    		        return true;
    		    }
    		}
        }

		return false;
	}

    private boolean isFullyQualifiedContentPropertyRequest(String[] path, StoreInfo info2) {
        return AssociativeStore.class.isAssignableFrom(info2.getInterface()) && path.length >= 4;
    }

	@Override
	protected void detectHandlerMethods(final Object handler) {
		super.detectHandlerMethods(handler);
	}

	/**
	 * Store requests have to be handled by different RequestMappings based on whether the
	 * request is targeting a Store or content associated with an Entity
	 */
	@Override
	protected RequestCondition<?> getCustomMethodCondition(Method method) {
		StoreType typeAnnotation = AnnotationUtils.findAnnotation(method,
				StoreType.class);
		if (typeAnnotation != null) {
			return new StoreCondition(typeAnnotation, this.contentStores, method, this.getConfiguration().getBaseUri());
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
		private Stores stores;
		private Method method;
		private URI baseUri;

		public StoreCondition(StoreType typeAnnotation, Stores stores, Method method, URI baseUri) {
			storeType = typeAnnotation.value();
			this.stores = stores;
			this.method = method;
			this.baseUri = baseUri;
		}

		@Override
		public StoreCondition combine(StoreCondition other) {
			throw new UnsupportedOperationException();
		}

		@Override
		public StoreCondition getMatchingCondition(HttpServletRequest request) {
			String path = new UrlPathHelper().getPathWithinApplication(request);

			String storeLookupPath = StoreUtils.storeLookupPath(path, baseUri);

			String[] segments = storeLookupPath.split("/");
			if (segments.length < 3) {
				return null;
			}
			StoreInfo info = stores.getStore(Store.class, StoreUtils.withStorePath(segments[1]));
			if (info != null
					&& ((Store.class.isAssignableFrom(info.getInterface())
							&& "store".equals(storeType))
					|| (ContentStore.class.isAssignableFrom(info.getInterface())
							&& "contentstore".equals(storeType)))
				) {
				return this;
			}

			return null;
		}

		@Override
		public int compareTo(StoreCondition other, HttpServletRequest request) {
			if (this.isMappingForRequest(request)
					&& other.isMappingForRequest(request) == false)
				return 1;
			else if (this.isMappingForRequest(request) == false
					&& other.isMappingForRequest(request))
				return -1;
			else {
				String path = new UrlPathHelper().getPathWithinApplication(request);
				String storeLookupPath = StoreUtils.storeLookupPath(path, baseUri);

				String filename = FilenameUtils.getName(storeLookupPath);
				String extension = FilenameUtils.getExtension(filename);
				if (extension != null && "store".equals(storeType)) {
					return -1;
				}
				else if (extension != null && "contentstore".equals(storeType)) {
					return 1;
				}
				return 0;
			}
		}

		public boolean isMappingForRequest(HttpServletRequest request) {
			String path = new UrlPathHelper().getPathWithinApplication(request);
			String storeLookupPath = StoreUtils.storeLookupPath(path, baseUri);

			String[] segments = storeLookupPath.split("/");
			if (segments.length < 3) {
				return false;
			}
			StoreInfo info = stores.getStore(Store.class, StoreUtils.withStorePath(segments[1]));
			if (info != null
					&& (Store.class.isAssignableFrom(info.getInterface())
							&& "store".equals(storeType))
					|| (ContentStore.class.isAssignableFrom(info.getInterface())
							&& "contentstore".equals(storeType))) {
				return true;
			}
			return false;
		}
	}
}
