package internal.org.springframework.content.gcs.config;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.store.factory.AbstractStoreFactoryBean;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.versions.LockingAndVersioningProxyFactory;

import com.google.cloud.spring.storage.GoogleStorageProtocolResolver;
import com.google.cloud.storage.Storage;

import internal.org.springframework.content.gcs.store.DefaultGCPStorageImpl;

@SuppressWarnings("rawtypes")
public class GCPStorageFactoryBean extends AbstractStoreFactoryBean {

    private ApplicationContext context;

	private Storage client;

	private PlacementService s3StorePlacementService;

	private GoogleStorageProtocolResolver resolver;

//	@Autowired(required=false)
//	private MultiTenantS3ClientProvider s3Provider = null;

    @Autowired(required=false)
    private MappingContext mappingContext;

	@Autowired(required=false)
	private LockingAndVersioningProxyFactory versioning;

	@Value("${spring.content.gcp.storage.bucket:#{environment.GCP_STORAGE_BUCKET}}")
	private String bucket;

	public GCPStorageFactoryBean(Class<? extends Store> storeInterface) {
		super(storeInterface);
	    this.context = context;
		this.client = client;
		this.s3StorePlacementService = s3StorePlacementService;
	}

	@Autowired
	public void setContext(ApplicationContext context) {
		this.context = context;
	}

	@Autowired
	public void setClient(Storage client) {
		this.client = client;
	}

	@Autowired
	public void setS3StorePlacementService(PlacementService s3StorePlacementService) {
		this.s3StorePlacementService = s3StorePlacementService;
	}

	@Autowired
	public void setResolver(GoogleStorageProtocolResolver resolver) {
		this.resolver = resolver;
	}

	@Override
	protected void addProxyAdvice(ProxyFactory result, BeanFactory beanFactory) {
		if (versioning != null) {
			versioning.apply(result);
		}
	}

	@Override
	protected Object getContentStoreImpl() {

//		GoogleStorageProtocolResolver s3Protocol = new GoogleStorageProtocolResolver();
//		s3Protocol.afterPropertiesSet();
//		s3Protocol.setBeanFactory(context);
//
		DefaultResourceLoader loader = new DefaultResourceLoader();
		loader.addProtocolResolver(resolver);

		return new DefaultGCPStorageImpl(context, loader, mappingContext, s3StorePlacementService, client/*, s3Provider*/);
	}
}
