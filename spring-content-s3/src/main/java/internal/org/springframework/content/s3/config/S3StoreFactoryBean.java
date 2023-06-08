package internal.org.springframework.content.s3.config;

import org.apache.commons.lang.ClassUtils;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.repository.ReactiveContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.store.factory.AbstractStoreFactoryBean;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.content.s3.config.MultiTenantS3ClientProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.versions.LockingAndVersioningProxyFactory;

import internal.org.springframework.content.s3.io.SimpleStorageProtocolResolver;
import internal.org.springframework.content.s3.store.DefaultReactiveS3StoreImpl;
import internal.org.springframework.content.s3.store.DefaultS3StoreImpl;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;

@SuppressWarnings("rawtypes")
public class S3StoreFactoryBean extends AbstractStoreFactoryBean {

    private ApplicationContext context;

	private S3Client client;

	private PlacementService s3StorePlacementService;

    @Autowired(required=false)
    private S3AsyncClient asyncClient;

	@Autowired(required=false)
	private MultiTenantS3ClientProvider s3Provider = null;

	@Autowired(required=false)
	private LockingAndVersioningProxyFactory versioning;

    @Autowired(required=false)
    private MappingContext mappingContext;

	@Value("${spring.content.s3.bucket:#{environment.AWS_BUCKET}}")
	private String bucket;

	public S3StoreFactoryBean(Class<? extends Store> storeInterface) {
		super(storeInterface);
	}

	@Autowired
	public void setContext(ApplicationContext context) {
		this.context = context;
	}

	@Autowired
	public void setClient(S3Client client) {
		this.client = client;
	}

	@Autowired
	public void setS3StorePlacementService(PlacementService s3StorePlacementService) {
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

		SimpleStorageProtocolResolver s3Protocol = new SimpleStorageProtocolResolver(client);
		s3Protocol.afterPropertiesSet();

		DefaultResourceLoader loader = new DefaultResourceLoader();
		loader.addProtocolResolver(s3Protocol);

        if (!ClassUtils.getAllInterfaces(getStoreInterface()).contains(ReactiveContentStore.class)) {
		    if (client == null) {
		        throw new NoSuchBeanDefinitionException(S3Client.class.getCanonicalName());
		    }
		    return new DefaultS3StoreImpl(context, loader, mappingContext, s3StorePlacementService, client, s3Provider);
		} else {
            if (asyncClient == null) {
                throw new NoSuchBeanDefinitionException(S3AsyncClient.class.getCanonicalName());
            }
            return new DefaultReactiveS3StoreImpl(context, loader, mappingContext, s3StorePlacementService, asyncClient, s3Provider);
		}
	}
}
