package org.springframework.content.azure.config;

import org.springframework.core.convert.converter.ConverterRegistry;

public interface AzureStorageConfigurer {

	void configureGCPStorageConverters(ConverterRegistry registry);
}
