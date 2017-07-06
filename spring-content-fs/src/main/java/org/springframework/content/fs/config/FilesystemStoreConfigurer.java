package org.springframework.content.fs.config;

import org.springframework.core.convert.converter.ConverterRegistry;

public interface FilesystemStoreConfigurer {
	
	void configureFilesystemStoreConverters(ConverterRegistry registry);
	
}
