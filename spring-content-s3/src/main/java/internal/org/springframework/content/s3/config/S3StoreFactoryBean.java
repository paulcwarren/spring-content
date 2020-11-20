package internal.org.springframework.content.s3.config;

import java.io.Serializable;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.core.io.s3.SimpleStorageProtocolResolver;
import org.springframework.content.commons.repository.factory.AbstractStoreFactoryBean;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.content.s3.S3ObjectIdResolver;
import org.springframework.content.s3.config.MultiTenantAmazonS3Provider;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.versions.LockingAndVersioningProxyFactory;

import com.amazonaws.services.s3.AmazonS3;

import internal.org.springframework.content.s3.store.DefaultS3StoreImpl;

@SuppressWarnings("rawtypes")
public class S3StoreFactoryBean extends AbstractStoreFactoryBean {

	public static final S3ObjectIdResolver<Serializable> DEFAULT_S3OBJECTID_RESOLVER_STORE = S3ObjectIdResolver.createDefaultS3ObjectIdHelper();

    @Autowired
    private ApplicationContext context;

	@Autowired
	private AmazonS3 client;

	@Autowired
	private PlacementService s3StorePlacementService;

	@Autowired(required=false)
	private MultiTenantAmazonS3Provider s3Provider = null;

	@Autowired(required=false)
	private LockingAndVersioningProxyFactory versioning;

	@Value("${spring.content.s3.bucket:#{environment.AWS_BUCKET}}")
	private String bucket;


	public S3StoreFactoryBean() {
		// required for bean instantiation
	}

	@Autowired
	public S3StoreFactoryBean(ApplicationContext context, AmazonS3 client, PlacementService s3StorePlacementService) {
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

		SimpleStorageProtocolResolver s3Protocol = new SimpleStorageProtocolResolver();
		s3Protocol.afterPropertiesSet();
		s3Protocol.setBeanFactory(context);

		DefaultResourceLoader loader = new DefaultResourceLoader();
		loader.addProtocolResolver(s3Protocol);

		return new DefaultS3StoreImpl(context, loader, s3StorePlacementService, client, s3Provider);
	}
}
