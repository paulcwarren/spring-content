package internal.org.springframework.content.rest.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;

import javax.persistence.Version;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import internal.org.springframework.content.rest.annotations.ContentRestController;
import internal.org.springframework.content.rest.mappings.ContentHandlerMapping.StoreType;
import internal.org.springframework.content.rest.mappings.StoreByteRangeHttpRequestHandler;
import internal.org.springframework.content.rest.utils.ContentStoreUtils;
import internal.org.springframework.content.rest.utils.HeaderUtils;
import internal.org.springframework.content.rest.utils.PersistentEntityUtils;

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
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
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
public class ContentPropertyCollectionRestController
		extends AbstractContentPropertyController {

	private static final String BASE_MAPPING = "/{repository}/{id}/{contentProperty}";

	private Repositories repositories;
	private ContentStoreService storeService;
	private StoreByteRangeHttpRequestHandler handler;
	private RepositoryInvokerFactory repoInvokerFactory;

	@Autowired
	public ContentPropertyCollectionRestController(ApplicationContext context, ContentStoreService stores, StoreByteRangeHttpRequestHandler handler, RepositoryInvokerFactory repoInvokerFactory) {
		super();
		try {
			this.repositories = context.getBean(Repositories.class);
		}
		catch (BeansException be) {
			this.repositories = new Repositories(context);
		}
		this.storeService = stores;
		this.handler = handler;
		this.repoInvokerFactory = repoInvokerFactory;
	}

	@StoreType("contentstore")
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.GET)
	public void get(HttpServletRequest request,
			HttpServletResponse response, @PathVariable String repository,
			@PathVariable String id, @PathVariable String contentProperty,
			@RequestHeader(value = "Accept", required = false) String mimeType)
			throws HttpRequestMethodNotSupportedException {

		Object domainObj = findOne(repoInvokerFactory, repositories, repository, id);

		PersistentEntity<?, ?> entity = repositories.getPersistentEntity(domainObj.getClass());
		if (null == entity)
			throw new ResourceNotFoundException();

		PersistentProperty<?> property = this.getContentPropertyDefinition(entity, contentProperty);
		if (PersistentEntityUtils.isPropertyMultiValued(property)) {
			throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED);
		}

		PersistentPropertyAccessor accessor = property.getOwner().getPropertyAccessor(domainObj);
		Object propVal = accessor.getProperty(property);
		if (propVal == null)
			throw new ResourceNotFoundException("No content");

		if (!BeanUtils.hasFieldWithAnnotation(propVal, ContentId.class)) {
			throw new ResourceNotFoundException("Missing @ContentId");
		}

		Serializable cid = (Serializable) BeanUtils.getFieldWithAnnotation(propVal, ContentId.class);
		if (cid == null) {
			throw new ResourceNotFoundException();
		}

		ContentStoreInfo info = ContentStoreUtils.findContentStore(storeService, propVal.getClass());
		if (info == null)
			throw new IllegalStateException(String.format("Unable to find a content store for %s", repository));

		List<MediaType> mimeTypes = MediaType.parseMediaTypes(mimeType);
		if (mimeTypes.size() == 0) {
			mimeTypes.add(MediaType.ALL);
		}

		ContentStore<Object, Serializable> store = info.getImpementation();
		ContentStoreUtils.ResourcePlan resourcePlan = ContentStoreUtils.resolveResource(store, domainObj, propVal, mimeTypes);

		Object version = BeanUtils.getFieldWithAnnotation(domainObj, Version.class);
		long lastModified = -1;
		try {
			lastModified = resourcePlan.getResource().lastModified();
		} catch (IOException e) {}
		if(new ServletWebRequest(request, response).checkNotModified(version != null ? version.toString() : null, lastModified)) {
			return;
		}

		request.setAttribute("SPRING_CONTENT_RESOURCE", resourcePlan.getResource());
		request.setAttribute("SPRING_CONTENT_CONTENTTYPE", resourcePlan.getMimeType());

		try {
			handler.handleRequest(request, response);
		}
		catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, format("Failed to handle request for %s", id), e);
		}
	}

	@StoreType("contentstore")
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.PUT, headers = "content-type!=multipart/form-data")
	@ResponseBody
	public void putContent(HttpServletRequest request, HttpServletResponse response,
												  @RequestHeader HttpHeaders headers,
												  @PathVariable String repository,
												  @PathVariable String id,
												  @PathVariable String contentProperty)
			throws IOException, HttpRequestMethodNotSupportedException,
			InstantiationException, IllegalAccessException {

		boolean isNew = this.saveContentInternal(headers, repositories, storeService,
				repository, id, contentProperty, request.getRequestURI(),
				request.getHeader("Content-Type"), null, request.getInputStream());

		if (isNew) {
			response.setStatus(HttpStatus.CREATED.value());
		}
		else {
			response.setStatus(HttpStatus.OK.value());
		}
	}

	@StoreType("contentstore")
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.POST, headers = "content-type!=multipart/form-data")
	@ResponseBody
	public void postContent(HttpServletRequest request, HttpServletResponse response,
												   @RequestHeader HttpHeaders headers,
												   @PathVariable String repository,
												   @PathVariable String id,
												   @PathVariable String contentProperty)
			throws IOException, HttpRequestMethodNotSupportedException,
			InstantiationException, IllegalAccessException {

		boolean isNew = this.saveContentInternal(headers, repositories, storeService,
				repository, id, contentProperty, request.getRequestURI(),
				request.getHeader("Content-Type"), null, request.getInputStream());

		if (isNew) {
			response.setStatus(HttpStatus.CREATED.value());
		}
		else {
			response.setStatus(HttpStatus.OK.value());
		}
	}

	@StoreType("contentstore")
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.POST, headers = "content-type=multipart/form-data")
	@ResponseBody
	public void postMultipartContent(HttpServletRequest request, HttpServletResponse response,
															@RequestHeader HttpHeaders headers,
															@PathVariable String repository,
															@PathVariable String id,
															@PathVariable String contentProperty,
															@RequestParam("file") MultipartFile multiPart)
			throws IOException, HttpRequestMethodNotSupportedException {

		boolean isNew = this.saveContentInternal(headers, repositories, storeService,
				repository, id, contentProperty, request.getRequestURI(),
				multiPart.getContentType(), multiPart.getOriginalFilename(),
				multiPart.getInputStream());

		if (isNew) {
			response.setStatus(HttpStatus.CREATED.value());
		}
		else {
			response.setStatus(HttpStatus.OK.value());
		}
	}

	@StoreType("contentstore")
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.DELETE, headers = "accept!=application/hal+json")
	public void delete(HttpServletRequest request, HttpServletResponse response,
						@RequestHeader HttpHeaders headers,
						@PathVariable String repository,
					   	@PathVariable String id,
						@PathVariable String contentProperty)
			throws HttpRequestMethodNotSupportedException {

		Object domainObj = findOne(repoInvokerFactory, repositories, repository, id);

		String etag = (BeanUtils.getFieldWithAnnotation(domainObj, Version.class) != null ? BeanUtils.getFieldWithAnnotation(domainObj, Version.class).toString() : null);
		Object lastModifiedDate = (BeanUtils.getFieldWithAnnotation(domainObj, LastModifiedDate.class) != null ? BeanUtils.getFieldWithAnnotation(domainObj, LastModifiedDate.class) : null);
		HeaderUtils.evaluateHeaderConditions(headers, etag, lastModifiedDate);

		PersistentEntity<?, ?> entity = repositories.getPersistentEntity(domainObj.getClass());
		if (null == entity) {
			response.setStatus(HttpStatus.NOT_FOUND.value());
			return;
		}

		PersistentProperty<?> property = this.getContentPropertyDefinition(entity,contentProperty);
		if (PersistentEntityUtils.isPropertyMultiValued(property)) {
			response.setStatus(HttpStatus.METHOD_NOT_ALLOWED.value());
			return;
		}

		PersistentPropertyAccessor accessor = property.getOwner().getPropertyAccessor(domainObj);
		Object propVal = accessor.getProperty(property);
		if (propVal == null) {
			response.setStatus(HttpStatus.NOT_FOUND.value());
			return;
		}

		if (!BeanUtils.hasFieldWithAnnotation(propVal, ContentId.class)) {
			response.setStatus(HttpStatus.NOT_FOUND.value(), "Missing @ContentId");
			return;
		}

		ContentStoreInfo info = ContentStoreUtils.findContentStore(storeService,propVal.getClass());
		if (info == null)
			throw new IllegalStateException(
					String.format("Unable to find a content store for %s", repository));

		info.getImplementation(ContentStore.class).unsetContent(propVal);

		if (BeanUtils.hasFieldWithAnnotation(propVal, MimeType.class)) {
			BeanUtils.setFieldWithAnnotation(propVal, MimeType.class, null);
		}

		save(repositories, domainObj);

		response.setStatus(HttpStatus.NO_CONTENT.value());
		return;
	}

	private boolean saveContentInternal(HttpHeaders headers,
										Repositories repositories,
										ContentStoreService stores,
										String repository,
										String id,
										String contentProperty,
										String requestUri,
										String mimeType,
										String originalFileName,
										InputStream stream)
			throws HttpRequestMethodNotSupportedException {

		Object domainObj = findOne(repoInvokerFactory, repositories, repository, id);

		String etag = (BeanUtils.getFieldWithAnnotation(domainObj, Version.class) != null ? BeanUtils.getFieldWithAnnotation(domainObj, Version.class).toString() : null);
		Object lastModifiedDate = (BeanUtils.getFieldWithAnnotation(domainObj, LastModifiedDate.class) != null ? BeanUtils.getFieldWithAnnotation(domainObj, LastModifiedDate.class) : null);
		HeaderUtils.evaluateHeaderConditions(headers, etag, lastModifiedDate);

		PersistentEntity<?, ?> entity = repositories.getPersistentEntity(domainObj.getClass());
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

		ContentStoreInfo info = ContentStoreUtils.findContentStore(stores, contentEntityClass);
		if (info == null)
			throw new IllegalStateException(String.format("Unable to find a content store for %s", repository));

		// null single-valued content property
		if (propVal == null && !PersistentEntityUtils.isPropertyMultiValued(property)) {
			propVal = instantiate(info.getDomainObjectClass());
			accessor.setProperty(property, propVal);
		}
		// null multi-valued content property
		else if (propVal == null
				&& PersistentEntityUtils.isPropertyMultiValued(property)) {
			// TODO: instantiate an instance of the required arrays or collection/set/list
			// and then
			// an instance of the content property and add it to the list
		}
		// non-null multi-valued property
		else if (propVal != null
				&& PersistentEntityUtils.isPropertyMultiValued(property)) {

			// instantiate an instance of the member type and add it
			if (property.isArray()) {
				Class<?> memberType = propVal.getClass().getComponentType();
				Object member = instantiate(memberType);
				Object newArray = Array.newInstance(propVal.getClass(),
						Array.getLength(propVal) + 1);
				System.arraycopy(propVal, 0, newArray, 0, Array.getLength(propVal));
				Array.set(newArray, Array.getLength(propVal), member);
				accessor.setProperty(property, newArray);
				propVal = member;

			}
			else if (property.isCollectionLike()) {
				Class<?> memberType = property.getActualType();
				Object member = instantiate(memberType);
				@SuppressWarnings("unchecked")
				Collection<Object> contentCollection = (Collection<Object>) accessor
						.getProperty(property);
				contentCollection.add(member);
				propVal = member;
			}
		}

		boolean isNew = true;
		if (BeanUtils.hasFieldWithAnnotation(propVal, ContentId.class)) {
			isNew = (BeanUtils.getFieldWithAnnotation(propVal, ContentId.class) == null);
		}

		if (BeanUtils.hasFieldWithAnnotation(propVal, MimeType.class)) {
			BeanUtils.setFieldWithAnnotation(propVal, MimeType.class, mimeType);
		}

		if (originalFileName != null && StringUtils.hasText(originalFileName)) {
			if (BeanUtils.hasFieldWithAnnotation(propVal, OriginalFileName.class)) {
				BeanUtils.setFieldWithAnnotation(propVal, OriginalFileName.class,
						originalFileName);
			}
		}

		info.getImpementation().setContent(propVal, stream);

		save(repositories, domainObj);

		return isNew;
	}

	private Object instantiate(Class<?> clazz) {
		Object newObject = null;
		try {
			newObject = clazz.newInstance();
		}
		catch (InstantiationException e) {
			e.printStackTrace();
		}
		catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return newObject;
	}
}