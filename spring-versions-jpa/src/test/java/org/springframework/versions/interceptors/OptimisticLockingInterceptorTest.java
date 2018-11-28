package org.springframework.versions.interceptors;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import lombok.Getter;
import lombok.Setter;
import org.junit.runner.RunWith;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.util.ReflectionUtils;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Version;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.FIt;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(Ginkgo4jRunner.class)
public class OptimisticLockingInterceptorTest {

    private OptimisticLockingInterceptor interceptor;

    private Object result;

    //mocks
    private EntityManager em;
    private ProxyMethodInvocation mi;
    private TestEntity entity;

    {
        Describe("OptimisticLockInterceptor", () -> {
            BeforeEach(() -> {
                em = mock(EntityManager.class);
            });
            JustBeforeEach(() -> {
                interceptor = new OptimisticLockingInterceptor(em);
            });
            Context("#invoke", () -> {
                BeforeEach(() -> {
                    mi = mock(ProxyMethodInvocation.class);
                });
                Context("when the method invocation is getContent", () -> {
                    BeforeEach(() -> {
                        entity = new TestEntity();
                        when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "getContent", Object.class));
                        when(mi.getArguments()).thenReturn(new Object[]{entity});
                        when(em.merge(entity)).thenReturn(entity);
                    });
                    JustBeforeEach(() -> {
                        result = interceptor.invoke(mi);
                    });
                    It("should lock the entity and proceed", () -> {
                        verify(em).lock(entity, LockModeType.OPTIMISTIC);
                        verify(mi).setArguments(entity);
                        verify(mi).proceed();
                    });
                });
                Context("when the method invocation is setContent", () -> {
                    BeforeEach(() -> {
                        entity = new TestEntity();
                        when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, InputStream.class));
                        when(mi.getArguments()).thenReturn(new Object[]{entity, new ByteArrayInputStream("".getBytes())});
                        when(em.merge(entity)).thenReturn(entity);
                    });
                    JustBeforeEach(() -> {
                        result = interceptor.invoke(mi);
                    });
                    It("should lock the entity and proceed", () -> {
                        verify(em).lock(entity, LockModeType.OPTIMISTIC);
                        verify(mi).setArguments(eq(entity), anyObject());
                        verify(mi).proceed();
                        assertThat(entity.getVersion(), is(1L));
                    });
                });
                Context("when the method invocation is unsetContent", () -> {
                    BeforeEach(() -> {
                        entity = new TestEntity();
                        when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class,"unsetContent", Object.class));
                        when(mi.getArguments()).thenReturn(new Object[]{entity});
                        when(em.merge(entity)).thenReturn(entity);
                    });
                    JustBeforeEach(() -> {
                        result = interceptor.invoke(mi);
                    });
                    It("should lock the entity and proceed", () -> {
                        verify(em).lock(entity, LockModeType.OPTIMISTIC);
                        verify(mi).setArguments(eq(entity));
                        verify(mi).proceed();
                        assertThat(entity.getVersion(), is(1L));
                    });
                });
            });
        });
    }

    @Getter
    @Setter
    private class TestEntity {
        @Version
        private Long version = 0L;
    }
}
