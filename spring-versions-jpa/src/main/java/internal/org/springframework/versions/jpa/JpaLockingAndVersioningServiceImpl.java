package internal.org.springframework.versions.jpa;

import internal.org.springframework.versions.AuthenticationFacade;
import internal.org.springframework.versions.LockingService;
import org.aopalliance.aop.Advice;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.util.Assert;
import org.springframework.versions.LockingAndVersioningService;
import org.springframework.versions.interceptors.OptimisticLockingInterceptor;
import org.springframework.versions.interceptors.PessimisticLockingInterceptor;

import javax.persistence.EntityManager;

public class JpaLockingAndVersioningServiceImpl implements LockingAndVersioningService {

    @Autowired(required=false)
    private PlatformTransactionManager ptm;

    @Autowired(required=false)
    private EntityManager em;

    @Autowired(required=false)
    private LockingService locker;

    @Autowired(required=false)
    private AuthenticationFacade auth;

    public void enableLockingAndVersioning(ProxyFactory result, BeanFactory beanFactory) {
        Assert.notNull(ptm, "Locking and Versioning requires a PlatformTransactionManager cannot be null");
        Assert.notNull(em, "Locking and Versioning requires an EntityManager");
        Assert.notNull(locker, "Locking and Versioning requires a locking service");
        Assert.notNull(auth, "Locking and Versioning requires an authentication service");

        result.addAdvice(transactionInterceptor(ptm, beanFactory));
        result.addAdvice(new OptimisticLockingInterceptor(em));
        result.addAdvice(new PessimisticLockingInterceptor(locker, auth));
    }

    protected Advice transactionInterceptor(PlatformTransactionManager ptm, BeanFactory beanFactory) {
        TransactionInterceptor transactionInterceptor = new TransactionInterceptor(this.ptm, new AnnotationTransactionAttributeSource());
        transactionInterceptor.setBeanFactory(beanFactory);
        transactionInterceptor.afterPropertiesSet();
        return transactionInterceptor;
    }
}
