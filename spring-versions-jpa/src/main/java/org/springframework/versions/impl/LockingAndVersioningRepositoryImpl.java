package org.springframework.versions.impl;

import internal.org.springframework.versions.AuthenticationFacade;
import internal.org.springframework.versions.LockingService;
import internal.org.springframework.versions.jpa.CloningService;
import internal.org.springframework.versions.jpa.EntityInformationFacade;
import internal.org.springframework.versions.jpa.VersioningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.versions.AncestorId;
import org.springframework.versions.AncestorRootId;
import org.springframework.versions.LockOwner;
import org.springframework.versions.LockOwnerException;
import org.springframework.versions.LockingAndVersioningException;
import org.springframework.versions.LockingAndVersioningRepository;
import org.springframework.versions.SuccessorId;
import org.springframework.versions.VersionInfo;
import org.springframework.versions.VersionNumber;

import javax.persistence.EntityManager;
import javax.persistence.Id;
import java.io.Serializable;
import java.security.Principal;
import java.util.List;

import static java.lang.String.format;

public class LockingAndVersioningRepositoryImpl<T, ID extends Serializable> implements LockingAndVersioningRepository<T, ID> {

    private EntityManager em;
    private EntityInformationFacade entityInfo;
    private EntityInformation<T, ?> entityInformation;
    private AuthenticationFacade auth;
    private LockingService lockingService;
    private VersioningService versioner;
    private CloningService cloner;

    @Autowired(required=false)
    public LockingAndVersioningRepositoryImpl() {
    }

    @Autowired(required=false)
    public LockingAndVersioningRepositoryImpl(EntityManager em, EntityInformationFacade entityInfo, AuthenticationFacade auth, LockingService locker, VersioningService versioner, CloningService cloner) {
        this.em = em;
        this.entityInfo = entityInfo;
        this.auth = auth;
        this.lockingService = locker;
        this.versioner = versioner;
        this.cloner = cloner;
    }

    @Override
    @Transactional
    public <S extends T> S lock(S entity) {
        Authentication authentication = auth.getAuthentication();
        if (!authentication.isAuthenticated()) {
            throw new SecurityException("no principal");
        }
        Object id = BeanUtils.getFieldWithAnnotation(entity, Id.class);
        if (id == null) {
            id = BeanUtils.getFieldWithAnnotation(entity, org.springframework.data.annotation.Id.class);
        }
        if (id == null) {
            return null;
        }
        if (lockingService.lock(id, authentication)) {
            BeanUtils.setFieldWithAnnotation(entity, LockOwner.class, authentication.getName());
            return this.save(entity);
        }
        return null;
    }

    @Override
    @Transactional
    public <S extends T> S unlock(S entity) {
        Authentication authentication = auth.getAuthentication();
        if (!authentication.isAuthenticated()) {
            throw new SecurityException("no principal");
        }
        Object id = BeanUtils.getFieldWithAnnotation(entity, Id.class);
        if (id == null) {
            id = BeanUtils.getFieldWithAnnotation(entity, org.springframework.data.annotation.Id.class);
        }
        if (id == null) {
            return null;
        }
        if (!lockingService.isLockOwner(id, authentication)) {
            throw new LockOwnerException("not lock owner");
        }

        BeanUtils.setFieldWithAnnotation(entity, LockOwner.class, null);
        entity = this.save(entity);

        if (lockingService.unlock(id, authentication)) {
            return entity;
        }
        return null;
    }

    @Transactional
    public <S extends T> S save(S entity) {
        if (entityInformation == null) {
            this.entityInformation = this.entityInfo.getEntityInformation(entity.getClass(), em);
        }

        if (entityInformation.isNew(entity)) {
            BeanUtils.setFieldWithAnnotation(entity, VersionNumber.class, "1.0");

            em.persist(entity);
            return entity;
        }

        Authentication authentication = auth.getAuthentication();
        if (!authentication.isAuthenticated()) {
            throw new SecurityException("no principal");
        }
        Object id = getId(entity);
        if (id == null) return null;

        Principal lockOwner;
        if ((lockOwner = lockingService.lockOwner(id)) == null || authentication.getName().equals(lockOwner.getName())) {
            return em.merge(entity);
        } else {
            throw new LockOwnerException(format("entity not locked by you"));
        }
    }

    @Transactional
    public <S extends T> S version(S currentVersion, VersionInfo info) {
        Object id = getId(currentVersion);
        if (id == null) return null;

        if (!isHead(currentVersion)) {
            throw new LockingAndVersioningException("not head");
        }

        Authentication authentication = auth.getAuthentication();
        Principal lockOwner;
        if ((lockOwner = lockingService.lockOwner(id)) == null || !authentication.isAuthenticated() || authentication.getName().equals(lockOwner.getName()) == false) {
            throw new LockOwnerException(format("not lock owner"));
        }

        S ancestorRoot;
        if (isAnestralRoot(currentVersion)) {
            currentVersion = (S)versioner.establishAncestralRoot(currentVersion);
            ancestorRoot = currentVersion;
        } else {
            Object ancestorRootId = getAncestralRootId(currentVersion);
            ancestorRoot = em.find((Class<S>) currentVersion.getClass(), ancestorRootId);
            if (ancestorRoot == null) {
                throw new LockingAndVersioningException(format("ancestor root not found: %s", ancestorRootId));
            }
        }

        S newVersion = (S)cloner.clone(currentVersion);
        newVersion = (S)versioner.establishSuccessor(newVersion, info.getNumber(), info.getLabel(), ancestorRoot, currentVersion);
        em.persist(newVersion);
        this.lock(newVersion);

        currentVersion = (S)versioner.establishAncestor(currentVersion, newVersion);
        this.unlock(currentVersion);

        return newVersion;
    }

    @Override
    public <S extends T> List<S> findAllLatestVersion() {
        return null;
    }

    @Override
    public <S extends T> List<S> findAllVersions(S entity) {
        return null;
    }

    @Override
    public <S extends T> void delete(S entity) {
        Object id = this.getId(entity);
        if (id == null) return;

        if (!isHead(entity)) {
            throw new LockingAndVersioningException("not head");
        }

        Authentication authentication = auth.getAuthentication();

        boolean relock = false;
        if (lockingService.lockOwner(id) != null && !lockingService.isLockOwner(id, authentication)) {
            throw new LockOwnerException("not lock owner");
        } else if (lockingService.lockOwner(id) != null && lockingService.isLockOwner(id, authentication)) {
            relock = true;
            lockingService.unlock(id, authentication);
        }

        Object ancestorRootId = getAncestralRootId(entity);
        Object ancestorId = getAncestorId(entity);

        S ancestor = null;
        if (ancestorId == null) {
            ancestorId = ancestorRootId;
        }

        if (ancestorId != null) {
            ancestor = (S) em.find(entity.getClass(), ancestorId);
            BeanUtils.setFieldWithAnnotation(ancestor, SuccessorId.class, null);
            lockingService.lock(ancestorId, authentication);
        }

        em.remove(entity);
    }

    protected <S extends T> boolean isHead(S entity) {
        boolean isHead = false;
        if (BeanUtils.hasFieldWithAnnotation(entity, SuccessorId.class)) {
            return BeanUtils.getFieldWithAnnotation(entity, SuccessorId.class) == null;
        }
        return isHead;
    }

    protected <S extends T> boolean isAnestralRoot(S entity) {
        boolean isAncestralRoot = false;
        if (BeanUtils.hasFieldWithAnnotation(entity, AncestorRootId.class)) {
            return BeanUtils.getFieldWithAnnotation(entity, AncestorRootId.class) == null;
        }
        return isAncestralRoot;
    }

    protected <S extends T> Object getAncestralRootId(S entity) {
        return BeanUtils.getFieldWithAnnotation(entity, AncestorRootId.class);
    }

    protected <S extends T> Object getAncestorId(S entity) {
        return BeanUtils.getFieldWithAnnotation(entity, AncestorId.class);
    }

    protected <S extends T> Object getId(S entity) {
        Object id = BeanUtils.getFieldWithAnnotation(entity, Id.class);
        if (id == null) {
            id = BeanUtils.getFieldWithAnnotation(entity, org.springframework.data.annotation.Id.class);
        }
        if (id == null) {
            return null;
        }
        return id;
    }
}
