package org.springframework.data.rest.extensions.versioning;

import java.lang.reflect.Method;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import internal.org.springframework.content.rest.utils.ControllerUtils;
import internal.org.springframework.content.rest.utils.RepositoryUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.utils.ReflectionService;
import org.springframework.content.commons.utils.ReflectionServiceImpl;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ReflectionUtils;
import org.springframework.versions.LockingAndVersioningRepository;
import org.springframework.versions.VersionInfo;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@RepositoryRestController
public class LockingAndVersioningRestController {

	private static final String ENTITY_LOCK_MAPPING = "/{repository}/{id}/lock";
	private static final String ENTITY_VERSION_MAPPING = "/{repository}/{id}/version";
	private static final String ENTITY_FINDALLLATESTVERSION_MAPPING = "/{repository}/findAllVersionsLatest";
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
		FINDALLLATESTVERSION_METHOD = ReflectionUtils.findMethod(LockingAndVersioningRepository.class, "findAllVersionsLatest");
		FINDALLVERSIONS_METHOD = ReflectionUtils.findMethod(LockingAndVersioningRepository.class, "findAllVersions", Object.class);
	}

    private final Repositories repositories;
	private final PagedResourcesAssembler<Object> pagedResourcesAssembler;


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
	public ResponseEntity<EntityModel<?>>  lock(RootResourceInformation repoInfo,
											@PathVariable String repository,
											@PathVariable String id, Principal principal)
			throws ResourceNotFoundException, HttpRequestMethodNotSupportedException {

		Object domainObj = repoInfo.getInvoker().invokeFindById(id).get();

		domainObj = ReflectionUtils.invokeMethod(LOCK_METHOD, repositories.getRepositoryFor(domainObj.getClass()).get(), domainObj);

		if (domainObj != null) {
			return ResponseEntity.ok().build();
		} else {
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		}
	}

	@ResponseBody
	@RequestMapping(value = ENTITY_LOCK_MAPPING, method = RequestMethod.DELETE)
	public ResponseEntity<EntityModel<?>>  unlock(RootResourceInformation repoInfo,
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
	public ResponseEntity<EntityModel<?>>  version(RootResourceInformation repoInfo,
											   @PathVariable String repository,
											   @PathVariable String id,
											   @RequestBody VersionInfo info,
											   Principal principal,
											   PersistentEntityResourceAssembler assembler)
			throws ResourceNotFoundException, HttpRequestMethodNotSupportedException {

		Object domainObj = repoInfo.getInvoker().invokeFindById(id).get();

		domainObj = ReflectionUtils.invokeMethod(VERSION_METHOD, repositories.getRepositoryFor(domainObj.getClass()).get(), domainObj, info);

		if (domainObj != null) {
			return new ResponseEntity(assembler.toFullResource(domainObj), HttpStatus.OK);
		} else {
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		}
	}

	@ResponseBody
	@RequestMapping(value = ENTITY_FINDALLLATESTVERSION_MAPPING, method = RequestMethod.GET)
	public CollectionModel<?>  findAllLatestVersion(RootResourceInformation repoInfo,
												  PersistentEntityResourceAssembler assembler,
												  @PathVariable String repository)
			throws ResourceNotFoundException, HttpRequestMethodNotSupportedException {

		RepositoryInformation repositoryInfo = RepositoryUtils.findRepositoryInformation(repositories, repository);
		Class<?> domainType = repositoryInfo.getDomainType();

		List results = (List)ReflectionUtils.invokeMethod(FINDALLLATESTVERSION_METHOD, repositories.getRepositoryFor(domainType).get());

        ResourceMetadata metadata = repoInfo.getResourceMetadata();
        CollectionModel<?> result = ControllerUtils.toCollectionModel(results, pagedResourcesAssembler, assembler, metadata.getDomainType(), Optional.empty());
        return result;
	}

	@ResponseBody
	@RequestMapping(value = FINDALLVERSIONS_METHOD_MAPPING, method = RequestMethod.GET)
	public CollectionModel<?> findAllVersions(RootResourceInformation repoInfo,
											 PersistentEntityResourceAssembler assembler,
											 @PathVariable String repository,
											 @PathVariable String id)
			throws ResourceNotFoundException, HttpRequestMethodNotSupportedException {

		Object domainObj = repoInfo.getInvoker().invokeFindById(id).get();

		List results = (List)ReflectionUtils.invokeMethod(FINDALLVERSIONS_METHOD, repositories.getRepositoryFor(domainObj.getClass()).get(), domainObj);

        ResourceMetadata metadata = repoInfo.getResourceMetadata();
        CollectionModel<?> result = ControllerUtils.toCollectionModel(results, pagedResourcesAssembler, assembler, metadata.getDomainType(), Optional.empty());
        return result;
	}
}
