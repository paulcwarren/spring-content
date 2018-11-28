package internal.org.springframework.versions.jpa;

import internal.org.springframework.versions.AuthenticationFacade;
import internal.org.springframework.versions.LockingService;
import org.aopalliance.aop.Advice;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.util.Assert;
import org.springframework.versions.LockingAndVersioningProxyFactory;
import org.springframework.versions.interceptors.OptimisticLockingInterceptor;
import org.springframework.versions.interceptors.PessimisticLockingInterceptor;

import javax.persistence.EntityManager;

public class JpaLockingAndVersioningProxyFactoryImpl implements LockingAndVersioningProxyFactory {

    private BeanFactory beanFactory;
    private PlatformTransactionManager ptm;
    private EntityManager em;
    private LockingService locker;
    private AuthenticationFacade auth;

    public JpaLockingAndVersioningProxyFactoryImpl(BeanFactory beanFactory,
                                                   PlatformTransactionManager ptm,
                                                   EntityManager em,
                                                   LockingService locker,
                                                   AuthenticationFacade auth) {
        this.beanFactory = beanFactory;
        this.ptm = ptm;
        this.em = em;
        this.locker = locker;
        this.auth = auth;
    }

    public void apply(ProxyFactory result) {
        Assert.notNull(beanFactory, "Locking and Versioning requires a BeanFactory");
        Assert.notNull(ptm, "Locking and Versioning requires a PlatformTransactionManager");
        Assert.notNull(em, "Locking and Versioning requires an EntityManager");
        Assert.notNull(locker, "Locking and Versioning requires a locking service");
        Assert.notNull(auth, "Locking and Versioning requires an authentication service");

        result.addAdvice(transactionInterceptor(ptm));
        result.addAdvice(new OptimisticLockingInterceptor(em));
        result.addAdvice(new PessimisticLockingInterceptor(locker, auth));
    }

    protected Advice transactionInterceptor(PlatformTransactionManager ptm) {
        TransactionInterceptor transactionInterceptor = new TransactionInterceptor(this.ptm, new AnnotationTransactionAttributeSource());
        transactionInterceptor.setBeanFactory(beanFactory);
        transactionInterceptor.afterPropertiesSet();
        return transactionInterceptor;
    }
}
