package internal.org.springframework.content.azure.config;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.content.commons.repository.factory.AbstractStoreFactoryBean;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.versions.LockingAndVersioningProxyFactory;

import com.azure.spring.autoconfigure.storage.resource.AzureStorageProtocolResolver;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;

import internal.org.springframework.content.azure.store.DefaultAzureStorageImpl;

@SuppressWarnings("rawtypes")
public class AzureStorageFactoryBean extends AbstractStoreFactoryBean {

    @Autowired
    private ApplicationContext context;

	@Autowired
	private BlobServiceClientBuilder clientBuilder;
    private BlobServiceClient client;

	@Autowired
	private PlacementService s3StorePlacementService;

//	@Autowired(required=false)
//	private MultiTenantAmazonS3Provider s3Provider = null;

	@Autowired(required=false)
	private LockingAndVersioningProxyFactory versioning;

	@Autowired
	private AzureStorageProtocolResolver resolver;

	@Value("${spring.content.gcp.storage.bucket:#{environment.AZURE_STORAGE_BUCKET}}")
	private String bucket;

	public AzureStorageFactoryBean() {
		// required for bean instantiation
	}

	@Autowired
	public AzureStorageFactoryBean(ApplicationContext context, BlobServiceClientBuilder client, PlacementService s3StorePlacementService) {
	    this.context = context;
		this.client = client.buildClient();
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

		return new DefaultAzureStorageImpl(context, loader, s3StorePlacementService, client/*, s3Provider*/);
	}
}
