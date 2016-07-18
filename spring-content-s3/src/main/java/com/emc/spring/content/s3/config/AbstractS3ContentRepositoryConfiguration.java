package com.emc.spring.content.s3.config;

import org.springframework.context.annotation.Bean;

import com.amazonaws.regions.Region;

public abstract class AbstractS3ContentRepositoryConfiguration {

	@Bean
	public abstract Region region();
	
	@Bean
	public abstract String bucket();
	
}
