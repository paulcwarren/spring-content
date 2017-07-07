package org.springframework.content.s3.config;

import org.springframework.core.convert.converter.ConverterRegistry;

public interface S3StoreConfigurer {

	void configureS3StoreConverters(ConverterRegistry registry);

}
