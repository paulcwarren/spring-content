package internal.org.springframework.content.rest.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.Content;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.storeservice.ContentStoreInfo;
import org.springframework.content.commons.storeservice.ContentStoreService;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.repository.support.Repositories;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import internal.org.springframework.content.rest.annotations.ContentRestController;
import internal.org.springframework.content.rest.mappings.ContentHandlerMapping.StoreType;
import internal.org.springframework.content.rest.mappings.StoreByteRangeHttpRequestHandler;
import internal.org.springframework.content.rest.utils.ContentPropertyUtils;
import internal.org.springframework.content.rest.utils.ContentStoreUtils;

@ContentRestController
public class ContentPropertyRestController extends AbstractContentPropertyController {

	private static final String BASE_MAPPING = "/{repository}/{id}/{contentProperty}/{contentId}";

	private Repositories repositories;
	private ContentStoreService storeService;
	private StoreByteRangeHttpRequestHandler handler;
	
	@Autowired(required=false)
	public ContentPropertyRestController(ApplicationContext context, ContentStoreService storeService, StoreByteRangeHttpRequestHandler handler) {
		super();
		try {
			this.repositories = context.getBean(Repositories.class);
		} catch (BeansException be) {
			this.repositories = new Repositories(context);
		}
		this.storeService = storeService;
		this.handler = handler;
	}

	@StoreType("contentstore")
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.GET, headers={"accept!=application/hal+json", "range"})
	public void getContent(HttpServletRequest request, HttpServletResponse response,
						   @PathVariable String repository, 
						   @PathVariable String id, 
						   @PathVariable String contentProperty,
						   @PathVariable String contentId) 
			throws HttpRequestMethodNotSupportedException {
		
		Object domainObj = findOne(repositories, repository, id);

		Object contentPropertyValue = null;
		Class<?> contentEntityClass = null;
		
		if (domainObj.getClass().isAnnotationPresent(Content.class)) {
			contentPropertyValue = domainObj;
			contentEntityClass = domainObj.getClass();
		} else {
			PersistentProperty<?> property = getContentPropertyDefinition(repositories.getPersistentEntity(domainObj.getClass()), contentProperty);
			contentEntityClass = ContentPropertyUtils.getContentPropertyType(property);
			contentPropertyValue = getContentProperty(domainObj, property, contentId);
		}
		
		Serializable cid = (Serializable) BeanUtils.getFieldWithAnnotation(contentPropertyValue, ContentId.class);
		
		ContentStoreInfo info = ContentStoreUtils.findContentStore(storeService, contentEntityClass);
		if (info == null) {
			throw new IllegalArgumentException("Entity not a content repository");
		}

		Resource r = ((Store)info.getImpementation()).getResource(cid);
		if (r == null) {
			throw new ResourceNotFoundException();
		}

		request.setAttribute("SPRING_CONTENT_RESOURCE", r);

		if (BeanUtils.hasFieldWithAnnotation(contentPropertyValue, MimeType.class)) {
			request.setAttribute("SPRING_CONTENT_CONTENTTYPE", BeanUtils.getFieldWithAnnotation(contentPropertyValue, MimeType.class).toString());
		}
		
		try {
			handler.handleRequest(request, response);
		} catch (ServletException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return;
	}
	
	@StoreType("contentstore")
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.GET, headers={"accept!=application/hal+json"})
	public ResponseEntity<InputStreamResource> getContent(@PathVariable String repository, 
														  @PathVariable String id, 
														  @PathVariable String contentProperty,
														  @PathVariable String contentId,
														  @RequestHeader("Accept") String mimeType) 
			throws HttpRequestMethodNotSupportedException {
		
		Object domainObj = findOne(repositories, repository, id);
		
		Object contentPropertyValue = null;
		Class<?> contentEntityClass = null;
		
		if (domainObj.getClass().isAnnotationPresent(Content.class)) {
			contentPropertyValue = domainObj;
			contentEntityClass = domainObj.getClass();
		} else {
			PersistentProperty<?> property = getContentPropertyDefinition(repositories.getPersistentEntity(domainObj.getClass()), contentProperty);
			contentEntityClass = ContentPropertyUtils.getContentPropertyType(property);
			contentPropertyValue = getContentProperty(domainObj, property, contentId);
		}

		// get content prop content-type
		// if content-type == Accept header then usual getContent, 
		// else getRendition
		Object contentTypeObj = BeanUtils.getFieldWithAnnotation(contentPropertyValue, MimeType.class);
		String contentType = contentTypeObj != null ? contentTypeObj.toString() : null;
		if (mimeType == null || mimeType.contains("*/*") || mimeType.equals(contentType)) {
			final HttpHeaders headers = new HttpHeaders();
			if (BeanUtils.hasFieldWithAnnotation(contentPropertyValue, MimeType.class)) {
				headers.add("Content-Type", BeanUtils.getFieldWithAnnotation(contentPropertyValue, MimeType.class).toString());
			}
			if (BeanUtils.hasFieldWithAnnotation(contentPropertyValue, ContentLength.class))
				headers.add("Content-Length", BeanUtils.getFieldWithAnnotation(contentPropertyValue, ContentLength.class).toString());
			
			ContentStoreInfo info = ContentStoreUtils.findContentStore(storeService, contentEntityClass);
			InputStream is = info.getImpementation().getContent(contentPropertyValue);
			if (is == null) {
				throw new ResourceNotFoundException();
			}
			InputStreamResource inputStreamResource = new InputStreamResource(is);
			return new ResponseEntity<InputStreamResource>(inputStreamResource, headers, HttpStatus.OK);
		} else {
			final HttpHeaders headers = new HttpHeaders();
			headers.add("Content-Type", mimeType);
//			if (BeanUtils.hasFieldWithAnnotation(contentPropertyValue, ContentLength.class))
//				headers.add("Content-Length", BeanUtils.getFieldWithAnnotation(contentPropertyValue, ContentLength.class).toString());
			
			ContentStoreInfo info = ContentStoreUtils.findContentStore(storeService, contentEntityClass);
			ContentStore<Object,Serializable> impl = info.getImpementation();
			
			if (impl instanceof Renderable) {
				InputStream is = ((Renderable)impl).getRendition(contentPropertyValue, mimeType);
				if (is != null) {
					InputStreamResource inputStreamResource = new InputStreamResource(is);
					return new ResponseEntity<InputStreamResource>(inputStreamResource, headers, HttpStatus.OK);
				} else {
					return new ResponseEntity<InputStreamResource>(null, headers, HttpStatus.NOT_ACCEPTABLE);
				}
			}
			
		}
		
		return null;
	}

	@StoreType("contentstore")
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.PUT)
	@ResponseBody
	public void setContent(HttpServletRequest request,
	        			   @PathVariable String repository, 
						   @PathVariable String id, 
						   @PathVariable String contentProperty,
						   @PathVariable String contentId) 
			throws IOException, HttpRequestMethodNotSupportedException, InstantiationException, IllegalAccessException {
		
		this.replaceContentInternal(repositories, storeService, repository, id, contentProperty, contentId, request.getHeader("Content-Type"), request.getInputStream());
	}	
	
	@StoreType("contentstore")
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.POST, headers = "content-type!=multipart/form-data")
	@ResponseBody
	public void postContent(HttpServletRequest request,
							@PathVariable String repository, 
							@PathVariable String id, 
							@PathVariable String contentProperty,
							@PathVariable String contentId) 
			throws IOException, HttpRequestMethodNotSupportedException, InstantiationException, IllegalAccessException {
		
		this.replaceContentInternal(repositories, storeService, repository, id, contentProperty, contentId, request.getHeader("Content-Type"), request.getInputStream());
	}	

	@StoreType("contentstore")
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.POST, headers = "content-type=multipart/form-data")
	@ResponseBody
	public void postMultipartContent(@PathVariable String repository, 
									 @PathVariable String id, 
									 @PathVariable String contentProperty,
									 @PathVariable String contentId,
									 @RequestParam("file") MultipartFile multiPart)
										throws IOException, HttpRequestMethodNotSupportedException, InstantiationException, IllegalAccessException {

		this.replaceContentInternal(repositories, storeService, repository, id, contentProperty, contentId, multiPart.getContentType(), multiPart.getInputStream());
	}

	@StoreType("contentstore")
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.DELETE)
	@ResponseBody
	public ResponseEntity<?> deleteContent(@PathVariable String repository, 
										   @PathVariable String id, 
										   @PathVariable String contentProperty,
										   @PathVariable String contentId) 
					throws IOException, HttpRequestMethodNotSupportedException {

		Object domainObj = findOne(repositories, repository, id);

		PersistentProperty<?> property = this.getContentPropertyDefinition(repositories.getPersistentEntity(domainObj.getClass()), contentProperty);

		Object contentPropertyValue = getContentProperty(domainObj, property, contentId); 
		
		Class<?> contentEntityClass = ContentPropertyUtils.getContentPropertyType(property);
		
		ContentStoreInfo info = ContentStoreUtils.findContentStore(storeService, contentEntityClass);
		info.getImpementation().unsetContent(contentPropertyValue);
		
		// remove the content property reference from the data object
		// setContentProperty(domainObj, property, contentId, null);
		
		save(repositories, repository, domainObj);
		
		return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
	}

	private void replaceContentInternal(Repositories repositories,
										ContentStoreService stores,
										String repository,
										String id, 
										String contentProperty, 
										String contentId, 
										String mimeType,
										InputStream stream) 
			throws HttpRequestMethodNotSupportedException {

		Object domainObj = findOne(repositories, repository, id);

		PersistentProperty<?> property = this.getContentPropertyDefinition(repositories.getPersistentEntity(domainObj.getClass()), contentProperty);
		
		Object contentPropertyValue = this.getContentProperty(domainObj, property, contentId); 
		
		if (BeanUtils.hasFieldWithAnnotation(contentPropertyValue, MimeType.class)) {
			BeanUtils.setFieldWithAnnotation(contentPropertyValue, MimeType.class, mimeType);
		}
		
		Class<?> contentEntityClass = ContentPropertyUtils.getContentPropertyType(property);
		
		ContentStoreInfo info = ContentStoreUtils.findContentStore(storeService, contentEntityClass);
		info.getImpementation().setContent(contentPropertyValue, stream);
		
		save(repositories, repository, domainObj);
	}
}
