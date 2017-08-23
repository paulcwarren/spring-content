package internal.org.springframework.content.rest.controllers;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.support.Repositories;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import internal.org.springframework.content.rest.utils.PersistentEntityUtils;
import internal.org.springframework.content.rest.utils.RepositoryUtils;

public abstract class AbstractContentPropertyController {
	
	public AbstractContentPropertyController() {
	}

	protected void setContentProperty(Object domainObj, PersistentProperty<?> property, String contentId, Object newValue) {

		PersistentPropertyAccessor accessor = property.getOwner().getPropertyAccessor(domainObj);
		Object contentPropertyObject = accessor.getProperty(property);
		if (contentPropertyObject == null) 
			return;
		else if (!PersistentEntityUtils.isPropertyMultiValued(property)) {
			accessor.setProperty(property, newValue);
		} else {
			// handle multi-valued
			if (property.isArray()) {
				throw new UnsupportedOperationException();
			} else if (property.isCollectionLike() && contentPropertyObject instanceof Set) {
				@SuppressWarnings("unchecked")
				Set<Object> contentSet = (Set<Object>)contentPropertyObject;
				Object oldValue = findContentPropertyObjectInSet(contentId, contentSet);
				contentSet.remove(oldValue);
				if (newValue != null)
					contentSet.add(newValue);
			}
		} 
	}

	protected Object getContentProperty(Object domainObj, PersistentProperty<?> property, String contentId) {

		PersistentPropertyAccessor accessor = property.getOwner().getPropertyAccessor(domainObj);
		Object contentPropertyObject = accessor.getProperty(property);

		// multi-valued property?
		if (PersistentEntityUtils.isPropertyMultiValued(property)) {
			if (property.isArray()) {
				throw new UnsupportedOperationException();
			} else if (property.isCollectionLike()) {
				contentPropertyObject = findContentPropertyObjectInSet(contentId, (Collection<?>)contentPropertyObject);
			}
		}
		
		if (contentPropertyObject == null) {
			throw new ResourceNotFoundException();
		}

		if (BeanUtils.hasFieldWithAnnotation(contentPropertyObject, ContentId.class)) {
			if (BeanUtils.getFieldWithAnnotation(contentPropertyObject, ContentId.class) == null) {
				throw new ResourceNotFoundException();
			}
		}
		
		return contentPropertyObject;
	}

	protected PersistentProperty<?> getContentPropertyDefinition(PersistentEntity<?, ?> persistentEntity, String contentProperty) {
		PersistentProperty<?> prop = persistentEntity.getPersistentProperty(contentProperty);
		if (null == prop)
			throw new ResourceNotFoundException();
		
		return prop;
	}

	protected Object findContentPropertyObjectInSet(String id, Collection<?> contents) {
		for (Object content : contents) {
			if (BeanUtils.hasFieldWithAnnotation(content, ContentId.class) && BeanUtils.getFieldWithAnnotation(content, ContentId.class) != null) {
				String candidateId = BeanUtils.getFieldWithAnnotation(content, ContentId.class).toString();
				if (candidateId.equals(id))
					return content;
			}
		}
		return null;
	}

	protected Object findOne(Repositories repositories, String repository, String id) 
			throws HttpRequestMethodNotSupportedException {
		
		Object domainObj = null;
		Class<?> domainObjClazz = null;
		Class<?> idClazz = null;
		
		RepositoryInformation ri = null;
		for (Class<?> clazz : repositories) {
			ri = repositories.getRepositoryInformationFor(clazz);
			if (ri == null) {
				continue;
			}
			if (repository.equals(RepositoryUtils.repositoryPath(ri))) {
				ri = repositories.getRepositoryInformationFor(clazz);
				domainObjClazz = clazz;
				idClazz = ri.getIdType();
				break;
			}
		}

		if (ri == null || domainObjClazz == null) {
			throw new ResourceNotFoundException();
		}
		
		Method findOneMethod = ri.getCrudMethods().getFindOneMethod();
		if (findOneMethod == null) {
			throw new HttpRequestMethodNotSupportedException("fineOne");
		}
		
		Object oid = new DefaultConversionService().convert(id, idClazz);
		domainObj = ReflectionUtils.invokeMethod(findOneMethod, repositories.getRepositoryFor(domainObjClazz), oid);
		
		if (null == domainObj) {
			throw new ResourceNotFoundException();
		}
		
		return domainObj;
	}

	protected Object save(Repositories repositories, String repository, Object domainObj) 
			throws HttpRequestMethodNotSupportedException {

		Class<?> domainObjClazz = null;
		
		RepositoryInformation ri = null;
		for (Class<?> clazz : repositories) {
			ri = repositories.getRepositoryInformationFor(clazz);
			if (ri == null) {
				continue;
			}
			if (repository.equals(RepositoryUtils.repositoryPath(ri))) {
				ri = repositories.getRepositoryInformationFor(clazz);
				domainObjClazz = clazz;
			}
		}

		if (ri == null || domainObjClazz == null) {
			throw new ResourceNotFoundException();
		}
		
		if (domainObjClazz != null) {
			Method saveMethod = ri.getCrudMethods().getSaveMethod();
			if (saveMethod == null) {
				throw new HttpRequestMethodNotSupportedException("save");
			}
			domainObj = ReflectionUtils.invokeMethod(saveMethod, repositories.getRepositoryFor(domainObjClazz), domainObj);
		}

		return domainObj;
	}
}
