package internal.org.springframework.versions.jpa;

import javax.persistence.EntityManager;

import org.aopalliance.aop.Advice;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.util.Assert;
import org.springframework.versions.LockingAndVersioningProxyFactory;
import org.springframework.versions.interceptors.OptimisticLockingInterceptor;

import internal.org.springframework.versions.AuthenticationFacade;

public class JpaAutoVersioningProxyFactoryImpl implements LockingAndVersioningProxyFactory {

    private BeanFactory beanFactory;
    private PlatformTransactionManager ptm;
    private EntityManager em;
    private AuthenticationFacade auth;

    public JpaAutoVersioningProxyFactoryImpl(BeanFactory beanFactory,
            PlatformTransactionManager ptm,
            EntityManager em,
            AuthenticationFacade auth) {
        this.beanFactory = beanFactory;
        this.ptm = ptm;
        this.em = em;
        this.auth = auth;
    }

    @Override
    public void apply(ProxyFactory proxy) {
        Assert.notNull(beanFactory, "Locking and Versioning requires a BeanFactory");
        Assert.notNull(ptm, "Locking and Versioning requires a PlatformTransactionManager");
        Assert.notNull(em, "Locking and Versioning requires an EntityManager");
        Assert.notNull(auth, "Locking and Versioning requires an authentication service");

        addTransactionAdviceIfNeeded(proxy, ptm);
        proxy.addAdvice(new OptimisticLockingInterceptor(em));
//        proxy.addAdvice(new PessimisticLockingInterceptor(locker, auth));
    }

    protected void addTransactionAdviceIfNeeded(ProxyFactory proxy, PlatformTransactionManager ptm) {
        Advisor[] advisors = proxy.getAdvisors();
        for (Advisor advisor : advisors) {
            if (advisor.getAdvice() instanceof TransactionInterceptor) {
                return;
            }
        }
        proxy.addAdvice(transactionInterceptor(ptm));
    }

    protected Advice transactionInterceptor(PlatformTransactionManager ptm) {
        TransactionInterceptor transactionInterceptor = new TransactionInterceptor(this.ptm, new AnnotationTransactionAttributeSource());
        transactionInterceptor.setBeanFactory(beanFactory);
        transactionInterceptor.afterPropertiesSet();
        return transactionInterceptor;
    }
}
