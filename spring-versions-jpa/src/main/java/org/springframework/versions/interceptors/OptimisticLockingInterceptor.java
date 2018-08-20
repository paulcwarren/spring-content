package org.springframework.versions.interceptors;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ReflectiveMethodInvocation;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.events.AfterGetContentEvent;
import org.springframework.content.commons.repository.events.AfterSetContentEvent;
import org.springframework.content.commons.repository.events.AfterUnsetContentEvent;
import org.springframework.content.commons.repository.events.BeforeGetContentEvent;
import org.springframework.content.commons.repository.events.BeforeSetContentEvent;
import org.springframework.content.commons.repository.events.BeforeUnsetContentEvent;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.versions.LockParticipant;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Version;
import java.io.InputStream;
import java.lang.reflect.Method;

public class OptimisticLockingInterceptor implements MethodInterceptor {

    private static Method getContentMethod;
    private static Method setContentMethod;
    private static Method unsetContentMethod;

    static {
        getContentMethod = ReflectionUtils.findMethod(ContentStore.class, "getContent", Object.class);
        Assert.notNull(getContentMethod);
        setContentMethod = ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, InputStream.class);
        Assert.notNull(setContentMethod);
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
                ((ReflectiveMethodInvocation)methodInvocation).setArguments(entity);
                rc = methodInvocation.proceed();
            }
        }
        else if (setContentMethod.equals(methodInvocation.getMethod())) {
            if (methodInvocation.getArguments().length > 0) {
                Object entity = methodInvocation.getArguments()[0];
                entity = lock(entity);
                ((ReflectiveMethodInvocation)methodInvocation).setArguments(entity, methodInvocation.getArguments()[1]);
                methodInvocation.proceed();
                touch(entity);
            }
        }
        else if (unsetContentMethod.equals(methodInvocation.getMethod())) {
            if (methodInvocation.getArguments().length > 0) {
                Object entity = methodInvocation.getArguments()[0];
                entity = lock(entity);
                ((ReflectiveMethodInvocation)methodInvocation).setArguments(entity);
                methodInvocation.proceed();
                touch(entity);
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

    private void touch(Object domainObj) {
        Object version = BeanUtils.getFieldWithAnnotation(domainObj, Version.class);
        if (version instanceof Integer) {
            version = Math.incrementExact((Integer)version);
        } else if (version instanceof Long) {
            version = Math.incrementExact((Long)version);
        }
        BeanUtils.setFieldWithAnnotation(domainObj, Version.class, version);
    }
}
