package org.springframework.content.azure.config;

import org.springframework.core.convert.converter.ConverterRegistry;

public interface AzureStorageConfigurer {

	void configureAzureStorageConverters(ConverterRegistry registry);
}
