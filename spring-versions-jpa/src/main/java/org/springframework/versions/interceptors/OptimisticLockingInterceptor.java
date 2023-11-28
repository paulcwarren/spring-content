package org.springframework.versions.interceptors;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Version;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

public class OptimisticLockingInterceptor implements MethodInterceptor {

    private static Method getContentMethod;
    private static Method getContentPropertyPathMethod;
    private static Method setContentMethod;
    private static Method setContentPropertyPathMethod;
    private static Method setContentMethodWithResource;
    private static Method setContentMethodWithPropertyPathAndResource;
    private static Method unsetContentMethod;
    private static Method unsetContentPropertyPathMethod;

    static {
        getContentMethod = ReflectionUtils.findMethod(ContentStore.class, "getContent", Object.class);
        Assert.notNull(getContentMethod, "Unable to find getContent method");
        getContentPropertyPathMethod = ReflectionUtils.findMethod(ContentStore.class, "getContent", Object.class, PropertyPath.class);
        Assert.notNull(getContentPropertyPathMethod, "Unable to find getContent method");
        setContentMethod = ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, InputStream.class);
        Assert.notNull(setContentMethod, "Unable to find setContent method");
        setContentPropertyPathMethod = ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, PropertyPath.class, InputStream.class);
        Assert.notNull(setContentPropertyPathMethod, "Unable to find setContent method");
        setContentMethodWithResource = ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, Resource.class);
        Assert.notNull(setContentMethodWithResource, "Unable to find setContent method");
        setContentMethodWithPropertyPathAndResource = ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, PropertyPath.class, Resource.class);
        Assert.notNull(setContentMethodWithPropertyPathAndResource, "Unable to find setContent method");
        unsetContentMethod = ReflectionUtils.findMethod(ContentStore.class,"unsetContent", Object.class);
        Assert.notNull(unsetContentMethod, "Unable to find unsetContent method");
        unsetContentPropertyPathMethod = ReflectionUtils.findMethod(ContentStore.class,"unsetContent", Object.class, PropertyPath.class);
        Assert.notNull(unsetContentPropertyPathMethod, "Unable to find unsetContent method");
    }

    private final EntityManager em;

    public OptimisticLockingInterceptor(EntityManager em) {
        this.em = em;
    }

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        Object rc = null;

        if (getContentMethod.equals(methodInvocation.getMethod()) ||
            getContentPropertyPathMethod.equals(methodInvocation.getMethod())) {
            if (methodInvocation.getArguments().length > 0) {
                Object[] args = methodInvocation.getArguments();
                args[0] = lock(args[0]);
                ((ProxyMethodInvocation)methodInvocation).setArguments(args);
                rc = methodInvocation.proceed();
            }
        }
        else if (setContentMethod.equals(methodInvocation.getMethod()) ||
                 setContentPropertyPathMethod.equals(methodInvocation.getMethod()) ||
                 setContentMethodWithResource.equals(methodInvocation.getMethod()) ||
                 setContentMethodWithPropertyPathAndResource.equals(methodInvocation.getMethod()) ||
                 unsetContentMethod.equals(methodInvocation.getMethod()) ||
                 unsetContentPropertyPathMethod.equals(methodInvocation.getMethod())) {
            if (methodInvocation.getArguments().length > 0) {
                Object[] args = methodInvocation.getArguments();
                args[0] = lock(args[0]);
                ((ProxyMethodInvocation)methodInvocation).setArguments(args);
                Object entity = methodInvocation.proceed();
                touch(entity, Version.class);
                return entity;
            }
        } else {
            rc = methodInvocation.proceed();
        }
        return rc;
    }

    protected Object lock(Object entity) {
        if (em == null) {
            return entity;
        }

        if (BeanUtils.hasFieldWithAnnotation(entity, Version.class) == false) {
            return entity;
        }

        entity = em.merge(entity);
        em.lock(entity, LockModeType.OPTIMISTIC);
        return entity;
    }

    private void touch(Object domainObj, Class<? extends Annotation> annotation) {
        Field f = BeanUtils.findFieldWithAnnotation(domainObj, annotation);
        if (f == null)
            return;
        Object version = BeanUtils.getFieldWithAnnotation(domainObj, annotation);
        if (f.getType().isAssignableFrom(Integer.class)) {
            version = Math.incrementExact((Integer)version);
        } else if (f.getType().isAssignableFrom(Long.class)) {
            version = Math.incrementExact((Long)version);
        }
        BeanUtils.setFieldWithAnnotation(domainObj, annotation, version);
    }
}
