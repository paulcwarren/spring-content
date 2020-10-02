package org.springframework.versions.impl;

import static java.lang.String.format;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.text.StringSubstitutor;
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

import internal.org.springframework.versions.AuthenticationFacade;
import internal.org.springframework.versions.LockingService;
import internal.org.springframework.versions.jpa.CloningService;
import internal.org.springframework.versions.jpa.EntityInformationFacade;
import internal.org.springframework.versions.jpa.JpaCloningServiceImpl;
import internal.org.springframework.versions.jpa.VersioningService;

public class LockingAndVersioningRepositoryImpl<T, ID extends Serializable> implements LockingAndVersioningRepository<T, ID> {

    private static Log logger = LogFactory.getLog(JpaCloningServiceImpl.class);

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
        if (authentication == null || !authentication.isAuthenticated()) {
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
        if (lockOwner != null && !principal.equals(lockOwner.getName())) {
            throw new LockOwnerException(format("not lock owner: %s has lock owner '%s'", id, (lockOwner != null) ? lockOwner.getName() : ""));
        }

        if (lockingService.lock(id, authentication)) {
            BeanUtils.setFieldWithAnnotation(entity, LockOwner.class, authentication.getName());
            return this.save(entity);
        }

        throw new LockingAndVersioningException(format("failed to lock %s", id));
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
            throw new LockOwnerException(format("not lock owner: %s has lock owner '%s'", id, (lockOwner != null) ? lockOwner.getName() : ""));
        }

        BeanUtils.setFieldWithAnnotation(entity, LockOwner.class, null);
        entity = this.save(entity);

        if (lockingService.unlock(id, authentication)) {
            return entity;
        }
        throw new LockingAndVersioningException(format("failed to unlock %s", id));
    }

    @Override
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

    @Override
    @Transactional
    public <S extends T> S workingCopy(S currentVersion) {

        Authentication authentication = auth.getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("no principal");
        }

        Object id = getId(currentVersion);
        if (id == null) {
            throw new IllegalStateException("not saved");
        }

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

        Object versionNumber = BeanUtils.getFieldWithAnnotation(currentVersion, VersionNumber.class);
        if (versionNumber == null) {
            versionNumber = "";
        }
        newVersion = (S)versioner.establishSuccessor(newVersion, versionNumber.toString(), "~~PWC~~", ancestorRoot, currentVersion);

        em.persist(newVersion);
        Object newId = getId(newVersion);

        newVersion = this.lock(newVersion);
      newVersion = em.merge(newVersion);

        return newVersion;
    }

    @Override
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

        Class<S> clz = (Class<S>) this.entityInformation.getJavaType();
        if (this.entityInformation == null) {
            logger.warn("Unknown entity context.  Try findAllVersionsLatest(Class<S> entityClass)");
            return new ArrayList<>();
        }

        String sql = "select t from ${entityClass} t where t.${successorId} = null and t.${id} NOT IN (select f1.${id} FROM ${entityClass} f1 inner join ${entityClass} f2 on f1.${ancestorId} = f2.${id} and f2.${successorId} = null)";

        StringSubstitutor sub = new StringSubstitutor(getAttributeMap(clz));
        sql = sub.replace(sql);

        TypedQuery<S> q = em.createQuery(sql, clz);

        try {
            return q.getResultList();
        } catch (NoResultException nre) {
            return new ArrayList<>();
        }
    }

    @Override
    public <S extends T> List<S> findAllVersionsLatest(Class<S> entityClass) {

        String sql = "select t from ${entityClass} t where t.${successorId} = null and t.${id} NOT IN (select f1.${id} FROM ${entityClass} f1 inner join ${entityClass} f2 on f1.${ancestorId} = f2.${id} and f2.${successorId} = null)";

        StringSubstitutor sub = new StringSubstitutor(getAttributeMap(entityClass));
        sql = sub.replace(sql);

        TypedQuery<S> q = em.createQuery(sql, entityClass);

        try {
            return q.getResultList();
        } catch (NoResultException nre) {
            return new ArrayList();
        }
    }

    @Override
    public <S extends T> List<S> findAllVersions(S entity) {

        String sql = "select t from ${entityClass} t where t.${ancestorRootId} = " + getAncestralRootId(entity);

        StringSubstitutor sub = new StringSubstitutor(getAttributeMap(entity.getClass()));
        sql = sub.replace(sql);

        TypedQuery<S> q = em.createQuery(sql, (Class<S>)entity.getClass());

        try {
            return q.getResultList();
        } catch (NoResultException nre) {
            return new ArrayList();
        }
    }

    @Override
    public void delete(T entity) {

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
        }

        Object ancestorRootId = getAncestralRootId(entity);
        Object ancestorId = getAncestorId(entity);

        T ancestor = null;
        if (ancestorId == null) {
            ancestorId = ancestorRootId;
        }

        if (ancestorId != null) {

            ancestor = (T) em.find(entity.getClass(), ancestorId);

            Principal lockOwner = null;
            if ((lockOwner = lockingService.lockOwner(ancestorId)) != null && !Objects.equals(authentication.getName(),lockOwner.getName())) {
                throw new LockOwnerException(format("Not lock owner %s", ancestorId));
            } else if (lockOwner == null && relock) {
                lockingService.lock(ancestorId, authentication);
            }

            BeanUtils.setFieldWithAnnotation(ancestor, SuccessorId.class, null);

            if (ancestorRootId.equals(ancestorId)) {
                BeanUtils.setFieldWithAnnotation(ancestor, AncestorRootId.class, null);
            }
        }

        lockingService.unlock(id, authentication);

        em.remove(em.contains(entity) ? entity : em.merge(entity));
    }

    protected <T> boolean isHead(T entity) {
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

    @Override
    public <S extends T> boolean isPrivateWorkingCopy(S entity) {

        String sql = "select count(f1.${id}) FROM ${entityClass} f1 inner join ${entityClass} f2 on f1.${ancestorId} = f2.${id} and f2.${successorId} IS NULL where f1.${id} = :id";

        Map<String,String> attributes = getAttributeMap(entity.getClass());
        StringSubstitutor sub = new StringSubstitutor(attributes);
        sql = sub.replace(sql);

        TypedQuery<Long> q = em.createQuery(sql, Long.class);
        q.setParameter("id", BeanUtils.getFieldWithAnnotation(entity, Id.class));

        return (q.getSingleResult() == 1L);
    }

    @Override
    public <S extends T> S findWorkingCopy(S entity) {

        String sql = "select f1 FROM ${entityClass} f1 inner join ${entityClass} f2 on f1.${ancestorId} = f2.${id} and f2.${successorId} IS NULL where f1.${ancestorRootId} = :id";

        Map<String,String> attributes = getAttributeMap(entity.getClass());
        StringSubstitutor sub = new StringSubstitutor(attributes);
        sql = sub.replace(sql);

        TypedQuery<S> q = em.createQuery(sql, (Class<S>)entity.getClass());

        q.setParameter("id", getAncestralRootId(entity));

        try {
            return q.getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }

    private Map<String,String> getAttributeMap(Class<?> entityClass) {
        Map<String,String> attributes = new HashMap<>();
        attributes.put("id", idAttribute(entityClass));
        attributes.put("ancestorId", ancestorIdAttribute(entityClass));
        attributes.put("ancestorRootId", ancestorRootIdAttribute(entityClass));
        attributes.put("successorId", successorIdAttribute(entityClass));
        attributes.put("entityClass", entityClass.getName());
        return attributes;
    }

    private <S extends T> String idAttribute(Class<?> entityClass) {
        Field idField = BeanUtils.findFieldWithAnnotation(entityClass, Id.class);
        if (idField == null) {
            idField = BeanUtils.findFieldWithAnnotation(entityClass, org.springframework.data.annotation.Id.class);
        }
        if (idField == null) {
            throw new IllegalStateException(format("Entity class is missing @Id field: %s", entityClass.getCanonicalName()));
        }
        return idField.getName();
    }

    private String successorIdAttribute(Class<?> entityClass) {
        Field successorIdField = BeanUtils.findFieldWithAnnotation(entityClass, SuccessorId.class);
        if (successorIdField == null) {
            throw new IllegalStateException(format("Entity class is missing @SuccessorId field: %s", entityClass.getCanonicalName()));
        }
        return successorIdField.getName();
    }

    private String ancestorIdAttribute(Class<?> entityClass) {
        Field ancestorIdField = BeanUtils.findFieldWithAnnotation(entityClass, AncestorId.class);
        if (ancestorIdField == null) {
            throw new IllegalStateException(format("Entity class is missing @AncestorId field: %s", entityClass.getCanonicalName()));
        }
        return ancestorIdField.getName();
    }

    private String ancestorRootIdAttribute(Class<?> entityClass) {
        Field ancestorRootIdField = BeanUtils.findFieldWithAnnotation(entityClass, AncestorRootId.class);
        if (ancestorRootIdField == null) {
            throw new IllegalStateException(format("Entity class is missing @AncestorRootId field: %s", entityClass.getCanonicalName()));
        }
        return ancestorRootIdField.getName();
    }

    protected <T> Object getAncestralRootId(T entity) {
        return BeanUtils.getFieldWithAnnotation(entity, AncestorRootId.class);
    }

    protected <T> Object getAncestorId(T entity) {
        return BeanUtils.getFieldWithAnnotation(entity, AncestorId.class);
    }

    protected <T> Object getId(T entity) {
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
