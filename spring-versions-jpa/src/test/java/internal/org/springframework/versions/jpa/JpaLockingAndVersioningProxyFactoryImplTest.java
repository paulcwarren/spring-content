package internal.org.springframework.versions.jpa;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import internal.org.springframework.versions.AuthenticationFacade;
import internal.org.springframework.versions.LockingService;
import org.aopalliance.aop.Advice;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.versions.interceptors.OptimisticLockingInterceptor;
import org.springframework.versions.interceptors.PessimisticLockingInterceptor;

import javax.persistence.EntityManager;
import java.util.List;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(Ginkgo4jRunner.class)
public class JpaLockingAndVersioningProxyFactoryImplTest {

    private JpaLockingAndVersioningProxyFactoryImpl factory;

    private PlatformTransactionManager txn;
    private EntityManager em;
    private LockingService locker;
    private AuthenticationFacade auth;

    private ProxyFactory proxyFactory;
    private BeanFactory beanFactory;

    {
        Describe("JpaLockingAndVersioningProxyFactoryImpl", () -> {
            BeforeEach(() -> {
                txn = mock(PlatformTransactionManager.class);
                em = mock(EntityManager.class);
                locker = mock(LockingService.class);
                auth = mock(AuthenticationFacade.class);
            });
            JustBeforeEach(() -> {
                factory = new JpaLockingAndVersioningProxyFactoryImpl(beanFactory, txn, em, locker, auth);
            });
            Context("#apply", () -> {
                BeforeEach(() -> {
                    proxyFactory = mock(ProxyFactory.class);
                    beanFactory = mock(BeanFactory.class);
                });
                JustBeforeEach(() -> {
                    factory.apply(proxyFactory);
                });
                It("should apply the advice", () -> {
                    ArgumentCaptor<Advice> captor = ArgumentCaptor.forClass(Advice.class);
                    verify(proxyFactory, times(3)).addAdvice(captor.capture());

                    List<Advice> advices = captor.getAllValues();
                    assertThat(advices.get(0), is(instanceOf(TransactionInterceptor.class)));
                    assertThat(advices.get(1), is(instanceOf(OptimisticLockingInterceptor.class)));
                    assertThat(advices.get(2), is(instanceOf(PessimisticLockingInterceptor.class)));
                });
            });
        });
    }
}
