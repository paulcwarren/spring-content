package org.springframework.content.s3.config;

import org.springframework.cloud.aws.core.io.s3.SimpleStorageResourceLoader;
import org.springframework.context.annotation.Bean;

public abstract class AbstractS3StoreConfiguration {

	@Bean
	public abstract SimpleStorageResourceLoader simpleStorageResourceLoader();
}
