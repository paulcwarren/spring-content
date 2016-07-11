package internal.org.springframework.content.rest.mappings;

import internal.org.springframework.content.rest.annotations.ContentRestController;
import internal.org.springframework.content.rest.utils.PersistentEntityUtils;
import internal.org.springframework.content.rest.utils.RepositoryUtils;

import javax.servlet.http.HttpServletRequest;

import org.springframework.content.annotations.Content;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

public class ContentHandlerMapping extends RequestMappingHandlerMapping {

	private Repositories repositories = null;
	private ResourceMappings repositoryMappings;
	
	public ContentHandlerMapping(Repositories repositories, ResourceMappings repositoryMappings) {
		this.repositories = repositories;
		this.repositoryMappings = repositoryMappings;
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
		if (path.length < 4 )
			return null;
		
		ResourceMetadata mapping = RepositoryUtils.findRepositoryMapping(repositories, repositoryMappings, path[1]);
		if (mapping == null)
			return null;
		
		Class<?> domainType = mapping.getDomainType();
		
		PersistentEntity<?,?> entity = PersistentEntityUtils.findPersistentEntity(repositories, domainType);
		if (null == entity)
			return null;
		
		PersistentProperty<?> prop = entity.getPersistentProperty(path[3]);
		if (null == prop)
			return null;
		
		if (prop.getField().isAnnotationPresent(Content.class))
			return super.lookupHandlerMethod(lookupPath, request);

		return null;
	}
}
