package internal.org.springframework.content.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.core.io.s3.PathMatchingSimpleStorageResourcePatternResolver;
import org.springframework.content.common.repository.factory.AbstractContentStoreFactoryBean;

import com.amazonaws.regions.Region;
import com.amazonaws.services.s3.AmazonS3;

import internal.org.springframework.content.store.DefaultS3ContentStoreImpl;

public class S3ContentRepositoryFactoryBean extends AbstractContentStoreFactoryBean {

	@Autowired
	private PathMatchingSimpleStorageResourcePatternResolver resourceLoader;

	@Autowired
	private AmazonS3 client; 
	
	@Autowired
	private String bucket;
	
	@Autowired
	Region region;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
	}

	@Override
	protected Object getContentStoreImpl() {
		return new DefaultS3ContentStoreImpl(resourceLoader, client, region, bucket);
	}

}
