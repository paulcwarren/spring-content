package org.springframework.content.rest.config;

import internal.org.springframework.content.rest.links.ContentLinksResourceProcessor;

import org.springframework.content.commons.storeservice.ContentStoreService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.mapping.RepositoryResourceMappings;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.hateoas.server.RepresentationModelProcessor;

@Configuration
@Import(RestConfiguration.class)
public class HypermediaConfiguration {

	@Bean
	public RepresentationModelProcessor<PersistentEntityResource> contentLinksProcessor(Repositories repos, ContentStoreService stores, RestConfiguration config, RepositoryResourceMappings mappings) {
		return new ContentLinksResourceProcessor(repos, stores, config, mappings);
	}
}
