package org.springframework.versions.interceptors;

import internal.org.springframework.versions.AuthenticationFacade;
import internal.org.springframework.versions.LockingService;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.util.Assert;
import org.springframework.versions.LockOwnerException;
import org.springframework.versions.LockParticipant;

import javax.persistence.Id;
import java.lang.reflect.Field;

import static java.lang.String.format;

public class PessimisticLockingInterceptor implements MethodInterceptor {

    private LockingService versions;
    private AuthenticationFacade auth;

    private static final ReflectionUtils.AnnotationFieldFilter ID_FILTER = new ReflectionUtils.AnnotationFieldFilter(Id.class);
    private static final ReflectionUtils.AnnotationFieldFilter DATA_ID_FILTER = new ReflectionUtils.AnnotationFieldFilter(org.springframework.data.annotation.Id.class);

    public PessimisticLockingInterceptor(LockingService versions, AuthenticationFacade auth) {
        Assert.notNull(versions, "versions cannot be null");
        Assert.notNull(auth, "auth cannot be null");
        this.versions = versions;
        this.auth = auth;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {

        LockParticipant participant = invocation.getMethod().getAnnotation(LockParticipant.class);
        if (participant != null) {
            return invokeWithIntecept(invocation);
        }

        return invocation.proceed();
    }

    private Object invokeWithIntecept(MethodInvocation invocation) throws Throwable {
        Object entity = invocation.getArguments()[0];
        Field idField = ReflectionUtils.findField(entity.getClass(), ID_FILTER);
        if (idField == null) {
            idField = ReflectionUtils.findField(entity.getClass(), DATA_ID_FILTER);
        }
        if (idField == null) {
            throw new IllegalArgumentException(format("ID field missing: %s", entity.getClass().getCanonicalName()));
        }

        org.springframework.util.ReflectionUtils.makeAccessible(idField);
        Object id = org.springframework.util.ReflectionUtils.getField(idField, entity);

        if (versions.lockOwner(id) == null || versions.isLockOwner(id, auth.getAuthentication())) {
            return invocation.proceed();
        } else {
            throw new LockOwnerException("Not lock owner");
        }
    }
}
