package internal.org.springframework.content.fs.config;

import internal.org.springframework.versions.AuthenticationFacade;
import internal.org.springframework.versions.LockingService;
import org.aopalliance.aop.Advice;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.repository.factory.AbstractStoreFactoryBean;
import org.springframework.content.commons.utils.FileServiceImpl;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.core.convert.ConversionService;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.util.Assert;

import internal.org.springframework.content.fs.repository.DefaultFilesystemStoreImpl;
import org.springframework.util.ClassUtils;
import org.springframework.versions.interceptors.OptimisticLockingInterceptor;
import org.springframework.versions.interceptors.PessimisticLockingInterceptor;

import javax.persistence.EntityManager;

@SuppressWarnings("rawtypes")
public class FilesystemStoreFactoryBean extends AbstractStoreFactoryBean {

	private static boolean lockingAndVersioning = false;

	static {
		lockingAndVersioning = ClassUtils.isPresent("org.springframework.versions.interceptors.PessimisticLockingInterceptor", ClassUtils.getDefaultClassLoader());
	}

	@Autowired
	FileSystemResourceLoader loader;

	@Autowired
	ConversionService filesystemStoreConverter;

	@Autowired(required=false)
	private LockingService versions;

	@Autowired(required=false)
	private AuthenticationFacade auth;

	@Autowired(required=false)
	private EntityManager em;

	@Autowired(required=false)
	private PlatformTransactionManager ptm;

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();

		Assert.notNull(loader, "resource loader cannot be null");
		Assert.notNull(loader, "filesystemStoreConverter cannot be null");
	}

	@Override
	protected Object getContentStoreImpl() {
		return new DefaultFilesystemStoreImpl(loader, filesystemStoreConverter,
				new FileServiceImpl());
	}

	@Override
	protected void addProxyAdvice(ProxyFactory result, BeanFactory beanFactory) {
		if (lockingAndVersioning) {
			if (ptm != null) {
				result.addAdvice(transactionInterceptor(ptm, beanFactory));
			}

			if (versions != null && auth != null) {
				result.addAdvice(new PessimisticLockingInterceptor(versions, auth));
			}

			if (em != null) {
				result.addAdvice(new OptimisticLockingInterceptor(em));
			}
		}
	}

	protected Advice transactionInterceptor(PlatformTransactionManager ptm, BeanFactory beanFactory) {
		TransactionInterceptor transactionInterceptor = new TransactionInterceptor(this.ptm, new AnnotationTransactionAttributeSource());
		transactionInterceptor.setBeanFactory(beanFactory);
		transactionInterceptor.afterPropertiesSet();
		return transactionInterceptor;
	}
}
