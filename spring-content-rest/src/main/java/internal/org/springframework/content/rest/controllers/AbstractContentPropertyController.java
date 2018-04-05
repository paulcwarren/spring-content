package internal.org.springframework.content.rest.controllers;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Optional;
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

	public static Object findOne(Repositories repositories, String repository, String id) 
			throws HttpRequestMethodNotSupportedException {
		
		Optional<Object> domainObj = null;
		
		RepositoryInformation ri = findRepositoryInformation(repositories, repository);

		if (ri == null) {
			throw new ResourceNotFoundException();
		}
		
		Class<?> domainObjClazz = ri.getDomainType();
		Class<?> idClazz = ri.getIdType();
		
		Optional<Method> findOneMethod = ri.getCrudMethods().getFindOneMethod();
		if (!findOneMethod.isPresent()) {
			throw new HttpRequestMethodNotSupportedException("fineOne");
		}

		Object oid = new DefaultConversionService().convert(id, idClazz);
		domainObj = (Optional<Object>) ReflectionUtils.invokeMethod(findOneMethod.get(), repositories.getRepositoryFor(domainObjClazz).get(), oid);
		
		if (null == domainObj) {
			throw new ResourceNotFoundException();
		}

		return domainObj.orElseThrow(ResourceNotFoundException::new);
	}

	public static Object findOne(Repositories repositories, Class<?> domainObjClass, String id) 
			throws HttpRequestMethodNotSupportedException {
		
		Optional<Object> domainObj = null;
		
		RepositoryInformation ri = findRepositoryInformation(repositories, domainObjClass);

		if (ri == null) {
			throw new ResourceNotFoundException();
		}
		
		Class<?> domainObjClazz = ri.getDomainType();
		Class<?> idClazz = ri.getIdType();
		
		Optional<Method> findOneMethod = ri.getCrudMethods().getFindOneMethod();
		if (!findOneMethod.isPresent()) {
			throw new HttpRequestMethodNotSupportedException("fineOne");
		}
		
		Object oid = new DefaultConversionService().convert(id, idClazz);
		domainObj = (Optional<Object>) ReflectionUtils.invokeMethod(findOneMethod.get(), repositories.getRepositoryFor(domainObjClazz).get(), oid);

		return domainObj.orElseThrow(ResourceNotFoundException::new);
	}

	public static Iterable findAll(Repositories repositories, String repository) 
			throws HttpRequestMethodNotSupportedException {
		
		Iterable entities = null;
		
		RepositoryInformation ri = findRepositoryInformation(repositories, repository);

		if (ri == null) {
			throw new ResourceNotFoundException();
		}
		
		Class<?> domainObjClazz = ri.getDomainType();
		Class<?> idClazz = ri.getIdType();
		
		Optional<Method> findAllMethod = ri.getCrudMethods().getFindAllMethod();
		if (!findAllMethod.isPresent()) {
			throw new HttpRequestMethodNotSupportedException("fineAll");
		}
		
		entities = (Iterable)ReflectionUtils.invokeMethod(findAllMethod.get(), repositories.getRepositoryFor(domainObjClazz));
		
		if (null == entities) {
			throw new ResourceNotFoundException();
		}
		
		return entities;
	}

	public static Object save(Repositories repositories, Object domainObj) 
			throws HttpRequestMethodNotSupportedException {

		RepositoryInformation ri = findRepositoryInformation(repositories, domainObj.getClass());

		if (ri == null) {
			throw new ResourceNotFoundException();
		}

		Class<?> domainObjClazz = ri.getDomainType();
		
		if (domainObjClazz != null) {
			Optional<Method> saveMethod = ri.getCrudMethods().getSaveMethod();
			if (!saveMethod.isPresent()) {
				throw new HttpRequestMethodNotSupportedException("save");
			}
			domainObj = ReflectionUtils.invokeMethod(saveMethod.get(), repositories.getRepositoryFor(domainObjClazz).get(), domainObj);
		}

		return domainObj;
	}
	
	public static RepositoryInformation findRepositoryInformation(Repositories repositories, String repository) {
		RepositoryInformation ri = null;
		for (Class<?> clazz : repositories) {
			Optional<RepositoryInformation> candidate = repositories.getRepositoryInformationFor(clazz);
			if (!candidate.isPresent()) {
				continue;
			}
			if (repository.equals(RepositoryUtils.repositoryPath(candidate.get()))) {
				ri = repositories.getRepositoryInformationFor(clazz).get();
				break;
			}
		}
		return ri;
	}

	public static RepositoryInformation findRepositoryInformation(Repositories repositories, Class<?> domainObjectClass) {
		RepositoryInformation ri = null;
		for (Class<?> clazz : repositories) {
			if (clazz.equals(domainObjectClass)) {
				return repositories.getRepositoryInformationFor(clazz).get();
			}
		}
		return ri;
	}
}
