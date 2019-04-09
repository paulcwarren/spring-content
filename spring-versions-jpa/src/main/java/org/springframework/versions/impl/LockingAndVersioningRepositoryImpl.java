package org.springframework.versions.impl;

import java.io.Serializable;
import java.security.Principal;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

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
import org.springframework.versions.VersionLabel;
import org.springframework.versions.VersionNumber;

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
            throw new IllegalStateException("@Id missing");
        }
        if (lockingService.lock(id, authentication)) {
            BeanUtils.setFieldWithAnnotation(entity, LockOwner.class, authentication.getName());
            return this.save(entity);
        }
        throw new IllegalStateException(format("failed to lock %s", id));
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
            throw new IllegalStateException("@Id missing");
        }
        String principal = authentication.getName();
        Principal lockOwner = lockingService.lockOwner(id);
        if (lockOwner == null || !principal.equals(lockOwner.getName())) {
            throw new LockOwnerException(format("not lock owner: %s has lock owner %s", id, (lockOwner != null) ? lockOwner.getName() : ""));
        }

        BeanUtils.setFieldWithAnnotation(entity, LockOwner.class, null);
        entity = this.save(entity);

        if (lockingService.unlock(id, authentication)) {
            return entity;
        }
        throw new IllegalStateException(format("failed to unlock %s", id));
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

        Object id = getId(entity);
        if (id == null) return null;

        Authentication authentication = auth.getAuthentication();
        Principal lockOwner = lockingService.lockOwner(id);
        if ((authentication == null || (authentication.isAuthenticated() == false) && lockOwner == null)) {
            return em.merge(entity);
        } else if (authentication != null && authentication.isAuthenticated() && (lockOwner == null || authentication.getName().equals(lockOwner.getName()))) {
            return em.merge(entity);
        } else {
            throw new LockOwnerException(format("entity not locked by you"));
        }
    }

    @Transactional
    public <S extends T> S createPrivateWorkingCopy(S currentVersion) {

        Authentication authentication = auth.getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("no principal");
        }

        Object id = getId(currentVersion);
        if (id == null) return null;

        if (!isHead(currentVersion)) {
            throw new LockingAndVersioningException("not head");
        }

        Principal lockOwner = lockingService.lockOwner(id);
        if (lockOwner  == null || !authentication.isAuthenticated() || authentication.getName().equals(lockOwner.getName()) == false) {
            throw new LockOwnerException(format("not lock owner"));
        }

        S ancestorRoot;
        if (isAnestralRoot(currentVersion)) {
            currentVersion = (S)versioner.establishAncestralRoot(currentVersion);
            em.merge(currentVersion);
            ancestorRoot = currentVersion;
        } else {
            Object ancestorRootId = getAncestralRootId(currentVersion);
            ancestorRoot = em.find((Class<S>) currentVersion.getClass(), ancestorRootId);
            if (ancestorRoot == null) {
                throw new LockingAndVersioningException(format("ancestor root not found: %s", ancestorRootId));
            }
        }

        S newVersion = (S)cloner.clone(currentVersion);

//        this.unlock(currentVersion);
//
        Object versionNumber = BeanUtils.getFieldWithAnnotation(currentVersion, VersionNumber.class);
        if (versionNumber == null) {
            versionNumber = "";
        }
        newVersion = (S)versioner.establishSuccessor(newVersion, versionNumber.toString(), "~~PWC~~", ancestorRoot, currentVersion);

        em.persist(newVersion);
        Object newId = getId(newVersion);

//        currentVersion = (S)versioner.establishAncestor(currentVersion, newVersion);

        newVersion = this.lock(newVersion);
		newVersion = em.merge(newVersion);

        return newVersion;
    }

    @SuppressWarnings("unchecked")
    @Transactional
    public <S extends T> S version(S currentVersion, VersionInfo info) {

    	Authentication authentication = auth.getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("no principal");
        }

        Object id = getId(currentVersion);
        if (id == null) return null;

        if (!isHead(currentVersion)) {
            throw new LockingAndVersioningException("not head");
        }

        Principal lockOwner = lockingService.lockOwner(id);
        if (lockOwner  == null || !authentication.isAuthenticated() || authentication.getName().equals(lockOwner.getName()) == false) {
            throw new LockOwnerException("not lock owner");
        }

        S newVersion = null;
        if (!isPrivateWorkingCopy(currentVersion)) {

            S ancestorRoot;
            if (isAnestralRoot(currentVersion)) {
                currentVersion = (S) versioner.establishAncestralRoot(currentVersion);
                ancestorRoot = currentVersion;
            }
            else {
                Object ancestorRootId = getAncestralRootId(currentVersion);
                ancestorRoot = em.find((Class<S>) currentVersion.getClass(), ancestorRootId);
                if (ancestorRoot == null) {
                    throw new LockingAndVersioningException(format("ancestor root not found: %s", ancestorRootId));
                }
            }

            newVersion = (S) cloner.clone(currentVersion);

            this.unlock(currentVersion);

            newVersion = (S) versioner
                    .establishSuccessor(newVersion, info.getNumber(), info.getLabel(), ancestorRoot, currentVersion);
            em.persist(newVersion);
            Object newId = getId(newVersion);

            newVersion = this.lock(newVersion);
			newVersion = em.merge(newVersion);
        } else {

            newVersion = currentVersion;
            BeanUtils.setFieldWithAnnotation(newVersion, VersionNumber.class, info.getNumber());
            BeanUtils.setFieldWithAnnotation(newVersion, VersionLabel.class, info.getLabel());
            newVersion = em.merge(newVersion);

            currentVersion = (S) em.find(newVersion.getClass(), BeanUtils.getFieldWithAnnotation(newVersion, AncestorId.class));
            this.unlock(currentVersion);
        }

        currentVersion = (S) versioner.establishAncestor(currentVersion, newVersion);
        em.merge(currentVersion);

        return newVersion;
    }

    @Override
    public <S extends T> List<S> findAllVersionsLatest() {
        return null;
    }

    @Override
    public <S extends T> List<S> findAllVersions(S entity) {
        return null;
    }

    @Override
    public <S extends T> void delete(S entity) {

    	Authentication authentication = auth.getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("no principal");
        }

        Object id = this.getId(entity);
        if (id == null) return;

        if (!isHead(entity)) {
            throw new LockingAndVersioningException("not head");
        }

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

            if (ancestorRootId.equals(ancestorId)) {
				BeanUtils.setFieldWithAnnotation(ancestor, AncestorRootId.class, null);
			}

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

    public <S extends T> boolean isPrivateWorkingCopy(S currentVersion) {

        TypedQuery<Long> q = em.createQuery(format("select count(f1.id) FROM %s f1 inner join %s f2 on f1.ancestorId = f2.id and f2.successorId = null where f1.id = %s",
                currentVersion.getClass().getName(),
                currentVersion.getClass().getName(),
                BeanUtils.getFieldWithAnnotation(currentVersion, Id.class)),
                Long.class);

        return (q.getSingleResult() == 1L);

    }

    public <S extends T> S findWorkingCopy(S entity) {
        TypedQuery<S> q = em.createQuery(format("select f1 FROM %s f1 inner join %s f2 on f1.ancestorId = f2.id and f2.successorId IS NULL where f1.ancestralRootId = %s",
                entity.getClass().getName(),
                entity.getClass().getName(),
                BeanUtils.getFieldWithAnnotation(entity, AncestorRootId.class)),
                (Class<S>)entity.getClass());

        try {
            return q.getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
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
