package org.springframework.data.rest.extensions.versioning;

import internal.org.springframework.content.rest.utils.RepositoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.utils.ReflectionService;
import org.springframework.content.commons.utils.ReflectionServiceImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.core.EmbeddedWrappers;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ReflectionUtils;
import org.springframework.versions.Lock;
import org.springframework.versions.LockingAndVersioningRepository;
import org.springframework.versions.VersionInfo;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.lang.reflect.Method;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.springframework.data.rest.webmvc.ControllerUtils.EMPTY_RESOURCE_LIST;

@RepositoryRestController
public class LockingAndVersioningRestController {

	private static final EmbeddedWrappers WRAPPERS = new EmbeddedWrappers(false);

	private static final String ENTITY_LOCK_MAPPING = "/{repository}/{id}/lock";
	private static final String ENTITY_VERSION_MAPPING = "/{repository}/{id}/version";
	private static final String ENTITY_FINDALLLATESTVERSION_MAPPING = "/{repository}/findAllLatestVersion";
	private static final String FINDALLVERSIONS_METHOD_MAPPING = "/{repository}/{id}/findAllVersions";

	private static Method LOCK_METHOD = null;
	private static Method UNLOCK_METHOD = null;
	private static Method VERSION_METHOD = null;
	private static Method FINDALLLATESTVERSION_METHOD = null;
	private static Method FINDALLVERSIONS_METHOD = null;

	static {
		LOCK_METHOD = ReflectionUtils.findMethod(LockingAndVersioningRepository.class, "lock", Object.class);
		UNLOCK_METHOD = ReflectionUtils.findMethod(LockingAndVersioningRepository.class, "unlock", Object.class);
		VERSION_METHOD = ReflectionUtils.findMethod(LockingAndVersioningRepository.class, "version", Object.class, VersionInfo.class);
		FINDALLLATESTVERSION_METHOD = ReflectionUtils.findMethod(LockingAndVersioningRepository.class, "findAllLatestVersion");
		FINDALLVERSIONS_METHOD = ReflectionUtils.findMethod(LockingAndVersioningRepository.class, "findAllVersions", Object.class);
	}

	private Repositories repositories;
	private PagedResourcesAssembler<Object> pagedResourcesAssembler;

	private ReflectionService reflectionService;

	@Autowired
	public LockingAndVersioningRestController(Repositories repositories, PagedResourcesAssembler<Object> assembler) {
		this.repositories = repositories;
		this.pagedResourcesAssembler = assembler;

		this.reflectionService = new ReflectionServiceImpl();
	}

	public void setReflectionService(ReflectionService reflectionService) {
		this.reflectionService = reflectionService;
	}

	@ResponseBody
	@RequestMapping(value = ENTITY_LOCK_MAPPING, method = RequestMethod.PUT)
	public ResponseEntity<Resource<?>> lock(RootResourceInformation repoInfo,
											@PathVariable String repository,
											@PathVariable String id, Principal principal)
			throws ResourceNotFoundException, HttpRequestMethodNotSupportedException {

		Object domainObj = repoInfo.getInvoker().invokeFindById(id).get();

		domainObj = ReflectionUtils.invokeMethod(LOCK_METHOD, repositories.getRepositoryFor(domainObj.getClass()).get(), domainObj);

		if (domainObj != null) {
			return new ResponseEntity<>(new LockResource(new Lock(id, principal.getName())), HttpStatus.OK);
		} else {
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		}
	}

	@ResponseBody
	@RequestMapping(value = ENTITY_LOCK_MAPPING, method = RequestMethod.DELETE)
	public ResponseEntity<Resource<?>> unlock(RootResourceInformation repoInfo,
											  @PathVariable String repository,
											  @PathVariable String id, Principal principal)
			throws ResourceNotFoundException, HttpRequestMethodNotSupportedException {

		Object domainObj = repoInfo.getInvoker().invokeFindById(id).get();

		domainObj = ReflectionUtils.invokeMethod(UNLOCK_METHOD, repositories.getRepositoryFor(domainObj.getClass()).get(), domainObj);

		if (domainObj != null) {
			return ResponseEntity.ok().build();
		} else {
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		}
	}

	@ResponseBody
	@RequestMapping(value = ENTITY_VERSION_MAPPING, method = RequestMethod.PUT)
	public ResponseEntity<Resource<?>> version(RootResourceInformation repoInfo,
											   @PathVariable String repository,
											   @PathVariable String id,
											   @RequestBody VersionInfo info,
											   Principal principal,
											   PersistentEntityResourceAssembler assembler)
			throws ResourceNotFoundException, HttpRequestMethodNotSupportedException {

		Object domainObj = repoInfo.getInvoker().invokeFindById(id).get();

		domainObj = ReflectionUtils.invokeMethod(VERSION_METHOD, repositories.getRepositoryFor(domainObj.getClass()).get(), domainObj, info);

		if (domainObj != null) {
			return new ResponseEntity(assembler.toResource(domainObj), HttpStatus.OK);
		} else {
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		}
	}

	@ResponseBody
	@RequestMapping(value = ENTITY_FINDALLLATESTVERSION_MAPPING, method = RequestMethod.GET)
	public ResponseEntity<?> findAllLatestVersion(RootResourceInformation repoInfo,
												  PersistentEntityResourceAssembler assembler,
												  @PathVariable String repository)
			throws ResourceNotFoundException, HttpRequestMethodNotSupportedException {

		RepositoryInformation repositoryInfo = RepositoryUtils.findRepositoryInformation(repositories, repository);
		Class<?> domainType = repositoryInfo.getDomainType();

		List result = (List)ReflectionUtils.invokeMethod(FINDALLLATESTVERSION_METHOD, repositories.getRepositoryFor(domainType).get());

		return ResponseEntity.ok(toResources(result, assembler, this.pagedResourcesAssembler, domainType, null));
	}

	@ResponseBody
	@RequestMapping(value = FINDALLVERSIONS_METHOD_MAPPING, method = RequestMethod.GET)
	public ResponseEntity<?> findAllVersions(RootResourceInformation repoInfo,
											 PersistentEntityResourceAssembler assembler,
											 @PathVariable String repository,
											 @PathVariable String id)
			throws ResourceNotFoundException, HttpRequestMethodNotSupportedException {

		Object domainObj = repoInfo.getInvoker().invokeFindById(id).get();

		List result = (List)ReflectionUtils.invokeMethod(FINDALLVERSIONS_METHOD, repositories.getRepositoryFor(domainObj.getClass()).get(), domainObj);

		return ResponseEntity.ok(toResources(result, assembler, this.pagedResourcesAssembler, domainObj.getClass(), null));
	}

	public static Resources<?> toResources(Iterable<?> source,
										   PersistentEntityResourceAssembler assembler,
										   PagedResourcesAssembler resourcesAssembler,
										   Class<?> domainType,
										   Link baseLink) {

		if (source instanceof Page) {
			Page<Object> page = (Page<Object>) source;
			return entitiesToResources(page, assembler, resourcesAssembler, domainType, baseLink);
		}
		else if (source instanceof Iterable) {
			return entitiesToResources((Iterable<Object>) source, assembler, domainType);
		}
		else {
			return new Resources(EMPTY_RESOURCE_LIST);
		}
	}

	protected static Resources<?> entitiesToResources(Page<Object> page,
											   PersistentEntityResourceAssembler assembler, PagedResourcesAssembler resourcesAssembler, Class<?> domainType,
											   Link baseLink) {

		if (page.getContent().isEmpty()) {
			return resourcesAssembler.toEmptyResource(page, domainType, baseLink);
		}

		return baseLink == null ? resourcesAssembler.toResource(page, assembler)
				: resourcesAssembler.toResource(page, assembler, baseLink);
	}

	protected static Resources<?> entitiesToResources(Iterable<Object> entities,
			PersistentEntityResourceAssembler assembler, Class<?> domainType) {

		if (!entities.iterator().hasNext()) {

			List<Object> content = Arrays
					.<Object>asList(WRAPPERS.emptyCollectionOf(domainType));
			return new Resources<Object>(content, getDefaultSelfLink());
		}

		List<Resource<Object>> resources = new ArrayList<Resource<Object>>();

		for (Object obj : entities) {
			resources.add(obj == null ? null : assembler.toResource(obj));
		}

		return new Resources<Resource<Object>>(resources, getDefaultSelfLink());
	}

	protected static Link getDefaultSelfLink() {
		return new Link(
				ServletUriComponentsBuilder.fromCurrentRequest().build().toUriString());
	}
}
