package org.springframework.versions.interceptors;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Version;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

public class OptimisticLockingInterceptor implements MethodInterceptor {

    private static Method getContentMethod;
    private static Method setContentMethod;
    private static Method setContentMethodWithResource;
    private static Method unsetContentMethod;

    static {
        getContentMethod = ReflectionUtils.findMethod(ContentStore.class, "getContent", Object.class);
        Assert.notNull(getContentMethod);
        setContentMethod = ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, InputStream.class);
        Assert.notNull(setContentMethod);
        setContentMethodWithResource = ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, Resource.class);
        Assert.notNull(setContentMethodWithResource);
        unsetContentMethod = ReflectionUtils.findMethod(ContentStore.class,"unsetContent", Object.class);
        Assert.notNull(unsetContentMethod);
    }

    private final EntityManager em;

    public OptimisticLockingInterceptor(EntityManager em) {
        this.em = em;
    }

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        Object rc = null;

        if (getContentMethod.equals(methodInvocation.getMethod())) {
            if (methodInvocation.getArguments().length > 0) {
                Object entity = methodInvocation.getArguments()[0];
                entity = lock(entity);
                ((ProxyMethodInvocation)methodInvocation).setArguments(entity);
                rc = methodInvocation.proceed();
            }
        }
        else if (setContentMethod.equals(methodInvocation.getMethod())) {
            if (methodInvocation.getArguments().length > 0) {
                Object entity = methodInvocation.getArguments()[0];
                entity = lock(entity);
                ((ProxyMethodInvocation)methodInvocation).setArguments(entity, methodInvocation.getArguments()[1]);
                entity = methodInvocation.proceed();
                touch(entity, Version.class);
                return entity;
            }
        }
        else if (setContentMethodWithResource.equals(methodInvocation.getMethod())) {
            if (methodInvocation.getArguments().length > 0) {
                Object entity = methodInvocation.getArguments()[0];
                entity = lock(entity);
                ((ProxyMethodInvocation)methodInvocation).setArguments(entity, methodInvocation.getArguments()[1]);
                entity = methodInvocation.proceed();
                touch(entity, Version.class);
                return entity;
            }
        }
        else if (unsetContentMethod.equals(methodInvocation.getMethod())) {
            if (methodInvocation.getArguments().length > 0) {
                Object entity = methodInvocation.getArguments()[0];
                entity = lock(entity);
                ((ProxyMethodInvocation)methodInvocation).setArguments(entity);
                entity = methodInvocation.proceed();
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
