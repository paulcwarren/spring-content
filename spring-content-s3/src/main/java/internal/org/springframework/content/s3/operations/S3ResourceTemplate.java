package internal.org.springframework.content.s3.operations;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.core.io.s3.SimpleStorageResourceLoader;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.operations.AbstractResourceTemplate;
import org.springframework.content.commons.operations.ContentOperations;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.amazonaws.regions.Region;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import org.springframework.core.io.WritableResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class S3ResourceTemplate extends AbstractResourceTemplate implements InitializingBean, ContentOperations {

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
	public String getLocation(Object contentId) {
		return "s3://" + bucket + "/" + contentId;
	}

	@Override
	protected void deleteResource(Resource resource) throws Exception {
		if (resource.exists()) {
			client.deleteObject(new DeleteObjectRequest(bucket, resource.getFilename()));
		}
	}

}
