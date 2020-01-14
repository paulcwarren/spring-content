package internal.org.springframework.content.jpa.config;

import internal.org.springframework.content.jpa.io.DelegatingBlobResourceLoader;
import internal.org.springframework.content.jpa.repository.DefaultJpaStoreImpl;

import org.aopalliance.aop.Advice;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.repository.factory.AbstractStoreFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.util.Assert;

@SuppressWarnings("rawtypes")
public class JpaStoreFactoryBean extends AbstractStoreFactoryBean {

	@Autowired
	private DelegatingBlobResourceLoader blobResourceLoader;

	@Autowired(required=false)
	private PlatformTransactionManager ptm;
	
	@Override
	protected Object getContentStoreImpl() {
		Assert.notNull(blobResourceLoader, "blobResourceLoader cannot be null");
		return new DefaultJpaStoreImpl(blobResourceLoader);
	}

	@Override
	protected void addProxyAdvice(ProxyFactory result, BeanFactory beanFactory) {
		super.addProxyAdvice(result, beanFactory);
		
        result.addAdvice(this.transactionInterceptor(this.ptm, beanFactory));
	}
	
    protected Advice transactionInterceptor(PlatformTransactionManager ptm, BeanFactory beanFactory) {
        TransactionInterceptor transactionInterceptor = new TransactionInterceptor(this.ptm, new AnnotationTransactionAttributeSource());
        transactionInterceptor.setBeanFactory(beanFactory);
        transactionInterceptor.afterPropertiesSet();
        return transactionInterceptor;
    }
}
