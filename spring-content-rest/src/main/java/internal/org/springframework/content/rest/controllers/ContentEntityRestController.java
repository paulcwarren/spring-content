package internal.org.springframework.content.rest.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.storeservice.ContentStoreInfo;
import org.springframework.content.commons.storeservice.ContentStoreService;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.repository.support.Repositories;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import internal.org.springframework.content.rest.utils.ContentStoreUtils;

@ContentRestController
public class ContentEntityRestController extends AbstractContentPropertyController {

	private static final String BASE_MAPPING = "/{store}/{id}";

	private Repositories repositories;
	private ContentStoreService storeService;
	private StoreByteRangeHttpRequestHandler handler;

	@Autowired 
	public ContentEntityRestController(ApplicationContext context, ContentStoreService storeService, StoreByteRangeHttpRequestHandler handler) {
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
	public void getContent(HttpServletRequest request, 
						   HttpServletResponse response,
						   @PathVariable String store, 
						   @PathVariable String id) 
			throws HttpRequestMethodNotSupportedException {
		
		ContentStoreInfo info = ContentStoreUtils.findStore(storeService, store);
		
		if (info == null) {
			throw new IllegalArgumentException(String.format("Store for path %s not found", store));
		}
		
		Object domainObj = findOne(repositories, info.getDomainObjectClass(), id);

		Serializable cid = (Serializable) BeanUtils.getFieldWithAnnotation(domainObj, ContentId.class);
		
		Resource r = ((Store)info.getImpementation()).getResource(cid);
		if (r == null) {
			throw new ResourceNotFoundException();
		}

		request.setAttribute("SPRING_CONTENT_RESOURCE", r);

		if (BeanUtils.hasFieldWithAnnotation(domainObj, MimeType.class)) {
			request.setAttribute("SPRING_CONTENT_CONTENTTYPE", BeanUtils.getFieldWithAnnotation(domainObj, MimeType.class).toString());
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
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.GET)
	public ResponseEntity<InputStreamResource> getContent(@PathVariable String store, 
														  @PathVariable String id, 
														  @RequestHeader(value="Accept", required=false) String mimeType) 
			throws HttpRequestMethodNotSupportedException {
		
		ContentStoreInfo info = ContentStoreUtils.findStore(storeService, store);
		if (info == null) {
			throw new IllegalArgumentException(String.format("Store for path %s not found", store));
		}
		
		Object domainObj = findOne(repositories, info.getDomainObjectClass(), id);
		
		Object contentId = BeanUtils.getFieldWithAnnotation(domainObj, ContentId.class);
		if (contentId == null) {
			throw new ResourceNotFoundException();
		}
		
		List<MediaType> mimeTypes = new ArrayList<>(MediaType.parseMediaTypes(mimeType));
		if (mimeTypes.size() == 0) {
			mimeTypes.add(MediaType.ALL);
		}
		
		final HttpHeaders headers = new HttpHeaders();
		ContentStore<Object,Serializable> storeImpl = info.getImpementation();
		InputStream content = ContentStoreUtils.getContent(storeImpl, domainObj, mimeTypes, headers);
		if (content != null) {		
			InputStreamResource inputStreamResource = new InputStreamResource(content);
			return new ResponseEntity<InputStreamResource>(inputStreamResource, headers, HttpStatus.OK);
		} else {
			return new ResponseEntity<InputStreamResource>(null, headers, HttpStatus.NOT_ACCEPTABLE);
		}
	}
	
	@StoreType("contentstore")
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.PUT, headers={"content-type!=multipart/form-data", "accept!=application/hal+json"})
	@ResponseBody
	public void putContent(HttpServletRequest request,
						   HttpServletResponse response,
        				   @PathVariable String store, 
						   @PathVariable String id) 
			throws IOException, HttpRequestMethodNotSupportedException, InstantiationException, IllegalAccessException {

		ContentStoreInfo info = ContentStoreUtils.findContentStore(storeService, store);
		
		if (info == null) {
			throw new IllegalArgumentException(String.format("Store for path %s not found", store));
		}
		
		Object domainObj = findOne(repositories, info.getDomainObjectClass(), id);
		
		boolean isNew = true;
		if (BeanUtils.hasFieldWithAnnotation(domainObj, ContentId.class)) {
			isNew = (BeanUtils.getFieldWithAnnotation(domainObj, ContentId.class) == null);
		}
		
		info.getImpementation().setContent(domainObj, request.getInputStream());
		
		if (BeanUtils.hasFieldWithAnnotation(domainObj, MimeType.class)) {
			BeanUtils.setFieldWithAnnotation(domainObj, MimeType.class, request.getHeader("Content-Type"));
		}
		
		save(repositories, domainObj);
		
		if (isNew) {
			response.setStatus(HttpStatus.CREATED.value());
		} else {
			response.setStatus(HttpStatus.OK.value());
		}
	}

	@StoreType("contentstore")
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.PUT, headers = "content-type=multipart/form-data")
	@ResponseBody
	public void putMultipartContent(HttpServletResponse response,
									@PathVariable String store, 
									@PathVariable String id, 
									@RequestParam("file") MultipartFile multiPart)
											 throws IOException, HttpRequestMethodNotSupportedException, InstantiationException, IllegalAccessException {
		handleMultipart(response, store, id, multiPart);
	}

	@StoreType("contentstore")
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.POST, headers = "content-type=multipart/form-data")
	@ResponseBody
	public void postMultipartContent(HttpServletResponse response,
									 @PathVariable String store, 
									 @PathVariable String id, 
									 @RequestParam("file") MultipartFile multiPart)
											 throws IOException, HttpRequestMethodNotSupportedException, InstantiationException, IllegalAccessException {
		handleMultipart(response, store, id, multiPart);
	}
	
	@StoreType("contentstore")
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.DELETE, headers="accept!=application/hal+json")
	public void deleteContent(HttpServletResponse response,
							  @PathVariable String store, 
							  @PathVariable String id)
			throws HttpRequestMethodNotSupportedException, IOException {

		ContentStoreInfo info = ContentStoreUtils.findContentStore(storeService, store);
		if (info == null) {
			throw new IllegalArgumentException(String.format("Store for path %s not found", store));
		}
		
		Object domainObj = findOne(repositories, info.getDomainObjectClass(), id);

		try (InputStream content = info.getImpementation().getContent(domainObj)) {
			if (content == null) {
				throw new ResourceNotFoundException();
			}
		}

		info.getImpementation().unsetContent(domainObj);
		
		if (BeanUtils.hasFieldWithAnnotation(domainObj, MimeType.class)) {
			BeanUtils.setFieldWithAnnotation(domainObj, MimeType.class, null);
		}
		
		save(repositories, domainObj);
		
		response.setStatus(HttpStatus.NO_CONTENT.value());
	}

	protected void handleMultipart(HttpServletResponse response, String store, String id, MultipartFile multiPart)
			throws HttpRequestMethodNotSupportedException, IOException {

		ContentStoreInfo info = ContentStoreUtils.findContentStore(storeService, store);
		
		if (info == null) {
			throw new IllegalArgumentException(String.format("Store for path %s not found", store));
		}
		
		Object domainObj = findOne(repositories, info.getDomainObjectClass(), id);
		
		info.getImpementation().setContent(domainObj, multiPart.getInputStream());
		
		if (BeanUtils.hasFieldWithAnnotation(domainObj, MimeType.class)) {
			BeanUtils.setFieldWithAnnotation(domainObj, MimeType.class, multiPart.getContentType());
		}
		
		boolean isNew = true;
		if (BeanUtils.hasFieldWithAnnotation(domainObj, ContentId.class)) {
			isNew = (BeanUtils.getFieldWithAnnotation(domainObj, ContentId.class) == null);
		}
		
		save(repositories, domainObj);
		
		if (isNew) {
			response.setStatus(HttpStatus.CREATED.value());
		} else {
			response.setStatus(HttpStatus.OK.value());
		}
	}
}
