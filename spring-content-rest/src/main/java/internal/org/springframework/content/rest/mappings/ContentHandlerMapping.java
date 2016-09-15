package internal.org.springframework.content.rest.mappings;

import javax.servlet.http.HttpServletRequest;

import org.springframework.content.commons.annotations.Content;
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
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import internal.org.springframework.content.rest.annotations.ContentRestController;
import internal.org.springframework.content.rest.utils.ContentStoreUtils;
import internal.org.springframework.content.rest.utils.PersistentEntityUtils;
import internal.org.springframework.content.rest.utils.RepositoryUtils;

public class ContentHandlerMapping extends RequestMappingHandlerMapping {

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
		String accept = request.getHeader("Accept");
		if (accept != null) {
			try {
				MediaType mediaType = MediaType.parseMediaType(accept);
				if (mediaType.getType().equals("application") && mediaType.getSubtype().equals("hal+json")) {
					return true;
				}
			} catch (InvalidMediaTypeException imte) {
				return true;
			}
		}
		String contentType = request.getHeader("Content-Type");
		if (contentType != null) {
			try {
				MediaType mediaType = MediaType.parseMediaType(contentType);
				if (mediaType.getType().equals("application") && mediaType.getSubtype().equals("hal+json")) {
					return true;
				}
			} catch (InvalidMediaTypeException imte) {
				return true;
			}
		}
		return false;
	}
}
