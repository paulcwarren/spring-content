package internal.org.springframework.content.azure.config;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.store.factory.AbstractStoreFactoryBean;
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

    private ApplicationContext context;

	private BlobServiceClientBuilder clientBuilder;
    private BlobServiceClient client;

	private PlacementService storePlacementService;

	private AzureStorageProtocolResolver resolver;

//	@Autowired(required=false)
//	private MultiTenantS3ClientProvider s3Provider = null;

    @Autowired(required=false)
    private MappingContext mappingContext;

	@Autowired(required=false)
	private LockingAndVersioningProxyFactory versioning;


	public AzureStorageFactoryBean(Class<? extends Store> storeInterface) {
		super(storeInterface);
	}

	@Autowired
	public void setContext(ApplicationContext context) {
		this.context = context;
	}

	@Autowired
	public void setClientBuilder(BlobServiceClientBuilder clientBuilder) {
		this.clientBuilder = clientBuilder;
		this.client = clientBuilder.buildClient();
	}

	@Autowired
	public void setStorePlacementService(PlacementService storePlacementService) {
		this.storePlacementService = storePlacementService;
	}

	@Autowired
	public void setResolver(AzureStorageProtocolResolver resolver) {
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

		DefaultResourceLoader loader = new DefaultResourceLoader();
		loader.addProtocolResolver(resolver);

		return new DefaultAzureStorageImpl(context, loader, mappingContext, storePlacementService, client);
	}
}
