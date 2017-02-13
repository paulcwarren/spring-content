package internal.org.springframework.content.s3.operations;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.core.io.s3.SimpleStorageResourceLoader;
import org.springframework.content.commons.operations.AbstractResourceTemplate;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.amazonaws.regions.Region;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;

public class S3ResourceTemplate extends AbstractResourceTemplate implements InitializingBean {

	private SimpleStorageResourceLoader resourceLoader;
	
	@Autowired
	private AmazonS3 client; 
	
	@Autowired
	private String bucket;
	
	@Autowired
	Region region;
	
	@Override
	public void afterPropertiesSet() throws Exception {
        client.setRegion(region);

        if (resourceLoader == null) {
			resourceLoader = new SimpleStorageResourceLoader(client);
			resourceLoader.afterPropertiesSet();
		}
	}

	public S3ResourceTemplate(ResourceLoader resourceLoader) {
		super(resourceLoader);
	}

	@Override
	protected String getLocation(Object contentId) {
		return "s3://" + bucket + "/" + contentId;
	}

	@Override
	protected void deleteResource(Resource resource) throws Exception {
		if (resource.exists()) {
			client.deleteObject(new DeleteObjectRequest(bucket, resource.getFilename()));
		}
	}

}
