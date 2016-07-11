package org.springframework.content.config;

import org.springframework.context.annotation.Bean;

import com.amazonaws.regions.Region;

public abstract class AbstractS3ContentStoreConfiguration {

	@Bean
	public abstract Region region();
	
	@Bean
	public abstract String bucket();
	
}
