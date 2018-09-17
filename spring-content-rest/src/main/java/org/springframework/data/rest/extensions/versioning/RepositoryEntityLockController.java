package org.springframework.data.rest.extensions.versioning;

import internal.org.springframework.content.rest.annotations.ContentRestController;
import internal.org.springframework.content.rest.utils.ContentStoreUtils;
import internal.org.springframework.versions.LockingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.storeservice.ContentStoreInfo;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.rest.webmvc.support.BackendId;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ReflectionUtils;
import org.springframework.versions.Lock;
import org.springframework.versions.LockingAndVersioningRepository;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.security.Principal;

@ContentRestController
public class RepositoryEntityLockController implements ApplicationEventPublisherAware {

    private static final String ENTITY_LOCK_MAPPING = "/{repository}/{id}/lock";
    private static final String ENTITY_VERSION_MAPPING = "/{repository}/{id}/version";

    private static Method LOCK_METHOD = null;

    static {
        LOCK_METHOD = ReflectionUtils.findMethod(LockingAndVersioningRepository.class, "lock", Object.class);
    }

    private Repositories repositories;
    private PagedResourcesAssembler<Object> pagedResourcesAssembler;

    @Autowired(required=false)
    private LockingService locker;

    private ApplicationEventPublisher publisher;

    @Autowired
    public RepositoryEntityLockController(Repositories repositories, PagedResourcesAssembler<Object> assembler) {
        this.repositories = repositories;
        this.pagedResourcesAssembler = assembler;
        this.locker = locker;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.context.ApplicationEventPublisherAware#setApplicationEventPublisher(org.springframework.context.ApplicationEventPublisher)
     */
    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    /**
     * <code>GET /{repository}</code> - Returns the collection resource (paged or unpaged).
     *
     * @param id
     * @param principal
     * @return
     * @throws ResourceNotFoundException
     * @throws HttpRequestMethodNotSupportedException
     */
    @ResponseBody
    @RequestMapping(value = ENTITY_LOCK_MAPPING, method = RequestMethod.GET)
    public ResponseEntity<Resource<?>> getLock(@PathVariable String id,
                                                  Principal principal)
            throws ResourceNotFoundException, HttpRequestMethodNotSupportedException {

        if (locker == null) {
            throw new IllegalStateException("locking and versioning not enabled");
        }

        if (locker.isLockOwner(id, principal)) {
            return new ResponseEntity<>(new LockResource(new Lock(id, principal.getName())), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @ResponseBody
    @RequestMapping(value = ENTITY_LOCK_MAPPING, method = RequestMethod.PUT)
    public ResponseEntity<Resource<?>> setLock(@PathVariable String id,
                                               Principal principal)
            throws ResourceNotFoundException, HttpRequestMethodNotSupportedException {

        if (locker == null) {
            throw new IllegalStateException("locking and versioning not enabled");
        }

        if (locker.lock(id, principal)) {
            return new ResponseEntity<>(new LockResource(new Lock(id, principal.getName())), HttpStatus.OK);
        }

        return null;
    }
}
