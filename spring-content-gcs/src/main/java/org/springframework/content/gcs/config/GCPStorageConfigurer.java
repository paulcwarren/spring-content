package org.springframework.content.gcs.config;

import org.springframework.core.convert.converter.ConverterRegistry;

public interface GCPStorageConfigurer {

	void configureGCPStorageConverters(ConverterRegistry registry);
}
