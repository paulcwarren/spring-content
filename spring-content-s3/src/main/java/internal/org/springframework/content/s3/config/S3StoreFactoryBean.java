package internal.org.springframework.content.s3.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.core.io.s3.SimpleStorageResourceLoader;
import org.springframework.content.commons.repository.factory.AbstractStoreFactoryBean;
import org.springframework.core.convert.ConversionService;

import com.amazonaws.services.s3.AmazonS3;

import internal.org.springframework.content.s3.store.DefaultS3StoreImpl;

@SuppressWarnings("rawtypes")
public class S3StoreFactoryBean extends AbstractStoreFactoryBean {

	@Autowired
	private AmazonS3 client; 

	@Autowired
	private SimpleStorageResourceLoader loader;
	
	@Autowired
	private ConversionService s3StoreConverter;

	@Value("${spring.content.s3.bucket:#{environment.AWS_BUCKET}}")
	private String bucket;
	
	@Override
	protected Object getContentStoreImpl() {
		return new DefaultS3StoreImpl(loader, s3StoreConverter, client, bucket);
	}
}
