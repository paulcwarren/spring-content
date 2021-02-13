package org.springframework.content.s3.config;

import org.springframework.core.convert.converter.ConverterRegistry;

public interface S3StoreConfigurer {

    default void configureS3ObjectIdResolvers(S3ObjectIdResolvers resolvers) {
    }

	void configureS3StoreConverters(ConverterRegistry registry);
}
