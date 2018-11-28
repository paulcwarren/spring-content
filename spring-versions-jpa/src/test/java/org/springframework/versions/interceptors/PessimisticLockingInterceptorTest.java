package org.springframework.versions.interceptors;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import internal.org.springframework.versions.AuthenticationFacade;
import internal.org.springframework.versions.LockingService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.junit.runner.RunWith;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.security.core.Authentication;
import org.springframework.util.ReflectionUtils;
import org.springframework.versions.LockOwnerException;

import javax.persistence.Id;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(Ginkgo4jRunner.class)
public class PessimisticLockingInterceptorTest {

    private PessimisticLockingInterceptor interceptor;

    // mocks
    private LockingService locker;
    private AuthenticationFacade auth;
    private ProxyMethodInvocation mi;

    private Object result;
    private Exception e;

    private TestEntity entity;

    private Authentication principal, lockOwner;

    {
        Describe("PessimisticLockingInterceptor", () -> {
            BeforeEach(() -> {
                locker = mock(LockingService.class);
                auth = mock(AuthenticationFacade.class);
            });
            JustBeforeEach(() -> {
                interceptor = new PessimisticLockingInterceptor(locker, auth);
            });
            Context("#invoke", () -> {
                BeforeEach(() -> {
                    mi = mock(ProxyMethodInvocation.class);
                });
                JustBeforeEach(() -> {
                    try {
                        result = interceptor.invoke(mi);
                    } catch (Exception e) {
                        this.e = e;
                    }
                });
                Context("given a method invocation", () -> {
                    BeforeEach(() -> {
                        mi = mock(ProxyMethodInvocation.class);
                    });
                    Context("given the method is setContent", () -> {
                        BeforeEach(() -> {
                            when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, InputStream.class));
                            when(mi.getArguments()).thenReturn(new Object[]{new TestEntity(), new ByteArrayInputStream("".getBytes())});
                        });
                        Context("when there is no lock owner", () -> {
                            BeforeEach(() -> {
                                when(locker.lockOwner(0L)).thenReturn(null);
                            });
                            It("should proceed", () -> {
                                verify(locker).lockOwner(0L);
                                verify(mi).proceed();
                            });
                        });
                        Context("when the principal is the lock owner", () -> {
                            BeforeEach(() -> {
                                principal = mock(Authentication.class);
                                when(auth.getAuthentication()).thenReturn(principal);
                                when(locker.lockOwner(0L)).thenReturn(principal);
                                when(locker.isLockOwner(eq(0L), anyObject())).thenReturn(true);
                            });
                            It("should proceed", () -> {
                                verify(locker).lockOwner(0L);
                                verify(locker).isLockOwner(0L,  principal);
                                verify(mi).proceed();
                            });
                        });
                        Context("when the principal is not the lock owner", () -> {
                            BeforeEach(() -> {
                                lockOwner = mock(Authentication.class);
                                principal = mock(Authentication.class);
                                when(auth.getAuthentication()).thenReturn(principal);
                                when(locker.lockOwner(0L)).thenReturn(lockOwner);
                                when(locker.isLockOwner(eq(0L), anyObject())).thenReturn(false);
                            });
                            It("should proceed", () -> {
                                assertThat(e, is(instanceOf(LockOwnerException.class)));
                            });
                        });
                        Context("when the entity doesn't have an ID", () -> {
                            BeforeEach(() -> {
                                when(mi.getArguments()).thenReturn(new Object[]{new Object(), new ByteArrayInputStream("".getBytes())});
                            });
                            It("should throw an IllegalArgumentException", () -> {
                                assertThat(e, is(instanceOf(IllegalArgumentException.class)));
                            });
                        });
                    });
                });
            });
        });
    }

    @NoArgsConstructor
    @Getter
    @Setter
    public class TestEntity {
        @Id
        private Long id = 0L;
    }

    @NoArgsConstructor
    @Getter
    @Setter
    public class TestEntity2 {
        @org.springframework.data.annotation.Id
        private Long id = 0L;
    }
}
