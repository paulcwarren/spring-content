package internal.org.springframework.content.gcs.config;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.content.commons.repository.factory.AbstractStoreFactoryBean;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.versions.LockingAndVersioningProxyFactory;

import com.google.cloud.spring.storage.GoogleStorageProtocolResolver;
import com.google.cloud.storage.Storage;

import internal.org.springframework.content.gcs.store.DefaultGCPStorageImpl;

@SuppressWarnings("rawtypes")
public class GCPStorageFactoryBean extends AbstractStoreFactoryBean {

//	public static final S3ObjectIdResolver<Serializable> DEFAULT_S3OBJECTID_RESOLVER_STORE = S3ObjectIdResolver.createDefaultS3ObjectIdHelper();

    @Autowired
    private ApplicationContext context;

	@Autowired
	private Storage client;

	@Autowired
	private PlacementService s3StorePlacementService;

//	@Autowired(required=false)
//	private MultiTenantAmazonS3Provider s3Provider = null;

	@Autowired(required=false)
	private LockingAndVersioningProxyFactory versioning;

	@Autowired
	private GoogleStorageProtocolResolver resolver;

	@Value("${spring.content.gcp.storage.bucket:#{environment.GCP_STORAGE_BUCKET}}")
	private String bucket;

	public GCPStorageFactoryBean() {
		// required for bean instantiation
	}

	@Autowired
	public GCPStorageFactoryBean(ApplicationContext context, Storage client, PlacementService s3StorePlacementService) {
	    this.context = context;
		this.client = client;
		this.s3StorePlacementService = s3StorePlacementService;
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

		return new DefaultGCPStorageImpl(context, loader, s3StorePlacementService, client/*, s3Provider*/);
	}
}
