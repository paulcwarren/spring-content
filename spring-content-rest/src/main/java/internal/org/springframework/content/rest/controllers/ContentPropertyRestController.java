package internal.org.springframework.content.rest.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.annotations.ContentLength;
import org.springframework.content.annotations.MimeType;
import org.springframework.content.common.renditions.Renderable;
import org.springframework.content.common.repository.ContentStore;
import org.springframework.content.common.storeservice.ContentStoreInfo;
import org.springframework.content.common.storeservice.ContentStoreService;
import org.springframework.content.common.utils.BeanUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.rest.webmvc.RootResourceInformation;
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
import internal.org.springframework.content.rest.utils.ContentPropertyUtils;
import internal.org.springframework.content.rest.utils.ContentStoreUtils;

@ContentRestController
public class ContentPropertyRestController extends AbstractContentPropertyController {

	private static final String BASE_MAPPING = "/{repository}/{id}/{contentProperty}/{contentId}";

	@Autowired 
	ContentStoreService storeService;
	
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.GET)
	public ResponseEntity<InputStreamResource> getContent(final RootResourceInformation rootInfo,
														  @PathVariable String repository, 
														  @PathVariable String id, 
														  @PathVariable String contentProperty,
														  @PathVariable String contentId,
														  @RequestHeader("Accept") String mimeType) 
			throws HttpRequestMethodNotSupportedException {
		
		PersistentProperty<?> property = getContentPropertyDefinition(rootInfo.getPersistentEntity(), contentProperty);
		
		Object domainObj = getDomainObject(rootInfo.getInvoker(), id);

		Object contentPropertyValue = getContentProperty(domainObj, property, contentId); 
		
		// get content prop content-type
		// if content-type == Accept header then usual getContent, 
		// else getRendition
		String contentType = BeanUtils.getFieldWithAnnotation(contentPropertyValue, MimeType.class).toString();
		if (mimeType == null || mimeType.equals("*/*") || mimeType.equals(contentType)) {
			final HttpHeaders headers = new HttpHeaders();
			if (BeanUtils.hasFieldWithAnnotation(contentPropertyValue, MimeType.class)) {
				headers.add("Content-Type", BeanUtils.getFieldWithAnnotation(contentPropertyValue, MimeType.class).toString());
			}
			if (BeanUtils.hasFieldWithAnnotation(contentPropertyValue, ContentLength.class))
				headers.add("Content-Length", BeanUtils.getFieldWithAnnotation(contentPropertyValue, ContentLength.class).toString());
			
			Class<?> contentEntityClass = ContentPropertyUtils.getContentPropertyType(property);
			
			ContentStoreInfo info = ContentStoreUtils.findContentStore(storeService, contentEntityClass);
			InputStreamResource inputStreamResource = new InputStreamResource(info.getImpementation().getContent(contentPropertyValue));
			return new ResponseEntity<InputStreamResource>(inputStreamResource, headers, HttpStatus.OK);
		} else {
			final HttpHeaders headers = new HttpHeaders();
			headers.add("Content-Type", mimeType);
//			if (BeanUtils.hasFieldWithAnnotation(contentPropertyValue, ContentLength.class))
//				headers.add("Content-Length", BeanUtils.getFieldWithAnnotation(contentPropertyValue, ContentLength.class).toString());
			
			Class<?> contentEntityClass = ContentPropertyUtils.getContentPropertyType(property);
			
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

	/**
	 * Handles all POSTed requests that aren't multipart forms
	 * 
	 * This method is also called by modern browsers and IE >= 10
	 * @throws IOException 
	 * @throws HttpRequestMethodNotSupportedException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.POST, headers = "content-type!=multipart/form-data")
	@ResponseBody
	public void postContent(final HttpServletRequest request,
									final RootResourceInformation rootInfo,
			        				@PathVariable String repository, 
									@PathVariable String id, 
									@PathVariable String contentProperty,
									@PathVariable String contentId) 
			throws IOException, HttpRequestMethodNotSupportedException, InstantiationException, IllegalAccessException {
		
		this.replaceContentInternal(rootInfo, repository, id, contentProperty, contentId, request.getHeader("Content-Type"), request.getInputStream());
	}	
	

	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.POST, headers = "content-type=multipart/form-data")
	@ResponseBody
	public void postMultipartContent(RootResourceInformation rootInfo,
									 @PathVariable String repository, 
									 @PathVariable String id, 
									 @PathVariable String contentProperty,
									 @PathVariable String contentId,
									 @RequestParam("file") MultipartFile multiPart)
										throws IOException, HttpRequestMethodNotSupportedException, InstantiationException, IllegalAccessException {

		this.replaceContentInternal(rootInfo, repository, id, contentProperty, contentId, multiPart.getContentType(), multiPart.getInputStream());
	}

	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.DELETE)
	@ResponseBody
	public ResponseEntity<?> deleteContent(final RootResourceInformation rootInfo, 
									@PathVariable String repository, 
									@PathVariable String id, 
									@PathVariable String contentProperty,
									@PathVariable String contentId) 
					throws IOException, HttpRequestMethodNotSupportedException {

		PersistentProperty<?> property = getContentPropertyDefinition(rootInfo.getPersistentEntity(), contentProperty);
	
		Object domainObj = getDomainObject(rootInfo.getInvoker(), id);

		Object contentPropertyValue = getContentProperty(domainObj, property, contentId); 
		
		Class<?> contentEntityClass = ContentPropertyUtils.getContentPropertyType(property);
		
		ContentStoreInfo info = ContentStoreUtils.findContentStore(storeService, contentEntityClass);
		info.getImpementation().unsetContent(contentPropertyValue);
		
		// remove the content property reference from the data object
		setContentProperty(domainObj, property, contentId, null);
		
		rootInfo.getInvoker().invokeSave(domainObj);
		
		return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
	}

	private void replaceContentInternal(RootResourceInformation rootInfo,
										String repository,
										String id, 
										String contentProperty, 
										String contentId, 
										String mimeType,
										InputStream stream) 
			throws HttpRequestMethodNotSupportedException {

		PersistentProperty<?> property = this.getContentPropertyDefinition(rootInfo.getPersistentEntity(), contentProperty);
		
		Object domainObj = this.getDomainObject(rootInfo.getInvoker(), id);

		Object contentPropertyValue = this.getContentProperty(domainObj, property, contentId); 
		
		if (BeanUtils.hasFieldWithAnnotation(contentPropertyValue, MimeType.class)) {
			BeanUtils.setFieldWithAnnotation(contentPropertyValue, MimeType.class, mimeType);
		}
		
		Class<?> contentEntityClass = ContentPropertyUtils.getContentPropertyType(property);
		
		ContentStoreInfo info = ContentStoreUtils.findContentStore(storeService, contentEntityClass);
		info.getImpementation().setContent(contentPropertyValue, stream);
		
		rootInfo.getInvoker().invokeSave(domainObj);
	}


}
