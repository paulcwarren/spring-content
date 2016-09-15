package internal.org.springframework.content.s3.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.core.io.s3.PathMatchingSimpleStorageResourcePatternResolver;
import org.springframework.cloud.aws.core.io.s3.SimpleStorageResourceLoader;
import org.springframework.content.commons.repository.factory.AbstractContentStoreFactoryBean;

import com.amazonaws.regions.Region;
import com.amazonaws.services.s3.AmazonS3;

import internal.org.springframework.content.s3.store.DefaultS3RepositoryImpl;

public class S3ContentRepositoryFactoryBean extends AbstractContentStoreFactoryBean {

	private SimpleStorageResourceLoader resourceLoader;
	
	@Autowired
	private AmazonS3 client; 
	
	@Autowired
	private String bucket;
	
	@Autowired
	Region region;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		if (resourceLoader == null) {
			resourceLoader = new SimpleStorageResourceLoader(client);
			resourceLoader.afterPropertiesSet();
		}

		super.afterPropertiesSet();
	}

	@Override
	protected Object getContentStoreImpl() {
		return new DefaultS3RepositoryImpl(resourceLoader, client, region, bucket);
	}

}
