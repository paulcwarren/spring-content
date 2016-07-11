package internal.org.springframework.content.rest.controllers;

import java.util.Set;

import org.springframework.content.annotations.ContentId;
import org.springframework.content.common.utils.BeanUtils;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import internal.org.springframework.content.rest.utils.PersistentEntityUtils;

public abstract class AbstractContentPropertyController {
	
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
		if (contentPropertyObject == null) 
			throw new ResourceNotFoundException();

		// multi-valued property?
		if (PersistentEntityUtils.isPropertyMultiValued(property)) {
			if (property.isArray()) {
				throw new UnsupportedOperationException();
			} else if (property.isCollectionLike()) {
				contentPropertyObject = findContentPropertyObjectInSet(contentId, (Set<?>)contentPropertyObject);
				if (contentPropertyObject == null)
					throw new ResourceNotFoundException();
			}
		}
		return contentPropertyObject;
	}

	protected Object getDomainObject(RepositoryInvoker invoker, String id) 
			throws HttpRequestMethodNotSupportedException {
		if (!invoker.hasFindOneMethod()) {
			throw new HttpRequestMethodNotSupportedException("fineOne");
		}

		Object domainObj = invoker.invokeFindOne(id);

		if (null == domainObj) {
			throw new ResourceNotFoundException();
		}

		return domainObj;
	}

	protected PersistentProperty<?> getContentPropertyDefinition(PersistentEntity<?, ?> persistentEntity, String contentProperty) {
		PersistentProperty<?> prop = persistentEntity.getPersistentProperty(contentProperty);
		if (null == prop)
			throw new ResourceNotFoundException();
		
		return prop;
	}

	protected Object findContentPropertyObjectInSet(String id, Set<?> contents) {
		for (Object content : contents) {
			if (BeanUtils.hasFieldWithAnnotation(content, ContentId.class) && BeanUtils.getFieldWithAnnotation(content, ContentId.class) != null) {
				String candidateId = BeanUtils.getFieldWithAnnotation(content, ContentId.class).toString();
				if (candidateId.equals(id))
					return content;
			}
		}
		return null;
	}
}
