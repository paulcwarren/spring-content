package org.springframework.content.rest.config;

import org.springframework.content.commons.storeservice.ContentStoreService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.hateoas.ResourceProcessor;

import internal.org.springframework.content.rest.links.ContentLinksResourceProcessor;

@Configuration
public class HypermediaConfiguration {

	@Bean
	public ResourceProcessor<PersistentEntityResource> contentLinksProcessor(ContentStoreService stores) {
		return new ContentLinksResourceProcessor(stores);
	}
}
