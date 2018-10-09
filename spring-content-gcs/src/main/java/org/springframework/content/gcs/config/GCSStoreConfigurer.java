package org.springframework.content.gcs.config;

import org.springframework.core.convert.converter.ConverterRegistry;

public interface GCSStoreConfigurer {

	void configureGCSStoreConverters(ConverterRegistry registry);

	void configureGCSObjectIdResolvers(GCSObjectIdResolvers resolvers);
}
