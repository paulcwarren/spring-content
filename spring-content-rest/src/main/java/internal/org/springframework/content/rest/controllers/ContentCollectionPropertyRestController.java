package internal.org.springframework.content.rest.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.annotations.ContentId;
import org.springframework.content.annotations.MimeType;
import org.springframework.content.common.storeservice.ContentStoreInfo;
import org.springframework.content.common.storeservice.ContentStoreService;
import org.springframework.content.common.utils.BeanUtils;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import internal.org.springframework.content.rest.annotations.ContentRestController;
import internal.org.springframework.content.rest.links.ContentLinks;
import internal.org.springframework.content.rest.links.ContentResource;
import internal.org.springframework.content.rest.links.ContentResources;
import internal.org.springframework.content.rest.utils.ContentStoreUtils;
import internal.org.springframework.content.rest.utils.PersistentEntityUtils;

@ContentRestController
public class ContentCollectionPropertyRestController extends AbstractContentPropertyController {

	private static final String BASE_MAPPING = "/{repository}/{id}/{contentProperty}";

	@Autowired 
	ContentStoreService storeService;
	
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.GET)
	public ResponseEntity<Resources<?>> getCollection(final RootResourceInformation rootInfo, 
													  @PathVariable String repository, 
												  	  @PathVariable String id, 
												  	  @PathVariable String contentProperty) 
			throws HttpRequestMethodNotSupportedException {
		
		Object domainObj = this.getDomainObject(rootInfo.getInvoker(), id);
				
		PersistentEntity<?,?> entity = rootInfo.getPersistentEntity();
		if (null == entity)
			throw new ResourceNotFoundException();
		
		PersistentProperty<?> property = this.getContentPropertyDefinition(entity, contentProperty);

		PersistentPropertyAccessor accessor = property.getOwner().getPropertyAccessor(domainObj);
		Object propVal = accessor.getProperty(property);
		if (propVal == null)
			throw new ResourceNotFoundException("No content");

		if (property.isArray())
			throw new UnsupportedOperationException();
		else if (property.isCollectionLike()) {
			@SuppressWarnings("unchecked")
			List<Resource<Object>> resources = toResources(new ContentLinks(""), (Collection<Object>)propVal);
			return new ResponseEntity<Resources<?>>(new ContentResources(resources), HttpStatus.OK);
		}
		
		return null;
	}

	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.POST, headers = "content-type!=multipart/form-data")
	@ResponseBody
	public ResponseEntity<Resource<?>> postContent(final HttpServletRequest request,
			        				final HttpServletResponse response,
			        				RootResourceInformation rootInfo,
			        				@PathVariable String repository, 
									@PathVariable String id, 
									@PathVariable String contentProperty) 
									throws IOException, HttpRequestMethodNotSupportedException, InstantiationException, IllegalAccessException {
		
		Object newContent = this.saveContentInternal(rootInfo, repository, id, contentProperty, request.getRequestURI(), request.getHeader("Content-Type"), request.getInputStream());
		if (newContent != null) {
			Resource<?> contentResource = toResource(request, newContent);
			return new ResponseEntity<Resource<?>>(contentResource, HttpStatus.CREATED);
		}
		return null;
	}

	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.POST, headers = "content-type=multipart/form-data")
	@ResponseBody
	public ResponseEntity<Resource<?>> postMultipartContent(final HttpServletRequest request,
									 final HttpServletResponse response,
									 RootResourceInformation rootInfo,
									 @PathVariable String repository, 
									 @PathVariable String id, 
									 @PathVariable String contentProperty,
									 @RequestParam("file") MultipartFile multiPart)
											 throws IOException, HttpRequestMethodNotSupportedException, InstantiationException, IllegalAccessException {

		Object newContent = this.saveContentInternal(rootInfo, repository, id, contentProperty, request.getRequestURI(), multiPart.getContentType(), multiPart.getInputStream());
		if (newContent != null) {
			Resource<?> contentResource = toResource(request, newContent);
			return new ResponseEntity<Resource<?>>(contentResource, HttpStatus.CREATED);
		}
		return null;
	}

	Resource<?> toResource(final HttpServletRequest request, Object newContent)
			throws SecurityException, BeansException {
		Link self = new Link(StringUtils.trimTrailingCharacter(request.getRequestURL().toString(), '/') + "/" + BeanUtils.getFieldWithAnnotation(newContent, ContentId.class));
		Resource<?> contentResource = new Resource<Object>(newContent, Collections.singletonList(self));
		return contentResource;
	}	

	private List<Resource<Object>> toResources(ContentLinks cl, Collection<Object> contents) {
		List<Resource<Object>> resources = new ArrayList<Resource<Object>>();
		for (Object content : contents) {
			if (BeanUtils.hasFieldWithAnnotation(content, ContentId.class) && BeanUtils.getFieldWithAnnotation(content, ContentId.class) != null) {
				ContentResource resource = new ContentResource(content, cl.linkToContent(content));
				resources.add(resource);
			}
		}
		return resources;
	}

	private Object saveContentInternal(RootResourceInformation rootInfo, 
									 String repository,
									 String id, 
									 String contentProperty,  
									 String requestUri,
									 String mimeType,
									 InputStream stream) 
			throws HttpRequestMethodNotSupportedException {
		
		Object domainObj = this.getDomainObject(rootInfo.getInvoker(), id);
				
		PersistentEntity<?,?> entity = rootInfo.getPersistentEntity();
		if (null == entity)
			throw new ResourceNotFoundException();
		
		PersistentProperty<?> property = this.getContentPropertyDefinition(entity, contentProperty);

		PersistentPropertyAccessor accessor = property.getOwner().getPropertyAccessor(domainObj);
		Object propVal = accessor.getProperty(property);
		Class<?> contentEntityClass = null;
		
		// null single-valued content property
		if (!PersistentEntityUtils.isPropertyMultiValued(property)) {
			contentEntityClass = property.getActualType();
		} 
		// null multi-valued content property
		else if (PersistentEntityUtils.isPropertyMultiValued(property)) {
			if (property.isArray()) {
				contentEntityClass = propVal.getClass().getComponentType();
			}
			else if (property.isCollectionLike()) {
				contentEntityClass = property.getActualType();
			}
		}
		
		ContentStoreInfo info = ContentStoreUtils.findContentStore(storeService, contentEntityClass);
		if (info == null)
			throw new IllegalStateException(String.format("Unable to find a content store for %s", repository));

		// null single-valued content property
		if (propVal == null && !PersistentEntityUtils.isPropertyMultiValued(property)) {
			propVal = instantiate(info.getDomainObjectClass());
			accessor.setProperty(property, propVal);
		} 
		// null multi-valued content property
		else if (propVal == null && PersistentEntityUtils.isPropertyMultiValued(property)) {
			// TODO: instantiate an instance of the required arrays or collection/set/list and then 
			// an instance of the content property and add it to the list
		} 
		// non-null multi-valued property
		else if (propVal != null && PersistentEntityUtils.isPropertyMultiValued(property)) {

			// instantiate an instance of the member type and add it
			if (property.isArray()) {
				Class<?> memberType = propVal.getClass().getComponentType();
				Object member = instantiate(memberType);
				Object newArray = Array.newInstance(propVal.getClass(), Array.getLength(propVal) + 1);
				System.arraycopy(propVal, 0, newArray, 0, Array.getLength(propVal));
				Array.set(newArray, Array.getLength(propVal), member);
				accessor.setProperty(property, newArray);
				propVal = member;
				
			} else if (property.isCollectionLike()) {
				Class<?> memberType = property.getActualType();
				Object member = instantiate(memberType);
				@SuppressWarnings("unchecked") Collection<Object> contentCollection = (Collection<Object>)accessor.getProperty(property);
				contentCollection.add(member);
				propVal = member;
			}
		}

		if (BeanUtils.hasFieldWithAnnotation(propVal, MimeType.class)) {
			BeanUtils.setFieldWithAnnotation(propVal, MimeType.class, mimeType);
		}
		
		info.getImpementation().setContent(propVal, stream);
		
		rootInfo.getInvoker().invokeSave(domainObj);
		
		return propVal;
	}
	
	private Object instantiate(Class<?> clazz) {
		Object newObject = null;
		try {
			newObject = clazz.newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return newObject;
	}
}