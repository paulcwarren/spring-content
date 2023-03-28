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

    @Autowired
    private ApplicationContext context;

	@Autowired
	private BlobServiceClientBuilder clientBuilder;
    private BlobServiceClient client;

	@Autowired
	private PlacementService storePlacementService;

//	@Autowired(required=false)
//	private MultiTenantS3ClientProvider s3Provider = null;

    @Autowired(required=false)
    private MappingContext mappingContext;

	@Autowired(required=false)
	private LockingAndVersioningProxyFactory versioning;

	@Autowired
	private AzureStorageProtocolResolver resolver;

	@Autowired
	public AzureStorageFactoryBean(Class<? extends Store> storeInterface, ApplicationContext context, BlobServiceClientBuilder client, PlacementService storePlacementService) {
		super(storeInterface);
	    this.context = context;
		this.client = client.buildClient();
		this.storePlacementService = storePlacementService;
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
