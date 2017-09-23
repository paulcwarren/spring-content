package org.springframework.data.rest.webmvc;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.search.Searchable;
import org.springframework.content.commons.storeservice.ContentStoreInfo;
import org.springframework.content.commons.storeservice.ContentStoreService;
import org.springframework.content.commons.storeservice.StoreFilter;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.commons.utils.ReflectionService;
import org.springframework.content.commons.utils.ReflectionServiceImpl;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Resources;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import internal.org.springframework.content.rest.controllers.BadRequestException;
import internal.org.springframework.content.rest.mappings.ContentHandlerMapping.StoreType;

@RepositoryRestController
public class ContentSearchRestController extends AbstractRepositoryRestController {

	private static final String ENTITY_CONTENTSEARCH_MAPPING = "/{repository}/searchContent/{searchMethod}";
	private static final String PROPERTY_CONTENTSEARCH_MAPPING = "/{repository}/searchContent/{contentProperty}/{searchMethod}";

	private static Map<String,Method> searchMethods = new HashMap<>();

	private Repositories repositories;
	private ContentStoreService stores;
	private ReflectionService reflectionService;

	static {
		searchMethods.put("findKeyword", ReflectionUtils.findMethod(Searchable.class, "findKeyword", new Class<?>[]{String.class}));
	}
	
	@Autowired 
	public ContentSearchRestController(Repositories repositories, ContentStoreService stores, PagedResourcesAssembler<Object> assembler) {
		super(assembler);
		
		this.repositories = repositories;
		this.stores = stores;
		this.reflectionService = new ReflectionServiceImpl();
	}
	
	public void setReflectionService(ReflectionService reflectionService) {
		this.reflectionService = reflectionService;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@StoreType("contentstore")
	@RequestMapping(value = ENTITY_CONTENTSEARCH_MAPPING, method = RequestMethod.GET)
	public ResponseEntity<?> searchContent(RootResourceInformation repoInfo,
										   PersistentEntityResourceAssembler assembler, 
									   	   @PathVariable String repository, 
									   	   @PathVariable String searchMethod,
									   	   @RequestParam(name="keyword") List<String> keywords)
			throws HttpRequestMethodNotSupportedException {
		
		ContentStoreInfo[] infos = stores.getStores(ContentStore.class, new StoreFilter() {
			@Override
			public boolean matches(ContentStoreInfo info) {
				return repoInfo.getDomainType().equals(info.getDomainObjectClass());
			}});

		if (infos.length == 0) {
			throw new ResourceNotFoundException("Entity has no content associations");
		}
		
		if (infos.length > 1) {
			throw new IllegalStateException(String.format("Too many content assocation for Entity %s", repoInfo.getDomainType().getCanonicalName()));
		}

		ContentStoreInfo info = infos[0];
		
		ContentStore<Object,Serializable> store = info.getImpementation();
		if (store instanceof Searchable == false) {
			throw new ResourceNotFoundException("Entity content is not searchable");
		}
		
		Method method = searchMethods.get(searchMethod);
		if (method == null) {
			throw new ResourceNotFoundException(String.format("Invalid search: %s", searchMethod));
		}
		
		if (keywords == null || keywords.size() == 0) {
			throw new BadRequestException();
		}
		
		List contentIds = (List)reflectionService.invokeMethod(method, store, keywords.get(0));
		
		if (contentIds != null && contentIds.size() > 0) {
			Class<?> entityType = repoInfo.getDomainType();
			
			Field idField = BeanUtils.findFieldWithAnnotation(entityType, Id.class);
			if (idField == null) {
				idField = BeanUtils.findFieldWithAnnotation(entityType, javax.persistence.Id.class);
			}
			
			Field contentIdField = BeanUtils.findFieldWithAnnotation(entityType, ContentId.class);
			 
			List<Object> results = new ArrayList<>();
			if (idField.equals(contentIdField)) {
				for (Object contentId : contentIds) {
					Object entity = repoInfo.getInvoker().invokeFindOne(contentId.toString());
					results.add(entity);
				}
			} else {
				Pageable pageable = null;
				Iterable<?> entities = repoInfo.getInvoker().invokeFindAll(pageable);
				
				for (Object entity : entities) {
					for (Object contentId : contentIds) {
						
						Object candidate = BeanUtils.getFieldWithAnnotation(entity, ContentId.class);
						if (contentId.equals(candidate)) {
							results.add(entity);
						}
					}
				}
			}
			return ResponseEntity.ok(toResources(results, assembler, entityType, null));
		}

		return ResponseEntity.ok(new Resources(ControllerUtils.EMPTY_RESOURCE_LIST));
	}
}
