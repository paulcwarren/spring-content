package internal.org.springframework.content.s3.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.core.io.s3.SimpleStorageResourceLoader;
import org.springframework.content.commons.repository.factory.AbstractContentStoreFactoryBean;
import org.springframework.util.Assert;

import internal.org.springframework.content.s3.operations.S3ResourceTemplate;
import internal.org.springframework.content.s3.store.DefaultS3RepositoryImpl;

public class S3ContentRepositoryFactoryBean extends AbstractContentStoreFactoryBean {

	private SimpleStorageResourceLoader resourceLoader;
	
	@Autowired
	private S3ResourceTemplate template;
	
	@Override
	protected Object getContentStoreImpl() {
		Assert.notNull(template, "template cannot be null");
		return new DefaultS3RepositoryImpl(template);
	}

}
