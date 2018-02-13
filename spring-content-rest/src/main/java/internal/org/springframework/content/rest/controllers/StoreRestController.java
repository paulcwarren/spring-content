package internal.org.springframework.content.rest.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.storeservice.ContentStoreInfo;
import org.springframework.content.commons.storeservice.ContentStoreService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UrlPathHelper;

import internal.org.springframework.content.rest.annotations.ContentRestController;
import internal.org.springframework.content.rest.mappings.ContentHandlerMapping.StoreType;
import internal.org.springframework.content.rest.mappings.StoreByteRangeHttpRequestHandler;
import internal.org.springframework.content.rest.utils.ContentStoreUtils;

@ContentRestController
public class StoreRestController extends AbstractContentPropertyController {

	private static final String BASE_MAPPING = "/{store}/**";
	
	private ContentStoreService storeService;
	private StoreByteRangeHttpRequestHandler handler;
	
	@Autowired 
	public StoreRestController(ContentStoreService storeService, StoreByteRangeHttpRequestHandler handler) {
		super();
		this.storeService = storeService;
		this.handler = handler;
	}

	@StoreType("store")
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.GET)
	public void getContent(HttpServletRequest request, 
						   HttpServletResponse response,
						   @PathVariable String store) 
			throws ServletException, IOException {
		
		ContentStoreInfo info = ContentStoreUtils.findStore(storeService, store);
		if (info == null) {
			throw new IllegalArgumentException("Entity not a content repository");
		}
		
		String path = new UrlPathHelper().getPathWithinApplication(request);
		String pathToUse = path.substring(ContentStoreUtils.storePath(info).length() + 1);

		Resource r = ((Store)info.getImpementation()).getResource(pathToUse);
		if (r == null) {
			throw new ResourceNotFoundException();
		}

		request.setAttribute("SPRING_CONTENT_RESOURCE", r);

		handler.handleRequest(request, response);

		return;
	}
	
	@StoreType("store")
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.PUT, headers={"content-type!=multipart/form-data", "accept!=application/hal+json"})
	@ResponseBody
	public void putContent(HttpServletRequest request, 
						   HttpServletResponse response,
						   @PathVariable String store) 
			throws IOException, HttpRequestMethodNotSupportedException, InstantiationException, IllegalAccessException {

		ContentStoreInfo info = ContentStoreUtils.findStore(storeService, store);
		if (info == null) {
			throw new IllegalArgumentException("Not a Store");
		}
		
		String path = new UrlPathHelper().getPathWithinApplication(request);
		String pathToUse = path.substring(ContentStoreUtils.storePath(info).length() + 1);

		Resource r = ((Store)info.getImpementation()).getResource(pathToUse);
		if (r == null) {
			throw new ResourceNotFoundException();
		}
		if (r instanceof WritableResource == false) {
			throw new UnsupportedOperationException();
		}
		InputStream in = request.getInputStream();
		OutputStream out = ((WritableResource)r).getOutputStream();
		IOUtils.copy(in, out);
		IOUtils.closeQuietly(out);
		IOUtils.closeQuietly(in);
	}

	@StoreType("store")
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.PUT, headers = "content-type=multipart/form-data")
	@ResponseBody
	public void putMultipartContent(HttpServletRequest request, 
			   						HttpServletResponse response,
									@PathVariable String store, 
									@RequestParam("file") MultipartFile multiPart)
											 throws IOException, HttpRequestMethodNotSupportedException, InstantiationException, IllegalAccessException {
		String path = new UrlPathHelper().getPathWithinApplication(request);
		handleMultipart(store, path, multiPart);
	}
	
	@StoreType("store")
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.POST, headers = "content-type=multipart/form-data")
	@ResponseBody
	public void postMultipartContent(HttpServletRequest request, 
									 HttpServletResponse response,
									 @PathVariable String store, 
									 @RequestParam("file") MultipartFile multiPart)
											 throws IOException, HttpRequestMethodNotSupportedException, InstantiationException, IllegalAccessException {

		String path = new UrlPathHelper().getPathWithinApplication(request);
		handleMultipart(store, path, multiPart);
	}
	
	@StoreType("store")
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.DELETE, headers="accept!=application/hal+json")
	public void deleteContent(HttpServletRequest request, 
							  HttpServletResponse response,
							  @PathVariable String store) 
			throws HttpRequestMethodNotSupportedException {

		ContentStoreInfo info = ContentStoreUtils.findStore(storeService, store);
		if (info == null) {
			throw new IllegalArgumentException("Not a Store");
		}
		
		String path = new UrlPathHelper().getPathWithinApplication(request);
		String pathToUse = path.substring(ContentStoreUtils.storePath(info).length() + 1);

		Resource r = ((Store)info.getImpementation()).getResource(pathToUse);
		if (r == null) {
			throw new ResourceNotFoundException();
		}
		if (r instanceof DeletableResource == false) {
			throw new UnsupportedOperationException();
		}
		((DeletableResource)r).delete();
	}

	protected void handleMultipart(String store, String path, MultipartFile multiPart)
			throws HttpRequestMethodNotSupportedException, IOException {

		ContentStoreInfo info = ContentStoreUtils.findStore(storeService, store);
		if (info == null) {
			throw new IllegalArgumentException("Not a Store");
		}

		String pathToUse = path.substring(store.length() + 1);
		Resource r = ((Store)info.getImpementation()).getResource(pathToUse);
		if (r == null) {
			throw new ResourceNotFoundException();
		}
		if (r instanceof WritableResource == false) {
			throw new UnsupportedOperationException();
		}
		InputStream in = multiPart.getInputStream();
		OutputStream out = ((WritableResource)r).getOutputStream();
		IOUtils.copy(in, out);
		IOUtils.closeQuietly(out);
		IOUtils.closeQuietly(in);
	}
}
