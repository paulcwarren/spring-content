package org.springframework.versions.impl;

import static java.lang.String.format;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;
import org.springframework.versions.AncestorId;
import org.springframework.versions.AncestorRootId;
import org.springframework.versions.AutoVersioningRepository;
import org.springframework.versions.LockingAndVersioningException;
import org.springframework.versions.SubjectId;
import org.springframework.versions.SuccessorId;
import org.springframework.versions.jpa.config.AutoVersion;

import internal.org.springframework.versions.AuthenticationFacade;
import internal.org.springframework.versions.jpa.CloningService;
import internal.org.springframework.versions.jpa.EntityInformationFacade;
import internal.org.springframework.versions.jpa.JpaCloningServiceImpl;
import internal.org.springframework.versions.jpa.VersioningService;

public class AutoVersioningRepositoryImpl<T, ID extends Serializable, V> implements AutoVersioningRepository<T, ID, V> {

    private static Log logger = LogFactory.getLog(JpaCloningServiceImpl.class);

    private EntityManager em;
    private EntityInformationFacade entityInfo;
    private EntityInformation<T, ?> entityInformation;
    private AuthenticationFacade auth;
    private VersioningService versioner;
    private CloningService cloner;

    @Autowired(required=false)
    public AutoVersioningRepositoryImpl() {
    }

    @Autowired(required=false)
    public AutoVersioningRepositoryImpl(EntityManager em, EntityInformationFacade entityInfo, AuthenticationFacade auth, VersioningService versioner, CloningService cloner) {
        this.em = em;
        this.entityInfo = entityInfo;
        this.auth = auth;
        this.versioner = versioner;
        this.cloner = cloner;
    }

    @Override
    @Transactional
    public <S extends T> S save(S entity) {
        if (entityInformation == null) {
            this.entityInformation = this.entityInfo.getEntityInformation(entity.getClass(), em);
        }



        if (entityInformation.isNew(entity)) {

//            BeanUtils.setFieldWithAnnotation(entity, VersionNumber.class, "1.0");
            em.persist(entity);

            createVersion(entity);

            return entity;
        }

        Object id = getId(entity);
        if (id == null) return null;

        V currentVersion = findLatestVersion(entity);
        V newVersion = null;

        V ancestorRoot;
        if (isAnestralRoot(currentVersion)) {

            currentVersion = (V) versioner.establishAncestralRoot(currentVersion);
            ancestorRoot = currentVersion;
        } else {

            Object ancestorRootId = getAncestralRootId(currentVersion);
            ancestorRoot = em.find((Class<V>) currentVersion.getClass(), ancestorRootId);
            if (ancestorRoot == null) {
                throw new LockingAndVersioningException(format("ancestor root not found: %s", ancestorRootId));
            }
        }

        newVersion = createVersion(entity);

        newVersion = (V) versioner.establishSuccessor(newVersion, null, null, ancestorRoot, currentVersion);
        em.persist(newVersion);

        currentVersion = (V) versioner.establishAncestor(currentVersion, newVersion);
        em.persist(currentVersion);

        //        Object newId = getId(newVersion);
//
//        newVersion = em.merge(newVersion);


//        Authentication authentication = auth.getAuthentication();
//        Principal lockOwner = lockingService.lockOwner(id);
//        if ((authentication == null || (authentication.isAuthenticated() == false) && lockOwner == null)) {
            return em.merge(entity);
//        } else if (authentication != null && authentication.isAuthenticated() && (lockOwner == null || authentication.getName().equals(lockOwner.getName()))) {
//            return em.merge(entity);
//        } else {
//            throw new LockOwnerException(format("entity not locked by you"));
//        }
    }

    private <S extends T> V createVersion(S entity) {

        Class<?> versionType = versionType(entity);

        V versionInstance = null;
        try {
            versionInstance = createVersionInstance(versionType, entity);
            BeanWrapper wrapper = new BeanWrapperImpl(versionInstance);
            wrapper.setPropertyValue(subjectIdAttribute(versionType), getId(entity));
            em.persist(versionInstance);
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return versionInstance;
    }

    private V createVersionInstance(Class<?> versionType, Object subject)
            throws InstantiationException,
            IllegalAccessException {

        V versionInstance = null;

        try {
            Constructor<?> ctor = ReflectionUtils.accessibleConstructor(versionType, subject.getClass());
            if (ctor == null) {
                versionInstance = (V) versionType.newInstance();
            }
            versionInstance = (V) ctor.newInstance(subject);

        } catch (NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException e) {
            int i=0;
        }

        return versionInstance;
    }

    private <S extends T> Class<?> versionType(S entity)
            throws IllegalArgumentException {
        AutoVersion autoVersion = entity.getClass().getAnnotation(AutoVersion.class);
        if (autoVersion == null) {
            throw new IllegalArgumentException(format("Missing @AutoVersion on entity type %s", entity.getClass().getCanonicalName()));
        }

        Class<?> versionType = autoVersion.versionType();
        if (versionType == null) {
            throw new IllegalArgumentException(format("Missing @AutoVersion.versionType on entity type %s", entity.getClass().getCanonicalName()));
        }
        return versionType;
    }

    @Override
    public <S extends T> List<V> findAllVersions(S entity) {

        Class<?> versionType = versionType(entity);

        String sql = "select t from ${entityClass} t where t.${subjectId} = " + getId(entity);

        StringSubstitutor sub = new StringSubstitutor(getAttributeMap(versionType));
        sql = sub.replace(sql);

        TypedQuery<V> q = em.createQuery(sql, (Class<V>)versionType);

        try {
            return q.getResultList();
        } catch (NoResultException nre) {
            return new ArrayList<V>();
        }
    }

    public <S extends T> V findLatestVersion(S entity) {

        Class<?> versionType = versionType(entity);

        String sql = "select t from ${entityClass} t where t.${subjectId} = " + getId(entity) + " order by t.${id} desc";

        StringSubstitutor sub = new StringSubstitutor(getAttributeMap(versionType));
        sql = sub.replace(sql);

        TypedQuery<V> q = em.createQuery(sql, (Class<V>)versionType);

        try {
            return q.getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }

//    @Override
//    public void delete(T entity) {
//
//      Authentication authentication = auth.getAuthentication();
//        if (authentication == null || !authentication.isAuthenticated()) {
//            throw new SecurityException("no principal");
//        }
//
//        Object id = this.getId(entity);
//        if (id == null) return;
//
//        if (!isHead(entity)) {
//            throw new LockingAndVersioningException("not head");
//        }
//
//        boolean relock = false;
//        if (lockingService.lockOwner(id) != null && !lockingService.isLockOwner(id, authentication)) {
//            throw new LockOwnerException("not lock owner");
//        } else if (lockingService.lockOwner(id) != null && lockingService.isLockOwner(id, authentication)) {
//            relock = true;
//        }
//
//        Object ancestorRootId = getAncestralRootId(entity);
//        Object ancestorId = getAncestorId(entity);
//
//        T ancestor = null;
//        if (ancestorId == null) {
//            ancestorId = ancestorRootId;
//        }
//
//        if (ancestorId != null) {
//
//            ancestor = (T) em.find(entity.getClass(), ancestorId);
//
//            Principal lockOwner = null;
//            if ((lockOwner = lockingService.lockOwner(ancestorId)) != null && !Objects.equals(authentication.getName(),lockOwner.getName())) {
//                throw new LockOwnerException(format("Not lock owner %s", ancestorId));
//            } else if (lockOwner == null && relock) {
//                lockingService.lock(ancestorId, authentication);
//            }
//
//            BeanUtils.setFieldWithAnnotation(ancestor, SuccessorId.class, null);
//
//            if (ancestorRootId.equals(ancestorId)) {
//                BeanUtils.setFieldWithAnnotation(ancestor, AncestorRootId.class, null);
//            }
//        }
//
//        lockingService.unlock(id, authentication);
//
//        em.remove(em.contains(entity) ? entity : em.merge(entity));
//    }

    protected <T> boolean isHead(T entity) {
        boolean isHead = false;
        if (BeanUtils.hasFieldWithAnnotation(entity, SuccessorId.class)) {
            return BeanUtils.getFieldWithAnnotation(entity, SuccessorId.class) == null;
        }
        return isHead;
    }

    protected boolean isAnestralRoot(V entity) {
        boolean isAncestralRoot = false;
        if (BeanUtils.hasFieldWithAnnotation(entity, AncestorRootId.class)) {
            return BeanUtils.getFieldWithAnnotation(entity, AncestorRootId.class) == null;
        }
        return isAncestralRoot;
    }

    private Map<String,String> getAttributeMap(Class<?> entityClass) {
        Map<String,String> attributes = new HashMap<>();
        attributes.put("id", idAttribute(entityClass));
        attributes.put("subjectId", subjectIdAttribute(entityClass));
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

    private String subjectIdAttribute(Class<?> entityClass) {
        Field subjectIdField = BeanUtils.findFieldWithAnnotation(entityClass, SubjectId.class);
        if (subjectIdField == null) {
            throw new IllegalStateException(format("Entity class is missing @SubjectId field: %s", entityClass.getCanonicalName()));
        }
        return subjectIdField.getName();
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
