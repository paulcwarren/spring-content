package org.springframework.versions;

import internal.org.springframework.versions.LockingService;
import internal.org.springframework.versions.AuthenticationFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.Version;
import java.io.Serializable;
import java.security.Principal;
import java.util.List;

import static java.lang.String.format;

public class LockingAndVersioningRepositoryImpl<T, ID extends Serializable> implements LockingAndVersioningRepository<T, ID> {

    private EntityManager em;
    private JpaEntityInformation<T, ?>  entityInformation;
    private AuthenticationFacade auth;
    private LockingService lockingService;

    @Autowired(required=false)
    public LockingAndVersioningRepositoryImpl() {
    }

    @Autowired(required=false)
    public LockingAndVersioningRepositoryImpl(EntityManager em, AuthenticationFacade auth, LockingService versioning) {
        this.em = em;
        this.auth = auth;
        this.lockingService = versioning;
    }

//    public LockingAndVersioningRepositoryImpl(EntityManager em, AuthenticationFacade auth, LockingService versioning, JpaEntityInformation<T, ?>  entityInformation) {
//        this(em, auth, versioning);
//        this.entityInformation = entityInformation;
//    }

    @Override
    @Transactional
    public <S extends T> S lock(S entity) {
        Authentication authentication = auth.getAuthentication();
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
            this.entityInformation = JpaEntityInformationSupport.getEntityInformation((Class<T>) entity.getClass(), em);
        }

        if (entityInformation.isNew(entity)) {
            BeanUtils.setFieldWithAnnotation(entity, VersionNumber.class, "1.0");

            em.persist(entity);
            return entity;
        }

        Authentication authentication = auth.getAuthentication();
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
    public <S extends T> S version(S entity, VersionInfo info) {
        Authentication authentication = auth.getAuthentication();
        Object id = getId(entity);
        if (id == null) return null;

        Principal lockOwner;
        if ((lockOwner = lockingService.lockOwner(id)) != null && authentication.getName().equals(lockOwner.getName()) == false) {
            throw new LockOwnerException(format("not lock owner"));
        }

        S old = em.find((Class<S>) entity.getClass(), id);
        if (isAnestralRoot(old)) {
           old = updateVersionAttributes(old, null, id, false);
        } else {
            if (BeanUtils.hasFieldWithAnnotation(old, VersionStatus.class)) {
                BeanUtils.setFieldWithAnnotation(old, VersionStatus.class, false);
            }
        }
        em.merge(old);
        this.unlock(old);
        em.flush();

        entity = clone(entity);
        BeanUtils.setFieldWithAnnotation(entity, Id.class, null);
        BeanUtils.setFieldWithAnnotation(entity, Version.class, 0);
        BeanUtils.setFieldWithAnnotation(entity, VersionNumber.class, info.getNumber());
        BeanUtils.setFieldWithAnnotation(entity, VersionLabel.class, info.getLabel());
        entity = updateVersionAttributes(entity, id, getAncestralRootId(old), true);
        em.persist(entity);
        entity = this.lock(entity);

        return entity;
    }

    protected <S extends T> boolean isAnestralRoot(S old) {
        boolean isAncestralRoot = false;
        if (BeanUtils.hasFieldWithAnnotation(old, AncestorRootId.class)) {
            return BeanUtils.getFieldWithAnnotation(old, AncestorRootId.class) == null;
        }
        return isAncestralRoot;
    }

    protected <S extends T> Object getAncestralRootId(S existing) {
        return BeanUtils.getFieldWithAnnotation(existing, AncestorRootId.class);
    }

    protected <S extends T> S clone(S entity) {
        em.detach(entity);
        return entity;
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

    protected <S extends T> S updateVersionAttributes(S entity, Object ancestorId, Object ancestralRootId, boolean versionStatus) {

        if (BeanUtils.hasFieldWithAnnotation(entity, AncestorId.class)) {
            BeanUtils.setFieldWithAnnotation(entity, AncestorId.class, ancestorId);
        }

        if (BeanUtils.hasFieldWithAnnotation(entity, AncestorRootId.class) && BeanUtils.getFieldWithAnnotation(entity, AncestorRootId.class) == null) {
            BeanUtils.setFieldWithAnnotation(entity, AncestorRootId.class, ancestralRootId);
        }

        if (BeanUtils.hasFieldWithAnnotation(entity, VersionStatus.class)) {
            BeanUtils.setFieldWithAnnotation(entity, VersionStatus.class, versionStatus);
        }

        return entity;
    }

    @Override
    public <S extends T> List<S> findAllLatestVersion() {
        return null;
    }

    @Override
    public <S extends T> List<S> findAllVersions(S entity) {
        return null;
    }
}
