package internal.org.springframework.content.s3.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.core.io.s3.SimpleStorageResourceLoader;
import org.springframework.content.commons.placement.PlacementService;
import org.springframework.content.commons.repository.factory.AbstractContentStoreFactoryBean;

import com.amazonaws.regions.Region;
import com.amazonaws.services.s3.AmazonS3;

import internal.org.springframework.content.s3.store.DefaultS3StoreImpl;

@SuppressWarnings("rawtypes")
public class S3ContentRepositoryFactoryBean extends AbstractContentStoreFactoryBean {

	@Autowired
	private AmazonS3 client; 

	@Autowired
	private Region region;
	
	@Autowired
	private SimpleStorageResourceLoader loader;
	
	@Autowired
	private PlacementService placement;

	@Value("${spring.content.s3.bucket:#{environment.AWS_BUCKET}}")
	private String bucket;
	
	@Override
	protected Object getContentStoreImpl() {
        client.setRegion(region);
		return new DefaultS3StoreImpl(loader, placement, client, bucket);
	}
}
