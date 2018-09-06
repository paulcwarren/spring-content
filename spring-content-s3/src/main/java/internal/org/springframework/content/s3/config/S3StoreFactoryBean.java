package internal.org.springframework.content.s3.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.core.io.s3.SimpleStorageProtocolResolver;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.repository.factory.AbstractStoreFactoryBean;
import org.springframework.content.s3.S3ObjectIdResolver;
import org.springframework.content.s3.config.S3ObjectIdResolvers;
import org.springframework.core.convert.ConversionService;

import com.amazonaws.services.s3.AmazonS3;

import internal.org.springframework.content.s3.store.DefaultS3StoreImpl;
import org.springframework.core.io.DefaultResourceLoader;

import java.io.Serializable;

@SuppressWarnings("rawtypes")
public class S3StoreFactoryBean extends AbstractStoreFactoryBean {

	public static final S3ObjectIdResolver<Serializable> DEFAULT_S3OBJECTID_RESOLVER_STORE = S3ObjectIdResolver
			.createDefaultS3ObjectIdHelper();

	@Autowired
	private AmazonS3 client;

	@Autowired
	private ConversionService s3StoreConverter;

	@Autowired
	private S3ObjectIdResolvers resolvers;

	@Value("${spring.content.s3.bucket:#{environment.AWS_BUCKET}}")
	private String bucket;

	public S3StoreFactoryBean() {
	}

	@Autowired
	public S3StoreFactoryBean(AmazonS3 client, ConversionService s3StoreConverter, S3ObjectIdResolvers resolvers) {
		this.client = client;
		this.s3StoreConverter = s3StoreConverter;
		this.resolvers = resolvers;
	}

	@Override
	protected Object getContentStoreImpl() {
		S3ObjectIdResolver resolver = null;
		if (AssociativeStore.class.isAssignableFrom(this.getStoreInterface())
				|| ContentStore.class.isAssignableFrom(this.getStoreInterface())) {
			resolver = resolvers
					.getResolverFor(this.getDomainClass(this.getStoreInterface()));
			if (resolver == null) {
				resolver = resolvers
						.getResolverFor(this.getContentIdClass(this.getStoreInterface()));
				if (resolver == null) {
					resolver = new DefaultAssociativeStoreS3ObjectIdResolver(
							this.s3StoreConverter);
				}
			}
		}
		else if (Store.class.isAssignableFrom(this.getStoreInterface())) {
			resolver = resolvers
					.getResolverFor(this.getContentIdClass(this.getStoreInterface()));
			if (resolver == null) {
				resolver = DEFAULT_S3OBJECTID_RESOLVER_STORE;
			}
		}

		SimpleStorageProtocolResolver s3Protocol = new SimpleStorageProtocolResolver(client);
		s3Protocol.afterPropertiesSet();

		DefaultResourceLoader loader = new DefaultResourceLoader();
		loader.addProtocolResolver(s3Protocol);

		return new DefaultS3StoreImpl(/* Edited */loader, s3StoreConverter, client, resolver, bucket);
	}
}
