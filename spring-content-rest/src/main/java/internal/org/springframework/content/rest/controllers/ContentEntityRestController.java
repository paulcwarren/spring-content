package internal.org.springframework.content.rest.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Version;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import internal.org.springframework.content.rest.annotations.ContentRestController;
import internal.org.springframework.content.rest.mappings.ContentHandlerMapping.StoreType;
import internal.org.springframework.content.rest.mappings.StoreByteRangeHttpRequestHandler;
import internal.org.springframework.content.rest.utils.ContentStoreUtils;
import internal.org.springframework.content.rest.utils.HeaderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.storeservice.ContentStoreInfo;
import org.springframework.content.commons.storeservice.ContentStoreService;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.repository.support.Repositories;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static java.lang.String.format;

@ContentRestController
public class ContentEntityRestController extends AbstractContentPropertyController {

	private static final Logger logger = LoggerFactory.getLogger(ContentEntityRestController.class);

	private static final String BASE_MAPPING = "/{store}/{id}";

	private Repositories repositories;
	private ContentStoreService storeService;
	private StoreByteRangeHttpRequestHandler handler;

	@Autowired
	public ContentEntityRestController(ApplicationContext context, ContentStoreService storeService, StoreByteRangeHttpRequestHandler handler) {
		try {
			this.repositories = context.getBean(Repositories.class);
		}
		catch (BeansException be) {
			this.repositories = new Repositories(context);
		}
		this.storeService = storeService;
		this.handler = handler;
	}

	@StoreType("contentstore")
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.GET)
	public void getContent(HttpServletRequest request, HttpServletResponse response,
											   @PathVariable String store, @PathVariable String id,
											   @RequestHeader(value = "Accept", required = false) String mimeType)
			throws HttpRequestMethodNotSupportedException {

		ContentStoreInfo info = ContentStoreUtils.findStore(storeService, store);
		if (info == null) {
			throw new IllegalArgumentException(
					String.format("Store for path %s not found", store));
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

		ContentStore<Object, Serializable> storeImpl = info.getImpementation();
		ContentStoreUtils.ResourcePlan r = ContentStoreUtils.resolveResource(storeImpl, domainObj, null, mimeTypes);
		if (r.getResource().exists() == false) {
			throw new ResourceNotFoundException();
		}

		Object version = BeanUtils.getFieldWithAnnotation(domainObj, Version.class);
		long lastModified = -1;
		try {
			lastModified = r.getResource().lastModified();
		} catch (IOException e) {}
		if(new ServletWebRequest(request, response).checkNotModified(version != null ? version.toString() : null, lastModified)) {
			return;
		}

		request.setAttribute("SPRING_CONTENT_RESOURCE", r.getResource());
		request.setAttribute("SPRING_CONTENT_CONTENTTYPE", r.getMimeType());

		try {
			handler.handleRequest(request, response);
		}
		catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, format("Failed to handle request for %s", id), e);
		}
	}

	@StoreType("contentstore")
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.PUT, headers = {
			"content-type!=multipart/form-data", "accept!=application/hal+json" })
	@ResponseBody
	public void putContent(HttpServletRequest request, HttpServletResponse response, @RequestHeader HttpHeaders headers,
						   @PathVariable String store, @PathVariable String id)
			throws IOException, HttpRequestMethodNotSupportedException {

		handleMultipart(request, response, headers, store, id, request.getInputStream(), headers.getContentType(), null);
	}

	@StoreType("contentstore")
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.PUT, headers = "content-type=multipart/form-data")
	@ResponseBody
	public void putMultipartContent(HttpServletRequest request, HttpServletResponse response, @RequestHeader HttpHeaders headers,
									@PathVariable String store, @PathVariable String id,
									@RequestParam("file") MultipartFile multiPart)
			throws IOException, HttpRequestMethodNotSupportedException {
		handleMultipart(request, response, headers, store, id, multiPart.getInputStream(), MediaType.parseMediaType(multiPart.getContentType()), multiPart.getOriginalFilename());
	}

	@StoreType("contentstore")
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.POST, headers = "content-type=multipart/form-data")
	@ResponseBody
	public void postMultipartContent(HttpServletRequest request, HttpServletResponse response, @RequestHeader HttpHeaders headers,
									 @PathVariable String store,
									 @PathVariable String id,
									 @RequestParam("file") MultipartFile multiPart)
			throws IOException, HttpRequestMethodNotSupportedException {
		handleMultipart(request, response, headers, store, id, multiPart.getInputStream(), MediaType.parseMediaType(multiPart.getContentType()), multiPart.getOriginalFilename());
	}

	@StoreType("contentstore")
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.DELETE, headers = "accept!=application/hal+json")
	public void deleteContent(@RequestHeader HttpHeaders headers, HttpServletResponse response,
							  @PathVariable String store,
							  @PathVariable String id)
			throws HttpRequestMethodNotSupportedException, IOException {

		ContentStoreInfo info = ContentStoreUtils.findContentStore(storeService, store);
		if (info == null) {
			throw new IllegalArgumentException(
					String.format("Store for path %s not found", store));
		}

		Object domainObj = findOne(repositories, info.getDomainObjectClass(), id);

		String etag = (BeanUtils.getFieldWithAnnotation(domainObj, Version.class) != null ? BeanUtils.getFieldWithAnnotation(domainObj, Version.class).toString() : null);
		Object lastModifiedDate = (BeanUtils.getFieldWithAnnotation(domainObj, LastModifiedDate.class) != null ? BeanUtils.getFieldWithAnnotation(domainObj, LastModifiedDate.class) : null);
		HeaderUtils.evaluateHeaderConditions(headers, etag, lastModifiedDate);

		try (InputStream content = info.getImpementation().getContent(domainObj)) {
			if (content == null) {
				throw new ResourceNotFoundException();
			}
		}

		info.getImpementation().unsetContent(domainObj);

		// re-fetch to make sure we have the latest
		if (BeanUtils.hasFieldWithAnnotation(domainObj, Version.class)) {
			domainObj = findOne(repositories, info.getDomainObjectClass(), id);
		}

		if (BeanUtils.hasFieldWithAnnotation(domainObj, MimeType.class)) {
			BeanUtils.setFieldWithAnnotation(domainObj, MimeType.class, null);
		}

		save(repositories, domainObj);

		response.setStatus(HttpStatus.NO_CONTENT.value());
	}


	protected void handleMultipart(HttpServletRequest request, HttpServletResponse response, HttpHeaders headers, String store, String id, InputStream content, MediaType mimeType, String originalFilename)
			throws HttpRequestMethodNotSupportedException, IOException {

		ContentStoreInfo info = ContentStoreUtils.findContentStore(storeService, store);

		if (info == null) {
			throw new IllegalArgumentException(
					String.format("Store for path %s not found", store));
		}

		Object domainObj = findOne(repositories, info.getDomainObjectClass(), id);

		String etag = (BeanUtils.getFieldWithAnnotation(domainObj, Version.class) != null ? BeanUtils.getFieldWithAnnotation(domainObj, Version.class).toString() : null);
		Object lastModifiedDate = (BeanUtils.getFieldWithAnnotation(domainObj, LastModifiedDate.class) != null ? BeanUtils.getFieldWithAnnotation(domainObj, LastModifiedDate.class) : null);
		HeaderUtils.evaluateHeaderConditions(headers, etag, lastModifiedDate);

		boolean isNew = true;
		if (BeanUtils.hasFieldWithAnnotation(domainObj, ContentId.class)) {
			isNew = (BeanUtils.getFieldWithAnnotation(domainObj, ContentId.class) == null);
		}

		if (BeanUtils.hasFieldWithAnnotation(domainObj, MimeType.class)) {
			BeanUtils.setFieldWithAnnotation(domainObj, MimeType.class, mimeType.toString());
		}

		if (originalFilename != null && StringUtils.hasText(originalFilename)) {
			if (BeanUtils.hasFieldWithAnnotation(domainObj, OriginalFileName.class)) {
				BeanUtils.setFieldWithAnnotation(domainObj, OriginalFileName.class, originalFilename);
			}
		}

		info.getImpementation().setContent(domainObj, content);

		save(repositories, domainObj);

		if (isNew) {
			response.setStatus(HttpStatus.CREATED.value());
		}
		else {
			response.setStatus(HttpStatus.OK.value());
		}
	}
}