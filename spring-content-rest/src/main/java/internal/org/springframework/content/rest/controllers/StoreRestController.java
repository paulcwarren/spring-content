package internal.org.springframework.content.rest.controllers;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.storeservice.ContentStoreInfo;
import org.springframework.content.commons.storeservice.ContentStoreService;
import org.springframework.core.io.Resource;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.util.UrlPathHelper;

import internal.org.springframework.content.rest.annotations.ContentRestController;
import internal.org.springframework.content.rest.annotations.ContentStoreRestResource;
import internal.org.springframework.content.rest.mappings.ContentHandlerMapping.StoreType;
import internal.org.springframework.content.rest.mappings.ContentRestByteRangeHttpRequestHandler;
import internal.org.springframework.content.rest.utils.ContentStoreUtils;

@ContentRestController
public class StoreRestController extends AbstractContentPropertyController {

	private static final String BASE_MAPPING = "/{store}/{id}";

	private ContentStoreService storeService;
	private ContentRestByteRangeHttpRequestHandler handler;
	
	@Autowired 
	public StoreRestController(ContentStoreService storeService, ContentRestByteRangeHttpRequestHandler handler) {
		super();
		this.storeService = storeService;
		this.handler = handler;
	}

	@StoreType("store")
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.GET, headers={"accept!=application/hal+json", "range"})
	public void getContent(HttpServletRequest request, 
						   HttpServletResponse response,
						   @PathVariable String store, 
						   @PathVariable String id) 
			throws ServletException, IOException {
		
//		String path = request.getPathInfo();
		String path = new UrlPathHelper().getPathWithinApplication(request);
		
		ContentStoreInfo info = ContentStoreUtils.findStore(storeService, path);
		if (info == null) {
			throw new IllegalArgumentException("Entity not a content repository");
		}
		
		String pathToUse = path.substring(info.getInterface().getAnnotation(ContentStoreRestResource.class).path().length() + 1);

		Resource r = ((Store)info.getImpementation()).getResource(pathToUse);
		if (r == null) {
			throw new ResourceNotFoundException();
		}

		request.setAttribute("SPRING_CONTENT_RESOURCE", r);

		handler.handleRequest(request, response);

		return;
	}
	
//	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.PUT, headers={"content-type!=multipart/form-data", "accept!=application/hal+json"})
//	@ResponseBody
//	public void putContent(final HttpServletRequest request,
//									final RootResourceInformation rootInfo,
//			        				@PathVariable String repository, 
//									@PathVariable String id) 
//			throws IOException, HttpRequestMethodNotSupportedException, InstantiationException, IllegalAccessException {
//
//		RepositoryInvoker invoker = rootInfo.getInvoker();
//		Object domainObj = getDomainObject(invoker, id);
//
//		ContentStoreInfo info = ContentStoreUtils.findContentStore(storeService, domainObj.getClass());
//		if (info == null) {
//			throw new IllegalArgumentException("Entity not a content repository");
//		}
//
//		info.getImpementation().setContent(domainObj, request.getInputStream());
//		
//		if (BeanUtils.hasFieldWithAnnotation(domainObj, MimeType.class)) {
//			BeanUtils.setFieldWithAnnotation(domainObj, MimeType.class, request.getHeader("Content-Type"));
//		}
//		
//		invoker.invokeSave(domainObj);
//	}
//	
//	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.PUT, headers = "content-type=multipart/form-data")
//	@ResponseBody
//	public void putMultipartContent(RootResourceInformation rootInfo,
//									 @PathVariable String repository, 
//									 @PathVariable String id, 
//									 @RequestParam("file") MultipartFile multiPart)
//											 throws IOException, HttpRequestMethodNotSupportedException, InstantiationException, IllegalAccessException {
//		handleMultipart(rootInfo, id, multiPart);
//	}
//	
//	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.POST, headers = "content-type=multipart/form-data")
//	@ResponseBody
//	public void postMultipartContent(RootResourceInformation rootInfo,
//									 @PathVariable String repository, 
//									 @PathVariable String id, 
//									 @RequestParam("file") MultipartFile multiPart)
//											 throws IOException, HttpRequestMethodNotSupportedException, InstantiationException, IllegalAccessException {
//		handleMultipart(rootInfo, id, multiPart);
//	}
//	
//	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.DELETE, headers="accept!=application/hal+json")
//	public void deleteContent(final RootResourceInformation rootInfo,
//														  @PathVariable String repository, 
//														  @PathVariable String id) 
//			throws HttpRequestMethodNotSupportedException {
//
//		RepositoryInvoker invoker = rootInfo.getInvoker();
//		Object domainObj = getDomainObject(invoker, id);
//
//		ContentStoreInfo info = ContentStoreUtils.findContentStore(storeService, domainObj.getClass());
//		if (info == null) {
//			throw new IllegalArgumentException("Entity not a content repository");
//		}
//
//		if (info.getImpementation().getContent(domainObj) == null) {
//			throw new ResourceNotFoundException();
//		}
//		
//		info.getImpementation().unsetContent(domainObj);
//		
//		if (BeanUtils.hasFieldWithAnnotation(domainObj, MimeType.class)) {
//			BeanUtils.setFieldWithAnnotation(domainObj, MimeType.class, null);
//		}
//		
//		invoker.invokeSave(domainObj);
//	}
//
//	protected void handleMultipart(RootResourceInformation rootInfo, String id, MultipartFile multiPart)
//			throws HttpRequestMethodNotSupportedException, IOException {
//		RepositoryInvoker invoker = rootInfo.getInvoker();
//		Object domainObj = getDomainObject(invoker, id);
//
//		ContentStoreInfo info = ContentStoreUtils.findContentStore(storeService, domainObj.getClass());
//		if (info == null) {
//			throw new IllegalArgumentException("Entity not a content repository");
//		}
//
//		info.getImpementation().setContent(domainObj, multiPart.getInputStream());
//		
//		if (BeanUtils.hasFieldWithAnnotation(domainObj, MimeType.class)) {
//			BeanUtils.setFieldWithAnnotation(domainObj, MimeType.class, multiPart.getContentType());
//		}
//		
//		invoker.invokeSave(domainObj);
//	}
}
