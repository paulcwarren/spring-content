package org.springframework.data.rest.extensions.contentsearch;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import internal.org.springframework.content.rest.controllers.BadRequestException;
import internal.org.springframework.content.rest.mappings.ContentHandlerMapping.StoreType;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.data.rest.webmvc.support.DefaultedPageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.core.EmbeddedWrappers;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import static org.springframework.data.rest.webmvc.ControllerUtils.EMPTY_RESOURCE_LIST;

@RepositoryRestController
public class ContentSearchRestController /* extends AbstractRepositoryRestController */ {

	private static final EmbeddedWrappers WRAPPERS = new EmbeddedWrappers(false);
	private static final String ENTITY_CONTENTSEARCH_MAPPING = "/{repository}/searchContent";
	private static final String ENTITY_SEARCHMETHOD_MAPPING = "/{repository}/searchContent/findKeyword";
	private static final String PROPERTY_SEARCHMETHOD_MAPPING = "/{repository}/searchContent/{contentProperty}/{searchMethod}";

	private static Map<String, Method> searchMethods = new HashMap<>();

	private Repositories repositories;
	private ContentStoreService stores;
	private PagedResourcesAssembler<Object> pagedResourcesAssembler;

	private ReflectionService reflectionService;

	static {
		searchMethods.put("search", ReflectionUtils.findMethod(Searchable.class,"search", new Class<?>[] { String.class }));
		searchMethods.put("findKeyword", ReflectionUtils.findMethod(Searchable.class,"findKeyword", new Class<?>[] { String.class }));
	}

	@Autowired
	public ContentSearchRestController(Repositories repositories,
			ContentStoreService stores, PagedResourcesAssembler<Object> assembler) {
		// super(assembler);

		this.repositories = repositories;
		this.stores = stores;
		this.pagedResourcesAssembler = assembler;

		this.reflectionService = new ReflectionServiceImpl();
	}

	public void setReflectionService(ReflectionService reflectionService) {
		this.reflectionService = reflectionService;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@StoreType("contentstore")
	@ResponseBody
	@RequestMapping(value = ENTITY_CONTENTSEARCH_MAPPING, method = RequestMethod.GET)
	public CollectionModel<?> searchContent(RootResourceInformation repoInfo,
			DefaultedPageable pageable,
			Sort sort,
			PersistentEntityResourceAssembler assembler,
			@PathVariable String repository,
			@RequestParam(name = "queryString") String queryString)
			throws HttpRequestMethodNotSupportedException {

		return searchContentInternal(repoInfo, pageable, sort, assembler, "search", new String[]{queryString});
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@StoreType("contentstore")
	@ResponseBody
	@RequestMapping(value = ENTITY_SEARCHMETHOD_MAPPING, method = RequestMethod.GET)
	public CollectionModel<?> searchContent(RootResourceInformation repoInfo,
										   DefaultedPageable pageable,
										   Sort sort,
										   PersistentEntityResourceAssembler assembler,
										   @PathVariable String repository,
										   @RequestParam(name = "keyword") List<String> keywords)
			throws HttpRequestMethodNotSupportedException {

		return searchContentInternal(repoInfo, pageable, sort, assembler, "findKeyword", keywords.toArray(new String[]{}));
	}

	CollectionModel<?> searchContentInternal(RootResourceInformation repoInfo,
			DefaultedPageable pageable,
			Sort sort,
			PersistentEntityResourceAssembler assembler,
			String searchMethod,
			String[] keywords) {

		ContentStoreInfo[] infos = stores.getStores(ContentStore.class,
				new StoreFilter() {
					@Override
					public boolean matches(ContentStoreInfo info) {
						return repoInfo.getDomainType()
								.equals(info.getDomainObjectClass());
					}
				});

		if (infos.length == 0) {
			throw new ResourceNotFoundException("Entity has no content associations");
		}

		if (infos.length > 1) {
			throw new IllegalStateException(
					String.format("Too many content assocation for Entity %s",
							repoInfo.getDomainType().getCanonicalName()));
		}

		ContentStoreInfo info = infos[0];

		ContentStore<Object, Serializable> store = info.getImpementation();
		if (store instanceof Searchable == false) {
			throw new ResourceNotFoundException("Entity content is not searchable");
		}

		Method method = searchMethods.get(searchMethod);
		if (method == null) {
			throw new ResourceNotFoundException(
					String.format("Invalid search: %s", searchMethod));
		}

		if (keywords == null || keywords.length == 0) {
			throw new BadRequestException();
		}

		List contentIds = (List) reflectionService.invokeMethod(method, store, keywords[0]);

		List<Object> results = new ArrayList<>();
		if (contentIds != null && contentIds.size() > 0) {

			Class<?> entityType = repoInfo.getDomainType();

			Field idField = BeanUtils.findFieldWithAnnotation(entityType, Id.class);
			if (idField == null) {
				idField = BeanUtils.findFieldWithAnnotation(entityType,
						javax.persistence.Id.class);
			}

			Field contentIdField = BeanUtils.findFieldWithAnnotation(entityType,
					ContentId.class);

			if (idField.equals(contentIdField)) {
				for (Object contentId : contentIds) {
					Optional<Object> entity = repoInfo.getInvoker()
							.invokeFindById(contentId.toString());
					if (entity.isPresent()) {
						results.add(entity.get());
					}
				}
			}
			else {
				RepositoryInvoker invoker = repoInfo.getInvoker();
				Iterable<?> entities = pageable.getPageable() != null ? invoker.invokeFindAll(pageable.getPageable()) : invoker.invokeFindAll(sort);

				for (Object entity : entities) {
					for (Object contentId : contentIds) {

						Object candidate = BeanUtils.getFieldWithAnnotation(entity,
								ContentId.class);
						if (contentId.equals(candidate)) {
							results.add(entity);
						}
					}
				}
			}

//			return ResponseEntity.ok(toResources(results, assembler, pagedResourcesAssembler, entityType, null));
		}

		ResourceMetadata metadata = repoInfo.getResourceMetadata();
		CollectionModel<?> result = toCollectionModel(results, assembler, metadata.getDomainType(), Optional.empty());
		return result;

//		return ResponseEntity.ok(new Resources(ControllerUtils.EMPTY_RESOURCE_LIST));
	}

//	public static Resources<?> toResources(Iterable<?> source,
//										   PersistentEntityResourceAssembler assembler,
//										   PagedResourcesAssembler resourcesAssembler,
//										   Class<?> domainType,
//										   Link baseLink) {
//
//		if (source instanceof Page) {
//			Page<Object> page = (Page<Object>) source;
//			return entitiesToResources(page, assembler, resourcesAssembler, domainType, baseLink);
//		}
//		else if (source instanceof Iterable) {
//			return entitiesToResources((Iterable<Object>) source, assembler, domainType);
//		}
//		else {
//			return new Resources(EMPTY_RESOURCE_LIST);
//		}
//	}

//	protected static Resources<?> entitiesToResources(Page<Object> page,
//											   PersistentEntityResourceAssembler assembler, PagedResourcesAssembler resourcesAssembler, Class<?> domainType,
//											   Link baseLink) {
//
//		if (page.getContent().isEmpty()) {
//			return resourcesAssembler.toEmptyResource(page, domainType, baseLink);
//		}
//
//		return baseLink == null ? resourcesAssembler.toResource(page, assembler)
//				: resourcesAssembler.toResource(page, assembler, baseLink);
//	}

	protected CollectionModel<?> entitiesToResources(Page<Object> page, PersistentEntityResourceAssembler assembler,
			Class<?> domainType, Optional<Link> baseLink) {

		if (page.getContent().isEmpty()) {
			return baseLink.<PagedModel<?>> map(it -> pagedResourcesAssembler.toEmptyModel(page, domainType, it))//
					.orElseGet(() -> pagedResourcesAssembler.toEmptyModel(page, domainType));
		}

		return baseLink.map(it -> pagedResourcesAssembler.toModel(page, assembler, it))//
				.orElseGet(() -> pagedResourcesAssembler.toModel(page, assembler));
	}

	protected CollectionModel<?> entitiesToResources(Iterable<Object> entities,
			PersistentEntityResourceAssembler assembler, Class<?> domainType) {

		if (!entities.iterator().hasNext()) {

			List<Object> content = Arrays.<Object> asList(WRAPPERS.emptyCollectionOf(domainType));
			return new CollectionModel<Object>(content, getDefaultSelfLink());
		}

		List<EntityModel<Object>> resources = new ArrayList<EntityModel<Object>>();

		for (Object obj : entities) {
			resources.add(obj == null ? null : assembler.toModel(obj));
		}

		return new CollectionModel<EntityModel<Object>>(resources, getDefaultSelfLink());
	}

	protected static Link getDefaultSelfLink() {
		return new Link(
				ServletUriComponentsBuilder.fromCurrentRequest().build().toUriString());
	}

	protected CollectionModel<?> toCollectionModel(Iterable<?> source, PersistentEntityResourceAssembler assembler,
			Class<?> domainType, Optional<Link> baseLink) {

		if (source instanceof Page) {
			Page<Object> page = (Page<Object>) source;
			return entitiesToResources(page, assembler, domainType, baseLink);
		} else if (source instanceof Iterable) {
			return entitiesToResources((Iterable<Object>) source, assembler, domainType);
		} else {
			return new CollectionModel(EMPTY_RESOURCE_LIST);
		}
	}
}
