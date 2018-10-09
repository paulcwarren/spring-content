package internal.org.springframework.content.gcs.config;

import java.io.Serializable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.repository.factory.AbstractStoreFactoryBean;
import org.springframework.content.gcs.GCSObjectIdResolver;
import org.springframework.content.gcs.config.GCSObjectIdResolvers;
import org.springframework.core.convert.ConversionService;

import com.google.cloud.storage.Storage;

import internal.org.springframework.content.gcs.store.DefaultGCSStoreImpl;

@SuppressWarnings("rawtypes")
public class GCSStoreFactoryBean extends AbstractStoreFactoryBean {

	public static final GCSObjectIdResolver<Serializable> DEFAULT_GCSOBJECTID_RESOLVER_STORE = GCSObjectIdResolver
			.createDefaultGCSObjectIdHelper();

	@Autowired
	private Storage client;

	@Autowired
	private ConversionService GCSStoreConverter;

	@Autowired
	private GCSObjectIdResolvers resolvers;

	@Value("${spring.content.gcs.bucket:#{environment.GCS_BUCKET}}")
	private String bucket;

	public GCSStoreFactoryBean() {
	}

	@Autowired
	public GCSStoreFactoryBean(Storage client, ConversionService GCSStoreConverter, GCSObjectIdResolvers resolvers) {
		this.client = client;
		this.GCSStoreConverter = GCSStoreConverter;
		this.resolvers = resolvers;
	}

	@Override
	protected Object getContentStoreImpl() {
		GCSObjectIdResolver resolver = null;
		if (AssociativeStore.class.isAssignableFrom(this.getStoreInterface())
				|| ContentStore.class.isAssignableFrom(this.getStoreInterface())) {
			resolver = resolvers.getResolverFor(this.getDomainClass(this.getStoreInterface()));
			if (resolver == null) {
				resolver = resolvers.getResolverFor(this.getContentIdClass(this.getStoreInterface()));
				if (resolver == null) {
					resolver = new DefaultAssociativeStoreGCSObjectIdResolver(this.GCSStoreConverter);
				}
			}
		} else if (Store.class.isAssignableFrom(this.getStoreInterface())) {
			resolver = resolvers.getResolverFor(this.getContentIdClass(this.getStoreInterface()));
			if (resolver == null) {
				resolver = DEFAULT_GCSOBJECTID_RESOLVER_STORE;
			}
		}

		return new DefaultGCSStoreImpl(GCSStoreConverter, client, resolver, bucket);
	}
}
